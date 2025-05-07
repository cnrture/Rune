package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.*
import com.github.teknasyon.getcontactdevtools.components.*
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
) : GetcontactDialogWrapper(
    width = Constants.FEATURE_MAKER_WINDOW_WIDTH,
    height = Constants.FEATURE_MAKER_WINDOW_HEIGHT,
) {

    private val fileWriter = FileWriter()

    private var selectedSrc = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private var featureName = mutableStateOf(Constants.EMPTY)

    init {
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
                    text = "Feature Creator",
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
                    ),
                )
                Spacer(modifier = Modifier.size(24.dp))
                Row {
                    FileTreePanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.5f),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 32.dp)
                            .background(GetcontactTheme.colors.white)
                            .width(2.dp)
                    )
                    ConfigurationPanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.5f),
                    )
                }
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
                            createFeature()
                        } else {
                            MessageDialogWrapper("Please fill out required values").show()
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                GetcontactText(
                    text = "Selected root: ${selectedSrc.value}",
                    color = GetcontactTheme.colors.orange,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                GetcontactTextField(
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Enter feature name",
                    value = featureName.value,
                    onValueChange = { featureName.value = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                GetcontactText(
                    text = "Be sure to use camel case for the feature name (e.g. MyFeature)",
                    color = GetcontactTheme.colors.lightGray,
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    softWrap = true,
                )
            }
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
                    Constants.EMPTY,
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