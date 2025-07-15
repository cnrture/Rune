package com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.components.GTCTextField
import com.github.teknasyon.getcontactdevtools.data.FeatureTemplate
import com.github.teknasyon.getcontactdevtools.data.FileTemplate
import com.github.teknasyon.getcontactdevtools.data.ModuleTemplate
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.component.FileTemplateEditor

@Composable
fun TemplateEditorContent(
    template: ModuleTemplate,
    onCancelClick: () -> Unit,
    onApplyClick: (ModuleTemplate) -> Unit,
    onOkayClick: (ModuleTemplate) -> Unit,
) {
    var templateName by remember { mutableStateOf(template.name) }
    val fileTemplates = remember { mutableStateListOf<FileTemplate>().apply { addAll(template.fileTemplates) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = GTCTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier.size(28.dp)
            )
            GTCText(
                text = template.name,
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onCancelClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                GTCTextField(
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Template Name",
                    color = if (template.isDefault) GTCTheme.colors.lightGray.copy(alpha = 0.5f) else GTCTheme.colors.white,
                    value = templateName,
                    onValueChange = { if (!template.isDefault) templateName = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                GTCText(
                    text = "File Content (use {NAME}, {PACKAGE}, {FILE_PACKAGE} placeholders):",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GTCText(
                    text = "{NAME} -> Name of the file without extension",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                GTCText(
                    text = "{PACKAGE} -> Package structure (ex., com.example.app)",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                GTCText(
                    text = "{FILE_PACKAGE} -> Package structure without dots (ex., com.example.app.repository)",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(24.dp))
                fileTemplates.forEachIndexed { index, fileTemplate ->
                    FileTemplateEditor(
                        fileTemplate = fileTemplate,
                        isModuleEdit = true,
                        onUpdate = { fileTemplates[index] = it },
                        onDelete = { fileTemplates.removeAt(index) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                GTCActionCard(
                    title = "Add File Template",
                    icon = Icons.Rounded.Add,
                    type = GTCActionCardType.MEDIUM,
                    actionColor = GTCTheme.colors.blue,
                    onClick = {
                        fileTemplates.add(
                            FileTemplate("", "", "")
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            GTCActionCard(
                title = "Apply",
                type = GTCActionCardType.MEDIUM,
                actionColor = GTCTheme.colors.blue,
                isEnabled = templateName.isNotBlank() && fileTemplates.any { it.fileName.isNotBlank() },
                onClick = {
                    val updatedTemplate = template.copy(
                        name = templateName,
                        fileTemplates = fileTemplates.filter { it.fileName.isNotBlank() }
                    )
                    onApplyClick(updatedTemplate)
                }
            )

            GTCActionCard(
                title = "Okay",
                type = GTCActionCardType.MEDIUM,
                actionColor = GTCTheme.colors.blue,
                isEnabled = templateName.isNotBlank() && fileTemplates.any { it.fileName.isNotBlank() },
                onClick = {
                    val updatedTemplate = template.copy(
                        name = templateName,
                        fileTemplates = fileTemplates.filter { it.fileName.isNotBlank() }
                    )
                    onOkayClick(updatedTemplate)
                },
            )
        }
    }
}

@Composable
fun FeatureTemplateEditorContent(
    template: FeatureTemplate,
    onCancelClick: () -> Unit,
    onApplyClick: (FeatureTemplate) -> Unit,
    onOkayClick: (FeatureTemplate) -> Unit,
) {
    var templateName by remember { mutableStateOf(template.name) }
    val fileTemplates = remember { mutableStateListOf<FileTemplate>().apply { addAll(template.fileTemplates) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = GTCTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier.size(28.dp)
            )
            GTCText(
                text = template.name,
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onCancelClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                GTCTextField(
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Template Name",
                    color = if (template.isDefault) GTCTheme.colors.lightGray.copy(alpha = 0.5f) else GTCTheme.colors.white,
                    value = templateName,
                    onValueChange = { if (!template.isDefault) templateName = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                GTCText(
                    text = "File Content (use {NAME}, {PACKAGE}, {FILE_PACKAGE} placeholders):",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GTCText(
                    text = "{NAME} -> Name of the file without extension",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                GTCText(
                    text = "{PACKAGE} -> Package structure (ex., com.example.app)",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                GTCText(
                    text = "{FILE_PACKAGE} -> Package structure without dots (ex., com.example.app.repository)",
                    color = GTCTheme.colors.lightGray,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(24.dp))
                fileTemplates.forEachIndexed { index, fileTemplate ->
                    FileTemplateEditor(
                        fileTemplate = fileTemplate,
                        isModuleEdit = false,
                        onUpdate = { fileTemplates[index] = it },
                        onDelete = { fileTemplates.removeAt(index) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                GTCActionCard(
                    title = "Add File Template",
                    icon = Icons.Rounded.Add,
                    type = GTCActionCardType.MEDIUM,
                    actionColor = GTCTheme.colors.blue,
                    onClick = {
                        fileTemplates.add(
                            FileTemplate("", "", "")
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            GTCActionCard(
                title = "Apply",
                type = GTCActionCardType.MEDIUM,
                actionColor = GTCTheme.colors.blue,
                isEnabled = templateName.isNotBlank() && fileTemplates.any { it.fileName.isNotBlank() },
                onClick = {
                    val updatedTemplate = template.copy(
                        name = templateName,
                        fileTemplates = fileTemplates.filter { it.fileName.isNotBlank() }
                    )
                    onApplyClick(updatedTemplate)
                }
            )

            GTCActionCard(
                title = "Okay",
                type = GTCActionCardType.MEDIUM,
                actionColor = GTCTheme.colors.blue,
                isEnabled = templateName.isNotBlank() && fileTemplates.any { it.fileName.isNotBlank() },
                onClick = {
                    val updatedTemplate = template.copy(
                        name = templateName,
                        fileTemplates = fileTemplates.filter { it.fileName.isNotBlank() }
                    )
                    onOkayClick(updatedTemplate)
                },
            )
        }
    }
}
