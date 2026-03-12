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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPSwitch
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import java.awt.Cursor
import javax.swing.SwingUtilities

private val urlRegex = Regex("""https?://\S+""")

private class UrlHighlightTransformation(private val urlColor: androidx.compose.ui.graphics.Color) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val annotated = buildAnnotatedString {
            append(text)
            urlRegex.findAll(text.text).forEach { match ->
                addStyle(
                    SpanStyle(
                        color = urlColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                    match.range.first,
                    match.range.last + 1,
                )
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

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
    onSlashTyped: () -> Unit = {},
    onClearSlash: () -> Unit = {},
    shouldClearSlash: Boolean = false,
    isRemoteControlActive: Boolean = false,
    onRemoteControlStart: () -> Unit = {},
    onRemoteControlStop: () -> Unit = {},
    onClickPreviewImage: (String) -> Unit = {},
    inputFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = inputFocusRequester

    LaunchedEffect(shouldClearSlash) {
        if (shouldClearSlash && inputValue.text.trimStart() == "/") {
            inputValue = TextFieldValue("")
            onClearSlash()
        }
    }

    LaunchedEffect(pendingInput) {
        if (pendingInput != null) {
            // Clear slash prefix if input was just "/" (triggered by command palette)
            val baseText = if (inputValue.text.trimStart() == "/") "" else inputValue.text
            val newText = if (baseText.isEmpty()) pendingInput else "$baseText $pendingInput"
            inputValue = TextFieldValue(newText, TextRange(newText.length))
            onPendingInputConsumed()
            focusRequester.requestFocus()
        }
    }

    val urlColor = TPTheme.colors.blue
    val urlTransformation = remember(urlColor) { UrlHighlightTransformation(urlColor) }
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
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(TPTheme.colors.gray)
            .border(
                width = 1.dp,
                color = TPTheme.colors.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            )
            .padding(8.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            TPActionCard(
                title = "Skills",
                icon = Icons.Rounded.AutoFixHigh,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onSkillsClick() },
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPActionCard(
                title = "Commands",
                icon = Icons.Rounded.PlayArrow,
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onCommandsClick() },
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPActionCard(
                title = "Plan",
                icon = Icons.Rounded.Map,
                actionColor = TPTheme.colors.warning,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = {
                    onSend("/plan")
                    focusRequester.requestFocus()
                },
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPActionCard(
                title = "Model",
                icon = Icons.Rounded.SmartToy,
                actionColor = TPTheme.colors.hintGray,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onChangeModelClick() },
            )
            Spacer(modifier = Modifier.weight(1f))
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
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color = TPTheme.colors.blue.copy(alpha = 0.15f))
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
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = TPTheme.colors.blue,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.size(4.dp))
                                val nameWithoutExt = fileName.substringBeforeLast(".")
                                val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
                                val shortedName =
                                    if (nameWithoutExt.length > 14) nameWithoutExt.take(5)
                                        .plus("...")
                                        .plus(nameWithoutExt.takeLast(5)) else nameWithoutExt
                                TPText(
                                    text = "$shortedName$extension",
                                    color = TPTheme.colors.blue,
                                    style = TextStyle(fontSize = 10.sp),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                        .clickable { onRemoveImage(path) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove image",
                                        tint = TPTheme.colors.blue,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Input field
                BasicTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        inputValue = newValue
                        if (newValue.text.trimStart() == "/") {
                            onSlashTyped()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isFocused) TPTheme.colors.blue.copy(alpha = 0.5f)
                                else TPTheme.colors.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(TPTheme.colors.black)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
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
                                Box(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (inputValue.text.isEmpty()) {
                                        TPText(
                                            text = "Ask Claude or type / for commands...",
                                            color = TPTheme.colors.hintGray,
                                            style = TextStyle(fontSize = 14.sp),
                                        )
                                    }
                                    innerTPTextField()
                                }
                            }
                            Spacer(modifier = Modifier.size(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // File inject button
                                val fileHover = remember { MutableInteractionSource() }
                                val isFileHovered by fileHover.collectIsHoveredAsState()
                                @OptIn(ExperimentalFoundationApi::class)
                                TooltipArea(
                                    tooltip = {
                                        Box(
                                            modifier = Modifier
                                                .background(TPTheme.colors.gray, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            TPText(text = "Add active file", color = TPTheme.colors.white, fontSize = 11.sp)
                                        }
                                    },
                                    tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AlternateEmail,
                                        contentDescription = "Add active file path",
                                        tint = if (isFileHovered) TPTheme.colors.blue else TPTheme.colors.lightGray,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .hoverable(fileHover)
                                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                            .clickable {
                                                val path = onInjectFile() ?: return@clickable
                                                val newText =
                                                    if (inputValue.text.isEmpty()) path else "${inputValue.text} $path"
                                                inputValue = TextFieldValue(newText, TextRange(newText.length))
                                            }
                                    )
                                }
                                // Image picker button
                                val imageHover = remember { MutableInteractionSource() }
                                val isImageHovered by imageHover.collectIsHoveredAsState()
                                @OptIn(ExperimentalFoundationApi::class)
                                TooltipArea(
                                    tooltip = {
                                        Box(
                                            modifier = Modifier
                                                .background(TPTheme.colors.gray, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            TPText(text = "Add image", color = TPTheme.colors.white, fontSize = 11.sp)
                                        }
                                    },
                                    tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = "Add image",
                                        tint = if (isImageHovered || selectedImagePaths.isNotEmpty()) TPTheme.colors.blue else TPTheme.colors.lightGray,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .hoverable(imageHover)
                                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                            .clickable { onPickImage() }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (inputValue.text.isNotEmpty()) {
                                    TPText(
                                        text = "${inputValue.text.length} characters",
                                        color = TPTheme.colors.hintGray.copy(alpha = 0.6f),
                                        style = TextStyle(fontSize = 10.sp),
                                    )
                                }
                                TPText(
                                    text = "Shift+Enter for new line",
                                    color = TPTheme.colors.hintGray,
                                    style = TextStyle(fontSize = 10.sp),
                                )
                            }
                        }
                    },
                    visualTransformation = urlTransformation,
                    minLines = 2,
                    maxLines = 8,
                )
            }

            // Send button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        color = if (hasContent) TPTheme.colors.blue
                            else TPTheme.colors.outline.copy(alpha = 0.3f),
                    )
                    .then(
                        if (hasContent) Modifier
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                            .clickable { doSend() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (hasContent) TPTheme.colors.white else TPTheme.colors.hintGray,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
