package com.github.teknasyon.plugin.toolwindow.manager.settings.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.components.TPTextField
import com.github.teknasyon.plugin.service.SettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ExportSettingsContent(
    settings: SettingsService,
    onExport: (Boolean, String) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedDirectory by remember { mutableStateOf<VirtualFile?>(null) }
    var fileName by remember {
        mutableStateOf(
            "TP-settings-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}"
        )
    }

    Column(
        modifier = Modifier
            .wrapContentHeight()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = TPTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        TPText(
            text = "Export Settings",
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        TPText(
            text = "Export all your templates and configurations to a JSON file",
            color = TPTheme.colors.lightGray,
            style = TextStyle(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        TPText(
            text = "Export Location",
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TPText(
                modifier = Modifier.weight(1f),
                text = selectedDirectory?.path ?: "Select a directory...",
                color = if (selectedDirectory != null) TPTheme.colors.white else TPTheme.colors.lightGray,
                style = TextStyle(fontSize = 14.sp)
            )

            TPActionCard(
                title = "Browse",
                icon = Icons.Rounded.FolderOpen,
                type = TPActionCardType.SMALL,
                actionColor = TPTheme.colors.lightGray,
                onClick = {
                    val project = ProjectManager.getInstance().defaultProject
                    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    descriptor.title = "Select Export Directory"

                    FileChooser.chooseFile(descriptor, project, null) { directory ->
                        selectedDirectory = directory
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TPText(
            text = "File Name",
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        TPTextField(
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Enter filename (without .json)",
            color = TPTheme.colors.white,
            value = fileName,
            onValueChange = { fileName = it }
        )

        Spacer(modifier = Modifier.height(4.dp))

        TPText(
            text = "File will be saved as: $fileName.json",
            color = TPTheme.colors.lightGray,
            style = TextStyle(fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.size(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            TPActionCard(
                title = "Cancel",
                type = TPActionCardType.MEDIUM,
                actionColor = TPTheme.colors.lightGray,
                onClick = onCancel
            )

            TPActionCard(
                title = "Export",
                icon = Icons.Rounded.Save,
                type = TPActionCardType.MEDIUM,
                actionColor = TPTheme.colors.blue,
                onClick = {
                    if (selectedDirectory != null && fileName.isNotBlank()) {
                        val filePath = "${selectedDirectory!!.path}/$fileName.json"
                        val success = settings.exportToFile(filePath)
                        val message = if (success) {
                            "Settings exported successfully to:\n$fileName.json"
                        } else {
                            "Failed to export settings. Please check permissions."
                        }
                        onExport(success, message)
                    } else {
                        onExport(false, "Please select a directory and enter a filename.")
                    }
                }
            )
        }
    }
}