package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReviewTrackerDialog(
    onStart: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var prUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Request Tracker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the URL of the pull request you want to track:")
                OutlinedTextField(
                    value = prUrl,
                    onValueChange = { prUrl = it },
                    label = { Text("PR URL") },
                    placeholder = { Text("https://github.com/owner/repo/pull/123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (prUrl.isNotBlank()) onStart(prUrl.trim()) },
                enabled = prUrl.isNotBlank(),
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
