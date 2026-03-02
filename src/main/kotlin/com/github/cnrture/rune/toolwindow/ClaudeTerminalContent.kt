package com.github.cnrture.rune.toolwindow

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
import androidx.compose.ui.zIndex
import com.github.cnrture.rune.actions.dialog.CreateSkillDialog
import com.github.cnrture.rune.components.RActionCard
import com.github.cnrture.rune.components.RActionCardType
import com.github.cnrture.rune.components.RText
import com.github.cnrture.rune.data.repository.SkillRepositoryImpl
import com.github.cnrture.rune.domain.model.Skill
import com.github.cnrture.rune.domain.usecase.ScanSkillsUseCase
import com.github.cnrture.rune.service.FileScanner
import com.github.cnrture.rune.settings.PluginConfigurable
import com.github.cnrture.rune.settings.PluginSettingsService
import com.github.cnrture.rune.theme.RTheme
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

    LaunchedEffect(Unit) {
        service.checkClaudeInstalled()
    }

    fun sendToTerminal(cmd: String, autoRun: Boolean) {
        service.sendToTerminal(cmd, autoRun)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RTheme.colors.black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RTheme.colors.black)
        ) {
            when (state.claudeInstalled) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        RText(
                            text = "Checking Claude CLI...",
                            color = RTheme.colors.lightGray
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
                            onSettingsClick = {
                                ShowSettingsUtil.getInstance()
                                    .editConfigurable(project, PluginConfigurable(project))
                            },
                        )

                        if (showCommandPalette) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(RTheme.colors.black),
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
                            onUsageClick = { sendToTerminal("/usage", true) },
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
                onDismiss = { showCommandPalette = false },
                onItemSelected = { item ->
                    showCommandPalette = false
                    sendToTerminal(item.terminalText, item.autoRun)
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
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RTheme.colors.black)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sessions.forEach { session ->
                val isActive = session.id == activeSessionId
                Row(
                    modifier = Modifier
                        .background(
                            color = if (isActive) RTheme.colors.gray else RTheme.colors.black,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RText(
                        text = session.title,
                        color = if (isActive) RTheme.colors.white else RTheme.colors.lightGray,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close session",
                        tint = if (isActive) RTheme.colors.lightGray else RTheme.colors.hintGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New session",
                tint = RTheme.colors.lightGray,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onAddSession() }
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        RActionCard(
            title = "Settings",
            icon = Icons.Rounded.Settings,
            actionColor = RTheme.colors.purple,
            type = RActionCardType.EXTRA_SMALL,
            onClick = { onSettingsClick() },
        )
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
            tint = RTheme.colors.purple,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        RText(
            text = "Claude CLI Not Found",
            color = RTheme.colors.white,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.size(8.dp))
        RText(
            text = "Claude CLI is not installed. Run the following command to install:",
            color = RTheme.colors.lightGray,
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.size(16.dp))

        Row(
            modifier = Modifier
                .background(
                    color = RTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RText(
                text = installCommand,
                color = RTheme.colors.purple,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = RTheme.colors.lightGray,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(StringSelection(installCommand), null)
                    }
            )
        }

        Spacer(modifier = Modifier.size(24.dp))
        RText(
            text = "Try Again",
            color = RTheme.colors.blue,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .clickable { onRetry() }
                .background(
                    color = RTheme.colors.blue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
    onUsageClick: () -> Unit,
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
            .background(RTheme.colors.gray)
            .padding(8.dp),
    ) {
        Row {
            RActionCard(
                title = "Change Model",
                icon = Icons.Rounded.SmartToy,
                actionColor = RTheme.colors.purple,
                type = RActionCardType.EXTRA_SMALL,
                onClick = { onChangeModelClick() },
            )
            Spacer(modifier = Modifier.size(8.dp))
            RActionCard(
                title = "Create Skill",
                icon = Icons.Rounded.AutoFixHigh,
                actionColor = RTheme.colors.blue,
                type = RActionCardType.EXTRA_SMALL,
                onClick = { onCreateSkillClick() },
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(8.dp))
            RActionCard(
                title = "Usage",
                icon = Icons.Rounded.DataUsage,
                actionColor = RTheme.colors.blue,
                type = RActionCardType.EXTRA_SMALL,
                onClick = { onUsageClick() },
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
                    tint = RTheme.colors.lightGray,
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
                    tint = if (selectedImagePaths.isNotEmpty()) RTheme.colors.blue else RTheme.colors.lightGray,
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
                                        color = RTheme.colors.blue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = RTheme.colors.blue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                RText(
                                    text = fileName,
                                    color = RTheme.colors.blue,
                                    style = TextStyle(fontSize = 12.sp),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove image",
                                    tint = RTheme.colors.blue,
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
                        .background(RTheme.colors.black)
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
                        color = RTheme.colors.white,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(RTheme.colors.white),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                            ) {
                                if (inputValue.text.isEmpty()) {
                                    RText(
                                        text = "Write your message here...",
                                        color = RTheme.colors.hintGray,
                                        style = TextStyle(fontSize = 14.sp),
                                    )
                                }
                                innerTextField()
                            }
                            Box(
                                modifier = Modifier
                                    .background(RTheme.colors.primaryContainer, RoundedCornerShape(6.dp))
                                    .clickable { onSlashClick() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    RText(
                                        text = "/",
                                        color = RTheme.colors.white,
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.UnfoldMore,
                                        contentDescription = null,
                                        tint = RTheme.colors.white,
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
                tint = if (hasContent) RTheme.colors.blue else RTheme.colors.hintGray,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { doSend() }
            )
        }
    }

}

// --- Unified Command Palette ---

private enum class PaletteCategory { SKILL, AGENT, COMMAND }

private enum class PaletteFilter { ALL, SKILLS, AGENTS, COMMANDS }

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

@Composable
private fun UnifiedCommandPalette(
    project: Project,
    scanSkillsUseCase: ScanSkillsUseCase,
    settingsService: PluginSettingsService,
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

    val allItems = remember(skills, agents) {
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
        }
    }

    val filteredItems = remember(allItems, searchQuery, selectedFilter) {
        allItems.filter { item ->
            val matchesFilter = when (selectedFilter) {
                PaletteFilter.ALL -> true
                PaletteFilter.SKILLS -> item.category == PaletteCategory.SKILL
                PaletteFilter.AGENTS -> item.category == PaletteCategory.AGENT
                PaletteFilter.COMMANDS -> item.category == PaletteCategory.COMMAND
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
            .background(RTheme.colors.black.copy(alpha = 0.6f))
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
                .background(RTheme.colors.gray, RoundedCornerShape(12.dp))
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
                        color = RTheme.colors.black,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle(color = RTheme.colors.white, fontSize = 14.sp),
                cursorBrush = SolidColor(RTheme.colors.white),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = RTheme.colors.hintGray,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search commands, skills, agents...",
                                    color = RTheme.colors.hintGray,
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
                    val label = when (filter) {
                        PaletteFilter.ALL -> "All"
                        PaletteFilter.SKILLS -> "Skills"
                        PaletteFilter.AGENTS -> "Agents"
                        PaletteFilter.COMMANDS -> "Commands"
                    }
                    val isSelected = selectedFilter == filter
                    Text(
                        text = label,
                        modifier = Modifier
                            .background(
                                color = if (isSelected) RTheme.colors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (isSelected) RTheme.colors.white else RTheme.colors.lightGray,
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
                    Text("No results found", color = RTheme.colors.lightGray, fontSize = 13.sp)
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
                            }
                            val headerColor = when (category) {
                                PaletteCategory.SKILL -> RTheme.colors.blue
                                PaletteCategory.AGENT -> RTheme.colors.blue
                                PaletteCategory.COMMAND -> RTheme.colors.purple
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
                                PaletteCategory.SKILL -> RTheme.colors.blue
                                PaletteCategory.AGENT -> RTheme.colors.blue
                                PaletteCategory.COMMAND -> RTheme.colors.purple
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
                                        color = RTheme.colors.white,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                    if (paletteItem.description.isNotBlank()) {
                                        Text(
                                            text = paletteItem.description,
                                            color = RTheme.colors.hintGray,
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
                                        tint = RTheme.colors.hintGray,
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
