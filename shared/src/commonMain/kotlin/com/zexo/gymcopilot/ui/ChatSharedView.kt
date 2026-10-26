package com.zexo.gymcopilot.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.ChatSyncRequest
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

data class SharedChatMessage(
    val sender: String,
    val recipient: String,
    val text: String,
    val timestamp: String = "11:27"
)

@Composable
fun ChatSharedScreen(
    gymName: String = "Gym 26",
    userEmail: String = "socio@gymcopilot.com",
    recipientName: String = "Gym 26",
    onBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val localData = remember { readLocalMemberProfile() }
    val myEmail = localData?.email?.ifBlank { userEmail } ?: userEmail
    val activeGym = localData?.gymName?.ifBlank { gymName } ?: gymName
    val serverUrl = localData?.gymApiUrl ?: GymKtorApiClient.MASTER_SCRIPT_URL

    var currentRecipient by remember { mutableStateOf(if (recipientName == "Chat General") activeGym else recipientName) }
    var showRecipientMenu by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            SharedChatMessage(sender = myEmail, recipient = currentRecipient, text = "hola", timestamp = "11:27")
        )
    }

    val listState = rememberLazyListState()

    fun sendMessage() {
        if (messageText.isBlank()) return
        val msgToSend = messageText
        messageText = ""

        val newMsg = SharedChatMessage(
            sender = myEmail,
            recipient = currentRecipient,
            text = msgToSend,
            timestamp = "11:28"
        )
        messages.add(newMsg)

        coroutineScope.launch {
            try {
                val req = ChatSyncRequest(
                    action = "send_chat_message",
                    gymId = activeGym,
                    sender = myEmail,
                    recipient = currentRecipient,
                    text = msgToSend
                )
                val jsonPayload = GymKtorApiClient.jsonInstance.encodeToString(ChatSyncRequest.serializer(), req)
                GymKtorApiClient.postAppsScriptData(serverUrl, jsonPayload, "send_chat_message", activeGym, myEmail)
            } catch (t: Throwable) { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // FONDO DE CHAT PERSONALIZADO (fondo_chat.png)
        Image(
            painter = painterResource(Res.drawable.fondo_chat),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = Color(0xFF00151C),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PrimaryTurquoise,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { onBack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // AVATAR CIRCULAR
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // SELECTOR DE DESTINATARIO (TITULO Y SUBTITULO TURQUESA)
                            Box {
                                Column(
                                    modifier = Modifier.clickable { showRecipientMenu = true }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentRecipient,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Administrador",
                                        color = PrimaryTurquoise,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                DropdownMenu(
                                    expanded = showRecipientMenu,
                                    onDismissRequest = { showRecipientMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(activeGym, color = TextWhite) },
                                        onClick = { currentRecipient = activeGym; showRecipientMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Chat General", color = TextWhite) },
                                        onClick = { currentRecipient = "Chat General"; showRecipientMenu = false }
                                    )
                                }
                            }
                        }

                        // BOTÓN ELIMINAR / VACIAR
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Vaciar", tint = PrimaryTurquoise)
                        }
                    }
                }
            },
            bottomBar = {
                // BARRA DE ENVÍO INFERIOR
                Surface(
                    color = Color(0xFF00151C),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Escribe aquí...", color = TextGray, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        IconButton(
                            onClick = { sendMessage() },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = if (messageText.isNotBlank()) PrimaryTurquoise else TextGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    val isMine = msg.sender == myEmail
                    SharedMessageBubble(msg, isMine)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Vaciar Chat", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas vaciar el historial de conversación?", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(
                    onClick = {
                        messages.clear()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("VACIAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCELAR", color = TextWhite)
                }
            }
        )
    }
}

@Composable
fun SharedMessageBubble(message: SharedChatMessage, isMine: Boolean) {
    val density = LocalDensity.current

    val bubbleShape = remember(isMine, density) {
        GenericShape { size, _ ->
            val r = with(density) { 16.dp.toPx() }
            val tailW = with(density) { 8.dp.toPx() }
            val tailH = with(density) { 12.dp.toPx() }

            if (isMine) {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, size.width - tailW, size.height),
                        topLeft = CornerRadius(r),
                        topRight = CornerRadius(r),
                        bottomLeft = CornerRadius(r),
                        bottomRight = CornerRadius(0f)
                    )
                )
                moveTo(size.width - tailW, size.height - tailH)
                lineTo(size.width, size.height)
                lineTo(size.width - tailW, size.height)
                close()
            } else {
                addRoundRect(
                    RoundRect(
                        rect = Rect(tailW, 0f, size.width, size.height),
                        topLeft = CornerRadius(r),
                        topRight = CornerRadius(r),
                        bottomLeft = CornerRadius(0f),
                        bottomRight = CornerRadius(r)
                    )
                )
                moveTo(tailW, size.height - tailH)
                lineTo(0f, size.height)
                lineTo(tailW, size.height)
                close()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isMine) PrimaryTurquoise else Color(0xFF081C24),
            shape = bubbleShape,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = if (isMine) Color.Black else TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message.timestamp,
                    color = if (isMine) Color.Black.copy(alpha = 0.6f) else TextGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
