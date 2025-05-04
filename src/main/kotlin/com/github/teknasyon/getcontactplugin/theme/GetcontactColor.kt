package com.github.teknasyon.getcontactplugin.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun lightColors(
    white: Color = Color(0xffecedee),
    black: Color = Color(0xff000000),
    lightBlack: Color = Color(0xff121214),
    gray: Color = Color(0xff18181b),
    lightGray: Color = Color(0xffa1a1aa),
    blue: Color = Color(0xff006fee),
    purple: Color = Color(0xff7829c8),
    orange: Color = Color(0xfff5a524),
): GetcontactColor = GetcontactColor(
    white = white,
    black = black,
    lightBlack = lightBlack,
    gray = gray,
    lightGray = lightGray,
    blue = blue,
    purple = purple,
    orange = orange,
)

class GetcontactColor(
    white: Color,
    black: Color,
    lightBlack: Color,
    gray: Color,
    lightGray: Color,
    blue: Color,
    purple: Color,
    orange: Color,
) {
    private var _white: Color by mutableStateOf(white)
    val white: Color = _white

    private var _black: Color by mutableStateOf(black)
    val black: Color = _black

    private var _lightBlack: Color by mutableStateOf(lightBlack)
    val lightBlack: Color = _lightBlack

    private var _gray: Color by mutableStateOf(gray)
    val gray: Color = _gray

    private var _lightGray: Color by mutableStateOf(lightGray)
    val lightGray: Color = _lightGray

    private var _blue: Color by mutableStateOf(blue)
    val blue: Color = _blue

    private var _purple: Color by mutableStateOf(purple)
    val purple: Color = _purple

    private var _orange: Color by mutableStateOf(orange)
    val orange: Color = _orange
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }