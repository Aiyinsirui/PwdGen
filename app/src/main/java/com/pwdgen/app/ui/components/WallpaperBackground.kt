package com.pwdgen.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.pwdgen.app.data.SettingsRepository
import com.pwdgen.app.ui.theme.Gradients

/**
 * Renders the user-selected wallpaper behind the whole app.
 *  - solid   : flat color
 *  - gradient: preset linear gradient
 *  - image   : photo picked from the gallery (persisted as a content:// URI)
 */
@Composable
fun WallpaperBackground(
    style: String,
    solidColor: Long,
    gradientIndex: Int,
    imageUri: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (style) {
            SettingsRepository.WALL_GRADIENT -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Gradients.byIndex(gradientIndex).brush())
                )
            }
            SettingsRepository.WALL_IMAGE -> {
                if (!imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Slight scrim so foreground cards stay readable.
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                } else {
                    Box(Modifier.fillMaxSize().background(Color(solidColor)))
                }
            }
            else -> {
                Box(Modifier.fillMaxSize().background(Color(solidColor)))
            }
        }
        content()
    }
}