package com.github.teknasyon.plugin.toolwindow.ai

import com.github.teknasyon.plugin.domain.model.Skill

sealed interface SkillDockEvent {
    data class TabChanged(val tab: SkillDockTab) : SkillDockEvent
    data class SearchQueryChanged(val query: String) : SkillDockEvent
    data class ExecuteSkill(val skill: Skill, val input: String) : SkillDockEvent
    data class ToggleFavorite(val skill: Skill) : SkillDockEvent
    data object RefreshSkills : SkillDockEvent
    data object ToggleFavoritesFilter : SkillDockEvent
    data class SaveSkillsRootPath(val path: String) : SkillDockEvent
    data class SaveAgentsRootPath(val path: String) : SkillDockEvent
    data object OpenReviewTracker : SkillDockEvent
    data class StartReviewTracking(val prUrl: String) : SkillDockEvent
    data object CloseReviewTracker : SkillDockEvent
    data class RunCommand(val command: String) : SkillDockEvent
}
