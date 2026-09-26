package com.pwdgen.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A named preset gradient used as an app background / accent source.
 * Mirrors the "preset gradient" feature found in Via-like browsers.
 */
data class GradientPreset(
    val id: String,
    val colors: List<Long>
) {
    fun brush(): Brush = Brush.linearGradient(colors.map { Color(it) })
    fun start(): Color = Color(colors.first())
    fun end(): Color = Color(colors.last())
}

object Gradients {
    val presets: List<GradientPreset> = listOf(
        GradientPreset("aurora", listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364)),
        GradientPreset("sunset", listOf(0xFFFF7E5F, 0xFFFEB47B)),
        GradientPreset("ocean", listOf(0xFF2193B0, 0xFF6DD5ED)),
        GradientPreset("purple", listOf(0xFF662D8C, 0xFFED1E79)),
        GradientPreset("mint", listOf(0xFF11998E, 0xFF38EF7D)),
        GradientPreset("night", listOf(0xFF232526, 0xFF414345)),
        GradientPreset("peach", listOf(0xFFFFB88C, 0xFFFC6076)),
        GradientPreset("indigo", listOf(0xFF3F5EFB, 0xFFFC466B)),
        GradientPreset("forest", listOf(0xFF134E5E, 0xFF71B280)),
        GradientPreset("candy", listOf(0xFFD9A7C7, 0xFFFFFCDC)),
    )

    fun byIndex(i: Int): GradientPreset = presets[i.coerceIn(0, presets.size - 1)]

    /** Solid presets for the color picker. */
    val solidPresets: List<Long> = listOf(
        0xFF101623, 0xFF1B1B1B, 0xFF232946, 0xFF2E3440, 0xFF3A3A3A,
        0xFFF5F5F5, 0xFFFFFFFF, 0xFFEFE9E3, 0xFFFDF6E3, 0xFFE8F0FE,
        0xFF0B3D2E, 0xFF4A148C, 0xFFB71C1C, 0xFF01579B, 0xFF33691E,
    )
}