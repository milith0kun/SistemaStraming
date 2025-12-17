package com.example.streaming.chat

import android.util.Log
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * ============================================================================
 * CHAT MANAGER - Gestión del Chat en Vivo
 * ============================================================================
 *
 * Maneja la conexión Socket.IO con el servidor de streaming para:
 * - Enviar y recibir mensajes de chat
 * - Tracking de viewers en tiempo real
 * - Notificaciones de eventos del stream
 *
 * ============================================================================
 */
class ChatManager {

    companion object {
        private const val TAG = "ChatManager"
        // URL del servidor de streaming desplegado
        private const val SERVER_URL = "https://streamingpe.myvnc.com"
    }

    private var socket: Socket? = null
    private val gson = Gson()

    // Estados observables
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _viewerCount = MutableStateFlow(ViewerInfo())
    val viewerCount: StateFlow<ViewerInfo> = _viewerCount.asStateFlow()

    private val _streamEvents = MutableStateFlow<StreamEvent?>(null)
    val streamEvents: StateFlow<StreamEvent?> = _streamEvents.asStateFlow()

    private var currentStreamKey: String = "stream"
    private var currentUsername: String = "Android User"

    /**
     * Conecta al servidor Socket.IO
     */
    fun connect() {
        if (socket?.connected() == true) {
            Log.d(TAG, "Ya está conectado")
            return
        }

        try {
            _connectionState.value = ConnectionState.CONNECTING

            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 10000
            }

            socket = IO.socket(SERVER_URL, options)
            setupSocketListeners()
            socket?.connect()

            Log.d(TAG, "📡 Intentando conectar a $SERVER_URL")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al conectar: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    /**
     * Configura los listeners de Socket.IO
     */
    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ Conectado al servidor")
                _connectionState.value = ConnectionState.CONNECTED
                // Unirse al stream actual
                joinStream(currentStreamKey)
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "🔌 Desconectado del servidor")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "❌ Error de conexión: ${args.firstOrNull()}")
                _connectionState.value = ConnectionState.ERROR
            }

            // Recibir mensajes de chat
            on("chat-message") { args ->
                try {
                    val data = args[0] as JSONObject
                    val message = ChatMessage(
                        id = data.optString("id", System.currentTimeMillis().toString()),
                        username = data.getString("username"),
                        message = data.getString("message"),
                        timestamp = data.optLong("timestamp", System.currentTimeMillis()),
                        isFromAndroid = data.optString("username") == currentUsername
                    )
                    
                    // Agregar mensaje a la lista (máximo 100 mensajes)
                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.add(message)
                    if (currentMessages.size > 100) {
                        currentMessages.removeAt(0)
                    }
                    _messages.value = currentMessages
                    
                    Log.d(TAG, "💬 Mensaje recibido: ${message.username}: ${message.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando mensaje: ${e.message}")
                }
            }

            // Actualización de viewers
            on("viewer-count") { args ->
                try {
                    val data = args[0] as JSONObject
                    _viewerCount.value = ViewerInfo(
                        current = data.optInt("viewers", 0),
                        peak = data.optInt("peakViewers", 0),
                        streamKey = data.optString("streamKey", currentStreamKey)
                    )
                    Log.d(TAG, "👥 Viewers: ${_viewerCount.value.current}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando viewer count: ${e.message}")
                }
            }

            // Eventos del stream (inicio/fin)
            on("stream-started") { args ->
                _streamEvents.value = StreamEvent.STREAM_STARTED
                Log.d(TAG, "🔴 Stream iniciado")
            }

            on("stream-ended") { args ->
                _streamEvents.value = StreamEvent.STREAM_ENDED
                Log.d(TAG, "⏹️ Stream terminado")
            }
        }
    }

    /**
     * Unirse a un stream específico
     */
    fun joinStream(streamKey: String) {
        currentStreamKey = streamKey
        socket?.emit("join-stream", streamKey)
        Log.d(TAG, "📺 Unido al stream: $streamKey")
    }

    /**
     * Salir de un stream
     */
    fun leaveStream(streamKey: String) {
        socket?.emit("leave-stream", streamKey)
        Log.d(TAG, "👋 Saliendo del stream: $streamKey")
    }

    /**
     * Enviar un mensaje de chat
     */
    fun sendMessage(message: String, username: String = currentUsername) {
        if (message.isBlank()) return
        
        currentUsername = username.ifBlank { "Android User" }
        
        val messageData = JSONObject().apply {
            put("streamKey", currentStreamKey)
            put("username", currentUsername)
            put("message", message.take(500)) // Máximo 500 caracteres
        }

        socket?.emit("chat-message", messageData)
        Log.d(TAG, "📤 Mensaje enviado: $message")
    }

    /**
     * Establecer el nombre de usuario
     */
    fun setUsername(username: String) {
        currentUsername = username.ifBlank { "Android User" }
    }

    /**
     * Limpiar mensajes
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * Limpiar evento de stream
     */
    fun clearStreamEvent() {
        _streamEvents.value = null
    }

    /**
     * Desconectar del servidor
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "🔌 Desconectado")
    }

    /**
     * Verificar si está conectado
     */
    fun isConnected(): Boolean = socket?.connected() == true
}

/**
 * Estados de conexión
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Modelo de mensaje de chat
 */
data class ChatMessage(
    val id: String,
    val username: String,
    val message: String,
    val timestamp: Long,
    val isFromAndroid: Boolean = false
)

/**
 * Información de viewers
 */
data class ViewerInfo(
    val current: Int = 0,
    val peak: Int = 0,
    val streamKey: String = "stream"
)

/**
 * Eventos del stream
 */
enum class StreamEvent {
    STREAM_STARTED,
    STREAM_ENDED
}
