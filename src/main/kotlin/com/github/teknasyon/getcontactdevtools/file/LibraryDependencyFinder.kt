package com.github.teknasyon.getcontactdevtools.file

import com.github.teknasyon.getcontactdevtools.common.Constants
import java.io.File

/**
 * Utility class to parse libs.versions.toml and find library dependencies in source files
 */
class LibraryDependencyFinder {

    // Data class to store library information from libs.versions.toml
    data class LibraryInfo(
        val alias: String,
        val group: String,
        val artifact: String,
        val versionRef: String? = null,
        val version: String? = null,
    ) {
        val module: String
            get() = "$group:$artifact"
    }

    /**
     * Parse the libs.versions.toml file to extract library definitions
     * @param projectRoot The root directory of the project
     * @return A list of LibraryInfo objects
     */
    fun parseLibsVersionsToml(projectRoot: File): List<LibraryInfo> {
        val libraries = mutableListOf<LibraryInfo>()

        val versionsTomlPath = "gradle/libs.versions.toml"
        var versionsToml = File(projectRoot, versionsTomlPath)

        // Some projects use different locations for libs.versions.toml
        if (!versionsToml.exists()) {
            versionsToml = File(projectRoot.parentFile, versionsTomlPath)
        }

        // If still not found, try at the root level
        if (!versionsToml.exists()) {
            versionsToml = File(projectRoot, "libs.versions.toml")
        }

        if (!versionsToml.exists()) {
            println("LibraryDependencyFinder: Could not find libs.versions.toml file")
            return emptyList()
        }

        println("LibraryDependencyFinder: Found libs.versions.toml at ${versionsToml.absolutePath}")
        val content = versionsToml.readText()

        // Find the [libraries] section
        val librariesSection = content.split("[libraries]")
        if (librariesSection.size < 2) return emptyList()
        println("Parsing libraries section: $librariesSection")

        val librariesContent = librariesSection[1].split("[").first()

        // Regular expression to match library declarations like:
        // name = { module = "group:artifact", version.ref = "version" }
        // Iki format: version.ref veya version parametresi (ikisi de opsiyonel)
        val modulePattern =
            """(\w+(?:-\w+)*)\s*=\s*\{\s*module\s*=\s*["']([^:"']+):([^"']+)["']\s*(?:,\s*version\.ref\s*=\s*["']([^"']+)["'])?\s*(?:,\s*version\s*=\s*["']([^"']+)["'])?\s*\}""".toRegex()

        // Regular expression to match library declarations like:
        // name = { group = "group", name = "artifact", version = "1.0.0" } or
        // name = { group = "group", name = "artifact", version.ref = "versionRef" }
        val groupNamePattern =
            """(\w+(?:-\w+)*)\s*=\s*\{\s*group\s*=\s*["']([^"']+)["']\s*,\s*name\s*=\s*["']([^"']+)["']\s*(?:,\s*version\.ref\s*=\s*["']([^"']+)["'])?\s*(?:,\s*version\s*=\s*["']([^"']+)["'])?\s*\}""".toRegex()

        // Find matches for module pattern
        val moduleMatches = modulePattern.findAll(librariesContent)
        moduleMatches.forEach { match ->
            val matchGroups = match.groupValues
            val alias = matchGroups[1]
            val group = matchGroups[2]
            val artifact = matchGroups[3]
            val versionRef = if (matchGroups.size > 4) matchGroups[4] else ""
            val version = if (matchGroups.size > 5) matchGroups[5] else ""

            libraries.add(LibraryInfo(alias, group, artifact, versionRef.ifEmpty { null }, version.ifEmpty { null }))
        }

        // Find matches for group/name pattern
        val groupNameMatches = groupNamePattern.findAll(librariesContent)
        groupNameMatches.forEach { match ->
            val matchGroups = match.groupValues
            val alias = matchGroups[1]
            val group = matchGroups[2]
            val artifact = matchGroups[3]
            val versionRef = if (matchGroups.size > 4) matchGroups[4] else ""
            val version = if (matchGroups.size > 5) matchGroups[5] else ""

            libraries.add(
                LibraryInfo(
                    alias,
                    group,
                    artifact,
                    versionRef.ifEmpty { null },
                    version.ifEmpty { null }
                )
            )
        }

        println("LibraryDependencyFinder: Found ${libraries.size} libraries in libs.versions.toml")
        libraries.forEach { library ->
            println("  - ${library.alias}: ${library.group}:${library.artifact}")
        }

        return libraries
    }

    /**
     * Find which libraries are imported in the given source directory
     * @param sourceDir The source directory to scan
     * @param libraries The list of available libraries
     * @return A list of library aliases that are found in imports
     */
    fun findImportedLibraries(sourceDir: File, libraries: List<LibraryInfo>): List<String> {
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            println("LibraryDependencyFinder: Source directory ${sourceDir.absolutePath} does not exist or is not a directory")
            return emptyList()
        }

        val usedLibraries = mutableSetOf<String>()
        val importRegex = """import\s+([\w.]+(?:\*)?)\s*""".toRegex()

        println("LibraryDependencyFinder: Scanning for imports in ${sourceDir.absolutePath}")

        // Tüm dosyaları topla
        val sourceFiles = sourceDir.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .toList()

        if (sourceFiles.isEmpty()) {
            println("LibraryDependencyFinder: No source files found")
            return emptyList()
        }

        println("LibraryDependencyFinder: Found ${sourceFiles.size} source files")

        // First, extract all imports from source files
        val allImports = mutableSetOf<String>()
        sourceFiles.forEach { file ->
            val content = file.readText()
            importRegex.findAll(content).forEach { matchResult ->
                allImports.add(matchResult.groupValues[1])
            }
        }

        val fileContent = sourceFiles.joinToString("\n") { it.readText() }

        // Now check each library against our imports
        libraries.forEach { library ->
            val group = library.group
            val artifact = library.artifact.replace("-", "")

            // Kütüphanenin "kullanım skoru"nu hesapla
            val usageScore = calculateLibraryUsageScore(group, artifact, allImports, fileContent)

            // Belirli bir eşiği geçerse kütüphaneyi kullanılmış olarak işaretle
            if (usageScore >= LIBRARY_USAGE_THRESHOLD) {
                usedLibraries.add(library.alias)
                println("LibraryDependencyFinder: Detected usage of library ${library.alias} (${library.group}:${library.artifact}) with score $usageScore")
            } else {
                println("LibraryDependencyFinder: Library ${library.alias} (${library.group}:${library.artifact}) not detected (score: $usageScore)")
            }
        }

        println("LibraryDependencyFinder: Found ${usedLibraries.size} used libraries out of ${libraries.size} available")
        return usedLibraries.toList()
    }

    // Kütüphane kullanım skorlaması için eşik değeri
    private val LIBRARY_USAGE_THRESHOLD = 2

    /**
     * Kütüphane kullanımını tespit etmek için bir skor hesaplar
     * Çeşitli faktörlere göre puan vererek, gerçek kullanımları daha doğru tespit etmeye çalışır
     */
    private fun calculateLibraryUsageScore(
        group: String,
        artifact: String,
        imports: Set<String>,
        fileContent: String,
    ): Int {
        val normalizedArtifact = artifact.replace("-", "")
        var score = 0

        // Grup adının parçaları (com.squareup.retrofit2 -> [com, squareup, retrofit2])
        val groupParts = group.split(".")
        val lastGroupPart = groupParts.lastOrNull()?.replace("-", "") ?: ""

        // 1. Tam olarak grup.artifact importu var mı? (+3 puan)
        if (imports.any { it == "$group.$artifact" || it == "$group.$normalizedArtifact" }) {
            println("  - Found exact import match for $group.$artifact")
            score += 3
        }

        // 2. group.* şeklinde wildcard import var mı? (+1 puan)
        if (imports.any { it == "$group.*" }) {
            println("  - Found wildcard import for $group.*")
            score += 1
        }

        // 3. group.artifact ile başlayan alt paket importları var mı? (+2 puan)
        if (imports.any { it.startsWith("$group.$artifact.") || it.startsWith("$group.$normalizedArtifact.") }) {
            println("  - Found subpackage imports for $group.$artifact")
            score += 2
        }

        // 4. group ile başlayan herhangi bir import var mı? (+1 puan)
        val hasGroupImports = imports.any { it.startsWith("$group.") }
        if (hasGroupImports) {
            println("  - Found imports starting with $group")
            score += 1
        }

        // 5. En son grup parçası (örn: retrofit2) ve artifact importlarda geçiyor mu? (+2 puan)
        if (lastGroupPart.isNotEmpty() && artifact.isNotEmpty() &&
            imports.any { it.contains(lastGroupPart) && it.contains(normalizedArtifact) }
        ) {
            println("  - Found imports containing both $lastGroupPart and $normalizedArtifact")
            score += 2
        }

        // 6. Içerikte artifact adı büyük harfle başlayan sınıf olarak geçiyor mu? (+1 puan)
        val capitalizedArtifact = normalizedArtifact.replaceFirstChar { it.uppercase() }
        if (capitalizedArtifact.length > 3 && fileContent.contains(capitalizedArtifact)) {
            println("  - Found class usage: $capitalizedArtifact")
            score += 1
        }

        // 7. En son grup parçası importlarda geçiyor mu? (örn: retrofit2) (+1 puan)
        // Bu genelde kütüphanenin bir parçası olduğunu gösterir
        if (lastGroupPart.length > 3 && imports.any { it.contains(lastGroupPart) }) {
            println("  - Found imports containing $lastGroupPart")
            score += 1
        }

        return score
    }

    /**
     * Format library dependencies for the build.gradle file
     * @param libraryAliases List of library aliases to include
     * @return A formatted string with library dependencies
     */
    fun formatLibraryDependencies(libraryAliases: List<String>): String {
        if (libraryAliases.isEmpty()) return Constants.EMPTY

        return StringBuilder().apply {
            append("    // Library Dependencies\n")
            libraryAliases.forEachIndexed { index, alias ->
                append("    implementation(libs.${alias.replace("-", ".")})")
                if (index != libraryAliases.lastIndex) append("\n")
            }
        }.toString()
    }
}