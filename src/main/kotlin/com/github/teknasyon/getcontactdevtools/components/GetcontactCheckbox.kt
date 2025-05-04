package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@Composable
fun GetcontactCheckbox(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .selectable(
                selected = checked,
                role = Role.Checkbox,
                onClick = { onCheckedChange(checked.not()) }
            )
            .padding(end = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = GetcontactTheme.colors.blue,
                uncheckedColor = GetcontactTheme.colors.white,
            )
        )
        Text(
            text = label,
            color = GetcontactTheme.colors.white,
        )
    }
}