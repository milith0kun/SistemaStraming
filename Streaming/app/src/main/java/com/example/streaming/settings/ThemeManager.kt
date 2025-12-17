package com.example.streaming.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ============================================================================
 * THEME MANAGER - Gestión de Tema Oscuro/Claro
 * ============================================================================
 *
 * Maneja la persistencia de preferencias de tema usando DataStore.
 * Permite cambiar entre modo oscuro y claro con persistencia.
 *
 * ============================================================================
 */

// Extension para crear DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {

    companion object {
        // Keys para preferencias
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val STREAM_KEY = stringPreferencesKey("stream_key")
        private val RTMP_URL_KEY = stringPreferencesKey("rtmp_url")
        private val QUALITY_KEY = stringPreferencesKey("quality")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val PIP_ENABLED_KEY = booleanPreferencesKey("pip_enabled")
        private val THEATER_MODE_KEY = booleanPreferencesKey("theater_mode")
        
        // Valores por defecto
        private const val DEFAULT_RTMP_URL = "rtmp://streamingpe.myvnc.com:1935/live"
        private const val DEFAULT_STREAM_KEY = "stream"
        private const val DEFAULT_QUALITY = "HIGH"
        private const val DEFAULT_USERNAME = "Android User"
    }

    /**
     * Flow para observar el modo oscuro
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: true // Por defecto modo oscuro
    }

    /**
     * Flow para observar el nombre de usuario
     */
    val username: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: DEFAULT_USERNAME
    }

    /**
     * Flow para observar la stream key guardada
     */
    val streamKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[STREAM_KEY] ?: DEFAULT_STREAM_KEY
    }

    /**
     * Flow para observar la URL RTMP guardada
     */
    val rtmpUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[RTMP_URL_KEY] ?: DEFAULT_RTMP_URL
    }

    /**
     * Flow para observar la calidad seleccionada
     */
    val quality: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[QUALITY_KEY] ?: DEFAULT_QUALITY
    }

    /**
     * Flow para observar si las notificaciones están habilitadas
     */
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    /**
     * Flow para observar si PiP está habilitado
     */
    val pipEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PIP_ENABLED_KEY] ?: true
    }

    /**
     * Flow para observar el modo teatro
     */
    val theaterMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[THEATER_MODE_KEY] ?: false
    }

    /**
     * Cambiar modo oscuro
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    /**
     * Toggle modo oscuro
     */
    suspend fun toggleDarkMode() {
        context.dataStore.edit { preferences ->
            val current = preferences[DARK_MODE_KEY] ?: true
            preferences[DARK_MODE_KEY] = !current
        }
    }

    /**
     * Guardar nombre de usuario
     */
    suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username.ifBlank { DEFAULT_USERNAME }
        }
    }

    /**
     * Guardar stream key
     */
    suspend fun setStreamKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[STREAM_KEY] = key.ifBlank { DEFAULT_STREAM_KEY }
        }
    }

    /**
     * Guardar URL RTMP
     */
    suspend fun setRtmpUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[RTMP_URL_KEY] = url.ifBlank { DEFAULT_RTMP_URL }
        }
    }

    /**
     * Guardar calidad
     */
    suspend fun setQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[QUALITY_KEY] = quality
        }
    }

    /**
     * Habilitar/deshabilitar notificaciones
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    /**
     * Habilitar/deshabilitar PiP
     */
    suspend fun setPipEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PIP_ENABLED_KEY] = enabled
        }
    }

    /**
     * Toggle modo teatro
     */
    suspend fun toggleTheaterMode() {
        context.dataStore.edit { preferences ->
            val current = preferences[THEATER_MODE_KEY] ?: false
            preferences[THEATER_MODE_KEY] = !current
        }
    }

    /**
     * Establecer modo teatro
     */
    suspend fun setTheaterMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEATER_MODE_KEY] = enabled
        }
    }
}

/**
 * Tema de la aplicación
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}
