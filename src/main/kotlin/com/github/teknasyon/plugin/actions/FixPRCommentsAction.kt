package com.github.teknasyon.plugin.actions

import com.github.teknasyon.plugin.actions.dialog.FixPRCommentsDialog
import com.github.teknasyon.plugin.common.CliUtils
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.common.VcsProvider
import com.github.teknasyon.plugin.common.VcsProviderDetector
import com.github.teknasyon.plugin.service.BitbucketCloudPlatformService
import com.github.teknasyon.plugin.service.BitbucketCredentialService
import com.github.teknasyon.plugin.service.GitHubApiClient
import com.github.teknasyon.plugin.service.GitHubCredentialService
import com.github.teknasyon.plugin.service.GitHubPlatformService
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.github.teknasyon.plugin.service.VcsPlatformService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.io.File

class FixPRCommentsAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project?.basePath != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dir = File(project.basePath ?: return)

        val settings = PluginSettingsService.getInstance(project)

        val remoteUrl = CliUtils.runGit(dir, "remote", "get-url", "origin").trim()
        if (remoteUrl.isBlank()) {
            notify(project, "Could not get remote URL. Ensure 'origin' remote is set.")
            return
        }

        val provider = settings.getVcsProvider()
        val remoteInfo = VcsProviderDetector.parseRemote(remoteUrl)
        if (remoteInfo == null) {
            notify(project, "Could not parse owner/repo from git remote.")
            return
        }

        val platformService: VcsPlatformService = when (provider) {
            VcsProvider.GITHUB -> {
                if (!GitHubCredentialService.hasCredentials()) {
                    notify(project, Constants.GITHUB_TOKEN_MISSING_MESSAGE)
                    return
                }
                val token = GitHubCredentialService.getToken()!!
                GitHubPlatformService(GitHubApiClient(token), remoteInfo.ownerOrProject, remoteInfo.repo)
            }
            VcsProvider.BITBUCKET_CLOUD -> {
                if (!BitbucketCredentialService.hasCredentials()) {
                    notify(project, Constants.BITBUCKET_CREDENTIALS_MISSING_MESSAGE)
                    return
                }
                BitbucketCloudPlatformService(remoteInfo.ownerOrProject, remoteInfo.repo)
            }
        }

        ApplicationManager.getApplication().invokeLater {
            val dialog = FixPRCommentsDialog(project, platformService)
            dialog.show()
        }
    }

    private fun notify(project: Project, message: String) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TeknasyonIntelliJPlugin")
                .createNotification(message, NotificationType.ERROR)
                .notify(project)
        }
    }
}
