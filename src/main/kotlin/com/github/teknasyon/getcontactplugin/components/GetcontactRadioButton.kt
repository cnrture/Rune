package com.github.teknasyon.getcontactplugin.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactplugin.theme.GetcontactTheme

@Composable
fun GetcontactRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = onClick,
            )
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            colors = RadioButtonDefaults.colors(
                selectedColor = GetcontactTheme.colors.blue,
                unselectedColor = GetcontactTheme.colors.white,
            ),
            selected = selected,
            onClick = onClick,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            color = GetcontactTheme.colors.white,
        )
    }
}