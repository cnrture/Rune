package com.github.cnrture.rune.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density

object AppIcons {
    @Suppress("DEPRECATION")
    @Composable
    fun painter(name: String): Painter {
        val density = Density(1f)
        return remember(name) {
            val stream = AppIcons::class.java.classLoader.getResourceAsStream("icons/$name.svg")
                ?: AppIcons::class.java.classLoader.getResourceAsStream("icons/help.svg")
                ?: error("Fallback icon not found: icons/help.svg")
            stream.use { loadSvgPainter(it, density) }
        }
    }
}
