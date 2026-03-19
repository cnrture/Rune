package com.github.cnrture.rune.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.theme.TPTheme

enum class TPActionCardType { EXTRA_SMALL, SMALL, MEDIUM }

@Composable
fun TPActionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: Painter? = null,
    actionColor: Color,
    isTextVisible: Boolean = true,
    type: TPActionCardType = TPActionCardType.MEDIUM,
    isEnabled: Boolean = true,
    isBorderless: Boolean = false,
    onClick: () -> Unit,
) {
    val fontSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 12.sp
        TPActionCardType.SMALL -> 14.sp
        TPActionCardType.MEDIUM -> 16.sp
    }
    val iconSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 14.dp
        TPActionCardType.SMALL -> 16.dp
        TPActionCardType.MEDIUM -> 20.dp
    }
    val borderSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 1.dp
        TPActionCardType.SMALL -> 2.dp
        TPActionCardType.MEDIUM -> 2.dp
    }
    val padding = when (type) {
        TPActionCardType.EXTRA_SMALL -> 6.dp
        TPActionCardType.SMALL -> 8.dp
        TPActionCardType.MEDIUM -> 12.dp
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isBorderless) {
                    if (isEnabled) actionColor.copy(alpha = 0.12f) else TPTheme.colors.textSecondary.copy(alpha = 0.05f)
                } else {
                    if (isEnabled) TPTheme.colors.surface else TPTheme.colors.textSecondary.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(12.dp),
            )
            .then(
                if (!isBorderless) Modifier.border(
                    width = borderSize,
                    color = if (isEnabled) actionColor else TPTheme.colors.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .then(
                if (isEnabled) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(iconSize)
            )
        }

        if (icon != null && title != null && isTextVisible) {
            Spacer(modifier = Modifier.size(4.dp))
        }

        if (isTextVisible) {
            title?.let {
                TPText(
                    text = it,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = fontSize,
                        color = TPTheme.colors.textPrimary,
                    ),
                )
            }
        }
    }
}