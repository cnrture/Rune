package com.github.teknasyon.plugin.components

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
import com.github.teknasyon.plugin.theme.TPTheme

enum class TPActionCardType { EXTRA_SMALL, SMALL, MEDIUM, LARGE }

@Composable
fun TPActionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: Painter? = null,
    actionColor: Color,
    isTextVisible: Boolean = true,
    type: TPActionCardType = TPActionCardType.LARGE,
    isEnabled: Boolean = true,
    isBorderless: Boolean = false,
    onClick: () -> Unit,
) {
    val fontSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 12.sp
        TPActionCardType.SMALL -> 14.sp
        TPActionCardType.MEDIUM -> 16.sp
        TPActionCardType.LARGE -> 20.sp
    }
    val iconBoxSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 20.dp
        TPActionCardType.SMALL -> 24.dp
        TPActionCardType.MEDIUM -> 28.dp
        TPActionCardType.LARGE -> 32.dp
    }
    val iconSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 14.dp
        TPActionCardType.SMALL -> 16.dp
        TPActionCardType.MEDIUM -> 20.dp
        TPActionCardType.LARGE -> 24.dp
    }
    val borderSize = when (type) {
        TPActionCardType.EXTRA_SMALL -> 1.dp
        TPActionCardType.SMALL -> 2.dp
        TPActionCardType.MEDIUM -> 2.dp
        TPActionCardType.LARGE -> 3.dp
    }
    val padding = when (type) {
        TPActionCardType.EXTRA_SMALL -> 6.dp
        TPActionCardType.SMALL -> 8.dp
        TPActionCardType.MEDIUM -> 12.dp
        TPActionCardType.LARGE -> 16.dp
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isBorderless) {
                    if (isEnabled) actionColor.copy(alpha = 0.12f) else TPTheme.colors.lightGray.copy(alpha = 0.05f)
                } else {
                    if (isEnabled) TPTheme.colors.gray else TPTheme.colors.lightGray.copy(alpha = 0.1f)
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
            if (type == TPActionCardType.LARGE) {
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = TPTheme.colors.white,
                        modifier = Modifier.size(iconSize)
                    )
                }
            } else {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(iconSize)
                )
            }
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
                        color = TPTheme.colors.white,
                    ),
                )
            }
        }
    }
}