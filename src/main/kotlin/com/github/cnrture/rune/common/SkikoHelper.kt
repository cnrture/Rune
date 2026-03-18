package com.github.cnrture.rune.common

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

/**
 * Ensures the Skiko native library is available for Compose Desktop rendering.
 *
 * IntelliJ 2024.3+ bundles Skiko classes in lib/modules/intellij.libraries.skiko.jar.
 * Due to parent-first classloading, the platform's Library class gets loaded but the
 * module classloader cannot expose native resources (.dylib/.so/.dll) via getResource().
 * This helper extracts the native library to a temp directory and sets skiko.library.path
 * so Skiko loads it from the filesystem instead.
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
            doEnsure()
        }
    }

    private fun doEnsure() {
        if (System.getProperty("skiko.library.path") != null) {
            LOG.info("skiko.library.path already set: ${System.getProperty("skiko.library.path")}")
            return
        }

        val nativeLibName = getNativeLibName()
        if (nativeLibName == null) {
            LOG.warn("Could not determine Skiko native library name for current platform")
            return
        }

        LOG.info("Ensuring Skiko native library: $nativeLibName")

        // Strategy 1: Use plugin's own classloader to find native lib from plugin JARs
        if (tryExtractViaClassloader(SkikoHelper::class.java.classLoader, "plugin", nativeLibName)) return

        // Strategy 2: Try Library class's protectionDomain to find the platform JAR
        if (tryExtractViaProtectionDomain(nativeLibName)) return

        // Strategy 3: Try Library class's getResource to locate the JAR
        if (tryExtractViaClassResource(nativeLibName)) return

        LOG.warn("All Skiko native library extraction strategies failed")
    }

    /**
     * Extract native library using a classloader's getResourceAsStream.
     * Works when the native lib JAR is on the classloader's classpath.
     */
    private fun tryExtractViaClassloader(cl: ClassLoader, label: String, nativeLibName: String): Boolean {
        return try {
            val stream = cl.getResourceAsStream(nativeLibName)
            if (stream == null) {
                LOG.info("Strategy '$label classloader': $nativeLibName not found as resource")
                return false
            }

            val tempDir = Files.createTempDirectory("skiko-native-")
            stream.use { input ->
                Files.copy(input, tempDir.resolve(nativeLibName), StandardCopyOption.REPLACE_EXISTING)
            }
            cl.getResourceAsStream("$nativeLibName.sha256")?.use { input ->
                Files.copy(input, tempDir.resolve("$nativeLibName.sha256"), StandardCopyOption.REPLACE_EXISTING)
            }

            val libFile = tempDir.resolve(nativeLibName).toFile()
            if (libFile.exists() && libFile.length() > 0) {
                System.setProperty("skiko.library.path", tempDir.toString())
                LOG.info("Extracted Skiko native library via $label classloader to $tempDir (${libFile.length()} bytes)")
                true
            } else {
                LOG.warn("Strategy '$label classloader': extracted file is empty")
                tempDir.toFile().deleteRecursively()
                false
            }
        } catch (e: Throwable) {
            LOG.info("Strategy '$label classloader' failed: ${e.message}")
            false
        }
    }

    /**
     * Find the JAR containing the Library class via protectionDomain.codeSource.location
     * and extract the native library from it.
     */
    private fun tryExtractViaProtectionDomain(nativeLibName: String): Boolean {
        return try {
            val libraryClass = Class.forName("org.jetbrains.skiko.Library")
            val location = libraryClass.protectionDomain?.codeSource?.location
            if (location == null) {
                LOG.info("Strategy 'protectionDomain': codeSource location is null")
                return false
            }

            LOG.info("Strategy 'protectionDomain': Library loaded from $location")
            val file = File(location.toURI())
            if (!file.exists() || !file.name.endsWith(".jar")) {
                LOG.info("Strategy 'protectionDomain': not a JAR file: $file")
                return false
            }

            val tempDir = Files.createTempDirectory("skiko-native-")
            if (extractFromJar(JarFile(file), nativeLibName, tempDir)) {
                System.setProperty("skiko.library.path", tempDir.toString())
                LOG.info("Extracted Skiko native library via protectionDomain to $tempDir")
                true
            } else {
                tempDir.toFile().deleteRecursively()
                false
            }
        } catch (e: Throwable) {
            LOG.info("Strategy 'protectionDomain' failed: ${e.message}")
            false
        }
    }

    /**
     * Find the JAR containing the Library class via getResource("Library.class")
     * and extract the native library from it.
     */
    private fun tryExtractViaClassResource(nativeLibName: String): Boolean {
        return try {
            val libraryClass = Class.forName("org.jetbrains.skiko.Library")
            val classResource = libraryClass.getResource("${libraryClass.simpleName}.class")
            if (classResource == null) {
                LOG.info("Strategy 'classResource': Library.class resource is null")
                return false
            }

            val classUrl = classResource.toString()
            LOG.info("Strategy 'classResource': Library.class URL = $classUrl")

            // Extract JAR path from URL formats like jar:file:/path/to/file.jar!/class/path
            val jarPath = when {
                classUrl.startsWith("jar:file:") ->
                    classUrl.substringAfter("jar:file:").substringBefore("!")
                classUrl.startsWith("jar:nested:") ->
                    classUrl.substringAfter("jar:nested:").substringBefore("!")
                else -> {
                    LOG.info("Strategy 'classResource': unrecognized URL protocol")
                    return false
                }
            }

            val jarFile = try {
                JarFile(File(URI("file:$jarPath")))
            } catch (_: Exception) {
                JarFile(File(jarPath))
            }

            val tempDir = Files.createTempDirectory("skiko-native-")
            if (extractFromJar(jarFile, nativeLibName, tempDir)) {
                System.setProperty("skiko.library.path", tempDir.toString())
                LOG.info("Extracted Skiko native library via classResource to $tempDir")
                true
            } else {
                tempDir.toFile().deleteRecursively()
                false
            }
        } catch (e: Throwable) {
            LOG.info("Strategy 'classResource' failed: ${e.message}")
            false
        }
    }

    private fun extractFromJar(jar: JarFile, nativeLibName: String, tempDir: Path): Boolean {
        var extracted = false
        jar.use { jarFile ->
            jarFile.getEntry(nativeLibName)?.let { entry ->
                jarFile.getInputStream(entry).use { input ->
                    Files.copy(input, tempDir.resolve(nativeLibName), StandardCopyOption.REPLACE_EXISTING)
                    extracted = true
                }
            }
            jarFile.getEntry("$nativeLibName.sha256")?.let { entry ->
                jarFile.getInputStream(entry).use { input ->
                    Files.copy(input, tempDir.resolve("$nativeLibName.sha256"), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        if (!extracted) {
            LOG.info("Native library $nativeLibName not found in JAR: ${jar.name}")
        }
        return extracted
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