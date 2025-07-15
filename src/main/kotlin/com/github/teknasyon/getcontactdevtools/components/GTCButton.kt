package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.teknasyon.getcontactdevtools.common.NoRippleInteractionSource
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme

@Composable
fun GTCButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = GTCTheme.colors.white,
        ),
        interactionSource = NoRippleInteractionSource(),
        onClick = onClick,
        content = {
            GTCText(
                text = text,
                color = GTCTheme.colors.white,
            )
        },
    )
}