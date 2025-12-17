package com.example.streaming

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.streaming.ui.screens.StreamingScreen
import com.example.streaming.ui.theme.StreamingTheme

/**
 * ============================================================================
 * MAIN ACTIVITY - STREAMING APP
 * ============================================================================
 * 
 * Actividad principal de la aplicación de streaming RTMP.
 * Configura la UI con tema oscuro inmersivo y mantiene la pantalla encendida.
 * 
 * ============================================================================
 */
class MainActivity : ComponentActivity() {
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
        
        setContent {
            StreamingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A1A2E)
                ) {
                    StreamingScreen()
                }
            }
        }
    }
}