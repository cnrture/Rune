package com.github.cnrture.rune.actions.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.components.*
import com.github.cnrture.rune.service.GitHubCacheService
import com.github.cnrture.rune.service.JiraService
import com.github.cnrture.rune.theme.RTheme
import java.io.File
import java.util.concurrent.TimeUnit

data class PRDialogState(
    val isLoading: Boolean = true,
    val collaborators: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val selectedReviewers: Set<String> = emptySet(),
    val selectedLabels: Set<String> = emptySet(),
    val reviewerFilter: String = "",
    val labelFilter: String = "",
    val errorMessage: String? = null,
    val isCreatingLabel: Boolean = false,
)

class CreatePRDialog(
    private val ghPath: String,
    private val dir: File,
    private val owner: String,
    private val repo: String,
    private val ticketId: String? = null,
    private val onConfirm: (reviewers: List<String>, labels: List<String>) -> Unit,
) : RDialogWrapper(
    width = 600,
    height = 500,
) {

    private var state = mutableStateOf(PRDialogState())

    private val cacheService = GitHubCacheService.getInstance()

    init {
        title = "Create Review PR"
        loadData()
    }

    private fun loadData() {
        val cached = cacheService.getRepoCache(owner, repo)
        if (cached != null) {
            state.value = state.value.copy(
                isLoading = false,
                collaborators = cached.collaborators.sorted(),
                labels = cached.labels.sorted(),
            )
            autoSelectLabels(cached.labels)
        } else {
            fetchGitHubData()
        }
    }

    private fun autoSelectLabels(labels: List<String>) {
        if (ticketId == null) return

        // Team label mapping: ticket prefix -> label name
        val teamLabelMap = mapOf(
            "GR" to "revenue",
            "COM" to "spam-protection",
        )

        val prefix = ticketId.substringBefore("-")
        val teamLabel = teamLabelMap[prefix.uppercase()]
        if (teamLabel != null) {
            val match = labels.firstOrNull { it.equals(teamLabel, ignoreCase = true) }
            if (match != null) {
                state.value = state.value.copy(
                    selectedLabels = state.value.selectedLabels + match,
                )
            }
        }

        // Fix version from Jira
        if (!JiraService.hasCredentials()) return
        kotlin.concurrent.thread {
            val fixVersions = JiraService.fetchFixVersions(ticketId)
            if (fixVersions.isNotEmpty()) {
                val matchingLabels = labels.filter { label ->
                    fixVersions.any { it.contains(label, ignoreCase = true) }
                }.toSet()
                if (matchingLabels.isNotEmpty()) {
                    state.value = state.value.copy(
                        selectedLabels = state.value.selectedLabels + matchingLabels,
                    )
                }
            }
        }
    }

    private fun fetchGitHubData() {
        state.value = state.value.copy(isLoading = true, errorMessage = null)
        kotlin.concurrent.thread {
            var collaborators: List<String> = emptyList()
            var labels: List<String> = emptyList()
            var error: String? = null

            try {
                val collabOutput = runProcess(
                    ghPath, "api", "repos/$owner/$repo/contributors",
                    "--jq", ".[].login", "--paginate"
                )
                collaborators = collabOutput.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .sorted()
            } catch (e: Exception) {
                error = "Failed to fetch contributors: ${e.message}"
            }

            try {
                val labelsOutput = runProcess(
                    ghPath, "api", "repos/$owner/$repo/labels",
                    "--jq", ".[].name", "--paginate"
                )
                labels = labelsOutput.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .sorted()
            } catch (e: Exception) {
                val labelError = "Failed to fetch labels: ${e.message}"
                error = if (error != null) "$error\n$labelError" else labelError
            }

            if (error == null) {
                cacheService.saveRepoCache(owner, repo, collaborators, labels)
            }

            state.value = state.value.copy(
                isLoading = false,
                collaborators = collaborators,
                labels = labels,
                errorMessage = error,
            )

            if (error == null) {
                autoSelectLabels(labels)
            }
        }
    }

    private fun createLabel(name: String) {
        state.value = state.value.copy(isCreatingLabel = true)
        kotlin.concurrent.thread {
            try {
                runProcess(ghPath, "label", "create", name, "--repo", "$owner/$repo")
                val updatedLabels = (state.value.labels + name).sorted()
                cacheService.saveRepoCache(owner, repo, state.value.collaborators, updatedLabels)
                state.value = state.value.copy(
                    isCreatingLabel = false,
                    labels = updatedLabels,
                    selectedLabels = state.value.selectedLabels + name,
                    labelFilter = "",
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isCreatingLabel = false,
                    errorMessage = "Failed to create label: ${e.message}",
                )
            }
        }
    }

    private fun runProcess(vararg cmd: String): String {
        val process = ProcessBuilder(*cmd)
            .directory(dir)
            .redirectErrorStream(true)
            .start()
        process.outputStream.close()
        val completed = process.waitFor(30, TimeUnit.SECONDS)
        val output = process.inputStream.bufferedReader().readText().trim()
        if (!completed || process.exitValue() != 0) {
            throw RuntimeException(output.ifBlank { "Command timed out" })
        }
        return output
    }

    @Composable
    override fun createDesign() {
        val currentState by remember { state }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = RTheme.colors.black,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                RText(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Create Review PR",
                    style = TextStyle(
                        color = RTheme.colors.blue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(modifier = Modifier.size(16.dp))

                when {
                    currentState.isLoading -> LoadingContent()
                    else -> SelectionContent(currentState)
                }
            }
        }
    }

    @Composable
    private fun LoadingContent() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = RTheme.colors.blue)
                Spacer(modifier = Modifier.size(16.dp))
                RText(
                    text = "Fetching GitHub data...",
                    color = RTheme.colors.lightGray,
                    style = TextStyle(fontSize = 14.sp),
                )
            }
        }
    }

    @Composable
    private fun ColumnScope.SelectionContent(currentState: PRDialogState) {
        Column(modifier = Modifier.weight(1f)) {
            currentState.errorMessage?.let { error ->
                ErrorBanner(error)
                Spacer(modifier = Modifier.size(12.dp))
            }

            Row(
                modifier = Modifier.weight(1f),
            ) {
                SectionContent(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = RTheme.colors.gray,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                    title = "Reviewers",
                    filterValue = currentState.reviewerFilter,
                    onFilterChange = { state.value = state.value.copy(reviewerFilter = it) },
                    filterPlaceholder = "Filter reviewers...",
                    items = currentState.collaborators,
                    selectedItems = currentState.selectedReviewers,
                    onToggle = { item ->
                        val current = state.value.selectedReviewers
                        state.value = state.value.copy(
                            selectedReviewers = if (item in current) current - item else current + item
                        )
                    },
                )

                Spacer(modifier = Modifier.size(16.dp))

                SectionContent(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = RTheme.colors.gray,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                    title = "Labels",
                    filterValue = currentState.labelFilter,
                    onFilterChange = { state.value = state.value.copy(labelFilter = it) },
                    filterPlaceholder = "Filter or create label...",
                    items = currentState.labels,
                    selectedItems = currentState.selectedLabels,
                    onToggle = { item ->
                        val current = state.value.selectedLabels
                        state.value = state.value.copy(
                            selectedLabels = if (item in current) current - item else current + item
                        )
                    },
                    onAdd = if (!currentState.isCreatingLabel) {
                        { name -> createLabel(name) }
                    } else null,
                    isAdding = currentState.isCreatingLabel,
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RActionCard(
                    title = "Refresh",
                    icon = Icons.Rounded.Refresh,
                    actionColor = RTheme.colors.hintGray,
                    type = RActionCardType.MEDIUM,
                    onClick = { fetchGitHubData() },
                )
                Spacer(modifier = Modifier.weight(1f))
                RActionCard(
                    title = "Cancel",
                    icon = Icons.Rounded.Cancel,
                    actionColor = RTheme.colors.lightGray,
                    type = RActionCardType.MEDIUM,
                    onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                )
                Spacer(modifier = Modifier.size(12.dp))
                RActionCard(
                    title = "Create PR",
                    icon = Icons.Rounded.CheckCircle,
                    actionColor = RTheme.colors.blue,
                    type = RActionCardType.MEDIUM,
                    onClick = {
                        onConfirm(
                            state.value.selectedReviewers.toList(),
                            state.value.selectedLabels.toList(),
                        )
                        close(0)
                    },
                )
            }
        }
    }

    @Composable
    private fun ErrorBanner(error: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RTheme.colors.red.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        ) {
            RText(
                text = error,
                color = RTheme.colors.red,
                style = TextStyle(fontSize = 12.sp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            RActionCard(
                title = "Retry",
                icon = Icons.Rounded.Refresh,
                actionColor = RTheme.colors.red,
                type = RActionCardType.SMALL,
                onClick = { fetchGitHubData() },
            )
        }
    }

    @Composable
    private fun SectionContent(
        modifier: Modifier = Modifier,
        title: String,
        filterValue: String,
        onFilterChange: (String) -> Unit,
        filterPlaceholder: String,
        items: List<String>,
        selectedItems: Set<String>,
        onToggle: (String) -> Unit,
        onAdd: ((String) -> Unit)? = null,
        isAdding: Boolean = false,
    ) {
        Column(
            modifier = modifier,
        ) {
            RText(
                text = title,
                color = RTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(modifier = Modifier.size(12.dp))

            RTextField(
                modifier = Modifier.fillMaxWidth(),
                value = filterValue,
                onValueChange = onFilterChange,
                placeholder = filterPlaceholder,
            )

            Spacer(modifier = Modifier.size(8.dp))

            val filtered = items
                .filter { filterValue.isBlank() || it.contains(filterValue, ignoreCase = true) }
                .sortedByDescending { it in selectedItems }

            val exactMatch = items.any { it.equals(filterValue.trim(), ignoreCase = true) }
            if (onAdd != null && filterValue.isNotBlank() && !exactMatch) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RTheme.colors.blue,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        RText(
                            text = "Creating...",
                            color = RTheme.colors.hintGray,
                            style = TextStyle(fontSize = 12.sp),
                        )
                    } else {
                        RActionCard(
                            title = "Add \"${filterValue.trim()}\"",
                            icon = Icons.Rounded.Add,
                            actionColor = RTheme.colors.blue,
                            type = RActionCardType.SMALL,
                            onClick = { onAdd(filterValue.trim()) },
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            if (filtered.isEmpty()) {
                RText(
                    text = if (items.isEmpty()) "No items found" else "No matches",
                    color = RTheme.colors.hintGray,
                    style = TextStyle(fontSize = 12.sp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    filtered.forEach { item ->
                        RCheckbox(
                            modifier = Modifier.fillMaxWidth(),
                            checked = item in selectedItems,
                            label = item,
                            onCheckedChange = { onToggle(item) },
                        )
                    }
                }
            }
        }
    }
}
