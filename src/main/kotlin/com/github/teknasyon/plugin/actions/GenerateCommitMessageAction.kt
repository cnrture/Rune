package com.github.teknasyon.plugin.actions

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
import java.util.concurrent.TimeUnit

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
                            .getNotificationGroup("TeknasyonAndroidStudioPlugin")
                            .createNotification("No changes found in the repository.", NotificationType.WARNING)
                            .notify(project)
                    }
                    return
                }

                val jiraUrl = getJiraTicketUrl(projectDir)

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
        val claudePath = findClaudeCli() ?: return false
        val truncatedDiff = if (diff.length > 8000) diff.take(8000) + "\n…(truncated)" else diff
        val prompt = "Based on the following git diff, write a single conventional commit message " +
            "(format: type: description). Output only the commit message, nothing else.\n\n$truncatedDiff"

        return try {
            val process = ProcessBuilder(claudePath, "-p", prompt)
                .directory(File(projectDir))
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
                val finalMessage = buildString {
                    append(output.toString().trim())
                    if (jiraUrl != null) append("\n$jiraUrl")
                }
                setDocumentText(project, commitDocument, finalMessage)
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

    private fun getAllChanges(projectDir: String): String {
        val dir = File(projectDir)
        val staged = runGit(dir, "diff", "--cached")
        val unstaged = runGit(dir, "diff")
        return (staged + "\n" + unstaged).trim()
    }

    private fun runGit(dir: File, vararg args: String): String {
        return try {
            // Use login shell so git is found even with minimal PATH
            val cmd = listOf("git") + args.toList()
            val process = ProcessBuilder(cmd)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(10, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun findClaudeCli(): String? {
        // Use a login shell so it picks up the user's PATH (nvm, homebrew, etc.)
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which claude")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(5, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            // Fallback: check common install locations directly
            val home = System.getProperty("user.home")
            listOf(
                "/usr/local/bin/claude",
                "/usr/bin/claude",
                "$home/.npm-global/bin/claude",
                "$home/.local/bin/claude",
                "$home/.nvm/versions/node/$(ls $home/.nvm/versions/node 2>/dev/null | tail -1)/bin/claude",
            ).firstOrNull { File(it).exists() }
        }
    }

    private fun getJiraTicketUrl(projectDir: String): String? {
        val branch = runGit(File(projectDir), "rev-parse", "--abbrev-ref", "HEAD")
        val ticketId = Regex("[A-Z]+-\\d+").find(branch)?.value ?: return null
        return "https://pozitim.atlassian.net/browse/$ticketId"
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
