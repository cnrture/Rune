package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.actions.dialog.CommentThread
import com.github.teknasyon.plugin.common.VcsProvider

data class VcsUser(
    val displayName: String,
    val username: String,
    val id: String = "",
)

data class PrCreationResult(
    val url: String? = null,
    val errorMessage: String? = null,
)

data class PrIdentifier(
    val owner: String,
    val repo: String,
    val number: Int,
)

interface VcsPlatformService {
    val providerType: VcsProvider
    val supportsLabels: Boolean
    val supportsAssignee: Boolean

    // PR Creation
    fun fetchReviewerCandidates(): Result<List<VcsUser>>
    fun fetchLabels(): Result<List<String>>
    fun createPullRequest(
        currentBranch: String,
        targetBranch: String,
        title: String,
        body: String,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ): PrCreationResult

    fun createLabel(name: String): Result<Unit>
    fun findExistingPr(currentBranch: String): String?

    // PR Comments (FixPRCommentsAction)
    fun fetchPrTitle(prIdentifier: PrIdentifier): Result<String>
    fun fetchUnresolvedComments(prIdentifier: PrIdentifier): Result<List<CommentThread>>
    fun parsePrUrl(url: String): PrIdentifier?

    fun getPlaceholderUrl(): String
}
