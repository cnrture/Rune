package com.github.cnrture.rune.service

import com.github.cnrture.rune.common.Constants
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URI

data class RemoteCommand(val command: String, val description: String, val icon: String)

@Service
class CommandsRepository {

    private val log = Logger.getInstance(CommandsRepository::class.java)

    init {
        ApplicationManager.getApplication().executeOnPooledThread {
            fetchCommands()
        }
    }

    @Volatile
    private var cachedClaudeCommands: List<RemoteCommand>? = null

    @Volatile
    private var cachedScCommands: List<RemoteCommand>? = null

    @Volatile
    var isFetching: Boolean = true
        private set

    fun getClaudeCommands(): List<RemoteCommand>? = cachedClaudeCommands

    fun getScCommands(): List<RemoteCommand>? = cachedScCommands

    fun fetchCommands() {
        isFetching = true
        try {
            val body = fetchJson(REMOTE_URL) ?: loadBundled() ?: return
            parseAndCache(body)
        } catch (e: Exception) {
            log.warn("Remote commands fetch failed, trying bundled fallback", e)
            try {
                val bundled = loadBundled() ?: return
                parseAndCache(bundled)
            } catch (e2: Exception) {
                log.warn("Bundled commands fallback also failed", e2)
            }
        } finally {
            isFetching = false
        }
    }

    private fun parseAndCache(body: String) {
        val root = Json.parseToJsonElement(body).jsonObject
        cachedClaudeCommands = parseCommandList(root, "claude_commands")
        cachedScCommands = parseCommandList(root, "sc_commands")
}

    private fun parseCommandList(root: JsonObject, key: String): List<RemoteCommand> {
        return root[key]?.jsonArray?.mapNotNull { element ->
            val obj = element.jsonObject
            val command = obj["command"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val description = obj["description"]?.jsonPrimitive?.content ?: ""
            val icon = obj["icon"]?.jsonPrimitive?.content ?: "help"
            RemoteCommand(command, description, icon)
        } ?: emptyList()
    }

    private fun fetchJson(url: String): String? {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = Constants.TIMEOUT_HTTP_MS
        connection.readTimeout = Constants.TIMEOUT_HTTP_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Cache-Control", "no-cache, no-store")
        connection.useCaches = false
        return try {
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun loadBundled(): String? {
        return CommandsRepository::class.java.classLoader
            .getResourceAsStream("commands.json")
            ?.bufferedReader()
            ?.readText()
    }

    companion object {
        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/cnrture/Rune/master/src/main/resources/commands.json"

        fun getInstance(): CommandsRepository =
            ApplicationManager.getApplication().getService(CommandsRepository::class.java)
    }
}
