package com.github.cnrture.rune.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "GitHubCacheSettings", storages = [Storage("githubCache.xml")])
class VcsCacheService : PersistentStateComponent<VcsCacheService.CacheState> {

    data class VcsUserEntry(
        var displayName: String = "",
        var username: String = "",
        var id: String = "",
    )

    data class RepoCache(
        var collaborators: MutableList<String> = mutableListOf(),
        var labels: MutableList<String> = mutableListOf(),
        var userDetails: MutableList<VcsUserEntry> = mutableListOf(),
    )

    data class CacheState(
        var repos: MutableMap<String, RepoCache> = mutableMapOf(),
    )

    private var state = CacheState()

    override fun getState(): CacheState = state

    override fun loadState(state: CacheState) {
        this.state = state
    }

    fun getRepoCache(owner: String, repo: String): RepoCache? {
        return state.repos["$owner/$repo"]
    }

    fun saveRepoCacheWithUsers(owner: String, repo: String, users: List<VcsUser>, labels: List<String>) {
        state.repos["$owner/$repo"] = RepoCache(
            collaborators = users.map { it.displayName }.toMutableList(),
            labels = labels.toMutableList(),
            userDetails = users.map { VcsUserEntry(it.displayName, it.username, it.id) }.toMutableList(),
        )
    }

    fun getUserDetails(owner: String, repo: String): List<VcsUser>? {
        val cache = state.repos["$owner/$repo"] ?: return null
        if (cache.userDetails.isEmpty()) return null
        return cache.userDetails.map { VcsUser(it.displayName, it.username, it.id) }
    }

    companion object {
        fun getInstance(): VcsCacheService {
            return ApplicationManager.getApplication().getService(VcsCacheService::class.java)
        }
    }
}
