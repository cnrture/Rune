package com.github.teknasyon.plugin.toolwindow.claude

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.JBColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

data class ClaudeSession(
    val id: Int,
    val title: String,
    val widget: JBTerminalWidget? = null,
)

data class ClaudeSessionState(
    val sessions: List<ClaudeSession> = emptyList(),
    val activeSessionId: Int = 0,
    val claudeInstalled: Boolean? = null,
    val superClaudeInstalled: Boolean? = null,
)

@Service(Service.Level.PROJECT)
class ClaudeSessionService(private val project: Project) : Disposable {

    private val _state = MutableStateFlow(ClaudeSessionState())
    val state: StateFlow<ClaudeSessionState> = _state.asStateFlow()

    private val _pendingInput = MutableStateFlow<String?>(null)
    val pendingInput: StateFlow<String?> = _pendingInput.asStateFlow()

    fun setPendingInput(text: String) {
        _pendingInput.value = text
    }

    fun consumePendingInput() {
        _pendingInput.value = null
    }

    val sessionManager = SessionManager()
    private var nextId = 1

    val activeWidget: JBTerminalWidget?
        get() {
            val s = _state.value
            return s.sessions.find { it.id == s.activeSessionId }?.widget
        }

    fun checkClaudeInstalled() {
        if (_state.value.claudeInstalled != null) return
        ApplicationManager.getApplication().executeOnPooledThread {
            val installed = doCheckClaudeInstalled()
            val scInstalled = doCheckSuperClaudeInstalled()
            ApplicationManager.getApplication().invokeLater {
                _state.value = _state.value.copy(
                    claudeInstalled = installed,
                    superClaudeInstalled = scInstalled,
                )
            }
        }
    }

    fun retryClaudeCheck() {
        _state.value = _state.value.copy(claudeInstalled = null, superClaudeInstalled = null)
        checkClaudeInstalled()
    }

    fun addNewSession() {
        val id = nextId++
        val widget = sessionManager.addSession(id, project)
        val session = ClaudeSession(id, "Claude $id", widget)
        _state.value = _state.value.copy(
            sessions = _state.value.sessions + session,
            activeSessionId = id,
        )
    }

    fun closeSession(id: Int) {
        sessionManager.removeSession(id)
        val current = _state.value
        val remaining = current.sessions.filter { it.id != id }

        if (remaining.isEmpty()) {
            _state.value = current.copy(sessions = emptyList())
            addNewSession()
        } else {
            val newActiveId = if (current.activeSessionId == id) {
                remaining.last().id
            } else {
                current.activeSessionId
            }
            _state.value = current.copy(sessions = remaining, activeSessionId = newActiveId)
            if (current.activeSessionId == id) {
                sessionManager.showSession(newActiveId)
            }
        }
    }

    fun switchToSession(id: Int) {
        _state.value = _state.value.copy(activeSessionId = id)
        sessionManager.showSession(id)
    }

    fun ensureSession() {
        if (_state.value.sessions.isEmpty() && _state.value.claudeInstalled == true) {
            addNewSession()
        }
    }

    @Suppress("DEPRECATION")
    fun sendToTerminal(text: String, autoRun: Boolean) {
        val widget = activeWidget ?: return
        widget.terminalStarter?.sendString(text, true)
        widget.preferredFocusableComponent.requestFocusInWindow()
        if (autoRun) {
            SwingUtilities.invokeLater {
                widget.terminalStarter?.sendBytes("\r".toByteArray(), true)
            }
        }
    }

    override fun dispose() {
        sessionManager.dispose()
    }

    companion object {
        fun getInstance(project: Project): ClaudeSessionService =
            project.getService(ClaudeSessionService::class.java)
    }
}

class SessionManager {
    val cardLayout = CardLayout()
    val parentPanel = JPanel(cardLayout)
    private val panels = mutableMapOf<Int, JPanel>()
    private val disposables = mutableMapOf<Int, Disposable>()

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

@Suppress("DEPRECATION")
private fun createClaudeTerminalPanel(
    project: Project,
    disposable: Disposable,
    onWidgetReady: (JBTerminalWidget) -> Unit,
): JPanel {
    val panel = JPanel(BorderLayout())

    try {
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val widget = runner.createTerminalWidget(disposable, project.basePath ?: "", false)

        panel.add(widget.component, BorderLayout.CENTER)
        onWidgetReady(widget)

        val keyDispatcher = KeyEventDispatcher { e ->
            if (e.id != KeyEvent.KEY_PRESSED) return@KeyEventDispatcher false
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner == null || !SwingUtilities.isDescendingFrom(
                    focusOwner,
                    panel
                )
            ) return@KeyEventDispatcher false

            val isMeta = e.isMetaDown // Cmd on macOS
            when {
                e.keyCode == KeyEvent.VK_ESCAPE -> {
                    @Suppress("DEPRECATION")
                    widget.terminalStarter?.sendString("\u001B", true)
                    e.consume()
                    true
                }
                // Cmd+C → copy selected text from terminal
                isMeta && e.keyCode == KeyEvent.VK_C -> {
                    try {
                        val selected = widget.selectedText
                        if (!selected.isNullOrEmpty()) {
                            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                            clipboard.setContents(StringSelection(selected), null)
                        }
                    } catch (_: Exception) {
                    }
                    e.consume()
                    true
                }

                else -> false
            }
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher)
        Disposer.register(disposable) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher)
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            Thread.sleep(1500)
            ApplicationManager.getApplication().invokeLater {
                try {
                    widget.terminalStarter?.sendString("claude\n", true)
                } catch (_: Exception) {
                }
            }
        }
    } catch (_: Exception) {
        val label = JLabel(
            "<html><center><br><br>Terminal is not available.<br><br>Make sure you have 'claude' installed and accessible in your PATH.</center></html>"
        )
        label.horizontalAlignment = SwingConstants.CENTER
        label.foreground = JBColor.LIGHT_GRAY
        panel.add(label, BorderLayout.CENTER)
    }

    return panel
}

private fun doCheckSuperClaudeInstalled(): Boolean {
    return try {
        val commandsDir = java.io.File(System.getProperty("user.home"), ".claude/commands")
        commandsDir.exists() && commandsDir.isDirectory
    } catch (_: Exception) {
        false
    }
}

private fun doCheckClaudeInstalled(): Boolean {
    // Use login shell so it picks up user's PATH (homebrew, nvm, etc.)
    // GUI-launched IDE doesn't inherit terminal PATH
    return try {
        val process = ProcessBuilder("bash", "-l", "-c", "which claude")
            .redirectErrorStream(true)
            .start()
        process.outputStream.close()
        val exitCode = process.waitFor()
        exitCode == 0
    } catch (_: Exception) {
        // Fallback: check common install locations directly
        val home = System.getProperty("user.home")
        listOf(
            "/usr/local/bin/claude",
            "/usr/bin/claude",
            "$home/.npm-global/bin/claude",
            "$home/.local/bin/claude",
        ).any { java.io.File(it).exists() }
    }
}
