package com.example.streaming.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Esquema de colores oscuro personalizado para la app de streaming */
private val StreamingDarkColorScheme =
        darkColorScheme(
                primary = Primary,
                onPrimary = TextPrimary,
                primaryContainer = PrimaryDark,
                onPrimaryContainer = TextPrimary,
                secondary = Accent,
                onSecondary = TextPrimary,
                secondaryContainer = AccentLight,
                onSecondaryContainer = SurfacePrimary,
                tertiary = PrimaryLight,
                onTertiary = TextPrimary,
                background = SurfacePrimary,
                onBackground = TextPrimary,
                surface = SurfaceSecondary,
                onSurface = TextPrimary,
                surfaceVariant = SurfaceTertiary,
                onSurfaceVariant = TextSecondary,
                error = Error,
                onError = TextPrimary,
                outline = BorderColor,
                outlineVariant = BorderGlow
        )

/**
 * Tema de la aplicación de streaming Siempre usa modo oscuro para mejor experiencia visual durante
 * streaming
 */
@Composable
fun StreamingTheme(content: @Composable () -> Unit) {
    val colorScheme = StreamingDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfacePrimary.toArgb()
            window.navigationBarColor = SurfaceTertiary.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
