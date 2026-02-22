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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.JBColor
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

private data class ClaudeSession(
    val id: Int,
    val title: String,
    val widget: JBTerminalWidget? = null,
)

private class SessionManager {
    val cardLayout = CardLayout()
    val parentPanel = JPanel(cardLayout)
    private val panels = mutableMapOf<Int, JPanel>()
    private val disposables = mutableMapOf<Int, com.intellij.openapi.Disposable>()

    fun addSession(id: Int, project: Project): JBTerminalWidget? {
        val disposable = Disposer.newDisposable("ClaudeTerminal-$id")
        var widget: JBTerminalWidget? = null
        val panel = createClaudeTerminalPanel(project, disposable) { w -> widget = w }
        panels[id] = panel
        disposables[id] = disposable
        parentPanel.add(panel, id.toString())
        cardLayout.show(parentPanel, id.toString())
        parentPanel.revalidate()
        parentPanel.repaint()
        return widget
    }

    fun showSession(id: Int) {
        cardLayout.show(parentPanel, id.toString())
        parentPanel.revalidate()
        parentPanel.repaint()
    }

    fun removeSession(id: Int) {
        disposables[id]?.let { Disposer.dispose(it) }
        panels[id]?.let { parentPanel.remove(it) }
        disposables.remove(id)
        panels.remove(id)
        parentPanel.revalidate()
        parentPanel.repaint()
    }

    fun dispose() {
        disposables.values.toList().forEach { Disposer.dispose(it) }
        disposables.clear()
        panels.clear()
    }
}

@Composable
fun ClaudeTerminalContent(project: Project) {
    var claudeInstalled by remember { mutableStateOf<Boolean?>(null) }
    var sessions by remember { mutableStateOf(listOf<ClaudeSession>()) }
    var activeSessionId by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf(1) }
    val sessionManager = remember { SessionManager() }

    DisposableEffect(Unit) {
        onDispose { sessionManager.dispose() }
    }

    fun addNewSession() {
        val id = nextId
        nextId++
        val widget = sessionManager.addSession(id, project)
        sessions = sessions + ClaudeSession(id, "Claude $id", widget)
        activeSessionId = id
    }

    fun closeSession(id: Int) {
        sessionManager.removeSession(id)
        sessions = sessions.filter { it.id != id }
        if (sessions.isEmpty()) {
            addNewSession()
        } else if (activeSessionId == id) {
            activeSessionId = sessions.last().id
            sessionManager.showSession(activeSessionId)
        }
    }

    fun switchToSession(id: Int) {
        activeSessionId = id
        sessionManager.showSession(id)
    }

    LaunchedEffect(Unit) {
        claudeInstalled = checkClaudeInstalled()
    }

    val activeWidget = sessions.find { it.id == activeSessionId }?.widget

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                tint = TPTheme.colors.purple,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TPText(
                text = "Claude",
                color = TPTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
                contentDescription = "Aktif dosyayı gönder",
                tint = if (activeWidget != null) TPTheme.colors.lightGray else TPTheme.colors.hintGray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        val widget = activeWidget ?: return@clickable
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

        when (claudeInstalled) {
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
                    onRetry = { claudeInstalled = null }
                )
                LaunchedEffect(claudeInstalled) {
                    if (claudeInstalled == null) {
                        claudeInstalled = checkClaudeInstalled()
                    }
                }
            }

            true -> {
                LaunchedEffect(Unit) {
                    if (sessions.isEmpty()) {
                        addNewSession()
                    }
                }

                if (sessions.isNotEmpty()) {
                    SessionTabBar(
                        sessions = sessions,
                        activeSessionId = activeSessionId,
                        onSelectSession = { id -> switchToSession(id) },
                        onCloseSession = { id -> closeSession(id) },
                        onAddSession = { addNewSession() },
                    )

                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = { sessionManager.parentPanel },
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

@Suppress("DEPRECATION")
private fun createClaudeTerminalPanel(
    project: Project,
    disposable: com.intellij.openapi.Disposable,
    onWidgetReady: (JBTerminalWidget) -> Unit,
): JPanel {
    val panel = JPanel(BorderLayout())

    try {
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val widget = runner.createTerminalWidget(disposable, project.basePath ?: "", false)

        panel.add(widget.component, BorderLayout.CENTER)
        onWidgetReady(widget)

        // Intercept ESC at the AWT level before IntelliJ's action system,
        // so ESC goes to Claude instead of hiding the tool window
        val escDispatcher = KeyEventDispatcher { e ->
            if (e.id == KeyEvent.KEY_PRESSED && e.keyCode == KeyEvent.VK_ESCAPE) {
                val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, panel)) {
                    @Suppress("DEPRECATION")
                    widget.terminalStarter?.sendString("\u001B", true)
                    e.consume()
                    true
                } else false
            } else false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(escDispatcher)
        Disposer.register(disposable) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(escDispatcher)
        }

        // Execute claude command after shell initializes
        ApplicationManager.getApplication().executeOnPooledThread {
            Thread.sleep(1500)
            ApplicationManager.getApplication().invokeLater {
                try {
                    widget.terminalStarter?.sendString("claude\n", true)
                } catch (_: Exception) {
                }
            }
        }
    } catch (e: Exception) {
        val label = JLabel(
            "<html><center><br><br>Terminal oluşturulamadı.<br><br>${e.message ?: "Bilinmeyen hata"}</center></html>"
        )
        label.horizontalAlignment = SwingConstants.CENTER
        label.foreground = JBColor.LIGHT_GRAY
        panel.add(label, BorderLayout.CENTER)
    }

    return panel
}

private fun checkClaudeInstalled(): Boolean {
    return try {
        val process = ProcessBuilder("/bin/sh", "-c", "which claude")
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        exitCode == 0
    } catch (_: Exception) {
        false
    }
}
