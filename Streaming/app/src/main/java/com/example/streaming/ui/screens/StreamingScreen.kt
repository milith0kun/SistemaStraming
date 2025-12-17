package com.example.streaming.ui.screens

import android.Manifest
import android.app.Activity
import android.view.SurfaceView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.streaming.chat.ChatManager
import com.example.streaming.pip.PipHelper
import com.example.streaming.settings.ThemeManager
import com.example.streaming.streaming.StreamConfig
import com.example.streaming.streaming.StreamManager
import com.example.streaming.streaming.StreamState
import com.example.streaming.ui.components.AdvancedControlsPanel
import com.example.streaming.ui.components.ChatPanel
import com.example.streaming.ui.components.ChatToggleButton
import com.example.streaming.ui.components.MiniStats
import com.example.streaming.ui.components.StreamDashboard
import com.example.streaming.ui.components.ThemeToggleButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ============================================================================ PANTALLA DE
 * STREAMING - VERSIÓN MEJORADA
 * ============================================================================
 */

// Colores personalizados
private val GradientStart = Color(0xFF8B5CF6)
private val GradientEnd = Color(0xFF06B6D4)
private val SurfaceColorDark = Color(0xFF1A1A2E)
private val SurfaceColorLight = Color(0xFFF8FAFC)
private val CardColorDark = Color(0xFF16213E)
private val CardColorLight = Color(0xFFFFFFFF)
private val ErrorColor = Color(0xFFEF4444)
private val SuccessColor = Color(0xFF10B981)
private val LiveColor = Color(0xFFEF4444)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StreamingScreen() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Managers
        val themeManager = remember { ThemeManager(context) }
        val chatManager = remember { ChatManager() }
        val streamManager = remember { StreamManager(context) }

        // Estados del tema (con persistencia)
        val isDarkMode by themeManager.isDarkMode.collectAsState(initial = true)
        val savedUsername by themeManager.username.collectAsState(initial = "Android User")
        val savedStreamKey by themeManager.streamKey.collectAsState(initial = "stream")
        val savedRtmpUrl by
                themeManager.rtmpUrl.collectAsState(
                        initial = "rtmp://streamingpe.myvnc.com:1935/live"
                )
        val savedQuality by themeManager.quality.collectAsState(initial = "HIGH")
        val notificationsEnabled by themeManager.notificationsEnabled.collectAsState(initial = true)
        val pipEnabled by themeManager.pipEnabled.collectAsState(initial = true)
        val isTheaterMode by themeManager.theaterMode.collectAsState(initial = false)

        // Estados del stream
        val streamState by streamManager.streamState.collectAsState()
        val streamStats by streamManager.streamStats.collectAsState()
        val errorMessage by streamManager.errorMessage.collectAsState()

        // Estados del chat
        val chatConnectionState by chatManager.connectionState.collectAsState()
        val chatMessages by chatManager.messages.collectAsState()
        val viewerInfo by chatManager.viewerCount.collectAsState()

        // Manejo de permisos
        val permissionsState =
                rememberMultiplePermissionsState(
                        permissions =
                                listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )

        // Estados de la UI
        var rtmpUrl by remember { mutableStateOf(savedRtmpUrl) }
        var streamKey by remember { mutableStateOf(savedStreamKey) }
        var username by remember { mutableStateOf(savedUsername) }
        var showSettings by remember { mutableStateOf(false) }
        var showAdvancedSettings by remember { mutableStateOf(false) }
        var showChat by remember { mutableStateOf(false) }
        var showDashboard by remember { mutableStateOf(false) }
        var selectedQuality by remember { mutableStateOf(savedQuality) }

        // Estados locales
        var isMuted by remember { mutableStateOf(false) }
        var streamDuration by remember { mutableStateOf(0L) }

        // Historial para gráficas
        var bitrateHistory by remember { mutableStateOf(listOf<Float>()) }
        var viewersHistory by remember { mutableStateOf(listOf<Float>()) }

        // Sincronizar estados guardados
        LaunchedEffect(savedRtmpUrl) { rtmpUrl = savedRtmpUrl }
        LaunchedEffect(savedStreamKey) { streamKey = savedStreamKey }
        LaunchedEffect(savedUsername) { username = savedUsername }
        LaunchedEffect(savedQuality) { selectedQuality = savedQuality }

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

        // Conectar al chat cuando se inicia el stream
        LaunchedEffect(streamState) {
                if (streamState == StreamState.STREAMING) {
                        chatManager.setUsername(username)
                        chatManager.connect()
                        chatManager.joinStream(streamKey)
                }
        }

        // Actualizar duración y estadísticas del stream
        LaunchedEffect(streamState) {
                if (streamState == StreamState.STREAMING) {
                        while (true) {
                                streamDuration = streamManager.getStreamDuration()

                                // Actualizar historial de bitrate (últimos 30 valores)
                                val currentBitrate = streamStats.bitrate.toFloat() / 1000f
                                bitrateHistory = (bitrateHistory + currentBitrate).takeLast(30)

                                // Actualizar historial de viewers
                                viewersHistory =
                                        (viewersHistory + viewerInfo.current.toFloat()).takeLast(30)

                                delay(1000)
                        }
                } else {
                        streamDuration = 0L
                        bitrateHistory = emptyList()
                        viewersHistory = emptyList()
                }
        }

        // Limpiar recursos al salir
        DisposableEffect(Unit) {
                onDispose {
                        streamManager.release()
                        chatManager.disconnect()
                }
        }

        // Colores según tema
        val surfaceColor = if (isDarkMode) SurfaceColorDark else SurfaceColorLight
        val cardColor = if (isDarkMode) CardColorDark else CardColorLight

        Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
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
                                        },
                                isDarkMode = isDarkMode
                        )
                } else {
                        // Contenido principal
                        Column(modifier = Modifier.fillMaxSize()) {
                                // Header (ocultar en modo teatro)
                                if (!isTheaterMode) {
                                        StreamingHeader(
                                                streamState = streamState,
                                                streamDuration = streamDuration,
                                                onSettingsClick = { showSettings = !showSettings },
                                                onAdvancedSettingsClick = {
                                                        showAdvancedSettings = !showAdvancedSettings
                                                },
                                                isDarkMode = isDarkMode,
                                                onToggleTheme = {
                                                        scope.launch {
                                                                themeManager.toggleDarkMode()
                                                        }
                                                },
                                                viewersCount = viewerInfo.current
                                        )
                                }

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

                                        // Mini estadísticas sobre la cámara
                                        if (streamState == StreamState.STREAMING && !showDashboard
                                        ) {
                                                MiniStats(
                                                        bitrate = streamStats.bitrate,
                                                        fps = 30,
                                                        viewers = viewerInfo.current,
                                                        duration = streamDuration,
                                                        isDarkMode = isDarkMode,
                                                        modifier =
                                                                Modifier.align(Alignment.TopEnd)
                                                                        .padding(16.dp)
                                                )
                                        }

                                        // Botón para mostrar dashboard
                                        if (streamState == StreamState.STREAMING) {
                                                IconButton(
                                                        onClick = {
                                                                showDashboard = !showDashboard
                                                        },
                                                        modifier =
                                                                Modifier.align(Alignment.TopEnd)
                                                                        .padding(
                                                                                top = 60.dp,
                                                                                end = 16.dp
                                                                        )
                                                                        .size(40.dp)
                                                                        .background(
                                                                                Color.Black.copy(
                                                                                        alpha = 0.5f
                                                                                ),
                                                                                CircleShape
                                                                        )
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        if (showDashboard)
                                                                                Icons.Filled.Close
                                                                        else Icons.Filled.Analytics,
                                                                contentDescription = "Dashboard",
                                                                tint = Color.White
                                                        )
                                                }
                                        }

                                        // Dashboard expandible
                                        if (showDashboard && streamState == StreamState.STREAMING) {
                                                Card(
                                                        modifier =
                                                                Modifier.align(Alignment.TopCenter)
                                                                        .fillMaxWidth()
                                                                        .padding(16.dp),
                                                        colors =
                                                                CardDefaults.cardColors(
                                                                        containerColor =
                                                                                cardColor.copy(
                                                                                        alpha =
                                                                                                0.95f
                                                                                )
                                                                ),
                                                        shape = RoundedCornerShape(16.dp)
                                                ) {
                                                        StreamDashboard(
                                                                bitrate = streamStats.bitrate,
                                                                duration = streamDuration,
                                                                fps = 30,
                                                                viewersCount = viewerInfo.current,
                                                                peakViewers = viewerInfo.peak,
                                                                isStreaming =
                                                                        streamState ==
                                                                                StreamState
                                                                                        .STREAMING,
                                                                bitrateHistory = bitrateHistory,
                                                                viewersHistory = viewersHistory,
                                                                isDarkMode = isDarkMode
                                                        )
                                                }
                                        }

                                        // Panel de configuración básica
                                        if (showSettings) {
                                                Box(
                                                        modifier =
                                                                Modifier.align(
                                                                        Alignment.BottomCenter
                                                                )
                                                ) {
                                                        SettingsPanel(
                                                                rtmpUrl = rtmpUrl,
                                                                onRtmpUrlChange = {
                                                                        rtmpUrl = it
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .setRtmpUrl(
                                                                                                it
                                                                                        )
                                                                        }
                                                                },
                                                                streamKey = streamKey,
                                                                onStreamKeyChange = {
                                                                        streamKey = it
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .setStreamKey(
                                                                                                it
                                                                                        )
                                                                        }
                                                                },
                                                                selectedQuality = selectedQuality,
                                                                onQualityChange = {
                                                                        selectedQuality = it
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .setQuality(
                                                                                                it
                                                                                        )
                                                                        }
                                                                },
                                                                onDismiss = {
                                                                        showSettings = false
                                                                },
                                                                isDarkMode = isDarkMode
                                                        )
                                                }
                                        }

                                        // Panel de controles avanzados
                                        if (showAdvancedSettings) {
                                                Box(
                                                        modifier =
                                                                Modifier.align(
                                                                        Alignment.BottomCenter
                                                                )
                                                ) {
                                                        AdvancedControlsPanel(
                                                                isExpanded = showAdvancedSettings,
                                                                onDismiss = {
                                                                        showAdvancedSettings = false
                                                                },
                                                                isDarkMode = isDarkMode,
                                                                onToggleTheme = {
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .toggleDarkMode()
                                                                        }
                                                                },
                                                                isPipEnabled = pipEnabled,
                                                                onTogglePip = {
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .setPipEnabled(
                                                                                                !pipEnabled
                                                                                        )
                                                                        }
                                                                },
                                                                onEnterPip = {
                                                                        (context as? Activity)
                                                                                ?.let { activity ->
                                                                                        PipHelper
                                                                                                .enterPipMode(
                                                                                                        activity
                                                                                                )
                                                                                }
                                                                },
                                                                isTheaterMode = isTheaterMode,
                                                                onToggleTheater = {
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .toggleTheaterMode()
                                                                        }
                                                                },
                                                                notificationsEnabled =
                                                                        notificationsEnabled,
                                                                onToggleNotifications = {
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .setNotificationsEnabled(
                                                                                                !notificationsEnabled
                                                                                        )
                                                                        }
                                                                },
                                                                selectedQuality = selectedQuality,
                                                                onQualityChange = {
                                                                        selectedQuality = it
                                                                        scope.launch {
                                                                                themeManager
                                                                                        .setQuality(
                                                                                                it
                                                                                        )
                                                                        }
                                                                },
                                                                isStreaming =
                                                                        streamState ==
                                                                                StreamState
                                                                                        .STREAMING
                                                        )
                                                }
                                        }

                                        // Panel de Chat
                                        ChatPanel(
                                                messages = chatMessages,
                                                connectionState = chatConnectionState,
                                                onSendMessage = { message ->
                                                        chatManager.sendMessage(message, username)
                                                },
                                                username = username,
                                                onUsernameChange = {
                                                        username = it
                                                        chatManager.setUsername(it)
                                                        scope.launch {
                                                                themeManager.setUsername(it)
                                                        }
                                                },
                                                isExpanded = showChat,
                                                onToggleExpand = { showChat = !showChat },
                                                isDarkMode = isDarkMode,
                                                modifier = Modifier.align(Alignment.CenterEnd)
                                        )

                                        // Botón flotante del chat
                                        if (!showChat && streamState == StreamState.STREAMING) {
                                                ChatToggleButton(
                                                        onClick = { showChat = true },
                                                        unreadCount = 0,
                                                        connectionState = chatConnectionState,
                                                        modifier =
                                                                Modifier.align(Alignment.BottomEnd)
                                                                        .padding(16.dp)
                                                )
                                        }
                                }

                                // Controles inferiores (ocultar en modo teatro)
                                if (!isTheaterMode) {
                                        StreamingControls(
                                                streamState = streamState,
                                                isMuted = isMuted,
                                                onStartStop = {
                                                        if (streamManager.isStreaming()) {
                                                                streamManager.stopStreaming()
                                                                chatManager.leaveStream(streamKey)
                                                        } else {
                                                                streamManager.configure(
                                                                        streamConfig
                                                                )
                                                                streamManager.startStreaming(
                                                                        rtmpUrl,
                                                                        streamKey
                                                                )
                                                        }
                                                },
                                                onSwitchCamera = { streamManager.switchCamera() },
                                                onToggleMute = {
                                                        isMuted = streamManager.toggleMute()
                                                },
                                                enabled = streamState != StreamState.PREPARING,
                                                isDarkMode = isDarkMode
                                        )
                                }
                        }
                }

                // Mostrar errores
                errorMessage?.let { error ->
                        ErrorSnackbar(
                                message = error,
                                onDismiss = { streamManager.clearError() },
                                modifier = Modifier.align(Alignment.BottomCenter),
                                isDarkMode = isDarkMode
                        )
                }

                // Botón para salir del modo teatro
                if (isTheaterMode) {
                        IconButton(
                                onClick = { scope.launch { themeManager.setTheaterMode(false) } },
                                modifier =
                                        Modifier.align(Alignment.TopEnd)
                                                .padding(16.dp)
                                                .size(40.dp)
                                                .background(
                                                        Color.Black.copy(alpha = 0.5f),
                                                        CircleShape
                                                )
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Fullscreen,
                                        contentDescription = "Salir modo teatro",
                                        tint = Color.White
                                )
                        }
                }
        }
}

@Composable
fun StreamingHeader(
        streamState: StreamState,
        streamDuration: Long,
        onSettingsClick: () -> Unit,
        onAdvancedSettingsClick: () -> Unit,
        isDarkMode: Boolean,
        onToggleTheme: () -> Unit,
        viewersCount: Int
) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                if (isDarkMode)
                                                                        Color.Black.copy(
                                                                                alpha = 0.7f
                                                                        )
                                                                else Color.White.copy(alpha = 0.9f),
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
                                                                listOf(GradientStart, GradientEnd)
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
                                        color = if (isDarkMode) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                )
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                        Text(
                                                text =
                                                        when (streamState) {
                                                                StreamState.IDLE ->
                                                                        "Listo para transmitir"
                                                                StreamState.PREPARING ->
                                                                        "Conectando..."
                                                                StreamState.STREAMING ->
                                                                        formatDuration(
                                                                                streamDuration
                                                                        )
                                                                StreamState.STOPPED ->
                                                                        "Transmisión finalizada"
                                                                StreamState.ERROR ->
                                                                        "Error de conexión"
                                                                StreamState.PAUSED -> "Pausado"
                                                        },
                                                color =
                                                        if (isDarkMode)
                                                                Color.White.copy(alpha = 0.7f)
                                                        else Color.Gray,
                                                fontSize = 12.sp
                                        )
                                        if (streamState == StreamState.STREAMING && viewersCount > 0
                                        ) {
                                                Text("•", color = Color.Gray, fontSize = 12.sp)
                                                Icon(
                                                        imageVector = Icons.Filled.People,
                                                        contentDescription = null,
                                                        tint = SuccessColor,
                                                        modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                        text = "$viewersCount",
                                                        color = SuccessColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                }
                        }
                }

                // Botones de control
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeToggleButton(isDarkMode = isDarkMode, onToggle = onToggleTheme)

                        IconButton(
                                onClick = onAdvancedSettingsClick,
                                modifier =
                                        Modifier.size(40.dp)
                                                .background(
                                                        if (isDarkMode)
                                                                Color.White.copy(alpha = 0.1f)
                                                        else Color.Black.copy(alpha = 0.1f),
                                                        CircleShape
                                                )
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Configuración avanzada",
                                        tint = if (isDarkMode) Color.White else Color.Black
                                )
                        }

                        IconButton(
                                onClick = onSettingsClick,
                                modifier =
                                        Modifier.size(40.dp)
                                                .background(
                                                        if (isDarkMode)
                                                                Color.White.copy(alpha = 0.1f)
                                                        else Color.Black.copy(alpha = 0.1f),
                                                        CircleShape
                                                )
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Configuración",
                                        tint = if (isDarkMode) Color.White else Color.Black
                                )
                        }
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
fun StreamingControls(
        streamState: StreamState,
        isMuted: Boolean,
        onStartStop: () -> Unit,
        onSwitchCamera: () -> Unit,
        onToggleMute: () -> Unit,
        enabled: Boolean,
        isDarkMode: Boolean
) {
        val isStreaming = streamState == StreamState.STREAMING

        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(if (isDarkMode) CardColorDark else CardColorLight)
                                .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
                ControlButton(
                        icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        label = if (isMuted) "Silenciado" else "Audio",
                        onClick = onToggleMute,
                        enabled = enabled,
                        tint =
                                if (isMuted) ErrorColor
                                else if (isDarkMode) Color.White else Color.Black,
                        isDarkMode = isDarkMode
                )

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
                                        .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
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

                ControlButton(
                        icon = Icons.Filled.Cameraswitch,
                        label = "Cámara",
                        onClick = onSwitchCamera,
                        enabled = enabled,
                        isDarkMode = isDarkMode
                )
        }
}

@Composable
fun ControlButton(
        icon: ImageVector,
        label: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        tint: Color = Color.White,
        isDarkMode: Boolean = true
) {
        val buttonTint = if (isDarkMode) tint else if (tint == Color.White) Color.Black else tint

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                        onClick = onClick,
                        enabled = enabled,
                        modifier =
                                Modifier.size(56.dp)
                                        .background(
                                                if (isDarkMode) Color.White.copy(alpha = 0.1f)
                                                else Color.Black.copy(alpha = 0.1f),
                                                CircleShape
                                        )
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (enabled) buttonTint else buttonTint.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                        )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = label,
                        color =
                                (if (isDarkMode) Color.White else Color.Black).copy(
                                        alpha = if (enabled) 0.7f else 0.3f
                                ),
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
        onDismiss: () -> Unit,
        isDarkMode: Boolean
) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = if (isDarkMode) CardColorDark else CardColorLight
                        ),
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
                                        color = if (isDarkMode) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                )
                                IconButton(onClick = onDismiss) {
                                        Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Cerrar",
                                                tint = if (isDarkMode) Color.White else Color.Black
                                        )
                                }
                        }

                        OutlinedTextField(
                                value = rtmpUrl,
                                onValueChange = onRtmpUrlChange,
                                label = { Text("URL del Servidor RTMP") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedTextColor =
                                                        if (isDarkMode) Color.White
                                                        else Color.Black,
                                                unfocusedTextColor =
                                                        if (isDarkMode) Color.White
                                                        else Color.Black,
                                                focusedBorderColor = GradientStart,
                                                unfocusedBorderColor =
                                                        if (isDarkMode)
                                                                Color.White.copy(alpha = 0.3f)
                                                        else Color.Gray
                                        ),
                                leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Filled.Cloud,
                                                contentDescription = null,
                                                tint = GradientStart
                                        )
                                }
                        )

                        OutlinedTextField(
                                value = streamKey,
                                onValueChange = onStreamKeyChange,
                                label = { Text("Stream Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedTextColor =
                                                        if (isDarkMode) Color.White
                                                        else Color.Black,
                                                unfocusedTextColor =
                                                        if (isDarkMode) Color.White
                                                        else Color.Black,
                                                focusedBorderColor = GradientStart,
                                                unfocusedBorderColor =
                                                        if (isDarkMode)
                                                                Color.White.copy(alpha = 0.3f)
                                                        else Color.Gray
                                        ),
                                leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Filled.Key,
                                                contentDescription = null,
                                                tint = GradientEnd
                                        )
                                }
                        )

                        Text(
                                text = "Calidad de Video",
                                color =
                                        if (isDarkMode) Color.White.copy(alpha = 0.7f)
                                        else Color.Gray,
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
                                                                                if (isDarkMode)
                                                                                        Color.White
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.1f
                                                                                                )
                                                                                else
                                                                                        Color.Gray
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.1f
                                                                                                ),
                                                                        labelColor =
                                                                                if (isDarkMode)
                                                                                        Color.White
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.7f
                                                                                                )
                                                                                else Color.Gray
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
        audioGranted: Boolean,
        isDarkMode: Boolean
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
                        color = if (isDarkMode) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text =
                                "Para transmitir video en vivo, necesitamos acceso a tu cámara y micrófono.",
                        color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PermissionItem(
                                icon = Icons.Outlined.Videocam,
                                title = "Cámara",
                                description = "Para capturar video",
                                isGranted = cameraGranted,
                                isDarkMode = isDarkMode
                        )
                        PermissionItem(
                                icon = Icons.Outlined.Mic,
                                title = "Micrófono",
                                description = "Para capturar audio",
                                isGranted = audioGranted,
                                isDarkMode = isDarkMode
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
        icon: ImageVector,
        title: String,
        description: String,
        isGranted: Boolean,
        isDarkMode: Boolean
) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        if (isDarkMode) Color.White.copy(alpha = 0.05f)
                                        else Color.Gray.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint =
                                if (isGranted) SuccessColor
                                else if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                color = if (isDarkMode) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                text = description,
                                color =
                                        if (isDarkMode) Color.White.copy(alpha = 0.5f)
                                        else Color.Gray,
                                fontSize = 12.sp
                        )
                }
                Icon(
                        imageVector =
                                if (isGranted) Icons.Filled.CheckCircle
                                else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint =
                                if (isGranted) SuccessColor
                                else if (isDarkMode) Color.White.copy(alpha = 0.3f)
                                else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                )
        }
}

@Composable
fun ErrorSnackbar(
        message: String,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
        isDarkMode: Boolean
) {
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
