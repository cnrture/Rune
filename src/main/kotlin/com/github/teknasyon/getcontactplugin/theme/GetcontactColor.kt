package com.github.teknasyon.getcontactplugin.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun lightColors(
    primary: Color = Color(0xff007aff),
    onPrimary: Color = Color(0xffffffff),
    primaryContainer: Color = Color(0xfff0f7ff),
    onPrimaryContainer: Color = Color(0xff007aff),
    secondaryContainer: Color = Color(0xffebf4ff),
    onSecondaryContainer: Color = Color.White,
    tertiary: Color = Color(0xff313444),
    onTertiary: Color = Color(0xffffffff),
    error: Color = Color(0xffdf384c),
    onError: Color = Color(0xffffffff),
    errorContainer: Color = Color(0xfffff5f5),
    onErrorContainer: Color = Color(0xffdf384c),
    success: Color = Color(0xff21d840),
    onSuccess: Color = Color(0xffffffff),
    successContainer: Color = Color(0xfff3fcf5),
    onSuccessContainer: Color = Color(0xff21d840),
    warning: Color = Color(0xffF3BD33),
    onWarning: Color = Color(0xffffffff),
    warningContainer: Color = Color(0xffFFF3DB),
    onWarningContainer: Color = Color(0xffA67A07),
    background: Color = Color(0xffffffff),
    onBackground: Color = Color(0xff17181c),
    surface: Color = Color(0xffffffff),
    surface1: Color = Color(0xfff8f9fc),
    surface2: Color = Color(0xfff4f6fa),
    surface3: Color = Color(0xffebedf6),
    surface4: Color = Color(0xffe0e2ed),
    surface5: Color = Color(0xffd5d8e7),
    surfaceVariant: Color = Color(0xfff0f2f8),
    onSurface: Color = Color(0xff17171c),
    onSurfaceSoft: Color = Color(0xffb4b4c1),
    onSurfaceVariant: Color = Color(0xff828297),
    inverseSurface: Color = Color(0xcc17171c),
    outline: Color = Color(0xffe7eaf4),
    yellowLight: Color = Color(0xffe4b949),
    yellowMedium: Color = Color(0xffffeabf),
    greenLight: Color = Color(0xff6fc099),
    blueMedium: Color = Color(0xff459eff),
    blueLight: Color = Color(0xffb7d5fb),
): GetcontactColor = GetcontactColor(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    success = success,
    onSuccess = onSuccess,
    successContainer = successContainer,
    onSuccessContainer = onSuccessContainer,
    warning = warning,
    onWarning = onWarning,
    warningContainer = warningContainer,
    onWarningContainer = onWarningContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    surface1 = surface1,
    surface2 = surface2,
    surface3 = surface3,
    surface4 = surface4,
    surface5 = surface5,
    surfaceVariant = surfaceVariant,
    onSurface = onSurface,
    onSurfaceSoft = onSurfaceSoft,
    onSurfaceVariant = onSurfaceVariant,
    inverseSurface = inverseSurface,
    outline = outline,
    yellowLight = yellowLight,
    yellowMedium = yellowMedium,
    greenLight = greenLight,
    blueMedium = blueMedium,
    blueLight = blueLight,
)

class GetcontactColor(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    error: Color,
    onError: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    success: Color,
    onSuccess: Color,
    successContainer: Color,
    onSuccessContainer: Color,
    warning: Color,
    onWarning: Color,
    warningContainer: Color,
    onWarningContainer: Color,
    background: Color,
    onBackground: Color,
    inverseSurface: Color,
    surface: Color,
    surface1: Color,
    surface2: Color,
    surface3: Color,
    surface4: Color,
    surface5: Color,
    surfaceVariant: Color,
    onSurface: Color,
    onSurfaceSoft: Color,
    onSurfaceVariant: Color,
    outline: Color,
    yellowLight: Color,
    yellowMedium: Color,
    greenLight: Color,
    blueMedium: Color,
    blueLight: Color,
) {
    private var _primary: Color by mutableStateOf(primary)
    val primary: Color = _primary

    private var _onPrimary: Color by mutableStateOf(onPrimary)
    val onPrimary: Color = _onPrimary

    private var _primaryContainer: Color by mutableStateOf(primaryContainer)
    val primaryContainer: Color = _primaryContainer

    private var _onPrimaryContainer: Color by mutableStateOf(onPrimaryContainer)
    val onPrimaryContainer: Color = _onPrimaryContainer

    private var _secondaryContainer: Color by mutableStateOf(secondaryContainer)
    val secondaryContainer: Color = _secondaryContainer

    private var _onSecondaryContainer: Color by mutableStateOf(onSecondaryContainer)
    val onSecondaryContainer: Color = _onSecondaryContainer

    private var _tertiary: Color by mutableStateOf(tertiary)
    val tertiary: Color = _tertiary

    private var _onTertiary: Color by mutableStateOf(onTertiary)
    val onTertiary: Color = _onTertiary

    private var _error: Color by mutableStateOf(error)
    val error: Color = _error

    private var _onError: Color by mutableStateOf(onError)
    val onError: Color = _onError

    private var _errorContainer: Color by mutableStateOf(errorContainer)
    val errorContainer: Color = _errorContainer

    private var _onErrorContainer: Color by mutableStateOf(onErrorContainer)
    val onErrorContainer: Color = _onErrorContainer

    private var _success: Color by mutableStateOf(success)
    val success: Color = _success

    private var _onSuccess: Color by mutableStateOf(onSuccess)
    val onSuccess: Color = _onSuccess

    private var _successContainer: Color by mutableStateOf(successContainer)
    val successContainer: Color = _successContainer

    private var _onSuccessContainer: Color by mutableStateOf(onSuccessContainer)
    val onSuccessContainer: Color = _onSuccessContainer

    private var _warning: Color by mutableStateOf(warning)
    val warning: Color = _warning

    private var _onWarning: Color by mutableStateOf(onWarning)
    val onWarning: Color = _onWarning

    private var _warningContainer: Color by mutableStateOf(warningContainer)
    val warningContainer: Color = _warningContainer

    private var _onWarningContainer: Color by mutableStateOf(onWarningContainer)
    val onWarningContainer: Color = _onWarningContainer

    private var _background: Color by mutableStateOf(background)
    val background: Color = _background

    private var _onBackground: Color by mutableStateOf(onBackground)
    val onBackground: Color = _onBackground

    private var _surface: Color by mutableStateOf(surface)
    val surface: Color = _surface

    private var _surface1: Color by mutableStateOf(surface1)
    val surface1: Color = _surface1

    private var _surface2: Color by mutableStateOf(surface2)
    val surface2: Color = _surface2

    private var _surface3: Color by mutableStateOf(surface3)
    val surface3: Color = _surface3

    private var _surface4: Color by mutableStateOf(surface4)
    val surface4: Color = _surface4

    private var _surface5: Color by mutableStateOf(surface5)
    val surface5: Color = _surface5

    private var _surfaceVariant: Color by mutableStateOf(surfaceVariant)
    val surfaceVariant: Color = _surfaceVariant

    private var _onSurface: Color by mutableStateOf(onSurface)
    val onSurface: Color = _onSurface

    private var _inverseSurface: Color by mutableStateOf(inverseSurface)
    val inverseSurface: Color = _inverseSurface

    private var _onSurfaceSoft: Color by mutableStateOf(onSurfaceSoft)
    val onSurfaceSoft: Color = _onSurfaceSoft

    private var _onSurfaceVariant: Color by mutableStateOf(onSurfaceVariant)
    val onSurfaceVariant: Color = _onSurfaceVariant

    private var _outline: Color by mutableStateOf(outline)
    val outline: Color = _outline

    private var _yellowLight: Color by mutableStateOf(yellowLight)
    val yellowLight: Color = _yellowLight

    private var _yellowMedium: Color by mutableStateOf(yellowMedium)
    val yellowMedium: Color = _yellowMedium

    private var _greenLight: Color by mutableStateOf(greenLight)
    val greenLight: Color = _greenLight

    private var _blueMedium: Color by mutableStateOf(blueMedium)
    val blueMedium: Color = _blueMedium

    private var _blueLight: Color by mutableStateOf(blueLight)
    val blueLight: Color = _blueLight
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }