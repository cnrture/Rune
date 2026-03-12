package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.common.VcsProvider
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "TeknasyonIntelliJPluginSettings",
    storages = [Storage("teknasyonintellijplugin.xml")]
)
class PluginSettingsService :
    PersistentStateComponent<PluginSettingsService.State> {
    data class State(
        var rootPath: String = "",
        var agentsRootPath: String = "",
        var commitMessagePrompt: String = DEFAULT_COMMIT_PROMPT,
        var includeJiraUrlInCommit: Boolean = false,
        var useReviewBranch: Boolean = false,
        var vcsProviderOverride: String = "",
        var cachedClaudeModel: String = "",
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

    fun getCachedClaudeModel(): String? = state.cachedClaudeModel.takeIf { it.isNotBlank() }

    fun setCachedClaudeModel(model: String?) {
        state.cachedClaudeModel = model ?: ""
    }

    fun getVcsProvider(): VcsProvider {
        return state.vcsProviderOverride.takeIf { it.isNotBlank() }?.let {
            try { VcsProvider.valueOf(it) } catch (_: Exception) { null }
        } ?: VcsProvider.GITHUB
    }

    fun setVcsProvider(provider: VcsProvider) {
        state.vcsProviderOverride = provider.name
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
