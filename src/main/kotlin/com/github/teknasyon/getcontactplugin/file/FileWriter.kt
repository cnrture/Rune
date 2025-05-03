package com.github.teknasyon.getcontactplugin.file

import com.github.teknasyon.getcontactplugin.common.Constants
import com.github.teknasyon.getcontactplugin.template.FeatureTemplate
import com.github.teknasyon.getcontactplugin.template.GitIgnoreTemplate
import com.github.teknasyon.getcontactplugin.template.ManifestTemplate
import com.github.teknasyon.getcontactplugin.template.TemplateWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class FileWriter {
    private val templateWriter = TemplateWriter()

    fun createModule(
        settingsGradleFile: File,
        workingDirectory: File,
        modulePathAsString: String,
        moduleType: String,
        showErrorDialog: (String) -> Unit,
        showSuccessDialog: () -> Unit,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean,
        dependencies: List<String> = emptyList(),
    ): List<File> {
        val filesCreated = mutableListOf<File>()

        val fileReady = modulePathAsString.replace(":", "/")

        val path = Paths.get(workingDirectory.toURI())
        val modulePath = Paths.get(path.toString(), fileReady)
        val moduleFile = File(modulePath.absolutePathString())

        val moduleName = modulePathAsString.split(":").last()

        if (moduleName.isEmpty()) {
            showErrorDialog("Module name empty / not as expected (is it formatted as :module?)")
            return emptyList()
        }

        moduleFile.mkdirs()

        addToSettingsAtCorrectLocation(
            settingsGradleFile = settingsGradleFile,
            modulePathAsString = modulePathAsString,
        )

        filesCreated += createDefaultModuleStructure(
            moduleFile = moduleFile,
            moduleName = moduleName,
            moduleType = moduleType,
            packageName = packageName,
            addReadme = addReadme,
            addGitIgnore = addGitIgnore,
            dependencies = dependencies,
        )

        showSuccessDialog()

        return filesCreated
    }

    private fun createDefaultModuleStructure(
        moduleFile: File,
        moduleName: String,
        moduleType: String,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean,
        dependencies: List<String> = emptyList(),
    ): List<File> {
        val filesCreated = mutableListOf<File>()

        filesCreated += templateWriter.createGradleFile(
            moduleFile = moduleFile,
            moduleType = moduleType,
            packageName = packageName,
            dependencies = dependencies,
        )

        if (moduleType == Constants.ANDROID) {
            filesCreated += createAndroidManifest(moduleFile, moduleName)
            filesCreated += createResourceDirectories(moduleFile)
        }

        if (addReadme) filesCreated += templateWriter.createReadmeFile(moduleFile, moduleName)

        filesCreated += createDefaultPackages(moduleFile, packageName)

        if (addGitIgnore) filesCreated += createGitIgnore(moduleFile)

        return filesCreated
    }

    private fun createAndroidManifest(moduleFile: File, moduleName: String): List<File> {
        val manifestDir = Paths.get(moduleFile.absolutePath, "src", "main").toFile()
        manifestDir.mkdirs()

        val manifestFile = Paths.get(manifestDir.absolutePath, "AndroidManifest.xml").toFile()
        val writer: Writer = FileWriter(manifestFile)
        val manifestContent = ManifestTemplate.getManifestTemplate(moduleName)

        writer.write(manifestContent)
        writer.flush()
        writer.close()

        return listOf(manifestFile)
    }

    private fun createResourceDirectories(moduleFile: File): List<File> {
        val createdDirs = mutableListOf<File>()

        val resDir = Paths.get(moduleFile.absolutePath, "src", "main", "res").toFile()
        resDir.mkdirs()
        createdDirs.add(resDir)

        val subDirs = listOf(
            "drawable",
            "values",
        )

        subDirs.forEach { dirName ->
            val dir = Paths.get(resDir.absolutePath, dirName).toFile()
            dir.mkdirs()
            createdDirs.add(dir)
        }

        return createdDirs
    }

    private fun createGitIgnore(moduleFile: File): List<File> {
        val gitignoreFile = Paths.get(moduleFile.absolutePath).toFile()
        val filePath = Paths.get(gitignoreFile.absolutePath, ".gitignore").toFile()
        val writer: Writer = FileWriter(filePath)
        val dataToWrite = GitIgnoreTemplate.data

        writer.write(dataToWrite)
        writer.flush()
        writer.close()

        return listOf(filePath)
    }

    fun createFeatureFiles(
        moduleFile: File,
        moduleName: String,
        packageName: String,
        showErrorDialog: (String) -> Unit,
        showSuccessDialog: () -> Unit,
    ): List<File> {
        val featureFile = Paths.get(moduleFile.absolutePath, moduleName.lowercase()).toFile()

        val commonDir = Paths.get(featureFile.absolutePath, "common").toFile()
        val dataDir = Paths.get(featureFile.absolutePath, "data").toFile()
        val diDir = Paths.get(featureFile.absolutePath, "di").toFile()
        val domainDir = Paths.get(featureFile.absolutePath, "domain").toFile()
        val presentationDir = Paths.get(featureFile.absolutePath, "presentation").toFile()

        listOf(commonDir, dataDir, diDir, domainDir, presentationDir).forEach { it.mkdirs() }

        val capitalizedModuleName = moduleName.replaceFirstChar { it.uppercase() }

        val filePaths = listOf(
            Paths.get(presentationDir.absolutePath, "${capitalizedModuleName}Screen.kt").toFile(),
            Paths.get(presentationDir.absolutePath, "${capitalizedModuleName}ViewModel.kt").toFile(),
            Paths.get(presentationDir.absolutePath, "${capitalizedModuleName}ComponentKey.kt").toFile(),
            Paths.get(presentationDir.absolutePath, "${capitalizedModuleName}Contract.kt").toFile(),
            Paths.get(presentationDir.absolutePath, "${capitalizedModuleName}PreviewProvider.kt").toFile(),
        )

        val successfullyCreatedFiles = mutableListOf<File>()

        filePaths.forEach { file ->
            try {
                val writer: Writer = FileWriter(file)
                val extendedPackageName = if (packageName.endsWith(".$moduleName")) {
                    "$packageName.presentation"
                } else {
                    "$packageName.$moduleName.presentation"
                }
                val dataToWrite = when (file.name) {
                    "${capitalizedModuleName}Screen.kt" -> {
                        FeatureTemplate.getScreen(extendedPackageName, capitalizedModuleName)
                    }

                    "${capitalizedModuleName}ViewModel.kt" -> {
                        FeatureTemplate.getViewModel(extendedPackageName, capitalizedModuleName)
                    }

                    "${capitalizedModuleName}ComponentKey.kt" -> {
                        FeatureTemplate.getComponentKey(extendedPackageName, capitalizedModuleName)
                    }

                    "${capitalizedModuleName}Contract.kt" -> {
                        FeatureTemplate.getContract(extendedPackageName, capitalizedModuleName)
                    }

                    "${capitalizedModuleName}PreviewProvider.kt" -> {
                        FeatureTemplate.getPreviewProvider(extendedPackageName, capitalizedModuleName)
                    }

                    else -> Constants.EMPTY
                }

                if (dataToWrite.isNotEmpty()) {
                    writer.write(dataToWrite)
                    writer.flush()
                    writer.close()
                    successfullyCreatedFiles.add(file)
                } else {
                    showErrorDialog("No data to write for ${file.name}")
                }
            } catch (e: IOException) {
                showErrorDialog("Error creating file ${file.name}: ${e.message}")
            } catch (e: Exception) {
                showErrorDialog("Unexpected error: ${e.message}")
            }
        }
        showSuccessDialog()
        return successfullyCreatedFiles
    }

    private fun createDefaultPackages(
        moduleFile: File,
        packageName: String,
    ): List<File> {
        fun makePath(srcPath: File): File {
            val packagePath = Paths.get(
                srcPath.path, packageName.split(".").joinToString(File.separator)
            ).toFile()
            val stringBuilder = StringBuilder()
            val filePath = Paths.get(srcPath.absolutePath, stringBuilder.toString()).toFile()
            packagePath.mkdirs()
            filePath.mkdirs()
            return packagePath
        }

        val srcPath = Paths.get(moduleFile.absolutePath, "src/main/kotlin").toFile()
        val packagePath = makePath(srcPath)
        return listOf(packagePath)
    }

    private fun addToSettingsAtCorrectLocation(
        settingsGradleFile: File,
        modulePathAsString: String,
    ) {
        val settingsFileContent = Files.readAllLines(Paths.get(settingsGradleFile.toURI()))

        val modulePath = modulePathAsString.removePrefix(":")
        val moduleCategory = determineModuleCategory(modulePath)

        val includeBlocks = findIncludeBlocksWithCategories(settingsFileContent)

        val updatedContent = insertModuleInCategory(
            settingsFileContent = settingsFileContent.toMutableList(),
            modulePathAsString = modulePathAsString,
            moduleCategory = moduleCategory,
            categoryBlocks = includeBlocks,
        )

        Files.write(Paths.get(settingsGradleFile.toURI()), updatedContent)
    }

    private fun determineModuleCategory(modulePath: String): String {
        return when {
            modulePath.startsWith("library:") -> "Libraries"
            modulePath.startsWith("plugin:") -> "Plugins"
            modulePath.startsWith("feature:") -> "Features"
            modulePath.startsWith("launcher:") -> "Launchers"
            else -> "Root"
        }
    }

    private fun findIncludeBlocksWithCategories(settingsFileContent: List<String>): Map<String, Pair<Int, Int>> {
        val categoryBlocks = mutableMapOf<String, Pair<Int, Int>>()
        var currentCategory = "Root"
        var categoryStart = -1
        var inIncludeBlock = false

        settingsFileContent.forEachIndexed { index, line ->
            if (line.trim().startsWith("//") && !inIncludeBlock) {
                val potentialCategory = line.trim().removePrefix("//").trim()
                if (potentialCategory in listOf("Libraries", "Plugins", "Features", "Launchers")) {
                    currentCategory = potentialCategory
                    categoryStart = index
                }
            }

            if (line.contains("include ") || line.contains("include(")) {
                if (!inIncludeBlock) {
                    inIncludeBlock = true
                    if (!categoryBlocks.containsKey(currentCategory)) {
                        categoryBlocks[currentCategory] = Pair(index, index)
                    }
                }

                if (!line.endsWith(",")) {
                    inIncludeBlock = false
                    categoryBlocks[currentCategory] = Pair(
                        categoryBlocks[currentCategory]?.first ?: index,
                        index
                    )
                }
            } else if (inIncludeBlock) {
                if (!line.trim().endsWith(",")) {
                    inIncludeBlock = false
                    categoryBlocks[currentCategory] = Pair(
                        categoryBlocks[currentCategory]?.first ?: categoryStart,
                        index
                    )
                }
            }
        }

        return categoryBlocks
    }

    private fun insertModuleInCategory(
        settingsFileContent: MutableList<String>,
        modulePathAsString: String,
        moduleCategory: String,
        categoryBlocks: Map<String, Pair<Int, Int>>,
    ): MutableList<String> {
        if (categoryBlocks.containsKey(moduleCategory)) {
            val (blockStart, blockEnd) = categoryBlocks[moduleCategory]!!

            val insertPosition = blockEnd
            val lastLine = settingsFileContent[insertPosition]

            val baseIndentation = lastLine.takeWhile { it.isWhitespace() }

            var continuationIndentation = ""
            if (insertPosition > blockStart) {
                for (i in blockStart + 1..insertPosition) {
                    val line = settingsFileContent[i].trim()
                    if (line.startsWith("':") && !line.startsWith("include")) {
                        // Continuation satırı bulduk, girintiyi al
                        continuationIndentation = settingsFileContent[i].takeWhile { it.isWhitespace() }
                        break
                    }
                }

                if (continuationIndentation.isEmpty()) {
                    continuationIndentation = baseIndentation + " ".repeat(8)
                }
            } else {
                continuationIndentation = baseIndentation + " ".repeat(8)
            }

            val trimmedLastLine = lastLine.trim()

            if (trimmedLastLine.endsWith(",")) {
                settingsFileContent.add(insertPosition + 1, "$continuationIndentation'$modulePathAsString'")
            } else if (trimmedLastLine.contains("include") &&
                (trimmedLastLine.endsWith("'") || trimmedLastLine.endsWith("\""))
            ) {
                settingsFileContent[insertPosition] = "${lastLine},"
                settingsFileContent.add(insertPosition + 1, "$continuationIndentation'$modulePathAsString'")
            } else if (trimmedLastLine.endsWith("'") || trimmedLastLine.endsWith("\"")) {
                settingsFileContent[insertPosition] = "${lastLine},"
                settingsFileContent.add(insertPosition + 1, "$continuationIndentation'$modulePathAsString'")
            } else {
                val includeStatement = constructIncludeStatement(modulePathAsString)
                settingsFileContent.add(insertPosition + 1, "$baseIndentation$includeStatement")
            }

            return settingsFileContent
        } else {
            settingsFileContent.add("")
            settingsFileContent.add("// $moduleCategory")

            val includeStatement = constructIncludeStatement(modulePathAsString)
            settingsFileContent.add(includeStatement)

            return settingsFileContent
        }
    }

    private fun constructIncludeStatement(modulePathAsString: String): String {
        return "include '$modulePathAsString'"
    }
}