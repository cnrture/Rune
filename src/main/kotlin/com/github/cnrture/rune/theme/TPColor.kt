package com.github.cnrture.rune.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun lightColors(
    background: Color = Color(0xFF1C1B1F),
    surface: Color = Color(0xFF2D2B30),
    textPrimary: Color = Color(0xFFE6E1E5),
    textSecondary: Color = Color(0xFF938F99),
    accent: Color = Color(0xFFAE8FDB),
    accentContainer: Color = Color(0xFF3A2566),
    outline: Color = Color(0xFF49454F),
    error: Color = Color(0xFFFF8A80),
    success: Color = Color(0xFF69F0AE),
    warning: Color = Color(0xFFFFD54F),
): TPColor = TPColor(
    background = background,
    surface = surface,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    accent = accent,
    accentContainer = accentContainer,
    outline = outline,
    error = error,
    success = success,
    warning = warning,
)

class TPColor(
    background: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    accentContainer: Color,
    outline: Color,
    error: Color,
    success: Color,
    warning: Color,
) {
    private var _background: Color by mutableStateOf(background)
    val background: Color = _background

    private var _surface: Color by mutableStateOf(surface)
    val surface: Color = _surface

    private var _textPrimary: Color by mutableStateOf(textPrimary)
    val textPrimary: Color = _textPrimary

    private var _textSecondary: Color by mutableStateOf(textSecondary)
    val textSecondary: Color = _textSecondary

    private var _accent: Color by mutableStateOf(accent)
    val accent: Color = _accent

    private var _accentContainer: Color by mutableStateOf(accentContainer)
    val accentContainer: Color = _accentContainer

    private var _outline: Color by mutableStateOf(outline)
    val outline: Color = _outline

    private var _error: Color by mutableStateOf(error)
    val error: Color = _error

    private var _success: Color by mutableStateOf(success)
    val success: Color = _success

    private var _warning: Color by mutableStateOf(warning)
    val warning: Color = _warning
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }
