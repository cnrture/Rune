package com.github.teknasyon.getcontactdevtools.toolwindow.manager.featuregenerator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Create
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Utils
import com.github.teknasyon.getcontactdevtools.common.file.FileWriter
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.components.GTCTextField
import com.github.teknasyon.getcontactdevtools.data.FeatureTemplate
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigurationPanel(
    modifier: Modifier = Modifier,
    project: Project,
    fileWriter: FileWriter,
    selectedSrc: String,
    featureName: String,
    onFeatureNameChange: (String) -> Unit,
    showFileTreeDialog: Boolean,
    onFileTreeDialogStateChange: () -> Unit,
) {
    val settings = SettingsService.getInstance()
    var selectedTemplate by remember { mutableStateOf(settings.getDefaultFeatureTemplate()) }
    val availableTemplates = remember { settings.getFeatureTemplates() }
    Scaffold(
        modifier = modifier,
        backgroundColor = GTCTheme.colors.black,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                GTCActionCard(
                    title = "Create",
                    icon = Icons.Rounded.Create,
                    actionColor = GTCTheme.colors.blue,
                    type = GTCActionCardType.MEDIUM,
                    onClick = {
                        if (Utils.validateFeatureInput(featureName, selectedSrc)) {
                            selectedTemplate?.let { selectedTemplate ->
                                Utils.createFeature(
                                    project = project,
                                    selectedSrc = selectedSrc,
                                    featureName = featureName,
                                    fileWriter = fileWriter,
                                    selectedTemplate = selectedTemplate,
                                )
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
                .padding(padding),
        ) {
            GTCTextField(
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Feature Name",
                color = GTCTheme.colors.blue,
                value = featureName,
                onValueChange = onFeatureNameChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            GTCText(
                text = "Be sure to use camel case for the feature name (e.g. MyFeature)",
                color = GTCTheme.colors.lightGray,
                style = TextStyle(fontWeight = FontWeight.SemiBold),
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

            RootSelectionContent(
                modifier = Modifier.fillMaxWidth(),
                selectedSrc = selectedSrc,
                showFileTreeDialog = showFileTreeDialog,
                onChooseRootClick = { onFileTreeDialogStateChange() }
            )
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
                shape = RoundedCornerShape(8.dp)
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
        shape = RoundedCornerShape(8.dp),
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
                            shape = RoundedCornerShape(4.dp),
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