package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.intellij.ui.JBColor
import java.awt.Color as AwtColor

@Composable
fun SkillDockTheme(content: @Composable () -> Unit) {
    val isDarkTheme = !JBColor.isBright()

    val colors = if (isDarkTheme) {
        darkColors(
            primary = getJBColor("Component.focusColor", Color(0xFF3592C4)),
            primaryVariant = getJBColor("Component.focusColor", Color(0xFF2B7DA8)),
            secondary = getJBColor("Component.focusColor", Color(0xFF3592C4)),
            background = getJBColor("Panel.background", Color(0xFF2B2B2B)),
            surface = getJBColor("Component.background", Color(0xFF3C3F41)),
            error = getJBColor("Component.errorFocusColor", Color(0xFFE86363)),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = getJBColor("Label.foreground", Color(0xFFBBBBBB)),
            onSurface = getJBColor("Label.foreground", Color(0xFFBBBBBB)),
            onError = Color.White
        )
    } else {
        lightColors(
            primary = getJBColor("Component.focusColor", Color(0xFF4A9FDB)),
            primaryVariant = getJBColor("Component.focusColor", Color(0xFF3592C4)),
            secondary = getJBColor("Component.focusColor", Color(0xFF4A9FDB)),
            background = getJBColor("Panel.background", Color.White),
            surface = getJBColor("Component.background", Color(0xFFF5F5F5)),
            error = getJBColor("Component.errorFocusColor", Color(0xFFE53935)),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = getJBColor("Label.foreground", Color(0xFF000000)),
            onSurface = getJBColor("Label.foreground", Color(0xFF000000)),
            onError = Color.White
        )
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}

private fun getJBColor(name: String, fallback: Color): Color {
    return try {
        val jbColor = JBColor.namedColor(name, JBColor(fallback.toAwtColor(), fallback.toAwtColor()))
        Color(jbColor.rgb)
    } catch (_: Exception) {
        fallback
    }
}

private fun Color.toAwtColor(): AwtColor {
    return AwtColor(
        this.red,
        this.green,
        this.blue,
        this.alpha
    )
}
