package com.github.teknasyon.plugin.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "GitHubCacheSettings", storages = [Storage("githubCache.xml")])
class GitHubCacheService : PersistentStateComponent<GitHubCacheService.State> {

    data class RepoCache(
        var collaborators: MutableList<String> = mutableListOf(),
        var labels: MutableList<String> = mutableListOf(),
    )

    data class State(
        var repos: MutableMap<String, RepoCache> = mutableMapOf(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getRepoCache(owner: String, repo: String): RepoCache? {
        return state.repos["$owner/$repo"]
    }

    fun saveRepoCache(owner: String, repo: String, collaborators: List<String>, labels: List<String>) {
        state.repos["$owner/$repo"] = RepoCache(
            collaborators = collaborators.toMutableList(),
            labels = labels.toMutableList(),
        )
    }

    companion object {
        fun getInstance(): GitHubCacheService {
            return ApplicationManager.getApplication().getService(GitHubCacheService::class.java)
        }
    }
}
