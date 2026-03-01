package com.github.cnrture.rune.actions

import com.github.cnrture.rune.actions.dialog.FixPRCommentsDialog
import com.github.cnrture.rune.service.CliDiscoveryService
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

        val ghPath = CliDiscoveryService.findGhCli()
        if (ghPath == null) {
            notify(project)
            return
        }

        ApplicationManager.getApplication().invokeLater {
            val dialog = FixPRCommentsDialog(project, ghPath, dir)
            dialog.show()
        }
    }

    private fun notify(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("RunePlugin")
                .createNotification(
                    "GitHub CLI (gh) not found. Install from https://cli.github.com and run 'gh auth login'.",
                    NotificationType.ERROR,
                )
                .notify(project)
        }
    }
}
