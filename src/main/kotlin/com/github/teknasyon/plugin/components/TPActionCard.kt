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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.theme.TPTheme

enum class TPActionCardType { SMALL, MEDIUM, LARGE }

@Composable
fun TPActionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    actionColor: Color,
    isTextVisible: Boolean = true,
    type: TPActionCardType = TPActionCardType.LARGE,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fontSize = when (type) {
        TPActionCardType.SMALL -> 14.sp
        TPActionCardType.MEDIUM -> 16.sp
        TPActionCardType.LARGE -> 20.sp
    }
    val iconBoxSize = when (type) {
        TPActionCardType.SMALL -> 24.dp
        TPActionCardType.MEDIUM -> 28.dp
        TPActionCardType.LARGE -> 32.dp
    }
    val iconSize = when (type) {
        TPActionCardType.SMALL -> 16.dp
        TPActionCardType.MEDIUM -> 20.dp
        TPActionCardType.LARGE -> 24.dp
    }
    val borderSize = when (type) {
        TPActionCardType.SMALL -> 1.dp
        TPActionCardType.MEDIUM -> 2.dp
        TPActionCardType.LARGE -> 3.dp
    }
    val padding = when (type) {
        TPActionCardType.SMALL -> 8.dp
        TPActionCardType.MEDIUM -> 12.dp
        TPActionCardType.LARGE -> 16.dp
    }
    Row(
        modifier = modifier
            .background(
                color = if (isEnabled) TPTheme.colors.gray else TPTheme.colors.lightGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = borderSize,
                color = if (isEnabled) actionColor else TPTheme.colors.lightGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = TPTheme.colors.white,
                        modifier = Modifier.size(iconSize)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (icon != null && title != null && isTextVisible) {
            Spacer(modifier = Modifier.width(8.dp))
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