package com.example.streaming.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.DefaultAlpha
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

/**
 * ============================================================================
 * DASHBOARD COMPONENTS - Componentes del Dashboard de Estadísticas
 * ============================================================================
 *
 * Incluye:
 * - Gráficas de bitrate en tiempo real
 * - Estadísticas de viewers
 * - Métricas de rendimiento
 * - Historial de transmisiones
 *
 * ============================================================================
 */

// Colores del tema
private val GradientStart = Color(0xFF8B5CF6)
private val GradientEnd = Color(0xFF06B6D4)
private val CardColor = Color(0xFF16213E)
private val CardColorLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF1A1A2E)
private val SuccessColor = Color(0xFF10B981)
private val ErrorColor = Color(0xFFEF4444)
private val WarningColor = Color(0xFFF59E0B)
private val InfoColor = Color(0xFF3B82F6)

/**
 * Dashboard completo de estadísticas
 */
@Composable
fun StreamDashboard(
    bitrate: Long,
    duration: Long,
    fps: Int,
    viewersCount: Int,
    peakViewers: Int,
    isStreaming: Boolean,
    bitrateHistory: List<Float>,
    viewersHistory: List<Float>,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fila superior de estadísticas rápidas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Filled.Speed,
                title = "Bitrate",
                value = "${bitrate / 1000} kbps",
                color = GradientStart,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.People,
                title = "Viewers",
                value = viewersCount.toString(),
                subtitle = "Máx: $peakViewers",
                color = SuccessColor,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Filled.Videocam,
                title = "FPS",
                value = fps.toString(),
                color = GradientEnd,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Timer,
                title = "Duración",
                value = formatDuration(duration),
                color = InfoColor,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }

        // Gráfica de bitrate
        if (bitrateHistory.isNotEmpty() && isStreaming) {
            BitrateChart(
                data = bitrateHistory,
                isDarkMode = isDarkMode
            )
        }

        // Gráfica de viewers
        if (viewersHistory.isNotEmpty() && isStreaming) {
            ViewersChart(
                data = viewersHistory,
                isDarkMode = isDarkMode
            )
        }

        // Estado de la conexión
        ConnectionStatusCard(
            isStreaming = isStreaming,
            isDarkMode = isDarkMode
        )
    }
}

/**
 * Tarjeta de estadística individual
 */
@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String? = null,
    color: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) CardColor else CardColorLight
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                color = if (isDarkMode) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Gráfica de bitrate en tiempo real
 */
@Composable
fun BitrateChart(
    data: List<Float>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val entries = remember(data) {
        data.mapIndexed { index, value ->
            FloatEntry(x = index.toFloat(), y = value)
        }
    }

    val modelProducer = remember { ChartEntryModelProducer() }
    
    LaunchedEffect(entries) {
        modelProducer.setEntries(entries)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) CardColor else CardColorLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Bitrate",
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Últimos 30s",
                    color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Chart(
                chart = lineChart(
                    lines = listOf(
                        lineSpec(
                            lineColor = GradientStart,
                            lineBackgroundShader = DynamicShaders.fromBrush(
                                Brush.verticalGradient(
                                    listOf(
                                        GradientStart.copy(alpha = DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                        GradientStart.copy(alpha = DefaultAlpha.LINE_BACKGROUND_SHADER_END)
                                    )
                                )
                            )
                        )
                    )
                ),
                chartModelProducer = modelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
    }
}

/**
 * Gráfica de viewers
 */
@Composable
fun ViewersChart(
    data: List<Float>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val entries = remember(data) {
        data.mapIndexed { index, value ->
            FloatEntry(x = index.toFloat(), y = value)
        }
    }

    val modelProducer = remember { ChartEntryModelProducer() }
    
    LaunchedEffect(entries) {
        modelProducer.setEntries(entries)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) CardColor else CardColorLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 Viewers",
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "En tiempo real",
                    color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Chart(
                chart = lineChart(
                    lines = listOf(
                        lineSpec(
                            lineColor = SuccessColor,
                            lineBackgroundShader = DynamicShaders.fromBrush(
                                Brush.verticalGradient(
                                    listOf(
                                        SuccessColor.copy(alpha = DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                        SuccessColor.copy(alpha = DefaultAlpha.LINE_BACKGROUND_SHADER_END)
                                    )
                                )
                            )
                        )
                    )
                ),
                chartModelProducer = modelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
    }
}

/**
 * Tarjeta de estado de conexión
 */
@Composable
fun ConnectionStatusCard(
    isStreaming: Boolean,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStreaming) 
                SuccessColor.copy(alpha = 0.1f) 
            else 
                if (isDarkMode) CardColor else CardColorLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isStreaming) 
                                SuccessColor.copy(alpha = alpha) 
                            else 
                                Color.Gray,
                            shape = CircleShape
                        )
                )
                Column {
                    Text(
                        text = if (isStreaming) "Transmitiendo" else "Sin transmitir",
                        color = if (isDarkMode) Color.White else Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isStreaming) 
                            "Conectado al servidor" 
                        else 
                            "Presiona el botón para iniciar",
                        color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Icon(
                imageVector = if (isStreaming) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = null,
                tint = if (isStreaming) SuccessColor else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Mini estadísticas para mostrar sobre la cámara
 */
@Composable
fun MiniStats(
    bitrate: Long,
    fps: Int,
    viewers: Int,
    duration: Long,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MiniStatItem(
            icon = Icons.Filled.Speed,
            value = "${bitrate / 1000}",
            unit = "kbps"
        )
        MiniStatItem(
            icon = Icons.Filled.Videocam,
            value = fps.toString(),
            unit = "fps"
        )
        MiniStatItem(
            icon = Icons.Filled.People,
            value = viewers.toString(),
            unit = ""
        )
        Text(
            text = formatDuration(duration),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MiniStatItem(
    icon: ImageVector,
    value: String,
    unit: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Formatea la duración en HH:MM:SS
 */
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
