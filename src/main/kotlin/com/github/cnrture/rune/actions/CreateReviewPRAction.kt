package com.github.cnrture.rune.actions

import com.github.cnrture.rune.actions.dialog.CreatePRDialog
import com.github.cnrture.rune.common.CliUtils
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.common.VcsProvider
import com.github.cnrture.rune.common.VcsProviderDetector
import com.github.cnrture.rune.service.BitbucketCloudPlatformService
import com.github.cnrture.rune.service.BitbucketCredentialService
import com.github.cnrture.rune.service.GitHubApiClient
import com.github.cnrture.rune.service.GitHubCredentialService
import com.github.cnrture.rune.service.GitHubPlatformService
import com.github.cnrture.rune.service.PluginSettingsService
import com.github.cnrture.rune.service.VcsPlatformService
import com.github.cnrture.rune.service.VcsUser
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

                // 2. Parse remote URL
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

                // 3. Create platform service
                val platformService = createPlatformService(project, provider, remoteInfo)
                if (platformService == null) return

                // 4. Read local branches for base branch selection
                indicator.text = "Reading branches…"
                val localBranches = getLocalBranches(dir).filter { it != currentBranch }

                // 5. Detect suggested base branch via merge-base
                val suggestedBase = detectBaseBranch(dir, localBranches)

                // 6. Push current branch
                indicator.text = "Pushing $currentBranch…"
                runGit(dir, "push", "-u", "origin", currentBranch)

                // 7. Extract Jira ticket ID from branch name
                val ticketId = Constants.JIRA_TICKET_REGEX.find(currentBranch)?.value?.uppercase()

                // 8. Open dialog on EDT for reviewer/label/base branch selection
                ApplicationManager.getApplication().invokeLater {
                    val dialog = CreatePRDialog(
                        platformService = platformService,
                        owner = remoteInfo.ownerOrProject,
                        repo = remoteInfo.repo,
                        ticketId = ticketId,
                        remoteBranches = localBranches,
                        suggestedBaseBranch = suggestedBase,
                        useReviewBranch = useReviewBranch,
                        jiraBaseUrl = settings.getJiraBaseUrl(),
                        onConfirm = { reviewers, labels, baseBranch ->
                            val targetBranch = if (useReviewBranch) {
                                createReviewBranch(project, dir, currentBranch, baseBranch) ?: return@CreatePRDialog
                            } else {
                                baseBranch
                            }
                            createPR(
                                project = project,
                                platformService = platformService,
                                currentBranch = currentBranch,
                                targetBranch = targetBranch,
                                jiraUrl = getJiraTicketUrl(project, dir),
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

    private fun createReviewBranch(project: Project, dir: File, currentBranch: String, baseBranch: String): String? {
        val reviewBranch = "review/$currentBranch"
        val reviewExists = runGit(dir, "ls-remote", "--heads", "origin", reviewBranch).isNotBlank()
        if (!reviewExists) {
            val result = runGit(dir, "push", "origin", "origin/$baseBranch:refs/heads/$reviewBranch")
            if (result.contains("error") || result.contains("fatal")) {
                notify(project, "Failed to create $reviewBranch: $result", NotificationType.ERROR)
                return null
            }
        }
        return reviewBranch
    }

    private fun createPlatformService(
        project: Project,
        provider: VcsProvider,
        remoteInfo: com.github.cnrture.rune.common.RemoteInfo,
    ): VcsPlatformService? {
        return when (provider) {
            VcsProvider.GITHUB -> {
                if (!GitHubCredentialService.hasCredentials()) {
                    notify(project, Constants.GITHUB_TOKEN_MISSING_MESSAGE, NotificationType.ERROR)
                    return null
                }
                val token = GitHubCredentialService.getToken()!!
                GitHubPlatformService(GitHubApiClient(token), remoteInfo.ownerOrProject, remoteInfo.repo)
            }
            VcsProvider.BITBUCKET_CLOUD -> {
                if (!BitbucketCredentialService.hasCredentials()) {
                    notify(project, Constants.BITBUCKET_CREDENTIALS_MISSING_MESSAGE, NotificationType.ERROR)
                    return null
                }
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
     * Detects the base branch this branch was likely created from.
     * Uses merge-base distance against local branches to find the closest ancestor.
     */
    private fun detectBaseBranch(
        dir: File,
        localBranches: List<String>,
    ): String {
        val priorityBranches = listOf("develop", "main", "master", "staging", "release")
        val candidates = (priorityBranches.filter { it in localBranches } +
            localBranches.filter { it !in priorityBranches }).distinct()

        if (candidates.isEmpty()) return "main"

        val closest = candidates.minByOrNull { candidate ->
            val mergeBase = runGit(dir, "merge-base", "HEAD", candidate).trim()
            if (mergeBase.isBlank()) Int.MAX_VALUE
            else runGit(dir, "rev-list", "--count", "$mergeBase..HEAD").trim().toIntOrNull() ?: Int.MAX_VALUE
        }

        return closest ?: "main"
    }

    private fun getLocalBranches(dir: File): List<String> {
        return runGit(dir, "branch", "--format=%(refname:short)")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun getJiraTicketUrl(project: Project, dir: File): String? {
        val branch = CliUtils.runGit(dir, "rev-parse", "--abbrev-ref", "HEAD")
        val ticketId = Constants.JIRA_TICKET_REGEX.find(branch)?.value?.uppercase() ?: return null
        return PluginSettingsService.getInstance(project).jiraBrowseUrl(ticketId)
    }

    private fun runGit(dir: File, vararg args: String): String = CliUtils.runGit(dir, *args)

    private fun notify(project: Project, message: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("RunePlugin")
                .createNotification(message, type)
                .notify(project)
        }
    }
}
