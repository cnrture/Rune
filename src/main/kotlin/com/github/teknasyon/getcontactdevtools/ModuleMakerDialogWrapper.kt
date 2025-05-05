package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.common.getCurrentlySelectedFile
import com.github.teknasyon.getcontactdevtools.common.rootDirectoryString
import com.github.teknasyon.getcontactdevtools.common.rootDirectoryStringDropLast
import com.github.teknasyon.getcontactdevtools.components.*
import com.github.teknasyon.getcontactdevtools.file.FileTree
import com.github.teknasyon.getcontactdevtools.file.FileWriter
import com.github.teknasyon.getcontactdevtools.file.toProjectFile
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

class ModuleMakerDialogWrapper(
    private val project: Project,
    private val startingLocation: VirtualFile?,
) : GetcontactDialogWrapper(
    width = Constants.MODULE_MAKER_WINDOW_WIDTH,
    height = Constants.MODULE_MAKER_WINDOW_HEIGHT,
) {

    private val fileWriter = FileWriter()

    private var existingModules = listOf<String>()
    private var selectedModules = mutableStateListOf<String>()
    private var detectedModules = mutableStateListOf<String>()

    private val isMoveFiles = mutableStateOf(false)

    private var selectedSrc = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private val moduleType = mutableStateOf(Constants.ANDROID)
    private val packageName = mutableStateOf(Constants.EMPTY)
    private val moduleName = mutableStateOf(Constants.EMPTY)

    private val isAnalyzing = mutableStateOf(false)
    private val analysisResult = mutableStateOf<String?>(null)

    init {
        loadExistingModules()

        selectedSrc.value = if (startingLocation != null) {
            File(startingLocation.path).absolutePath.removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            File(project.rootDirectoryString()).absolutePath.removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        }
    }

    private fun analyzeSelectedDirectory(directory: File) {
        try {
            if (!directory.exists() || !directory.isDirectory) {
                analysisResult.value = "Directory does not exist or is not a directory"
                return
            }

            isAnalyzing.value = true
            analysisResult.value = null

            thread {
                try {
                    val analyzer = ImportAnalyzer()
                    val projectRoot = project.basePath?.let { File(it) }
                    if (projectRoot != null && projectRoot.exists()) {
                        analyzer.discoverProjectModules(projectRoot)
                    }
                    val findModules = analyzer.analyzeSourceDirectory(directory)
                    SwingUtilities.invokeLater {
                        detectedModules.clear()
                        detectedModules.addAll(findModules)
                        selectedModules.clear()
                        selectedModules.addAll(findModules)

                        if (detectedModules.isEmpty()) {
                            analysisResult.value = "No dependencies detected"
                        } else {
                            analysisResult.value = "Detected ${detectedModules.size} dependencies"
                        }

                        isAnalyzing.value = false
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        analysisResult.value = "Error analyzing directory: ${e.message}"
                        isAnalyzing.value = false
                        e.printStackTrace()
                    }
                }
            }

        } catch (e: Exception) {
            analysisResult.value = "Error analyzing directory: ${e.message}"
            isAnalyzing.value = false
            e.printStackTrace()
        }
    }

    private fun loadExistingModules() {
        val settingsFile = getSettingsGradleFile()
        if (settingsFile != null) {
            try {
                val content = settingsFile.readText()

                val patterns = listOf(
                    """include\s*\(\s*["']([^"']+)["']\s*\)""".toRegex(),
                    """include\s+['"]([^"']+)["']""".toRegex(),
                    """include\s+['"]([^"']+)["'](?:\s*,\s*['"]([^"']+)["'])*""".toRegex(),
                    """include\s+['"]([^"']+)["'](?:\s*,\s*\n\s*['"]([^"']+)["'])*""".toRegex()
                )

                val modulesSet = mutableSetOf<String>()

                patterns.forEach { pattern ->
                    val matches = pattern.findAll(content)
                    matches.forEach { matchResult ->
                        matchResult.groupValues.drop(1).forEach { moduleValue ->
                            if (moduleValue.isNotEmpty()) {
                                modulesSet.add(moduleValue)
                            }
                        }
                    }
                }

                val multiLinePattern =
                    """include\s*(?:'[^']*'|"[^"]*")\s*(?:,\s*\n\s*(?:'[^']*'|"[^"]*")\s*)*""".toRegex()
                val multiLineMatches = multiLinePattern.findAll(content)

                multiLineMatches.forEach { match ->
                    val modulePattern = """['"]([^"']+)["']""".toRegex()
                    val moduleMatches = modulePattern.findAll(match.value)
                    moduleMatches.forEach { moduleMatch ->
                        val moduleValue = moduleMatch.groupValues[1]
                        if (moduleValue.isNotEmpty()) {
                            modulesSet.add(moduleValue)
                        }
                    }
                }

                existingModules = modulesSet.toList().sorted()
            } catch (e: Exception) {
                e.printStackTrace()
                existingModules = emptyList()
            }
        }
    }

    @Composable
    override fun createDesign() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GetcontactTheme.colors.gray,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                GetcontactText(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Module Creator",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                GetcontactTheme.colors.blue,
                                GetcontactTheme.colors.purple,
                            ),
                            tileMode = TileMode.Mirror,
                        ),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                )
                Spacer(modifier = Modifier.size(24.dp))
                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    FileTreePanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.3f)
                            .padding(16.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp)
                            .background(GetcontactTheme.colors.white)
                            .width(2.dp)
                    )
                    ConfigurationPanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f),
                    )
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        return packageName.value.isNotEmpty() && moduleName.value.isNotEmpty() && moduleName.value != Constants.DEFAULT_MODULE_NAME
    }

    @Composable
    private fun FileTreePanel(modifier: Modifier = Modifier) {
        GetcontactFileTree(
            modifier = modifier,
            model = FileTree(root = File(project.rootDirectoryString()).toProjectFile()),
            onClick = { fileTreeNode ->
                val absolutePathAtNode = fileTreeNode.file.absolutePath
                val relativePath = absolutePathAtNode.removePrefix(project.rootDirectoryStringDropLast())
                    .removePrefix(File.separator)
                if (fileTreeNode.file.isDirectory) {
                    selectedSrc.value = relativePath
                }
            }
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ConfigurationPanel(modifier: Modifier = Modifier) {
        var selectedSrc by remember { selectedSrc }
        val radioOptions = listOf(Constants.ANDROID, Constants.KOTLIN)
        var moduleType by remember { moduleType }
        var packageName by remember { packageName }
        var moduleNameState by remember { moduleName }
        val selectedModules = remember { selectedModules }
        var isMoveFiles by remember { isMoveFiles }
        val isAnalyzingState by remember { isAnalyzing }
        val analysisResultState by remember { analysisResult }

        Scaffold(
            modifier = modifier,
            backgroundColor = GetcontactTheme.colors.gray,
            bottomBar = {
                GetcontactDialogActions(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GetcontactTheme.colors.gray),
                    onCancelClick = { close(Constants.DEFAULT_EXIT_CODE) },
                    onCreateClick = {
                        if (validateInput()) {
                            createModule()
                        } else {
                            MessageDialogWrapper("Please fill out required values").show()
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                GetcontactText(
                    text = "Selected root: $selectedSrc",
                    color = GetcontactTheme.colors.orange,
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                DetectModulesContent(
                    isAnalyzingState = isAnalyzingState,
                    analysisResultState = analysisResultState,
                )

                Spacer(modifier = Modifier.height(16.dp))

                MoveFilesContent(
                    isChecked = isMoveFiles,
                    onCheckedChange = { isMoveFiles = it },
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModuleTypeNameContent(
                    moduleTypeSelectionState = moduleType,
                    packageName = packageName,
                    moduleNameState = moduleNameState,
                    radioOptions = radioOptions,
                    onPackageNameChanged = { packageName = it },
                    onModuleTypeSelected = { moduleType = it },
                    onModuleNameChanged = { moduleNameState = it },
                )

                ExistingModulesContent(
                    existingModules = existingModules,
                    selectedDependencies = selectedModules,
                    onCheckedModule = { module ->
                        if (selectedModules.contains(module)) {
                            selectedModules.remove(module)
                        } else {
                            selectedModules.add(module)
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun DetectModulesContent(
        isAnalyzingState: Boolean,
        analysisResultState: String?,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = GetcontactTheme.colors.white,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GetcontactText(
                    text = "Detect Modules",
                    color = GetcontactTheme.colors.white,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Box {
                    if (isAnalyzingState) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GetcontactTheme.colors.orange,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    val selectedFile = project.getCurrentlySelectedFile(selectedSrc.value)
                                    if (selectedFile.exists()) {
                                        analyzeSelectedDirectory(selectedFile)
                                    }
                                },
                            imageVector = Icons.Rounded.PlayArrow,
                            tint = GetcontactTheme.colors.orange,
                            contentDescription = null,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "These modules will be added to the new module's build.gradle file.",
                color = GetcontactTheme.colors.lightGray,
            )
            Spacer(modifier = Modifier.size(8.dp))
            analysisResultState?.let { result ->
                Text(
                    text = result,
                    color = GetcontactTheme.colors.orange,
                )
            }
        }
    }

    @Composable
    private fun MoveFilesContent(
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = GetcontactTheme.colors.white,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
        ) {
            GetcontactCheckbox(
                label = "Move selected files to new module",
                checked = isChecked,
                onCheckedChange = { onCheckedChange(it) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                text = "This will move files from the selected directory to the new module.",
                color = GetcontactTheme.colors.lightGray,
            )
        }
    }

    @Composable
    private fun ModuleTypeNameContent(
        moduleTypeSelectionState: String,
        packageName: String,
        moduleNameState: String,
        radioOptions: List<String>,
        onPackageNameChanged: (String) -> Unit,
        onModuleTypeSelected: (String) -> Unit,
        onModuleNameChanged: (String) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = GetcontactTheme.colors.white,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Module Type",
                    color = GetcontactTheme.colors.white,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    radioOptions.forEach { text ->
                        GetcontactRadioButton(
                            text = text,
                            selected = text == moduleTypeSelectionState,
                            isBackgroundEnable = true,
                            onClick = { onModuleTypeSelected(text) },
                        )
                        if (text != radioOptions.last()) {
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
            Column {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Package Name") },
                    placeholder = { Text("Package Name") },
                    value = packageName,
                    onValueChange = { onPackageNameChanged(it) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedLabelColor = GetcontactTheme.colors.white,
                        unfocusedLabelColor = GetcontactTheme.colors.white,
                        cursorColor = GetcontactTheme.colors.white,
                        textColor = GetcontactTheme.colors.white,
                        unfocusedBorderColor = GetcontactTheme.colors.white,
                        focusedBorderColor = GetcontactTheme.colors.white,
                        placeholderColor = GetcontactTheme.colors.white,
                    )
                )
                Spacer(modifier = Modifier.size(16.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Module Name") },
                    placeholder = { Text(Constants.DEFAULT_MODULE_NAME) },
                    value = moduleNameState,
                    onValueChange = { onModuleNameChanged(it) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedLabelColor = GetcontactTheme.colors.white,
                        unfocusedLabelColor = GetcontactTheme.colors.white,
                        cursorColor = GetcontactTheme.colors.white,
                        textColor = GetcontactTheme.colors.white,
                        unfocusedBorderColor = GetcontactTheme.colors.white,
                        focusedBorderColor = GetcontactTheme.colors.white,
                        placeholderColor = GetcontactTheme.colors.white,
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ExistingModulesContent(
        existingModules: List<String>,
        selectedDependencies: List<String>,
        onCheckedModule: (String) -> Unit,
    ) {
        if (existingModules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = GetcontactTheme.colors.white,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Module Dependencies",
                    color = GetcontactTheme.colors.white,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Select modules that your new module will depend on:",
                    color = GetcontactTheme.colors.lightGray,
                    fontSize = 14.sp,
                )
                Divider(
                    color = GetcontactTheme.colors.lightGray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    existingModules.forEachIndexed { index, module ->
                        val isChecked = module in selectedDependencies
                        GetcontactCheckbox(
                            checked = isChecked,
                            label = module,
                            isBackgroundEnable = true,
                            onCheckedChange = { onCheckedModule(module) },
                        )
                    }
                }
            }
        }
    }

    private fun moveFilesToNewModule(sourceDir: File, targetModulePath: String, packageName: String) {
        if (!isMoveFiles.value) return

        try {
            if (!sourceDir.exists() || !sourceDir.isDirectory) {
                MessageDialogWrapper("Source directory does not exist or is not a directory").show()
                return
            }

            val modulePath = File(project.basePath, targetModulePath.replace(":", "/"))
            val targetSrcDir = File(modulePath, "src/main/kotlin")
            targetSrcDir.mkdirs()

            val packagePath = packageName.split(".").joinToString(File.separator)
            val targetPackageDir = File(targetSrcDir, packagePath)
            targetPackageDir.mkdirs()

            val sourceFiles = sourceDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .toList()

            if (sourceFiles.isEmpty()) {
                MessageDialogWrapper("No source files found to move in ${sourceDir.absolutePath}").show()
                return
            }

            val movedFiles = mutableListOf<VirtualFile>()

            sourceFiles.forEach { sourceFile ->
                try {
                    val relativePath = getRelativePath(sourceFile, sourceDir)

                    val targetFile = File(targetPackageDir, relativePath)
                    targetFile.parentFile.mkdirs()

                    sourceFile.copyTo(targetFile, overwrite = true)

                    val relativeDir = targetFile.parentFile.absolutePath
                        .removePrefix(targetPackageDir.absolutePath)
                        .trim(File.separatorChar)

                    val subPackage = if (relativeDir.isNotEmpty()) {
                        "." + relativeDir.replace(File.separator, ".")
                    } else {
                        ""
                    }

                    val fullPackageName = packageName + subPackage

                    val content = targetFile.readText()
                    val packagePattern = """package\s+([a-zA-Z0-9_.]+)""".toRegex()
                    val updatedContent = packagePattern.replace(content, "package $fullPackageName")

                    if (content != updatedContent) {
                        targetFile.writeText(updatedContent)
                    }

                    VfsUtil.findFileByIoFile(targetFile, true)?.let { vFile ->
                        movedFiles.add(vFile)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val projectDir = File(project.basePath.orEmpty())
            VfsUtil.markDirtyAndRefresh(false, true, true, VfsUtil.findFileByIoFile(projectDir, true))

            ApplicationManager.getApplication().invokeLater {
                openNewModule(modulePath, movedFiles)
            }

            MessageDialogWrapper("Moved ${movedFiles.size} files to new module").show()
        } catch (e: Exception) {
            MessageDialogWrapper("Error moving files: ${e.message}").show()
            e.printStackTrace()
        }
    }

    private fun openNewModule(modulePath: File, filesToOpen: List<VirtualFile>) {
        try {
            val moduleRootDir = VfsUtil.findFileByIoFile(modulePath, true)
            if (moduleRootDir != null) {
                val buildGradleFile = moduleRootDir.findChild("build.gradle")
                    ?: moduleRootDir.findChild("build.gradle.kts")

                if (buildGradleFile != null) {
                    FileEditorManager.getInstance(project).openFile(buildGradleFile, true)
                }

                filesToOpen.take(5).forEach { file ->
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSettingsGradleFile(): File? {
        val settingsGradleKtsPath = File(project.basePath, "settings.gradle.kts")
        val settingsGradlePath = File(project.basePath, "settings.gradle")

        return listOf(settingsGradleKtsPath, settingsGradlePath)
            .firstOrNull { it.exists() }
            ?: run {
                MessageDialogWrapper("Can't find settings.gradle(.kts) file")
                null
            }
    }

    private fun createModule(): List<File> {
        try {
            val settingsGradleFile = getSettingsGradleFile()
            val moduleType = moduleType.value

            val selectedSrcPath = selectedSrc.value
            val sourceFile = getSourceDirectoryFromSelected(selectedSrcPath)

            if (settingsGradleFile != null) {
                val moduleName = moduleName.value.trim()
                if (!moduleName.startsWith(":")) {
                    MessageDialogWrapper("Module name must start with ':' (e.g. ':home' or ':feature:home')").show()
                    return emptyList()
                }

                val moduleNameTrimmed = moduleName.removePrefix(":").replace(":", ".")
                val finalPackageName = "${packageName.value}.${moduleNameTrimmed.split(".").last()}"

                val filesCreated = fileWriter.createModule(
                    packageName = finalPackageName,
                    settingsGradleFile = settingsGradleFile,
                    modulePathAsString = moduleName,
                    moduleType = moduleType,
                    showErrorDialog = { MessageDialogWrapper(it).show() },
                    showSuccessDialog = {
                        MessageDialogWrapper("Module '$moduleName' created successfully").show()

                        val projectDir = File(project.basePath.orEmpty())
                        VfsUtil.markDirtyAndRefresh(false, true, true, VfsUtil.findFileByIoFile(projectDir, true))

                        if (isMoveFiles.value) {
                            moveFilesToNewModule(sourceFile, moduleName, finalPackageName)
                        } else {
                            val modulePath = File(project.basePath, moduleName.replace(":", "/"))
                            ApplicationManager.getApplication().invokeLater {
                                openNewModule(modulePath, emptyList())
                            }
                        }

                        syncProject()
                    },
                    workingDirectory = File(project.basePath.orEmpty()),
                    dependencies = selectedModules
                )
                return filesCreated
            } else {
                MessageDialogWrapper("Couldn't find settings.gradle(.kts) file").show()
                return emptyList()
            }
        } catch (e: Exception) {
            MessageDialogWrapper("Error: ${e.message}").show()
            return emptyList()
        } finally {
            close(0)
        }
    }

    private fun syncProject() {
        val projectDir = File(project.basePath.orEmpty())
        VfsUtil.markDirtyAndRefresh(false, true, true, VfsUtil.findFileByIoFile(projectDir, true))
        ExternalSystemUtil.refreshProject(
            project,
            ProjectSystemId("GRADLE"),
            project.rootDirectoryString(),
            false,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC
        )
    }

    private fun getRelativePath(sourceFile: File, sourceDir: File): String {
        val sourceFilePath = sourceFile.absolutePath
        val sourceDirPath = sourceDir.absolutePath

        if (sourceFilePath.startsWith(sourceDirPath)) {
            val relPath = sourceFilePath.substring(sourceDirPath.length)
            return if (relPath.startsWith(File.separator)) relPath.substring(1) else relPath
        }

        return sourceFile.name
    }

    private fun getSourceDirectoryFromSelected(selectedPath: String): File {
        if (selectedPath.isBlank()) {
            val projectRoot = File(project.basePath.orEmpty())
            return projectRoot
        }

        val projectBasePath = project.basePath.orEmpty()

        val pathOptions = mutableListOf<File>()

        pathOptions.add(File(projectBasePath, selectedPath))
        pathOptions.add(File(selectedPath))

        if (startingLocation != null) {
            pathOptions.add(File(startingLocation.path))
        }

        val pathParts = selectedPath.split(File.separator)
        if (pathParts.size > 1) {
            val reducedPath = pathParts.drop(1).joinToString(File.separator)
            pathOptions.add(File(projectBasePath, reducedPath))
        }

        for (option in pathOptions) {
            if (option.exists() && option.isDirectory) {
                return option
            }
        }
        return pathOptions.first()
    }
}