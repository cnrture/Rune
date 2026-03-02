package com.github.teknasyon.plugin.toolwindow.claude

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.github.teknasyon.plugin.actions.dialog.CreateSkillDialog
import com.github.teknasyon.plugin.components.*
import com.github.teknasyon.plugin.data.repository.SkillRepositoryImpl
import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.service.FileScanner
import com.github.teknasyon.plugin.service.PluginConfigurable
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.SwingUtilities

@Composable
fun ClaudeTerminalContent(project: Project) {
    val service = remember { ClaudeSessionService.getInstance(project) }
    val state by service.state.collectAsState()
    val settingsService = remember { PluginSettingsService.getInstance(project) }
    val scanSkillsUseCase = remember {
        val fileScanner = FileScanner(project)
        val repository = SkillRepositoryImpl(fileScanner, settingsService)
        ScanSkillsUseCase(repository)
    }

    var showCommandPalette by remember { mutableStateOf(false) }
    var showRCDialog by remember { mutableStateOf(false) }

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
                    }

                    if (state.sessions.isNotEmpty()) {
                        SessionTabBar(
                            sessions = state.sessions,
                            activeSessionId = state.activeSessionId,
                            onSelectSession = { service.switchToSession(it) },
                            onCloseSession = { service.closeSession(it) },
                            onAddSession = { service.addNewSession() },
                            onUsageClick = { sendToTerminal("/usage", true) },
                            onSettingsClick = {
                                ShowSettingsUtil.getInstance()
                                    .editConfigurable(project, PluginConfigurable(project))
                            },
                        )

                        if (showCommandPalette || showRCDialog) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(TPTheme.colors.black),
                            )
                        } else {
                            SwingPanel(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 1.dp),
                                factory = { service.sessionManager.parentPanel },
                                update = {},
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
                            onSlashClick = { showCommandPalette = !showCommandPalette },
                            onChangeModelClick = { sendToTerminal("/model", true) },
                            onCreateSkillClick = { CreateSkillDialog(project).show() },
                            isRemoteControlActive = state.remoteControlActive,
                            onRemoteControlStart = { showRCDialog = true },
                            onRemoteControlStop = { service.stopRemoteControl() },
                        )
                    }
                }
            }
        }

        if (showCommandPalette) {
            UnifiedCommandPalette(
                project = project,
                scanSkillsUseCase = scanSkillsUseCase,
                settingsService = settingsService,
                superClaudeInstalled = state.superClaudeInstalled == true,
                onDismiss = { showCommandPalette = false },
                onItemSelected = { item ->
                    showCommandPalette = false
                    sendToTerminal(item.terminalText, item.autoRun)
                },
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
    }
}

@Composable
private fun SessionTabBar(
    sessions: List<ClaudeSession>,
    activeSessionId: Int,
    onSelectSession: (Int) -> Unit,
    onCloseSession: (Int) -> Unit,
    onAddSession: () -> Unit,
    onUsageClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TPTheme.colors.black)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TPActionCard(
                title = "Usage",
                icon = Icons.Rounded.DataUsage,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.SMALL,
                onClick = { onUsageClick() },
            )
            TPActionCard(
                title = "Settings",
                icon = Icons.Rounded.Settings,
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.SMALL,
                onClick = { onSettingsClick() },
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sessions.forEach { session ->
                val isActive = session.id == activeSessionId
                Row(
                    modifier = Modifier
                        .background(
                            color = if (isActive) TPTheme.colors.gray else TPTheme.colors.black,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TPText(
                        text = session.title,
                        color = if (isActive) TPTheme.colors.white else TPTheme.colors.lightGray,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close session",
                        tint = if (isActive) TPTheme.colors.lightGray else TPTheme.colors.hintGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New session",
                tint = TPTheme.colors.lightGray,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onAddSession() }
            )
        }
    }
}

@Composable
private fun ClaudeInstallGuide(onRetry: () -> Unit) {
    val installCommand = "npm install -g @anthropic-ai/claude-code"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Terminal,
            contentDescription = null,
            tint = TPTheme.colors.purple,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        TPText(
            text = "Claude CLI Not Found",
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.size(8.dp))
        TPText(
            text = "Claude CLI is not installed. Run the following command to install:",
            color = TPTheme.colors.lightGray,
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.size(16.dp))

        Row(
            modifier = Modifier
                .background(
                    color = TPTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TPText(
                text = installCommand,
                color = TPTheme.colors.purple,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = TPTheme.colors.lightGray,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(StringSelection(installCommand), null)
                    }
            )
        }

        Spacer(modifier = Modifier.size(24.dp))
        TPText(
            text = "Try Again",
            color = TPTheme.colors.blue,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .clickable { onRetry() }
                .background(
                    color = TPTheme.colors.blue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RemoteControlDialog(
    onDismiss: () -> Unit,
    onConfirm: (preventSleep: Boolean) -> Unit,
) {
    var preventSleep by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(TPTheme.colors.gray, RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            TPText(
                text = "Remote Control",
                color = TPTheme.colors.white,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TPText(
                text = "Start a remote control session to continue this conversation from your phone or browser.",
                color = TPTheme.colors.lightGray,
                style = TextStyle(fontSize = 12.sp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TPCheckbox(
                checked = preventSleep,
                label = "Prevent sleep mode (caffeinate)",
                color = TPTheme.colors.blue,
                onCheckedChange = { preventSleep = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                TPText(
                    text = "Prevents sleep in these cases:",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                TPText(
                    text = "\u2022 Screen idle timeout (display sleep)\n\u2022 System idle timeout (idle sleep)\n\u2022 Lid close while charging (system sleep)",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp, lineHeight = 14.sp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                TPText(
                    text = "Does not prevent: lid close on battery, manual sleep, low battery shutdown.",
                    color = TPTheme.colors.lightGray.copy(alpha = 0.6f),
                    style = TextStyle(fontSize = 12.sp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TPButton(
                    text = "Cancel",
                    backgroundColor = TPTheme.colors.black,
                    onClick = onDismiss,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TPButton(
                    text = "Start",
                    backgroundColor = TPTheme.colors.blue,
                    onClick = { onConfirm(preventSleep) },
                )
            }
        }
    }
}

@Composable
private fun TerminalInputBar(
    onSend: (String) -> Unit,
    onInjectFile: () -> String?,
    selectedImagePaths: List<String>,
    onPickImage: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onClearImages: () -> Unit,
    pendingInput: String?,
    onPendingInputConsumed: () -> Unit,
    onSlashClick: () -> Unit,
    onChangeModelClick: () -> Unit,
    onCreateSkillClick: () -> Unit,
    isRemoteControlActive: Boolean = false,
    onRemoteControlStart: () -> Unit = {},
    onRemoteControlStop: () -> Unit = {},
) {
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(pendingInput) {
        if (pendingInput != null) {
            val newText = if (inputValue.text.isEmpty()) pendingInput else "${inputValue.text} $pendingInput"
            inputValue = TextFieldValue(newText, TextRange(newText.length))
            onPendingInputConsumed()
        }
    }

    val hasContent = inputValue.text.isNotBlank() || selectedImagePaths.isNotEmpty()

    fun doSend() {
        if (!hasContent) return
        val message = buildString {
            if (inputValue.text.isNotBlank()) append(inputValue.text)
            for (path in selectedImagePaths) {
                if (isNotEmpty()) append(" ")
                append(path)
            }
        }
        inputValue = TextFieldValue("")
        onSend(message)
        SwingUtilities.invokeLater { onClearImages() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TPTheme.colors.gray)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TPActionCard(
                title = "Change Model",
                icon = Icons.Rounded.SmartToy,
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.EXTRA_SMALL,
                onClick = { onChangeModelClick() },
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPActionCard(
                title = "Create Skill",
                icon = Icons.Rounded.AutoFixHigh,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                onClick = { onCreateSkillClick() },
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(8.dp))
            TPSwitch(
                checked = isRemoteControlActive,
                text = "Remote Control",
                onCheckedChange = {
                    if (isRemoteControlActive) onRemoteControlStop() else onRemoteControlStart()
                },
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Left side icons — horizontal row
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // File inject button
                Icon(
                    imageVector = Icons.Rounded.AlternateEmail,
                    contentDescription = "Add active file path",
                    tint = TPTheme.colors.lightGray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            val path = onInjectFile() ?: return@clickable
                            val newText = if (inputValue.text.isEmpty()) path else "${inputValue.text} $path"
                            inputValue = TextFieldValue(newText, TextRange(newText.length))
                        }
                )
                // Image picker button
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = "Add image",
                    tint = if (selectedImagePaths.isNotEmpty()) TPTheme.colors.blue else TPTheme.colors.lightGray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onPickImage() }
                )
            }

            // Middle area: image chips + input field
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Image chips
                if (selectedImagePaths.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        selectedImagePaths.forEach { path ->
                            val fileName = path.substringAfterLast("/")
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = TPTheme.colors.blue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = TPTheme.colors.blue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                TPText(
                                    text = fileName,
                                    color = TPTheme.colors.blue,
                                    style = TextStyle(fontSize = 12.sp),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove image",
                                    tint = TPTheme.colors.blue,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveImage(path) }
                                )
                            }
                        }
                    }
                }

                // Input field
                BasicTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        val wasEmpty = inputValue.text.isEmpty()
                        inputValue = newValue
                        if (wasEmpty && newValue.text == "/") {
                            onSlashClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TPTheme.colors.black)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                if (event.isShiftPressed) {
                                    val cursor = inputValue.selection.start
                                    val newText =
                                        inputValue.text.substring(0, cursor) + "\n" + inputValue.text.substring(cursor)
                                    inputValue = TextFieldValue(newText, TextRange(cursor + 1))
                                    true
                                } else {
                                    doSend()
                                    true
                                }
                            } else false
                        },
                    textStyle = TextStyle(
                        color = TPTheme.colors.white,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(TPTheme.colors.white),
                    decorationBox = { inneTPTextField ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                            ) {
                                if (inputValue.text.isEmpty()) {
                                    TPText(
                                        text = "Write your message here...",
                                        color = TPTheme.colors.hintGray,
                                        style = TextStyle(fontSize = 14.sp),
                                    )
                                }
                                inneTPTextField()
                            }
                            Box(
                                modifier = Modifier
                                    .background(TPTheme.colors.primaryContainer, RoundedCornerShape(6.dp))
                                    .clickable { onSlashClick() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    TPText(
                                        text = "/",
                                        color = TPTheme.colors.white,
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.UnfoldMore,
                                        contentDescription = null,
                                        tint = TPTheme.colors.white,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    },
                    minLines = 4,
                )
            }

            // Send button
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send",
                tint = if (hasContent) TPTheme.colors.blue else TPTheme.colors.hintGray,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { doSend() }
            )
        }
    }

}

// --- Unified Command Palette ---

private enum class PaletteCategory { SKILL, AGENT, COMMAND, SC_COMMAND }

private enum class PaletteFilter { ALL, SKILLS, AGENTS, COMMANDS, SC_COMMANDS }

private data class PaletteItem(
    val category: PaletteCategory,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val filePath: String? = null,
    val terminalText: String,
    val autoRun: Boolean,
)

private data class ClaudeCommand(val command: String, val description: String, val icon: ImageVector)

private val claudeCommands = listOf(
    ClaudeCommand("/clear", "Clear conversation", Icons.Rounded.DeleteSweep),
    ClaudeCommand("/compact", "Compact conversation", Icons.Rounded.Compress),
    ClaudeCommand("/config", "Settings (Config)", Icons.Rounded.Settings),
    ClaudeCommand("/context", "Context usage", Icons.Rounded.GridOn),
    ClaudeCommand("/copy", "Copy last response", Icons.Rounded.ContentCopy),
    ClaudeCommand("/cost", "Token usage", Icons.Rounded.AttachMoney),
    ClaudeCommand("/debug", "Debug session", Icons.Rounded.BugReport),
    ClaudeCommand("/desktop", "Switch to desktop", Icons.Rounded.DesktopWindows),
    ClaudeCommand("/doctor", "Health check", Icons.Rounded.HealthAndSafety),
    ClaudeCommand("/exit", "Exit REPL", Icons.AutoMirrored.Rounded.ExitToApp),
    ClaudeCommand("/export", "Export conversation", Icons.Rounded.FileDownload),
    ClaudeCommand("/help", "Usage help", Icons.AutoMirrored.Rounded.Help),
    ClaudeCommand("/init", "Init CLAUDE.md", Icons.AutoMirrored.Rounded.NoteAdd),
    ClaudeCommand("/mcp", "MCP servers", Icons.Rounded.Hub),
    ClaudeCommand("/memory", "Edit memory files", Icons.Rounded.Memory),
    ClaudeCommand("/model", "Change model", Icons.Rounded.SmartToy),
    ClaudeCommand("/permissions", "Permissions", Icons.Rounded.Security),
    ClaudeCommand("/plan", "Plan mode", Icons.Rounded.Map),
    ClaudeCommand("/rename", "Rename session", Icons.Rounded.DriveFileRenameOutline),
    ClaudeCommand("/resume", "Resume session", Icons.Rounded.PlayArrow),
    ClaudeCommand("/rewind", "Rewind conversation", Icons.AutoMirrored.Rounded.Undo),
    ClaudeCommand("/stats", "Usage stats", Icons.Rounded.BarChart),
    ClaudeCommand("/status", "Settings (Status)", Icons.Rounded.Info),
    ClaudeCommand("/statusline", "Status line UI", Icons.Rounded.LinearScale),
    ClaudeCommand("/tasks", "Background tasks", Icons.Rounded.Checklist),
    ClaudeCommand("/teleport", "Remote session", Icons.Rounded.Cloud),
    ClaudeCommand("/theme", "Color theme", Icons.Rounded.Palette),
    ClaudeCommand("/todos", "TODO items", Icons.AutoMirrored.Rounded.FormatListBulleted),
    ClaudeCommand("/usage", "Usage limits", Icons.Rounded.DataUsage),
)

private val scCommands = listOf(
    ClaudeCommand("/sc:analyze", "Code analysis", Icons.Rounded.Analytics),
    ClaudeCommand("/sc:brainstorm", "Requirements discovery", Icons.Rounded.Lightbulb),
    ClaudeCommand("/sc:build", "Build & compile", Icons.Rounded.Build),
    ClaudeCommand("/sc:business-panel", "Business panel analysis", Icons.Rounded.Business),
    ClaudeCommand("/sc:cleanup", "Code cleanup", Icons.Rounded.CleaningServices),
    ClaudeCommand("/sc:design", "System design", Icons.Rounded.Architecture),
    ClaudeCommand("/sc:document", "Generate documentation", Icons.Rounded.Description),
    ClaudeCommand("/sc:estimate", "Development estimates", Icons.Rounded.Timer),
    ClaudeCommand("/sc:explain", "Code explanation", Icons.Rounded.School),
    ClaudeCommand("/sc:git", "Git operations", Icons.Rounded.Hub),
    ClaudeCommand("/sc:help", "SC help", Icons.AutoMirrored.Rounded.Help),
    ClaudeCommand("/sc:implement", "Feature implementation", Icons.Rounded.Code),
    ClaudeCommand("/sc:improve", "Code improvements", Icons.AutoMirrored.Rounded.TrendingUp),
    ClaudeCommand("/sc:index", "Project indexing", Icons.Rounded.FindInPage),
    ClaudeCommand("/sc:load", "Load session context", Icons.Rounded.Download),
    ClaudeCommand("/sc:pm", "Project manager agent", Icons.Rounded.ManageAccounts),
    ClaudeCommand("/sc:recommend", "Command recommendation", Icons.Rounded.Recommend),
    ClaudeCommand("/sc:reflect", "Task reflection", Icons.Rounded.Psychology),
    ClaudeCommand("/sc:research", "Deep web research", Icons.Rounded.Search),
    ClaudeCommand("/sc:save", "Save session context", Icons.Rounded.Save),
    ClaudeCommand("/sc:select-tool", "MCP tool selection", Icons.Rounded.Handyman),
    ClaudeCommand("/sc:spawn", "Task orchestration", Icons.Rounded.AccountTree),
    ClaudeCommand("/sc:spec-panel", "Spec review panel", Icons.Rounded.RateReview),
    ClaudeCommand("/sc:task", "Task management", Icons.Rounded.Task),
    ClaudeCommand("/sc:test", "Test execution", Icons.Rounded.Science),
    ClaudeCommand("/sc:troubleshoot", "Issue diagnosis", Icons.Rounded.Troubleshoot),
    ClaudeCommand("/sc:workflow", "Workflow generation", Icons.Rounded.Route),
)

@Composable
private fun UnifiedCommandPalette(
    project: Project,
    scanSkillsUseCase: ScanSkillsUseCase,
    settingsService: PluginSettingsService,
    superClaudeInstalled: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (PaletteItem) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PaletteFilter.ALL) }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var agents by remember { mutableStateOf<List<Skill>>(emptyList()) }

    LaunchedEffect(Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val skillsRoot = settingsService.getSkillsRootPath()
            scanSkillsUseCase(skillsRoot, true).onSuccess { folders ->
                val all = folders.flatMap { it.getAllSkills() }
                ApplicationManager.getApplication().invokeLater { skills = all }
            }
        }
    }

    LaunchedEffect(Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val agentsRoot = settingsService.getAgentsRootPath()
            scanSkillsUseCase(agentsRoot, false).onSuccess { folders ->
                val all = folders.flatMap { it.getAllSkills() }
                ApplicationManager.getApplication().invokeLater { agents = all }
            }
        }
    }

    val allItems = remember(skills, agents, superClaudeInstalled) {
        buildList {
            skills.forEach { skill ->
                add(
                    PaletteItem(
                        category = PaletteCategory.SKILL,
                        title = skill.relativePath,
                        description = skill.description,
                        icon = Icons.Rounded.AutoFixHigh,
                        filePath = skill.filePath,
                        terminalText = skill.filePath,
                        autoRun = false,
                    )
                )
            }
            agents.forEach { agent ->
                add(
                    PaletteItem(
                        category = PaletteCategory.AGENT,
                        title = agent.relativePath,
                        description = agent.description,
                        icon = Icons.Rounded.Psychology,
                        filePath = agent.filePath,
                        terminalText = agent.filePath,
                        autoRun = false,
                    )
                )
            }
            claudeCommands.forEach { cmd ->
                add(
                    PaletteItem(
                        category = PaletteCategory.COMMAND,
                        title = cmd.command,
                        description = cmd.description,
                        icon = cmd.icon,
                        terminalText = cmd.command,
                        autoRun = true,
                    )
                )
            }
            if (superClaudeInstalled) {
                scCommands.forEach { cmd ->
                    add(
                        PaletteItem(
                            category = PaletteCategory.SC_COMMAND,
                            title = cmd.command,
                            description = cmd.description,
                            icon = cmd.icon,
                            terminalText = cmd.command,
                            autoRun = false,
                        )
                    )
                }
            }
        }
    }

    val filteredItems = remember(allItems, searchQuery, selectedFilter) {
        allItems.filter { item ->
            val matchesFilter = when (selectedFilter) {
                PaletteFilter.ALL -> true
                PaletteFilter.SKILLS -> item.category == PaletteCategory.SKILL
                PaletteFilter.AGENTS -> item.category == PaletteCategory.AGENT
                PaletteFilter.COMMANDS -> item.category == PaletteCategory.COMMAND
                PaletteFilter.SC_COMMANDS -> item.category == PaletteCategory.SC_COMMAND
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val groupedItems = remember(filteredItems) {
        filteredItems.groupBy { it.category }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(TPTheme.colors.black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = false) {}
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .background(TPTheme.colors.gray, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                }
        ) {
            // Search field
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = TPTheme.colors.black,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle(color = TPTheme.colors.white, fontSize = 14.sp),
                cursorBrush = SolidColor(TPTheme.colors.white),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TPTheme.colors.hintGray,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search commands, skills, agents...",
                                    color = TPTheme.colors.hintGray,
                                    fontSize = 14.sp,
                                )
                            }
                            innerTextField()
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // Category tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PaletteFilter.entries.forEach { filter ->
                    if (filter == PaletteFilter.SC_COMMANDS && !superClaudeInstalled) return@forEach
                    val label = when (filter) {
                        PaletteFilter.ALL -> "All"
                        PaletteFilter.SKILLS -> "Skills"
                        PaletteFilter.AGENTS -> "Agents"
                        PaletteFilter.COMMANDS -> "Commands"
                        PaletteFilter.SC_COMMANDS -> "SC"
                    }
                    val isSelected = selectedFilter == filter
                    Text(
                        text = label,
                        modifier = Modifier
                            .background(
                                color = if (isSelected) TPTheme.colors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (isSelected) TPTheme.colors.white else TPTheme.colors.lightGray,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Items list
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No results found", color = TPTheme.colors.lightGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    PaletteCategory.entries.forEach { category ->
                        val items = groupedItems[category] ?: return@forEach
                        if (items.isEmpty()) return@forEach

                        item(key = "header-$category") {
                            val headerText = when (category) {
                                PaletteCategory.SKILL -> "SKILLS"
                                PaletteCategory.AGENT -> "AGENTS"
                                PaletteCategory.COMMAND -> "COMMANDS"
                                PaletteCategory.SC_COMMAND -> "SC COMMANDS"
                            }
                            val headerColor = when (category) {
                                PaletteCategory.SKILL -> TPTheme.colors.blue
                                PaletteCategory.AGENT -> TPTheme.colors.blue
                                PaletteCategory.COMMAND -> TPTheme.colors.purple
                                PaletteCategory.SC_COMMAND -> TPTheme.colors.blue
                            }
                            Text(
                                text = headerText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                color = headerColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                        }

                        items(items, key = { "${it.category}-${it.title}" }) { paletteItem ->
                            val accentColor = when (paletteItem.category) {
                                PaletteCategory.SKILL -> TPTheme.colors.blue
                                PaletteCategory.AGENT -> TPTheme.colors.blue
                                PaletteCategory.COMMAND -> TPTheme.colors.purple
                                PaletteCategory.SC_COMMAND -> TPTheme.colors.blue
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemSelected(paletteItem) }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = paletteItem.icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = paletteItem.title,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TPTheme.colors.white,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                    if (paletteItem.description.isNotBlank()) {
                                        Text(
                                            text = paletteItem.description,
                                            color = TPTheme.colors.hintGray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                if (paletteItem.filePath != null) {
                                    @Suppress("DEPRECATION")
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInNew,
                                        contentDescription = "Open in editor",
                                        tint = TPTheme.colors.hintGray,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                val vf = LocalFileSystem.getInstance()
                                                    .findFileByPath(paletteItem.filePath)
                                                if (vf != null) {
                                                    ApplicationManager.getApplication().invokeLater {
                                                        FileEditorManager.getInstance(project)
                                                            .openFile(vf, true)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
