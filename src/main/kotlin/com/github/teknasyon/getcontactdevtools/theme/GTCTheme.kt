package com.github.teknasyon.getcontactdevtools.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object GTCTheme {
    val colors: GTCColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun GTCTheme(content: @Composable () -> Unit) {
    GTCTheme(
        colors = lightColors(),
        content = content,
    )
}

@Composable
private fun GTCTheme(
    colors: GTCColor = GTCTheme.colors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
    ) {
        content()
    }
}