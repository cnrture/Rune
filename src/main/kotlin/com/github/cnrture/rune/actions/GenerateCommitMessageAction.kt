package com.github.cnrture.rune.actions

import com.github.cnrture.rune.common.CliUtils
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.service.PluginSettingsService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import java.io.File

class GenerateCommitMessageAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitDocument = e.getData(VcsDataKeys.COMMIT_MESSAGE_DOCUMENT) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating commit message…", false) {
            override fun run(indicator: ProgressIndicator) {
                val projectDir = project.basePath ?: return

                indicator.text = "Getting changes…"
                val diff = getAllChanges(projectDir)

                if (diff.isBlank()) {
                    ApplicationManager.getApplication().invokeLater {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RunePlugin")
                            .createNotification("No changes found in the repository.", NotificationType.WARNING)
                            .notify(project)
                    }
                    return
                }

                val jiraUrl = if (PluginSettingsService.getInstance(project).isIncludeJiraUrlInCommit()) {
                    getJiraTicketUrl(projectDir)
                } else {
                    null
                }

                indicator.text = "Generating commit message with Claude…"
                val usedClaude = streamWithClaude(projectDir, diff, jiraUrl, project, commitDocument)

                if (!usedClaude) {
                    setDocumentText(project, commitDocument, generateFallbackMessage(diff, jiraUrl))
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.project != null && e.getData(VcsDataKeys.COMMIT_MESSAGE_DOCUMENT) != null
    }

    /** Returns true if claude was found and produced output. */
    private fun streamWithClaude(
        projectDir: String,
        diff: String,
        jiraUrl: String?,
        project: Project,
        commitDocument: Document,
    ): Boolean {
        val claudePath = findClaudeCli()
        if (claudePath == null) {
            notify(project, "Claude CLI not found. Install it or check your PATH.", NotificationType.WARNING)
            return false
        }

        val truncatedDiff = if (diff.length > 8000) diff.take(8000) + "\n…(truncated)" else diff
        val promptTemplate = PluginSettingsService.getInstance(project).getCommitMessagePrompt()
        val prompt = promptTemplate.replace("{diff}", truncatedDiff)

        return try {
            val pb = ProcessBuilder(claudePath, "-p", "--output-format", "text", "-")
                .directory(File(projectDir))
                .redirectErrorStream(true)
            CliUtils.getLoginShellPath()?.let { shellPath ->
                pb.environment()["PATH"] = shellPath
            }
            val process = pb.start()

            // Send prompt via stdin to avoid command-line length issues
            process.outputStream.bufferedWriter().use { it.write(prompt) }

            val output = StringBuilder()

            // Read in a daemon thread so we can apply a hard timeout
            val readerThread = Thread {
                try {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        output.append(line).append("\n")
                        setDocumentText(project, commitDocument, output.toString().trim())
                    }
                } catch (_: Exception) {
                }
            }.also { it.isDaemon = true; it.start() }

            readerThread.join(Constants.TIMEOUT_CLAUDE_STREAM_MS)
            if (readerThread.isAlive) {
                process.destroyForcibly()
                readerThread.interrupt()
                notify(project, "Claude timed out after ${Constants.TIMEOUT_CLAUDE_STREAM_MS / 1000}s.", NotificationType.WARNING)
            }

            if (output.isNotBlank()) {
                val finalMessage = buildString {
                    append(output.toString().trim())
                    if (jiraUrl != null) append("\n$jiraUrl")
                }
                setDocumentText(project, commitDocument, finalMessage)
                true
            } else {
                notify(project, "Claude returned empty output.", NotificationType.WARNING)
                false
            }
        } catch (e: Exception) {
            notify(project, "Claude error: ${e.message}", NotificationType.WARNING)
            false
        }
    }

    private fun setDocumentText(project: Project, document: Document, text: String) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(text)
            }
        }
    }

    private fun getAllChanges(projectDir: String): String {
        val dir = File(projectDir)
        val staged = runGit(dir, "diff", "--cached")
        val unstaged = runGit(dir, "diff")
        return (staged + "\n" + unstaged).trim()
    }

    private fun runGit(dir: File, vararg args: String): String = CliUtils.runGit(dir, *args)

    private fun findClaudeCli(): String? = CliUtils.findClaudeCli()

    private fun notify(project: Project, message: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("RunePlugin")
                .createNotification(message, type)
                .notify(project)
        }
    }

    private fun getJiraTicketUrl(projectDir: String): String? {
        val branch = CliUtils.runGit(File(projectDir), "rev-parse", "--abbrev-ref", "HEAD")
        val ticketId = Constants.JIRA_TICKET_REGEX.find(branch)?.value?.uppercase() ?: return null
        return Constants.jiraBrowseUrl(ticketId)
    }

    private fun generateFallbackMessage(diff: String, jiraUrl: String? = null): String {
        val changedFiles = diff.lines()
            .filter { it.startsWith("diff --git") }
            .mapNotNull { it.split(" ").lastOrNull()?.split("/")?.lastOrNull() }

        val added = diff.lines().count { it.startsWith("+") && !it.startsWith("+++") }
        val removed = diff.lines().count { it.startsWith("-") && !it.startsWith("---") }

        val type = when {
            changedFiles.any { it.contains("test", ignoreCase = true) } -> "test"
            changedFiles.any { it.endsWith(".md") } -> "docs"
            removed > added * 2 -> "refactor"
            added > removed * 2 -> "feat"
            else -> "fix"
        }

        val base = when {
            changedFiles.isEmpty() -> "$type: update changes"
            changedFiles.size == 1 -> "$type: update ${changedFiles.first()}"
            else -> "$type: update ${changedFiles.size} files"
        }
        return if (jiraUrl != null) "$base\n\n$jiraUrl" else base
    }
}
