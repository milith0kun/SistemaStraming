package com.example.streaming.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.streaming.chat.ChatMessage
import com.example.streaming.chat.ConnectionState
import java.text.SimpleDateFormat
import java.util.*

/**
 * ============================================================================
 * CHAT UI COMPONENTS - Componentes de UI para el Chat en Vivo
 * ============================================================================
 */

// Colores del tema
private val GradientStart = Color(0xFF8B5CF6)
private val GradientEnd = Color(0xFF06B6D4)
private val CardColor = Color(0xFF16213E)
private val SurfaceDark = Color(0xFF1A1A2E)
private val SurfaceLight = Color(0xFFF5F5F5)
private val SuccessColor = Color(0xFF10B981)
private val ErrorColor = Color(0xFFEF4444)
private val WarningColor = Color(0xFFF59E0B)

/**
 * Panel completo de chat
 */
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    connectionState: ConnectionState,
    onSendMessage: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll automático cuando llegan nuevos mensajes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    AnimatedVisibility(
        visible = isExpanded,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxHeight()
                .width(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) CardColor else Color.White
            ),
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header del chat
                ChatHeader(
                    connectionState = connectionState,
                    onClose = onToggleExpand,
                    isDarkMode = isDarkMode
                )

                // Campo de nombre de usuario
                UsernameInput(
                    username = username,
                    onUsernameChange = onUsernameChange,
                    isDarkMode = isDarkMode
                )

                // Lista de mensajes
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            EmptyChatMessage(isDarkMode = isDarkMode)
                        }
                    } else {
                        items(messages, key = { it.id }) { message ->
                            ChatMessageBubble(
                                message = message,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }

                // Input de mensaje
                ChatInput(
                    value = messageText,
                    onValueChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                            focusManager.clearFocus()
                        }
                    },
                    enabled = connectionState == ConnectionState.CONNECTED,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

/**
 * Header del chat
 */
@Composable
private fun ChatHeader(
    connectionState: ConnectionState,
    onClose: () -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Chat en Vivo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Indicador de conexión
            ConnectionIndicator(connectionState = connectionState)
            
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Indicador de conexión
 */
@Composable
private fun ConnectionIndicator(connectionState: ConnectionState) {
    val color = when (connectionState) {
        ConnectionState.CONNECTED -> SuccessColor
        ConnectionState.CONNECTING -> WarningColor
        ConnectionState.ERROR -> ErrorColor
        ConnectionState.DISCONNECTED -> Color.Gray
    }

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

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = if (connectionState == ConnectionState.CONNECTING) 
                    color.copy(alpha = alpha) 
                else 
                    color,
                shape = CircleShape
            )
    )
}

/**
 * Input de nombre de usuario
 */
@Composable
private fun UsernameInput(
    username: String,
    onUsernameChange: (String) -> Unit,
    isDarkMode: Boolean
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text("Tu nombre", fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = GradientStart,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
            unfocusedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
            focusedBorderColor = GradientStart,
            unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

/**
 * Mensaje de chat vacío
 */
@Composable
private fun EmptyChatMessage(isDarkMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¡Bienvenido al chat!",
            color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = "Sé el primero en enviar un mensaje",
            color = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

/**
 * Burbuja de mensaje de chat
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isDarkMode: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isOwn = message.isFromAndroid

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        // Username y tiempo
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.username,
                color = GradientStart,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "•",
                color = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                color = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray,
                fontSize = 10.sp
            )
        }

        // Burbuja del mensaje
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = if (isOwn) 12.dp else 4.dp,
                        topEnd = if (isOwn) 4.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
                .background(
                    if (isOwn) {
                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                                if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.05f)
                            )
                        )
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.message,
                color = if (isOwn) Color.White else (if (isDarkMode) Color.White else Color.Black),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Input de mensaje
 */
@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDarkMode) SurfaceDark else Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { 
                Text(
                    text = if (enabled) "Escribe un mensaje..." else "Conectando...",
                    fontSize = 14.sp
                ) 
            },
            enabled = enabled,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                focusedBorderColor = GradientStart,
                unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f),
                disabledBorderColor = Color.Gray.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(24.dp)
        )

        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = if (enabled && value.isNotBlank()) {
                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                    } else {
                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                    },
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Enviar",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Botón flotante para abrir el chat
 */
@Composable
fun ChatToggleButton(
    onClick: () -> Unit,
    unreadCount: Int = 0,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = GradientStart,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Filled.Chat,
                contentDescription = "Abrir Chat"
            )
        }

        // Badge de mensajes no leídos
        if (unreadCount > 0) {
            Badge(
                modifier = Modifier.align(Alignment.TopEnd),
                containerColor = ErrorColor
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    fontSize = 10.sp
                )
            }
        }

        // Indicador de conexión
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(12.dp)
                .background(
                    color = when (connectionState) {
                        ConnectionState.CONNECTED -> SuccessColor
                        ConnectionState.CONNECTING -> WarningColor
                        else -> ErrorColor
                    },
                    shape = CircleShape
                )
        )
    }
}
