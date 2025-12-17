/**
 * ============================================================================
 * SERVIDOR WEB - Plataforma de Visualización de Streaming
 * ============================================================================
 * 
 * Servidor Express que sirve la interfaz web para visualizar streams HLS
 * 
 * Puerto: 3000
 * 
 * ============================================================================
 */

const express = require('express');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// API endpoint para obtener configuración
app.get('/api/config', (req, res) => {
    res.json({
        mediaServerUrl: 'http://localhost:8000',
        rtmpUrl: 'rtmp://localhost:1935/live',
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

// Ruta principal
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Iniciar servidor
app.listen(PORT, () => {
    console.log('');
    console.log('╔════════════════════════════════════════════════════════════════╗');
    console.log('║          🌐 PLATAFORMA WEB DE STREAMING                        ║');
    console.log('╠════════════════════════════════════════════════════════════════╣');
    console.log('║                                                                ║');
    console.log(`║  🖥️  Web App:        http://localhost:${PORT}                      ║`);
    console.log('║                                                                ║');
    console.log('║  Asegúrate de que el Media Server esté corriendo:             ║');
    console.log('║  npm run media-server                                         ║');
    console.log('║                                                                ║');
    console.log('╚════════════════════════════════════════════════════════════════╝');
    console.log('');
});
