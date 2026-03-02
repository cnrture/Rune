package com.github.teknasyon.plugin.actions

import com.github.teknasyon.plugin.actions.dialog.CreatePRDialog
import com.github.teknasyon.plugin.common.CliUtils
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File

class CreateReviewPRAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project?.basePath != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Preparing review PR…", false) {
            override fun run(indicator: ProgressIndicator) {
                val dir = File(project.basePath ?: return)
                val useReviewBranch = PluginSettingsService.getInstance(project).isUseReviewBranch()

                // 1. Current branch
                val currentBranch = runGit(dir, "rev-parse", "--abbrev-ref", "HEAD")

                // 2. Detect base branch automatically
                indicator.text = "Detecting base branch…"
                val baseBranch = detectBaseBranch(dir, currentBranch)
                indicator.text = "Base branch: $baseBranch"

                // 3. Fetch remote refs
                indicator.text = "Fetching remote…"
                runGit(dir, "fetch", "origin")

                // 4. Determine target branch
                val targetBranch = if (useReviewBranch) {
                    val reviewBranch = "review/$currentBranch"
                    val reviewExists = runGit(dir, "ls-remote", "--heads", "origin", reviewBranch).isNotBlank()
                    if (!reviewExists) {
                        indicator.text = "Creating $reviewBranch from $baseBranch…"
                        val result = runGit(dir, "push", "origin", "origin/$baseBranch:refs/heads/$reviewBranch")
                        if (result.contains("error") || result.contains("fatal")) {
                            notify(project, "Failed to create $reviewBranch: $result", NotificationType.ERROR)
                            return
                        }
                    }
                    reviewBranch
                } else {
                    baseBranch
                }

                // 5. Push current branch
                indicator.text = "Pushing $currentBranch…"
                runGit(dir, "push", "-u", "origin", currentBranch)

                // 6. Find gh CLI
                val ghPath = findGhCli()
                if (ghPath == null) {
                    notify(
                        project,
                        Constants.GH_CLI_NOT_FOUND_MESSAGE,
                        NotificationType.ERROR
                    )
                    return
                }

                // 7. Parse owner/repo
                val ownerRepo = parseOwnerRepo(dir)
                if (ownerRepo == null) {
                    notify(
                        project,
                        "Could not parse owner/repo from git remote. Ensure 'origin' remote is set.",
                        NotificationType.ERROR,
                    )
                    return
                }

                // 8. Extract Jira ticket ID from branch name
                val ticketId = Constants.JIRA_TICKET_REGEX.find(currentBranch)?.value

                // 9. Open dialog on EDT for reviewer/label selection
                ApplicationManager.getApplication().invokeLater {
                    val dialog = CreatePRDialog(
                        ghPath = ghPath,
                        dir = dir,
                        owner = ownerRepo.first,
                        repo = ownerRepo.second,
                        ticketId = ticketId,
                        onConfirm = { reviewers, labels ->
                            createPR(
                                project = project,
                                dir = dir,
                                ghPath = ghPath,
                                currentBranch = currentBranch,
                                targetBranch = targetBranch,
                                ownerRepo = ownerRepo,
                                reviewers = reviewers,
                                labels = labels,
                            )
                        },
                    )
                    dialog.show()
                }
            }
        })
    }

    private fun createPR(
        project: Project,
        dir: File,
        ghPath: String,
        currentBranch: String,
        targetBranch: String,
        ownerRepo: Pair<String, String>,
        reviewers: List<String>,
        labels: List<String>,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating PR…", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Creating PR $currentBranch → $targetBranch…"

                val jiraUrl = getJiraTicketUrl(dir)

                // Check if a tag with the same name as targetBranch exists on remote.
                // If so, gh pr create (GraphQL) fails with "Base ref must be a branch"
                // because it resolves the ambiguous ref to the tag. Use REST API instead.
                val hasTagCollision = runGit(dir, "ls-remote", "--tags", "origin", targetBranch).isNotBlank()

                val prUrl = if (hasTagCollision) {
                    createPRviaRestApi(dir, ghPath, ownerRepo, currentBranch, targetBranch, jiraUrl, reviewers, labels)
                } else {
                    createPRviaGhCli(dir, ghPath, currentBranch, targetBranch, jiraUrl, reviewers, labels)
                }

                if (prUrl != null) {
                    ApplicationManager.getApplication().invokeLater { BrowserUtil.browse(prUrl) }
                    notify(project, "PR created: $prUrl", NotificationType.INFORMATION)
                } else {
                    val existingUrl = runProcess(dir, ghPath, "pr", "view", "--json", "url", "--jq", ".url")
                        .trim().takeIf { it.startsWith("https://") }
                    if (existingUrl != null) {
                        ApplicationManager.getApplication().invokeLater { BrowserUtil.browse(existingUrl) }
                        notify(project, "PR already exists: $existingUrl", NotificationType.INFORMATION)
                    } else {
                        notify(project, "PR creation failed. A tag with the same name as '$targetBranch' may exist on remote, causing ambiguity.", NotificationType.WARNING)
                    }
                }
            }
        })
    }

    private fun createPRviaGhCli(
        dir: File,
        ghPath: String,
        currentBranch: String,
        targetBranch: String,
        jiraUrl: String?,
        reviewers: List<String>,
        labels: List<String>,
    ): String? {
        val cmd = mutableListOf(
            ghPath,
            "pr", "create",
            "--assignee", "@me",
            "--base", targetBranch,
            "--head", currentBranch,
            "--title", currentBranch,
            "--body", jiraUrl ?: "",
        )

        if (reviewers.isNotEmpty()) {
            cmd += "--reviewer"
            cmd += reviewers.joinToString(",")
        }
        if (labels.isNotEmpty()) {
            cmd += "--label"
            cmd += labels.joinToString(",")
        }

        val prOutput = runProcess(dir, *cmd.toTypedArray())
        return prOutput.lines().firstOrNull { it.startsWith("https://github.com") }
    }

    /**
     * Creates a PR via GitHub REST API (`gh api`), which resolves `base` to a branch
     * even when a tag with the same name exists.
     */
    private fun createPRviaRestApi(
        dir: File,
        ghPath: String,
        ownerRepo: Pair<String, String>,
        currentBranch: String,
        targetBranch: String,
        jiraUrl: String?,
        reviewers: List<String>,
        labels: List<String>,
    ): String? {
        val (owner, repo) = ownerRepo

        // Create PR via REST API
        val createCmd = mutableListOf(
            ghPath, "api",
            "repos/$owner/$repo/pulls",
            "--method", "POST",
            "-f", "title=$currentBranch",
            "-f", "head=$currentBranch",
            "-f", "base=$targetBranch",
            "-f", "body=${jiraUrl ?: ""}",
            "--jq", ".html_url,.number",
        )

        val createOutput = runProcess(dir, *createCmd.toTypedArray()).trim()
        val outputLines = createOutput.lines()
        val prUrl = outputLines.firstOrNull { it.startsWith("https://github.com") } ?: return null
        val prNumber = outputLines.lastOrNull()?.trim() ?: return prUrl

        // Add assignee, reviewers, labels via gh pr edit
        val editCmd = mutableListOf(ghPath, "pr", "edit", prNumber, "--add-assignee", "@me")
        if (reviewers.isNotEmpty()) {
            editCmd += "--add-reviewer"
            editCmd += reviewers.joinToString(",")
        }
        if (labels.isNotEmpty()) {
            editCmd += "--add-label"
            editCmd += labels.joinToString(",")
        }
        runProcess(dir, *editCmd.toTypedArray())

        return prUrl
    }

    /**
     * Parses owner and repo name from the 'origin' remote URL.
     * Supports both HTTPS (https://github.com/owner/repo.git) and SSH (git@github.com:owner/repo.git) formats.
     */
    private fun parseOwnerRepo(dir: File): Pair<String, String>? {
        val remoteUrl = runGit(dir, "remote", "get-url", "origin").trim()
        if (remoteUrl.isBlank()) return null

        // SSH: git@github.com:owner/repo.git
        val sshMatch = Regex("""git@[^:]+:([^/]+)/(.+?)(?:\.git)?$""").find(remoteUrl)
        if (sshMatch != null) {
            return sshMatch.groupValues[1] to sshMatch.groupValues[2]
        }

        // HTTPS: https://github.com/owner/repo.git
        val httpsMatch = Regex("""https?://[^/]+/([^/]+)/(.+?)(?:\.git)?$""").find(remoteUrl)
        if (httpsMatch != null) {
            return httpsMatch.groupValues[1] to httpsMatch.groupValues[2]
        }

        return null
    }

    /**
     * Detects the base branch this branch was created from.
     *
     * Method 1: reflog — looks for "Created from <branch>" in the oldest reflog entry.
     * Method 2: merge-base — compares with common branches and picks the closest one.
     */
    private fun detectBaseBranch(dir: File, currentBranch: String): String {
        // Method 1: reflog
        val reflogLines = runGit(dir, "reflog", "show", "--format=%gs", currentBranch).lines()
        val createdFromEntry = reflogLines
            .lastOrNull { it.contains("Created from", ignoreCase = true) }
            ?: reflogLines.lastOrNull { it.startsWith("branch:") }

        if (createdFromEntry != null) {
            val match = Regex("(?i)(?:created from|moving from .+ to) ([^\\s]+)$").find(createdFromEntry)
            val detected = match?.groupValues?.get(1)
                ?.removePrefix("refs/heads/")
                ?.removePrefix("origin/")
                ?.trim()
            if (!detected.isNullOrBlank() && detected != currentBranch && isBranch(dir, detected)) {
                return detected
            }
        }

        // Method 2: find closest common ancestor among known branches
        val candidates = listOf("develop", "main", "master", "staging", "release")
        return candidates.minByOrNull { candidate ->
            val mergeBase = runGit(dir, "merge-base", "HEAD", "origin/$candidate").trim()
            if (mergeBase.isBlank()) Int.MAX_VALUE
            else runGit(dir, "rev-list", "--count", "$mergeBase..HEAD").trim().toIntOrNull() ?: Int.MAX_VALUE
        } ?: "develop"
    }

    /**
     * Checks whether the given ref name is a branch (local or remote), not a tag.
     */
    private fun isBranch(dir: File, ref: String): Boolean {
        val local = runGit(dir, "branch", "--list", ref).trim()
        if (local.isNotBlank()) return true
        val remote = runGit(dir, "branch", "--list", "--remotes", "origin/$ref").trim()
        return remote.isNotBlank()
    }

    private fun findGhCli(): String? = CliUtils.findGhCli()

    private fun getJiraTicketUrl(dir: File): String? {
        val branch = CliUtils.runGit(dir, "rev-parse", "--abbrev-ref", "HEAD")
        val ticketId = Constants.JIRA_TICKET_REGEX.find(branch)?.value ?: return null
        return Constants.jiraBrowseUrl(ticketId)
    }

    private fun runGit(dir: File, vararg args: String): String = CliUtils.runGit(dir, *args)

    private fun runProcess(dir: File, vararg cmd: String): String = CliUtils.runProcess(dir, *cmd)

    private fun notify(project: Project, message: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TeknasyonIntelliJPlugin")
                .createNotification(message, type)
                .notify(project)
        }
    }
}
