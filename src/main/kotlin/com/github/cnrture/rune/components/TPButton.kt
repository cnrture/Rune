package com.github.cnrture.rune.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.cnrture.rune.common.NoRippleInteractionSource
import com.github.cnrture.rune.theme.TPTheme

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