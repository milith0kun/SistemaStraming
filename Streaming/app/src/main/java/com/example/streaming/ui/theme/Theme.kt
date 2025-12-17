package com.example.streaming.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * ============================================================================
 * THEME - Sistema de Temas con Modo Oscuro/Claro
 * ============================================================================
 *
 * Soporta:
 * - Modo oscuro (por defecto)
 * - Modo claro
 * - Transiciones animadas entre temas
 *
 * ============================================================================
 */

// Colores para modo claro
private val LightSurfacePrimary = Color(0xFFF8FAFC)
private val LightSurfaceSecondary = Color(0xFFFFFFFF)
private val LightSurfaceTertiary = Color(0xFFF1F5F9)
private val LightTextPrimary = Color(0xFF0F172A)
private val LightTextSecondary = Color(0xFF64748B)
private val LightBorderColor = Color(0x1A000000)

/** Esquema de colores oscuro personalizado para la app de streaming */
private val StreamingDarkColorScheme = darkColorScheme(
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

/** Esquema de colores claro para la app de streaming */
private val StreamingLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = AccentLight,
    onSecondaryContainer = LightTextPrimary,
    tertiary = PrimaryDark,
    onTertiary = Color.White,
    background = LightSurfacePrimary,
    onBackground = LightTextPrimary,
    surface = LightSurfaceSecondary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceTertiary,
    onSurfaceVariant = LightTextSecondary,
    error = Error,
    onError = Color.White,
    outline = LightBorderColor,
    outlineVariant = Primary.copy(alpha = 0.3f)
)

/**
 * Tema de la aplicación de streaming con soporte para modo oscuro/claro
 *
 * @param darkTheme Si es true, usa el tema oscuro. Por defecto es true.
 * @param content Contenido composable
 */
@Composable
fun StreamingTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StreamingDarkColorScheme else StreamingLightColorScheme
    val view = LocalView.current

    // Colores animados para transición suave
    val animatedBackground = animateColorAsState(
        targetValue = colorScheme.background,
        animationSpec = tween(durationMillis = 300),
        label = "background"
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) SurfacePrimary.toArgb() else LightSurfacePrimary.toArgb()
            window.navigationBarColor = if (darkTheme) SurfaceTertiary.toArgb() else LightSurfaceTertiary.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Objeto con colores del tema para acceso directo
 */
object StreamingColors {
    // Gradientes
    val GradientStart = Primary
    val GradientEnd = Accent
    
    // Estados
    val Success = com.example.streaming.ui.theme.Success
    val Warning = com.example.streaming.ui.theme.Warning
    val Error = com.example.streaming.ui.theme.Error
    val Live = com.example.streaming.ui.theme.Live
    
    // Superficies oscuras
    val DarkSurface = SurfacePrimary
    val DarkSurfaceSecondary = SurfaceSecondary
    val DarkCard = SurfaceTertiary
    
    // Superficies claras
    val LightSurface = LightSurfacePrimary
    val LightSurfaceSecondary = Color(0xFFFFFFFF)
    val LightCard = LightSurfaceTertiary
    
    // Función para obtener color de superficie según tema
    fun getSurface(isDark: Boolean) = if (isDark) DarkSurface else LightSurface
    fun getCard(isDark: Boolean) = if (isDark) DarkCard else LightCard
    fun getTextPrimary(isDark: Boolean) = if (isDark) TextPrimary else LightTextPrimary
    fun getTextSecondary(isDark: Boolean) = if (isDark) TextSecondary else LightTextSecondary
}
