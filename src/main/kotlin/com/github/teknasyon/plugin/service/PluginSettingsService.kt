package com.github.teknasyon.plugin.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "TeknasyonAndroidStudioPluginSetting",
    storages = [Storage("teknasyonandroidstudioplugin.xml")]
)
class PluginSettingsService :
    PersistentStateComponent<PluginSettingsService.State> {
    data class State(
        var rootPath: String = "",
        var agentsRootPath: String = "",
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

    companion object {
        fun getInstance(project: Project): PluginSettingsService {
            return project.getService(PluginSettingsService::class.java)
        }
    }
}
