package com.github.teknasyon.plugin.common

enum class VcsProvider {
    GITHUB,
    BITBUCKET_CLOUD,
}

data class RemoteInfo(
    val host: String,
    val ownerOrProject: String,
    val repo: String,
)

object VcsProviderDetector {

    private val SSH_REGEX = Regex("""git@([^:]+):(.+)/(.+?)(?:\.git)?$""")
    private val HTTPS_REGEX = Regex("""https?://([^/]+)/(.+)/(.+?)(?:\.git)?$""")

    fun parseRemote(remoteUrl: String): RemoteInfo? {
        // SSH: git@host:owner/repo.git
        SSH_REGEX.find(remoteUrl)?.let { match ->
            return RemoteInfo(
                host = match.groupValues[1],
                ownerOrProject = match.groupValues[2],
                repo = match.groupValues[3],
            )
        }

        // HTTPS: https://host/owner/repo.git
        HTTPS_REGEX.find(remoteUrl)?.let { match ->
            return RemoteInfo(
                host = match.groupValues[1],
                ownerOrProject = match.groupValues[2],
                repo = match.groupValues[3],
            )
        }

        return null
    }
}
