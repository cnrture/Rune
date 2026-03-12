package com.github.teknasyon.plugin.common

import java.io.File
import java.util.concurrent.TimeUnit

object CliUtils {

    private val cachedLoginShellPath: String? by lazy { resolveLoginShellPath() }

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
     * Returns the PATH value from the user's login shell.
     * This is needed because IntelliJ's process environment often lacks paths
     * configured in shell profiles (e.g., nvm, volta, homebrew).
     */
    fun getLoginShellPath(): String? = cachedLoginShellPath

    private fun resolveLoginShellPath(): String? {
        return try {
            val shell = System.getenv("SHELL") ?: "/bin/bash"
            val process = ProcessBuilder(shell, "-l", "-c", "echo \$PATH")
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            val finished = process.waitFor(Constants.TIMEOUT_CLI_LOOKUP_SECONDS, TimeUnit.SECONDS)
            if (!finished || process.exitValue() != 0) return null
            val path = process.inputStream.bufferedReader().readText().trim()
            if (path.isNotBlank()) path else null
        } catch (_: Exception) {
            null
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
            val pb = ProcessBuilder(*cmd)
                .directory(dir)
                .redirectErrorStream(true)
            getLoginShellPath()?.let { shellPath ->
                pb.environment()["PATH"] = shellPath
            }
            val process = pb.start()
            process.outputStream.close()
            // Read output in a separate thread to prevent buffer deadlock
            var output = ""
            val readerThread = Thread {
                output = process.inputStream.bufferedReader().readText().trim()
            }
            readerThread.isDaemon = true
            readerThread.start()
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                readerThread.join(2000)
                return output
            }
            readerThread.join(5000)
            output
        } catch (_: Exception) {
            ""
        }
    }
}
