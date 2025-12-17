/**
 * ============================================================================
 * SERVIDOR WEB - Plataforma de Visualización de Streaming
 * ============================================================================
 *
 * Servidor Express con Socket.IO para viewers en tiempo real
 *
 * Puerto: 3000
 *
 * ============================================================================
 */

const express = require('express');
const cors = require('cors');
const path = require('path');
const http = require('http');
const { Server } = require('socket.io');
const db = require('./db');

const app = express();
const PORT = 3000;
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// Tracking de viewers por stream
const streamViewers = new Map(); // Map<streamKey, Set<socketId>>
const streamStats = new Map();  // Map<streamKey, {viewers, peakViewers, startTime}>

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// API endpoint para obtener configuración
app.get('/api/config', (req, res) => {
    res.json({
        mediaServerUrl: 'https://streamingpe.myvnc.com',
        rtmpUrl: 'rtmp://3.134.159.236:1935/live',
        defaultStreamKey: 'stream'
    });
});

// API endpoint para verificar estado del servidor de medios
app.get('/api/status', async (req, res) => {
    try {
        const response = await fetch('http://localhost:8000/api/server');
        const data = await response.json();
        res.json({ status: 'online', mediaServer: data });
    } catch (error) {
        res.json({ status: 'offline', error: 'Media server not running' });
    }
});

// API endpoint para verificar si un stream específico está activo
app.get('/api/stream-status/:streamKey', async (req, res) => {
    const { streamKey } = req.params;
    try {
        const response = await fetch('http://localhost:8000/api/streams');
        const data = await response.json();

        // Verificar si el stream está en la lista de streams activos
        const streamPath = `/live/${streamKey}`;
        const isLive = data && data.live && data.live[streamPath];

        res.json({
            streamKey,
            isLive: !!isLive,
            streamData: isLive || null
        });
    } catch (error) {
        res.json({
            streamKey,
            isLive: false,
            error: 'Could not check stream status'
        });
    }
});

// API endpoint para obtener estadísticas de viewers
app.get('/api/viewers/:streamKey?', (req, res) => {
    const { streamKey } = req.params;

    if (streamKey) {
        const viewers = streamViewers.get(streamKey)?.size || 0;
        const stats = streamStats.get(streamKey) || {
            viewers: 0,
            peakViewers: 0,
            startTime: null
        };
        res.json({ streamKey, viewers, ...stats });
    } else {
        // Retornar estadísticas de todos los streams
        const allStats = {};
        for (const [key, value] of streamStats.entries()) {
            allStats[key] = {
                ...value,
                viewers: streamViewers.get(key)?.size || 0
            };
        }
        res.json(allStats);
    }
});

// API endpoint para obtener historial de chat
app.get('/api/chat/:streamKey', (req, res) => {
    const { streamKey } = req.params;
    const { limit, offset, startDate, endDate } = req.query;

    try {
        const options = {
            limit: limit ? parseInt(limit) : 1000,
            offset: offset ? parseInt(offset) : 0,
            startDate: startDate ? parseInt(startDate) : undefined,
            endDate: endDate ? parseInt(endDate) : undefined
        };

        const messages = db.getChatHistory(streamKey, options);
        const stats = db.getChatStats(streamKey);

        res.json({
            streamKey,
            stats: {
                totalMessages: stats.total_messages || 0,
                uniqueUsers: stats.unique_users || 0,
                firstMessage: stats.first_message,
                lastMessage: stats.last_message
            },
            messages: messages.map(msg => ({
                id: msg.message_id,
                username: msg.username,
                message: msg.message,
                timestamp: msg.timestamp
            }))
        });
    } catch (error) {
        console.error('[API] Error obteniendo historial de chat:', error);
        res.status(500).json({ error: 'Error al obtener historial de chat' });
    }
});

// API endpoint para descargar historial de chat en formato TXT
app.get('/api/chat/:streamKey/download', (req, res) => {
    const { streamKey } = req.params;

    try {
        const messages = db.getChatHistory(streamKey, { limit: 100000 });
        const stats = db.getChatStats(streamKey);

        if (!messages || messages.length === 0) {
            return res.status(404).send('No hay mensajes para este stream');
        }

        // Generar contenido TXT
        let txtContent = '═══════════════════════════════════════════════════════════════\n';
        txtContent += `  HISTORIAL DE CHAT - Stream: ${streamKey}\n`;
        txtContent += '═══════════════════════════════════════════════════════════════\n\n';
        txtContent += `Total de mensajes: ${stats.total_messages || 0}\n`;
        txtContent += `Usuarios únicos: ${stats.unique_users || 0}\n`;

        if (stats.first_message) {
            const firstDate = new Date(stats.first_message);
            const lastDate = new Date(stats.last_message);
            txtContent += `Primer mensaje: ${firstDate.toLocaleString('es-ES')}\n`;
            txtContent += `Último mensaje: ${lastDate.toLocaleString('es-ES')}\n`;
        }

        txtContent += '\n═══════════════════════════════════════════════════════════════\n';
        txtContent += '  MENSAJES\n';
        txtContent += '═══════════════════════════════════════════════════════════════\n\n';

        // Agregar cada mensaje
        messages.forEach((msg, index) => {
            const date = new Date(msg.timestamp);
            const timeStr = date.toLocaleTimeString('es-ES', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
            const dateStr = date.toLocaleDateString('es-ES');

            txtContent += `[${dateStr} ${timeStr}] ${msg.username}: ${msg.message}\n`;
        });

        txtContent += '\n═══════════════════════════════════════════════════════════════\n';
        txtContent += `  FIN DEL HISTORIAL - ${messages.length} mensajes\n`;
        txtContent += '═══════════════════════════════════════════════════════════════\n';

        // Configurar headers para descarga
        const filename = `chat_${streamKey}_${Date.now()}.txt`;
        res.setHeader('Content-Type', 'text/plain; charset=utf-8');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
        res.send(txtContent);

        console.log(`[API] Chat descargado: ${streamKey} (${messages.length} mensajes)`);
    } catch (error) {
        console.error('[API] Error descargando chat:', error);
        res.status(500).json({ error: 'Error al descargar historial de chat' });
    }
});

// API endpoint para obtener todos los streams con chat
app.get('/api/streams-with-chat', (req, res) => {
    try {
        const streams = db.getAllStreamsWithChat();
        res.json({
            totalStreams: streams.length,
            streams: streams.map(stream => ({
                streamKey: stream.stream_key,
                messageCount: stream.message_count,
                uniqueUsers: stream.unique_users,
                firstMessage: stream.first_message,
                lastMessage: stream.last_message,
                firstMessageDate: new Date(stream.first_message).toISOString(),
                lastMessageDate: new Date(stream.last_message).toISOString()
            }))
        });
    } catch (error) {
        console.error('[API] Error obteniendo streams:', error);
        res.status(500).json({ error: 'Error al obtener streams' });
    }
});

// Socket.IO - Manejo de conexiones de viewers
io.on('connection', (socket) => {
    console.log(`[Socket.IO] Cliente conectado: ${socket.id}`);

    // Cuando un viewer se une a un stream
    socket.on('join-stream', (streamKey) => {
        console.log(`[Socket.IO] ${socket.id} se unió a stream: ${streamKey}`);

        // Agregar viewer al stream
        if (!streamViewers.has(streamKey)) {
            streamViewers.set(streamKey, new Set());
        }
        streamViewers.get(streamKey).add(socket.id);

        // Inicializar estadísticas si no existen
        if (!streamStats.has(streamKey)) {
            streamStats.set(streamKey, {
                viewers: 0,
                peakViewers: 0,
                startTime: Date.now()
            });
        }

        // Actualizar estadísticas
        const currentViewers = streamViewers.get(streamKey).size;
        const stats = streamStats.get(streamKey);
        stats.viewers = currentViewers;
        if (currentViewers > stats.peakViewers) {
            stats.peakViewers = currentViewers;
        }

        // Unirse a la sala del stream
        socket.join(streamKey);

        // Notificar a todos los viewers del stream
        io.to(streamKey).emit('viewer-count', {
            streamKey,
            viewers: currentViewers,
            peakViewers: stats.peakViewers
        });
    });

    // Cuando un viewer sale de un stream
    socket.on('leave-stream', (streamKey) => {
        handleViewerLeave(socket.id, streamKey);
    });

    // Chat - Cuando un usuario envía un mensaje
    socket.on('chat-message', (data) => {
        const { streamKey, username, message } = data;
        console.log(`[Chat] ${username} en ${streamKey}: ${message}`);

        // Validar datos
        if (!streamKey || !username || !message || message.trim().length === 0) {
            return;
        }

        // Prevenir mensajes muy largos
        const sanitizedMessage = message.substring(0, 500);
        const sanitizedUsername = username.substring(0, 50);
        const messageId = Date.now() + socket.id;
        const timestamp = Date.now();

        // Guardar mensaje en la base de datos
        db.saveChatMessage({
            messageId: messageId,
            streamKey: streamKey,
            username: sanitizedUsername,
            message: sanitizedMessage,
            timestamp: timestamp
        });

        // Emitir mensaje a todos en el stream
        io.to(streamKey).emit('chat-message', {
            id: messageId,
            username: sanitizedUsername,
            message: sanitizedMessage,
            timestamp: timestamp
        });
    });

    // Cuando un viewer se desconecta
    socket.on('disconnect', () => {
        console.log(`[Socket.IO] Cliente desconectado: ${socket.id}`);

        // Remover de todos los streams
        for (const [streamKey, viewers] of streamViewers.entries()) {
            if (viewers.has(socket.id)) {
                handleViewerLeave(socket.id, streamKey);
            }
        }
    });
});

function handleViewerLeave(socketId, streamKey) {
    if (streamViewers.has(streamKey)) {
        streamViewers.get(streamKey).delete(socketId);
        const currentViewers = streamViewers.get(streamKey).size;

        // Actualizar estadísticas
        if (streamStats.has(streamKey)) {
            streamStats.get(streamKey).viewers = currentViewers;
        }

        // Notificar a todos los viewers
        io.to(streamKey).emit('viewer-count', {
            streamKey,
            viewers: currentViewers,
            peakViewers: streamStats.get(streamKey)?.peakViewers || 0
        });

        // Limpiar si no hay viewers
        if (currentViewers === 0) {
            streamViewers.delete(streamKey);
        }
    }
}

// Ruta principal
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Iniciar servidor en todas las interfaces
server.listen(PORT, '0.0.0.0', () => {
    console.log('');
    console.log('╔════════════════════════════════════════════════════════════════╗');
    console.log('║          🌐 PLATAFORMA WEB DE STREAMING                        ║');
    console.log('╠════════════════════════════════════════════════════════════════╣');
    console.log('║                                                                ║');
    console.log(`║  🖥️  Web App:        https://streamingpe.myvnc.com             ║`);
    console.log(`║  🌍  Puerto local:   ${PORT}                                      ║`);
    console.log('║  🔒  HTTPS:          Habilitado                                ║');
    console.log('║  📊  Viewers:        Tracking en tiempo real                   ║');
    console.log('║                                                                ║');
    console.log('║  Asegúrate de que el Media Server esté corriendo:             ║');
    console.log('║  npm run media-server                                         ║');
    console.log('║                                                                ║');
    console.log('╚════════════════════════════════════════════════════════════════╝');
    console.log('');
});
