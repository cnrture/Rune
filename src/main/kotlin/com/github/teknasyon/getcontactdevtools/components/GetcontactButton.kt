package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@Composable
fun GetcontactButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = GetcontactTheme.colors.white,
        ),
        onClick = onClick,
        content = {
            Text(
                text = text,
                color = GetcontactTheme.colors.white,
                fontSize = 16.sp,
            )
        },
    )
}