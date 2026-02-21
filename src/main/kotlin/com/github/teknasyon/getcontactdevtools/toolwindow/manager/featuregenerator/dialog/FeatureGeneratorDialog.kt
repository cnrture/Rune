package com.github.teknasyon.getcontactdevtools.toolwindow.manager.featuregenerator.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.common.Utils
import com.github.teknasyon.getcontactdevtools.common.file.FileWriter
import com.github.teknasyon.getcontactdevtools.common.rootDirectoryString
import com.github.teknasyon.getcontactdevtools.common.rootDirectoryStringDropLast
import com.github.teknasyon.getcontactdevtools.components.*
import com.github.teknasyon.getcontactdevtools.data.FeatureTemplate
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class FeatureGeneratorDialog(
    private val project: Project,
    startingLocation: VirtualFile?,
) : GTCDialogWrapper(
    width = 600,
    height = 540,
) {
    private val fileWriter = FileWriter()

    private var selectedSrc = mutableStateOf(Constants.DEFAULT_SRC_VALUE)
    private var featureName = mutableStateOf(Constants.EMPTY)

    init {
        selectedSrc.value = if (startingLocation != null) {
            File(startingLocation.path).absolutePath
                .removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            File(project.rootDirectoryString()).absolutePath
                .removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        }
    }

    @Composable
    override fun createDesign() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GTCTheme.colors.black,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                GTCText(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Feature Generator",
                    style = TextStyle(
                        color = GTCTheme.colors.blue,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.size(24.dp))
                ConfigurationPanel(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.6f),
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ConfigurationPanel(modifier: Modifier = Modifier) {
        val selectedSrc = remember { selectedSrc }
        val featureName = remember { featureName }
        val settings = SettingsService.getInstance()
        var selectedTemplate by remember { mutableStateOf(settings.getDefaultFeatureTemplate()) }
        val availableTemplates = remember { settings.getFeatureTemplates() }

        Scaffold(
            modifier = modifier,
            backgroundColor = GTCTheme.colors.black,
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GTCActionCard(
                        title = "Cancel",
                        icon = Icons.Rounded.Cancel,
                        actionColor = GTCTheme.colors.blue,
                        type = GTCActionCardType.MEDIUM,
                        onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    GTCActionCard(
                        title = "Create",
                        icon = Icons.Rounded.CreateNewFolder,
                        actionColor = GTCTheme.colors.blue,
                        type = GTCActionCardType.MEDIUM,
                        onClick = {
                            if (validateInput()) {
                                selectedTemplate?.let {
                                    createFeature(it)
                                } ?: run {
                                    Utils.showInfo(
                                        message = "Please select a feature template",
                                        type = NotificationType.WARNING,
                                    )
                                }
                            } else {
                                Utils.showInfo(
                                    message = "Please fill out required values",
                                    type = NotificationType.WARNING,
                                )
                            }
                        },
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                GTCText(
                    text = "Selected root: ${selectedSrc.value}",
                    color = GTCTheme.colors.blue,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableTemplates.isNotEmpty()) {
                    TemplateSelectionContent(
                        templates = availableTemplates,
                        selectedTemplate = selectedTemplate,
                        defaultTemplateId = settings.getDefaultFeatureTemplate()?.id.orEmpty(),
                        onTemplateSelected = { template ->
                            selectedTemplate = template ?: settings.getDefaultFeatureTemplate()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .background(
                            color = GTCTheme.colors.gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    GTCTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        placeholder = "Enter feature name",
                        value = featureName.value,
                        onValueChange = { featureName.value = it },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GTCText(
                        text = "Be sure to use camel case for the feature name (e.g. MyFeature)",
                        color = GTCTheme.colors.lightGray,
                        style = TextStyle(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }

    @Composable
    fun TemplateSelectionContent(
        templates: List<FeatureTemplate>,
        selectedTemplate: FeatureTemplate?,
        defaultTemplateId: String,
        onTemplateSelected: (FeatureTemplate?) -> Unit,
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = GTCTheme.colors.gray,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            GTCText(
                text = "Templates",
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            GTCText(
                text = "Choose a template to auto-configure your module",
                color = GTCTheme.colors.lightGray,
                style = TextStyle(fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            templates.forEach { template ->
                TemplateOption(
                    title = template.name,
                    isSelected = selectedTemplate?.id == template.id,
                    onClick = {
                        onTemplateSelected(template)
                    },
                    badge = if (template.id == defaultTemplateId) "Default" else "",
                    badgeColor = if (template.id == defaultTemplateId) GTCTheme.colors.blue else GTCTheme.colors.purple
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun TemplateOption(
        title: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        badge: String,
        badgeColor: Color,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) GTCTheme.colors.blue else Color.Transparent
            ),
            backgroundColor = GTCTheme.colors.gray,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GTCText(
                            text = title,
                            color = GTCTheme.colors.white,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        if (badge.isNotEmpty()) {
                            Card(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                backgroundColor = badgeColor.copy(alpha = 0.2f)
                            ) {
                                GTCText(
                                    text = badge,
                                    color = badgeColor,
                                    style = TextStyle(fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = GTCTheme.colors.blue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        return featureName.value.isNotEmpty() && selectedSrc.value != Constants.DEFAULT_SRC_VALUE
    }

    private fun createFeature(selectedTemplate: FeatureTemplate) {
        try {
            Utils.createFeature(
                project = project,
                selectedSrc = selectedSrc.value,
                featureName = featureName.value,
                fileWriter = fileWriter,
                selectedTemplate = selectedTemplate,
            )
        } catch (e: Exception) {
            Utils.showInfo(
                message = "Failed to create feature: ${e.message}",
                type = NotificationType.ERROR,
            )
        } finally {
            close(0)
        }
    }
}