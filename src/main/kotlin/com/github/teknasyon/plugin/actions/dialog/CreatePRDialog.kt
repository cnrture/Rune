package com.github.teknasyon.plugin.actions.dialog

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
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.components.*
import com.github.teknasyon.plugin.service.JiraService
import com.github.teknasyon.plugin.service.VcsCacheService
import com.github.teknasyon.plugin.service.VcsPlatformService
import com.github.teknasyon.plugin.service.VcsUser
import com.github.teknasyon.plugin.theme.TPTheme

data class PRDialogState(
    val isLoading: Boolean = true,
    val reviewerUsers: List<VcsUser> = emptyList(),
    val labels: List<String> = emptyList(),
    val selectedReviewers: Set<String> = emptySet(),
    val selectedLabels: Set<String> = emptySet(),
    val reviewerFilter: String = "",
    val labelFilter: String = "",
    val errorMessage: String? = null,
    val isCreatingLabel: Boolean = false,
    val selectedBaseBranch: String = "",
    val baseBranchFilter: String = "",
    val remoteBranches: List<String> = emptyList(),
)

class CreatePRDialog(
    private val platformService: VcsPlatformService,
    private val owner: String,
    private val repo: String,
    private val ticketId: String? = null,
    remoteBranches: List<String> = emptyList(),
    suggestedBaseBranch: String = "main",
    private val useReviewBranch: Boolean = false,
    private val onConfirm: (reviewers: List<VcsUser>, labels: List<String>, baseBranch: String) -> Unit,
) : TPDialogWrapper(
    width = 700,
    height = 550,
) {

    private var state = mutableStateOf(
        PRDialogState(
            selectedBaseBranch = suggestedBaseBranch,
            remoteBranches = remoteBranches,
        )
    )

    private val cacheService = VcsCacheService.getInstance()

    init {
        title = "Create Review PR"
        loadData()
    }

    private fun loadData() {
        val cached = cacheService.getRepoCache(owner, repo)
        if (cached != null) {
            val cachedUsers = cacheService.getUserDetails(owner, repo)
            val reviewerUsers = cachedUsers ?: cached.collaborators.map { VcsUser(it, it) }
            state.value = state.value.copy(
                isLoading = false,
                reviewerUsers = reviewerUsers.sortedBy { it.displayName },
                labels = cached.labels.sorted(),
            )
            if (platformService.supportsLabels) {
                autoSelectLabels(cached.labels)
            }
        } else {
            fetchData()
        }
    }

    private fun autoSelectLabels(labels: List<String>) {
        if (ticketId == null || !platformService.supportsLabels) return

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

    private fun fetchData() {
        state.value = state.value.copy(isLoading = true, errorMessage = null)
        kotlin.concurrent.thread {
            var reviewerUsers: List<VcsUser> = emptyList()
            var labels: List<String> = emptyList()
            var error: String? = null

            platformService.fetchReviewerCandidates().fold(
                onSuccess = { reviewerUsers = it.sortedBy { u -> u.displayName } },
                onFailure = { error = "Failed to fetch reviewers: ${it.message}" },
            )

            if (platformService.supportsLabels) {
                platformService.fetchLabels().fold(
                    onSuccess = { labels = it.sorted() },
                    onFailure = { e ->
                        val labelError = "Failed to fetch labels: ${e.message}"
                        error = if (error != null) "$error\n$labelError" else labelError
                    },
                )
            }

            if (error == null) {
                cacheService.saveRepoCacheWithUsers(owner, repo, reviewerUsers, labels)
            }

            state.value = state.value.copy(
                isLoading = false,
                reviewerUsers = reviewerUsers,
                labels = labels,
                errorMessage = error,
            )

            if (error == null && platformService.supportsLabels) {
                autoSelectLabels(labels)
            }
        }
    }

    private fun createLabel(name: String) {
        state.value = state.value.copy(isCreatingLabel = true)
        kotlin.concurrent.thread {
            platformService.createLabel(name).fold(
                onSuccess = {
                    val updatedLabels = (state.value.labels + name).sorted()
                    cacheService.saveRepoCacheWithUsers(
                        owner, repo, state.value.reviewerUsers, updatedLabels,
                    )
                    state.value = state.value.copy(
                        isCreatingLabel = false,
                        labels = updatedLabels,
                        selectedLabels = state.value.selectedLabels + name,
                        labelFilter = "",
                    )
                },
                onFailure = { e ->
                    state.value = state.value.copy(
                        isCreatingLabel = false,
                        errorMessage = "Failed to create label: ${e.message}",
                    )
                },
            )
        }
    }

    @Composable
    override fun createDesign() {
        val currentState by remember { state }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TPTheme.colors.black,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                TPText(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Create Review PR",
                    style = TextStyle(
                        color = TPTheme.colors.blue,
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
                CircularProgressIndicator(color = TPTheme.colors.blue)
                Spacer(modifier = Modifier.size(16.dp))
                TPText(
                    text = "Fetching data...",
                    color = TPTheme.colors.lightGray,
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

            Spacer(modifier = Modifier.size(12.dp))

            Row(
                modifier = Modifier.weight(1f),
            ) {
                // Base branch selector
                BaseBranchSelector(currentState)

                Spacer(modifier = Modifier.size(16.dp))

                SectionContent(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = TPTheme.colors.gray,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                    title = "Reviewers",
                    filterValue = currentState.reviewerFilter,
                    onFilterChange = { state.value = state.value.copy(reviewerFilter = it) },
                    filterPlaceholder = "Filter reviewers...",
                    items = currentState.reviewerUsers.map { it.displayName },
                    selectedItems = currentState.selectedReviewers,
                    onToggle = { item ->
                        val current = state.value.selectedReviewers
                        state.value = state.value.copy(
                            selectedReviewers = if (item in current) current - item else current + item
                        )
                    },
                )

                if (platformService.supportsLabels) {
                    Spacer(modifier = Modifier.size(16.dp))

                    SectionContent(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = TPTheme.colors.gray,
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
            }

            Spacer(modifier = Modifier.size(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TPActionCard(
                    title = "Refresh",
                    icon = Icons.Rounded.Refresh,
                    actionColor = TPTheme.colors.hintGray,
                    type = TPActionCardType.SMALL,
                    onClick = { fetchData() },
                )
                Spacer(modifier = Modifier.weight(1f))
                TPActionCard(
                    title = "Cancel",
                    icon = Icons.Rounded.Cancel,
                    actionColor = TPTheme.colors.lightGray,
                    type = TPActionCardType.SMALL,
                    onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                )
                Spacer(modifier = Modifier.size(12.dp))
                TPActionCard(
                    title = "Create PR",
                    icon = Icons.Rounded.CheckCircle,
                    actionColor = TPTheme.colors.blue,
                    type = TPActionCardType.SMALL,
                    onClick = {
                        val selectedUserNames = state.value.selectedReviewers
                        val selectedUsers = state.value.reviewerUsers
                            .filter { it.displayName in selectedUserNames }
                        onConfirm(
                            selectedUsers,
                            state.value.selectedLabels.toList(),
                            state.value.selectedBaseBranch,
                        )
                        close(0)
                    },
                )
            }
        }
    }

    @Composable
    private fun RowScope.BaseBranchSelector(currentState: PRDialogState) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = TPTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        ) {
            val targetLabel = if (useReviewBranch) {
                "Base Branch (review/ branch will be created from this)"
            } else {
                "Base Branch (PR target)"
            }
            TPText(
                text = targetLabel,
                color = TPTheme.colors.white,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            )

            Spacer(modifier = Modifier.size(8.dp))

            TPTextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentState.baseBranchFilter,
                onValueChange = { state.value = state.value.copy(baseBranchFilter = it) },
                placeholder = "Filter branches...",
            )

            Spacer(modifier = Modifier.size(8.dp))

            val filtered = currentState.remoteBranches
                .filter { currentState.baseBranchFilter.isBlank() || it.contains(currentState.baseBranchFilter, ignoreCase = true) }
                .sortedByDescending { it == currentState.selectedBaseBranch }
                .take(10)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                filtered.forEach { branch ->
                    TPCheckbox(
                        modifier = Modifier.fillMaxWidth(),
                        checked = branch == currentState.selectedBaseBranch,
                        label = branch,
                        onCheckedChange = {
                            state.value = state.value.copy(selectedBaseBranch = branch)
                        },
                    )
                }
                if (filtered.isEmpty()) {
                    TPText(
                        text = "No matching branches",
                        color = TPTheme.colors.hintGray,
                        style = TextStyle(fontSize = 12.sp),
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
                icon = Icons.Rounded.Refresh,
                actionColor = TPTheme.colors.red,
                type = TPActionCardType.SMALL,
                onClick = { fetchData() },
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
            TPText(
                text = title,
                color = TPTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(modifier = Modifier.size(12.dp))

            TPTextField(
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
                            color = TPTheme.colors.blue,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TPText(
                            text = "Creating...",
                            color = TPTheme.colors.hintGray,
                            style = TextStyle(fontSize = 12.sp),
                        )
                    } else {
                        TPActionCard(
                            title = "Add \"${filterValue.trim()}\"",
                            icon = Icons.Rounded.Add,
                            actionColor = TPTheme.colors.blue,
                            type = TPActionCardType.SMALL,
                            onClick = { onAdd(filterValue.trim()) },
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            if (filtered.isEmpty()) {
                TPText(
                    text = if (items.isEmpty()) "No items found" else "No matches",
                    color = TPTheme.colors.hintGray,
                    style = TextStyle(fontSize = 12.sp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    filtered.forEach { item ->
                        TPCheckbox(
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
