package com.github.cnrture.rune.service

import java.io.File
import java.util.concurrent.TimeUnit

object CliDiscoveryService {

    /**
     * Finds the Claude CLI path using login shell for full PATH resolution.
     * Falls back to common install locations if `which` fails.
     */
    fun findClaudeCli(): String? {
        return findViaBashWhich("claude") ?: run {
            val home = System.getProperty("user.home")
            listOf(
                "/usr/local/bin/claude",
                "/usr/bin/claude",
                "$home/.npm-global/bin/claude",
                "$home/.local/bin/claude",
            ).firstOrNull { File(it).exists() }
        }
    }

    /**
     * Checks if Claude CLI is installed (returns boolean, no path).
     * Uses exit code check for efficiency instead of reading stdout.
     */
    fun isClaudeInstalled(): Boolean {
        val foundViaShell = try {
            val process = ProcessBuilder("bash", "-l", "-c", "which claude")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
        if (foundViaShell) return true

        val home = System.getProperty("user.home")
        return listOf(
            "/usr/local/bin/claude",
            "/usr/bin/claude",
            "$home/.npm-global/bin/claude",
            "$home/.local/bin/claude",
        ).any { File(it).exists() }
    }

    /**
     * Finds the GitHub CLI (gh) path using login shell for full PATH resolution.
     * Falls back to common install locations if `which` fails.
     */
    fun findGhCli(): String? {
        return findViaBashWhich("gh") ?: run {
            val home = System.getProperty("user.home")
            listOf(
                "/usr/local/bin/gh",
                "/usr/bin/gh",
                "/opt/homebrew/bin/gh",
                "$home/.local/bin/gh",
            ).firstOrNull { File(it).exists() }
        }
    }

    private fun findViaBashWhich(command: String): String? {
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which $command")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(5, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
