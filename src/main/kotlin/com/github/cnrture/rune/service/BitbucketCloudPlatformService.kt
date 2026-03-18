package com.github.cnrture.rune.service

import com.github.cnrture.rune.actions.dialog.CommentThread
import com.github.cnrture.rune.actions.dialog.Reply
import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.common.VcsProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

class BitbucketCloudPlatformService(
    private val workspace: String,
    private val repoSlug: String,
) : VcsPlatformService {

    override val providerType = VcsProvider.BITBUCKET_CLOUD
    override val supportsLabels = false
    override val supportsAssignee = false

    private val baseUrl = "https://api.bitbucket.org"

    override fun fetchReviewerCandidates(): Result<List<VcsUser>> {
        return try {
            val response = httpGet("/2.0/workspaces/$workspace/members?pagelen=100")
            val root = Json.parseToJsonElement(response).jsonObject
            val values = root["values"]?.jsonArray ?: return Result.success(emptyList())
            val users = values.mapNotNull { element ->
                val user = element.jsonObject["user"]?.jsonObject ?: return@mapNotNull null
                val displayName = user["display_name"]?.jsonPrimitive?.content ?: ""
                val username = user["nickname"]?.jsonPrimitive?.content
                    ?: user["username"]?.jsonPrimitive?.content ?: ""
                val uuid = user["uuid"]?.jsonPrimitive?.content ?: ""
                VcsUser(displayName = displayName, username = username, id = uuid)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchLabels(): Result<List<String>> {
        return Result.success(emptyList())
    }

    override fun createPullRequest(
        currentBranch: String,
        targetBranch: String,
        title: String,
        body: String,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ): PrCreationResult {
        return try {
            val reviewerArray = reviewers.joinToString(",") { """{"uuid":"${it.id}"}""" }
            val requestBody = """
                {
                    "title": ${escapeJsonString(title)},
                    "description": ${escapeJsonString(body)},
                    "source": {"branch": {"name": ${escapeJsonString(currentBranch)}}},
                    "destination": {"branch": {"name": ${escapeJsonString(targetBranch)}}},
                    "reviewers": [$reviewerArray],
                    "close_source_branch": true
                }
            """.trimIndent()

            val response = httpPost("/2.0/repositories/$workspace/$repoSlug/pullrequests", requestBody)
            val root = Json.parseToJsonElement(response).jsonObject
            val prUrl = root["links"]?.jsonObject
                ?.get("html")?.jsonObject
                ?.get("href")?.jsonPrimitive?.content
            PrCreationResult(url = prUrl)
        } catch (e: Exception) {
            PrCreationResult(errorMessage = "Failed to create PR: ${e.message}")
        }
    }

    override fun createLabel(name: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Bitbucket Cloud does not support labels"))
    }

    override fun findExistingPr(currentBranch: String): String? {
        return try {
            val encodedBranch = currentBranch.replace("\"", "\\\"")
            val response = httpGet(
                "/2.0/repositories/$workspace/$repoSlug/pullrequests" +
                    "?q=source.branch.name=\"$encodedBranch\"&state=OPEN"
            )
            val root = Json.parseToJsonElement(response).jsonObject
            val values = root["values"]?.jsonArray
            if (values != null && values.isNotEmpty()) {
                values[0].jsonObject["links"]?.jsonObject
                    ?.get("html")?.jsonObject
                    ?.get("href")?.jsonPrimitive?.content
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override fun fetchPrTitle(prIdentifier: PrIdentifier): Result<String> {
        return try {
            val response = httpGet(
                "/2.0/repositories/${prIdentifier.owner}/${prIdentifier.repo}/pullrequests/${prIdentifier.number}"
            )
            val root = Json.parseToJsonElement(response).jsonObject
            val title = root["title"]?.jsonPrimitive?.content ?: ""
            Result.success(title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchUnresolvedComments(prIdentifier: PrIdentifier): Result<List<CommentThread>> {
        return try {
            val response = httpGet(
                "/2.0/repositories/${prIdentifier.owner}/${prIdentifier.repo}" +
                    "/pullrequests/${prIdentifier.number}/comments?pagelen=100"
            )
            val root = Json.parseToJsonElement(response).jsonObject
            val values = root["values"]?.jsonArray ?: return Result.success(emptyList())

            val threads = mutableListOf<CommentThread>()
            var idCounter = 1L

            // Group: top-level comments (no parent) become threads, replies are nested
            val commentMap = mutableMapOf<Long, JsonObject>()
            val childMap = mutableMapOf<Long, MutableList<JsonObject>>()

            for (element in values) {
                val comment = element.jsonObject
                val commentId = comment["id"]?.jsonPrimitive?.int?.toLong() ?: continue
                val parentId = comment["parent"]?.jsonObject?.get("id")?.jsonPrimitive?.int?.toLong()
                commentMap[commentId] = comment
                if (parentId != null) {
                    childMap.getOrPut(parentId) { mutableListOf() }.add(comment)
                }
            }

            for ((commentId, comment) in commentMap) {
                // Skip replies (they have parent)
                if (comment["parent"] != null) continue
                // Skip deleted comments
                if (comment["deleted"]?.jsonPrimitive?.boolean == true) continue

                val inline = comment["inline"]?.jsonObject
                val path = inline?.get("path")?.jsonPrimitive?.content ?: continue
                val line = inline["to"]?.jsonPrimitive?.int ?: inline["from"]?.jsonPrimitive?.int

                val body = comment["content"]?.jsonObject?.get("raw")?.jsonPrimitive?.content ?: ""
                val reviewer = comment["user"]?.jsonObject?.get("display_name")?.jsonPrimitive?.content ?: "unknown"

                val replies = childMap[commentId]?.mapNotNull { reply ->
                    if (reply["deleted"]?.jsonPrimitive?.boolean == true) return@mapNotNull null
                    Reply(
                        user = reply["user"]?.jsonObject?.get("display_name")?.jsonPrimitive?.content ?: "unknown",
                        body = reply["content"]?.jsonObject?.get("raw")?.jsonPrimitive?.content ?: "",
                    )
                } ?: emptyList()

                threads.add(
                    CommentThread(
                        id = idCounter++,
                        path = path,
                        line = line,
                        body = body,
                        reviewer = reviewer,
                        diffHunk = "",
                        replies = replies,
                    )
                )
            }

            Result.success(threads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun parsePrUrl(url: String): PrIdentifier? {
        val match = Regex("""https?://bitbucket\.org/([^/]+)/([^/]+)/pull-requests/(\d+)""").find(url) ?: return null
        return PrIdentifier(
            owner = match.groupValues[1],
            repo = match.groupValues[2],
            number = match.groupValues[3].toInt(),
        )
    }

    override fun getPlaceholderUrl(): String = "https://bitbucket.org/workspace/repo/pull-requests/123"

    // region HTTP helpers

    private fun getAuthHeader(): String {
        val username = BitbucketCredentialService.getUsername()
        val token = BitbucketCredentialService.getToken() ?: ""
        // Username varsa Basic Auth (App Password), yoksa Bearer (Workspace/Repo Access Token)
        return if (!username.isNullOrBlank()) {
            "Basic " + Base64.getEncoder().encodeToString("$username:$token".toByteArray())
        } else {
            "Bearer $token"
        }
    }

    private fun httpGet(path: String): String {
        val url = URI("$baseUrl$path").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", getAuthHeader())
        connection.connectTimeout = Constants.TIMEOUT_HTTP_MS
        connection.readTimeout = Constants.TIMEOUT_HTTP_MS

        return try {
            if (connection.responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                throw RuntimeException("HTTP ${connection.responseCode}: $errorBody")
            }
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun httpPost(path: String, body: String): String {
        val url = URI("$baseUrl$path").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", getAuthHeader())
        connection.connectTimeout = Constants.TIMEOUT_HTTP_MS
        connection.readTimeout = Constants.TIMEOUT_HTTP_MS
        connection.doOutput = true

        return try {
            connection.outputStream.use { it.write(body.toByteArray()) }
            if (connection.responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                throw RuntimeException("HTTP ${connection.responseCode}: $errorBody")
            }
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun escapeJsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    // endregion
}
