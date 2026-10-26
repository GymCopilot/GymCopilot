package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.ChatSyncRequest
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import kotlinx.coroutines.launch

data class SharedChatMessage(
    val sender: String,
    val recipient: String,
    val text: String,
    val timestamp: String = "12:00"
)

@Composable
fun ChatSharedScreen(
    gymName: String = "Gym 19",
    userEmail: String = "socio@gymcopilot.com",
    recipientName: String = "Chat General",
    onBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var localData = remember { readLocalMemberProfile() }
    val myEmail = localData?.email?.ifBlank { userEmail } ?: userEmail
    val activeGym = localData?.gymName?.ifBlank { gymName } ?: gymName
    val serverUrl = localData?.gymApiUrl ?: GymKtorApiClient.MASTER_SCRIPT_URL

    var currentRecipient by remember { mutableStateOf(recipientName) }
    var showRecipientMenu by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            SharedChatMessage(sender = "Admin", recipient = "Chat General", text = "¡Bienvenidos al Chat Oficial de $activeGym!", timestamp = "09:00"),
            SharedChatMessage(sender = myEmail, recipient = "Chat General", text = "Hola, buenas días", timestamp = "09:05")
        )
    }

    val listState = rememberLazyListState()

    fun sendMessage() {
        if (messageText.isBlank()) return
        val newMsg = SharedChatMessage(
            sender = myEmail,
            recipient = currentRecipient,
            text = messageText,
            timestamp = "12:00"
        )
        messages.add(newMsg)
        val msgToSend = messageText
        messageText = ""

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
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
                        Spacer(modifier = Modifier.width(16.dp))

                        // SELECTOR DE DESTINATARIO
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showRecipientMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Surface(
                                    color = PrimaryTurquoise.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentRecipient,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Toca para cambiar canal",
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryTurquoise)
                            }

                            DropdownMenu(
                                expanded = showRecipientMenu,
                                onDismissRequest = { showRecipientMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Chat General") },
                                    onClick = { currentRecipient = "Chat General"; showRecipientMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Administrador ($activeGym)") },
                                    onClick = { currentRecipient = "Admin"; showRecipientMenu = false }
                                )
                            }
                        }
                    }

                    // BOTÓN VACIAR CHAT
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Vaciar", tint = Color.Red.copy(alpha = 0.8f))
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
                            placeholder = { Text("Escribe un mensaje...", color = TextGray, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF00222E),
                                unfocusedContainerColor = Color(0xFF00222E),
                                focusedBorderColor = PrimaryTurquoise,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = PrimaryTurquoise,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(46.dp)
                                .clickable { sendMessage() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.sender == myEmail
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isMe) PrimaryTurquoise.copy(alpha = 0.25f) else Color(0xFF001F29),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 16.dp
                            ),
                            border = BorderStroke(1.dp, if (isMe) PrimaryTurquoise else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!isMe) {
                                    Text(
                                        text = msg.sender,
                                        color = PrimaryTurquoise,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(
                                    text = msg.text,
                                    color = TextWhite,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = msg.timestamp,
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Vaciar Chat", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas vaciar el historial de mensajes de este canal?", color = TextGray) },
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
