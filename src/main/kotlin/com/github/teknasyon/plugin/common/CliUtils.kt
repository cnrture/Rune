package com.github.teknasyon.plugin.common

import java.io.File
import java.util.concurrent.TimeUnit

object CliUtils {

    fun findGhCli(): String? {
        return resolveViaShell("gh") ?: run {
            listOf("/usr/local/bin/gh", "/usr/bin/gh", "/opt/homebrew/bin/gh")
                .firstOrNull { File(it).exists() }
        }
    }

    fun findClaudeCli(): String? {
        return resolveViaShell("claude") ?: run {
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

    /**
     * Resolves a CLI tool path via the user's login shell.
     * Returns null if the tool is not found or the shell output is not a valid path.
     */
    private fun resolveViaShell(command: String): String? {
        return try {
            val shell = System.getenv("SHELL") ?: "/bin/bash"
            val process = ProcessBuilder(shell, "-l", "-c", "which $command")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            val finished = process.waitFor(Constants.TIMEOUT_CLI_LOOKUP_SECONDS, TimeUnit.SECONDS)
            if (!finished || process.exitValue() != 0) return null
            val path = process.inputStream.bufferedReader().readText().trim()
            if (path.isNotBlank() && File(path).exists()) path else null
        } catch (_: Exception) {
            null
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
