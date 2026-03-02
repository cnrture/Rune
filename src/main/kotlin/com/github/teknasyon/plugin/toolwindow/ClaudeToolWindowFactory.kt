package com.github.teknasyon.plugin.toolwindow

import androidx.compose.ui.awt.ComposePanel
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.common.SkikoHelper
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class ClaudeToolWindowFactory : ToolWindowFactory {

    companion object {
        private val LOG = Logger.getInstance(ClaudeToolWindowFactory::class.java)

        init {
            if (System.getProperty("skiko.renderApi") == null) {
                System.setProperty("skiko.renderApi", "SOFTWARE")
            }
            SkikoHelper.ensureNativeLibrary()
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        try {
            toolWindow.contentManager.addContent(
                ContentFactory.getInstance().createContent(
                    createComponent(project),
                    Constants.EMPTY,
                    false,
                )
            )
        } catch (e: Throwable) {
            // Catches errors from ComposePanel.addNotify() triggered during addContent
            LOG.error("Failed to initialize tool window content", e)
            toolWindow.contentManager.addContent(
                ContentFactory.getInstance().createContent(
                    createFallbackPanel(e),
                    Constants.EMPTY,
                    false,
                )
            )
        }
    }

    private fun createComponent(project: Project): JComponent {
        val panel = JPanel(BorderLayout())
        try {
            ComposePanel().apply {
                setContent {
                    TPTheme {
                        ClaudeTerminalContent(project = project)
                    }
                }
                panel.add(this)
            }
        } catch (e: Throwable) {
            LOG.error("Failed to initialize Compose panel", e)
            return createFallbackPanel(e)
        }
        return panel
    }

    private fun createFallbackPanel(error: Throwable): JComponent {
        return JPanel(BorderLayout()).apply {
            add(
                JLabel(
                    "<html><center>Compose UI could not be initialized.<br>" +
                        "Try restarting the IDE or check plugin compatibility.<br><br>" +
                        "Error: ${error.message}</center></html>",
                    SwingConstants.CENTER,
                ),
                BorderLayout.CENTER,
            )
        }
    }
}
