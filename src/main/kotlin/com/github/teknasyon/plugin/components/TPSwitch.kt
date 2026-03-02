package com.github.teknasyon.plugin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.theme.TPTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TPSwitch(
    modifier: Modifier = Modifier,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .selectable(
                selected = checked,
                role = Role.Switch,
                onClick = { onCheckedChange(checked.not()) }
            )
            .background(
                color = if (checked) TPTheme.colors.blue.copy(alpha = 0.3f) else TPTheme.colors.blue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TPText(
            text = text,
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.size(4.dp))
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            Switch(
                modifier = Modifier.scale(0.80f),
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TPTheme.colors.blue,
                    checkedTrackColor = TPTheme.colors.blue.copy(alpha = 0.5f),
                )
            )
        }
    }
}