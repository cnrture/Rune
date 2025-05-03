package com.github.teknasyon.getcontactplugin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactplugin.common.Constants
import com.github.teknasyon.getcontactplugin.components.GetcontactCheckbox
import com.github.teknasyon.getcontactplugin.components.GetcontactFileTree
import com.github.teknasyon.getcontactplugin.components.GetcontactRadioButton
import com.github.teknasyon.getcontactplugin.file.FileTree
import com.github.teknasyon.getcontactplugin.file.FileWriter
import com.github.teknasyon.getcontactplugin.file.toProjectFile
import com.github.teknasyon.getcontactplugin.theme.GetcontactTheme
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.event.ActionEvent
import java.io.File
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class ModuleMakerDialogWrapper(
    private val project: Project,
    startingLocation: VirtualFile?,
) : DialogWrapper(true) {

    private val fileWriter = FileWriter()

    private val existingModules = mutableStateOf<List<String>>(emptyList())

    private var selectedSrcValue = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private val gradleFileNamedAfterModule = mutableStateOf(false)
    private val addReadme = mutableStateOf(false)
    private val addGitIgnore = mutableStateOf(false)
    private val moduleTypeSelection = mutableStateOf(Constants.ANDROID)
    private val moduleName = mutableStateOf("")
    private val packageName = mutableStateOf(Constants.DEFAULT_BASE_PACKAGE_NAME)

    init {
        title = "Module Maker"
        init()

        loadExistingModules()

        selectedSrcValue.value = if (startingLocation != null) {
            File(startingLocation.path).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            File(rootDirectoryString()).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        }
    }

    private fun loadExistingModules() {
        val settingsFile = getSettingsGradleFile()
        if (settingsFile != null) {
            try {
                val content = settingsFile.readText()
                val modulePattern = """include\s*\(\s*["']([^"']+)["']\s*\)""".toRegex()
                val matches = modulePattern.findAll(content)
                val modules = matches.map { it.groupValues[1] }.toList()
                existingModules.value = modules
            } catch (e: Exception) {
                existingModules.value = emptyList()
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT)
            setContent {
                GetcontactTheme {
                    Surface(
                        color = Color.Transparent,
                    ) {
                        Row {
                            FileTreePanel(
                                modifier = Modifier
                                    .height(Constants.WINDOW_HEIGHT.dp)
                                    .width(Constants.FILE_TREE_WIDTH.dp)
                            )
                            ConfigurationPanel(
                                modifier = Modifier
                                    .height(Constants.WINDOW_HEIGHT.dp)
                                    .width(Constants.CONFIGURATION_PANEL_WIDTH.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            DialogWrapperExitAction("Cancel", 2),
            object : AbstractAction("Create") {
                override fun actionPerformed(e: ActionEvent?) {
                    if (validateInput()) {
                        create()
                    } else {
                        MessageDialogWrapper("Please fill out required values").show()
                    }
                }
            }
        )
    }

    private fun validateInput(): Boolean {
        return packageName.value.isNotEmpty() && selectedSrcValue.value != Constants.DEFAULT_SRC_VALUE &&
            moduleName.value.isNotEmpty() && moduleName.value != Constants.DEFAULT_MODULE_NAME
    }

    @Composable
    private fun FileTreePanel(modifier: Modifier = Modifier) {
        GetcontactFileTree(
            modifier = modifier,
            model = FileTree(root = File(rootDirectoryString()).toProjectFile()),
            height = Constants.WINDOW_HEIGHT.dp,
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
        val selectedRootState = remember { selectedSrcValue }
        val gradleFileNamedAfterModuleState = remember { gradleFileNamedAfterModule }
        val addReadmeState = remember { addReadme }
        val addGitIgnoreState = remember { addGitIgnore }
        val radioOptions = listOf(Constants.ANDROID, Constants.KOTLIN)
        val moduleTypeSelectionState = remember { moduleTypeSelection }
        val packageNameState = remember { packageName }
        val moduleNameState = remember { moduleName }

        val existingModulesState = remember { existingModules }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            Text(
                text = "Selected root: ${selectedRootState.value}",
                color = GetcontactTheme.colors.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GetcontactCheckbox(
                label = "Gradle file named after module",
                checked = gradleFileNamedAfterModuleState.value,
                onCheckedChange = { gradleFileNamedAfterModuleState.value = it }
            )

            GetcontactCheckbox(
                label = "Add README.md",
                checked = addReadmeState.value,
                onCheckedChange = { addReadmeState.value = it }
            )

            GetcontactCheckbox(
                label = "Add .gitignore",
                checked = addGitIgnoreState.value,
                onCheckedChange = { addGitIgnoreState.value = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Module Type",
                color = GetcontactTheme.colors.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            radioOptions.forEach { text ->
                GetcontactRadioButton(
                    text = text,
                    selected = text == moduleTypeSelectionState.value,
                    onClick = { moduleTypeSelectionState.value = text },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Package Name") },
                value = packageNameState.value,
                onValueChange = { packageNameState.value = it },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedLabelColor = GetcontactTheme.colors.onPrimary,
                    unfocusedLabelColor = GetcontactTheme.colors.onPrimary,
                    cursorColor = GetcontactTheme.colors.onPrimary,
                    textColor = GetcontactTheme.colors.onPrimary,
                    unfocusedBorderColor = GetcontactTheme.colors.onPrimary,
                    focusedBorderColor = GetcontactTheme.colors.onPrimary,
                    placeholderColor = GetcontactTheme.colors.onPrimary,
                )
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Module Name") },
                placeholder = { Text(Constants.DEFAULT_MODULE_NAME) },
                value = moduleNameState.value,
                onValueChange = { moduleNameState.value = it },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedLabelColor = GetcontactTheme.colors.onPrimary,
                    unfocusedLabelColor = GetcontactTheme.colors.onPrimary,
                    cursorColor = GetcontactTheme.colors.onPrimary,
                    textColor = GetcontactTheme.colors.onPrimary,
                    unfocusedBorderColor = GetcontactTheme.colors.onPrimary,
                    focusedBorderColor = GetcontactTheme.colors.onPrimary,
                    placeholderColor = GetcontactTheme.colors.onPrimary,
                )
            )

            if (existingModulesState.value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Existing Modules",
                    color = GetcontactTheme.colors.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = GetcontactTheme.colors.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            existingModulesState.value.forEach { module ->
                                Text(
                                    text = module,
                                    color = GetcontactTheme.colors.onPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            moduleNameState.value = module
                                        }
                                )
                                Divider(color = GetcontactTheme.colors.outline)
                            }
                        }
                    }
                }

                Text(
                    "Tip: Click on module name to use it",
                    color = GetcontactTheme.colors.onPrimary.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
        val settingsGradleFile = getSettingsGradleFile()
        val moduleType = moduleTypeSelection.value
        val currentlySelectedFile = getCurrentlySelectedFile()
        if (settingsGradleFile != null) {
            val filesCreated = fileWriter.createModule(
                rootPathString = removeRootFromPath(selectedSrcValue.value),
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
                gradleFileFollowModule = gradleFileNamedAfterModule.value,
                packageName = packageName.value,
                addReadme = addReadme.value,
                addGitIgnore = addGitIgnore.value,
            )

            return filesCreated
        } else {
            MessageDialogWrapper("Couldn't find settings.gradle(.kts)").show()
            return emptyList()
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

    private fun removeRootFromPath(path: String): String {
        return path.split(File.separator).drop(1).joinToString(File.separator)
    }
}