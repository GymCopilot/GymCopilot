package com.zexo.gymcopilot.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.ChatMessage
import com.zexo.gymcopilot.shared.network.ChatSyncRequest
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSharedScreen(
    onBack: () -> Unit = {},
    apiClient: GymKtorApiClient = GymKtorApiClient
) {
    val coroutineScope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }

    var activeGymName by remember { mutableStateOf("Gym 26") }
    var activeGymUrl by remember { mutableStateOf("https://script.google.com/macros/s/AKfycbyrN53_uZ4ECfRyp-Qwb2sGSYjv-g7OeR5ERbAQxLdirOL7qvL6rfaxXsjJShc28P-a/exec") }
    var userEmail by remember { mutableStateOf("socio@gymcopilot.com") }

    LaunchedEffect(Unit) {
        val localProfile = readLocalMemberProfile()
        if (localProfile != null) {
            if (localProfile.gymName.isNotBlank()) activeGymName = localProfile.gymName
            if (localProfile.gymApiUrl.isNotBlank()) activeGymUrl = localProfile.gymApiUrl
            if (localProfile.email.isNotBlank()) userEmail = localProfile.email
        }
    }

    // Polling & Fetching Chat Messages from Google Sheets Database (`type=chats`)
    LaunchedEffect(activeGymUrl, userEmail, activeGymName) {
        val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
        while (isActive) {
            try {
                val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                val url = "$activeGymUrl${paramChar}type=chats&gymId=$gymIdVal&email=${userEmail}"
                val responseText = apiClient.getData(url)
                if (responseText.isNotBlank() && responseText.contains("success")) {
                    val jsonElement = apiClient.jsonInstance.parseToJsonElement(responseText)
                    val jsonObject = jsonElement as? JsonObject
                    val dataArray = jsonObject?.get("data") as? JsonArray
                    if (dataArray != null && dataArray.size > 1) {
                        val fetchedMsgs = mutableListOf<ChatMessage>()
                        val myIdTrim = userEmail.lowercase().trim()
                        for (i in 1 until dataArray.size) {
                            val row = dataArray[i] as? JsonArray
                            if (row != null && row.size >= 4) {
                                val sender = row[1].jsonPrimitive.content
                                val recipient = row[2].jsonPrimitive.content
                                val text = row[3].jsonPrimitive.content
                                val tsStr = if (row.size > 4) row[4].jsonPrimitive.content else "0"
                                val ts = tsStr.toLongOrNull() ?: 0L
                                val isMine = sender.lowercase().trim() == myIdTrim || sender.lowercase().contains("socio")
                                fetchedMsgs.add(ChatMessage(sender, recipient, text, ts, isMine))
                            }
                        }
                        messages = fetchedMsgs.sortedBy { it.timestamp }
                    }
                }
            } catch (e: Throwable) {
                println("Error fetching chats: ${e.message}")
            }
            delay(5000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00151C))
    ) {
        Image(
            painter = painterResource(Res.drawable.fondo_chat),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTurquoise.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(text = "$activeGymName ▼", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Administrador (Base de Datos)", color = PrimaryTurquoise, fontSize = 11.sp)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryTurquoise)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF00151C))
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { msg ->
                        val isMine = msg.isMine
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                color = if (isMine) PrimaryTurquoise else Color(0xFF081C24),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.text,
                                        color = if (isMine) Color.Black else TextWhite,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (msg.isPending) "Enviando..." else "Sincronizado",
                                        color = if (isMine) Color.Black.copy(alpha = 0.6f) else TextGray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = Color(0xFF00151C),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Escribe aquí...", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = Color(0xFF081C24),
                                unfocusedContainerColor = Color(0xFF081C24),
                                focusedBorderColor = PrimaryTurquoise,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val txt = messageText
                                    val ts = 0L
                                    messageText = ""
                                    val newMsg = ChatMessage(userEmail, "Admin", txt, ts, true, isPending = true)
                                    messages = messages + newMsg

                                    coroutineScope.launch {
                                        try {
                                            val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                                            val chatReq = ChatSyncRequest(
                                                action = "send_chat_message",
                                                gymId = gymIdVal,
                                                sender = userEmail,
                                                recipient = "Admin",
                                                text = txt,
                                                timestamp = ts,
                                                senderRole = "socio"
                                            )
                                            apiClient.postData(activeGymUrl, chatReq)
                                        } catch (e: Throwable) {
                                            println("Error sending chat message: ${e.message}")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
