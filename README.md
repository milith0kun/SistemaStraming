# 🎬 Sistema de Streaming IoT

[![Android](https://img.shields.io/badge/Android-Kotlin-green?logo=android)](https://developer.android.com/)
[![Node.js](https://img.shields.io/badge/Node.js-18+-green?logo=node.js)](https://nodejs.org/)
[![RTMP](https://img.shields.io/badge/Protocol-RTMP-red)](https://en.wikipedia.org/wiki/Real-Time_Messaging_Protocol)
[![AWS](https://img.shields.io/badge/Cloud-AWS-orange?logo=amazon-aws)](https://aws.amazon.com/)

Sistema completo de streaming de video en vivo desde dispositivos Android embebidos hacia un servidor en AWS, utilizando arquitectura escalable basada en **RTMP** para ingest y **HTTP-FLV/HLS** para delivery web.

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Arquitectura](#-arquitectura)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Requisitos](#-requisitos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Uso](#-uso)
- [API Reference](#-api-reference)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Contribución](#-contribución)
- [Licencia](#-licencia)

---

## 📖 Descripción

### Objetivo General

Implementar un sistema de streaming de video en vivo desde un dispositivo Android embebido hacia un servidor en AWS (Ubuntu), utilizando arquitectura escalable basada en RTMP para ingest y HLS/HTTP-FLV para delivery web, demostrando principios de sistemas embebidos en IoT y procesamiento multimedia distribuido.

### Objetivos Específicos

1. ✅ **Servidor de Streaming Escalable**: Configurar un servidor de streaming escalable en AWS utilizando Node Media Server con soporte RTMP.

2. ✅ **Aplicación Android**: Desarrollar una aplicación Android que capture video de la cámara y lo transmita vía RTMP.

3. ✅ **Reproducción Multi-cliente**: Verificar la reproducción del stream en múltiples clientes web simultáneamente.

4. ✅ **Optimización de Ancho de Banda**: Entender las limitaciones de ancho de banda en dispositivos embebidos y la ventaja de servidores en la nube para distribución masiva.

5. ✅ **Codificación Multimedia**: Aplicar conceptos de codificación multimedia (H.264/AAC), protocolos de red y escalabilidad en sistemas embebidos.

---

## 🏗 Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ARQUITECTURA DEL SISTEMA                            │
└─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────┐         RTMP          ┌─────────────────┐       HTTP-FLV
  │   📱        │    ───────────────►   │    ☁️ AWS       │    ─────────────►  👁️ Viewer 1
  │   Android   │    Puerto 1935        │  Ubuntu Server  │                    👁️ Viewer 2
  │   Device    │                       │                 │       HLS          👁️ Viewer 3
  │             │    H.264 + AAC        │  Node Media     │    ─────────────►  👁️ Viewer N
  │  RootEncoder│                       │  Server         │    
  └─────────────┘                       └─────────────────┘
                                               │
                                               │ Puerto 8000 (Media)
                                               │ Puerto 3000 (Web)
                                               ▼
                                        ┌─────────────────┐
                                        │  🌐 Web Player  │
                                        │  mpegts.js      │
                                        │  HLS.js         │
                                        └─────────────────┘
```

### Flujo de Datos

1. **Captura (Android)**: La cámara captura video en tiempo real
2. **Codificación (Android)**: RootEncoder codifica en H.264 (video) y AAC (audio)
3. **Transmisión (RTMP)**: El stream se envía al servidor vía RTMP (puerto 1935)
4. **Recepción (AWS)**: Node Media Server recibe el stream RTMP
5. **Distribución (HTTP-FLV/HLS)**: El servidor distribuye a múltiples clientes
6. **Reproducción (Web)**: mpegts.js reproduce el stream en navegadores

---

## ✨ Características

### 📱 Aplicación Android

| Característica | Descripción |
|----------------|-------------|
| 📷 **Captura de Video** | Acceso a cámara frontal y trasera |
| 🔄 **Cambio de Cámara** | Switch entre cámara frontal/trasera en tiempo real |
| 🎤 **Audio** | Captura de audio estéreo con AAC |
| ⚙️ **Presets de Calidad** | LOW (480p), MEDIUM (720p), HIGH (720p@30fps), ULTRA (1080p) |
| 📊 **Estadísticas** | Bitrate, FPS, duración, estado de conexión |
| 🔇 **Control de Audio** | Silenciar/activar micrófono |
| 🔴 **Indicador Visual** | Estado de transmisión en tiempo real |
| 🌐 **URL Configurable** | RTMP URL y Stream Key personalizables |

### 🖥️ Servidor de Streaming

| Característica | Descripción |
|----------------|-------------|
| 📡 **RTMP Ingest** | Recepción de streams desde múltiples fuentes |
| 🎬 **HTTP-FLV** | Baja latencia para reproducción web |
| 📺 **HLS** | Alta compatibilidad con navegadores (requiere FFmpeg) |
| 📊 **API REST** | Información del servidor y streams activos |
| 🔐 **CORS** | Soporte para aplicaciones web externas |
| 📈 **Escalable** | Arquitectura lista para múltiples viewers |

### 🌐 Plataforma Web

| Característica | Descripción |
|----------------|-------------|
| 🎨 **UI Moderna** | Diseño glassmorphism con animaciones |
| ▶️ **Controles** | Play/Pause, Volumen, Pantalla completa |
| 📊 **Estadísticas** | Bitrate, Resolución, Latencia, FPS |
| 🔴 **Indicador LIVE** | Estado de transmisión en tiempo real |
| 📱 **Responsive** | Adaptable a móviles y desktop |
| 📋 **Documentación** | Guía integrada de configuración |

---

## 🛠 Tecnologías

### Android (Cliente de Transmisión)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Kotlin** | 2.0.21 | Lenguaje de programación |
| **Jetpack Compose** | Latest | UI declarativa |
| **RootEncoder** | 2.2.6 | Transmisión RTMP |
| **CameraX** | - | Captura de cámara |
| **Kotlin Coroutines** | - | Programación asíncrona |

### Servidor (AWS Ubuntu)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Node.js** | 18+ | Runtime de JavaScript |
| **Node Media Server** | 2.7.4 | Servidor RTMP/HTTP-FLV |
| **Express** | 4.x | Servidor web |
| **FFmpeg** | (Opcional) | Transcodificación HLS |

### Frontend (Reproductor Web)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **mpegts.js** | 1.7.3 | Reproductor HTTP-FLV |
| **HLS.js** | 1.4.12 | Reproductor HLS |
| **Vanilla CSS** | - | Estilos modernos |
| **ES6+ JavaScript** | - | Lógica del reproductor |

### Protocolos y Codecs

| Componente | Tecnología |
|------------|------------|
| **Protocolo Ingest** | RTMP (Real-Time Messaging Protocol) |
| **Protocolo Delivery** | HTTP-FLV, HLS |
| **Video Codec** | H.264 (AVC) |
| **Audio Codec** | AAC |
| **Container** | FLV, M3U8/TS |

---

## 📋 Requisitos

### Para el Servidor (AWS)

- Ubuntu 20.04 LTS o superior
- Node.js 18+
- npm 8+
- FFmpeg (opcional, para HLS)
- Puertos abiertos: 1935 (RTMP), 8000 (Media), 3000 (Web)

### Para la App Android

- Android Studio Ladybug 2024.2.1+
- JDK 17
- Android SDK 24+ (min) / 34 (target)
- Dispositivo Android con cámara

### Para Desarrollo Local

- Git
- Node.js 18+
- Android Studio con emulador o dispositivo físico
- ADB (Android Debug Bridge)

---

## 🚀 Instalación

### 1. Clonar el Repositorio

```bash
git clone https://github.com/milith0kun/SistemaStraming.git
cd SistemaStraming
```

### 2. Configurar el Servidor

```bash
cd PlataformaSteaming
npm install
```

### 3. Compilar la App Android

```bash
cd ../Streaming
./gradlew assembleDebug
```

### 4. Instalar en Dispositivo

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Configuración

### Configuración del Servidor

El servidor usa las siguientes configuraciones por defecto:

| Puerto | Servicio | Descripción |
|--------|----------|-------------|
| 1935 | RTMP | Ingest de streams |
| 8000 | HTTP Media | HTTP-FLV y HLS |
| 3000 | Web | Frontend del reproductor |

Para modificar, editar `PlataformaSteaming/media-server.js`:

```javascript
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
    }
};
```

### Configuración de la App Android

Editar `StreamConfig.kt`:

```kotlin
data class StreamConfig(
    val rtmpUrl: String = "rtmp://streamingpe.myvnc.com:1935/live",
    val streamKey: String = "stream",
    
    // Video
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val videoBitrate: Int = 2500000,  // 2.5 Mbps
    val videoFps: Int = 30,
    
    // Audio
    val audioBitrate: Int = 128000,   // 128 Kbps
    val audioSampleRate: Int = 44100,
    val audioIsStereo: Boolean = true
)
```

### Presets de Calidad Disponibles

| Preset | Resolución | FPS | Video Bitrate | Audio Bitrate |
|--------|------------|-----|---------------|---------------|
| LOW | 854x480 | 15 | 500 Kbps | 64 Kbps |
| MEDIUM | 1280x720 | 24 | 1.5 Mbps | 96 Kbps |
| HIGH | 1280x720 | 30 | 2.5 Mbps | 128 Kbps |
| ULTRA | 1920x1080 | 30 | 4 Mbps | 128 Kbps |

---

## 📖 Uso

### 1. Iniciar el Servidor

```bash
cd PlataformaSteaming

# Iniciar servidor de medios
npm run media-server

# En otra terminal, iniciar servidor web
npm start
```

### 2. Acceder a la Plataforma Web

Abrir en el navegador:
- **Local**: http://localhost:3000
- **Producción**: https://streamingpe.myvnc.com

### 3. Configurar y Transmitir desde Android

1. Abrir la app **Streaming**
2. Verificar la URL RTMP en configuración (⚙️)
3. Seleccionar calidad de video
4. Presionar el **botón rojo** para iniciar transmisión

### 4. Ver el Stream

En la plataforma web:
1. Ingresar el Stream Key (por defecto: `stream`)
2. Hacer clic en **"Conectar al Stream"**

---

## 📚 API Reference

### Endpoints del Servidor

#### GET /api/server
Información del servidor de streaming.

**Response:**
```json
{
  "os": {
    "arch": "x64",
    "platform": "linux",
    "release": "5.15.0"
  },
  "cpu": {...},
  "mem": {...},
  "net": {...},
  "nodejs": {...},
  "version": "2.7.4"
}
```

#### GET /api/streams
Lista de streams activos.

**Response:**
```json
{
  "live": {
    "stream": {
      "publisher": {
        "app": "live",
        "stream": "stream",
        "clientId": "...",
        "connectCreated": "2024-12-17T03:00:00.000Z",
        "video": {
          "codec": "H264",
          "width": 1280,
          "height": 720,
          "fps": 30
        },
        "audio": {
          "codec": "AAC",
          "samplerate": 44100,
          "channels": 2
        }
      },
      "subscribers": []
    }
  }
}
```

### URLs de Streaming

| Tipo | URL |
|------|-----|
| **RTMP Ingest** | `rtmp://streamingpe.myvnc.com:1935/live/stream` |
| **HTTP-FLV** | `http://streamingpe.myvnc.com:8000/live/stream.flv` |
| **HLS** | `http://streamingpe.myvnc.com:8000/live/stream/index.m3u8` |

---

## 📁 Estructura del Proyecto

```
SistemaStreaming/
│
├── README.md                           # Este archivo
├── .gitignore                          # Ignorar archivos de build
│
├── Streaming/                          # 📱 Aplicación Android
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/example/streaming/
│   │   │   │   ├── MainActivity.kt              # Actividad principal
│   │   │   │   ├── streaming/
│   │   │   │   │   ├── StreamManager.kt         # Lógica RTMP
│   │   │   │   │   └── StreamConfig.kt          # Configuración
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   └── StreamingScreen.kt   # UI principal
│   │   │   │       └── theme/
│   │   │   │           └── Theme.kt             # Tema visual
│   │   │   ├── res/                             # Recursos
│   │   │   └── AndroidManifest.xml              # Permisos
│   │   └── build.gradle.kts                     # Config de app
│   ├── gradle/
│   │   └── libs.versions.toml                   # Versiones
│   ├── build.gradle.kts                         # Config raíz
│   ├── settings.gradle.kts
│   ├── gradlew                                  # Gradle wrapper
│   └── gradlew.bat
│
└── PlataformaSteaming/                 # 🖥️ Servidor Web
    ├── server.js                       # Express server (puerto 3000)
    ├── media-server.js                 # Node Media Server (RTMP)
    ├── package.json                    # Dependencias
    ├── package-lock.json
    └── public/
        ├── index.html                  # Frontend
        ├── app.js                      # Lógica del reproductor
        └── styles.css                  # Estilos CSS
```

---

## 🔒 Seguridad

### Permisos de la App Android

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Configuración AWS Security Group

| Puerto | Protocolo | Fuente | Descripción |
|--------|-----------|--------|-------------|
| 22 | TCP | Tu IP | SSH |
| 1935 | TCP | 0.0.0.0/0 | RTMP Ingest |
| 8000 | TCP | 0.0.0.0/0 | HTTP Media |
| 3000 | TCP | 0.0.0.0/0 | Web Frontend |
| 80 | TCP | 0.0.0.0/0 | HTTP (Nginx) |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

---

## 🐛 Troubleshooting

### Error: "No se puede conectar al servidor RTMP"

1. Verificar que el servidor esté corriendo
2. Comprobar que el puerto 1935 esté abierto
3. Verificar la URL RTMP en la app

### Error: "Stream no visible en web"

1. Verificar que la app esté transmitiendo (indicador rojo)
2. Comprobar el Stream Key
3. Revisar logs del servidor: `npm run media-server`

### Error de compilación Android

```bash
# Limpiar y recompilar
./gradlew clean assembleDebug
```

---

## 🤝 Contribución

1. Fork el repositorio
2. Crear una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

## 👥 Autores

- **Desarrollador** - Sistema de Streaming IoT

---

## 🙏 Agradecimientos

- [RootEncoder](https://github.com/pedroSG94/RootEncoder) - Librería de streaming RTMP
- [Node Media Server](https://github.com/illuspas/Node-Media-Server) - Servidor de medios
- [mpegts.js](https://github.com/xqq/mpegts.js) - Reproductor HTTP-FLV
- [HLS.js](https://github.com/video-dev/hls.js/) - Reproductor HLS

---

<p align="center">
  Desarrollado con ❤️ para el curso de Sistemas Embebidos
</p>
