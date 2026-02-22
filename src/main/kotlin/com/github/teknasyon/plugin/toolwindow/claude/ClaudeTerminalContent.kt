package com.github.teknasyon.plugin.toolwindow.claude

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TPTheme.colors.gray)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.FileOpen,
                contentDescription = "Aktif dosyayı gönder",
                tint = if (service.activeWidget != null) TPTheme.colors.lightGray else TPTheme.colors.hintGray,
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        val widget = service.activeWidget ?: return@clickable
                        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
                            ?: return@clickable
                        val relativePath = file.path
                            .removePrefix(project.basePath ?: "")
                            .removePrefix("/")
                        widget.terminalStarter?.sendString(relativePath.plus("\n"), true)
                        widget.preferredFocusableComponent.requestFocusInWindow()
                    }
            )
        }

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
                        modifier = Modifier.fillMaxSize(),
                        factory = { service.sessionManager.parentPanel },
                        update = {}
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
