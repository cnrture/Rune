package com.github.teknasyon.plugin.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun lightColors(
    white: Color = Color(0xFFE6E1E5),
    black: Color = Color(0xFF1C1B1F),
    gray: Color = Color(0xFF2D2B30),
    lightGray: Color = Color(0xFFCAC4D0),
    blue: Color = Color(0xFF82B1FF),
    purple: Color = Color(0xFFB388FF),
    hintGray: Color = Color(0xFF938F99),
    red: Color = Color(0xFFFF8A80),
    primaryContainer: Color = Color(0xFF1A2857),
    outline: Color = Color(0xFF49454F),
): TPColor = TPColor(
    white = white,
    black = black,
    gray = gray,
    lightGray = lightGray,
    blue = blue,
    purple = purple,
    hintGray = hintGray,
    red = red,
    primaryContainer = primaryContainer,
    outline = outline,
)

class TPColor(
    white: Color,
    black: Color,
    gray: Color,
    lightGray: Color,
    blue: Color,
    purple: Color,
    hintGray: Color,
    red: Color,
    primaryContainer: Color,
    outline: Color,
) {
    private var _white: Color by mutableStateOf(white)
    val white: Color = _white

    private var _black: Color by mutableStateOf(black)
    val black: Color = _black

    private var _gray: Color by mutableStateOf(gray)
    val gray: Color = _gray

    private var _lightGray: Color by mutableStateOf(lightGray)
    val lightGray: Color = _lightGray

    private var _blue: Color by mutableStateOf(blue)
    val blue: Color = _blue

    private var _purple: Color by mutableStateOf(purple)
    val purple: Color = _purple

    private var _hintGray: Color by mutableStateOf(hintGray)
    val hintGray: Color = _hintGray

    private var _red: Color by mutableStateOf(red)
    val red: Color = _red

    private var _primaryContainer: Color by mutableStateOf(primaryContainer)
    val primaryContainer: Color = _primaryContainer

    private var _outline: Color by mutableStateOf(outline)
    val outline: Color = _outline
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }
