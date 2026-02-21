package com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.common.Utils.analyzeSelectedDirectory
import com.github.teknasyon.plugin.common.getCurrentlySelectedFile
import com.github.teknasyon.plugin.components.TPCheckbox
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.project.Project

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectedModulesContent(
    project: Project,
    isAnalyzingState: Boolean,
    analysisResultState: String?,
    selectedSrc: String,
    onAnalysisResultChange: (String?) -> Unit,
    onAnalyzingChange: (Boolean) -> Unit,
    onDetectedModulesLoaded: (List<String>) -> Unit,
    onSelectedModulesLoaded: (List<String>) -> Unit,
    detectedModules: List<String>,
    existingModules: List<String>,
    selectedModules: List<String>,
    onCheckedModule: (String) -> Unit,
) {
    if (existingModules.isNotEmpty()) {
        Column(
            modifier = Modifier
                .background(
                    color = TPTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TPText(
                    text = "Detected Modules in Selected Directory",
                    color = TPTheme.colors.white,
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
                            color = TPTheme.colors.blue,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    val selectedFile = project.getCurrentlySelectedFile(selectedSrc)
                                    if (selectedFile.exists()) {
                                        analyzeSelectedDirectory(
                                            directory = selectedFile,
                                            project = project,
                                            onAnalysisResultChange = onAnalysisResultChange,
                                            onAnalyzingChange = onAnalyzingChange,
                                            onDetectedModulesLoaded = onDetectedModulesLoaded,
                                            onSelectedModulesLoaded = onSelectedModulesLoaded,
                                            detectedModules = detectedModules,
                                        )
                                    }
                                },
                            imageVector = Icons.Rounded.Refresh,
                            tint = TPTheme.colors.blue,
                            contentDescription = null,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            TPText(
                text = "Select modules that your new module will depend on:",
                color = TPTheme.colors.lightGray,
                style = TextStyle(fontSize = 13.sp),
            )
            Divider(
                color = TPTheme.colors.lightGray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            analysisResultState?.let { result ->
                TPText(
                    text = result,
                    style = TextStyle(fontSize = 13.sp),
                    color = TPTheme.colors.blue,
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                existingModules.forEach { module ->
                    val isChecked = module in selectedModules
                    TPCheckbox(
                        checked = isChecked,
                        label = module,
                        isBackgroundEnable = true,
                        color = TPTheme.colors.blue,
                        onCheckedChange = { onCheckedModule(module) },
                    )
                }
            }
        }
    }
}