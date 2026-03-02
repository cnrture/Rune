package com.github.teknasyon.plugin.common

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

/**
 * Ensures the Skiko native library is available for Compose Desktop rendering.
 *
 * IntelliJ 2024.3+ bundles Skiko classes in lib/modules/intellij.libraries.skiko.jar,
 * but the module classloader cannot expose native resources (.dylib/.so/.dll) via getResource().
 * This helper extracts the native library from the JAR to a temp directory and sets
 * skiko.library.path so Skiko loads it from the filesystem instead.
 */
object SkikoHelper {

    private val LOG = Logger.getInstance(SkikoHelper::class.java)

    @Volatile
    private var initialized = false

    @JvmStatic
    fun ensureNativeLibrary() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            doEnsureNativeLibrary()
        }
    }

    private fun doEnsureNativeLibrary() {
        if (System.getProperty("skiko.library.path") != null) return

        try {
            val libraryClass = Class.forName("org.jetbrains.skiko.Library")
            val nativeLibName = getNativeLibName() ?: return

            // Check if the classloader can already find the native resource
            if (libraryClass.classLoader.getResource("$nativeLibName.sha256") != null) {
                return
            }

            LOG.info("Skiko native resource not accessible via classloader, extracting from JAR")

            // Find the JAR containing the Library class
            val classResource = libraryClass.getResource("${libraryClass.simpleName}.class") ?: return
            val classUrl = classResource.toString()

            val jarPath = when {
                classUrl.startsWith("jar:file:") -> {
                    classUrl.substringAfter("jar:file:").substringBefore("!")
                }
                classUrl.startsWith("jar:nested:") -> {
                    // Some classloaders use nested: protocol
                    classUrl.substringAfter("jar:nested:").substringBefore("!")
                }
                else -> {
                    LOG.warn("Unexpected Skiko Library class URL protocol: $classUrl")
                    return
                }
            }

            val jarFile = try {
                JarFile(File(URI("file:$jarPath")))
            } catch (e: Exception) {
                // URI parsing failed, try direct path
                JarFile(File(jarPath))
            }

            val tempDir = Files.createTempDirectory("skiko-native-")

            var extracted = false

            jarFile.use { jar ->
                jar.getEntry(nativeLibName)?.let { entry ->
                    jar.getInputStream(entry).use { input ->
                        val target = tempDir.resolve(nativeLibName)
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                        extracted = true
                    }
                }

                jar.getEntry("$nativeLibName.sha256")?.let { entry ->
                    jar.getInputStream(entry).use { input ->
                        val target = tempDir.resolve("$nativeLibName.sha256")
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }

            if (extracted) {
                System.setProperty("skiko.library.path", tempDir.toString())
                LOG.info("Extracted Skiko native library ($nativeLibName) to $tempDir")
            } else {
                LOG.warn("Skiko native library ($nativeLibName) not found in JAR: $jarPath")
                // Clean up empty temp dir
                tempDir.toFile().delete()
            }
        } catch (e: Throwable) {
            LOG.warn("Failed to ensure Skiko native library availability", e)
        }
    }

    private fun getNativeLibName(): String? {
        val os = System.getProperty("os.name")?.lowercase() ?: return null
        val arch = System.getProperty("os.arch")?.lowercase() ?: return null

        val platform = when {
            os.contains("mac") && (arch == "aarch64" || arch == "arm64") -> "macos-arm64"
            os.contains("mac") -> "macos-x64"
            os.contains("linux") && (arch == "aarch64" || arch == "arm64") -> "linux-arm64"
            os.contains("linux") -> "linux-x64"
            os.contains("win") && (arch.contains("amd64") || arch == "x86_64") -> "windows-x64"
            os.contains("win") -> "windows-arm64"
            else -> return null
        }

        return when {
            os.contains("win") -> "skiko-$platform.dll"
            os.contains("mac") -> "libskiko-$platform.dylib"
            else -> "libskiko-$platform.so"
        }
    }
}
