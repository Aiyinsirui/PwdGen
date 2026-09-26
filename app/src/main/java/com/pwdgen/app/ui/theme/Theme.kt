package com.pwdgen.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pwdgen.app.data.SettingsRepository

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    onPrimary = Color.White,
    secondary = Color(0xFF2C5364),
    background = Color(0xFFF7F9FB),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FCF97),
    onPrimary = Color(0xFF06231B),
    secondary = Color(0xFF6DD5ED),
    background = Color(0xFF101623),
    surface = Color(0xFF161D2B),
)

/**
 * App theme. When a gradient/photo wallpaper is active the caller renders it
 * behind a transparent Scaffold, so `background` stays translucent-friendly.
 */
@Composable
fun PwdGenTheme(
    themeMode: String,
    dynamicColor: Boolean,
    sharpAccent: Color? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        SettingsRepository.THEME_LIGHT -> false
        SettingsRepository.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        sharpAccent != null -> {
            val base = if (dark) DarkColors else LightColors
            base.copy(primary = sharpAccent, onPrimary = Color.White)
        }
        dark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = scheme, content = content)
}