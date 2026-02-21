package com.github.teknasyon.plugin.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object TPTheme {
    val colors: TPColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun TPTheme(content: @Composable () -> Unit) {
    TPTheme(
        colors = lightColors(),
        content = content,
    )
}

@Composable
private fun TPTheme(
    colors: TPColor = TPTheme.colors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
    ) {
        content()
    }
}