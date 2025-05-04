package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.components.GetcontactFileTree
import com.github.teknasyon.getcontactdevtools.file.FileTree
import com.github.teknasyon.getcontactdevtools.file.FileWriter
import com.github.teknasyon.getcontactdevtools.file.toProjectFile
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class FeatureMakerDialogWrapper(
    private val project: Project,
    startingLocation: VirtualFile?,
) : DialogWrapper(project) {

    private val fileWriter = FileWriter()

    private var selectedSrcValue = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private var featureNameState = mutableStateOf("")

    init {
        title = "Create New Feature"
        init()

        selectedSrcValue.value = if (startingLocation != null) {
            File(startingLocation.path).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            File(rootDirectoryString()).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        }
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
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
        val selectedRootState = remember { selectedSrcValue }
        val featureNameState = remember { featureNameState }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            Text(
                text = "Selected root: ${selectedRootState.value}",
                color = GetcontactTheme.colors.white,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Feature Name") },
                value = featureNameState.value,
                onValueChange = { featureNameState.value = it },
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

    override fun createActions(): Array<Action> {
        return arrayOf(
            DialogWrapperExitAction("Cancel", 2),
            object : AbstractAction("Create") {
                override fun actionPerformed(e: ActionEvent?) {
                    if (validateInput()) {
                        createFeature()
                    } else {
                        Messages.showErrorDialog(project, "Please fill out required values", "Feature Creation Error")
                    }
                }
            }
        )
    }

    private fun validateInput(): Boolean {
        return featureNameState.value.isNotEmpty() && selectedSrcValue.value != Constants.DEFAULT_SRC_VALUE
    }

    private fun rootDirectoryString(): String = project.basePath!!

    private fun rootDirectoryStringDropLast(): String {
        return project.basePath!!.split(File.separator).dropLast(1).joinToString(File.separator)
    }

    private fun createFeature() {
        try {
            // Get the project root path
            val projectRoot = rootDirectoryString()

            // Remove any duplicated project name from the selected path
            val cleanSelectedPath = selectedSrcValue.value.let { path ->
                val projectName = projectRoot.split(File.separator).last()
                if (path.startsWith(projectName + File.separator)) {
                    path.substring(projectName.length + 1)
                } else {
                    path
                }
            }

            // Convert file path to Java package format
            val packagePath = cleanSelectedPath
                .replace(
                    Regex("^.*?(/src/main/java/|/src/main/kotlin/)"),
                    ""
                ) // Remove everything before and including src/main/java/ or kotlin/
                .replace("/", ".") // Convert path separators to package dots

            fileWriter.createFeatureFiles(
                moduleFile = File(projectRoot, cleanSelectedPath),
                moduleName = featureNameState.value,
                packageName = packagePath.plus(".${featureNameState.value}"),
                showErrorDialog = { MessageDialogWrapper("Error: $it").show() },
                showSuccessDialog = {
                    MessageDialogWrapper("Success").show()
                    refreshFileSystem(getCurrentlySelectedFile())
                }
            )
        } catch (e: Exception) {
            MessageDialogWrapper("Error: ${e.message}").show()
        } finally {
            close(0)
        }
    }

    private fun getCurrentlySelectedFile(): File {
        return File(rootDirectoryStringDropLast() + File.separator + selectedSrcValue.value)
    }

    private fun refreshFileSystem(currentlySelectedFile: File) {
        VfsUtil.markDirtyAndRefresh(
            false,
            true,
            true,
            currentlySelectedFile
        )
    }
}