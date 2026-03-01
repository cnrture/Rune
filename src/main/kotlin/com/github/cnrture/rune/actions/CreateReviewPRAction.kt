package com.github.cnrture.rune.actions

import com.github.cnrture.rune.actions.dialog.CreatePRDialog
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
import java.util.concurrent.TimeUnit

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

                // 1. Current branch
                val currentBranch = runGit(dir, "rev-parse", "--abbrev-ref", "HEAD")
                val reviewBranch = "review/$currentBranch"

                // 2. Detect base branch automatically
                indicator.text = "Detecting base branch…"
                val baseBranch = detectBaseBranch(dir, currentBranch)
                indicator.text = "Base branch: $baseBranch"

                // 3. Fetch remote refs
                indicator.text = "Fetching remote…"
                runGit(dir, "fetch", "origin")

                // 4. Create review branch on remote from base (skip if already exists)
                val reviewExists = runGit(dir, "ls-remote", "--heads", "origin", reviewBranch).isNotBlank()
                if (!reviewExists) {
                    indicator.text = "Creating $reviewBranch from $baseBranch…"
                    val result = runGit(dir, "push", "origin", "origin/$baseBranch:refs/heads/$reviewBranch")
                    if (result.contains("error") || result.contains("fatal")) {
                        notify(project, "Failed to create $reviewBranch: $result", NotificationType.ERROR)
                        return
                    }
                }

                // 5. Push current branch
                indicator.text = "Pushing $currentBranch…"
                runGit(dir, "push", "-u", "origin", currentBranch)

                // 6. Find gh CLI
                val ghPath = findGhCli()
                if (ghPath == null) {
                    notify(
                        project,
                        "GitHub CLI (gh) not found. Install from https://cli.github.com and run 'gh auth login'.",
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
                val ticketId = Regex("[A-Z]+-\\d+").find(currentBranch)?.value

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
                                reviewBranch = reviewBranch,
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
        reviewBranch: String,
        reviewers: List<String>,
        labels: List<String>,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating review PR…", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Creating PR $currentBranch → $reviewBranch…"

                val jiraUrl = getJiraTicketUrl(dir)

                val cmd = mutableListOf(
                    ghPath,
                    "pr", "create",
                    "--assignee", "@me",
                    "--base", reviewBranch,
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

                val prUrl = prOutput.lines().firstOrNull { it.startsWith("https://github.com") }
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
                        notify(project, "PR creation output:\n$prOutput", NotificationType.WARNING)
                    }
                }
            }
        })
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
            if (!detected.isNullOrBlank() && detected != currentBranch) {
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

    private fun findGhCli(): String? {
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which gh")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(5, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            listOf("/usr/local/bin/gh", "/usr/bin/gh", "/opt/homebrew/bin/gh")
                .firstOrNull { File(it).exists() }
        }
    }

    private fun getJiraTicketUrl(dir: File): String? {
        val branch = runGit(dir, "rev-parse", "--abbrev-ref", "HEAD")
        val ticketId = Regex("[A-Z]+-\\d+").find(branch)?.value ?: return null
        return "https://pozitim.atlassian.net/browse/$ticketId"
    }

    private fun runGit(dir: File, vararg args: String): String {
        return runProcess(dir, "git", *args)
    }

    private fun runProcess(dir: File, vararg cmd: String): String {
        return try {
            val process = ProcessBuilder(*cmd)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(30, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("RunePlugin")
                .createNotification(message, type)
                .notify(project)
        }
    }
}
