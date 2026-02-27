package com.github.teknasyon.plugin.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

interface TerminalExecutor {
    fun executeCommand(project: Project, command: String)
}

@Suppress("DEPRECATION")
class TerminalExecutorImpl : TerminalExecutor {

    override fun executeCommand(project: Project, command: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val manager = TerminalToolWindowManager.getInstance(project)
                val widget = manager.createLocalShellWidget(
                    project.basePath ?: "",
                    "TeknasyonAndroidStudioPlugin"
                )
                widget.executeCommand(command)
            } catch (_: Exception) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(command)
                clipboard.setContents(selection, selection)
            }
        }
    }
}
