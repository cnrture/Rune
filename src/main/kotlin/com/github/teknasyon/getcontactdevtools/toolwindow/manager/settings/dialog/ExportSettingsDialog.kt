package com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.dialog

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
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.components.GTCTextField
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
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
            "GTC-settings-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}"
        )
    }

    Column(
        modifier = Modifier
            .wrapContentHeight()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = GTCTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        GTCText(
            text = "Export Settings",
            color = GTCTheme.colors.white,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        GTCText(
            text = "Export all your templates and configurations to a JSON file",
            color = GTCTheme.colors.lightGray,
            style = TextStyle(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        GTCText(
            text = "Export Location",
            color = GTCTheme.colors.white,
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
            GTCText(
                modifier = Modifier.weight(1f),
                text = selectedDirectory?.path ?: "Select a directory...",
                color = if (selectedDirectory != null) GTCTheme.colors.white else GTCTheme.colors.lightGray,
                style = TextStyle(fontSize = 14.sp)
            )

            GTCActionCard(
                title = "Browse",
                icon = Icons.Rounded.FolderOpen,
                type = GTCActionCardType.SMALL,
                actionColor = GTCTheme.colors.lightGray,
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

        GTCText(
            text = "File Name",
            color = GTCTheme.colors.white,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        GTCTextField(
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Enter filename (without .json)",
            color = GTCTheme.colors.white,
            value = fileName,
            onValueChange = { fileName = it }
        )

        Spacer(modifier = Modifier.height(4.dp))

        GTCText(
            text = "File will be saved as: $fileName.json",
            color = GTCTheme.colors.lightGray,
            style = TextStyle(fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.size(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            GTCActionCard(
                title = "Cancel",
                type = GTCActionCardType.MEDIUM,
                actionColor = GTCTheme.colors.lightGray,
                onClick = onCancel
            )

            GTCActionCard(
                title = "Export",
                icon = Icons.Rounded.Save,
                type = GTCActionCardType.MEDIUM,
                actionColor = GTCTheme.colors.blue,
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