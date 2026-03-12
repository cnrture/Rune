package com.github.teknasyon.plugin.toolwindow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.github.teknasyon.plugin.actions.dialog.CreateSkillDialog
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.data.SkillRepositoryImpl
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.service.FileScanner
import com.github.teknasyon.plugin.service.PluginConfigurable
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

internal enum class ActivePanel { NONE, SKILLS, COMMANDS }

@Composable
fun ClaudeTerminalContent(project: Project) {
    val service = remember { ClaudeSessionService.getInstance(project) }
    val state by service.state.collectAsState()
    val settingsService = remember { PluginSettingsService.getInstance(project) }
    val scanSkillsUseCase = remember {
        val fileScanner = FileScanner(project)
        val repository = SkillRepositoryImpl(fileScanner)
        ScanSkillsUseCase(repository)
    }

    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }
    var slashTriggered by remember { mutableStateOf(false) }
    var showRCDialog by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        service.checkClaudeInstalled()
    }

    fun sendToTerminal(cmd: String, autoRun: Boolean) {
        service.sendToTerminal(cmd, autoRun)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TPTheme.colors.black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TPTheme.colors.black)
        ) {
            when (state.claudeInstalled) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TPText(
                            text = "Checking Claude CLI...",
                            color = TPTheme.colors.lightGray
                        )
                    }
                }

                false -> {
                    ClaudeInstallGuide(
                        onRetry = { service.retryClaudeCheck() }
                    )
                }

                true -> {
                    LaunchedEffect(Unit) {
                        service.ensureSession()
                        service.loadCachedModel()
                    }

                    if (state.sessions.isNotEmpty()) {
                        SessionTabBar(
                            sessions = state.sessions,
                            activeSessionId = state.activeSessionId,
                            onSelectSession = { service.switchToSession(it) },
                            onCloseSession = { service.closeSession(it) },
                            onAddSession = { service.addNewSession() },
                            onCreateSkillClick = { CreateSkillDialog(project).show() },
                            onUsageClick = { sendToTerminal("/usage", true) },
                            onSettingsClick = {
                                ShowSettingsUtil.getInstance()
                                    .editConfigurable(project, PluginConfigurable(project))
                            },
                        )

                        if (activePanel == ActivePanel.NONE && !showRCDialog && previewImagePath == null && !showModelPicker) {
                            SwingPanel(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 1.dp),
                                factory = { service.sessionManager.parentPanel },
                                update = {},
                            )
                        } else if (activePanel != ActivePanel.NONE) {
                            SwingPanel(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 1.dp),
                                factory = { service.sessionManager.parentPanel },
                                update = {},
                            )
                        } else if (activePanel == ActivePanel.NONE) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(TPTheme.colors.black),
                            )
                        }

                        // Inline command panel with slide animation
                        AnimatedVisibility(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.6f),
                            visible = activePanel != ActivePanel.NONE,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                        ) {
                            val initialFilter = when (activePanel) {
                                ActivePanel.SKILLS -> PaletteFilter.SKILLS
                                ActivePanel.COMMANDS -> PaletteFilter.COMMANDS
                                else -> PaletteFilter.ALL
                            }
                            InlineCommandPanel(
                                project = project,
                                scanSkillsUseCase = scanSkillsUseCase,
                                settingsService = settingsService,
                                superClaudeInstalled = state.superClaudeInstalled == true,
                                initialFilter = initialFilter,
                                onDismiss = {
                                    activePanel = ActivePanel.NONE
                                    inputFocusRequester.requestFocus()
                                },
                                onItemSelected = { item ->
                                    activePanel = ActivePanel.NONE
                                    slashTriggered = false
                                    service.setPendingInput(item.terminalText + "\n")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }

                        val pendingInput by service.pendingInput.collectAsState()
                        var selectedImagePaths by remember { mutableStateOf<List<String>>(emptyList()) }

                        TerminalInputBar(
                            onSend = { text -> sendToTerminal(text, true) },
                            onInjectFile = {
                                val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
                                    ?: return@TerminalInputBar null
                                file.path
                                    .removePrefix(project.basePath ?: "")
                                    .removePrefix("/")
                            },
                            selectedImagePaths = selectedImagePaths,
                            onPickImage = {
                                val descriptor = FileChooserDescriptor(true, false, false, false, false, true)
                                    .apply {
                                        title = "Select Images"
                                        withFileFilter { file ->
                                            file.extension?.lowercase() in listOf(
                                                "png",
                                                "jpg",
                                                "jpeg",
                                                "gif",
                                                "webp",
                                                "bmp"
                                            )
                                        }
                                    }
                                FileChooser.chooseFiles(descriptor, project, null) { files ->
                                    val newPaths = files.map { it.path }
                                    selectedImagePaths = selectedImagePaths + newPaths
                                }
                            },
                            onRemoveImage = { path -> selectedImagePaths = selectedImagePaths - path },
                            onClearImages = { selectedImagePaths = emptyList() },
                            pendingInput = pendingInput,
                            onPendingInputConsumed = { service.consumePendingInput() },
                            onChangeModelClick = { showModelPicker = true },
                            activeModel = state.activeModel,
                            modelLoading = state.modelLoading,
                            onSkillsClick = {
                                activePanel =
                                    if (activePanel == ActivePanel.SKILLS) ActivePanel.NONE else ActivePanel.SKILLS
                            },
                            onCommandsClick = {
                                activePanel =
                                    if (activePanel == ActivePanel.COMMANDS) ActivePanel.NONE else ActivePanel.COMMANDS
                            },
                            onSlashTyped = {
                                slashTriggered = true
                                activePanel = ActivePanel.COMMANDS
                            },
                            shouldClearSlash = slashTriggered && activePanel == ActivePanel.NONE,
                            onClearSlash = { slashTriggered = false },
                            isRemoteControlActive = state.remoteControlActive,
                            onRemoteControlStart = { showRCDialog = true },
                            onRemoteControlStop = { service.stopRemoteControl() },
                            onClickPreviewImage = { previewImagePath = it },
                            inputFocusRequester = inputFocusRequester,
                        )
                    }
                }
            }
        }

        if (showModelPicker) {
            ModelPickerDialog(
                currentModelId = state.activeModel,
                onSelect = { model -> service.selectModel(model) },
                onDismiss = { showModelPicker = false },
            )
        }

        if (showRCDialog) {
            RemoteControlDialog(
                onDismiss = { showRCDialog = false },
                onConfirm = { preventSleep ->
                    showRCDialog = false
                    service.startRemoteControl(preventSleep)
                },
            )
        }

        if (previewImagePath != null) {
            ImagePreviewDialog(
                imagePath = previewImagePath!!,
                onDismiss = { previewImagePath = null },
            )
        }
    }
}
