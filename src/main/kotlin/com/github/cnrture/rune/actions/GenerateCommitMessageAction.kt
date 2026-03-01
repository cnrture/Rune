package com.github.cnrture.rune.actions

import com.github.cnrture.rune.common.ProcessRunner
import com.github.cnrture.rune.service.CliDiscoveryService
import com.github.cnrture.rune.settings.PluginSettingsService
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
import java.io.File

class GenerateCommitMessageAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitDocument = e.getData(com.intellij.openapi.vcs.VcsDataKeys.COMMIT_MESSAGE_DOCUMENT) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating commit message…", false) {
            override fun run(indicator: ProgressIndicator) {
                val projectDir = project.basePath ?: return
                val dir = File(projectDir)

                indicator.text = "Getting changes…"
                val staged = ProcessRunner.git(dir, "diff", "--cached", timeoutSeconds = 10)
                val unstaged = ProcessRunner.git(dir, "diff", timeoutSeconds = 10)
                val diff = (staged + "\n" + unstaged).trim()

                if (diff.isBlank()) {
                    ApplicationManager.getApplication().invokeLater {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RunePlugin")
                            .createNotification("No changes found in the repository.", NotificationType.WARNING)
                            .notify(project)
                    }
                    return
                }

                indicator.text = "Generating commit message with Claude…"
                val usedClaude = streamWithClaude(dir, diff, project, commitDocument)

                if (!usedClaude) {
                    setDocumentText(project, commitDocument, generateFallbackMessage(diff))
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.project != null && e.getData(com.intellij.openapi.vcs.VcsDataKeys.COMMIT_MESSAGE_DOCUMENT) != null
    }

    /** Returns true if claude was found and produced output. */
    private fun streamWithClaude(
        dir: File,
        diff: String,
        project: Project,
        commitDocument: Document,
    ): Boolean {
        val claudePath = CliDiscoveryService.findClaudeCli() ?: return false
        val truncatedDiff = if (diff.length > 8000) diff.take(8000) + "\n…(truncated)" else diff
        val promptTemplate = PluginSettingsService.getInstance(project).getCommitMessagePrompt()
        val prompt = promptTemplate.replace("{diff}", truncatedDiff)

        return try {
            val process = ProcessBuilder(claudePath, "-p", prompt)
                .directory(dir)
                .redirectErrorStream(true)
                .start()

            // Close stdin so the process cannot block waiting for input
            process.outputStream.close()

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

            readerThread.join(30_000) // 30 s hard timeout
            if (readerThread.isAlive) {
                process.destroyForcibly()
                readerThread.interrupt()
            }

            if (output.isNotBlank()) {
                setDocumentText(project, commitDocument, output.toString().trim())
                true
            } else {
                false
            }
        } catch (_: Exception) {
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

    private fun generateFallbackMessage(diff: String): String {
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

        return when {
            changedFiles.isEmpty() -> "$type: update changes"
            changedFiles.size == 1 -> "$type: update ${changedFiles.first()}"
            else -> "$type: update ${changedFiles.size} files"
        }
    }
}
