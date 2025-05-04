package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.components.*
import com.github.teknasyon.getcontactdevtools.file.FileTree
import com.github.teknasyon.getcontactdevtools.file.FileWriter
import com.github.teknasyon.getcontactdevtools.file.toProjectFile
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

class ModuleMakerDialogWrapper(
    private val project: Project,
    private val startingLocation: VirtualFile?,
) : GetcontactDialogWrapper("Create New Module") {

    private val fileWriter = FileWriter()

    private var existingModules = listOf<String>()
    private var selectedDependencies = mutableStateListOf<String>()
    private var detectedDependencies = mutableStateListOf<String>()

    private val shouldMoveFiles = mutableStateOf(false)

    private var selectedSrcValue = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private val moduleTypeSelection = mutableStateOf(Constants.ANDROID)
    private val moduleName = mutableStateOf("")

    private val isAnalyzing = mutableStateOf(false)
    private val analysisResult = mutableStateOf<String?>(null)

    init {
        loadExistingModules()

        selectedSrcValue.value = if (startingLocation != null) {
            File(startingLocation.path).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            File(rootDirectoryString()).absolutePath.removePrefix(rootDirectoryStringDropLast())
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
                    val detectedModules = analyzer.analyzeSourceDirectory(directory)
                    SwingUtilities.invokeLater {
                        detectedDependencies.clear()
                        detectedDependencies.addAll(detectedModules)
                        selectedDependencies.clear()
                        selectedDependencies.addAll(detectedModules)

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
                println("Error loading modules: ${e.message}")
                existingModules = emptyList()
            }
        }
    }

    @Composable
    override fun createDesign() {
        Surface(
            modifier = Modifier
                .width(Constants.WINDOW_WIDTH.dp)
                .height(Constants.WINDOW_HEIGHT.dp),
            color = GetcontactTheme.colors.black,
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
            ) {
                FileTreePanel(modifier = Modifier.weight(0.4f))
                ConfigurationPanel(modifier = Modifier.weight(0.6f))
            }
        }
    }

    private fun validateInput(): Boolean {
        return moduleName.value.isNotEmpty() && moduleName.value != Constants.DEFAULT_MODULE_NAME
    }

    @Composable
    private fun FileTreePanel(modifier: Modifier = Modifier) {
        GetcontactFileTree(
            modifier = modifier,
            model = FileTree(root = File(rootDirectoryString()).toProjectFile()),
            onClick = { fileTreeNode ->
                val absolutePathAtNode = fileTreeNode.file.absolutePath
                val relativePath = absolutePathAtNode.removePrefix(rootDirectoryStringDropLast())
                    .removePrefix(File.separator)
                if (fileTreeNode.file.isDirectory) selectedSrcValue.value = relativePath
            }
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ConfigurationPanel(modifier: Modifier = Modifier) {
        var selectedRootState by remember { selectedSrcValue }
        val radioOptions = listOf(Constants.ANDROID, Constants.KOTLIN)
        var moduleTypeSelectionState by remember { moduleTypeSelection }
        var moduleNameState by remember { moduleName }
        val selectedDependenciesState = remember { selectedDependencies }
        var shouldMoveFilesState by remember { shouldMoveFiles }
        val isAnalyzingState by remember { isAnalyzing }
        val analysisResultState by remember { analysisResult }

        Column(
            modifier = modifier,
        ) {
            Text(
                text = "Selected root: $selectedRootState",
                color = GetcontactTheme.colors.orange,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            DetectModulesContent(
                isAnalyzingState = isAnalyzingState,
                analysisResultState = analysisResultState,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MoveFilesContent(
                isChecked = shouldMoveFilesState,
                onCheckedChange = { shouldMoveFilesState = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModuleTypeNameContent(
                moduleTypeSelectionState = moduleTypeSelectionState,
                moduleNameState = moduleNameState,
                radioOptions = radioOptions,
                onModuleTypeSelected = { moduleTypeSelectionState = it },
                onModuleNameChanged = { moduleNameState = it },
            )

            ExistingModulesContent(
                existingModules = existingModules,
                selectedDependencies = selectedDependenciesState,
                onCheckedModule = { module ->
                    if (selectedDependenciesState.contains(module)) {
                        selectedDependenciesState.remove(module)
                    } else {
                        selectedDependenciesState.add(module)
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            GetcontactDialogActions(
                onCancelClick = { close(2) },
                onCreateClick = {
                    if (validateInput()) {
                        create()
                    } else {
                        MessageDialogWrapper("Please fill out required values").show()
                    }
                }
            )
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
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Detect Modules",
                        color = GetcontactTheme.colors.white,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
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
                                        val selectedFile = getCurrentlySelectedFile()
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
            Divider(
                color = GetcontactTheme.colors.lightGray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
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
        moduleNameState: String,
        radioOptions: List<String>,
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Module Type",
                    color = GetcontactTheme.colors.white,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Row {
                    radioOptions.forEach { text ->
                        GetcontactRadioButton(
                            text = text,
                            selected = text == moduleTypeSelectionState,
                            onClick = { onModuleTypeSelected(text) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    existingModules.forEachIndexed { index, module ->
                        val isChecked = module in selectedDependencies
                        GetcontactCheckbox(
                            checked = isChecked,
                            label = module,
                            onCheckedChange = { onCheckedModule(module) },
                        )
                    }
                }
            }
        }
    }

    private fun moveFilesToNewModule(sourceDir: File, targetModulePath: String) {
        if (!shouldMoveFiles.value) return

        try {
            val modulePath = File(project.basePath, targetModulePath.replace(":", "/"))
            val targetSrcDir = File(modulePath, "src/main/kotlin")

            val sourceFiles = sourceDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .toList()

            sourceFiles.forEach { sourceFile ->
                val relativePath = sourceFile.toRelativeString(sourceDir)
                val targetFile = File(targetSrcDir, relativePath)

                targetFile.parentFile.mkdirs()

                sourceFile.copyTo(targetFile, overwrite = true)

                // sourceFile.delete()
            }

            VfsUtil.markDirtyAndRefresh(false, true, true, VfsUtil.findFileByIoFile(modulePath, true))

        } catch (e: Exception) {
            MessageDialogWrapper("Error moving files: ${e.message}").show()
        }
    }

    private fun getSettingsGradleFile(): File? {
        val settingsGradleKtsCurrentlySelectedRoot =
            Path.of(getCurrentlySelectedFile().absolutePath, "settings.gradle.kts").toFile()
        val settingsGradleCurrentlySelectedRoot =
            Path.of(getCurrentlySelectedFile().absolutePath, "settings.gradle").toFile()
        val settingsGradleKtsPath = Path.of(rootDirectoryString(), "settings.gradle.kts").toFile()
        val settingsGradlePath = Path.of(rootDirectoryString(), "settings.gradle").toFile()

        return listOf(
            settingsGradleKtsCurrentlySelectedRoot,
            settingsGradleCurrentlySelectedRoot,
            settingsGradleKtsPath,
            settingsGradlePath
        ).firstOrNull {
            it.exists()
        } ?: run {
            MessageDialogWrapper("Can't find settings.gradle(.kts) file")
            null
        }
    }

    private fun create(): List<File> {
        try {
            val settingsGradleFile = getSettingsGradleFile()
            val moduleType = moduleTypeSelection.value
            val currentlySelectedFile = getCurrentlySelectedFile()
            if (settingsGradleFile != null) {
                val filesCreated = fileWriter.createModule(
                    settingsGradleFile = settingsGradleFile,
                    modulePathAsString = moduleName.value,
                    moduleType = moduleType,
                    showErrorDialog = { MessageDialogWrapper(it).show() },
                    showSuccessDialog = {
                        MessageDialogWrapper("Success").show()
                        refreshFileSystem(settingsGradleFile, currentlySelectedFile)
                        syncProject()
                    },
                    workingDirectory = currentlySelectedFile,
                    dependencies = selectedDependencies
                )
                if (filesCreated.isNotEmpty() && shouldMoveFiles.value && startingLocation != null) {
                    moveFilesToNewModule(File(startingLocation.path), moduleName.value)
                }
                return filesCreated
            } else {
                MessageDialogWrapper("Couldn't find settings.gradle(.kts)").show()
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
        ExternalSystemUtil.refreshProject(
            project,
            ProjectSystemId("GRADLE"),
            rootDirectoryString(),
            false,
            ProgressExecutionMode.START_IN_FOREGROUND_ASYNC
        )
    }

    private fun refreshFileSystem(settingsGradleFile: File, currentlySelectedFile: File) {
        VfsUtil.markDirtyAndRefresh(
            false,
            true,
            true,
            settingsGradleFile,
            currentlySelectedFile
        )
    }

    private fun getCurrentlySelectedFile(): File {
        return File(rootDirectoryStringDropLast() + File.separator + selectedSrcValue.value)
    }

    private fun rootDirectoryStringDropLast(): String {
        return project.basePath!!.split(File.separator).dropLast(1).joinToString(File.separator)
    }

    private fun rootDirectoryString(): String = project.basePath!!
}