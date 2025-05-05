package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@Composable
fun GetcontactButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = GetcontactTheme.colors.white,
        ),
        onClick = onClick,
        content = {
            GetcontactText(
                text = text,
                color = GetcontactTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                ),
            )
        },
    )
}

@Composable
fun GetcontactOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = backgroundColor.copy(alpha = 0.1f),
            contentColor = GetcontactTheme.colors.white,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = backgroundColor,
        ),
        onClick = onClick,
        content = {
            GetcontactText(
                text = text,
                color = GetcontactTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                ),
            )
        },
    )
}