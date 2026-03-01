package com.github.cnrture.rune.actions

import com.github.cnrture.rune.actions.dialog.CreatePRDialog
import com.github.cnrture.rune.common.ProcessRunner
import com.github.cnrture.rune.service.CliDiscoveryService
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

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Preparing PR…", false) {
            override fun run(indicator: ProgressIndicator) {
                val dir = File(project.basePath ?: return)

                // 1. Current branch
                val currentBranch = ProcessRunner.git(dir, "rev-parse", "--abbrev-ref", "HEAD")

                // 2. Detect base branch automatically
                indicator.text = "Detecting base branch…"
                val baseBranch = detectBaseBranch(dir, currentBranch)

                // 3. Fetch remote refs
                indicator.text = "Fetching remote…"
                ProcessRunner.git(dir, "fetch", "origin")

                // 4. Push current branch
                indicator.text = "Pushing $currentBranch…"
                ProcessRunner.git(dir, "push", "-u", "origin", currentBranch)

                // 5. Find gh CLI
                val ghPath = CliDiscoveryService.findGhCli()
                if (ghPath == null) {
                    notify(
                        project,
                        "GitHub CLI (gh) not found. Install from https://cli.github.com and run 'gh auth login'.",
                        NotificationType.ERROR
                    )
                    return
                }

                // 6. Parse owner/repo
                val ownerRepo = parseOwnerRepo(dir)
                if (ownerRepo == null) {
                    notify(
                        project,
                        "Could not parse owner/repo from git remote. Ensure 'origin' remote is set.",
                        NotificationType.ERROR,
                    )
                    return
                }

                // 7. Open dialog on EDT for base branch, reviewer/label selection
                ApplicationManager.getApplication().invokeLater {
                    val dialog = CreatePRDialog(
                        ghPath = ghPath,
                        dir = dir,
                        owner = ownerRepo.first,
                        repo = ownerRepo.second,
                        currentBranch = currentBranch,
                        detectedBaseBranch = baseBranch,
                        onConfirm = { selectedBaseBranch, reviewers, labels ->
                            createPR(
                                project = project,
                                dir = dir,
                                ghPath = ghPath,
                                currentBranch = currentBranch,
                                baseBranch = selectedBaseBranch,
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
        baseBranch: String,
        reviewers: List<String>,
        labels: List<String>,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating PR…", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Creating PR $currentBranch → $baseBranch…"

                val cmd = mutableListOf(
                    ghPath,
                    "pr", "create",
                    "--assignee", "@me",
                    "--base", baseBranch,
                    "--head", currentBranch,
                    "--title", currentBranch,
                    "--body", "",
                )

                if (reviewers.isNotEmpty()) {
                    cmd += "--reviewer"
                    cmd += reviewers.joinToString(",")
                }
                if (labels.isNotEmpty()) {
                    cmd += "--label"
                    cmd += labels.joinToString(",")
                }

                val prOutput = ProcessRunner.run(dir, *cmd.toTypedArray())

                val prUrl = prOutput.lines().firstOrNull { it.startsWith("https://github.com") }
                if (prUrl != null) {
                    ApplicationManager.getApplication().invokeLater { BrowserUtil.browse(prUrl) }
                    notify(project, "PR created: $prUrl", NotificationType.INFORMATION)
                } else {
                    val existingUrl = ProcessRunner.run(dir, ghPath, "pr", "view", "--json", "url", "--jq", ".url")
                        .takeIf { it.startsWith("https://") }
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
     * Supports both HTTPS and SSH formats.
     */
    private fun parseOwnerRepo(dir: File): Pair<String, String>? {
        val remoteUrl = ProcessRunner.git(dir, "remote", "get-url", "origin")
        if (remoteUrl.isBlank()) return null

        val sshMatch = Regex("""git@[^:]+:([^/]+)/(.+?)(?:\.git)?$""").find(remoteUrl)
        if (sshMatch != null) return sshMatch.groupValues[1] to sshMatch.groupValues[2]

        val httpsMatch = Regex("""https?://[^/]+/([^/]+)/(.+?)(?:\.git)?$""").find(remoteUrl)
        if (httpsMatch != null) return httpsMatch.groupValues[1] to httpsMatch.groupValues[2]

        return null
    }

    /**
     * Detects the base branch this branch was created from.
     * Method 1: reflog — looks for "Created from" in the oldest reflog entry.
     * Method 2: merge-base — compares with common branches and picks the closest one.
     */
    private fun detectBaseBranch(dir: File, currentBranch: String): String {
        val reflogLines = ProcessRunner.git(dir, "reflog", "show", "--format=%gs", currentBranch).lines()
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

        val candidates = listOf("main", "master", "develop", "staging", "release")
        return candidates.minByOrNull { candidate ->
            val mergeBase = ProcessRunner.git(dir, "merge-base", "HEAD", "origin/$candidate")
            if (mergeBase.isBlank()) Int.MAX_VALUE
            else ProcessRunner.git(dir, "rev-list", "--count", "$mergeBase..HEAD").toIntOrNull() ?: Int.MAX_VALUE
        } ?: "main"
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
