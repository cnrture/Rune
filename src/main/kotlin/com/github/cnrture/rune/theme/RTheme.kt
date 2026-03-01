package com.github.cnrture.rune.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object RTheme {
    val colors: RColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun RTheme(content: @Composable () -> Unit) {
    RTheme(
        colors = lightColors(),
        content = content,
    )
}

@Composable
private fun RTheme(
    colors: RColor = RTheme.colors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
    ) {
        content()
    }
}