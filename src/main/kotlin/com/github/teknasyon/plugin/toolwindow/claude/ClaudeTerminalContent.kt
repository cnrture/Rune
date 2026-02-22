package com.github.teknasyon.plugin.toolwindow.claude

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
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
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun ClaudeTerminalContent(project: Project) {
    val service = remember { ClaudeSessionService.getInstance(project) }
    val state by service.state.collectAsState()

    LaunchedEffect(Unit) {
        service.checkClaudeInstalled()
    }

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

                if (state.sessions.isNotEmpty()) {
                    SessionTabBar(
                        sessions = state.sessions,
                        activeSessionId = state.activeSessionId,
                        onSelectSession = { service.switchToSession(it) },
                        onCloseSession = { service.closeSession(it) },
                        onAddSession = { service.addNewSession() },
                    )

                    SwingPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp),
                        factory = { service.sessionManager.parentPanel },
                        update = {}
                    )

                    TPActionCard(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        title = "Change Model",
                        icon = Icons.Rounded.SmartToy,
                        type = TPActionCardType.SMALL,
                        actionColor = TPTheme.colors.purple,
                        onClick = {
                            val widget = service.activeWidget ?: return@TPActionCard
                            @Suppress("DEPRECATION")
                            widget.terminalStarter?.sendString("/model", true)
                            widget.preferredFocusableComponent.requestFocusInWindow()
                            javax.swing.SwingUtilities.invokeLater {
                                @Suppress("DEPRECATION")
                                widget.terminalStarter?.sendBytes("\r".toByteArray(), true)
                            }
                        },
                    )

                    TerminalInputBar(
                        onSend = { text ->
                            val widget = service.activeWidget ?: return@TerminalInputBar
                            @Suppress("DEPRECATION")
                            widget.terminalStarter?.sendString(text, true)
                            widget.preferredFocusableComponent.requestFocusInWindow()
                            // Send Enter separately so CLI treats it as submit, not part of paste
                            javax.swing.SwingUtilities.invokeLater {
                                @Suppress("DEPRECATION")
                                widget.terminalStarter?.sendBytes("\r".toByteArray(), true)
                            }
                        },
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
}

@Composable
private fun SessionTabBar(
    sessions: List<ClaudeSession>,
    activeSessionId: Int,
    onSelectSession: (Int) -> Unit,
    onCloseSession: (Int) -> Unit,
    onAddSession: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TPTheme.colors.black)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
