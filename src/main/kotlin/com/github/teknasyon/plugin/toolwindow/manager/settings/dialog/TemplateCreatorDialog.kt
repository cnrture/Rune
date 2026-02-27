package com.github.teknasyon.plugin.toolwindow.manager.settings.dialog

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.components.TPTextField
import com.github.teknasyon.plugin.data.FileTemplate
import com.github.teknasyon.plugin.data.ModuleTemplate
import com.github.teknasyon.plugin.theme.TPTheme
import com.github.teknasyon.plugin.toolwindow.manager.settings.component.FileTemplateEditor
import java.util.*

@Composable
fun TemplateCreatorContent(
    onCancelClick: () -> Unit,
    onApplyClick: (ModuleTemplate) -> Unit,
    onOkayClick: (ModuleTemplate) -> Unit,
) {
    var templateName by remember { mutableStateOf("") }
    val fileTemplates = remember { mutableStateListOf<FileTemplate>() }
    var id by remember { mutableStateOf(Constants.EMPTY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = TPTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Row {
            TPText(
                text = "Create New Module Template",
                color = TPTheme.colors.white,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = TPTheme.colors.lightGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onCancelClick() }
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

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
                TPTextField(
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Template Name",
                    color = TPTheme.colors.white,
                    value = templateName,
                    onValueChange = { templateName = it }
                )
                Spacer(modifier = Modifier.size(16.dp))
                TPText(
                    text = "File Content (use {NAME}, {PACKAGE}, {FILE_PACKAGE} placeholders):",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.size(8.dp))
                TPText(
                    text = "{NAME} -> Name of the file without extension",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )
                TPText(
                    text = "{PACKAGE} -> Package structure (ex., com.example.app)",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )
                TPText(
                    text = "{FILE_PACKAGE} -> Package structure without dots (ex., com.example.app.repository)",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.size(24.dp))
                fileTemplates.forEachIndexed { index, fileTemplate ->
                    FileTemplateEditor(
                        fileTemplate = fileTemplate,
                        isModuleEdit = true,
                        onUpdate = { fileTemplates[index] = it },
                        onDelete = { fileTemplates.removeAt(index) }
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }
                Spacer(modifier = Modifier.size(12.dp))
                TPActionCard(
                    title = "Add File Template",
                    icon = Icons.Rounded.Add,
                    type = TPActionCardType.MEDIUM,
                    actionColor = TPTheme.colors.blue,
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
            TPActionCard(
                title = "Apply",
                type = TPActionCardType.MEDIUM,
                actionColor = TPTheme.colors.blue,
                isEnabled = templateName.isNotBlank() && fileTemplates.any { it.fileName.isNotBlank() },
                onClick = {
                    if (templateName.isNotBlank()) {
                        val template = ModuleTemplate(
                            id = id.ifEmpty { UUID.randomUUID().toString().apply { id = this } },
                            name = templateName,
                            fileTemplates = fileTemplates.filter { it.fileName.isNotBlank() },
                            isDefault = false
                        )
                        onApplyClick(template)
                    }
                }
            )

            TPActionCard(
                title = "Okay",
                type = TPActionCardType.MEDIUM,
                actionColor = TPTheme.colors.blue,
                isEnabled = templateName.isNotBlank() && fileTemplates.any { it.fileName.isNotBlank() },
                onClick = {
                    if (templateName.isNotBlank()) {
                        val template = ModuleTemplate(
                            id = id.ifEmpty { UUID.randomUUID().toString().apply { id = this } },
                            name = templateName,
                            fileTemplates = fileTemplates.filter { it.fileName.isNotBlank() },
                            isDefault = false
                        )
                        onOkayClick(template)
                    }
                },
            )
        }
    }
}
