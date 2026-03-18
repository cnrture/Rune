package com.github.cnrture.rune.service

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object BitbucketCredentialService {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("Rune", "BitbucketCredentials")
    )

    fun getUsername(): String? {
        return PasswordSafe.instance.get(credentialAttributes)?.userName
    }

    fun getToken(): String? {
        return PasswordSafe.instance.get(credentialAttributes)?.getPasswordAsString()
    }

    fun saveCredentials(username: String, apiToken: String) {
        PasswordSafe.instance.set(credentialAttributes, Credentials(username, apiToken))
    }

    fun hasCredentials(): Boolean {
        val credentials = PasswordSafe.instance.get(credentialAttributes) ?: return false
        // Token yeterli (Bearer auth), username opsiyonel (Basic auth için)
        return !credentials.getPasswordAsString().isNullOrBlank()
    }
}
