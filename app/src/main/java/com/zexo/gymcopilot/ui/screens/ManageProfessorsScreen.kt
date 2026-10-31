package com.zexo.gymcopilot.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.gson.JsonArray
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zexo.gymcopilot.ChatMessage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfessorsScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onChatClick: (String) -> Unit = {},
    onAddProfessor: () -> Unit,
    onEditProfessor: (String) -> Unit,
    showAddCard: Boolean = true
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val attendanceRepository = remember(gymApiUrl) {
        com.zexo.gymcopilot.repository.AttendanceRepository(
            NetworkModule.getApiService(gymApiUrl),
            dataStoreManager
        )
    }
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")

    val myID = remember(userName, userEmail, userRole) {
        val role = userRole?.lowercase() ?: ""
        if (role == "admin") "Admin" else userEmail.ifBlank { userName }.trim()
    }

    LaunchedEffect(Unit) {
        dataStoreManager.setLastReadTimestamp("admin_professors_chats_global", System.currentTimeMillis())
    }

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val unreadMap = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(gymApiUrl, myID, professors) {
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
            try {
                val response = apiServiceGet.getChatsRaw(syncUrl, "Bearer session_active")
                if (response.isSuccessful) {
                    val jsonElement = response.body()
                    val rows = when {
                        jsonElement?.isJsonArray == true -> jsonElement.asJsonArray
                        jsonElement?.isJsonObject == true -> jsonElement.asJsonObject.getAsJsonArray("data") ?: JsonArray()
                        else -> JsonArray()
                    }

                    val allMessages = mutableListOf<ChatMessage>()
                    for (i in 1 until rows.size()) {
                        val arr = rows.get(i).asJsonArray
                        if (arr.size() >= 4) {
                            val sender = arr.get(1).asString
                            val recipient = arr.get(2).asString
                            val text = arr.get(3).asString
                            val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                            val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L
                            allMessages.add(ChatMessage(sender, recipient, text, ts, sender.lowercase().trim() == myID.lowercase().trim()))
                        }
                    }

                    for (prof in professors) {
                        val otherID = if (prof.email.isNotBlank()) prof.email else prof.fullName
                        val sortedIds = listOf(myID.lowercase().trim(), otherID.lowercase().trim()).sorted()
                        val chatKey = "${sortedIds[0]}_${sortedIds[1]}".replace(" ", "_")
                        val lastReadTs = dataStoreManager.getLastReadTimestamp(chatKey).first()
                        val hasNew = allMessages.any { msg ->
                            val msgSender = msg.sender.lowercase().trim()
                            val msgRecipient = msg.recipient.lowercase().trim()
                            val otherL = otherID.lowercase().trim()
                            val myL = myID.lowercase().trim()
                            val isMatch = (msgSender == myL && msgRecipient == otherL) || (msgSender == otherL && msgRecipient == myL)
                            isMatch && !msg.isMine && msg.timestamp > lastReadTs
                        }
                        unreadMap[otherID] = hasNew
                    }
                }
            } catch (e: Exception) { Log.e("ManageProfessors", "Sync error: ${e.message}") }
            delay(5000)
        }
    }

    val t = remember(appLanguage) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Gestionar Profesores",
                "add" to "Agregar Nuevo Profesor",
                "add_desc" to "Registrar un docente al equipo",
                "empty" to "No hay profesores registrados",
                "delete_confirm" to "¿Eliminar profesor?",
                "delete_msg" to "Esta acción no se puede deshacer.",
                "delete" to "Eliminar",
                "cancel" to "Cancelar"
            )
        } else {
            mapOf(
                "title" to "Manage Professors",
                "add" to "Add New Professor",
                "add_desc" to "Register a teacher to the team",
                "empty" to "No professors registered",
                "delete_confirm" to "Delete professor?",
                "delete_msg" to "This action cannot be undone.",
                "delete" to "Delete",
                "cancel" to "Cancel"
            )
        }
    }

    var professorToDelete by remember { mutableStateOf<Professor?>(null) }
    var showQrForProfessor by remember { mutableStateOf<Professor?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(id = R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(34.dp).clip(CircleShape),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                    }
                }
            )
        },
        bottomBar = {
            AdminBottomNavigation(
                currentRoute = "manage_professors",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onMembersClick = onMembersClick,
                onProfessorsClick = { },
                onSettingsClick = onSettingsClick,
                accentColor = accentColor
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 0.dp, top = 20.dp, end = 0.dp, bottom = 100.dp)
        ) {
            if (showAddCard) {
                item {
                    SettingsCard(
                        title = t["add"] ?: "",
                        description = t["add_desc"] ?: "",
                        icon = Icons.Default.PersonAdd,
                        accentColor = accentColor,
                        cardColor = accentColor,
                        shape = containerShape,
                        onClick = onAddProfessor
                    )
                }
            }

            if (professors.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(t["empty"] ?: "", color = TextGray, fontSize = 16.sp)
                    }
                }
            } else {
                items(professors) { prof ->
                    val identifier = if (prof.email.isNotBlank()) prof.email else prof.fullName
                    ProfessorManagementCard(
                        professor = prof,
                        accentColor = accentColor,
                        shape = containerShape,
                        hasUnreadMessages = unreadMap[identifier] ?: false,
                        onEdit = { onEditProfessor(prof.id) },
                        onDelete = { professorToDelete = prof },
                        onChat = { onChatClick(identifier) },
                        onQrClick = { showQrForProfessor = prof }
                    )
                }
            }
        }
    }

    if (professorToDelete != null) {
        AlertDialog(
            onDismissRequest = { professorToDelete = null },
            title = { Text(t["delete_confirm"] ?: "", color = TextWhite) },
            text = { Text(t["delete_msg"] ?: "", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = {
                    professorToDelete?.let { prof ->
                        coroutineScope.launch {
                            dataStoreManager.removeProfessor(prof.id)
                            attendanceRepository.deleteProfessor(prof.id, prof.email, prof.firstName, prof.lastName)
                            professorToDelete = null
                            Toast.makeText(context, if (appLanguage == "Español") "Profesor eliminado" else "Professor deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text(t["delete"] ?: "", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { professorToDelete = null }) {
                    Text(t["cancel"] ?: "", color = TextWhite)
                }
            }
        )
    }

    showQrForProfessor?.let { prof ->
        val colorStr = prof.profileColor?.toString() ?: ""
        val qrContent = "PROFLINK|${prof.id}|${prof.firstName}|${prof.lastName}|${prof.email}|$gymApiUrl|$gymName|$colorStr"
        ProfessorLinkQrDialogInternal(
            content = qrContent,
            onDismiss = { showQrForProfessor = null },
            accentColor = accentColor
        )
    }
}

@Composable
fun ProfessorManagementCard(
    professor: Professor,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    hasUnreadMessages: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit,
    onQrClick: () -> Unit
) {
    val profColor = professor.profileColor?.let { Color(it) } ?: accentColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF081C24))
            .border(2.dp, profColor, shape)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(profColor.copy(alpha = 0.12f))
                        .border(2.dp, profColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (professor.photoUri != null) {
                        val imgModel = remember(professor.photoUri) {
                            if (professor.photoUri!!.startsWith("data:image")) {
                                try {
                                    val base64String = professor.photoUri!!.substringAfter("base64,")
                                    val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                } catch (e: Exception) { professor.photoUri }
                            } else professor.photoUri
                        }
                        AsyncImage(
                            model = imgModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.FitnessCenter, null, tint = profColor, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(professor.fullName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onQrClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR", tint = accentColor, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onChat, modifier = Modifier.size(36.dp)) {
                    Box {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = accentColor, modifier = Modifier.size(24.dp))
                        if (hasUnreadMessages) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, CircleShape)
                                    .border(1.dp, Color(0xFF081C24), CircleShape)
                            )
                        }
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accentColor, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfessorLinkQrDialogInternal(content: String, onDismiss: () -> Unit, accentColor: Color) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF081C24),
            modifier = Modifier.padding(16.dp).border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VINCULAR PROFESOR", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text("Escanea este código con el dispositivo del profesor para vincularlo.", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))
                
                val bitmap: Bitmap? = remember(content) { generateProfessorLinkQrInternal(content) }
                if (bitmap != null) {
                    Box(modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR de Vinculación", modifier = Modifier.fillMaxSize())
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, accentColor, RoundedCornerShape(12.dp))
                ) {
                    val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                    Text("CERRAR", color = contentColor, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun generateProfessorLinkQrInternal(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}
