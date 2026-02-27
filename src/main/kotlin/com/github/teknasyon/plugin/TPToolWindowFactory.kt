package com.github.teknasyon.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.service.SettingsService
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

    private val settings = SettingsService.getInstance()

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
        var isExpanded by remember { mutableStateOf(settings.state.isActionsExpanded) }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(TPTheme.colors.black)
        ) {
            Card(
                modifier = Modifier
                    .width(if (isExpanded) 124.dp else 44.dp)
                    .fillMaxHeight(),
                backgroundColor = TPTheme.colors.gray,
                elevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isExpanded) {
                                TPText(
                                    text = "Actions",
                                    color = TPTheme.colors.white,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardDoubleArrowLeft,
                                    contentDescription = null,
                                    tint = TPTheme.colors.white,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            isExpanded = !isExpanded
                                            settings.loadState(settings.state.copy(isActionsExpanded = isExpanded))
                                        }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                                    contentDescription = null,
                                    tint = TPTheme.colors.white,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            isExpanded = !isExpanded
                                            settings.loadState(settings.state.copy(isActionsExpanded = isExpanded))
                                        }
                                )
                            }
                        }

                        SidebarButton(
                            title = "Jungle",
                            icon = Icons.Rounded.Language,
                            isSelected = selectedSection == "jungle",
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "jungle" }
                        )

                        SidebarButton(
                            title = "Module",
                            icon = Icons.Rounded.ViewModule,
                            isSelected = selectedSection == "module",
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "module" }
                        )

                        SidebarButton(
                            title = "Settings",
                            icon = Icons.Rounded.Settings,
                            isSelected = selectedSection == "settings",
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "settings" }
                        )

                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedSection) {
                    "module" -> ModuleGeneratorContent(project)
                    "jungle" -> JungleContent()
                    "settings" -> SettingsContent(project)
                }
            }
        }
    }

    @Composable
    private fun SidebarButton(
        title: String,
        icon: ImageVector,
        isSelected: Boolean,
        isExpanded: Boolean,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isSelected) TPTheme.colors.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TPTheme.colors.blue else TPTheme.colors.lightGray,
                modifier = Modifier.size(18.dp)
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.size(4.dp))
                TPText(
                    text = title,
                    color = if (isSelected) TPTheme.colors.white else TPTheme.colors.lightGray,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}