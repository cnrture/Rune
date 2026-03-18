package com.github.cnrture.rune.service

import com.github.cnrture.rune.common.Constants
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

object JiraService {

    private val JIRA_BASE_URL = Constants.JIRA_BASE_URL

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("TPDevTools", "JiraCredentials")
    )

    fun getEmail(): String? {
        return PasswordSafe.instance.get(credentialAttributes)?.userName
    }

    fun saveCredentials(email: String, apiToken: String) {
        PasswordSafe.instance.set(credentialAttributes, Credentials(email, apiToken))
    }

    fun hasCredentials(): Boolean {
        val credentials = PasswordSafe.instance.get(credentialAttributes) ?: return false
        return !credentials.userName.isNullOrBlank() && !credentials.getPasswordAsString().isNullOrBlank()
    }

    fun fetchFixVersions(ticketId: String): List<String> {
        val credentials = PasswordSafe.instance.get(credentialAttributes) ?: return emptyList()
        val email = credentials.userName ?: return emptyList()
        val token = credentials.getPasswordAsString() ?: return emptyList()

        val url = URI("$JIRA_BASE_URL/rest/api/3/issue/$ticketId?fields=fixVersions").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty(
            "Authorization",
            "Basic " + Base64.getEncoder().encodeToString("$email:$token".toByteArray())
        )
        connection.connectTimeout = Constants.TIMEOUT_HTTP_MS
        connection.readTimeout = Constants.TIMEOUT_HTTP_MS

        return try {
            if (connection.responseCode != 200) return emptyList()
            val body = connection.inputStream.bufferedReader().readText()
            val json = Json.parseToJsonElement(body)
            json.jsonObject["fields"]
                ?.jsonObject?.get("fixVersions")
                ?.jsonArray
                ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
