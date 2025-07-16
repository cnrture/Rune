package com.github.teknasyon.getcontactdevtools.toolwindow.manager.newsletter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class NewsItem(
    val id: String,
    val content: List<String>,
    val date: String,
    val slackUrl: String,
)

class NewsService {
    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val apiEndpoint = "https://api.canerture.com/gtcnews/news"

    suspend fun fetchNews(): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    println("Fetched ${response.body()} messages successfully.")
                    json.decodeFromString<List<NewsItem>>(response.body())
                } else {
                    println("API Error: ${response.statusCode()}")
                    emptyList()
                }
            } catch (e: Exception) {
                println("Error fetching team messages: ${e.message}")
                emptyList()
            }
        }
    }
}