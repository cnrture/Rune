package com.github.teknasyon.getcontactplugin.file

import com.github.teknasyon.getcontactplugin.template.GitIgnoreTemplate
import com.github.teknasyon.getcontactplugin.template.TemplateWriter
import java.io.File
import java.io.FileWriter
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
        gradleFileFollowModule: Boolean,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean,
        rootPathString: String,
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
            rootPathAsString = rootPathString,
            modulePathAsString = modulePathAsString,
            settingsGradleFile = settingsGradleFile,
            showErrorDialog = showErrorDialog
        )

        filesCreated += createDefaultModuleStructure(
            moduleFile = moduleFile,
            moduleName = moduleName,
            moduleType = moduleType,
            gradleFileFollowModule = gradleFileFollowModule,
            packageName = packageName,
            addReadme = addReadme,
            addGitIgnore = addGitIgnore,
        )

        showSuccessDialog()

        return filesCreated
    }

    private fun createDefaultModuleStructure(
        moduleFile: File,
        moduleName: String,
        moduleType: String,
        gradleFileFollowModule: Boolean,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean,
    ): List<File> {
        val filesCreated = mutableListOf<File>()

        filesCreated += templateWriter.createGradleFile(
            moduleFile = moduleFile,
            moduleName = moduleName,
            moduleType = moduleType,
            gradleFileFollowModule = gradleFileFollowModule,
            packageName = packageName,
        )

        if (addReadme) filesCreated += templateWriter.createReadmeFile(moduleFile, moduleName)

        filesCreated += createDefaultPackages(moduleFile, packageName)

        if (addGitIgnore) filesCreated += createGitIgnore(moduleFile)

        return filesCreated
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

    private fun createDefaultPackages(
        moduleFile: File,
        packageName: String,
    ): List<File> {
        fun makePath(srcPath: File): File {
            val packagePath = Paths.get(srcPath.path, packageName.split(".").joinToString(File.separator)).toFile()
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
        showErrorDialog: (String) -> Unit,
        rootPathAsString: String,
    ) {
        val settingsFile = Files.readAllLines(Paths.get(settingsGradleFile.toURI()))

        val includeKeywords = listOf("includeProject", "includeBuild", "include")

        val twoParametersPattern = """\(".+", ".+"\)""".toRegex()

        val lastNonEmptyLineInSettingsGradleFile = settingsFile.last { settingsFileLine ->
            settingsFileLine.isNotEmpty() && includeKeywords.any {
                settingsFileLine.contains(it)
            }
        }
        val projectIncludeKeyword = includeKeywords.firstOrNull { includeKeyword ->
            lastNonEmptyLineInSettingsGradleFile.contains(includeKeyword)
        }

        if (projectIncludeKeyword == null) {
            showErrorDialog("Could not find any include statements in settings.gradle(.kts) file")
            return
        }

        val usesTwoParameters = settingsFile.any { line ->
            twoParametersPattern.containsMatchIn(line)
        }

        val lastLineNumberOfFirstIncludeProjectStatement = settingsFile.indexOfLast {
            settingsFileContainsSpecialIncludeKeyword(it, projectIncludeKeyword)
        }

        var tempIndexForSettingsFile = lastLineNumberOfFirstIncludeProjectStatement
        while (tempIndexForSettingsFile >= 0) {
            val currentLine = settingsFile[tempIndexForSettingsFile]
            if (currentLine.trim().isEmpty() || settingsFileContainsSpecialIncludeKeyword(
                    stringToCheck = currentLine,
                    projectIncludeKeyword = projectIncludeKeyword,
                )
            ) {
                tempIndexForSettingsFile--
            } else {
                break
            }
        }

        val firstLineNumberOfFirstIncludeProjectStatement = tempIndexForSettingsFile + 1

        if (firstLineNumberOfFirstIncludeProjectStatement <= 0) {
            showErrorDialog("Could not find any include statements in settings.gradle(.kts) file")
            return
        }

        val includeProjectStatements = settingsFile.subList(
            firstLineNumberOfFirstIncludeProjectStatement,
            lastLineNumberOfFirstIncludeProjectStatement + 1
        ).filter { it.isNotEmpty() }.toMutableList()

        val textToWrite = constructTextToWrite(
            usesTwoParameters = usesTwoParameters,
            projectIncludeKeyword = projectIncludeKeyword,
            modulePathAsString = modulePathAsString,
            rootPathAsString = rootPathAsString
        )

        val insertionIndex = includeProjectStatements.indexOfFirst {
            it.isNotEmpty() && it.lowercase() >= textToWrite.lowercase()
        }

        if (insertionIndex < 0) {
            val offsetAmount = if (includeProjectStatements.size == 1 && includeProjectStatements.first()
                    .doesNotContainModule(projectIncludeKeyword)
            ) {
                0
            } else {
                1
            }
            settingsFile.add(lastLineNumberOfFirstIncludeProjectStatement + offsetAmount, textToWrite)
        } else {
            settingsFile.add(insertionIndex + firstLineNumberOfFirstIncludeProjectStatement, textToWrite)
        }

        Files.write(Paths.get(settingsGradleFile.toURI()), settingsFile)
    }

    private fun settingsFileContainsSpecialIncludeKeyword(
        stringToCheck: String,
        projectIncludeKeyword: String,
    ): Boolean {
        return stringToCheck.contains("$projectIncludeKeyword(\"") ||
            stringToCheck.contains("$projectIncludeKeyword('") ||
            stringToCheck.contains("$projectIncludeKeyword(") ||
            stringToCheck.contains("$projectIncludeKeyword \"") ||
            stringToCheck.contains("$projectIncludeKeyword '")
    }

    private fun constructTextToWrite(
        usesTwoParameters: Boolean,
        projectIncludeKeyword: String,
        modulePathAsString: String,
        rootPathAsString: String,
    ): String {
        fun buildText(path: String): String {
            val parametersString = if (usesTwoParameters) {
                val filePath = "$rootPathAsString${path.replace(":", File.separator)}".removePrefix("/")
                "\"$path\", \"$filePath\""
            } else {
                "\"$path\""
            }
            return "$projectIncludeKeyword($parametersString)"
        }

        return buildText(modulePathAsString)
    }
}

private fun String.doesNotContainModule(includeKeyword: String): Boolean {
    return this.replace(" ", "").replace("(", "") == includeKeyword
}