package com.example.streaming.streaming

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================================ STREAM MANAGER
 * ============================================================================
 *
 * Clase que maneja toda la lógica de streaming RTMP usando RootEncoder 2.4.x. Gestiona la conexión,
 * configuración de video/audio y estados del stream.
 *
 * ============================================================================
 */
class StreamManager(private val context: Context) : ConnectCheckerRtmp {

    companion object {
        private const val TAG = "StreamManager"
    }

    // RtmpCamera para streaming
    private var rtmpCamera: RtmpCamera2? = null

    // Estados observables
    private val _streamState = MutableStateFlow(StreamState.IDLE)
    val streamState: StateFlow<StreamState> = _streamState.asStateFlow()

    private val _streamStats = MutableStateFlow(StreamStats())
    val streamStats: StateFlow<StreamStats> = _streamStats.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Configuración actual
    private var currentConfig: StreamConfig = StreamConfig.HIGH_QUALITY

    // Tiempo de inicio del stream
    private var streamStartTime: Long = 0L

    // Flag para cámara frontal
    private var isFrontCamera = false

    // ============================================================================
    // ConnectCheckerRtmp Implementation
    // ============================================================================

    override fun onConnectionStartedRtmp(rtmpUrl: String) {
        Log.d(TAG, "📡 Conexión iniciando: $rtmpUrl")
        _streamState.value = StreamState.PREPARING
    }

    override fun onConnectionSuccessRtmp() {
        Log.d(TAG, "✅ Conexión exitosa")
        _streamState.value = StreamState.STREAMING
        streamStartTime = System.currentTimeMillis()
        _streamStats.value = _streamStats.value.copy(isConnected = true)
    }

    override fun onConnectionFailedRtmp(reason: String) {
        Log.e(TAG, "❌ Conexión fallida: $reason")
        _streamState.value = StreamState.ERROR
        _errorMessage.value = "Error de conexión: $reason"
        _streamStats.value = _streamStats.value.copy(isConnected = false)
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        _streamStats.value = _streamStats.value.copy(bitrate = bitrate)
    }

    override fun onDisconnectRtmp() {
        Log.d(TAG, "🔌 Desconectado")
        _streamState.value = StreamState.STOPPED
        _streamStats.value = _streamStats.value.copy(isConnected = false)
    }

    override fun onAuthErrorRtmp() {
        Log.e(TAG, "🔐 Error de autenticación")
        _streamState.value = StreamState.ERROR
        _errorMessage.value = "Error de autenticación con el servidor"
    }

    override fun onAuthSuccessRtmp() {
        Log.d(TAG, "🔓 Autenticación exitosa")
    }

    /**
     * Inicializa el stream manager con una vista de superficie
     *
     * @param surfaceView Vista donde se mostrará la preview de la cámara
     */
    fun initialize(surfaceView: SurfaceView) {
        try {
            rtmpCamera = RtmpCamera2(surfaceView, this)
            Log.d(TAG, "✅ StreamManager inicializado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar: ${e.message}")
            _errorMessage.value = "Error al inicializar la cámara: ${e.message}"
        }
    }

    /**
     * Configura los parámetros de streaming
     *
     * @param config Configuración de streaming a aplicar
     */
    fun configure(config: StreamConfig) {
        currentConfig = config
        Log.d(
                TAG,
                "⚙️ Configuración aplicada: ${config.videoWidth}x${config.videoHeight}@${config.videoFps}fps"
        )
    }

    /** Inicia la preview de la cámara */
    fun startPreview() {
        rtmpCamera?.let { camera ->
            try {
                if (!camera.isOnPreview) {
                    camera.startPreview()
                    Log.d(TAG, "📷 Preview iniciada")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al iniciar preview: ${e.message}")
                _errorMessage.value = "Error al iniciar la cámara: ${e.message}"
            }
        }
    }

    /** Detiene la preview de la cámara */
    fun stopPreview() {
        rtmpCamera?.let { camera ->
            if (camera.isOnPreview) {
                camera.stopPreview()
                Log.d(TAG, "📷 Preview detenida")
            }
        }
    }

    /**
     * Inicia el streaming hacia el servidor RTMP
     *
     * @param rtmpUrl URL del servidor RTMP
     * @param streamKey Clave del stream
     * @return true si el streaming se inició correctamente
     */
    fun startStreaming(rtmpUrl: String? = null, streamKey: String? = null): Boolean {
        val camera =
                rtmpCamera
                        ?: run {
                            _errorMessage.value = "Cámara no inicializada"
                            return false
                        }

        if (camera.isStreaming) {
            Log.w(TAG, "⚠️ Ya está transmitiendo")
            return true
        }

        // Preparar el encoder si no está preparado
        val prepared =
                camera.prepareVideo(
                        currentConfig.videoWidth,
                        currentConfig.videoHeight,
                        currentConfig.videoFps,
                        currentConfig.videoBitrate,
                        0 // rotation
                ) &&
                        camera.prepareAudio(
                                currentConfig.audioBitrate,
                                currentConfig.audioSampleRate,
                                currentConfig.audioIsStereo
                        )

        if (!prepared) {
            _errorMessage.value = "Error al preparar el encoder de video/audio"
            return false
        }

        // Construir URL del stream
        val url = rtmpUrl ?: currentConfig.rtmpUrl
        val key = streamKey ?: currentConfig.streamKey
        val fullUrl = "$url/$key"

        Log.d(TAG, "🚀 Iniciando streaming a: $fullUrl")
        _streamState.value = StreamState.PREPARING

        try {
            camera.startStream(fullUrl)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al iniciar streaming: ${e.message}")
            _errorMessage.value = "Error al conectar: ${e.message}"
            _streamState.value = StreamState.ERROR
            return false
        }
    }

    /** Detiene el streaming */
    fun stopStreaming() {
        rtmpCamera?.let { camera ->
            if (camera.isStreaming) {
                camera.stopStream()
                _streamState.value = StreamState.STOPPED
                _streamStats.value = StreamStats()
                Log.d(TAG, "⏹️ Streaming detenido")
            }
        }
    }

    /** Cambia entre cámara frontal y trasera */
    fun switchCamera() {
        rtmpCamera?.let { camera ->
            try {
                camera.switchCamera()
                isFrontCamera = !isFrontCamera
                Log.d(TAG, "🔄 Cámara cambiada a ${if (isFrontCamera) "frontal" else "trasera"}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cambiar cámara: ${e.message}")
            }
        }
    }

    /** Activa/desactiva el micrófono */
    fun toggleMute(): Boolean {
        rtmpCamera?.let { camera ->
            return if (camera.isAudioMuted) {
                camera.enableAudio()
                Log.d(TAG, "🔊 Audio activado")
                false
            } else {
                camera.disableAudio()
                Log.d(TAG, "🔇 Audio silenciado")
                true
            }
        }
        return false
    }

    /** Verifica si está transmitiendo */
    fun isStreaming(): Boolean = rtmpCamera?.isStreaming == true

    /** Verifica si la preview está activa */
    fun isOnPreview(): Boolean = rtmpCamera?.isOnPreview == true

    /** Verifica si el audio está silenciado */
    fun isAudioMuted(): Boolean = rtmpCamera?.isAudioMuted == true

    /** Obtiene la duración del streaming actual en segundos */
    fun getStreamDuration(): Long {
        return if (streamStartTime > 0 && isStreaming()) {
            (System.currentTimeMillis() - streamStartTime) / 1000
        } else {
            0L
        }
    }

    /** Limpia el mensaje de error */
    fun clearError() {
        _errorMessage.value = null
    }

    /** Libera todos los recursos */
    fun release() {
        stopStreaming()
        stopPreview()
        rtmpCamera = null
        _streamState.value = StreamState.IDLE
        Log.d(TAG, "🧹 Recursos liberados")
    }
}
