/**
 * ============================================================================
 * APP.JS - Lógica de la Plataforma de Streaming
 * ============================================================================
 * 
 * Funcionalidades:
 * - Reproducción de streams usando HTTP-FLV (mpegts.js - baja latencia)
 * - Fallback a HLS si está disponible
 * - Monitoreo de estadísticas en tiempo real
 * - Control del reproductor
 * 
 * ============================================================================
 */

// ============================================================================
// Variables globales
// ============================================================================
let player = null;
let video = null;
let isPlaying = false;
let reconnectAttempts = 0;
let statsInterval = null;
const MAX_RECONNECT_ATTEMPTS = 5;
let socket = null;
let currentStreamKey = 'stream';
let viewersCount = 0;
let peakViewersCount = 0;
let streamStatusInterval = null;
let username = localStorage.getItem('streaming_username') || '';

// ============================================================================
// Configuración
// ============================================================================
const CONFIG = {
    mediaServerUrl: 'https://streamingpe.myvnc.com',
    rtmpUrl: 'rtmp://3.134.159.236:1935/live',
    statusCheckInterval: 5000,
    statsUpdateInterval: 1000,
};

// ============================================================================
// Inicialización
// ============================================================================
document.addEventListener('DOMContentLoaded', () => {
    video = document.getElementById('videoPlayer');

    // Verificar soporte
    if (typeof mpegts !== 'undefined' && mpegts.isSupported()) {
        console.log('✅ mpegts.js soportado (HTTP-FLV)');
    } else if (typeof Hls !== 'undefined' && Hls.isSupported()) {
        console.log('✅ HLS.js soportado');
    } else {
        console.warn('⚠️ Reproducción limitada');
    }

    // Iniciar verificación de estado del servidor
    checkServerStatus();
    setInterval(checkServerStatus, CONFIG.statusCheckInterval);

    // Verificar estado del stream periódicamente
    checkStreamStatus();
    streamStatusInterval = setInterval(checkStreamStatus, 3000);

    // Event listeners del video
    setupVideoEventListeners();

    // Actualizar URLs cuando cambie el stream key
    document.getElementById('streamKey').addEventListener('input', updateStreamUrls);
    updateStreamUrls();

    // Inicializar Socket.IO para tracking de viewers
    initializeSocket();

    // Inicializar chat
    initializeChat();

    console.log('🎬 Plataforma de Streaming inicializada');
});

// ============================================================================
// Control del reproductor - HTTP-FLV con mpegts.js
// ============================================================================
function loadStream() {
    const streamKey = document.getElementById('streamKey').value || 'stream';
    const flvUrl = `${CONFIG.mediaServerUrl}/live/${streamKey}.flv`;

    console.log('📡 Conectando al stream FLV:', flvUrl);
    showToast('Conectando al stream...');

    // Destruir player anterior
    destroyPlayer();

    // Usar mpegts.js para HTTP-FLV
    if (typeof mpegts !== 'undefined' && mpegts.isSupported()) {
        loadMpegtsStream(flvUrl);
    } else if (typeof Hls !== 'undefined' && Hls.isSupported()) {
        // Fallback a HLS
        const hlsUrl = `${CONFIG.mediaServerUrl}/live/${streamKey}/index.m3u8`;
        loadHlsStream(hlsUrl);
    } else {
        showToast('Reproducción no soportada en este navegador');
    }
}

function loadMpegtsStream(flvUrl) {
    console.log('🎬 Usando mpegts.js (HTTP-FLV, baja latencia)');

    player = mpegts.createPlayer({
        type: 'flv',
        url: flvUrl,
        isLive: true,
        hasAudio: true,
        hasVideo: true,
        cors: true,
    }, {
        enableWorker: true,
        enableStashBuffer: false,
        stashInitialSize: 128,
        liveBufferLatencyChasing: true,
        liveBufferLatencyMaxLatency: 1.5,
        liveBufferLatencyMinRemain: 0.3,
    });

    player.attachMediaElement(video);

    // Eventos del player
    player.on(mpegts.Events.ERROR, (errorType, errorDetail, errorInfo) => {
        console.error('❌ Error mpegts:', errorType, errorDetail);

        if (errorType === mpegts.ErrorTypes.NETWORK_ERROR) {
            handleNetworkError();
        } else {
            showToast('Error en el stream: ' + errorDetail);
            showOverlay();
            updateLiveIndicator(false);
        }
    });

    player.on(mpegts.Events.LOADING_COMPLETE, () => {
        console.log('📦 Stream cargado');
    });

    player.on(mpegts.Events.RECOVERED_EARLY_EOF, () => {
        console.log('🔄 Recuperado de EOF temprano');
    });

    player.on(mpegts.Events.MEDIA_INFO, (mediaInfo) => {
        console.log('📊 Media info:', mediaInfo);

        if (mediaInfo.width && mediaInfo.height) {
            document.getElementById('resolutionValue').textContent =
                `${mediaInfo.width}x${mediaInfo.height}`;
        }
        if (mediaInfo.fps) {
            document.getElementById('fpsValue').textContent =
                Math.round(mediaInfo.fps);
        }
    });

    player.on(mpegts.Events.STATISTICS_INFO, (stats) => {
        if (stats.speed) {
            const kbps = Math.round(stats.speed * 8 / 1000);
            document.getElementById('bitrateValue').textContent = kbps || '--';
            document.getElementById('streamQuality').textContent = `${kbps} kbps`;
        }
    });

    // Cargar y reproducir
    player.load();

    player.play().then(() => {
        console.log('▶️ Reproducción iniciada');
        isPlaying = true;
        updatePlayPauseButton();
        hideOverlay();
        updateLiveIndicator(true);
        reconnectAttempts = 0;
        showToast('🎬 Stream conectado');

        // Iniciar actualización de stats
        startStatsUpdate();
    }).catch(err => {
        console.warn('Auto-play bloqueado:', err);
        showToast('Haz clic en play para iniciar');
        hideOverlay();
    });
}

function loadHlsStream(hlsUrl) {
    console.log('📡 Usando HLS (requiere FFmpeg en servidor):', hlsUrl);

    player = new Hls({
        debug: false,
        enableWorker: true,
        lowLatencyMode: true,
    });

    player.loadSource(hlsUrl);
    player.attachMedia(video);

    player.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log('✅ HLS manifest cargado');
        video.play().then(() => {
            isPlaying = true;
            updatePlayPauseButton();
            hideOverlay();
            updateLiveIndicator(true);
            showToast('Stream conectado (HLS)');
        }).catch(err => {
            console.warn('Auto-play bloqueado:', err);
            showToast('Haz clic en play para iniciar');
        });
    });

    player.on(Hls.Events.ERROR, (event, data) => {
        console.error('❌ Error HLS:', data.type, data.details);
        if (data.fatal) {
            showToast('Error HLS: ' + data.details);
            showOverlay();
            updateLiveIndicator(false);
        }
    });
}

// ============================================================================
// Manejo de errores y reconexión
// ============================================================================
function handleNetworkError() {
    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++;
        showToast(`Reconectando (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);

        setTimeout(() => {
            loadStream();
        }, 2000);
    } else {
        showToast('No se pudo conectar al stream');
        showOverlay();
        updateLiveIndicator(false);
        reconnectAttempts = 0;
    }
}

// ============================================================================
// Estadísticas
// ============================================================================
function startStatsUpdate() {
    if (statsInterval) clearInterval(statsInterval);

    statsInterval = setInterval(() => {
        updateStats();
    }, CONFIG.statsUpdateInterval);
}

function updateStats() {
    if (!player || !video) return;

    try {
        // Latencia
        if (video.buffered.length > 0) {
            const bufferedEnd = video.buffered.end(video.buffered.length - 1);
            const latency = bufferedEnd - video.currentTime;
            document.getElementById('latencyValue').textContent = latency.toFixed(1);
        }

        // FPS desde playback quality
        if (video.getVideoPlaybackQuality) {
            const quality = video.getVideoPlaybackQuality();
            if (quality.totalVideoFrames > 0 && video.currentTime > 0) {
                const fps = Math.round(quality.totalVideoFrames / video.currentTime);
                if (fps > 0 && fps < 120) {
                    document.getElementById('fpsValue').textContent = fps;
                }
            }
        }

        // Stats de mpegts.js
        if (player.statisticsInfo) {
            const stats = player.statisticsInfo;
            if (stats.speed) {
                const kbps = Math.round(stats.speed * 8 / 1000);
                document.getElementById('bitrateValue').textContent = kbps || '--';
            }
        }
    } catch (e) {
        // Ignorar errores de stats
    }
}

// ============================================================================
// Destruir player
// ============================================================================
function destroyPlayer() {
    if (statsInterval) {
        clearInterval(statsInterval);
        statsInterval = null;
    }

    if (player) {
        try {
            if (player.pause) player.pause();
            if (player.unload) player.unload();
            if (player.detachMediaElement) player.detachMediaElement();
            if (player.destroy) player.destroy();
        } catch (e) {
            console.warn('Error destruyendo player:', e);
        }
        player = null;
    }
}

// ============================================================================
// Event Listeners del Video
// ============================================================================
function setupVideoEventListeners() {
    video.addEventListener('play', () => {
        isPlaying = true;
        updatePlayPauseButton();
    });

    video.addEventListener('pause', () => {
        isPlaying = false;
        updatePlayPauseButton();
    });

    video.addEventListener('loadedmetadata', () => {
        const width = video.videoWidth;
        const height = video.videoHeight;
        if (width && height) {
            document.getElementById('resolutionValue').textContent = `${width}x${height}`;
        }
    });

    video.addEventListener('error', (e) => {
        console.error('Error de video:', e);
    });
}

// ============================================================================
// Controles del reproductor
// ============================================================================
function togglePlayPause() {
    if (video.paused) {
        video.play();
    } else {
        video.pause();
    }
}

function updatePlayPauseButton() {
    const playIcon = document.getElementById('playIcon');
    const pauseIcon = document.getElementById('pauseIcon');

    if (isPlaying) {
        playIcon.style.display = 'none';
        pauseIcon.style.display = 'block';
    } else {
        playIcon.style.display = 'block';
        pauseIcon.style.display = 'none';
    }
}

function toggleMute() {
    video.muted = !video.muted;
    document.getElementById('volumeSlider').value = video.muted ? 0 : video.volume * 100;
}

function changeVolume(value) {
    video.volume = value / 100;
    video.muted = value === 0;
}

function toggleFullscreen() {
    const playerContainer = document.querySelector('.player-container');

    if (!document.fullscreenElement) {
        playerContainer.requestFullscreen().catch(err => {
            console.warn('Error fullscreen:', err);
        });
    } else {
        document.exitFullscreen();
    }
}

// ============================================================================
// UI Updates
// ============================================================================
function showOverlay() {
    document.getElementById('videoOverlay').classList.remove('hidden');
}

function hideOverlay() {
    document.getElementById('videoOverlay').classList.add('hidden');
}

function updateLiveIndicator(isLive) {
    const indicator = document.getElementById('liveIndicator');
    const text = indicator.querySelector('.live-text');
    const streamStatus = document.getElementById('streamStatus');

    if (isLive) {
        indicator.classList.add('active');
        text.textContent = 'LIVE';
        if (streamStatus) streamStatus.textContent = 'Transmitiendo';
    } else {
        indicator.classList.remove('active');
        text.textContent = 'OFFLINE';
        if (streamStatus) streamStatus.textContent = 'Esperando...';
    }
}

function updateStreamUrls() {
    const streamKey = document.getElementById('streamKey').value || 'stream';
    const streamKeyDisplay = document.getElementById('streamKeyDisplay');
    if (streamKeyDisplay) {
        streamKeyDisplay.textContent = streamKey;
    }
}

// ============================================================================
// Server Status
// ============================================================================
async function checkServerStatus() {
    const statusElement = document.getElementById('serverStatus');
    const dot = statusElement.querySelector('.status-dot');
    const text = statusElement.querySelector('.status-text');

    try {
        const response = await fetch(`${CONFIG.mediaServerUrl}/api/server`, {
            method: 'GET',
            mode: 'cors',
        });

        if (response.ok) {
            dot.classList.remove('offline');
            dot.classList.add('online');
            text.textContent = 'Server Online';
        } else {
            throw new Error('Server not responding');
        }
    } catch (error) {
        dot.classList.remove('online');
        dot.classList.add('offline');
        text.textContent = 'Server Offline';
    }
}

// ============================================================================
// Utilidades
// ============================================================================
function copyToClipboard(elementId) {
    const element = document.getElementById(elementId);
    const text = element.textContent;

    navigator.clipboard.writeText(text)
        .then(() => showToast('Copiado al portapapeles'))
        .catch(err => showToast('Error al copiar'));
}

function showToast(message) {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');

    toastMessage.textContent = message;
    toast.classList.add('show');

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// ============================================================================
// Socket.IO - Tracking de Viewers en Tiempo Real
// ============================================================================
function initializeSocket() {
    try {
        // Conectar a Socket.IO
        socket = io({
            transports: ['websocket', 'polling']
        });

        socket.on('connect', () => {
            console.log('✅ Conectado a Socket.IO:', socket.id);
            // Unirse al stream actual
            joinStream(currentStreamKey);
        });

        socket.on('viewer-count', (data) => {
            console.log('📊 Actualización de viewers:', data);
            viewersCount = data.viewers || 0;
            peakViewersCount = data.peakViewers || 0;
            updateViewerDisplay();
        });

        // Listener de mensajes de chat
        socket.on('chat-message', displayChatMessage);

        socket.on('disconnect', () => {
            console.log('⚠️ Desconectado de Socket.IO');
        });

        socket.on('connect_error', (error) => {
            console.error('❌ Error de conexión Socket.IO:', error);
        });

    } catch (error) {
        console.error('❌ Error inicializando Socket.IO:', error);
    }
}

function joinStream(streamKey) {
    if (socket && socket.connected) {
        currentStreamKey = streamKey;
        socket.emit('join-stream', streamKey);
        console.log(`📺 Unido al stream: ${streamKey}`);
    }
}

function leaveStream(streamKey) {
    if (socket && socket.connected) {
        socket.emit('leave-stream', streamKey);
        console.log(`👋 Saliendo del stream: ${streamKey}`);
    }
}

function updateViewerDisplay() {
    const currentViewersElement = document.getElementById('currentViewers');
    const currentViewers2Element = document.getElementById('currentViewers2');
    const peakViewersElement = document.getElementById('peakViewersCount');

    if (currentViewersElement) {
        currentViewersElement.textContent = viewersCount;
    }

    if (currentViewers2Element) {
        currentViewers2Element.textContent = viewersCount;
    }

    if (peakViewersElement) {
        peakViewersElement.textContent = peakViewersCount;
    }
}

// ============================================================================
// Stream Status Check - Verificar si el stream está activo
// ============================================================================
async function checkStreamStatus() {
    const streamKey = document.getElementById('streamKey')?.value || 'stream';

    try {
        const response = await fetch(`/api/stream-status/${streamKey}`);
        const data = await response.json();

        if (data.isLive) {
            // Stream está activo
            updateLiveIndicator(true);
        } else {
            // Stream no está activo
            // Solo actualizar a offline si no estamos reproduciendo
            if (!isPlaying) {
                updateLiveIndicator(false);
            }
        }
    } catch (error) {
        console.error('Error verificando estado del stream:', error);
    }
}

// ============================================================================
// Chat Functions
// ============================================================================
function initializeChat() {
    // Cargar username guardado
    const usernameInput = document.getElementById('usernameInput');
    if (username) {
        usernameInput.value = username;
    }

    // Guardar username en localStorage cuando cambie
    usernameInput.addEventListener('change', (e) => {
        username = e.target.value.trim();
        localStorage.setItem('streaming_username', username);
    });

    // Enviar mensaje con Enter
    const chatInput = document.getElementById('chatInput');
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            sendChatMessage();
        }
    });

    console.log('💬 Chat inicializado');
}

function sendChatMessage() {
    const usernameInput = document.getElementById('usernameInput');
    const chatInput = document.getElementById('chatInput');
    const message = chatInput.value.trim();
    const user = usernameInput.value.trim() || 'Anónimo';

    if (!message) {
        return;
    }

    if (!socket || !socket.connected) {
        showToast('No estás conectado al servidor');
        return;
    }

    // Actualizar username en localStorage
    username = user;
    localStorage.setItem('streaming_username', username);

    // Enviar mensaje al servidor
    socket.emit('chat-message', {
        streamKey: currentStreamKey,
        username: user,
        message: message
    });

    // Limpiar input
    chatInput.value = '';
}

function displayChatMessage(data) {
    const chatMessages = document.getElementById('chatMessages');

    // Remover mensaje de bienvenida si existe
    const welcome = chatMessages.querySelector('.chat-welcome');
    if (welcome) {
        welcome.remove();
    }

    // Crear elemento de mensaje
    const messageElement = document.createElement('div');
    messageElement.className = 'chat-message';

    const usernameElement = document.createElement('div');
    usernameElement.className = 'chat-message-username';
    usernameElement.textContent = data.username;

    const textElement = document.createElement('div');
    textElement.className = 'chat-message-text';
    textElement.textContent = data.message;

    const timeElement = document.createElement('div');
    timeElement.className = 'chat-message-time';
    const time = new Date(data.timestamp);
    timeElement.textContent = time.toLocaleTimeString('es-ES', {
        hour: '2-digit',
        minute: '2-digit'
    });

    messageElement.appendChild(usernameElement);
    messageElement.appendChild(textElement);
    messageElement.appendChild(timeElement);

    // Agregar mensaje al contenedor
    chatMessages.appendChild(messageElement);

    // Scroll automático hacia abajo
    chatMessages.scrollTop = chatMessages.scrollHeight;

    // Limitar cantidad de mensajes (máximo 100)
    const messages = chatMessages.querySelectorAll('.chat-message');
    if (messages.length > 100) {
        messages[0].remove();
    }
}

/**
 * Descargar el historial de chat completo en formato TXT
 */
async function downloadChatHistory() {
    const streamKey = document.getElementById('streamKey').value || 'stream';

    try {
        showToast('Descargando historial del chat...');

        // Primero verificar si hay mensajes
        const checkResponse = await fetch(`/api/chat/${streamKey}`);
        const chatData = await checkResponse.json();

        if (!chatData.messages || chatData.messages.length === 0) {
            showToast('No hay mensajes en el historial de este stream');
            return;
        }

        // Descargar el archivo TXT
        const downloadUrl = `/api/chat/${streamKey}/download`;

        // Crear un elemento <a> temporal para descargar el archivo
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `chat_${streamKey}_${Date.now()}.txt`;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        showToast(`✅ Descargando ${chatData.stats.totalMessages} mensajes`);
        console.log(`📥 Chat descargado: ${streamKey} (${chatData.stats.totalMessages} mensajes)`);

    } catch (error) {
        console.error('Error descargando historial de chat:', error);
        showToast('❌ Error al descargar el historial del chat');
    }
}

// ============================================================================
// Export para debugging
// ============================================================================
window.streamingApp = {
    loadStream,
    checkServerStatus,
    checkStreamStatus,
    sendChatMessage,
    downloadChatHistory,
    player: () => player,
    video: () => video,
    config: CONFIG,
    socket: () => socket,
    viewers: () => ({ current: viewersCount, peak: peakViewersCount })
};
