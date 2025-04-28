package com.github.teknasyon.getcontactplugin.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkGreenColorPalette = darkColors(
    primary = Color(0xff007aff),
    primaryVariant = Color(0xff0051a2),
)

@Composable
fun WidgetTheme(content: @Composable () -> Unit) {
    val swingColor = SwingColor()

    MaterialTheme(
        colors = DarkGreenColorPalette.copy(
            background = swingColor.background,
            onBackground = swingColor.onBackground,
            surface = swingColor.background,
            onSurface = swingColor.onBackground
        ),
        typography = typography,
        shapes = shapes,
    ) {
        Surface {
            content()
        }
    }
}