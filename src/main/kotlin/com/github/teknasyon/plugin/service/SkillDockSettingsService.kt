package com.github.teknasyon.plugin.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "SkillDockSettings",
    storages = [Storage("skilldock.xml")]
)
class SkillDockSettingsService : PersistentStateComponent<SkillDockSettingsService.State> {
    data class State(
        var rootPath: String = "",
        var agentsRootPath: String = "",
        var favoriteSkills: MutableSet<String> = mutableSetOf(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun setSkillsRootPath(path: String) {
        state.rootPath = path
    }

    fun getSkillsRootPath(): String = state.rootPath

    fun setAgentsRootPath(path: String) {
        state.agentsRootPath = path
    }

    fun getAgentsRootPath(): String = state.agentsRootPath
    fun addFavorite(skillPath: String) = state.favoriteSkills.add(skillPath)
    fun removeFavorite(skillPath: String) = state.favoriteSkills.remove(skillPath)
    fun isFavorite(skillPath: String): Boolean = state.favoriteSkills.contains(skillPath)
    fun getFavorites(): Set<String> = state.favoriteSkills.toSet()
    fun clearFavorites() = state.favoriteSkills.clear()

    companion object {
        fun getInstance(project: Project): SkillDockSettingsService {
            return project.getService(SkillDockSettingsService::class.java)
        }
    }
}
