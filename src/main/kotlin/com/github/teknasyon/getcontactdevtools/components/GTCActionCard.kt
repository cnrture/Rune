package com.github.teknasyon.getcontactdevtools.components

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
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme

enum class GTCActionCardType { SMALL, MEDIUM, LARGE }

@Composable
fun GTCActionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    actionColor: Color,
    isTextVisible: Boolean = true,
    type: GTCActionCardType = GTCActionCardType.LARGE,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fontSize = when (type) {
        GTCActionCardType.SMALL -> 14.sp
        GTCActionCardType.MEDIUM -> 16.sp
        GTCActionCardType.LARGE -> 20.sp
    }
    val iconBoxSize = when (type) {
        GTCActionCardType.SMALL -> 24.dp
        GTCActionCardType.MEDIUM -> 28.dp
        GTCActionCardType.LARGE -> 32.dp
    }
    val iconSize = when (type) {
        GTCActionCardType.SMALL -> 16.dp
        GTCActionCardType.MEDIUM -> 20.dp
        GTCActionCardType.LARGE -> 24.dp
    }
    val borderSize = when (type) {
        GTCActionCardType.SMALL -> 1.dp
        GTCActionCardType.MEDIUM -> 2.dp
        GTCActionCardType.LARGE -> 3.dp
    }
    val padding = when (type) {
        GTCActionCardType.SMALL -> 8.dp
        GTCActionCardType.MEDIUM -> 12.dp
        GTCActionCardType.LARGE -> 16.dp
    }
    Row(
        modifier = modifier
            .background(
                color = if (isEnabled) GTCTheme.colors.gray else GTCTheme.colors.lightGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = borderSize,
                color = if (isEnabled) actionColor else GTCTheme.colors.lightGray.copy(alpha = 0.1f),
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
            if (type == GTCActionCardType.LARGE) {
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
                        tint = GTCTheme.colors.white,
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
                GTCText(
                    text = it,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = fontSize,
                        color = GTCTheme.colors.white,
                    ),
                )
            }
        }
    }
}