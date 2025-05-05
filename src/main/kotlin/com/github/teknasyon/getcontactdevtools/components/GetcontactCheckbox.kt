package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GetcontactCheckbox(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    isBackgroundEnable: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .selectable(
                selected = checked,
                role = Role.Checkbox,
                onClick = { onCheckedChange(checked.not()) }
            )
            .then(
                if (isBackgroundEnable && checked) {
                    Modifier.background(
                        color = GetcontactTheme.colors.blue,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = if (isBackgroundEnable && checked) {
                        GetcontactTheme.colors.white
                    } else {
                        GetcontactTheme.colors.blue
                    },
                    uncheckedColor = GetcontactTheme.colors.white,
                    checkmarkColor = if (isBackgroundEnable && checked) {
                        GetcontactTheme.colors.blue
                    } else {
                        GetcontactTheme.colors.white
                    },
                )
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        GetcontactText(
            text = label,
            color = GetcontactTheme.colors.white,
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}