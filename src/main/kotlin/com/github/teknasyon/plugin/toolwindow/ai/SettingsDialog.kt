package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project

@Composable
fun SettingsDialog(
    currentSkillsPath: String,
    currentAgentsPath: String,
    project: Project,
    onSave: (skillsPath: String, agentsPath: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var skillsPath by remember { mutableStateOf(currentSkillsPath) }
    var agentsPath by remember { mutableStateOf(currentAgentsPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SkillDock Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PathRow(
                    label = "Skills Root Path",
                    path = skillsPath,
                    onPathChange = { skillsPath = it },
                    project = project,
                    browseTitle = "Select Skills Directory",
                )
                PathRow(
                    label = "Agents Root Path",
                    path = agentsPath,
                    onPathChange = { agentsPath = it },
                    project = project,
                    browseTitle = "Select Agents Directory",
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(skillsPath.trim(), agentsPath.trim()) },
                enabled = skillsPath.isNotBlank() || agentsPath.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PathRow(
    label: String,
    path: String,
    onPathChange: (String) -> Unit,
    project: Project,
    browseTitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.caption)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = path,
                onValueChange = onPathChange,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle(browseTitle)
                    FileChooser.chooseFile(descriptor, project, null) { file ->
                        onPathChange(file.path)
                    }
                },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Browse")
            }
        }
    }
}
