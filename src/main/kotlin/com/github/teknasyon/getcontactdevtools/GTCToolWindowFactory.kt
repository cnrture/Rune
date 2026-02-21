package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.common.Utils
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.data.SettingsState
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.featuregenerator.FeatureGeneratorContent
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.formatter.FormatterContent
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.jungle.JungleContent
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.modulegenerator.ModuleGeneratorContent
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.newsletter.NewsletterContent
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.SettingsContent
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.dialog.ExportSettingsContent
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class GTCToolWindowFactory : ToolWindowFactory {

    private val settings = SettingsService.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
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
                GTCTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GTCTheme.colors.gray),
                    ) {
                        GTCText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            text = "GTC DevTools",
                            style = TextStyle(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        GTCTheme.colors.blue,
                                        GTCTheme.colors.purple,
                                    ),
                                    tileMode = TileMode.Mirror,
                                ),
                            ),
                        )
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
        var selectedSection by remember { mutableStateOf("module") }
        var isExpanded by remember { mutableStateOf(settings.state.isActionsExpanded) }
        var isExportDialogVisible by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(GTCTheme.colors.black)
        ) {
            Card(
                modifier = Modifier
                    .width(if (isExpanded) 180.dp else 60.dp)
                    .fillMaxHeight(),
                backgroundColor = GTCTheme.colors.gray,
                elevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isExpanded) 16.dp else 8.dp),
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
                                GTCText(
                                    text = "Actions",
                                    color = GTCTheme.colors.white,
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardDoubleArrowLeft,
                                    contentDescription = null,
                                    tint = GTCTheme.colors.white,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            isExpanded = !isExpanded
                                            settings.loadState(settings.state.copy(isActionsExpanded = isExpanded))
                                        }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                                    contentDescription = null,
                                    tint = GTCTheme.colors.white,
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
                            title = "Module",
                            icon = Icons.Rounded.ViewModule,
                            isSelected = selectedSection == "module",
                            color = GTCTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "module" }
                        )

                        SidebarButton(
                            title = "Feature",
                            icon = Icons.Rounded.FileOpen,
                            isSelected = selectedSection == "feature",
                            color = GTCTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "feature" }
                        )

                        SidebarButton(
                            title = "News",
                            icon = Icons.AutoMirrored.Rounded.Article,
                            isSelected = selectedSection == "news",
                            color = GTCTheme.colors.purple,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "news" }
                        )

                        SidebarButton(
                            title = "Jungle",
                            icon = Icons.Rounded.Language,
                            isSelected = selectedSection == "jungle",
                            color = GTCTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "jungle" }
                        )

                        SidebarButton(
                            title = "Picker",
                            icon = Icons.Rounded.ColorLens,
                            isSelected = selectedSection == "color",
                            color = GTCTheme.colors.purple,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "color" }
                        )

                        SidebarButton(
                            title = "Formatter",
                            icon = Icons.Rounded.FormatAlignCenter,
                            isSelected = selectedSection == "formatter",
                            color = GTCTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "formatter" }
                        )

                        SidebarButton(
                            title = "API Test",
                            icon = Icons.Rounded.Api,
                            isSelected = selectedSection == "api",
                            color = GTCTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "api" }
                        )

                        SidebarButton(
                            title = "Settings",
                            icon = Icons.Rounded.Settings,
                            isSelected = selectedSection == "settings",
                            color = GTCTheme.colors.lightGray,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "settings" }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GTCActionCard(
                            title = "Export Settings",
                            icon = Icons.Rounded.FileUpload,
                            type = GTCActionCardType.SMALL,
                            actionColor = GTCTheme.colors.blue,
                            isTextVisible = isExpanded,
                            onClick = { isExportDialogVisible = true }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GTCActionCard(
                            title = "Import Settings",
                            icon = Icons.Rounded.FileDownload,
                            type = GTCActionCardType.SMALL,
                            actionColor = GTCTheme.colors.lightGray,
                            isTextVisible = isExpanded,
                            onClick = {
                                importSettings(settings) { newSettings ->
                                    settings.loadState(newSettings)
                                }
                            }
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
                    "feature" -> FeatureGeneratorContent(project)
                    "formatter" -> FormatterContent()
                    "settings" -> SettingsContent(project)
                    "jungle" -> JungleContent()
                    "news" -> NewsletterContent()
                }
            }
        }

        if (isExportDialogVisible) {
            Dialog(
                onDismissRequest = { isExportDialogVisible = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = true
                )
            ) {
                ExportSettingsContent(
                    settings = settings,
                    onExport = { success, message ->
                        if (success) isExportDialogVisible = false
                        Utils.showInfo(
                            message = message,
                            type = if (success) NotificationType.INFORMATION else NotificationType.ERROR,
                        )
                    },
                    onCancel = { isExportDialogVisible = false }
                )
            }
        }
    }

    @Composable
    private fun SidebarButton(
        title: String,
        icon: ImageVector,
        isSelected: Boolean,
        color: Color,
        isExpanded: Boolean,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            backgroundColor = if (isSelected) color.copy(alpha = 0.2f) else GTCTheme.colors.black,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(if (isExpanded) 8.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) color else GTCTheme.colors.lightGray,
                    modifier = Modifier.size(20.dp)
                )
                if (isExpanded) {
                    GTCText(
                        modifier = Modifier.padding(8.dp),
                        text = title,
                        color = if (isSelected) GTCTheme.colors.white else GTCTheme.colors.lightGray,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }

    private fun importSettings(settings: SettingsService, onSuccess: (SettingsState) -> Unit) {
        val project = ProjectManager.getInstance().defaultProject
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")
        descriptor.title = "Import Settings"
        FileChooser.chooseFile(descriptor, project, null) { file ->
            if (settings.importFromFile(file.path)) {
                Utils.showInfo(
                    message = "Settings imported successfully!",
                    type = NotificationType.INFORMATION,
                )
                onSuccess(settings.state)
            } else {
                Utils.showInfo(
                    message = "Failed to import settings. Please check the file format.",
                    type = NotificationType.ERROR,
                )
            }
        }
    }
}