package com.github.teknasyon.plugin.toolwindow.ai

import com.github.teknasyon.plugin.service.SkillDockSettingsService
import com.github.teknasyon.plugin.service.TerminalExecutor
import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.domain.usecase.ExecuteSkillUseCase
import com.github.teknasyon.plugin.domain.usecase.ProcessReviewCommentsUseCase
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.domain.usecase.ToggleFavoriteUseCase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SkillDockViewModel(
    private val project: Project,
    private val settingsService: SkillDockSettingsService,
    private val scanSkillsUseCase: ScanSkillsUseCase,
    private val executeSkillUseCase: ExecuteSkillUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val processReviewCommentsUseCase: ProcessReviewCommentsUseCase,
    private val terminalExecutor: TerminalExecutor,
) {
    private val _state = MutableStateFlow(
        SkillDockState(
            skillsTab = TabState(rootPath = settingsService.getSkillsRootPath()),
            agentsTab = TabState(rootPath = settingsService.getAgentsRootPath()),
        )
    )
    val state: StateFlow<SkillDockState> = _state.asStateFlow()

    init {
        loadTab(SkillDockTab.SKILLS)
        loadTab(SkillDockTab.AGENTS)
    }

    fun onEvent(event: SkillDockEvent) {
        when (event) {
            is SkillDockEvent.TabChanged -> _state.update { it.copy(activeTab = event.tab) }
            is SkillDockEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is SkillDockEvent.ExecuteSkill -> executeSkillUseCase(project, event.skill, event.input)
            is SkillDockEvent.ToggleFavorite -> toggleFavorite(event.skill)
            is SkillDockEvent.RefreshSkills -> loadTab(_state.value.activeTab)
            is SkillDockEvent.ToggleFavoritesFilter -> toggleFavoritesFilter()
            is SkillDockEvent.SaveSkillsRootPath -> saveRootPath(SkillDockTab.SKILLS, event.path)
            is SkillDockEvent.SaveAgentsRootPath -> saveRootPath(SkillDockTab.AGENTS, event.path)
            is SkillDockEvent.OpenReviewTracker -> _state.update {
                it.copy(reviewTracker = it.reviewTracker.copy(isDialogVisible = true))
            }
            is SkillDockEvent.StartReviewTracking -> startReviewTracking(event.prUrl)
            is SkillDockEvent.CloseReviewTracker -> _state.update {
                it.copy(reviewTracker = ReviewTrackerState())
            }
            is SkillDockEvent.RunCommand -> terminalExecutor.executeCommand(project, "claude ${event.command}")
        }
    }

    private fun startReviewTracking(prUrl: String) {
        _state.update {
            it.copy(
                reviewTracker = ReviewTrackerState(
                    isDialogVisible = false,
                    status = ReviewTrackerStatus.FETCHING,
                    progressMessage = "Başlatılıyor...",
                )
            )
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            processReviewCommentsUseCase.execute(
                prUrl = prUrl,
                onProgress = { message ->
                    _state.update { state ->
                        val newStatus = when {
                            message.contains("task listesi") -> ReviewTrackerStatus.ANALYZING
                            message.contains("işleniyor") -> ReviewTrackerStatus.PROCESSING
                            else -> state.reviewTracker.status
                        }
                        state.copy(
                            reviewTracker = state.reviewTracker.copy(
                                status = newStatus,
                                progressMessage = message,
                            )
                        )
                    }
                },
                onTaskUpdate = { task ->
                    _state.update { state ->
                        val updatedTasks = state.reviewTracker.tasks.toMutableList()
                        val existingIdx = updatedTasks.indexOfFirst { it.id == task.id }
                        if (existingIdx >= 0) updatedTasks[existingIdx] = task
                        else updatedTasks.add(task)
                        state.copy(
                            reviewTracker = state.reviewTracker.copy(tasks = updatedTasks)
                        )
                    }
                },
                onChangeRecorded = { change ->
                    _state.update { state ->
                        state.copy(
                            reviewTracker = state.reviewTracker.copy(
                                changes = state.reviewTracker.changes + change,
                            )
                        )
                    }
                },
            ).onSuccess {
                _state.update { state ->
                    state.copy(
                        reviewTracker = state.reviewTracker.copy(
                            status = ReviewTrackerStatus.DONE,
                            progressMessage = "Tamamlandı.",
                        )
                    )
                }
            }.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        reviewTracker = state.reviewTracker.copy(
                            status = ReviewTrackerStatus.ERROR,
                            error = error.message ?: "Bilinmeyen hata",
                        )
                    )
                }
            }
        }
    }

    private fun saveRootPath(tab: SkillDockTab, path: String) {
        if (tab == SkillDockTab.SKILLS) settingsService.setSkillsRootPath(path)
        else settingsService.setAgentsRootPath(path)
        _state.update { state ->
            if (tab == SkillDockTab.SKILLS) state.copy(skillsTab = state.skillsTab.copy(rootPath = path))
            else state.copy(agentsTab = state.agentsTab.copy(rootPath = path))
        }
        loadTab(tab)
    }

    private fun loadTab(tab: SkillDockTab) {
        if (tab == SkillDockTab.COMMANDS) return
        val rootPath = if (tab == SkillDockTab.SKILLS) settingsService.getSkillsRootPath()
        else settingsService.getAgentsRootPath()

        _state.update { state ->
            if (tab == SkillDockTab.SKILLS) state.copy(skillsTab = state.skillsTab.copy(isLoading = true, error = null))
            else state.copy(agentsTab = state.agentsTab.copy(isLoading = true, error = null))
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val strictFilter = tab == SkillDockTab.SKILLS
            scanSkillsUseCase(rootPath, strictFilter)
                .onSuccess { folders ->
                    val items = folders.flatMap { it.getAllSkills() }
                    _state.update { state ->
                        val updated = (if (tab == SkillDockTab.SKILLS) state.skillsTab else state.agentsTab)
                            .copy(items = items, allFolders = folders, isLoading = false, error = null)
                        if (tab == SkillDockTab.SKILLS) state.copy(skillsTab = updated)
                        else state.copy(agentsTab = updated)
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        val updated = (if (tab == SkillDockTab.SKILLS) state.skillsTab else state.agentsTab)
                            .copy(error = error.message ?: "Unknown error occurred", isLoading = false)
                        if (tab == SkillDockTab.SKILLS) state.copy(skillsTab = updated)
                        else state.copy(agentsTab = updated)
                    }
                }
        }
    }

    private fun toggleFavorite(skill: Skill) {
        toggleFavoriteUseCase(skill.filePath)
        loadTab(_state.value.activeTab)
    }

    private fun toggleFavoritesFilter() {
        _state.update { state ->
            state.updateCurrentTab {
                val toggled = !showOnlyFavorites
                copy(
                    showOnlyFavorites = toggled,
                    items = applyFilters(allFolders.flatMap { it.getAllSkills() }, searchQuery, toggled)
                )
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { state ->
            if (state.activeTab == SkillDockTab.COMMANDS) {
                state.copy(commandsSearchQuery = query)
            } else {
                state.updateCurrentTab {
                    copy(
                        searchQuery = query,
                        items = applyFilters(allFolders.flatMap { it.getAllSkills() }, query, showOnlyFavorites)
                    )
                }
            }
        }
    }

    private fun applyFilters(allSkills: List<Skill>, query: String, onlyFavorites: Boolean): List<Skill> {
        return allSkills.filter { skill ->
            val matchesQuery = query.isBlank() ||
                skill.name.contains(query, ignoreCase = true) ||
                skill.description.contains(query, ignoreCase = true) ||
                skill.commandName.contains(query, ignoreCase = true)
            val matchesFavorite = !onlyFavorites || skill.isFavorite
            matchesQuery && matchesFavorite
        }
    }
}
