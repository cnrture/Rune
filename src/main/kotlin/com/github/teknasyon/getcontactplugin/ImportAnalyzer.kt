package com.github.teknasyon.getcontactplugin

import java.io.File

class ImportAnalyzer() {

    private val modulePackageMapping = mapOf(
        ":plugin:iap" to listOf("app.source.getcontact.iap"),
    )

    fun analyzeSourceDirectory(directory: File): List<String> {
        val requiredModules = mutableListOf<String>()
        val sourceFiles = findAllSourceFiles(directory)

        sourceFiles.forEach { file ->
            val imports = extractImports(file)
            val modules = mapImportsToModules(imports)
            requiredModules.addAll(modules)
        }

        return requiredModules.distinct()
    }

    private fun findAllSourceFiles(directory: File): List<File> {
        val sourceFiles = mutableListOf<File>()

        directory.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "kt" || file.extension == "java")) {
                sourceFiles.add(file)
            }
        }

        return sourceFiles
    }

    private fun extractImports(file: File): List<String> {
        val imports = mutableListOf<String>()

        try {
            file.readLines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.startsWith("import ")) {
                    val importPath = trimmedLine.removePrefix("import ").removeSuffix(";").trim()
                    imports.add(importPath)
                }
            }
        } catch (e: Exception) {
            println("Error reading file ${file.absolutePath}: ${e.message}")
        }

        return imports
    }

    private fun mapImportsToModules(imports: List<String>): List<String> {
        val modules = mutableListOf<String>()

        imports.forEach { importPath ->
            modulePackageMapping.forEach { (module, packages) ->
                packages.forEach { packagePrefix ->
                    if (importPath.startsWith(packagePrefix)) {
                        modules.add(module)
                    }
                }
            }
        }

        return modules.distinct()
    }

    fun discoverProjectModules() {
        // Projeyi tarayarak modül-paket eşleşmelerini güncelleme
        // Bu kısım gerçek implementasyonda geliştirilmelidir
    }
}
