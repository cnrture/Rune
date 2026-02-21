package com.github.teknasyon.plugin.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.teknasyon.plugin.common.NoRippleInteractionSource
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun TPButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = TPTheme.colors.white,
        ),
        interactionSource = NoRippleInteractionSource(),
        onClick = onClick,
        content = {
            TPText(
                text = text,
                color = TPTheme.colors.white,
            )
        },
    )
}