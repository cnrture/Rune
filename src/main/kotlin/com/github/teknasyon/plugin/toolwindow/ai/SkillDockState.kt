package com.github.teknasyon.plugin.toolwindow.ai

import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.domain.model.SkillFolder

enum class SkillDockTab { SKILLS, AGENTS }

data class TabState(
    val items: List<Skill> = emptyList(),
    val allFolders: List<SkillFolder> = emptyList(),
    val searchQuery: String = "",
    val showOnlyFavorites: Boolean = false,
    val rootPath: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class SkillDockState(
    val activeTab: SkillDockTab = SkillDockTab.SKILLS,
    val skillsTab: TabState = TabState(),
    val agentsTab: TabState = TabState(),
    val reviewTracker: ReviewTrackerState = ReviewTrackerState(),
) {
    val currentTab: TabState
        get() = if (activeTab == SkillDockTab.SKILLS) skillsTab else agentsTab

    fun updateCurrentTab(block: TabState.() -> TabState): SkillDockState =
        if (activeTab == SkillDockTab.SKILLS) copy(skillsTab = skillsTab.block())
        else copy(agentsTab = agentsTab.block())
}
