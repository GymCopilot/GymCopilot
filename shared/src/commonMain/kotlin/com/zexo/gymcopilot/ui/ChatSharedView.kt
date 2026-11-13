package com.zexo.gymcopilot.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.zexo.gymcopilot.utils.decodeBase64ToBitmap
import com.zexo.gymcopilot.LockConfig
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
    var activeGymUrl by remember { mutableStateOf(LockConfig.activeApiUrl) }
    var userEmail by remember { mutableStateOf("socio@gymcopilot.com") }
    var assignedTrainer by remember { mutableStateOf("") }
    var serverGymLogoUri by remember { mutableStateOf<String?>(null) }
    var assignedProfPhotoUri by remember { mutableStateOf<String?>(null) }
    var assignedProfEmail by remember { mutableStateOf("") }

    var currentRecipient by remember { mutableStateOf("Admin") }
    var showSelector by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val localProfile = readLocalMemberProfile()
        if (localProfile != null) {
            if (localProfile.gymName.isNotBlank()) activeGymName = localProfile.gymName
            if (localProfile.gymApiUrl.isNotBlank()) activeGymUrl = localProfile.gymApiUrl
            if (localProfile.email.isNotBlank()) userEmail = localProfile.email
            if (localProfile.assignedTrainer.isNotBlank()) assignedTrainer = localProfile.assignedTrainer
        }
    }

    LaunchedEffect(activeGymUrl, assignedTrainer) {
        if (activeGymUrl.isNotBlank()) {
            try {
                val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                val localProfile = readLocalMemberProfile()
                val ssId = localProfile?.spreadsheetId.orEmpty()
                val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                
                val configUrl = "$activeGymUrl${paramChar}type=config&gymId=$gymIdVal$ssParam"
                val configResp = apiClient.getData(configUrl)
                if (configResp.isNotBlank() && configResp.contains("success")) {
                    val jsonObj = apiClient.jsonInstance.parseToJsonElement(configResp) as? JsonObject
                    val dataArr = jsonObj?.get("data") as? JsonArray
                    if (dataArr != null) {
                        for (i in 0 until dataArr.size) {
                            val row = dataArr[i] as? JsonArray
                            if (row != null && row.size > 0 && row[0].jsonPrimitive.content == "GYM_CONFIG") {
                                if (row.size > 2) {
                                    val logo = row[2].jsonPrimitive.content
                                    if (logo.isNotBlank() && logo != "null") serverGymLogoUri = logo
                                }
                                break
                            }
                        }
                    }
                }

                if (assignedTrainer.isNotBlank() && !assignedTrainer.equals("Sin asignar", true)) {
                    val profsUrl = "$activeGymUrl${paramChar}type=professors&gymId=$gymIdVal$ssParam"
                    val profsResp = apiClient.getData(profsUrl)
                    if (profsResp.isNotBlank() && profsResp.contains("success")) {
                        val jsonObj = apiClient.jsonInstance.parseToJsonElement(profsResp) as? JsonObject
                        val dataArr = jsonObj?.get("data") as? JsonArray
                        if (dataArr != null) {
                            val searchTrainer = assignedTrainer.lowercase().trim()
                            for (i in 1 until dataArr.size) {
                                val row = dataArr[i] as? JsonArray
                                if (row != null && row.size > 2) {
                                    val fn = row[1].jsonPrimitive.content.trim()
                                    val ln = row[2].jsonPrimitive.content.trim()
                                    val fullName = "$fn $ln".trim().lowercase()
                                    val revName = "$ln $fn".trim().lowercase()
                                    val email = if (row.size > 3) row[3].jsonPrimitive.content.trim().lowercase() else ""

                                    if (searchTrainer == fullName || searchTrainer == revName || searchTrainer.contains(fn.lowercase()) || searchTrainer.contains(ln.lowercase()) || fullName.contains(searchTrainer) || (email.isNotBlank() && searchTrainer.contains(email))) {
                                        if (row.size > 3 && email.isNotBlank()) assignedProfEmail = email
                                        if (row.size > 6) {
                                            val photo = row[6].jsonPrimitive.content
                                            if (photo.isNotBlank() && photo != "null") assignedProfPhotoUri = photo
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    val gymLogoBitmap = remember(serverGymLogoUri) {
        if (!serverGymLogoUri.isNullOrBlank()) decodeBase64ToBitmap(serverGymLogoUri!!) else null
    }
    val profPhotoBitmap = remember(assignedProfPhotoUri) {
        if (!assignedProfPhotoUri.isNullOrBlank()) decodeBase64ToBitmap(assignedProfPhotoUri!!) else null
    }

    val recipientTitle = remember(currentRecipient, activeGymName, assignedTrainer) {
        if (currentRecipient.lowercase() == "admin") "Administrador ($activeGymName)"
        else assignedTrainer.ifBlank { currentRecipient }
    }

    val recipientSubtitle = remember(currentRecipient) {
        if (currentRecipient.lowercase() == "admin") "Administrador (Base de Datos)" else "Profesor Asignado"
    }

    // Polling & Fetching Chat Messages from Google Sheets Database (`type=chats`)
    LaunchedEffect(activeGymUrl, userEmail, activeGymName, currentRecipient) {
        val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val myIdTrim = userEmail.lowercase().trim()
        val recipientTrim = currentRecipient.lowercase().trim()
        val isWithAdmin = recipientTrim == "admin"

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
                        for (i in 1 until dataArray.size) {
                            val row = dataArray[i] as? JsonArray
                            if (row != null && row.size >= 4) {
                                val sender = row[1].jsonPrimitive.content
                                val recipient = row[2].jsonPrimitive.content
                                val text = row[3].jsonPrimitive.content
                                val tsStr = if (row.size > 4) row[4].jsonPrimitive.content else "0"
                                val ts = tsStr.toLongOrNull() ?: 0L
                                
                                val senderL = sender.lowercase().trim()
                                val recipientL = recipient.lowercase().trim()

                                val isMatch = if (isWithAdmin) {
                                    recipientL.contains("admin") || recipientL.contains("chat general") || senderL.contains("admin")
                                } else {
                                    (senderL == myIdTrim && recipientL == recipientTrim) || (senderL == recipientTrim && recipientL == myIdTrim)
                                }

                                if (isMatch) {
                                    val isMine = senderL == myIdTrim || senderL.contains("socio")
                                    fetchedMsgs.add(ChatMessage(sender, recipient, text, ts, isMine))
                                }
                            }
                        }
                        messages = fetchedMsgs.sortedBy { val ts = it.timestamp; if (ts == 0L) 1L else ts }
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
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showSelector = true }
                                    .padding(end = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryTurquoise.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isWithAdmin = currentRecipient.lowercase() == "admin"
                                    val activePhoto = if (isWithAdmin) gymLogoBitmap else profPhotoBitmap
                                    if (activePhoto != null) {
                                        Image(
                                            bitmap = activePhoto,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = recipientTitle,
                                            color = TextWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                                    }
                                    Text(text = recipientSubtitle, color = PrimaryTurquoise, fontSize = 11.sp)
                                }
                            }

                            DropdownMenu(
                                expanded = showSelector,
                                onDismissRequest = { showSelector = false },
                                modifier = Modifier
                                    .background(Color(0xFF081C24))
                                    .border(1.dp, PrimaryTurquoise.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Administrador ($activeGymName)", color = TextWhite) },
                                    onClick = { currentRecipient = "Admin"; showSelector = false },
                                    leadingIcon = {
                                        if (gymLogoBitmap != null) {
                                            Image(bitmap = gymLogoBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)))
                                        } else {
                                            Icon(Icons.Default.Person, null, tint = PrimaryTurquoise)
                                        }
                                    }
                                )
                                if (assignedTrainer.isNotBlank() && !assignedTrainer.equals("Sin asignar", true)) {
                                    DropdownMenuItem(
                                        text = { Text(assignedTrainer, color = TextWhite) },
                                        onClick = { currentRecipient = assignedProfEmail.ifBlank { assignedTrainer }; showSelector = false },
                                        leadingIcon = {
                                            if (profPhotoBitmap != null) {
                                                Image(bitmap = profPhotoBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(24.dp).clip(CircleShape))
                                            } else {
                                                Icon(Icons.Default.Person, null, tint = PrimaryTurquoise)
                                            }
                                        }
                                    )
                                }
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
                    contentPadding = PaddingValues(vertical = 16.dp),
                    reverseLayout = true
                ) {
                    items(messages.asReversed()) { msg ->
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
                                    val newMsg = ChatMessage(userEmail, currentRecipient, txt, ts, true, isPending = true)
                                    messages = messages + newMsg

                                    coroutineScope.launch {
                                        try {
                                            val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                                            val chatReq = ChatSyncRequest(
                                                action = "send_chat_message",
                                                gymId = gymIdVal,
                                                sender = userEmail,
                                                recipient = currentRecipient,
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
