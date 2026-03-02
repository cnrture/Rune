package com.github.teknasyon.plugin.common

import java.io.File
import java.util.concurrent.TimeUnit

object CliUtils {

    fun findGhCli(): String? {
        return try {
            val shell = System.getenv("SHELL") ?: "/bin/bash"
            val process = ProcessBuilder(shell, "-l", "-c", "which gh")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(Constants.TIMEOUT_CLI_LOOKUP_SECONDS, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            null
        } ?: run {
            listOf("/usr/local/bin/gh", "/usr/bin/gh", "/opt/homebrew/bin/gh")
                .firstOrNull { File(it).exists() }
        }
    }

    fun findClaudeCli(): String? {
        return try {
            val shell = System.getenv("SHELL") ?: "/bin/bash"
            val process = ProcessBuilder(shell, "-l", "-c", "which claude")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(Constants.TIMEOUT_CLI_LOOKUP_SECONDS, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            null
        } ?: run {
            val home = System.getProperty("user.home")
            listOf(
                "/usr/local/bin/claude",
                "/usr/bin/claude",
                "/opt/homebrew/bin/claude",
                "$home/.npm-global/bin/claude",
                "$home/.local/bin/claude",
                "$home/.nvm/current/bin/claude",
                "$home/.bun/bin/claude",
                "$home/.volta/bin/claude",
            ).firstOrNull { File(it).exists() }
        }
    }

    fun isClaudeInstalled(): Boolean {
        return findClaudeCli() != null
    }

    fun runGit(dir: File, vararg args: String): String {
        return runProcess(dir, "git", *args)
    }

    fun runProcess(dir: File, vararg cmd: String, timeoutSeconds: Long = Constants.TIMEOUT_PROCESS_DEFAULT_SECONDS): String {
        return try {
            val process = ProcessBuilder(*cmd)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
            ""
        }
    }
}
