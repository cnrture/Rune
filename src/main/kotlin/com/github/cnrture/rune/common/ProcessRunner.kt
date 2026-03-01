package com.github.cnrture.rune.common

import java.io.File
import java.util.concurrent.TimeUnit

object ProcessRunner {

    /**
     * Runs a process and returns its output.
     * Returns empty string on any failure (timeout, non-zero exit, exception).
     */
    fun run(
        dir: File,
        vararg cmd: String,
        timeoutSeconds: Long = 30,
    ): String {
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

    /**
     * Runs a process and returns its output.
     * Throws [RuntimeException] on timeout or non-zero exit code.
     */
    fun runOrThrow(
        dir: File,
        vararg cmd: String,
        timeoutSeconds: Long = 30,
    ): String {
        val process = ProcessBuilder(*cmd)
            .directory(dir)
            .redirectErrorStream(true)
            .start()
        process.outputStream.close()
        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        val output = process.inputStream.bufferedReader().readText().trim()
        if (!completed || process.exitValue() != 0) {
            throw RuntimeException(output.ifBlank { "Command timed out" })
        }
        return output
    }

    fun git(dir: File, vararg args: String, timeoutSeconds: Long = 30): String {
        return run(dir, "git", *args, timeoutSeconds = timeoutSeconds)
    }
}
