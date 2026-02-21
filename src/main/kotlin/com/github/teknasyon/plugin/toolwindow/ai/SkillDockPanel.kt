package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

@Composable
fun SkillDockPanel(
    viewModel: SkillDockViewModel,
    project: Project,
    onShowSettingsClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val tab = state.currentTab
    val tracker = state.reviewTracker

    if (tracker.isDialogVisible) {
        ReviewTrackerDialog(
            onStart = { prUrl -> viewModel.onEvent(SkillDockEvent.StartReviewTracking(prUrl)) },
            onDismiss = { viewModel.onEvent(SkillDockEvent.CloseReviewTracker) },
        )
    }

    val showTrackerPanel = tracker.status != ReviewTrackerStatus.IDLE || tracker.tasks.isNotEmpty()

    if (showTrackerPanel) {
        ReviewResultPanel(
            status = tracker.status,
            progressMessage = tracker.progressMessage,
            tasks = tracker.tasks,
            changes = tracker.changes,
            error = tracker.error,
            onBack = { viewModel.onEvent(SkillDockEvent.CloseReviewTracker) },
        )
        return
    }

    Column {
        TopAppBar(
            title = {
                SearchBar(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    query = tab.searchQuery,
                    onQueryChange = { viewModel.onEvent(SkillDockEvent.SearchQueryChanged(it)) },
                )
            },
            actions = {
                IconButton(onClick = { viewModel.onEvent(SkillDockEvent.OpenReviewTracker) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = "PR Tracker",
                        tint = TPTheme.colors.white,
                    )
                }
                IconButton(onClick = { viewModel.onEvent(SkillDockEvent.ToggleFavoritesFilter) }) {
                    Icon(
                        imageVector = if (tab.showOnlyFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Toggle Favorites",
                        tint = if (tab.showOnlyFavorites) Color(0xFFFFD700) else TPTheme.colors.white,
                    )
                }
                IconButton(onClick = { viewModel.onEvent(SkillDockEvent.RefreshSkills) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TPTheme.colors.white,
                    )
                }
            },
            elevation = 4.dp,
            backgroundColor = TPTheme.colors.black,
        )

        TabRow(
            selectedTabIndex = state.activeTab.ordinal,
            backgroundColor = TPTheme.colors.black,
            contentColor = TPTheme.colors.white,
        ) {
            Tab(
                selected = state.activeTab == SkillDockTab.SKILLS,
                onClick = { viewModel.onEvent(SkillDockEvent.TabChanged(SkillDockTab.SKILLS)) },
                text = { Text("Skills") }
            )
            Tab(
                selected = state.activeTab == SkillDockTab.AGENTS,
                onClick = { viewModel.onEvent(SkillDockEvent.TabChanged(SkillDockTab.AGENTS)) },
                text = { Text("Agents") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                tab.isLoading -> LoadingView()
                tab.error != null -> ErrorView(
                    message = tab.error,
                    onRetry = { viewModel.onEvent(SkillDockEvent.RefreshSkills) },
                    onOpenSettings = { onShowSettingsClick() }
                )

                tab.items.isEmpty() -> EmptyView(onOpenSettings = { onShowSettingsClick() })
                else -> SkillList(
                    skills = tab.items,
                    showRunButton = state.activeTab == SkillDockTab.AGENTS,
                    onExecuteSkill = { skill, input ->
                        viewModel.onEvent(SkillDockEvent.ExecuteSkill(skill, input))
                    },
                    onToggleFavorite = { skill ->
                        viewModel.onEvent(SkillDockEvent.ToggleFavorite(skill))
                    },
                    onOpenFile = { skill ->
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(skill.filePath)
                        if (virtualFile != null) {
                            FileEditorManager.getInstance(project).openFile(virtualFile, true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyView(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No skills found",
                style = MaterialTheme.typography.h6,
                color = TPTheme.colors.white.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onOpenSettings,
                colors = ButtonDefaults.textButtonColors(contentColor = TPTheme.colors.blue)
            ) {
                Text("Configure Root Path")
            }
        }
    }
}
