/**
 * ============================================================================
 * SERVIDOR DE STREAMING LOCAL - Node Media Server
 * ============================================================================
 * 
 * Este servidor maneja:
 * - Ingest RTMP (recibe stream desde Android) en puerto 1935
 * - Transmux a HLS para reproducción web
 * - HTTP-FLV para baja latencia
 * 
 * Endpoints:
 * - RTMP Ingest: rtmp://localhost:1935/live/{stream-key}
 * - HLS Playback: http://localhost:8000/live/{stream-key}/index.m3u8
 * - HTTP-FLV: http://localhost:8000/live/{stream-key}.flv
 * 
 * ============================================================================
 */

const NodeMediaServer = require('node-media-server');
const path = require('path');
const fs = require('fs');

// Crear directorio para archivos HLS
const mediaRoot = path.join(__dirname, 'media');
if (!fs.existsSync(mediaRoot)) {
    fs.mkdirSync(mediaRoot, { recursive: true });
}

const config = {
    rtmp: {
        port: 1935,
        chunk_size: 60000,
        gop_cache: true,
        ping: 30,
        ping_timeout: 60
    },
    http: {
        port: 8000,
        mediaroot: mediaRoot,
        allow_origin: '*',
        api: true
    },
    trans: {
        ffmpeg: 'ffmpeg', // Asegúrate de tener FFmpeg instalado
        tasks: [
            {
                app: 'live',
                hls: true,
                hlsFlags: '[hls_time=2:hls_list_size=3:hls_flags=delete_segments]',
                hlsKeep: false, // Limpiar segmentos cuando el stream termine
                dash: false,
                mp4: false
            }
        ]
    },
    auth: {
        api: false, // Sin autenticación para desarrollo local
        publish: false,
        play: false
    }
};

const nms = new NodeMediaServer(config);

// Eventos del servidor
nms.on('preConnect', (id, args) => {
    console.log('[NodeMediaServer] Cliente conectándose:', id, args);
});

nms.on('postConnect', (id, args) => {
    console.log('[NodeMediaServer] Cliente conectado:', id);
});

nms.on('doneConnect', (id, args) => {
    console.log('[NodeMediaServer] Cliente desconectado:', id);
});

nms.on('prePublish', (id, StreamPath, args) => {
    console.log('[NodeMediaServer] 📡 Stream iniciando:', StreamPath);
    console.log('[NodeMediaServer] Stream Key:', StreamPath.split('/').pop());
});

nms.on('postPublish', (id, StreamPath, args) => {
    console.log('[NodeMediaServer] ✅ Stream activo:', StreamPath);
    console.log('');
    console.log('╔════════════════════════════════════════════════════════════════╗');
    console.log('║                    🎬 STREAM EN VIVO                           ║');
    console.log('╠════════════════════════════════════════════════════════════════╣');
    console.log('║  HLS URL:                                                      ║');
    console.log(`║  http://localhost:8000${StreamPath}/index.m3u8`);
    console.log('║                                                                ║');
    console.log('║  HTTP-FLV URL (baja latencia):                                 ║');
    console.log(`║  http://localhost:8000${StreamPath}.flv`);
    console.log('║                                                                ║');
    console.log('║  Abre la plataforma web en: http://localhost:3000              ║');
    console.log('╚════════════════════════════════════════════════════════════════╝');
    console.log('');
});

nms.on('donePublish', (id, StreamPath, args) => {
    console.log('[NodeMediaServer] ❌ Stream finalizado:', StreamPath);
});

nms.on('prePlay', (id, StreamPath, args) => {
    console.log('[NodeMediaServer] 👀 Viewer conectando:', StreamPath);
});

nms.on('postPlay', (id, StreamPath, args) => {
    console.log('[NodeMediaServer] 👁️ Viewer viendo:', StreamPath);
});

nms.on('donePlay', (id, StreamPath, args) => {
    console.log('[NodeMediaServer] 👋 Viewer desconectado:', StreamPath);
});

// Iniciar servidor
nms.run();

console.log('');
console.log('╔════════════════════════════════════════════════════════════════╗');
console.log('║          🚀 SERVIDOR DE STREAMING INICIADO                     ║');
console.log('╠════════════════════════════════════════════════════════════════╣');
console.log('║                                                                ║');
console.log('║  📡 RTMP Server:    rtmp://localhost:1935/live                 ║');
console.log('║  🌐 HTTP Server:    http://localhost:8000                      ║');
console.log('║  📊 API Status:     http://localhost:8000/api/server           ║');
console.log('║                                                                ║');
console.log('║  Para transmitir desde Android:                                ║');
console.log('║  URL: rtmp://TU_IP_LOCAL:1935/live                             ║');
console.log('║  Stream Key: stream (o cualquier nombre)                       ║');
console.log('║                                                                ║');
console.log('║  Para ver el stream:                                           ║');
console.log('║  http://localhost:8000/live/stream/index.m3u8                  ║');
console.log('║                                                                ║');
console.log('╚════════════════════════════════════════════════════════════════╝');
console.log('');
console.log('Esperando conexiones...');
