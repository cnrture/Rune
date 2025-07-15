package com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.common.Utils
import com.github.teknasyon.getcontactdevtools.components.*
import com.github.teknasyon.getcontactdevtools.data.FeatureTemplate
import com.github.teknasyon.getcontactdevtools.data.ModuleTemplate
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.dialog.*
import com.intellij.openapi.project.Project
import java.util.*

@Composable
fun SettingsContent(project: Project) {
    val settings = SettingsService.getInstance()
    var currentSettings by mutableStateOf(settings.state.copy())
    var selectedTab by mutableStateOf("general")
    var selectedModuleType by mutableStateOf(currentSettings.preferredModuleType)
    var packageName by mutableStateOf(currentSettings.defaultPackageName)

    var refreshTrigger by remember { mutableStateOf(0) }
    val moduleTemplates by remember(refreshTrigger) {
        mutableStateOf(settings.getModuleTemplates())
    }
    val featureTemplates by remember(refreshTrigger) {
        mutableStateOf(settings.getFeatureTemplates())
    }

    val triggerRefresh = { refreshTrigger++ }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(GTCTheme.colors.black)
            .padding(24.dp),
        backgroundColor = GTCTheme.colors.black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GTCText(
                modifier = Modifier.fillMaxWidth(),
                text = "Settings",
                style = TextStyle(
                    color = GTCTheme.colors.lightGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            )

            Spacer(modifier = Modifier.size(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GTCTabRow(
                    text = "General",
                    isSelected = selectedTab == "general",
                    color = GTCTheme.colors.lightGray,
                    onTabSelected = { selectedTab = "general" }
                )
                GTCTabRow(
                    text = "Module",
                    isSelected = selectedTab == "templates",
                    color = GTCTheme.colors.lightGray,
                    onTabSelected = { selectedTab = "templates" }
                )
                GTCTabRow(
                    text = "Feature",
                    isSelected = selectedTab == "feature_templates",
                    color = GTCTheme.colors.lightGray,
                    onTabSelected = { selectedTab = "feature_templates" }
                )
            }

            Spacer(modifier = Modifier.size(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    "general" -> {
                        GeneralSettingsTab(
                            defaultPackageName = packageName,
                            preferredModuleType = selectedModuleType,
                            onSaveClick = {
                                currentSettings = currentSettings.copy(
                                    defaultPackageName = packageName,
                                    preferredModuleType = selectedModuleType
                                )
                                settings.loadState(currentSettings)
                                triggerRefresh()
                                Utils.showInfo(
                                    title = "GTC DevTools",
                                    message = "Settings saved successfully!",
                                )
                            },
                            onPackageNameChange = { packageName = it },
                            onModuleTypeChange = { selectedModuleType = it }
                        )
                    }

                    "templates" -> {
                        ModuleTemplatesTab(
                            project = project,
                            templates = moduleTemplates,
                            defaultTemplateId = currentSettings.defaultModuleTemplateId,
                            onTemplateDelete = { template ->
                                if (!template.isDefault) {
                                    settings.removeTemplate(template)
                                    triggerRefresh()
                                    Utils.showInfo(
                                        title = "GTC DevTools",
                                        message = "Template '${template.name}' deleted successfully!",
                                    )
                                }
                            },
                            onRefreshTriggered = { triggerRefresh() },
                            onSetDefault = { template ->
                                settings.setDefaultModuleTemplate(template.id)
                                triggerRefresh()
                                Utils.showInfo(
                                    title = "GTC DevTools",
                                    message = "Default template set to '${template.name}' successfully!",
                                )
                            },
                            onImport = {
                                Utils.importModuleTemplate(project) { template, message ->
                                    if (template != null) {
                                        val updatedTemplate = template.copy(
                                            id = UUID.randomUUID().toString(),
                                            isDefault = false
                                        )
                                        settings.addModuleTemplate(updatedTemplate)
                                        triggerRefresh()
                                    }
                                    Utils.showInfo(
                                        title = "GTC DevTools",
                                        message = message,
                                    )
                                }
                            },
                        )
                    }

                    "feature_templates" -> {
                        FeatureTemplatesTab(
                            project = project,
                            templates = featureTemplates,
                            defaultTemplateId = currentSettings.defaultFeatureTemplateId,
                            onTemplateDelete = { template ->
                                if (!template.isDefault) {
                                    settings.removeFeatureTemplate(template)
                                    triggerRefresh()
                                    Utils.showInfo(
                                        title = "GTC DevTools",
                                        message = "Feature template '${template.name}' deleted successfully!",
                                    )
                                }
                            },
                            onRefreshTriggered = { triggerRefresh() },
                            onSetDefault = { template ->
                                settings.setDefaultFeatureTemplate(template.id)
                                triggerRefresh()
                                Utils.showInfo(
                                    title = "GTC DevTools",
                                    message = "Default template set to '${template.name}' successfully!",
                                )
                            },
                            onImport = {
                                Utils.importFeatureTemplate(project) { template, message ->
                                    if (template != null) {
                                        val updatedTemplate = template.copy(
                                            id = UUID.randomUUID().toString(),
                                            name = "${template.name} (Copy)",
                                            isDefault = false
                                        )
                                        settings.addFeatureTemplate(updatedTemplate)
                                        triggerRefresh()
                                    }
                                    Utils.showInfo(
                                        title = "GTC DevTools",
                                        message = message,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleTemplatesTab(
    project: Project,
    templates: List<ModuleTemplate>,
    defaultTemplateId: String,
    onTemplateDelete: (ModuleTemplate) -> Unit,
    onRefreshTriggered: () -> Unit,
    onSetDefault: (ModuleTemplate) -> Unit,
    onImport: () -> Unit,
) {
    var isReviewDialogVisible by remember { mutableStateOf(Pair(false, ModuleTemplate.EMPTY)) }
    var isCreateDialogVisible by remember { mutableStateOf(Pair(false, ModuleTemplate.EMPTY)) }
    var isEditDialogVisible by remember { mutableStateOf(Pair(false, ModuleTemplate.EMPTY)) }
    val settings = SettingsService.getInstance()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GTCText(
                text = "Module",
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GTCActionCard(
                    title = "Add Template",
                    icon = Icons.Rounded.Add,
                    type = GTCActionCardType.SMALL,
                    actionColor = GTCTheme.colors.blue,
                    onClick = { isCreateDialogVisible = Pair(true, ModuleTemplate.EMPTY) }
                )
                GTCActionCard(
                    title = "Import",
                    icon = Icons.Rounded.FileDownload,
                    type = GTCActionCardType.SMALL,
                    actionColor = GTCTheme.colors.lightGray,
                    onClick = onImport,
                )
            }
        }

        templates.forEach { template ->
            ModuleTemplateCard(
                template = template,
                defaultTemplateId = defaultTemplateId,
                onEdit = { isEditDialogVisible = Pair(true, template) },
                onDelete = { if (!template.isDefault) onTemplateDelete(template) },
                onSetDefault = { onSetDefault(template) },
                onExport = {
                    Utils.exportModuleTemplate(project, template) { success, message ->
                        Utils.showInfo("GTC DevTools", message)
                    }
                },
                onReview = { isReviewDialogVisible = Pair(true, template) },
                onDuplicate = { templateToDuplicate ->
                    val duplicatedTemplate = templateToDuplicate.copy(
                        id = UUID.randomUUID().toString(),
                        name = "${templateToDuplicate.name} (Copy)",
                        isDefault = false
                    )
                    settings.addModuleTemplate(duplicatedTemplate)
                    onRefreshTriggered()
                    Utils.showInfo(
                        title = "GTC DevTools",
                        message = "Template duplicated as '${duplicatedTemplate.name}' successfully!",
                    )
                }
            )
        }

        if (isEditDialogVisible.first) {
            Dialog(
                onDismissRequest = { isEditDialogVisible = Pair(false, ModuleTemplate.EMPTY) },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            ) {
                TemplateEditorContent(
                    template = isEditDialogVisible.second,
                    onCancelClick = {
                        onRefreshTriggered()
                        isEditDialogVisible = Pair(false, ModuleTemplate.EMPTY)
                    },
                    onApplyClick = { updatedTemplate ->
                        settings.saveTemplate(updatedTemplate)
                    },
                    onOkayClick = { updatedTemplate ->
                        settings.saveTemplate(updatedTemplate)
                        Utils.showInfo(
                            title = "GTC DevTools",
                            message = "Module template '${updatedTemplate.name}' updated successfully!",
                        )
                        onRefreshTriggered()
                        isEditDialogVisible = Pair(false, ModuleTemplate.EMPTY)
                    },
                )
            }
        }

        if (isCreateDialogVisible.first) {
            Dialog(
                onDismissRequest = { isCreateDialogVisible = Pair(false, ModuleTemplate.EMPTY) },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            ) {
                TemplateCreatorContent(
                    onCancelClick = {
                        onRefreshTriggered()
                        isCreateDialogVisible = Pair(false, ModuleTemplate.EMPTY)
                    },
                    onApplyClick = { template ->
                        settings.saveTemplate(template)
                    },
                    onOkayClick = { template ->
                        settings.saveTemplate(template)
                        Utils.showInfo(
                            title = "GTC DevTools",
                            message = "Module template '${template.name}' added successfully!",
                        )
                        onRefreshTriggered()
                        isCreateDialogVisible = Pair(false, ModuleTemplate.EMPTY)
                    }
                )
            }
        }

        if (isReviewDialogVisible.first) {
            Dialog(
                onDismissRequest = { isReviewDialogVisible = Pair(false, ModuleTemplate.EMPTY) },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            ) {
                ModuleTemplateReviewContent(
                    template = isReviewDialogVisible.second,
                    onCancelClick = { isReviewDialogVisible = Pair(false, ModuleTemplate.EMPTY) },
                )
            }
        }
    }
}

@Composable
private fun ModuleTemplateCard(
    template: ModuleTemplate,
    defaultTemplateId: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onExport: () -> Unit,
    onReview: () -> Unit,
    onDuplicate: (ModuleTemplate) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = GTCTheme.colors.gray,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GTCText(
                        text = template.name,
                        color = GTCTheme.colors.white,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (template.id == defaultTemplateId) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            backgroundColor = GTCTheme.colors.blue.copy(alpha = 0.2f)
                        ) {
                            GTCText(
                                text = "Default",
                                color = GTCTheme.colors.blue,
                                style = TextStyle(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = GTCTheme.colors.lightGray
                    )
                }
                DropdownMenu(
                    modifier = Modifier.background(
                        color = GTCTheme.colors.black,
                        shape = RoundedCornerShape(0.dp)
                    ),
                    properties = PopupProperties(dismissOnClickOutside = true),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (template.id != defaultTemplateId) {
                        GTCDropdownItem(
                            text = "Set Default",
                            icon = Icons.Rounded.Check,
                            onClick = { expanded = false; onSetDefault() }
                        )
                    }
                    if (template.id != "candroid_template") {
                        GTCDropdownItem(
                            text = "Edit",
                            icon = Icons.Rounded.Edit,
                            onClick = { expanded = false; onEdit() }
                        )
                    } else {
                        GTCDropdownItem(
                            text = "Review",
                            icon = Icons.Rounded.RemoveRedEye,
                            onClick = { expanded = false; onReview() }
                        )
                    }
                    GTCDropdownItem(
                        text = "Duplicate",
                        icon = Icons.Rounded.ContentCopy,
                        onClick = { expanded = false; onDuplicate(template) }
                    )
                    GTCDropdownItem(
                        text = "Export",
                        icon = Icons.Rounded.Upload,
                        onClick = { expanded = false; onExport() }
                    )
                    if (!template.isDefault || template.id != "candroid_template") {
                        GTCDropdownItem(
                            text = "Delete",
                            icon = Icons.Rounded.Delete,
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    defaultPackageName: String,
    preferredModuleType: String,
    onSaveClick: () -> Unit,
    onPackageNameChange: (String) -> Unit,
    onModuleTypeChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingItem("Default Package Name") {
            GTCTextField(
                modifier = Modifier.fillMaxWidth(),
                placeholder = "com.example",
                color = GTCTheme.colors.lightGray,
                value = defaultPackageName,
                onValueChange = onPackageNameChange,
            )
        }
        SettingItem("Preferred Module Type") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GTCRadioButton(
                    text = Constants.ANDROID,
                    selected = preferredModuleType == Constants.ANDROID,
                    color = GTCTheme.colors.lightGray,
                    onClick = { onModuleTypeChange(Constants.ANDROID) },
                )

                Spacer(modifier = Modifier.width(16.dp))

                GTCRadioButton(
                    text = Constants.KOTLIN,
                    selected = preferredModuleType == Constants.KOTLIN,
                    color = GTCTheme.colors.lightGray,
                    onClick = { onModuleTypeChange(Constants.KOTLIN) },
                )
            }
        }
        GTCActionCard(
            modifier = Modifier.align(Alignment.End),
            title = "Save",
            icon = Icons.Rounded.Save,
            actionColor = GTCTheme.colors.lightGray,
            type = GTCActionCardType.MEDIUM,
            onClick = onSaveClick,
        )
    }
}

@Composable
private fun SettingItem(
    label: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = GTCTheme.colors.gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
    ) {
        GTCText(
            text = label,
            color = GTCTheme.colors.white,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        )

        description?.let {
            Spacer(modifier = Modifier.size(8.dp))
            GTCText(
                text = it,
                color = GTCTheme.colors.lightGray,
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        content()
    }
}

@Composable
private fun FeatureTemplatesTab(
    project: Project,
    templates: List<FeatureTemplate>,
    defaultTemplateId: String,
    onTemplateDelete: (FeatureTemplate) -> Unit,
    onRefreshTriggered: () -> Unit,
    onSetDefault: (FeatureTemplate) -> Unit,
    onImport: () -> Unit,
) {
    var isCreateDialogVisible by remember { mutableStateOf(Pair(false, FeatureTemplate.EMPTY)) }
    var isEditDialogVisible by remember { mutableStateOf(Pair(false, FeatureTemplate.EMPTY)) }
    var isReviewDialogVisible by remember { mutableStateOf(Pair(false, FeatureTemplate.EMPTY)) }
    val settings = SettingsService.getInstance()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GTCText(
                text = "Feature",
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GTCActionCard(
                    title = "Add Template",
                    icon = Icons.Rounded.Add,
                    type = GTCActionCardType.SMALL,
                    actionColor = GTCTheme.colors.blue,
                    onClick = { isCreateDialogVisible = Pair(true, FeatureTemplate.EMPTY) }
                )
                GTCActionCard(
                    title = "Import",
                    icon = Icons.Rounded.FileDownload,
                    type = GTCActionCardType.SMALL,
                    actionColor = GTCTheme.colors.lightGray,
                    onClick = onImport,
                )
            }
        }

        templates.forEach { template ->
            FeatureTemplateCard(
                template = template,
                defaultTemplateId = defaultTemplateId,
                onEdit = { isEditDialogVisible = Pair(true, template) },
                onDelete = { if (!template.isDefault) onTemplateDelete(template) },
                onSetDefault = { onSetDefault(template) },
                onExport = {
                    Utils.exportFeatureTemplate(project, template) { success, message ->
                        Utils.showInfo("GTC DevTools", message)
                    }
                },
                onReview = { isReviewDialogVisible = Pair(true, template) },
                onDuplicate = { templateToDuplicate ->
                    val duplicatedTemplate = templateToDuplicate.copy(
                        id = UUID.randomUUID().toString(),
                        name = "${templateToDuplicate.name} (Copy)",
                        isDefault = false
                    )
                    settings.addFeatureTemplate(duplicatedTemplate)
                    onRefreshTriggered()
                    Utils.showInfo(
                        title = "GTC DevTools",
                        message = "Template duplicated as '${duplicatedTemplate.name}' successfully!",
                    )
                }
            )
        }

        if (isEditDialogVisible.first) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            ) {
                FeatureTemplateEditorContent(
                    template = isEditDialogVisible.second,
                    onCancelClick = {
                        onRefreshTriggered()
                        isEditDialogVisible = Pair(false, FeatureTemplate.EMPTY)
                    },
                    onApplyClick = { updatedTemplate ->
                        settings.saveFeatureTemplate(updatedTemplate)
                    },
                    onOkayClick = { updatedTemplate ->
                        settings.saveFeatureTemplate(updatedTemplate)
                        Utils.showInfo(
                            title = "GTC DevTools",
                            message = "Feature template '${updatedTemplate.name}' updated successfully!",
                        )
                        onRefreshTriggered()
                        isEditDialogVisible = Pair(false, FeatureTemplate.EMPTY)
                    },
                )
            }
        }

        if (isCreateDialogVisible.first) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            ) {
                FeatureTemplateCreatorContent(
                    onCancelClick = {
                        onRefreshTriggered()
                        isCreateDialogVisible = Pair(false, FeatureTemplate.EMPTY)
                    },
                    onApplyClick = { template ->
                        settings.saveFeatureTemplate(template)
                    },
                    onOkayClick = { template ->
                        settings.saveFeatureTemplate(template)
                        Utils.showInfo(
                            title = "GTC DevTools",
                            message = "Feature template '${template.name}' added successfully!",
                        )
                        onRefreshTriggered()
                        isCreateDialogVisible = Pair(false, FeatureTemplate.EMPTY)
                    }
                )
            }
        }

        if (isReviewDialogVisible.first) {
            Dialog(
                onDismissRequest = { isReviewDialogVisible = Pair(false, FeatureTemplate.EMPTY) },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            ) {
                FeatureTemplateReviewContent(
                    template = isReviewDialogVisible.second,
                    onCancelClick = { isReviewDialogVisible = Pair(false, FeatureTemplate.EMPTY) },
                )
            }
        }
    }
}

@Composable
private fun FeatureTemplateCard(
    template: FeatureTemplate,
    defaultTemplateId: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onExport: () -> Unit,
    onReview: () -> Unit,
    onDuplicate: (FeatureTemplate) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = GTCTheme.colors.gray,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GTCText(
                        text = template.name,
                        color = GTCTheme.colors.white,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (template.id == defaultTemplateId) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            backgroundColor = GTCTheme.colors.blue.copy(alpha = 0.2f)
                        ) {
                            GTCText(
                                text = "Default",
                                color = GTCTheme.colors.blue,
                                style = TextStyle(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = GTCTheme.colors.lightGray
                    )
                }
                DropdownMenu(
                    modifier = Modifier.background(
                        color = GTCTheme.colors.black,
                        shape = RoundedCornerShape(0.dp)
                    ),
                    properties = PopupProperties(dismissOnClickOutside = true),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (template.id != defaultTemplateId) {
                        GTCDropdownItem(
                            text = "Set Default",
                            icon = Icons.Rounded.Check,
                            onClick = { expanded = false; onSetDefault() }
                        )
                    }
                    if (template.id != "candroid_template") {
                        GTCDropdownItem(
                            text = "Edit",
                            icon = Icons.Rounded.Edit,
                            onClick = { expanded = false; onEdit() }
                        )
                    } else {
                        GTCDropdownItem(
                            text = "Review",
                            icon = Icons.Rounded.RemoveRedEye,
                            onClick = { expanded = false; onReview() }
                        )
                    }
                    GTCDropdownItem(
                        text = "Duplicate",
                        icon = Icons.Rounded.ContentCopy,
                        onClick = { expanded = false; onDuplicate(template) }
                    )
                    GTCDropdownItem(
                        text = "Export",
                        icon = Icons.Rounded.Upload,
                        onClick = { expanded = false; onExport() }
                    )
                    if (!template.isDefault || template.id != "candroid_template") {
                        GTCDropdownItem(
                            text = "Delete",
                            icon = Icons.Rounded.Delete,
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}
