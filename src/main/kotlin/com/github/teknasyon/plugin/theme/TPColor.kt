package com.github.teknasyon.plugin.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun lightColors(
    white: Color = Color(0xffecedee),
    black: Color = Color(0xff000000),
    gray: Color = Color(0xff18181b),
    lightGray: Color = Color(0xffa1a1aa),
    blue: Color = Color(0xff006fee),
    purple: Color = Color(0xff7828C8),
    hintGray: Color = Color(0xFF565656),
): TPColor = TPColor(
    white = white,
    black = black,
    gray = gray,
    lightGray = lightGray,
    blue = blue,
    purple = purple,
    hintGray = hintGray,
)

class TPColor(
    white: Color,
    black: Color,
    gray: Color,
    lightGray: Color,
    blue: Color,
    purple: Color,
    hintGray: Color,
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
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }