package com.github.cnrture.rune.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.cnrture.rune.components.TPActionCard
import com.github.cnrture.rune.components.TPActionCardType
import com.github.cnrture.rune.components.TPCheckbox
import com.github.cnrture.rune.components.TPText
import com.github.cnrture.rune.theme.TPTheme

@Composable
internal fun RemoteControlDialog(
    onDismiss: () -> Unit,
    onConfirm: (preventSleep: Boolean) -> Unit,
) {
    var preventSleep by remember { mutableStateOf(false) }

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
                .width(320.dp)
                .background(TPTheme.colors.surface, RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            TPText(
                text = "Remote Control",
                color = TPTheme.colors.textPrimary,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TPText(
                text = "Start a remote control session to continue this conversation from your phone or browser.",
                color = TPTheme.colors.textSecondary,
                style = TextStyle(fontSize = 12.sp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TPCheckbox(
                checked = preventSleep,
                label = "Prevent sleep mode (caffeinate)",
                color = TPTheme.colors.accent,
                onCheckedChange = { preventSleep = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                TPText(
                    text = "Prevents sleep in these cases:",
                    color = TPTheme.colors.textSecondary,
                    style = TextStyle(fontSize = 12.sp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                TPText(
                    text = "\u2022 Screen idle timeout (display sleep)\n\u2022 System idle timeout (idle sleep)\n\u2022 Lid close while charging (system sleep)",
                    color = TPTheme.colors.textSecondary,
                    style = TextStyle(fontSize = 12.sp, lineHeight = 14.sp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                TPText(
                    text = "Does not prevent: lid close on battery, manual sleep, low battery shutdown.",
                    color = TPTheme.colors.textSecondary.copy(alpha = 0.6f),
                    style = TextStyle(fontSize = 12.sp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TPActionCard(
                    title = "Cancel",
                    actionColor = TPTheme.colors.textSecondary,
                    type = TPActionCardType.MEDIUM,
                    onClick = onDismiss,
                    isBorderless = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TPActionCard(
                    title = "Start",
                    actionColor = TPTheme.colors.accent,
                    type = TPActionCardType.MEDIUM,
                    onClick = { onConfirm(preventSleep) },
                    isBorderless = true,
                )
            }
        }
    }
}
