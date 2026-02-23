package com.github.teknasyon.plugin.domain.usecase

import com.github.teknasyon.plugin.domain.model.ReviewChange
import com.github.teknasyon.plugin.domain.model.ReviewTask
import com.github.teknasyon.plugin.domain.model.TaskStatus
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

class ProcessReviewCommentsUseCase(private val project: Project) {

    fun execute(
        prUrl: String,
        onProgress: (String) -> Unit,
        onTaskUpdate: (ReviewTask) -> Unit,
        onChangeRecorded: (ReviewChange) -> Unit,
    ): Result<List<ReviewChange>> {
        return try {
            val (repo, prNumber) = parsePrUrl(prUrl)
                ?: return Result.failure(IllegalArgumentException("Geçersiz PR URL: $prUrl"))

            val ghPath = findGhCli()
                ?: return Result.failure(IllegalStateException("GitHub CLI (gh) bulunamadı. 'gh auth login' ile giriş yapın."))

            val claudePath = findClaudeCli()
                ?: return Result.failure(IllegalStateException("Claude CLI bulunamadı. 'npm install -g @anthropic-ai/claude-code' ile kurun."))

            onProgress("PR review comment'leri çekiliyor...")
            val reviewJson = runProcess(
                File(project.basePath ?: "."),
                ghPath,
                "pr", "view", prNumber,
                "--repo", repo,
                "--json", "reviewThreads"
            )

            if (reviewJson.isBlank()) {
                return Result.failure(IllegalStateException("PR bilgileri alınamadı. PR URL'ini ve gh auth login'i kontrol edin."))
            }

            val unresolvedComments = parseUnresolvedComments(reviewJson)
            if (unresolvedComments.isEmpty()) {
                return Result.success(emptyList())
            }

            onProgress("${unresolvedComments.size} unresolved comment bulundu. Task listesi oluşturuluyor...")

            val tasksPrompt = buildTaskListPrompt(unresolvedComments)
            val taskListOutput = runClaude(claudePath, tasksPrompt, project.basePath ?: ".")

            val tasks = parseTaskList(taskListOutput)
            if (tasks.isEmpty()) {
                return Result.failure(IllegalStateException("Claude'dan task listesi alınamadı."))
            }

            tasks.forEach { onTaskUpdate(it) }

            val changes = mutableListOf<ReviewChange>()
            val projectBase = project.basePath ?: ""

            for (task in tasks) {
                val updatedTask = task.copy(status = TaskStatus.IN_PROGRESS)
                onTaskUpdate(updatedTask)
                onProgress("'${task.title}' işleniyor...")

                try {
                    val filePath = "$projectBase/${task.filePath}".replace("//", "/")
                    val file = File(filePath)
                    val beforeContent = if (file.exists()) file.readText() else ""

                    val fixPrompt = buildFixPrompt(task, beforeContent)
                    val afterContent = runClaude(claudePath, fixPrompt, projectBase)
                    val cleanedAfter = stripMarkdownCodeBlocks(afterContent)

                    if (cleanedAfter.isNotBlank() && cleanedAfter != beforeContent) {
                        file.parentFile?.mkdirs()
                        file.writeText(cleanedAfter)
                        val change = ReviewChange(
                            taskId = task.id,
                            taskTitle = task.title,
                            filePath = task.filePath,
                            before = beforeContent,
                            after = cleanedAfter,
                        )
                        changes.add(change)
                        onChangeRecorded(change)
                        onTaskUpdate(task.copy(status = TaskStatus.DONE))
                    } else {
                        onTaskUpdate(task.copy(status = TaskStatus.DONE))
                    }
                } catch (_: Exception) {
                    onTaskUpdate(task.copy(status = TaskStatus.FAILED))
                }
            }

            Result.success(changes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parsePrUrl(url: String): Pair<String, String>? {
        val regex = Regex("github\\.com/([^/]+/[^/]+)/pull/(\\d+)")
        val match = regex.find(url) ?: return null
        return Pair(match.groupValues[1], match.groupValues[2])
    }

    private fun parseUnresolvedComments(json: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val threads = root.getAsJsonArray("reviewThreads") ?: return result
            for (threadEl in threads) {
                val thread = threadEl.asJsonObject
                if (thread.get("isResolved")?.asBoolean == true) continue
                val filePath = thread.get("path")?.asString ?: ""
                val comments = thread.getAsJsonArray("comments") ?: continue
                val commentBodies = StringBuilder()
                for (commentEl in comments) {
                    val body = commentEl.asJsonObject.get("body")?.asString ?: ""
                    if (body.isNotBlank()) commentBodies.append(body).append("\n")
                }
                if (filePath.isNotBlank() && commentBodies.isNotBlank()) {
                    result.add(Pair(filePath, commentBodies.toString().trim()))
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun buildTaskListPrompt(comments: List<Pair<String, String>>): String {
        val commentSection = comments.mapIndexed { index, (file, body) ->
            "Comment ${index + 1}:\nFile: $file\n$body"
        }.joinToString("\n\n")

        return """You are analyzing GitHub PR review comments.

[UNRESOLVED COMMENTS]
$commentSection

Create a task list in this exact format (no extra text, just the tasks):
TASK 1:
TITLE: <short title>
FILE: <file path>
DESCRIPTION: <what to change>
---
TASK 2:
TITLE: <short title>
FILE: <file path>
DESCRIPTION: <what to change>
---"""
    }

    private fun buildFixPrompt(task: ReviewTask, fileContent: String): String {
        return """Fix this code review comment.
File: ${task.filePath}
Task: ${task.description}
Current content:
${fileContent.take(6000)}
Output ONLY the complete modified file content. No explanations, no markdown code blocks."""
    }

    fun parseTaskList(output: String): List<ReviewTask> {
        val tasks = mutableListOf<ReviewTask>()
        val blocks = output.split("---").map { it.trim() }.filter { it.isNotBlank() }
        var idCounter = 1

        for (block in blocks) {
            val titleMatch = Regex("TITLE:\\s*(.+)").find(block)
            val fileMatch = Regex("FILE:\\s*(.+)").find(block)
            val descMatch = Regex("DESCRIPTION:\\s*([\\s\\S]+)").find(block)

            val title = titleMatch?.groupValues?.get(1)?.trim() ?: continue
            val file = fileMatch?.groupValues?.get(1)?.trim() ?: continue
            val desc = descMatch?.groupValues?.get(1)?.trim() ?: ""

            tasks.add(
                ReviewTask(
                    id = idCounter++,
                    title = title,
                    filePath = file,
                    description = desc,
                )
            )
        }
        return tasks
    }

    fun stripMarkdownCodeBlocks(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("```")) {
            val firstNewline = trimmed.indexOf('\n')
            if (firstNewline < 0) return trimmed
            val withoutFence = trimmed.substring(firstNewline + 1)
            return if (withoutFence.trimEnd().endsWith("```")) {
                withoutFence.trimEnd().dropLast(3).trimEnd()
            } else {
                withoutFence
            }
        }
        return trimmed
    }

    private fun runClaude(claudePath: String, prompt: String, workDir: String): String {
        return try {
            val process = ProcessBuilder(claudePath, "-p", prompt)
                .directory(File(workDir))
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            val output = StringBuilder()
            val reader = Thread {
                try {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        output.append(line).append("\n")
                    }
                } catch (_: Exception) {
                }
            }.also { it.isDaemon = true; it.start() }
            reader.join(60_000)
            if (reader.isAlive) {
                process.destroyForcibly()
                reader.interrupt()
            }
            output.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun findClaudeCli(): String? {
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which claude")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(5, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            val home = System.getProperty("user.home")
            listOf(
                "/usr/local/bin/claude",
                "/usr/bin/claude",
                "$home/.npm-global/bin/claude",
                "$home/.local/bin/claude",
            ).firstOrNull { File(it).exists() }
        }
    }

    private fun findGhCli(): String? {
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which gh")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(5, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            listOf("/usr/local/bin/gh", "/usr/bin/gh", "/opt/homebrew/bin/gh")
                .firstOrNull { File(it).exists() }
        }
    }

    private fun runProcess(dir: File, vararg cmd: String): String {
        return try {
            val process = ProcessBuilder(*cmd)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(30, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
            ""
        }
    }
}
