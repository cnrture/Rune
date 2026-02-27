package com.github.teknasyon.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.components.TPTabRow
import com.github.teknasyon.plugin.theme.TPTheme
import com.github.teknasyon.plugin.toolwindow.manager.jungle.JungleContent
import com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.ModuleGeneratorContent
import com.github.teknasyon.plugin.toolwindow.manager.settings.SettingsContent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class TPToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (System.getProperty("skiko.renderApi") == null) {
            System.setProperty("skiko.renderApi", "SOFTWARE")
        }

        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(
                createToolWindowComponent(project),
                Constants.EMPTY,
                false,
            )
        )
    }

    private fun createToolWindowComponent(project: Project): JComponent {
        val panel = JPanel(BorderLayout())
        ComposePanel().apply {
            setContent {
                TPTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TPTheme.colors.gray),
                    ) {
                        MainContent(project)
                    }
                }
            }
            panel.add(this)
        }
        return panel
    }

    @Composable
    private fun MainContent(project: Project) {
        var selectedSection by remember { mutableStateOf("jungle") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TPTheme.colors.black)
        ) {
            // Top tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TPTheme.colors.black)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TPTabRow(
                    text = "Jungle",
                    isSelected = selectedSection == "jungle",
                    onTabSelected = { selectedSection = "jungle" },
                )
                TPTabRow(
                    text = "Module",
                    isSelected = selectedSection == "module",
                    onTabSelected = { selectedSection = "module" },
                )
                Spacer(modifier = Modifier.weight(1f))
                // Settings icon
                Box(
                    modifier = Modifier
                        .background(
                            color = if (selectedSection == "settings") TPTheme.colors.primaryContainer else TPTheme.colors.gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSection = "settings" }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Ayarlar",
                        tint = if (selectedSection == "settings") TPTheme.colors.white else TPTheme.colors.lightGray,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Content area — full width
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedSection) {
                    "module" -> ModuleGeneratorContent(project)
                    "jungle" -> JungleContent()
                    "settings" -> SettingsContent(project)
                }
            }
        }
    }
}
