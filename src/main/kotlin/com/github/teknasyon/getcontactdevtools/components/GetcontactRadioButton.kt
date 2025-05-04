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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GetcontactRadioButton(
    text: String,
    selected: Boolean,
    isBackgroundEnable: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(
                if (isBackgroundEnable) {
                    Modifier.background(
                        color = GetcontactTheme.colors.orange,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            RadioButton(
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (isBackgroundEnable) GetcontactTheme.colors.gray else GetcontactTheme.colors.white,
                    unselectedColor = if (isBackgroundEnable) GetcontactTheme.colors.gray else GetcontactTheme.colors.white,
                ),
                selected = selected,
                onClick = onClick,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            color = if (isBackgroundEnable) GetcontactTheme.colors.gray else GetcontactTheme.colors.white,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}