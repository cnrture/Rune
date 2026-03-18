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
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.common.AppIcons
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.components.*
import com.github.cnrture.rune.service.VcsPlatformService
import com.github.cnrture.rune.theme.TPTheme
import com.github.cnrture.rune.toolwindow.ClaudeSessionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

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
    private val platformService: VcsPlatformService,
) : TPDialogWrapper(
    width = 800,
    height = 600,
) {

    private var state = mutableStateOf(FixPRCommentsState())

    init {
        title = "Fix PR Comments"
    }

    // region VCS API

    private fun fetchPRComments() {
        val url = state.value.prUrl.trim()
        val prIdentifier = platformService.parsePrUrl(url)
        if (prIdentifier == null) {
            state.value = state.value.copy(
                errorMessage = "Invalid PR URL. Expected format: ${platformService.getPlaceholderUrl()}"
            )
            return
        }

        state.value = state.value.copy(isLoading = true, errorMessage = null, threads = emptyList())

        kotlin.concurrent.thread {
            platformService.fetchPrTitle(prIdentifier).fold(
                onSuccess = { prTitle ->
                    platformService.fetchUnresolvedComments(prIdentifier).fold(
                        onSuccess = { threads ->
                            state.value = state.value.copy(
                                isLoading = false,
                                prTitle = prTitle,
                                threads = threads,
                                selectedIds = threads.map { it.id }.toSet(),
                            )
                        },
                        onFailure = { e ->
                            state.value = state.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to fetch PR comments: ${e.message}",
                            )
                        }
                    )
                },
                onFailure = { e ->
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to fetch PR title: ${e.message}",
                    )
                }
            )
        }
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
            color = TPTheme.colors.black,
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
            TPText(
                modifier = Modifier.fillMaxWidth(),
                text = "Fix PR Comments",
                style = TextStyle(
                    color = TPTheme.colors.blue,
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
                TPTextField(
                    modifier = Modifier.weight(1f),
                    value = currentState.prUrl,
                    onValueChange = { state.value = state.value.copy(prUrl = it, errorMessage = null) },
                    placeholder = platformService.getPlaceholderUrl(),
                )
                Spacer(modifier = Modifier.size(12.dp))
                TPActionCard(
                    title = "Fetch",
                    icon = AppIcons.painter("search"),
                    actionColor = TPTheme.colors.blue,
                    type = TPActionCardType.MEDIUM,
                    onClick = { fetchPRComments() },
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Error banner
            currentState.errorMessage?.let { error ->
                ErrorBanner(error)
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
                        CircularProgressIndicator(color = TPTheme.colors.blue)
                        Spacer(modifier = Modifier.size(16.dp))
                        TPText(
                            text = "Fetching PR comments...",
                            color = TPTheme.colors.lightGray,
                            style = TextStyle(fontSize = 14.sp),
                        )
                    }
                }
            } else if (currentState.threads.isNotEmpty()) {
                // PR title
                if (currentState.prTitle.isNotBlank()) {
                    TPText(
                        text = currentState.prTitle,
                        color = TPTheme.colors.white,
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
                    TPCheckbox(
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
                    TPText(
                        text = "${currentState.selectedIds.size} selected",
                        color = TPTheme.colors.hintGray,
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
                TPActionCard(
                    title = "Cancel",
                    icon = AppIcons.painter("cancel"),
                    actionColor = TPTheme.colors.lightGray,
                    type = TPActionCardType.MEDIUM,
                    onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                )
                if (currentState.threads.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(12.dp))
                    val count = currentState.selectedIds.size
                    TPActionCard(
                        title = "Fix $count Comment${if (count != 1) "s" else ""} with Claude",
                        icon = AppIcons.painter("check_circle"),
                        actionColor = if (count > 0) TPTheme.colors.blue else TPTheme.colors.gray,
                        type = TPActionCardType.MEDIUM,
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
                        color = TPTheme.colors.green.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TPText(
                    text = "Sent to Claude!",
                    color = TPTheme.colors.green,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.size(12.dp))
                if (currentState.prTitle.isNotBlank()) {
                    TPText(
                        text = currentState.prTitle,
                        color = TPTheme.colors.white,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                TPText(
                    text = currentState.prUrl,
                    color = TPTheme.colors.hintGray,
                    style = TextStyle(fontSize = 12.sp),
                )
                Spacer(modifier = Modifier.size(12.dp))

                val selectedCount = currentState.selectedIds.size
                TPText(
                    text = "$selectedCount comment${if (selectedCount != 1) "s" else ""} sent for fixing",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp),
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Summary of sent comments
                val selected = currentState.threads.filter { it.id in currentState.selectedIds }
                selected.forEach { thread ->
                    TPText(
                        modifier = Modifier.fillMaxWidth(),
                        text = "${thread.path}${if (thread.line != null) ":${thread.line}" else ""} - @${thread.reviewer}",
                        color = TPTheme.colors.hintGray,
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
                TPActionCard(
                    title = "Close",
                    icon = AppIcons.painter("cancel"),
                    actionColor = TPTheme.colors.lightGray,
                    type = TPActionCardType.MEDIUM,
                    onClick = { close(0) },
                )
                Spacer(modifier = Modifier.size(12.dp))
                TPActionCard(
                    title = "Open Claude Terminal",
                    icon = AppIcons.painter("open_in_new"),
                    actionColor = TPTheme.colors.blue,
                    type = TPActionCardType.MEDIUM,
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
                    color = if (isSelected) TPTheme.colors.blue.copy(alpha = 0.5f) else TPTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp),
                )
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) TPTheme.colors.blue.copy(alpha = 0.05f) else TPTheme.colors.black,
                )
                .padding(12.dp),
        ) {
            // Header row: checkbox + file path + reviewer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TPCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                )
                Spacer(modifier = Modifier.size(8.dp))
                TPText(
                    modifier = Modifier.weight(1f),
                    text = "${thread.path}${if (thread.line != null) ":${thread.line}" else ""}",
                    color = TPTheme.colors.blue,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(8.dp))
                TPText(
                    text = "@${thread.reviewer}",
                    color = TPTheme.colors.hintGray,
                    style = TextStyle(fontSize = 11.sp),
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Comment body
            TPText(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                text = thread.body,
                color = TPTheme.colors.white,
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
                        TPText(
                            text = "@${reply.user}: ",
                            color = TPTheme.colors.hintGray,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        )
                        TPText(
                            text = reply.body,
                            color = TPTheme.colors.lightGray,
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
                    TPText(
                        text = if (expanded) "Hide code context" else "Show code context",
                        color = TPTheme.colors.hintGray,
                        style = TextStyle(fontSize = 11.sp),
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    TPText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, top = 4.dp)
                            .background(
                                color = TPTheme.colors.gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(8.dp),
                        text = thread.diffHunk,
                        color = TPTheme.colors.lightGray,
                        style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }

    @Composable
    private fun ErrorBanner(error: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = TPTheme.colors.red.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        ) {
            TPText(
                text = error,
                color = TPTheme.colors.red,
                style = TextStyle(fontSize = 12.sp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            TPActionCard(
                title = "Retry",
                icon = AppIcons.painter("refresh"),
                actionColor = TPTheme.colors.red,
                type = TPActionCardType.SMALL,
                onClick = { fetchPRComments() },
            )
        }
    }

    // endregion
}
