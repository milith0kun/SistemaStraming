# Sistema de Streaming IoT

Sistema completo de streaming de video en tiempo real desde dispositivos Android a una plataforma web.

## 📱 Componentes

### 1. App Android (`/Streaming`)
Aplicación Android nativa con Jetpack Compose para transmitir video en vivo usando RTMP.

**Características:**
- 📷 Transmisión de cámara en tiempo real
- 🎤 Audio incluido
- 🔄 Cambio de cámara frontal/trasera
- ⚙️ Configuración de calidad (480p, 720p, 1080p)
- 📊 Estadísticas en tiempo real (bitrate, fps, duración)

### 2. Plataforma Web (`/PlataformaSteaming`)
Servidor Node.js con Node Media Server para recibir streams RTMP y visualizarlos en web.

**Características:**
- 🖥️ Interfaz web moderna con glassmorphism
- 📡 Servidor RTMP (puerto 1935)
- 🌐 HTTP-FLV para baja latencia
- 📊 Estadísticas en tiempo real
- 🎬 Reproductor con controles completos

## 🚀 Instalación

### Requisitos
- Node.js 18+
- Android Studio (para la app)
- Dispositivo Android con cámara

### Servidor Web
```bash
cd PlataformaSteaming
npm install
npm start
```

### App Android
1. Abre `/Streaming` en Android Studio
2. Conecta tu dispositivo Android
3. Ejecuta la app

## ⚙️ Configuración

### En la App Android
- **URL RTMP**: `rtmp://TU_IP_PC:1935/live`
- **Stream Key**: `stream`

### Puertos
- **1935**: Servidor RTMP (recibe stream de Android)
- **8000**: Media Server HTTP (FLV)
- **3000**: Plataforma Web

## 📖 Uso

1. Inicia el servidor:
   ```bash
   cd PlataformaSteaming
   npm start
   ```

2. Abre la web: `http://localhost:3000`

3. En la app Android:
   - Configura la IP de tu PC
   - Presiona el botón rojo para transmitir

4. En la web, haz clic en "Conectar al Stream"

## 🛠️ Tecnologías

### Android
- Kotlin
- Jetpack Compose
- RootEncoder (RTMP)
- CameraX

### Web
- Node.js
- Node Media Server
- mpegts.js / HLS.js
- CSS con glassmorphism

## 📄 Licencia

MIT License
