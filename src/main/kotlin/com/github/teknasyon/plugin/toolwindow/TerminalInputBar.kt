package com.github.teknasyon.plugin.toolwindow

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPSwitch
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import java.awt.Cursor
import javax.swing.SwingUtilities

@Composable
internal fun TerminalInputBar(
    onSend: (String) -> Unit,
    onInjectFile: () -> String?,
    selectedImagePaths: List<String>,
    onPickImage: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onClearImages: () -> Unit,
    pendingInput: String?,
    onPendingInputConsumed: () -> Unit,
    onChangeModelClick: () -> Unit,
    onSkillsClick: () -> Unit,
    onCommandsClick: () -> Unit,
    isRemoteControlActive: Boolean = false,
    onRemoteControlStart: () -> Unit = {},
    onRemoteControlStop: () -> Unit = {},
    onClickPreviewImage: (String) -> Unit = {},
) {
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(pendingInput) {
        if (pendingInput != null) {
            val newText = if (inputValue.text.isEmpty()) pendingInput else "${inputValue.text} $pendingInput"
            inputValue = TextFieldValue(newText, TextRange(newText.length))
            onPendingInputConsumed()
            focusRequester.requestFocus()
        }
    }

    val hasContent = inputValue.text.isNotBlank() || selectedImagePaths.isNotEmpty()

    fun doSend() {
        if (!hasContent) return
        val message = buildString {
            if (inputValue.text.isNotBlank()) append(inputValue.text)
            for (path in selectedImagePaths) {
                if (isNotEmpty()) append(" ")
                append(path)
            }
        }
        inputValue = TextFieldValue("")
        onSend(message)
        SwingUtilities.invokeLater { onClearImages() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TPTheme.colors.gray)
            .padding(8.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            TPActionCard(
                title = "Model",
                icon = Icons.Rounded.SmartToy,
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.EXTRA_SMALL,
                onClick = { onChangeModelClick() },
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPActionCard(
                title = "Skills",
                icon = Icons.Rounded.AutoFixHigh,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                onClick = { onSkillsClick() },
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPActionCard(
                title = "Commands",
                icon = Icons.Rounded.PlayArrow,
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.EXTRA_SMALL,
                onClick = { onCommandsClick() },
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(8.dp))
            TPSwitch(
                checked = isRemoteControlActive,
                text = "Remote",
                onCheckedChange = {
                    if (isRemoteControlActive) onRemoteControlStop() else onRemoteControlStart()
                },
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Middle area: image chips + input field
            Column(
                modifier = Modifier.weight(1f),
            ) {
                if (selectedImagePaths.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        selectedImagePaths.forEach { path ->
                            val fileName = path.substringAfterLast("/")
                            val thumbnail = remember(path) { loadImageBitmapFromPath(path) }
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = TPTheme.colors.blue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                    .clickable { onClickPreviewImage(path) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (thumbnail != null) {
                                    Image(
                                        bitmap = thumbnail,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = TPTheme.colors.blue,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.size(4.dp))
                                val fileNameWithoutExtension = fileName.substringBefore(".")
                                val shortedFileName =
                                    if (fileNameWithoutExtension.length > 8) fileNameWithoutExtension.take(3)
                                        .plus("...")
                                        .plus(fileNameWithoutExtension.takeLast(4)) else fileNameWithoutExtension
                                TPText(
                                    text = shortedFileName,
                                    color = TPTheme.colors.blue,
                                    style = TextStyle(fontSize = 10.sp),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove image",
                                    tint = TPTheme.colors.blue,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                        .clickable { onRemoveImage(path) },
                                )
                            }
                        }
                    }
                }

                // Input field
                BasicTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        inputValue = newValue
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TPTheme.colors.black)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                if (event.isShiftPressed) {
                                    val cursor = inputValue.selection.start
                                    val newText =
                                        inputValue.text.substring(0, cursor) + "\n" + inputValue.text.substring(cursor)
                                    inputValue = TextFieldValue(newText, TextRange(cursor + 1))
                                    true
                                } else {
                                    doSend()
                                    true
                                }
                            } else false
                        },
                    textStyle = TextStyle(
                        color = TPTheme.colors.white,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(TPTheme.colors.white),
                    decorationBox = { innerTPTextField ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                // Left side icons — horizontal row
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    // File inject button
                                    Icon(
                                        imageVector = Icons.Rounded.AlternateEmail,
                                        contentDescription = "Add active file path",
                                        tint = TPTheme.colors.lightGray,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                            .clickable {
                                                val path = onInjectFile() ?: return@clickable
                                                val newText =
                                                    if (inputValue.text.isEmpty()) path else "${inputValue.text} $path"
                                                inputValue = TextFieldValue(newText, TextRange(newText.length))
                                            }
                                    )
                                    // Image picker button
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = "Add image",
                                        tint = if (selectedImagePaths.isNotEmpty()) TPTheme.colors.blue else TPTheme.colors.lightGray,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                            .clickable { onPickImage() }
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (inputValue.text.isEmpty()) {
                                        TPText(
                                            text = "Write your message here...",
                                            color = TPTheme.colors.hintGray,
                                            style = TextStyle(fontSize = 14.sp),
                                        )
                                    }
                                    innerTPTextField()
                                }
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            // Plan Mode
                            TPActionCard(
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                                title = "Plan Mode",
                                icon = Icons.Rounded.Map,
                                actionColor = TPTheme.colors.warning,
                                type = TPActionCardType.EXTRA_SMALL,
                                onClick = {
                                    onSend("/plan")
                                    focusRequester.requestFocus()
                                },
                            )
                        }
                    },
                    minLines = 4,
                )
            }

            // Send button
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send",
                tint = if (hasContent) TPTheme.colors.blue else TPTheme.colors.hintGray,
                modifier = Modifier
                    .size(28.dp)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                    .clickable { doSend() }
            )
        }
    }
}
