package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun SkillItemView(
    skill: Skill,
    showRunButton: Boolean = true,
    onExecute: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenFile: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    if (showDialog) {
        val focusRequester = remember { FocusRequester() }

        Dialog(
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
            onDismissRequest = {
                showDialog = false
                inputText = ""
            },
            content = {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .background(
                            color = TPTheme.colors.gray,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(16.dp),
                ) {
                    Text(
                        text = skill.relativePath,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = TPTheme.colors.white,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Arguments (optional)") },
                        placeholder = { Text("e.g. --flag value") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TPTheme.colors.white,
                            focusedBorderColor = TPTheme.colors.blue,
                            unfocusedBorderColor = TPTheme.colors.outline,
                            focusedLabelColor = TPTheme.colors.blue,
                            unfocusedLabelColor = TPTheme.colors.lightGray,
                            placeholderColor = TPTheme.colors.hintGray,
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                showDialog = false
                                inputText = ""
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = TPTheme.colors.lightGray)
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                onExecute(inputText.trim())
                                showDialog = false
                                inputText = ""
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = TPTheme.colors.blue)
                        ) {
                            Text("Run")
                        }
                    }
                }
            }
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = TPTheme.colors.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = skill.relativePath,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    color = TPTheme.colors.white,
                )
                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open file",
                        tint = TPTheme.colors.lightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (skill.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (skill.isFavorite) TPTheme.colors.warning else TPTheme.colors.lightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (showRunButton) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "Run",
                            tint = TPTheme.colors.blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            if (skill.description.isNotBlank()) {
                Text(
                    text = skill.description,
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.4.sp),
                    color = TPTheme.colors.lightGray,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
