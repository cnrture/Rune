package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.common.Constants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI

class GitHubApiClient(private val token: String) {

    private val baseUrl = "https://api.github.com"

    fun get(path: String): ApiResponse {
        val url = URI("$baseUrl$path").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        configureConnection(connection)
        return executeRequest(connection)
    }

    fun getPaginated(path: String): List<String> {
        val results = mutableListOf<String>()
        var nextUrl: String? = "$baseUrl$path"

        while (nextUrl != null) {
            val url = URI(nextUrl).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            configureConnection(connection)

            try {
                val code = connection.responseCode
                if (code !in 200..299) break

                val body = connection.inputStream.bufferedReader().readText()
                results.add(body)
                nextUrl = parseLinkHeader(connection.getHeaderField("Link"))
            } catch (_: Exception) {
                break
            } finally {
                connection.disconnect()
            }
        }
        return results
    }

    fun post(path: String, jsonBody: String): ApiResponse {
        val url = URI("$baseUrl$path").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        configureConnection(connection)
        connection.outputStream.use { it.write(jsonBody.toByteArray()) }
        return executeRequest(connection)
    }

    fun graphql(query: String): ApiResponse {
        val escapedQuery = query
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
        val body = """{"query":"$escapedQuery"}"""
        val url = URI("$baseUrl/graphql").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        configureConnection(connection)
        connection.outputStream.use { it.write(body.toByteArray()) }
        return executeRequest(connection)
    }

    private fun configureConnection(connection: HttpURLConnection) {
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.connectTimeout = Constants.TIMEOUT_HTTP_MS
        connection.readTimeout = Constants.TIMEOUT_HTTP_MS
    }

    private fun executeRequest(connection: HttpURLConnection): ApiResponse {
        return try {
            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }
            ApiResponse(code, body)
        } catch (e: Exception) {
            ApiResponse(-1, e.message ?: "Connection failed")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLinkHeader(header: String?): String? {
        if (header == null) return null
        val nextPattern = Regex("""<([^>]+)>;\s*rel="next"""")
        return nextPattern.find(header)?.groupValues?.get(1)
    }

    data class ApiResponse(val statusCode: Int, val body: String) {
        val isSuccess: Boolean get() = statusCode in 200..299

        fun parseErrorMessage(): String {
            return try {
                val json = Json.parseToJsonElement(body).jsonObject
                json["message"]?.jsonPrimitive?.content ?: "HTTP $statusCode"
            } catch (_: Exception) {
                "HTTP $statusCode: $body"
            }
        }
    }
}
