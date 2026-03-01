package com.github.cnrture.rune.toolwindow.claude

import androidx.compose.ui.awt.ComposePanel
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.theme.RTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ClaudeToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (System.getProperty("skiko.renderApi") == null) {
            System.setProperty("skiko.renderApi", "SOFTWARE")
        }

        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(
                createComponent(project),
                Constants.EMPTY,
                false,
            )
        )
    }

    private fun createComponent(project: Project): JComponent {
        val panel = JPanel(BorderLayout())
        ComposePanel().apply {
            setContent {
                RTheme {
                    ClaudeTerminalContent(project = project)
                }
            }
            panel.add(this)
        }
        return panel
    }
}
