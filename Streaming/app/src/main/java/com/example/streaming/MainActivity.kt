package com.example.streaming

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.streaming.pip.PipHelper
import com.example.streaming.settings.ThemeManager
import com.example.streaming.ui.screens.StreamingScreen
import com.example.streaming.ui.theme.StreamingTheme

/**
 * ============================================================================ MAIN ACTIVITY -
 * STREAMING APP ============================================================================
 *
 * Actividad principal de la aplicación de streaming RTMP.
 *
 * Características:
 * - Soporte para Picture-in-Picture
 * - Tema dinámico (oscuro/claro) con persistencia
 * - Modo inmersivo edge-to-edge
 * - Mantiene pantalla encendida durante streaming
 *
 * ============================================================================
 */
class MainActivity : ComponentActivity() {

    private var isInPipMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantener pantalla encendida durante el uso de la app
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configurar modo inmersivo (edge-to-edge)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Configurar barra de estado transparente con iconos claros
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Configurar PiP si es soportado
        if (PipHelper.isPipSupported(this)) {
            PipHelper.setAutoEnterPip(this, true)
        }

        setContent {
            // Obtener el ThemeManager para el tema dinámico
            val themeManager = remember { ThemeManager(this) }
            val isDarkMode by themeManager.isDarkMode.collectAsState(initial = true)

            StreamingTheme(darkTheme = isDarkMode) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isDarkMode) Color(0xFF1A1A2E) else Color(0xFFF8FAFC)
                ) { StreamingScreen() }
            }
        }
    }

    /** Maneja el evento de entrar/salir de Picture-in-Picture */
    override fun onPictureInPictureModeChanged(
            isInPictureInPictureMode: Boolean,
            newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            // Ocultar UI innecesaria en modo PiP
            // La UI se adapta automáticamente con Composable AnimatedVisibility
        } else {
            // Restaurar UI cuando sale de PiP
        }
    }

    /** Cuando el usuario minimiza la app mientras transmite, entra en modo PiP */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        // Auto-entrar en PiP si está transmitiendo
        // Esto se maneja automáticamente si setAutoEnterPip está habilitado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PipHelper.isPipSupported(this)) {
            // El sistema manejará esto automáticamente si autoEnterEnabled está activo
        }
    }

    override fun onResume() {
        super.onResume()
        // Si volvemos de PiP, actualizar la UI
        if (isInPipMode && !PipHelper.isInPipMode(this)) {
            isInPipMode = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar flag de pantalla encendida
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
