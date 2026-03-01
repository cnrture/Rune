package com.github.cnrture.rune.actions.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.common.ProcessRunner
import com.github.cnrture.rune.components.*
import com.github.cnrture.rune.theme.RTheme
import com.github.cnrture.rune.toolwindow.claude.ClaudeSessionService
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File

// region Data models

data class CommentThread(
    val id: Long,
    val path: String,
    val line: Int?,
    val body: String,
    val reviewer: String,
    val diffHunk: String,
    val replies: List<Reply> = emptyList(),
)

data class Reply(val user: String, val body: String)

enum class Phase { INPUT, RESULT }

private data class FixPRCommentsState(
    val phase: Phase = Phase.INPUT,
    val prUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val prTitle: String = "",
    val threads: List<CommentThread> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
)

// endregion

class FixPRCommentsDialog(
    private val project: Project,
    private val ghPath: String,
    private val dir: File,
) : RDialogWrapper(
    width = 800,
    height = 600,
) {

    private var state = mutableStateOf(FixPRCommentsState())

    private val prUrlRegex = Regex("""https?://github\.com/([^/]+)/([^/]+)/pull/(\d+)""")

    init {
        title = "Fix PR Comments"
    }

    // region GitHub API

    private fun fetchPRComments() {
        val url = state.value.prUrl.trim()
        val match = prUrlRegex.find(url)
        if (match == null) {
            state.value = state.value.copy(errorMessage = "Invalid PR URL. Expected: https://github.com/owner/repo/pull/123")
            return
        }

        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        val number = match.groupValues[3].toInt()

        state.value = state.value.copy(isLoading = true, errorMessage = null, threads = emptyList())

        kotlin.concurrent.thread {
            try {
                // Fetch PR title
                val prTitle = runProcess(
                    ghPath, "pr", "view", number.toString(),
                    "--repo", "$owner/$repo",
                    "--json", "title", "--jq", ".title",
                )

                // Fetch unresolved review threads via GraphQL
                val query = """
                    {
                      repository(owner: "$owner", name: "$repo") {
                        pullRequest(number: $number) {
                          reviewThreads(first: 100) {
                            nodes {
                              isResolved
                              comments(first: 10) {
                                nodes {
                                  body
                                  path
                                  line
                                  originalLine
                                  diffHunk
                                  author { login }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                """.trimIndent()

                val graphqlOutput = runProcess(ghPath, "api", "graphql", "-f", "query=$query")
                val threads = parseThreads(graphqlOutput)

                state.value = state.value.copy(
                    isLoading = false,
                    prTitle = prTitle,
                    threads = threads,
                    selectedIds = threads.map { it.id }.toSet(),
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to fetch PR comments: ${e.message}",
                )
            }
        }
    }

    private fun parseThreads(json: String): List<CommentThread> {
        val root = JsonParser.parseString(json).asJsonObject
        val threads = root
            .getAsJsonObject("data")
            .getAsJsonObject("repository")
            .getAsJsonObject("pullRequest")
            .getAsJsonObject("reviewThreads")
            .getAsJsonArray("nodes")

        val result = mutableListOf<CommentThread>()
        var idCounter = 1L

        for (threadElement in threads) {
            val thread = threadElement.asJsonObject
            if (thread.get("isResolved").asBoolean) continue

            val comments = thread.getAsJsonObject("comments").getAsJsonArray("nodes")
            if (comments.size() == 0) continue

            val first = comments[0].asJsonObject
            val path = first.get("path")?.asString ?: ""
            val line = when {
                first.has("line") && !first.get("line").isJsonNull -> first.get("line").asInt
                first.has("originalLine") && !first.get("originalLine").isJsonNull -> first.get("originalLine").asInt
                else -> null
            }
            val body = first.get("body")?.asString ?: ""
            val reviewer = first.getAsJsonObject("author")?.get("login")?.asString ?: "unknown"
            val diffHunk = first.get("diffHunk")?.asString ?: ""

            val replies = (1 until comments.size()).map { i ->
                val reply = comments[i].asJsonObject
                Reply(
                    user = reply.getAsJsonObject("author")?.get("login")?.asString ?: "unknown",
                    body = reply.get("body")?.asString ?: "",
                )
            }

            result.add(
                CommentThread(
                    id = idCounter++,
                    path = path,
                    line = line,
                    body = body,
                    reviewer = reviewer,
                    diffHunk = diffHunk,
                    replies = replies,
                )
            )
        }

        return result
    }

    private fun runProcess(vararg cmd: String): String {
        return ProcessRunner.runOrThrow(dir, *cmd)
    }

    // endregion

    // region Claude integration

    private fun fixWithClaude() {
        val selected = state.value.threads.filter { it.id in state.value.selectedIds }
        if (selected.isEmpty()) return

        val url = state.value.prUrl.trim()
        val prompt = buildString {
            appendLine("Please fix the following unresolved PR review comments from $url")
            appendLine("For each comment, read the file, understand the feedback, and make the fix.")
            appendLine()

            selected.forEachIndexed { index, thread ->
                appendLine("--- Comment ${index + 1}/${selected.size} ---")
                append("File: ${thread.path}")
                if (thread.line != null) append(":${thread.line}")
                appendLine()
                appendLine("Reviewer: @${thread.reviewer}")
                appendLine("Comment: \"${thread.body}\"")
                if (thread.replies.isNotEmpty()) {
                    appendLine("Discussion:")
                    thread.replies.forEach { reply ->
                        appendLine("  @${reply.user}: \"${reply.body}\"")
                    }
                }
                if (thread.diffHunk.isNotBlank()) {
                    appendLine("Code context:")
                    appendLine("```")
                    appendLine(thread.diffHunk)
                    appendLine("```")
                }
                appendLine()
            }

            appendLine("After all fixes, provide a summary of changes per comment.")
        }

        val service = ClaudeSessionService.getInstance(project)
        service.ensureSession()
        service.sendToTerminal(prompt, autoRun = true)

        state.value = state.value.copy(phase = Phase.RESULT)
    }

    private fun openClaudeTerminal() {
        close(0)
        ToolWindowManager.getInstance(project).getToolWindow("Claude")?.show()
    }

    // endregion

    // region UI

    @Composable
    override fun createDesign() {
        val currentState by remember { state }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = RTheme.colors.black,
        ) {
            when (currentState.phase) {
                Phase.INPUT -> InputPhase(currentState)
                Phase.RESULT -> ResultPhase(currentState)
            }
        }
    }

    @Composable
    private fun InputPhase(currentState: FixPRCommentsState) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            // Title
            RText(
                modifier = Modifier.fillMaxWidth(),
                text = "Fix PR Comments",
                style = TextStyle(
                    color = RTheme.colors.blue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.size(20.dp))

            // URL input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RTextField(
                    modifier = Modifier.weight(1f),
                    value = currentState.prUrl,
                    onValueChange = { state.value = state.value.copy(prUrl = it, errorMessage = null) },
                    placeholder = "https://github.com/owner/repo/pull/123",
                )
                Spacer(modifier = Modifier.size(12.dp))
                RActionCard(
                    title = "Fetch",
                    icon = Icons.Rounded.Search,
                    actionColor = RTheme.colors.blue,
                    type = RActionCardType.MEDIUM,
                    onClick = { fetchPRComments() },
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Error banner
            currentState.errorMessage?.let { error ->
                RErrorBanner(error = error, onRetry = { fetchPRComments() })
                Spacer(modifier = Modifier.size(12.dp))
            }

            // Loading
            if (currentState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RTheme.colors.blue)
                        Spacer(modifier = Modifier.size(16.dp))
                        RText(
                            text = "Fetching PR comments...",
                            color = RTheme.colors.lightGray,
                            style = TextStyle(fontSize = 14.sp),
                        )
                    }
                }
            } else if (currentState.threads.isNotEmpty()) {
                // PR title
                if (currentState.prTitle.isNotBlank()) {
                    RText(
                        text = currentState.prTitle,
                        color = RTheme.colors.white,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                // Select all / deselect all toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val allSelected = currentState.selectedIds.size == currentState.threads.size
                    RCheckbox(
                        checked = allSelected,
                        label = if (allSelected) "Deselect All" else "Select All (${currentState.threads.size})",
                        onCheckedChange = {
                            state.value = if (allSelected) {
                                state.value.copy(selectedIds = emptySet())
                            } else {
                                state.value.copy(selectedIds = currentState.threads.map { it.id }.toSet())
                            }
                        },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    RText(
                        text = "${currentState.selectedIds.size} selected",
                        color = RTheme.colors.hintGray,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                // Comment thread list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(currentState.threads, key = { it.id }) { thread ->
                        CommentThreadCard(
                            thread = thread,
                            isSelected = thread.id in currentState.selectedIds,
                            onToggle = {
                                val ids = state.value.selectedIds
                                state.value = state.value.copy(
                                    selectedIds = if (thread.id in ids) ids - thread.id else ids + thread.id,
                                )
                            },
                        )
                    }
                }
            } else {
                // Empty state
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Bottom bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RActionCard(
                    title = "Cancel",
                    icon = Icons.Rounded.Cancel,
                    actionColor = RTheme.colors.lightGray,
                    type = RActionCardType.MEDIUM,
                    onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                )
                if (currentState.threads.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(12.dp))
                    val count = currentState.selectedIds.size
                    RActionCard(
                        title = "Fix $count Comment${if (count != 1) "s" else ""} with Claude",
                        icon = Icons.Rounded.CheckCircle,
                        actionColor = if (count > 0) RTheme.colors.blue else RTheme.colors.gray,
                        type = RActionCardType.MEDIUM,
                        onClick = { if (count > 0) fixWithClaude() },
                    )
                }
            }
        }
    }

    @Composable
    private fun ResultPhase(currentState: FixPRCommentsState) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Success banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF69F0AE).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RText(
                    text = "Sent to Claude!",
                    color = Color(0xFF69F0AE),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.size(12.dp))
                if (currentState.prTitle.isNotBlank()) {
                    RText(
                        text = currentState.prTitle,
                        color = RTheme.colors.white,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                RText(
                    text = currentState.prUrl,
                    color = RTheme.colors.hintGray,
                    style = TextStyle(fontSize = 12.sp),
                )
                Spacer(modifier = Modifier.size(12.dp))

                val selectedCount = currentState.selectedIds.size
                RText(
                    text = "$selectedCount comment${if (selectedCount != 1) "s" else ""} sent for fixing",
                    color = RTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp),
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Summary of sent comments
                val selected = currentState.threads.filter { it.id in currentState.selectedIds }
                selected.forEach { thread ->
                    RText(
                        modifier = Modifier.fillMaxWidth(),
                        text = "${thread.path}${if (thread.line != null) ":${thread.line}" else ""} - @${thread.reviewer}",
                        color = RTheme.colors.hintGray,
                        style = TextStyle(fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                RActionCard(
                    title = "Close",
                    icon = Icons.Rounded.Cancel,
                    actionColor = RTheme.colors.lightGray,
                    type = RActionCardType.MEDIUM,
                    onClick = { close(0) },
                )
                Spacer(modifier = Modifier.size(12.dp))
                RActionCard(
                    title = "Open Claude Terminal",
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    actionColor = RTheme.colors.blue,
                    type = RActionCardType.MEDIUM,
                    onClick = { openClaudeTerminal() },
                )
            }
        }
    }

    @Composable
    private fun CommentThreadCard(
        thread: CommentThread,
        isSelected: Boolean,
        onToggle: () -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isSelected) RTheme.colors.blue.copy(alpha = 0.5f) else RTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp),
                )
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) RTheme.colors.blue.copy(alpha = 0.05f) else RTheme.colors.black,
                )
                .padding(12.dp),
        ) {
            // Header row: checkbox + file path + reviewer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                )
                Spacer(modifier = Modifier.size(8.dp))
                RText(
                    modifier = Modifier.weight(1f),
                    text = "${thread.path}${if (thread.line != null) ":${thread.line}" else ""}",
                    color = RTheme.colors.blue,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(8.dp))
                RText(
                    text = "@${thread.reviewer}",
                    color = RTheme.colors.hintGray,
                    style = TextStyle(fontSize = 11.sp),
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Comment body
            RText(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                text = thread.body,
                color = RTheme.colors.white,
                style = TextStyle(fontSize = 12.sp),
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )

            // Replies
            if (thread.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.size(6.dp))
                thread.replies.forEach { reply ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                    ) {
                        RText(
                            text = "@${reply.user}: ",
                            color = RTheme.colors.hintGray,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        )
                        RText(
                            text = reply.body,
                            color = RTheme.colors.lightGray,
                            style = TextStyle(fontSize = 11.sp),
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Expandable diff hunk
            if (thread.diffHunk.isNotBlank()) {
                Spacer(modifier = Modifier.size(6.dp))
                Row(
                    modifier = Modifier
                        .padding(start = 36.dp)
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RText(
                        text = if (expanded) "Hide code context" else "Show code context",
                        color = RTheme.colors.hintGray,
                        style = TextStyle(fontSize = 11.sp),
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    RText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, top = 4.dp)
                            .background(
                                color = RTheme.colors.gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(8.dp),
                        text = thread.diffHunk,
                        color = RTheme.colors.lightGray,
                        style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }


    // endregion
}
