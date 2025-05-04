package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.components.GetcontactDialogActions
import com.github.teknasyon.getcontactdevtools.components.GetcontactFileTree
import com.github.teknasyon.getcontactdevtools.file.FileTree
import com.github.teknasyon.getcontactdevtools.file.FileWriter
import com.github.teknasyon.getcontactdevtools.file.toProjectFile
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import java.io.File
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JRootPane
import javax.swing.border.Border

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
            modifier = modifier,
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

            Spacer(modifier = Modifier.weight(1f))

            GetcontactDialogActions(
                onCancelClick = { close(2) },
                onCreateClick = {
                    if (validateInput()) {
                        createFeature()
                    } else {
                        MessageDialogWrapper("Please fill out required values").show()
                    }
                }
            )
        }
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
            val projectRoot = rootDirectoryString()

            val cleanSelectedPath = selectedSrcValue.value.let { path ->
                val projectName = projectRoot.split(File.separator).last()
                if (path.startsWith(projectName + File.separator)) {
                    path.substring(projectName.length + 1)
                } else {
                    path
                }
            }

            val packagePath = cleanSelectedPath
                .replace(
                    Regex("^.*?(/src/main/java/|/src/main/kotlin/)"),
                    ""
                )
                .replace("/", ".")

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

    override fun createActions(): Array<Action> = emptyArray()

    override fun createSouthPanel(): JComponent {
        val southPanel = super.createSouthPanel()
        southPanel.background = java.awt.Color(30, 30, 30)

        for (component in southPanel.components) {
            component.background = java.awt.Color(30, 30, 30)
            if (component is JComponent) {
                component.isOpaque = true
            }
        }

        return southPanel
    }

    override fun getRootPane(): JRootPane? {
        val rootPane = super.getRootPane()
        rootPane.background = java.awt.Color(30, 30, 30)
        return rootPane
    }

    override fun createContentPaneBorder(): Border {
        return JBUI.Borders.empty()
    }
}