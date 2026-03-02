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
        var commitMessagePrompt: String = DEFAULT_COMMIT_PROMPT,
        var includeJiraUrlInCommit: Boolean = false,
        var useReviewBranch: Boolean = false,
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

    fun getCommitMessagePrompt(): String = state.commitMessagePrompt

    fun setCommitMessagePrompt(prompt: String) {
        state.commitMessagePrompt = prompt
    }

    fun isIncludeJiraUrlInCommit(): Boolean = state.includeJiraUrlInCommit

    fun setIncludeJiraUrlInCommit(include: Boolean) {
        state.includeJiraUrlInCommit = include
    }

    fun isUseReviewBranch(): Boolean = state.useReviewBranch

    fun setUseReviewBranch(use: Boolean) {
        state.useReviewBranch = use
    }

    companion object {

        const val DEFAULT_COMMIT_PROMPT =
            "Based on the following git diff, write a single conventional commit message " +
                "(format: type: description). Output only the commit message, nothing else.\n\n{diff}"

        fun getInstance(project: Project): PluginSettingsService {
            return project.getService(PluginSettingsService::class.java)
        }
    }
}
