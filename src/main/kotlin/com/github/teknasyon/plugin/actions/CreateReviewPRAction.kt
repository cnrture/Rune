package com.github.teknasyon.plugin.actions

import com.github.teknasyon.plugin.actions.dialog.CreatePRDialog
import com.github.teknasyon.plugin.common.CliUtils
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.common.VcsProvider
import com.github.teknasyon.plugin.common.VcsProviderDetector
import com.github.teknasyon.plugin.service.BitbucketCloudPlatformService
import com.github.teknasyon.plugin.service.BitbucketCredentialService
import com.github.teknasyon.plugin.service.GitHubPlatformService
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.github.teknasyon.plugin.service.VcsPlatformService
import com.github.teknasyon.plugin.service.VcsUser
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
                val settings = PluginSettingsService.getInstance(project)
                val useReviewBranch = settings.isUseReviewBranch()

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

                // 6. Parse remote URL
                val remoteUrl = runGit(dir, "remote", "get-url", "origin").trim()
                if (remoteUrl.isBlank()) {
                    notify(project, "Could not get remote URL. Ensure 'origin' remote is set.", NotificationType.ERROR)
                    return
                }

                val provider = settings.getVcsProvider()
                val remoteInfo = VcsProviderDetector.parseRemote(remoteUrl)
                if (remoteInfo == null) {
                    notify(project, "Could not parse owner/repo from git remote.", NotificationType.ERROR)
                    return
                }

                // 7. Create platform service
                val platformService = createPlatformService(provider, remoteInfo, dir)
                if (platformService == null) return

                // 8. Extract Jira ticket ID from branch name
                val ticketId = Constants.JIRA_TICKET_REGEX.find(currentBranch)?.value

                // 9. Open dialog on EDT for reviewer/label selection
                ApplicationManager.getApplication().invokeLater {
                    val dialog = CreatePRDialog(
                        platformService = platformService,
                        owner = remoteInfo.ownerOrProject,
                        repo = remoteInfo.repo,
                        ticketId = ticketId,
                        onConfirm = { reviewers, labels ->
                            createPR(
                                project = project,
                                platformService = platformService,
                                currentBranch = currentBranch,
                                targetBranch = targetBranch,
                                jiraUrl = getJiraTicketUrl(dir),
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

    private fun createPlatformService(
        provider: VcsProvider,
        remoteInfo: com.github.teknasyon.plugin.common.RemoteInfo,
        dir: File,
    ): VcsPlatformService? {
        return when (provider) {
            VcsProvider.GITHUB -> {
                val ghPath = CliUtils.findGhCli()
                if (ghPath == null) return null
                GitHubPlatformService(ghPath, dir, remoteInfo.ownerOrProject, remoteInfo.repo)
            }
            VcsProvider.BITBUCKET_CLOUD -> {
                if (!BitbucketCredentialService.hasCredentials()) return null
                BitbucketCloudPlatformService(remoteInfo.ownerOrProject, remoteInfo.repo)
            }
        }
    }

    private fun createPR(
        project: Project,
        platformService: VcsPlatformService,
        currentBranch: String,
        targetBranch: String,
        jiraUrl: String?,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating PR…", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Creating PR $currentBranch → $targetBranch…"

                val result = platformService.createPullRequest(
                    currentBranch = currentBranch,
                    targetBranch = targetBranch,
                    title = currentBranch,
                    body = jiraUrl ?: "",
                    reviewers = reviewers,
                    labels = labels,
                )

                if (result.url != null) {
                    ApplicationManager.getApplication().invokeLater { BrowserUtil.browse(result.url) }
                    val message = if (result.errorMessage != null) {
                        "${result.errorMessage}: ${result.url}"
                    } else {
                        "PR created: ${result.url}"
                    }
                    notify(project, message, NotificationType.INFORMATION)
                } else {
                    notify(
                        project,
                        result.errorMessage ?: "PR creation failed.",
                        NotificationType.WARNING,
                    )
                }
            }
        })
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

    private fun getJiraTicketUrl(dir: File): String? {
        val branch = CliUtils.runGit(dir, "rev-parse", "--abbrev-ref", "HEAD")
        val ticketId = Constants.JIRA_TICKET_REGEX.find(branch)?.value ?: return null
        return Constants.jiraBrowseUrl(ticketId)
    }

    private fun runGit(dir: File, vararg args: String): String = CliUtils.runGit(dir, *args)

    private fun notify(project: Project, message: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TeknasyonIntelliJPlugin")
                .createNotification(message, type)
                .notify(project)
        }
    }
}
