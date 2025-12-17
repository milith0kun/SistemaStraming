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
