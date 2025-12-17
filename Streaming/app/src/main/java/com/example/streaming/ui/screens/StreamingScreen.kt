package com.example.streaming.ui.screens

import android.Manifest
import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.streaming.streaming.StreamConfig
import com.example.streaming.streaming.StreamManager
import com.example.streaming.streaming.StreamState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * ============================================================================ PANTALLA DE
 * STREAMING ============================================================================
 *
 * UI principal de la aplicación de streaming con:
 * - Preview de cámara
 * - Controles de transmisión
 * - Configuración de servidor RTMP
 * - Estadísticas en tiempo real
 *
 * ============================================================================
 */

// Colores personalizados
private val GradientStart = Color(0xFF8B5CF6)
private val GradientEnd = Color(0xFF06B6D4)
private val SurfaceColor = Color(0xFF1A1A2E)
private val CardColor = Color(0xFF16213E)
private val ErrorColor = Color(0xFFEF4444)
private val SuccessColor = Color(0xFF10B981)
private val LiveColor = Color(0xFFEF4444)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StreamingScreen() {
        val context = LocalContext.current

        // Manejo de permisos
        val permissionsState =
                rememberMultiplePermissionsState(
                        permissions =
                                listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )

        // Estados de la UI
        var rtmpUrl by remember { mutableStateOf("rtmp://streamingpe.myvnc.com:1935/live") }
        var streamKey by remember { mutableStateOf("stream") }
        var showSettings by remember { mutableStateOf(false) }
        var selectedQuality by remember { mutableStateOf("HIGH") }

        // Stream Manager
        val streamManager = remember { StreamManager(context) }
        val streamState by streamManager.streamState.collectAsState()
        val streamStats by streamManager.streamStats.collectAsState()
        val errorMessage by streamManager.errorMessage.collectAsState()

        // Configuración basada en calidad seleccionada
        val streamConfig =
                remember(selectedQuality) {
                        when (selectedQuality) {
                                "LOW" -> StreamConfig.LOW_QUALITY
                                "MEDIUM" -> StreamConfig.MEDIUM_QUALITY
                                "HIGH" -> StreamConfig.HIGH_QUALITY
                                "ULTRA" -> StreamConfig.ULTRA_QUALITY
                                else -> StreamConfig.HIGH_QUALITY
                        }
                }

        // Estados locales
        var isMuted by remember { mutableStateOf(false) }
        var streamDuration by remember { mutableStateOf(0L) }

        // Actualizar duración del stream
        LaunchedEffect(streamState) {
                if (streamState == StreamState.STREAMING) {
                        while (true) {
                                streamDuration = streamManager.getStreamDuration()
                                kotlinx.coroutines.delay(1000)
                        }
                } else {
                        streamDuration = 0L
                }
        }

        // Limpiar recursos al salir
        DisposableEffect(Unit) { onDispose { streamManager.release() } }

        Box(modifier = Modifier.fillMaxSize().background(SurfaceColor)) {
                // Verificar permisos
                if (!permissionsState.allPermissionsGranted) {
                        PermissionRequestScreen(
                                onRequestPermissions = {
                                        permissionsState.launchMultiplePermissionRequest()
                                },
                                cameraGranted =
                                        permissionsState.permissions.any {
                                                it.permission == Manifest.permission.CAMERA &&
                                                        it.status.isGranted
                                        },
                                audioGranted =
                                        permissionsState.permissions.any {
                                                it.permission == Manifest.permission.RECORD_AUDIO &&
                                                        it.status.isGranted
                                        }
                        )
                } else {
                        // Contenido principal
                        Column(modifier = Modifier.fillMaxSize()) {
                                // Header
                                StreamingHeader(
                                        streamState = streamState,
                                        streamDuration = streamDuration,
                                        onSettingsClick = { showSettings = !showSettings }
                                )

                                // Vista de cámara
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        CameraPreview(
                                                streamManager = streamManager,
                                                streamConfig = streamConfig
                                        )

                                        // Indicador de LIVE
                                        if (streamState == StreamState.STREAMING) {
                                                LiveIndicator(
                                                        modifier =
                                                                Modifier.align(Alignment.TopStart)
                                                                        .padding(16.dp)
                                                )
                                        }

                                        // Estadísticas
                                        if (streamState == StreamState.STREAMING) {
                                                StreamStatsCard(
                                                        bitrate = streamStats.bitrate,
                                                        duration = streamDuration,
                                                        modifier =
                                                                Modifier.align(Alignment.TopEnd)
                                                                        .padding(16.dp)
                                                )
                                        }

                                        // Panel de configuración
                                        if (showSettings) {
                                                Box(
                                                        modifier =
                                                                Modifier.align(
                                                                        Alignment.BottomCenter
                                                                )
                                                ) {
                                                        SettingsPanel(
                                                                rtmpUrl = rtmpUrl,
                                                                onRtmpUrlChange = { rtmpUrl = it },
                                                                streamKey = streamKey,
                                                                onStreamKeyChange = {
                                                                        streamKey = it
                                                                },
                                                                selectedQuality = selectedQuality,
                                                                onQualityChange = {
                                                                        selectedQuality = it
                                                                },
                                                                onDismiss = { showSettings = false }
                                                        )
                                                }
                                        }
                                }

                                // Controles inferiores
                                StreamingControls(
                                        streamState = streamState,
                                        isMuted = isMuted,
                                        onStartStop = {
                                                if (streamManager.isStreaming()) {
                                                        streamManager.stopStreaming()
                                                } else {
                                                        streamManager.configure(streamConfig)
                                                        streamManager.startStreaming(
                                                                rtmpUrl,
                                                                streamKey
                                                        )
                                                }
                                        },
                                        onSwitchCamera = { streamManager.switchCamera() },
                                        onToggleMute = { isMuted = streamManager.toggleMute() },
                                        enabled = streamState != StreamState.PREPARING
                                )
                        }
                }

                // Mostrar errores
                errorMessage?.let { error ->
                        ErrorSnackbar(
                                message = error,
                                onDismiss = { streamManager.clearError() },
                                modifier = Modifier.align(Alignment.BottomCenter)
                        )
                }
        }
}

@Composable
fun StreamingHeader(streamState: StreamState, streamDuration: Long, onSettingsClick: () -> Unit) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                Color.Black.copy(alpha = 0.7f),
                                                                Color.Transparent
                                                        )
                                        )
                                )
                                .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Logo y título
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                GradientStart,
                                                                                GradientEnd
                                                                        )
                                                        )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                )
                        }
                        Column {
                                Text(
                                        text = "StreamHub",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                )
                                Text(
                                        text =
                                                when (streamState) {
                                                        StreamState.IDLE -> "Listo para transmitir"
                                                        StreamState.PREPARING -> "Conectando..."
                                                        StreamState.STREAMING ->
                                                                formatDuration(streamDuration)
                                                        StreamState.STOPPED ->
                                                                "Transmisión finalizada"
                                                        StreamState.ERROR -> "Error de conexión"
                                                        StreamState.PAUSED -> "Pausado"
                                                },
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                )
                        }
                }

                // Botón de configuración
                IconButton(
                        onClick = onSettingsClick,
                        modifier =
                                Modifier.size(40.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                        Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Configuración",
                                tint = Color.White
                        )
                }
        }
}

@Composable
fun CameraPreview(streamManager: StreamManager, streamConfig: StreamConfig) {
        AndroidView(
                factory = { context ->
                        SurfaceView(context).apply {
                                holder.addCallback(
                                        object : android.view.SurfaceHolder.Callback {
                                                override fun surfaceCreated(
                                                        holder: android.view.SurfaceHolder
                                                ) {
                                                        streamManager.initialize(this@apply)
                                                        streamManager.configure(streamConfig)
                                                        streamManager.startPreview()
                                                }

                                                override fun surfaceChanged(
                                                        holder: android.view.SurfaceHolder,
                                                        format: Int,
                                                        width: Int,
                                                        height: Int
                                                ) {}

                                                override fun surfaceDestroyed(
                                                        holder: android.view.SurfaceHolder
                                                ) {
                                                        streamManager.stopPreview()
                                                }
                                        }
                                )
                        }
                },
                modifier = Modifier.fillMaxSize()
        )
}

@Composable
fun LiveIndicator(modifier: Modifier = Modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "live")
        val alpha by
                infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.3f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "pulse"
                )

        Row(
                modifier =
                        modifier.background(LiveColor.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                        modifier =
                                Modifier.size(8.dp)
                                        .background(Color.White.copy(alpha = alpha), CircleShape)
                )
                Text(
                        text = "EN VIVO",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                )
        }
}

@Composable
fun StreamStatsCard(bitrate: Long, duration: Long, modifier: Modifier = Modifier) {
        Column(
                modifier =
                        modifier.background(
                                        Color.Black.copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                horizontalAlignment = Alignment.End
        ) {
                Text(
                        text = "${bitrate / 1000} kbps",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                )
                Text(
                        text = formatDuration(duration),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                )
        }
}

@Composable
fun StreamingControls(
        streamState: StreamState,
        isMuted: Boolean,
        onStartStop: () -> Unit,
        onSwitchCamera: () -> Unit,
        onToggleMute: () -> Unit,
        enabled: Boolean
) {
        val isStreaming = streamState == StreamState.STREAMING

        Row(
                modifier = Modifier.fillMaxWidth().background(CardColor).padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Botón de mute
                ControlButton(
                        icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        label = if (isMuted) "Silenciado" else "Audio",
                        onClick = onToggleMute,
                        enabled = enabled,
                        tint = if (isMuted) ErrorColor else Color.White
                )

                // Botón principal de streaming
                Box(
                        modifier =
                                Modifier.size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                                brush =
                                                        if (isStreaming)
                                                                Brush.linearGradient(
                                                                        listOf(
                                                                                ErrorColor,
                                                                                ErrorColor
                                                                        )
                                                                )
                                                        else
                                                                Brush.linearGradient(
                                                                        listOf(
                                                                                GradientStart,
                                                                                GradientEnd
                                                                        )
                                                                )
                                        )
                                        .clickable(enabled = enabled) { onStartStop() }
                                        .border(
                                                width = 4.dp,
                                                color = Color.White.copy(alpha = 0.3f),
                                                shape = CircleShape
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector =
                                        if (isStreaming) Icons.Filled.Stop
                                        else Icons.Filled.FiberManualRecord,
                                contentDescription = if (isStreaming) "Detener" else "Iniciar",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                        )
                }

                // Botón de cambiar cámara
                ControlButton(
                        icon = Icons.Filled.Cameraswitch,
                        label = "Cámara",
                        onClick = onSwitchCamera,
                        enabled = enabled
                )
        }
}

@Composable
fun ControlButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        tint: Color = Color.White
) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                        onClick = onClick,
                        enabled = enabled,
                        modifier =
                                Modifier.size(56.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (enabled) tint else tint.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                        )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = label,
                        color = Color.White.copy(alpha = if (enabled) 0.7f else 0.3f),
                        fontSize = 12.sp
                )
        }
}

@Composable
fun SettingsPanel(
        rtmpUrl: String,
        onRtmpUrlChange: (String) -> Unit,
        streamKey: String,
        onStreamKeyChange: (String) -> Unit,
        selectedQuality: String,
        onQualityChange: (String) -> Unit,
        onDismiss: () -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp)
        ) {
                Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "⚙️ Configuración",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                )
                                IconButton(onClick = onDismiss) {
                                        Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Cerrar",
                                                tint = Color.White
                                        )
                                }
                        }

                        // URL RTMP
                        OutlinedTextField(
                                value = rtmpUrl,
                                onValueChange = onRtmpUrlChange,
                                label = { Text("URL del Servidor RTMP") },
                                placeholder = { Text("rtmp://192.168.1.100:1935/live") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = GradientStart,
                                                unfocusedBorderColor =
                                                        Color.White.copy(alpha = 0.3f),
                                                focusedLabelColor = GradientStart,
                                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                        ),
                                leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Filled.Cloud,
                                                contentDescription = null,
                                                tint = GradientStart
                                        )
                                }
                        )

                        // Stream Key
                        OutlinedTextField(
                                value = streamKey,
                                onValueChange = onStreamKeyChange,
                                label = { Text("Stream Key") },
                                placeholder = { Text("stream") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = GradientStart,
                                                unfocusedBorderColor =
                                                        Color.White.copy(alpha = 0.3f),
                                                focusedLabelColor = GradientStart,
                                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                        ),
                                leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Filled.Key,
                                                contentDescription = null,
                                                tint = GradientEnd
                                        )
                                }
                        )

                        // Selector de calidad
                        Text(
                                text = "Calidad de Video",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                        )
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                listOf(
                                                "LOW" to "480p",
                                                "MEDIUM" to "720p",
                                                "HIGH" to "720p+",
                                                "ULTRA" to "1080p"
                                        )
                                        .forEach { (key, label) ->
                                                FilterChip(
                                                        selected = selectedQuality == key,
                                                        onClick = { onQualityChange(key) },
                                                        label = { Text(label) },
                                                        colors =
                                                                FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor =
                                                                                GradientStart,
                                                                        selectedLabelColor =
                                                                                Color.White,
                                                                        containerColor =
                                                                                Color.White.copy(
                                                                                        alpha = 0.1f
                                                                                ),
                                                                        labelColor =
                                                                                Color.White.copy(
                                                                                        alpha = 0.7f
                                                                                )
                                                                ),
                                                        modifier = Modifier.weight(1f)
                                                )
                                        }
                        }
                }
        }
}

@Composable
fun PermissionRequestScreen(
        onRequestPermissions: () -> Unit,
        cameraGranted: Boolean,
        audioGranted: Boolean
) {
        Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = GradientStart,
                        modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = "Permisos Requeridos",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text =
                                "Para transmitir video en vivo, necesitamos acceso a tu cámara y micrófono.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Lista de permisos
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PermissionItem(
                                icon = Icons.Outlined.Videocam,
                                title = "Cámara",
                                description = "Para capturar video",
                                isGranted = cameraGranted
                        )
                        PermissionItem(
                                icon = Icons.Outlined.Mic,
                                title = "Micrófono",
                                description = "Para capturar audio",
                                isGranted = audioGranted
                        )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GradientStart),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Conceder Permisos", fontWeight = FontWeight.SemiBold)
                }
        }
}

@Composable
fun PermissionItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        isGranted: Boolean
) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) SuccessColor else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                                text = description,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                        )
                }
                Icon(
                        imageVector =
                                if (isGranted) Icons.Filled.CheckCircle
                                else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isGranted) SuccessColor else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                )
        }
}

@Composable
fun ErrorSnackbar(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
        Card(
                modifier = modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorColor),
                shape = RoundedCornerShape(12.dp)
        ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = Color.White
                        )
                        Text(text = message, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                                Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Cerrar",
                                        tint = Color.White
                                )
                        }
                }
        }
}

/** Formatea la duración en formato HH:MM:SS */
private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
                String.format("%02d:%02d", minutes, secs)
        }
}
