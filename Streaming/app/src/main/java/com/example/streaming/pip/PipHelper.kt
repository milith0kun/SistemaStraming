package com.example.streaming.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Rational

/**
 * ============================================================================
 * PICTURE IN PICTURE HELPER
 * ============================================================================
 *
 * Helper para gestionar el modo Picture-in-Picture en la app de streaming.
 * Permite mantener la transmisión visible mientras se usan otras apps.
 *
 * ============================================================================
 */
object PipHelper {

    private const val TAG = "PipHelper"

    /**
     * Verifica si el dispositivo soporta PiP
     */
    fun isPipSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }
    }

    /**
     * Entra en modo PiP
     */
    fun enterPipMode(
        activity: Activity,
        aspectRatioWidth: Int = 16,
        aspectRatioHeight: Int = 9,
        autoEnterEnabled: Boolean = true
    ): Boolean {
        if (!isPipSupported(activity)) {
            Log.w(TAG, "PiP no soportado en este dispositivo")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(aspectRatioWidth, aspectRatioHeight))
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.setAutoEnterEnabled(autoEnterEnabled)
                    params.setSeamlessResizeEnabled(true)
                }

                activity.enterPictureInPictureMode(params.build())
                Log.d(TAG, "✅ Entró en modo PiP")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al entrar en modo PiP: ${e.message}")
            false
        }
    }

    /**
     * Actualiza los parámetros de PiP (para cambiar aspect ratio durante streaming)
     */
    fun updatePipParams(
        activity: Activity,
        aspectRatioWidth: Int = 16,
        aspectRatioHeight: Int = 9
    ) {
        if (!isPipSupported(activity)) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(aspectRatioWidth, aspectRatioHeight))
                    .build()
                
                activity.setPictureInPictureParams(params)
                Log.d(TAG, "📐 Parámetros de PiP actualizados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando PiP: ${e.message}")
        }
    }

    /**
     * Configura auto-enter PiP cuando se minimiza la app mientras transmite
     */
    fun setAutoEnterPip(
        activity: Activity,
        enabled: Boolean,
        aspectRatioWidth: Int = 16,
        aspectRatioHeight: Int = 9
    ) {
        if (!isPipSupported(activity)) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(aspectRatioWidth, aspectRatioHeight))
                    .setAutoEnterEnabled(enabled)
                    .build()
                
                activity.setPictureInPictureParams(params)
                Log.d(TAG, "🔄 Auto-enter PiP: $enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando auto-enter PiP: ${e.message}")
        }
    }

    /**
     * Verifica si la actividad está actualmente en modo PiP
     */
    fun isInPipMode(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
    }
}

/**
 * Información del estado de PiP
 */
data class PipState(
    val isSupported: Boolean = false,
    val isInPipMode: Boolean = false,
    val autoEnterEnabled: Boolean = true
)
