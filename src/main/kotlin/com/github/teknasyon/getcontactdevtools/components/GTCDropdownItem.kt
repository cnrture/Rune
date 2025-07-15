package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme

@Composable
fun GTCDropdownItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GTCTheme.colors.white,
            )
            Spacer(modifier = Modifier.size(8.dp))
            GTCText(
                text = text,
                color = GTCTheme.colors.white,
            )
        }
    }
}