package com.example.streaming.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================================
 * ADVANCED CONTROLS - Controles Avanzados del Reproductor
 * ============================================================================
 *
 * Incluye:
 * - Toggle de tema oscuro/claro
 * - Botón de Picture-in-Picture
 * - Modal de configuración completa
 * - Modo teatro
 * - Controles de calidad en tiempo real
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

/**
 * Panel de controles avanzados expandible
 */
@Composable
fun AdvancedControlsPanel(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    isPipEnabled: Boolean,
    onTogglePip: () -> Unit,
    onEnterPip: () -> Unit,
    isTheaterMode: Boolean,
    onToggleTheater: () -> Unit,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) CardColor else CardColorLight
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Controles Avanzados",
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

                Divider(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
                )

                // Sección de Apariencia
                SectionTitle(
                    title = "Apariencia",
                    icon = Icons.Filled.Palette,
                    isDarkMode = isDarkMode
                )

                // Toggle de tema
                SettingsToggle(
                    icon = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    title = "Modo Oscuro",
                    description = if (isDarkMode) "Activado" else "Desactivado",
                    isEnabled = isDarkMode,
                    onToggle = onToggleTheme,
                    isDarkMode = isDarkMode
                )

                // Modo Teatro
                SettingsToggle(
                    icon = Icons.Filled.Tv,
                    title = "Modo Teatro",
                    description = "Ocultar controles para vista limpia",
                    isEnabled = isTheaterMode,
                    onToggle = onToggleTheater,
                    isDarkMode = isDarkMode
                )

                Divider(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
                )

                // Sección de Reproducción
                SectionTitle(
                    title = "Reproducción",
                    icon = Icons.Filled.PlayCircle,
                    isDarkMode = isDarkMode
                )

                // Picture-in-Picture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsToggle(
                        icon = Icons.Filled.PictureInPicture,
                        title = "Picture-in-Picture",
                        description = "Auto-activar al minimizar",
                        isEnabled = isPipEnabled,
                        onToggle = onTogglePip,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Botón para entrar en PiP ahora
                    if (isStreaming) {
                        FilledTonalButton(
                            onClick = onEnterPip,
                            modifier = Modifier.padding(start = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = GradientStart.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = null,
                                tint = GradientStart,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PiP", color = GradientStart, fontSize = 12.sp)
                        }
                    }
                }

                Divider(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
                )

                // Sección de Notificaciones
                SectionTitle(
                    title = "Notificaciones",
                    icon = Icons.Filled.Notifications,
                    isDarkMode = isDarkMode
                )

                SettingsToggle(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Alertas de Stream",
                    description = "Notificar inicio, nuevos viewers",
                    isEnabled = notificationsEnabled,
                    onToggle = onToggleNotifications,
                    isDarkMode = isDarkMode
                )

                Divider(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
                )

                // Selector de calidad
                SectionTitle(
                    title = "Calidad de Video",
                    icon = Icons.Filled.HighQuality,
                    isDarkMode = isDarkMode
                )
                
                QualitySelector(
                    selectedQuality = selectedQuality,
                    onQualityChange = onQualityChange,
                    isDarkMode = isDarkMode,
                    enabled = !isStreaming
                )

                if (isStreaming) {
                    Text(
                        text = "⚠️ No se puede cambiar la calidad mientras transmites",
                        color = WarningColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Título de sección
 */
@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GradientStart,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

/**
 * Toggle de configuración
 */
@Composable
fun SettingsToggle(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isEnabled) GradientStart.copy(alpha = 0.2f)
                        else if (isDarkMode) Color.White.copy(alpha = 0.05f)
                        else Color.Gray.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) GradientStart 
                           else if (isDarkMode) Color.White.copy(alpha = 0.5f) 
                           else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GradientStart,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * Selector de calidad de video
 */
@Composable
fun QualitySelector(
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean = true
) {
    val qualities = listOf(
        "LOW" to "480p • 500 kbps",
        "MEDIUM" to "720p • 1.5 Mbps",
        "HIGH" to "720p+ • 2.5 Mbps",
        "ULTRA" to "1080p • 4 Mbps"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        qualities.forEach { (key, description) ->
            QualityOption(
                key = key,
                description = description,
                isSelected = selectedQuality == key,
                onSelect = { if (enabled) onQualityChange(key) },
                isDarkMode = isDarkMode,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun QualityOption(
    key: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(GradientStart.copy(alpha = 0.2f), GradientEnd.copy(alpha = 0.2f))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .clickable(enabled = enabled) { onSelect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { if (enabled) onSelect() },
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = GradientStart,
                    unselectedColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray
                )
            )
            Column {
                Text(
                    text = key,
                    color = if (enabled) {
                        if (isDarkMode) Color.White else Color.Black
                    } else {
                        Color.Gray
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = GradientStart,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Botón de toggle de tema (compacto para el header)
 */
@Composable
fun ThemeToggleButton(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(40.dp)
            .background(
                if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                CircleShape
            )
    ) {
        AnimatedContent(
            targetState = isDarkMode,
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "theme-toggle"
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Cambiar tema",
                tint = if (dark) Color.White else Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Botón flotante de configuración avanzada
 */
@Composable
fun AdvancedSettingsButton(
    onClick: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isDarkMode) CardColor else CardColorLight,
        contentColor = GradientStart
    ) {
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Configuración avanzada"
        )
    }
}
