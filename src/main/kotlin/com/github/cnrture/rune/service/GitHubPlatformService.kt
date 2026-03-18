package com.github.cnrture.rune.service

import com.github.cnrture.rune.actions.dialog.CommentThread
import com.github.cnrture.rune.actions.dialog.Reply
import com.github.cnrture.rune.common.VcsProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GitHubPlatformService(
    private val api: GitHubApiClient,
    private val owner: String,
    private val repo: String,
) : VcsPlatformService {

    override val providerType = VcsProvider.GITHUB
    override val supportsLabels = true
    override val supportsAssignee = true

    override fun fetchReviewerCandidates(): Result<List<VcsUser>> {
        return try {
            val pages = api.getPaginated("/repos/$owner/$repo/contributors")
            val users = pages.flatMap { page ->
                val array = Json.parseToJsonElement(page).jsonArray
                array.map { it.jsonObject["login"]!!.jsonPrimitive.content }
            }.distinct().map { VcsUser(displayName = it, username = it) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchLabels(): Result<List<String>> {
        return try {
            val pages = api.getPaginated("/repos/$owner/$repo/labels")
            val labels = pages.flatMap { page ->
                val array = Json.parseToJsonElement(page).jsonArray
                array.map { it.jsonObject["name"]!!.jsonPrimitive.content }
            }.distinct()
            Result.success(labels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchAuthenticatedUser(): Result<String> {
        return try {
            val response = api.get("/user")
            if (!response.isSuccess) {
                return Result.failure(IllegalStateException(response.parseErrorMessage()))
            }
            val json = Json.parseToJsonElement(response.body).jsonObject
            Result.success(json["login"]!!.jsonPrimitive.content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun createPullRequest(
        currentBranch: String,
        targetBranch: String,
        title: String,
        body: String,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ): PrCreationResult {
        // Create PR
        val createBody = buildJsonObject(
            "title" to title,
            "head" to currentBranch,
            "base" to targetBranch,
            "body" to body,
        )

        val createResponse = api.post("/repos/$owner/$repo/pulls", createBody)

        if (!createResponse.isSuccess) {
            // Check if PR already exists
            val existingUrl = findExistingPr(currentBranch)
            if (existingUrl != null) {
                return PrCreationResult(url = existingUrl, errorMessage = "PR already exists")
            }
            val hint = when (createResponse.statusCode) {
                404 -> " (token may lack 'repo' scope or repository access)"
                401 -> " (invalid or expired token)"
                403 -> " (insufficient permissions)"
                422 -> " (${createResponse.parseErrorMessage()})"
                else -> ""
            }
            return PrCreationResult(
                errorMessage = "PR creation failed (HTTP ${createResponse.statusCode})$hint"
            )
        }

        val prJson = Json.parseToJsonElement(createResponse.body).jsonObject
        val prUrl = prJson["html_url"]!!.jsonPrimitive.content
        val prNumber = prJson["number"]!!.jsonPrimitive.int

        // Assign to self
        val username = fetchAuthenticatedUser().getOrNull()
        if (username != null) {
            api.post(
                "/repos/$owner/$repo/issues/$prNumber/assignees",
                """{"assignees":["$username"]}""",
            )
        }

        // Add reviewers
        var warningMessage: String? = null
        if (reviewers.isNotEmpty()) {
            val reviewerList = reviewers.joinToString(",") { "\"${it.username}\"" }
            val reviewerResponse = api.post(
                "/repos/$owner/$repo/pulls/$prNumber/requested_reviewers",
                """{"reviewers":[$reviewerList]}""",
            )
            if (!reviewerResponse.isSuccess) {
                warningMessage = "PR created but failed to add reviewers: ${reviewerResponse.parseErrorMessage()}"
            }
        }

        // Add labels
        if (labels.isNotEmpty()) {
            val labelList = labels.joinToString(",") { "\"$it\"" }
            val labelResponse = api.post(
                "/repos/$owner/$repo/issues/$prNumber/labels",
                """{"labels":[$labelList]}""",
            )
            if (!labelResponse.isSuccess) {
                val labelError = "Failed to add labels: ${labelResponse.parseErrorMessage()}"
                warningMessage = if (warningMessage != null) "$warningMessage; $labelError" else "PR created but $labelError"
            }
        }

        return PrCreationResult(url = prUrl, errorMessage = warningMessage)
    }

    override fun createLabel(name: String): Result<Unit> {
        return try {
            val body = """{"name":"$name"}"""
            val response = api.post("/repos/$owner/$repo/labels", body)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(response.parseErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun findExistingPr(currentBranch: String): String? {
        val response = api.get("/repos/$owner/$repo/pulls?head=$owner:$currentBranch&state=open")
        if (!response.isSuccess) return null
        val array = Json.parseToJsonElement(response.body).jsonArray
        if (array.isEmpty()) return null
        return array[0].jsonObject["html_url"]!!.jsonPrimitive.content
    }

    override fun fetchPrTitle(prIdentifier: PrIdentifier): Result<String> {
        return try {
            val response = api.get("/repos/${prIdentifier.owner}/${prIdentifier.repo}/pulls/${prIdentifier.number}")
            if (!response.isSuccess) {
                return Result.failure(IllegalStateException(response.parseErrorMessage()))
            }
            val json = Json.parseToJsonElement(response.body).jsonObject
            Result.success(json["title"]!!.jsonPrimitive.content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchUnresolvedComments(prIdentifier: PrIdentifier): Result<List<CommentThread>> {
        return try {
            val query = """
                {
                  repository(owner: "${prIdentifier.owner}", name: "${prIdentifier.repo}") {
                    pullRequest(number: ${prIdentifier.number}) {
                      reviewThreads(first: 100) {
                        nodes {
                          isResolved
                          comments(first: 10) {
                            nodes {
                              body
                              path
                              line
                              originalLine
                              diffHunk
                              author { login }
                            }
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val response = api.graphql(query)
            if (!response.isSuccess) {
                return Result.failure(IllegalStateException("GitHub GraphQL error: ${response.parseErrorMessage()}"))
            }

            val threads = parseGitHubThreads(response.body)
            Result.success(threads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGitHubThreads(json: String): List<CommentThread> {
        val root = try {
            Json.parseToJsonElement(json).jsonObject
        } catch (e: Exception) {
            throw IllegalStateException("GitHub API returned invalid JSON: ${json.take(200)}", e)
        }
        if ("errors" in root) {
            val errors = root["errors"]?.jsonArray
                ?.joinToString { it.jsonObject["message"]?.jsonPrimitive?.content ?: it.toString() }
            throw IllegalStateException("GitHub GraphQL error: $errors")
        }
        val threads = root["data"]!!.jsonObject["repository"]!!.jsonObject["pullRequest"]!!
            .jsonObject["reviewThreads"]!!.jsonObject["nodes"]!!.jsonArray

        val result = mutableListOf<CommentThread>()
        var idCounter = 1L

        for (threadElement in threads) {
            val thread = threadElement.jsonObject
            if (thread["isResolved"]!!.jsonPrimitive.boolean) continue

            val comments = thread["comments"]!!.jsonObject["nodes"]!!.jsonArray
            if (comments.isEmpty()) continue

            val first = comments[0].jsonObject
            val path = first["path"]?.jsonPrimitive?.content ?: ""
            val line = when {
                first["line"] != null && first["line"] !is JsonNull -> first["line"]!!.jsonPrimitive.int
                first["originalLine"] != null && first["originalLine"] !is JsonNull -> first["originalLine"]!!.jsonPrimitive.int
                else -> null
            }
            val body = first["body"]?.jsonPrimitive?.content ?: ""
            val reviewer = first["author"]?.jsonObject?.get("login")?.jsonPrimitive?.content ?: "unknown"
            val diffHunk = first["diffHunk"]?.jsonPrimitive?.content ?: ""

            val replies = (1 until comments.size).map { i ->
                val reply = comments[i].jsonObject
                Reply(
                    user = reply["author"]?.jsonObject?.get("login")?.jsonPrimitive?.content ?: "unknown",
                    body = reply["body"]?.jsonPrimitive?.content ?: "",
                )
            }

            result.add(
                CommentThread(
                    id = idCounter++,
                    path = path,
                    line = line,
                    body = body,
                    reviewer = reviewer,
                    diffHunk = diffHunk,
                    replies = replies,
                )
            )
        }

        return result
    }

    override fun parsePrUrl(url: String): PrIdentifier? {
        val match = Regex("""https?://github\.com/([^/]+)/([^/]+)/pull/(\d+)""").find(url) ?: return null
        return PrIdentifier(
            owner = match.groupValues[1],
            repo = match.groupValues[2],
            number = match.groupValues[3].toInt(),
        )
    }

    override fun getPlaceholderUrl(): String = "https://github.com/owner/repo/pull/123"

    private fun buildJsonObject(vararg pairs: Pair<String, String>): String {
        val entries = pairs.joinToString(",") { (k, v) ->
            val escaped = v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            "\"$k\":\"$escaped\""
        }
        return "{$entries}"
    }
}
