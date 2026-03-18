package com.github.cnrture.rune.service

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object GitHubCredentialService {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("TPDevTools", "GitHubCredentials")
    )

    fun getToken(): String? {
        return PasswordSafe.instance.get(credentialAttributes)?.getPasswordAsString()
    }

    fun saveToken(token: String) {
        PasswordSafe.instance.set(credentialAttributes, Credentials("github", token))
    }

    fun hasCredentials(): Boolean {
        return !getToken().isNullOrBlank()
    }
}
