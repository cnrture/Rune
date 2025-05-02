package com.github.teknasyon.getcontactplugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactplugin.components.GetcontactButton
import com.github.teknasyon.getcontactplugin.theme.WidgetTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent

class GetcontactToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createToolWindowComponent(project), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createToolWindowComponent(project: Project): JComponent {
        return ComposePanel().apply {
            setContent {
                WidgetTheme {
                    GetcontactToolWindowContent(project)
                }
            }
        }
    }

    @Composable
    private fun GetcontactToolWindowContent(project: Project) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                GetcontactButton(
                    modifier = Modifier.weight(1f),
                    text = "Create Module",
                    onClick = { ModuleMakerDialogWrapper(project, null).apply { showAndGet() } },
                )

                GetcontactButton(
                    modifier = Modifier.weight(1f),
                    text = "Create Feature",
                    onClick = { FeatureMakerDialogWrapper(project, null).apply { showAndGet() } }
                )
            }
        }
    }
}