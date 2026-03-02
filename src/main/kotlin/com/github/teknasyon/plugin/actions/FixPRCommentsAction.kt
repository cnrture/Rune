package com.github.teknasyon.plugin.actions

import com.github.teknasyon.plugin.actions.dialog.FixPRCommentsDialog
import com.github.teknasyon.plugin.common.CliUtils
import com.github.teknasyon.plugin.common.Constants
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

        val ghPath = findGhCli()
        if (ghPath == null) {
            notify(project)
            return
        }

        ApplicationManager.getApplication().invokeLater {
            val dialog = FixPRCommentsDialog(project, ghPath, dir)
            dialog.show()
        }
    }

    private fun findGhCli(): String? = CliUtils.findGhCli()

    private fun notify(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TeknasyonIntelliJPlugin")
                .createNotification(
                    Constants.GH_CLI_NOT_FOUND_MESSAGE,
                    NotificationType.ERROR,
                )
                .notify(project)
        }
    }
}
