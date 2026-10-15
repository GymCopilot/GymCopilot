package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonArray
import com.zexo.gymcopilot.ChatMessage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.ChatSyncRequest
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    recipientName: String = "Chat General"
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")
    val gymNameStore by dataStoreManager.getGymName().collectAsState(initial = "")

    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)

    var currentRecipient by remember { mutableStateOf(recipientName) }
    var showSelector by remember { mutableStateOf(false) }

    // Identidad del remitente mejorada con fallback
    val myID = remember(userName, userEmail, userRole) {
        val role = userRole?.lowercase() ?: ""
        if (role == "admin") "Admin"
        else {
            val id = userEmail.ifBlank { userName }.trim()
            if (id.isEmpty()) "Usuario" else id
        }
    }

    val accentColor = PrimaryTurquoise

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var messageText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var pauseSyncUntil by remember { mutableStateOf(0L) }
    var isDeleting by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )

    val currentMember = remember(members, userEmail) { members.find { it.email.equals(userEmail, true) } }
    val myProfessor = remember(currentMember, professors) {
        if (currentMember?.assignedTrainer.isNullOrBlank()) null
        else professors.find { it.fullName.equals(currentMember?.assignedTrainer, true) }
    }

    val otherInfo = remember(currentRecipient, members, professors) {
        val prof = professors.find { it.fullName.equals(currentRecipient, true) || it.email.equals(currentRecipient, true) }
        val member = members.find { it.fullName.equals(currentRecipient, true) || it.email.equals(currentRecipient, true) }
        Triple(
            prof?.fullName ?: member?.fullName ?: if(currentRecipient.lowercase() == "admin") (gymNameStore.ifBlank { "Admin" }) else currentRecipient,
            prof?.email ?: member?.email ?: currentRecipient,
            if (prof != null) "Profesor" else if (member != null) "Alumno" else if(currentRecipient.lowercase() == "admin") "Administrador" else null
        )
    }

    val gymLogoModel = remember(gymLogoUri) {
        if (gymLogoUri?.startsWith("data:image") == true) {
            try {
                val base64String = gymLogoUri!!.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) { gymLogoUri }
        } else gymLogoUri
    }

    val myProfessorPhotoModel = remember(myProfessor) {
        val uri = myProfessor?.photoUri
        if (uri?.startsWith("data:image") == true) {
            try {
                val base64String = uri.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) { uri }
        } else uri
    }

    val otherPhotoModel = remember(otherInfo.second, professors, members, gymLogoModel, currentRecipient) {
        val isWithAdmin = currentRecipient.lowercase() == "admin"
        if (isWithAdmin) gymLogoModel else {
            val uri = professors.find { it.fullName.equals(currentRecipient, true) || it.email.equals(currentRecipient, true) }?.photoUri
                ?: members.find { it.fullName.equals(currentRecipient, true) || it.email.equals(currentRecipient, true) }?.photoUri
            
            if (uri?.startsWith("data:image") == true) {
                try {
                    val base64String = uri.substringAfter("base64,")
                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) { uri }
            } else uri
        }
    }

    val chatKey = remember(myID, otherInfo.second) {
        if (otherInfo.second.lowercase().contains("chat general")) "chat_general_global"
        else {
            val sortedIds = listOf(myID.lowercase().trim(), otherInfo.second.lowercase().trim()).sorted()
            "${sortedIds[0]}_${sortedIds[1]}".replace(" ", "_")
        }
    }

    LaunchedEffect(chatKey) {
        val cached = dataStoreManager.getChatCache(chatKey).first()
        if (cached.isNotEmpty()) {
            messages.clear()
            messages.addAll(cached)
            delay(100)
            listState.scrollToItem(0)

            val lastMsg = cached.lastOrNull { !it.isMine }
            if (lastMsg != null) {
                dataStoreManager.setLastReadTimestamp(chatKey, lastMsg.timestamp)
            } else if (cached.isNotEmpty()) {
                dataStoreManager.setLastReadTimestamp(chatKey, cached.last().timestamp)
            }
        }
    }

    LaunchedEffect(gymApiUrl, myID, otherInfo.second, refreshTrigger, pauseSyncUntil) {
        if (gymApiUrl.isBlank()) return@LaunchedEffect
        val gymNameVal = dataStoreManager.getGymName().first()
        val gymIdVal = gymNameVal.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val emailVal = dataStoreManager.getUserEmail().first()
        val ssIdVal = dataStoreManager.getGymSpreadsheetId().first()
        val paramChar = if (gymApiUrl.contains("?")) "&" else "?"
        val ssParam = if (ssIdVal.isNotBlank()) "&spreadsheetId=$ssIdVal" else ""
        val syncUrl = "$gymApiUrl${paramChar}type=chats&gymId=$gymIdVal&email=${java.net.URLEncoder.encode(emailVal, "UTF-8")}$ssParam"
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)

        while (isActive) {
            if (System.currentTimeMillis() > pauseSyncUntil && !isDeleting) {
                isSyncing = true
                try {
                    val response = apiServiceGet.getChatsRaw(syncUrl, "Bearer session_active")
                    if (response.isSuccessful) {
                        val jsonElement = response.body()
                        val rows = when {
                            jsonElement?.isJsonArray == true -> jsonElement.asJsonArray
                            jsonElement?.isJsonObject == true -> jsonElement.asJsonObject.getAsJsonArray("data") ?: JsonArray()
                            else -> JsonArray()
                        }

                        val remoteMsgs = mutableListOf<ChatMessage>()
                        val myL = myID.lowercase().trim()
                        val otherL = otherInfo.second.lowercase().trim()
                        val isChatGeneral = otherL.contains("chat general")

                        for (i in 1 until rows.size()) {
                            val arr = rows.get(i).asJsonArray
                            if (arr.size() >= 4) {
                                val sender = arr.get(1).asString.lowercase().trim()
                                val recipient = arr.get(2).asString.lowercase().trim()

                                val isMatch = if (isChatGeneral) {
                                    recipient.contains("chat general")
                                } else {
                                    (sender == myL && recipient == otherL) || (sender == otherL && recipient == myL)
                                }

                                if (isMatch) {
                                    val text = arr.get(3).asString
                                    val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                                    val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L
                                    remoteMsgs.add(ChatMessage(arr.get(1).asString, arr.get(2).asString, text, ts, sender == myL))
                                }
                            }
                        }

                        val sortedRemote = remoteMsgs.sortedBy { it.timestamp }
                        if (System.currentTimeMillis() > pauseSyncUntil && !isDeleting) {
                            val currentCount = messages.filter { !it.isPending }.size
                            val hasNewRemote = sortedRemote.size > currentCount || (sortedRemote.isNotEmpty() && messages.isNotEmpty() && sortedRemote.last().timestamp > messages.lastOrNull { !it.isPending }?.timestamp ?: 0L)
                            
                            if (hasNewRemote) {
                                // Verificar si hay mensajes nuevos de la otra persona para sonar
                                val oldLastTs = messages.filter { !it.isMine && !it.isPending }.lastOrNull()?.timestamp ?: 0L
                                val newLastTs = sortedRemote.filter { !it.isMine }.lastOrNull()?.timestamp ?: 0L
                                
                                if (newLastTs > oldLastTs) {
                                    try {
                                        val mediaPlayer = MediaPlayer.create(context, R.raw.mensaje)
                                        mediaPlayer.setOnCompletionListener { it.release() }
                                        mediaPlayer.start()
                                    } catch (e: Exception) {}
                                }

                                messages.clear()
                                messages.addAll(sortedRemote)
                                dataStoreManager.saveChatCache(chatKey, messages.toList())

                                val lastMsg = sortedRemote.lastOrNull { !it.isMine }
                                if (lastMsg != null) {
                                    dataStoreManager.setLastReadTimestamp(chatKey, lastMsg.timestamp)
                                } else if (sortedRemote.isNotEmpty()) {
                                    dataStoreManager.setLastReadTimestamp(chatKey, sortedRemote.last().timestamp)
                                }

                                if (messages.isNotEmpty()) listState.animateScrollToItem(0)
                            } else if (sortedRemote.isEmpty() && messages.isNotEmpty() && !messages.any { it.isPending }) {
                                messages.clear()
                                dataStoreManager.clearChatCache(chatKey)
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("ChatSync", "Error: ${e.message}") }
                isSyncing = false
            }
            delay(5000)
        }
    }

    if (showDeleteDialog) {
        val isAdmin = userRole?.lowercase() == "admin"
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF081C24),
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text(if (appLanguage == "Español") "Eliminar conversación" else "Delete chat", fontWeight = FontWeight.Bold) },
            text = { 
                val body = if (isAdmin) {
                    if (appLanguage == "Español") "Como administrador, borrarás permanentemente todo el hilo para todos." else "As admin, you will permanently delete the thread for everyone."
                } else {
                    if (appLanguage == "Español") "Esto borrará tus mensajes de la conversación." else "This will delete your messages from the conversation."
                }
                Text(body)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        isDeleting = true
                        pauseSyncUntil = System.currentTimeMillis() + 3000
                        coroutineScope.launch {
                            try {
                                messages.clear()
                                dataStoreManager.clearChatCache(chatKey)
                                val apiService = NetworkModule.getApiService(gymApiUrl)
                                apiService.postChatMessage(gymApiUrl, "Bearer active", ChatSyncRequest("delete_chat", gymNameStore, myID, otherInfo.second, senderRole = userRole ?: ""))
                            } catch (e: Exception) { pauseSyncUntil = 0L } finally { isDeleting = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (appLanguage == "Español") "Eliminar" else "Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(if (appLanguage == "Español") "Cancelar" else "Cancel", color = PrimaryTurquoise) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo de chat personalizado
        Image(
            painter = painterResource(id = R.drawable.fondo_chat),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f // Ajustar opacidad para que el texto sea legible
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
            TopAppBar(
                title = { 
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = userRole?.lowercase() == "member") { showSelector = true }
                                .padding(end = 16.dp)
                        ) {
                            // Mostrar imagen según destinatario
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                                if (otherPhotoModel != null) {
                                    AsyncImage(
                                        model = otherPhotoModel,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val isWithAdmin = currentRecipient.lowercase() == "admin"
                                    if (isWithAdmin) {
                                        Icon(Icons.Default.Sync, null, tint = PrimaryTurquoise, modifier = Modifier.size(20.dp))
                                    } else {
                                        val isProfessor = professors.any { it.fullName.equals(currentRecipient, true) || it.email.equals(currentRecipient, true) }
                                        if (isProfessor) {
                                            Icon(painterResource(R.drawable.assistant), null, tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
                                        } else {
                                            // Es un alumno: mostrar inicial
                                            val initial = otherInfo.first.take(1).uppercase()
                                            Text(
                                                text = initial,
                                                color = PrimaryTurquoise,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        otherInfo.first, 
                                        color = TextWhite, 
                                        fontSize = 17.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (userRole?.lowercase() == "member") {
                                        Icon(Icons.Default.ArrowDropDown, null, tint = TextGray, modifier = Modifier.size(20.dp))
                                    }
                                }
                                otherInfo.third?.let { role -> Text(text = role, color = PrimaryTurquoise, fontSize = 11.sp) } 
                            } 
                        }

                        // Menú de selección para alumnos
                        DropdownMenu(
                            expanded = showSelector,
                            onDismissRequest = { showSelector = false },
                            modifier = Modifier.background(Color(0xFF081C24)).border(1.dp, PrimaryTurquoise.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Administrador (${gymNameStore})", color = TextWhite) },
                                onClick = { currentRecipient = "Admin"; showSelector = false },
                                leadingIcon = { 
                                    if (gymLogoModel != null) {
                                        AsyncImage(model = gymLogoModel, contentDescription = null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Fit)
                                    } else {
                                        Icon(Icons.Default.AccountBalance, null, tint = PrimaryTurquoise)
                                    }
                                }
                            )
                            myProfessor?.let { prof ->
                                DropdownMenuItem(
                                    text = { Text(prof.fullName, color = TextWhite) },
                                    onClick = { currentRecipient = prof.email; showSelector = false },
                                    leadingIcon = { 
                                        if (myProfessorPhotoModel != null) {
                                            AsyncImage(model = myProfessorPhotoModel, null, modifier = Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Default.Person, null, tint = PrimaryTurquoise)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF00151C)),
                navigationIcon = {
                    IconButton(onClick = onBack) { Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(PrimaryTurquoise)) }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) { Icon(Icons.Default.Sync, null, tint = if (isSyncing) PrimaryTurquoise else TextGray, modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier) }
                    
                    val currentRole = userRole?.lowercase() ?: ""
                    if (currentRole == "admin") {
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, null, tint = PrimaryTurquoise) }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), reverseLayout = true, verticalArrangement = Arrangement.Bottom, contentPadding = PaddingValues(vertical = 16.dp)) {
                    items(messages.asReversed()) { msg -> MessageBubble(msg, timeFormat) }
                }
                if (isDeleting) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), Alignment.Center) { CircularProgressIndicator(color = PrimaryTurquoise) }
            }
            Surface(color = Color(0xFF00222E), tonalElevation = 8.dp) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = messageText, onValueChange = { messageText = it }, modifier = Modifier.weight(1f), placeholder = { Text(if (appLanguage == "Español") "Escribe aquí..." else "Type here...", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = TextWhite, cursorColor = PrimaryTurquoise, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                    IconButton(onClick = {
                        if (messageText.isNotBlank()) {
                            val txt = messageText; val ts = System.currentTimeMillis(); messageText = ""
                            messages.add(ChatMessage(myID, otherInfo.second, txt, ts, true, isPending = true))
                            coroutineScope.launch {
                                try { NetworkModule.getApiService(gymApiUrl).postChatMessage(gymApiUrl, "Bearer active", ChatSyncRequest(gymId = gymNameStore, sender = myID, recipient = otherInfo.second, text = txt, timestamp = ts, senderRole = userRole ?: "")) } catch (e: Exception) {}
                            }
                        }
                    }, enabled = messageText.isNotBlank() && !isDeleting) { Icon(Icons.Default.Send, null, tint = if (messageText.isNotBlank()) PrimaryTurquoise else TextGray) }
                }
            }
        }
    }
}
}

@Composable
fun MessageBubble(message: ChatMessage, timeFormat: SimpleDateFormat) {
    val isMine = message.isMine
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) PrimaryTurquoise else Color(0xFF081C24),
            shape = bubbleShape,
            modifier = Modifier.alpha(if (message.isPending) 0.6f else 1f)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = if (isMine) 12.dp else 20.dp,
                    end = if (isMine) 20.dp else 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
            ) {
                Text(
                    text = message.text,
                    color = if (isMine) Color.Black else TextWhite,
                    fontSize = 14.sp
                )
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    color = if (isMine) Color.Black.copy(alpha = 0.5f) else TextGray,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
    }
}
