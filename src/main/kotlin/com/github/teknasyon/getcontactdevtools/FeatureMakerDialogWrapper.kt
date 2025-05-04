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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.*
import com.github.teknasyon.getcontactdevtools.components.GetcontactDialogActions
import com.github.teknasyon.getcontactdevtools.components.GetcontactDialogWrapper
import com.github.teknasyon.getcontactdevtools.components.GetcontactFileTree
import com.github.teknasyon.getcontactdevtools.file.FileTree
import com.github.teknasyon.getcontactdevtools.file.FileWriter
import com.github.teknasyon.getcontactdevtools.file.toProjectFile
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class FeatureMakerDialogWrapper(
    private val project: Project,
    startingLocation: VirtualFile?,
) : GetcontactDialogWrapper("Create New Feature") {

    private val fileWriter = FileWriter()

    private var selectedSrc = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private var featureName = mutableStateOf("")

    init {
        title = "Create New Feature"
        init()

        selectedSrc.value = if (startingLocation != null) {
            File(startingLocation.path).absolutePath.removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            File(project.rootDirectoryString()).absolutePath.removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        }
    }

    @Composable
    override fun createDesign() {
        Surface(
            modifier = Modifier
                .width(Constants.FEATURE_MAKER_WINDOW_WIDTH.dp)
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

    @Composable
    private fun FileTreePanel(modifier: Modifier = Modifier) {
        GetcontactFileTree(
            modifier = modifier,
            model = FileTree(root = File(project.rootDirectoryString()).toProjectFile()),
            onClick = { fileTreeNode ->
                val absolutePathAtNode = fileTreeNode.file.absolutePath
                val relativePath = absolutePathAtNode.removePrefix(project.rootDirectoryStringDropLast())
                    .removePrefix(File.separator)
                if (fileTreeNode.file.isDirectory) selectedSrc.value = relativePath
            }
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ConfigurationPanel(modifier: Modifier = Modifier) {
        val selectedSrc = remember { selectedSrc }
        val featureName = remember { featureName }

        Column(
            modifier = modifier,
        ) {
            Text(
                text = "Selected root: ${selectedSrc.value}",
                color = GetcontactTheme.colors.white,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Feature Name") },
                value = featureName.value,
                onValueChange = { featureName.value = it },
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
                onCancelClick = { close(Constants.DEFAULT_EXIT_CODE) },
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
        return featureName.value.isNotEmpty() && selectedSrc.value != Constants.DEFAULT_SRC_VALUE
    }

    private fun createFeature() {
        try {
            val projectRoot = project.rootDirectoryString()

            val cleanSelectedPath = selectedSrc.value.let { path ->
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
                file = File(projectRoot, cleanSelectedPath),
                featureName = featureName.value,
                packageName = packagePath.plus(".${featureName.value.lowercase()}"),
                showErrorDialog = { MessageDialogWrapper("Error: $it").show() },
                showSuccessDialog = {
                    MessageDialogWrapper("Success").show()
                    val currentlySelectedFile = project.getCurrentlySelectedFile(selectedSrc.value)
                    listOf(currentlySelectedFile).refreshFileSystem()
                }
            )
        } catch (e: Exception) {
            MessageDialogWrapper("Error: ${e.message}").show()
        } finally {
            close(0)
        }
    }
}