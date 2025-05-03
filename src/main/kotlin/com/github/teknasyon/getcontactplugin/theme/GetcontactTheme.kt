package com.github.teknasyon.getcontactplugin.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object GetcontactTheme {
    val colors: GetcontactColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun GetcontactTheme(content: @Composable () -> Unit) {
    GetcontactTheme(
        colors = lightColors(),
        content = content,
    )
}

@Composable
private fun GetcontactTheme(
    colors: GetcontactColor = GetcontactTheme.colors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
    ) {
        content()
    }
}