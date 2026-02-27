package com.github.teknasyon.plugin.actions

import com.github.teknasyon.plugin.actions.dialog.FixPRCommentsDialog
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

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

    private fun notify(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TeknasyonAndroidStudioPlugin")
                .createNotification(
                    "GitHub CLI (gh) not found. Install from https://cli.github.com and run 'gh auth login'.",
                    NotificationType.ERROR,
                )
                .notify(project)
        }
    }
}
