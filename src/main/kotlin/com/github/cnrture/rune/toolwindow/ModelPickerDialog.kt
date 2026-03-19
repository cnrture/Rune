package com.github.cnrture.rune.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.cnrture.rune.common.AppIcons
import com.github.cnrture.rune.components.TPText
import com.github.cnrture.rune.theme.TPTheme
import java.awt.Cursor

@Composable
internal fun ModelPickerDialog(
    currentModelId: String?,
    onSelect: (ClaudeModel) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .background(TPTheme.colors.surface, RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp),
        ) {
            TPText(
                text = "Select Model",
                color = TPTheme.colors.textPrimary,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ClaudeModel.AVAILABLE.forEach { model ->
                val isSelected = currentModelId == model.id
                val hoverSource = remember { MutableInteractionSource() }
                val isHovered by hoverSource.collectIsHoveredAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverable(hoverSource)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isSelected -> TPTheme.colors.accent.copy(alpha = 0.15f)
                                isHovered -> TPTheme.colors.outline.copy(alpha = 0.15f)
                                else -> TPTheme.colors.surface
                            }
                        )
                        .clickable {
                            onSelect(model)
                            onDismiss()
                        }
                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (isSelected) {
                        Icon(
                            painter = AppIcons.painter("check"),
                            contentDescription = null,
                            tint = TPTheme.colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Spacer(modifier = Modifier.size(18.dp))
                    }
                    TPText(
                        text = model.displayName,
                        color = if (isSelected) TPTheme.colors.accent else TPTheme.colors.textPrimary,
                        style = TextStyle(fontSize = 14.sp),
                    )
                }
            }
        }
    }
}
