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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.theme.RTheme

enum class RActionCardType { EXTRA_SMALL, SMALL, MEDIUM, LARGE }

@Composable
fun RActionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    actionColor: Color,
    isTextVisible: Boolean = true,
    type: RActionCardType = RActionCardType.LARGE,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fontSize = when (type) {
        RActionCardType.EXTRA_SMALL -> 12.sp
        RActionCardType.SMALL -> 14.sp
        RActionCardType.MEDIUM -> 16.sp
        RActionCardType.LARGE -> 20.sp
    }
    val iconBoxSize = when (type) {
        RActionCardType.EXTRA_SMALL -> 20.dp
        RActionCardType.SMALL -> 24.dp
        RActionCardType.MEDIUM -> 28.dp
        RActionCardType.LARGE -> 32.dp
    }
    val iconSize = when (type) {
        RActionCardType.EXTRA_SMALL -> 14.dp
        RActionCardType.SMALL -> 16.dp
        RActionCardType.MEDIUM -> 20.dp
        RActionCardType.LARGE -> 24.dp
    }
    val borderSize = when (type) {
        RActionCardType.EXTRA_SMALL -> 1.dp
        RActionCardType.SMALL -> 2.dp
        RActionCardType.MEDIUM -> 2.dp
        RActionCardType.LARGE -> 3.dp
    }
    val padding = when (type) {
        RActionCardType.EXTRA_SMALL -> 6.dp
        RActionCardType.SMALL -> 8.dp
        RActionCardType.MEDIUM -> 12.dp
        RActionCardType.LARGE -> 16.dp
    }
    Row(
        modifier = modifier
            .background(
                color = if (isEnabled) RTheme.colors.gray else RTheme.colors.lightGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = borderSize,
                color = if (isEnabled) actionColor else RTheme.colors.outline.copy(alpha = 0.3f),
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
            if (type == RActionCardType.LARGE) {
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
                        tint = RTheme.colors.white,
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
            Spacer(modifier = Modifier.size(4.dp))
        }

        if (isTextVisible) {
            title?.let {
                RText(
                    text = it,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = fontSize,
                        color = RTheme.colors.white,
                    ),
                )
            }
        }
    }
}