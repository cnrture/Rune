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
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.common.Utils
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.data.SettingsState
import com.github.teknasyon.plugin.data.repository.SkillRepositoryImpl
import com.github.teknasyon.plugin.domain.usecase.ExecuteSkillUseCase
import com.github.teknasyon.plugin.domain.usecase.ProcessReviewCommentsUseCase
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.domain.usecase.ToggleFavoriteUseCase
import com.github.teknasyon.plugin.service.FileScanner
import com.github.teknasyon.plugin.service.SettingsService
import com.github.teknasyon.plugin.service.SkillDockSettingsService
import com.github.teknasyon.plugin.service.TerminalExecutorImpl
import com.github.teknasyon.plugin.theme.TPTheme
import com.github.teknasyon.plugin.toolwindow.ai.SkillDockViewModel
import com.github.teknasyon.plugin.toolwindow.manager.ai.AiContent
import com.github.teknasyon.plugin.toolwindow.manager.featuregenerator.FeatureGeneratorContent
import com.github.teknasyon.plugin.toolwindow.manager.jungle.JungleContent
import com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.ModuleGeneratorContent
import com.github.teknasyon.plugin.toolwindow.manager.settings.SettingsContent
import com.github.teknasyon.plugin.toolwindow.manager.settings.dialog.ExportSettingsContent
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TPTheme.colors.gray),
                    ) {
                        TPText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            text = "Teknasyon DevTools",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        TPTheme.colors.blue,
                                        TPTheme.colors.purple,
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
        val viewModel = remember {
            createViewModel(project)
        }

        var selectedSection by remember { mutableStateOf("ai") }
        var isExpanded by remember { mutableStateOf(settings.state.isActionsExpanded) }
        var isExportDialogVisible by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(TPTheme.colors.black)
        ) {
            Card(
                modifier = Modifier
                    .width(if (isExpanded) 124.dp else 60.dp)
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
                            title = "AI Tools",
                            icon = Icons.Rounded.Memory,
                            isSelected = selectedSection == "ai",
                            color = TPTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "ai" }
                        )

                        SidebarButton(
                            title = "Jungle",
                            icon = Icons.Rounded.Language,
                            isSelected = selectedSection == "jungle",
                            color = TPTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "jungle" }
                        )

                        SidebarButton(
                            title = "Module",
                            icon = Icons.Rounded.ViewModule,
                            isSelected = selectedSection == "module",
                            color = TPTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "module" }
                        )

                        SidebarButton(
                            title = "Feature",
                            icon = Icons.Rounded.FileOpen,
                            isSelected = selectedSection == "feature",
                            color = TPTheme.colors.blue,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "feature" }
                        )

                        SidebarButton(
                            title = "Settings",
                            icon = Icons.Rounded.Settings,
                            isSelected = selectedSection == "settings",
                            color = TPTheme.colors.lightGray,
                            isExpanded = isExpanded,
                            onClick = { selectedSection = "settings" }
                        )

                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TPActionCard(
                            title = "Export Settings",
                            icon = Icons.Rounded.FileUpload,
                            type = TPActionCardType.SMALL,
                            actionColor = TPTheme.colors.blue,
                            isTextVisible = isExpanded,
                            onClick = { isExportDialogVisible = true }
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        TPActionCard(
                            title = "Import Settings",
                            icon = Icons.Rounded.FileDownload,
                            type = TPActionCardType.SMALL,
                            actionColor = TPTheme.colors.lightGray,
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
                    "jungle" -> JungleContent()
                    "settings" -> SettingsContent(
                        project = project,
                        viewModel = viewModel,
                    )

                    "ai" -> AiContent(
                        project = project,
                        viewModel = viewModel,
                        onShowSettingsClick = { selectedSection = "settings" },
                    )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isSelected) color.copy(alpha = 0.2f) else TPTheme.colors.black,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 6.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else TPTheme.colors.lightGray,
                modifier = Modifier.size(18.dp)
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.size(4.dp))
                TPText(
                    text = title,
                    color = if (isSelected) TPTheme.colors.white else TPTheme.colors.lightGray.copy(alpha = 0.4f),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
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

    private fun createViewModel(project: Project): SkillDockViewModel {
        val settingsService = SkillDockSettingsService.getInstance(project)
        val fileScanner = FileScanner(project)
        val terminalExecutor = TerminalExecutorImpl()
        val repository = SkillRepositoryImpl(fileScanner, settingsService)
        val scanSkillsUseCase = ScanSkillsUseCase(repository, settingsService)
        val executeSkillUseCase = ExecuteSkillUseCase(terminalExecutor)
        val toggleFavoriteUseCase = ToggleFavoriteUseCase(settingsService)
        val processReviewCommentsUseCase = ProcessReviewCommentsUseCase(project)
        return SkillDockViewModel(
            project = project,
            settingsService = settingsService,
            scanSkillsUseCase = scanSkillsUseCase,
            executeSkillUseCase = executeSkillUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            processReviewCommentsUseCase = processReviewCommentsUseCase,
            terminalExecutor = terminalExecutor,
        )
    }
}