package com.pwdgen.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pwdgen.app.data.SettingsRepository
import com.pwdgen.app.ui.AppNav
import com.pwdgen.app.ui.MainViewModel
import com.pwdgen.app.ui.components.WallpaperBackground
import com.pwdgen.app.ui.theme.PwdGenTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.ui.collectAsState()
            ApplyAppLanguage(state.language)

            PwdGenTheme(
                themeMode = state.themeMode,
                dynamicColor = state.dynamicColor,
            ) {
                WallpaperBackground(
                    style = state.wallpaperStyle,
                    solidColor = state.wallpaperColor,
                    gradientIndex = state.wallpaperGradientIndex,
                    imageUri = state.wallpaperImageUri,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNav(state = state, vm = vm)
                }
            }
        }
    }
}

/**
 * Applies the in-app language choice without restarting the activity by wrapping
 * the base context with the requested locale. "system" clears the override.
 */
@Composable
private fun ApplyAppLanguage(language: String) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val target = when (language) {
        SettingsRepository.LANG_EN -> Locale.ENGLISH
        SettingsRepository.LANG_ZH -> Locale.SIMPLIFIED_CHINESE
        else -> null
    }

    val current = configuration.locales.get(0)
    if (target != null && current.language != target.language) {
        val updated = Configuration(configuration)
        updated.setLocale(target)
        context.resources.updateConfiguration(updated, context.resources.displayMetrics)
    } else if (target == null) {
        // Follow system: reset to the device default locale.
        val default = Locale.getDefault()
        if (current.language != default.language) {
            val updated = Configuration(configuration)
            updated.setLocale(default)
            context.resources.updateConfiguration(updated, context.resources.displayMetrics)
        }
    }
}