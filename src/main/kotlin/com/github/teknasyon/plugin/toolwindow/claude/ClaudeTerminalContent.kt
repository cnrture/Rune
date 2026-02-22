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
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.data.repository.SkillRepositoryImpl
import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.domain.model.SkillFolder
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.service.FileScanner
import com.github.teknasyon.plugin.service.SkillDockSettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun ClaudeTerminalContent(project: Project) {
    val service = remember { ClaudeSessionService.getInstance(project) }
    val state by service.state.collectAsState()
    val settingsService = remember { SkillDockSettingsService.getInstance(project) }
    val scanSkillsUseCase = remember {
        val fileScanner = FileScanner(project)
        val repository = SkillRepositoryImpl(fileScanner, settingsService)
        ScanSkillsUseCase(repository, settingsService)
    }

    var showSkillsDialog by remember { mutableStateOf(false) }
    var showAgentsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        service.checkClaudeInstalled()
    }

    // Helper: send command to active terminal
    fun sendToTerminal(cmd: String, autoRun: Boolean) {
        val widget = service.activeWidget ?: return
        @Suppress("DEPRECATION")
        widget.terminalStarter?.sendString(cmd, true)
        widget.preferredFocusableComponent.requestFocusInWindow()
        if (autoRun) {
            javax.swing.SwingUtilities.invokeLater {
                @Suppress("DEPRECATION")
                widget.terminalStarter?.sendBytes("\r".toByteArray(), true)
            }
        }
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
                            text = "Claude CLI kontrol ediliyor...",
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

                    val anyDialogOpen = showSkillsDialog || showAgentsDialog || showSettingsDialog

                    if (state.sessions.isNotEmpty()) {
                        SessionTabBar(
                            sessions = state.sessions,
                            activeSessionId = state.activeSessionId,
                            onSelectSession = { service.switchToSession(it) },
                            onCloseSession = { service.closeSession(it) },
                            onAddSession = { service.addNewSession() },
                            onSettingsClick = { showSettingsDialog = true },
                        )

                        if (anyDialogOpen) {
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

                        // Action buttons row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TPActionCard(
                                title = "Model",
                                icon = Icons.Rounded.SmartToy,
                                type = TPActionCardType.SMALL,
                                actionColor = TPTheme.colors.purple,
                                onClick = { sendToTerminal("/model", true) },
                            )
                            TPActionCard(
                                title = "Skills",
                                icon = Icons.Rounded.AutoFixHigh,
                                type = TPActionCardType.SMALL,
                                actionColor = TPTheme.colors.blue,
                                onClick = { showSkillsDialog = true },
                            )
                            TPActionCard(
                                title = "Agents",
                                icon = Icons.Rounded.Psychology,
                                type = TPActionCardType.SMALL,
                                actionColor = TPTheme.colors.blue,
                                onClick = { showAgentsDialog = true },
                            )
                        }

                        TerminalInputBar(
                            onSend = { text -> sendToTerminal(text, true) },
                            onInjectFile = {
                                val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
                                    ?: return@TerminalInputBar null
                                file.path
                                    .removePrefix(project.basePath ?: "")
                                    .removePrefix("/")
                            },
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showSkillsDialog) {
            SkillPickerDialog(
                title = "Skills",
                scanSkillsUseCase = scanSkillsUseCase,
                rootPath = settingsService.getSkillsRootPath(),
                strictFilter = true,
                onDismiss = { showSkillsDialog = false },
                onSkillSelected = { skill ->
                    showSkillsDialog = false
                    sendToTerminal(skill.filePath, false)
                },
            )
        }
        if (showAgentsDialog) {
            SkillPickerDialog(
                title = "Agents",
                scanSkillsUseCase = scanSkillsUseCase,
                rootPath = settingsService.getAgentsRootPath(),
                strictFilter = false,
                onDismiss = { showAgentsDialog = false },
                onSkillSelected = { skill ->
                    showAgentsDialog = false
                    sendToTerminal(skill.filePath, false)
                },
            )
        }
        if (showSettingsDialog) {
            SettingsDialog(
                settingsService = settingsService,
                onDismiss = { showSettingsDialog = false },
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
            .background(TPTheme.colors.black)
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                            color = if (isActive) TPTheme.colors.gray else TPTheme.colors.black,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Oturumu kapat",
                        tint = if (isActive) TPTheme.colors.lightGray else TPTheme.colors.hintGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Yeni oturum",
                tint = TPTheme.colors.lightGray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onAddSession() }
                    .padding(2.dp)
            )
        }
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Ayarlar",
            tint = TPTheme.colors.lightGray,
            modifier = Modifier
                .size(18.dp)
                .clickable { onSettingsClick() }
                .padding(1.dp)
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
            tint = TPTheme.colors.purple,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TPText(
            text = "Claude CLI Bulunamadı",
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        TPText(
            text = "Claude CLI kurulu değil. Aşağıdaki komutu çalıştırarak kurabilirsiniz:",
            color = TPTheme.colors.lightGray,
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.height(16.dp))

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
            Spacer(modifier = Modifier.width(12.dp))
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

        Spacer(modifier = Modifier.height(24.dp))
        TPText(
            text = "Tekrar Dene",
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
private fun TerminalInputBar(
    onSend: (String) -> Unit,
    onInjectFile: () -> String?,
) {
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TPTheme.colors.gray)
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // File inject button
        Icon(
            imageVector = Icons.Rounded.AttachFile,
            contentDescription = "Aktif dosyayı ekle",
            tint = TPTheme.colors.lightGray,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    val path = onInjectFile() ?: return@clickable
                    inputText = if (inputText.isEmpty()) path else "$inputText $path"
                }
                .padding(2.dp)
                .align(Alignment.CenterVertically)
        )

        // Input field
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp, max = 120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TPTheme.colors.black)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        if (event.isShiftPressed) {
                            false // yeni satır
                        } else {
                            if (inputText.isNotBlank()) {
                                onSend(inputText)
                                inputText = ""
                            }
                            true
                        }
                    } else false
                },
            textStyle = TextStyle(
                color = TPTheme.colors.white,
                fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(TPTheme.colors.white),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.TopStart) {
                    if (inputText.isEmpty()) {
                        TPText(
                            text = "Mesajınızı yazın...",
                            color = TPTheme.colors.hintGray,
                            style = TextStyle(fontSize = 13.sp),
                        )
                    }
                    innerTextField()
                }
            },
            minLines = 2,
        )

        // Send button
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Gönder",
            tint = if (inputText.isNotBlank()) TPTheme.colors.blue else TPTheme.colors.hintGray,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    if (inputText.isNotBlank()) {
                        onSend(inputText)
                        inputText = ""
                    }
                }
                .padding(2.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun SkillPickerDialog(
    title: String,
    scanSkillsUseCase: ScanSkillsUseCase,
    rootPath: String,
    strictFilter: Boolean,
    onDismiss: () -> Unit,
    onSkillSelected: (Skill) -> Unit,
) {
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(rootPath) {
        ApplicationManager.getApplication().executeOnPooledThread {
            scanSkillsUseCase(rootPath, strictFilter)
                .onSuccess { folders ->
                    val all = folders.flatMap { it.getAllSkills() }
                    ApplicationManager.getApplication().invokeLater { skills = all }
                }
                .onFailure { e ->
                    ApplicationManager.getApplication().invokeLater { error = e.message }
                }
        }
    }

    val filtered = remember(skills, searchQuery) {
        if (searchQuery.isBlank()) skills
        else skills.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.commandName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(TPTheme.colors.black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = false) {} // iç tıklamaları yutmak için
                .padding(24.dp)
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .background(TPTheme.colors.gray, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = TPTheme.colors.white,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Kapat",
                    tint = TPTheme.colors.lightGray,
                    modifier = Modifier.size(20.dp).clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ara...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = TPTheme.colors.white,
                    focusedBorderColor = TPTheme.colors.blue,
                    unfocusedBorderColor = TPTheme.colors.hintGray,
                    placeholderColor = TPTheme.colors.hintGray,
                ),
            )

            Spacer(Modifier.height(8.dp))

            when {
                error != null -> {
                    Text(
                        text = error ?: "",
                        color = TPTheme.colors.red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                filtered.isEmpty() && skills.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Yükleniyor...", color = TPTheme.colors.lightGray)
                    }
                }
                filtered.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sonuç bulunamadı", color = TPTheme.colors.lightGray)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered, key = { it.filePath }) { skill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSkillSelected(skill) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skill.relativePath,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TPTheme.colors.white,
                                        style = MaterialTheme.typography.body2,
                                    )
                                    if (skill.description.isNotBlank()) {
                                        Text(
                                            text = skill.description,
                                            color = TPTheme.colors.lightGray,
                                            style = MaterialTheme.typography.caption,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            Divider(color = TPTheme.colors.hintGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    settingsService: SkillDockSettingsService,
    onDismiss: () -> Unit,
) {
    var skillsPath by remember { mutableStateOf(settingsService.getSkillsRootPath()) }
    var agentsPath by remember { mutableStateOf(settingsService.getAgentsRootPath()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(TPTheme.colors.black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = false) {} // iç tıklamaları yutmak için
                .padding(24.dp)
                .fillMaxWidth()
                .background(TPTheme.colors.gray, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "SkillDock Ayarları",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = TPTheme.colors.white,
            )

            Spacer(Modifier.height(16.dp))

            Text("Skills Root Path", color = TPTheme.colors.lightGray, style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = skillsPath,
                onValueChange = { skillsPath = it },
                singleLine = true,
                placeholder = { Text("/path/to/skills") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = TPTheme.colors.white,
                    focusedBorderColor = TPTheme.colors.blue,
                    unfocusedBorderColor = TPTheme.colors.hintGray,
                    placeholderColor = TPTheme.colors.hintGray,
                ),
            )

            Spacer(Modifier.height(12.dp))

            Text("Agents Root Path", color = TPTheme.colors.lightGray, style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = agentsPath,
                onValueChange = { agentsPath = it },
                singleLine = true,
                placeholder = { Text("/path/to/agents") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = TPTheme.colors.white,
                    focusedBorderColor = TPTheme.colors.blue,
                    unfocusedBorderColor = TPTheme.colors.hintGray,
                    placeholderColor = TPTheme.colors.hintGray,
                ),
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = TPTheme.colors.lightGray)
                ) { Text("İptal") }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        settingsService.setSkillsRootPath(skillsPath.trim())
                        settingsService.setAgentsRootPath(agentsPath.trim())
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TPTheme.colors.blue)
                ) { Text("Kaydet") }
            }
        }
    }
}
