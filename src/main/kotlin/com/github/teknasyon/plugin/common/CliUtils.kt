package com.github.teknasyon.plugin.common

import java.io.File
import java.util.concurrent.TimeUnit

object CliUtils {

    fun findGhCli(): String? {
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which gh")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(Constants.TIMEOUT_CLI_LOOKUP_SECONDS, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            listOf("/usr/local/bin/gh", "/usr/bin/gh", "/opt/homebrew/bin/gh")
                .firstOrNull { File(it).exists() }
        }
    }

    fun findClaudeCli(): String? {
        return try {
            val process = ProcessBuilder("bash", "-l", "-c", "which claude")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            process.waitFor(Constants.TIMEOUT_CLI_LOOKUP_SECONDS, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        } catch (_: Exception) {
            val home = System.getProperty("user.home")
            listOf(
                "/usr/local/bin/claude",
                "/usr/bin/claude",
                "$home/.npm-global/bin/claude",
                "$home/.local/bin/claude",
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
