package com.github.teknasyon.plugin.toolwindow.manager.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.github.teknasyon.plugin.common.Utils
import com.github.teknasyon.plugin.components.*
import com.github.teknasyon.plugin.data.ModuleTemplate
import com.github.teknasyon.plugin.service.SettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.github.teknasyon.plugin.toolwindow.manager.settings.dialog.ModuleTemplateReviewContent
import com.github.teknasyon.plugin.toolwindow.manager.settings.dialog.TemplateCreatorContent
import com.github.teknasyon.plugin.toolwindow.manager.settings.dialog.TemplateEditorContent
import com.intellij.openapi.project.Project
import java.util.*

@Composable
fun SettingsContent(project: Project) {
    val settings = SettingsService.getInstance()
    var currentSettings by mutableStateOf(settings.state.copy())

    var refreshTrigger by remember { mutableStateOf(0) }
    val moduleTemplates by remember(refreshTrigger) {
        mutableStateOf(settings.getModuleTemplates())
    }

    val triggerRefresh = { refreshTrigger++ }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(TPTheme.colors.black)
            .padding(24.dp),
        backgroundColor = TPTheme.colors.black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TPText(
                modifier = Modifier.fillMaxWidth(),
                text = "Settings",
                style = TextStyle(
                    color = TPTheme.colors.lightGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            )

            Spacer(modifier = Modifier.size(24.dp))

            ModuleTemplatesTab(
                project = project,
                templates = moduleTemplates,
                defaultTemplateId = currentSettings.defaultModuleTemplateId,
                onTemplateDelete = { template ->
                    if (!template.isDefault) {
                        settings.removeTemplate(template)
                        triggerRefresh()
                        Utils.showInfo(
                            title = "Teknasyon DevTools",
                            message = "Template '${template.name}' deleted successfully!",
                        )
                    }
                },
                onRefreshTriggered = { triggerRefresh() },
                onSetDefault = { template ->
                    settings.setDefaultModuleTemplate(template.id)
                    triggerRefresh()
                    Utils.showInfo(
                        title = "Teknasyon DevTools",
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
                            title = "Teknasyon DevTools",
                            message = message,
                        )
                    }
                },
            )
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
            TPText(
                text = "Module",
                color = TPTheme.colors.white,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TPActionCard(
                    title = "Add Template",
                    icon = Icons.Rounded.Add,
                    type = TPActionCardType.SMALL,
                    actionColor = TPTheme.colors.blue,
                    onClick = { isCreateDialogVisible = Pair(true, ModuleTemplate.EMPTY) }
                )
                TPActionCard(
                    title = "Import",
                    icon = Icons.Rounded.FileDownload,
                    type = TPActionCardType.SMALL,
                    actionColor = TPTheme.colors.lightGray,
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
                    Utils.exportModuleTemplate(project, template) { _, message ->
                        Utils.showInfo("Teknasyon DevTools", message)
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
                        title = "Teknasyon DevTools",
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
                            title = "Teknasyon DevTools",
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
                            title = "Teknasyon DevTools",
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
        backgroundColor = TPTheme.colors.gray,
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
                    TPText(
                        text = template.name,
                        color = TPTheme.colors.white,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (template.id == defaultTemplateId) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            backgroundColor = TPTheme.colors.blue.copy(alpha = 0.2f)
                        ) {
                            TPText(
                                text = "Default",
                                color = TPTheme.colors.blue,
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
                        tint = TPTheme.colors.lightGray
                    )
                }
                DropdownMenu(
                    modifier = Modifier.background(
                        color = TPTheme.colors.black,
                        shape = RoundedCornerShape(0.dp)
                    ),
                    properties = PopupProperties(dismissOnClickOutside = true),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (template.id != defaultTemplateId) {
                        TPDropdownItem(
                            text = "Set Default",
                            icon = Icons.Rounded.Check,
                            onClick = { expanded = false; onSetDefault() }
                        )
                    }
                    if (template.id != "candroid_template") {
                        TPDropdownItem(
                            text = "Edit",
                            icon = Icons.Rounded.Edit,
                            onClick = { expanded = false; onEdit() }
                        )
                    } else {
                        TPDropdownItem(
                            text = "Review",
                            icon = Icons.Rounded.RemoveRedEye,
                            onClick = { expanded = false; onReview() }
                        )
                    }
                    TPDropdownItem(
                        text = "Duplicate",
                        icon = Icons.Rounded.ContentCopy,
                        onClick = { expanded = false; onDuplicate(template) }
                    )
                    TPDropdownItem(
                        text = "Export",
                        icon = Icons.Rounded.Upload,
                        onClick = { expanded = false; onExport() }
                    )
                    if (!template.isDefault || template.id != "candroid_template") {
                        TPDropdownItem(
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