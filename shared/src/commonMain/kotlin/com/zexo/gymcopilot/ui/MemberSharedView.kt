package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.CheckInRequest
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.shared.network.MemberSyncRequest
import com.zexo.gymcopilot.utils.LocalMemberData
import com.zexo.gymcopilot.utils.decodeBase64ToBitmap
import com.zexo.gymcopilot.utils.launchQrScanner
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import com.zexo.gymcopilot.utils.saveLocalMemberProfile
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun MemberDashboardSharedScreen(
    gymName: String = "Gym 19",
    userEmail: String = "socio@gymcopilot.com",
    customLogoUri: String? = null,
    apiClient: GymKtorApiClient = GymKtorApiClient,
    currentTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var showQrDialog by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var showMemberProfileScreen by remember { mutableStateOf(false) }

    // DATOS DINÁMICOS DEL PERFIL DEL SOCIO Y GIMNASIO
    var activeFirstName by remember { mutableStateOf("pe") }
    var activeLastName by remember { mutableStateOf("lado") }
    var activeEmail by remember { mutableStateOf(userEmail) }
    var activePhone by remember { mutableStateOf("") }
    var activeAddress by remember { mutableStateOf("") }
    var activeWeight by remember { mutableStateOf("") }
    var activeHeight by remember { mutableStateOf("") }
    var activePhotoUri by remember { mutableStateOf<String?>(null) }
    var activeGymName by remember { mutableStateOf(gymName) }
    var activeGymUrl by remember { mutableStateOf("https://script.google.com/macros/s/AKfycbyrN53_uZ4ECfRyp-Qwb2sGSYjv-g7OeR5ERbAQxLdirOL7qvL6rfaxXsjJShc28P-a/exec") }

    // CARGA PERSISTENTE DESDE LOCALSTORAGE EN WEB / DISPOSITIVO
    LaunchedEffect(Unit) {
        val localData = readLocalMemberProfile()
        if (localData != null && localData.email.isNotBlank()) {
            activeFirstName = localData.firstName
            activeLastName = localData.lastName
            activeEmail = localData.email
            activePhone = localData.phone
            activeAddress = localData.address
            activeWeight = localData.weight
            activeHeight = localData.height
            if (localData.photoUri != null) activePhotoUri = localData.photoUri
            if (localData.gymName.isNotBlank()) activeGymName = localData.gymName
            if (localData.gymApiUrl.isNotBlank()) activeGymUrl = localData.gymApiUrl
        }
    }

    var broadcastMessage by remember { mutableStateOf("hola") }

    LaunchedEffect(activeGymUrl) {
        if (activeGymUrl.isNotBlank()) {
            while (true) {
                try {
                    val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    val localProfile = readLocalMemberProfile()
                    val ssId = localProfile?.spreadsheetId.orEmpty()
                    val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                    val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                    val url = "$activeGymUrl${paramChar}type=config&gymId=$gymIdVal$ssParam"
                    val responseText = apiClient.getData(url)
                    if (responseText.isNotBlank() && responseText.contains("success")) {
                        val jsonElement = apiClient.jsonInstance.parseToJsonElement(responseText)
                        val jsonObject = jsonElement as? JsonObject
                        val dataArray = jsonObject?.get("data") as? JsonArray
                        if (dataArray != null) {
                            var msgFound: String? = null
                            for (i in 0 until dataArray.size) {
                                val row = dataArray[i] as? JsonArray
                                if (row != null && row.size > 0 && row[0].jsonPrimitive.content == "GYM_CONFIG") {
                                    if (row.size > 13) {
                                        val rawMsg = row[13].jsonPrimitive.content
                                        val isIsoDate = rawMsg.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.*"""))
                                        if (!isIsoDate && rawMsg != "null") {
                                            msgFound = rawMsg
                                        }
                                    }
                                    break
                                }
                            }
                            if (msgFound != null) {
                                broadcastMessage = msgFound
                            }
                        }
                    }
                } catch (e: Throwable) {}
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    val gymIsOpen = true
    val classInSession = false

    val logoBitmap = remember(customLogoUri) {
        if (customLogoUri != null) decodeBase64ToBitmap(customLogoUri) else null
    }

    if (showHelpScreen) {
        HelpSharedScreen(onBack = { showHelpScreen = false })
        return
    }

    if (showMemberProfileScreen) {
        MemberProfileSharedScreen(
            initialFirstName = activeFirstName,
            initialLastName = activeLastName,
            initialEmail = activeEmail,
            initialPhone = activePhone,
            initialAddress = activeAddress,
            initialWeight = activeWeight,
            initialHeight = activeHeight,
            initialPhotoUri = activePhotoUri,
            onBack = { showMemberProfileScreen = false },
            onSaveProfile = { fn, ln, em, ph, ad, wt, ht, photo ->
                activeFirstName = fn
                activeLastName = ln
                activeEmail = em
                activePhone = ph
                activeAddress = ad
                activeWeight = wt
                activeHeight = ht
                activePhotoUri = photo

                // Guardar en almacenamiento local persistente (LocalStorage)
                saveLocalMemberProfile(
                    LocalMemberData(
                        firstName = fn,
                        lastName = ln,
                        email = em,
                        phone = ph,
                        address = ad,
                        weight = wt,
                        height = ht,
                        photoUri = photo,
                        gymName = activeGymName,
                        gymApiUrl = activeGymUrl
                    )
                )

                // Sincronización real con Google Sheets Database Ktor API
                coroutineScope.launch {
                    try {
                        val syncReq = MemberSyncRequest(
                            action = "update_member",
                            firstName = fn,
                            lastName = ln,
                            email = em,
                            phone = ph,
                            address = ad,
                            weight = wt,
                            height = ht,
                            photoUri = photo,
                            membershipStatus = "ACTIVO"
                        )
                        GymKtorApiClient.postData(activeGymUrl, syncReq)
                    } catch (t: Throwable) { }
                }
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // 1. HEADER CON RECUADRO DE LOGO 1:1 Y BOTONES DE ACCIÓN
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // RECUADRO DE LOGO 1:1
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF00151C))
                                .border(BorderStroke(2.dp, PrimaryTurquoise), RoundedCornerShape(20.dp))
                                .clickable { showMemberProfileScreen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = "Gym Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.add_photo),
                                    contentDescription = "Logo",
                                    tint = PrimaryTurquoise,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = activeGymName.ifBlank { "Gym 19" },
                                color = TextWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (activeFirstName.isNotBlank()) "$activeFirstName $activeLastName" else "Socio del Gimnasio",
                                color = PrimaryTurquoise,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // BOTONES SUPERIORES (PERFIL E INFO)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showMemberProfileScreen = true }) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = PrimaryTurquoise,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { showHelpScreen = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Ayuda",
                                tint = PrimaryTurquoise,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // 2. BADGES DE ESTADÍSTICAS E INDICADORES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusChip(
                        text = if (gymIsOpen) "GIMNASIO ABIERTO" else "GIMNASIO CERRADO",
                        color = if (gymIsOpen) Color(0xFF00C853) else Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        text = if (classInSession) "CLASES EN VIVO" else "SIN CLASES AHORA",
                        color = if (classInSession) Color(0xFF00C853) else Color(0xFFD32F2F),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. TARJETA "ESTADO DE MEMBRESÍA"
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, PrimaryTurquoise),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ESTADO DE MEMBRESÍA",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vigente",
                                color = Color(0xFF00C853),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Plan: Plan Mensual",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vence: N/A",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }

                        // ESCUDO DE VERIFICACIÓN
                        Surface(
                            color = Color(0xFF00C853).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Vigente",
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3.5 BANNER DE COMUNICADOS / MENSAJE GENERAL (BROADCAST)
            item {
                BroadcastMessageSharedDisplay(broadcastMessage)
            }

            // 4. GRILLA DE ACCIONES RÁPIDAS (2X2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MemberActionSquareCard(
                            title = "Mi QR Login",
                            iconVector = Icons.Default.QrCode2,
                            iconBg = Color(0xFF00363A),
                            iconTint = PrimaryTurquoise,
                            modifier = Modifier.weight(1f)
                        ) {
                            showQrDialog = true
                        }

                        MemberActionSquareCard(
                            title = "Check-in",
                            iconVector = Icons.Default.QrCodeScanner,
                            iconBg = Color(0xFF1B381E),
                            iconTint = Color(0xFF00C853),
                            modifier = Modifier.weight(1f)
                        ) {
                            showCheckInDialog = true
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MemberActionSquareCard(
                            title = "Mis Rutinas",
                            iconVector = Icons.Default.FitnessCenter,
                            iconBg = Color(0xFF0D233A),
                            iconTint = Color(0xFF42A5F5),
                            modifier = Modifier.weight(1f)
                        ) {
                            onTabSelected(3)
                        }

                        MemberActionSquareCard(
                            title = "Timer",
                            iconVector = Icons.Default.Timer,
                            iconBg = Color(0xFF3B331A),
                            iconTint = Color(0xFFFFD54F),
                            modifier = Modifier.weight(1f)
                        ) {
                            showTimerDialog = true
                        }
                    }
                }
            }

            // 5. TARJETA "RUTINA DE HOY"
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "RUTINA DE HOY",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "¡Día de Descanso!",
                                color = TextWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGOS MODALES INTERACTIVOS
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Mi Código QR", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(180.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QrCode2, contentDescription = "QR Login", tint = Color.Black, modifier = Modifier.size(140.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Escanea en la recepción para ingresar", color = TextGray, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { showQrDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCheckInDialog) {
        var checkInSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCheckInDialog = false },
            title = { Text("Check-in con Cámara QR", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    if (checkInSuccess) {
                        Surface(
                            color = Color(0xFF00C853).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF00C853)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("¡Asistencia Registrada Exitosamente!", color = Color(0xFF00C853), fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF00151C))
                                .border(BorderStroke(2.dp, PrimaryTurquoise), RoundedCornerShape(20.dp))
                                .clickable {
                                    launchQrScanner { scannedCode ->
                                        val cleanCode = scannedCode.trim()
                                        if (cleanCode.startsWith("MEMBERLINK|")) {
                                            val parts = cleanCode.split("|")
                                            if (parts.size >= 4) {
                                                activeFirstName = parts[1]
                                                activeLastName = parts[2]
                                                activeEmail = parts[3]
                                                if (parts.size >= 5 && parts[4].isNotBlank()) activeGymUrl = parts[4]
                                                if (parts.size >= 6 && parts[5].isNotBlank()) activeGymName = parts[5]
                                            }
                                        } else if (cleanCode.startsWith("MEMBER_LINK:")) {
                                            val parts = cleanCode.split(":")
                                            if (parts.size >= 5) {
                                                activeEmail = parts[2]
                                                activeFirstName = parts[3]
                                                activeLastName = parts[4]
                                            }
                                        } else if (cleanCode.contains("@")) {
                                            activeEmail = cleanCode.trim()
                                        }

                                        // Guardar en almacenamiento local persistente (LocalStorage)
                                        saveLocalMemberProfile(
                                            LocalMemberData(
                                                firstName = activeFirstName,
                                                lastName = activeLastName,
                                                email = activeEmail,
                                                phone = activePhone,
                                                address = activeAddress,
                                                weight = activeWeight,
                                                height = activeHeight,
                                                photoUri = activePhotoUri,
                                                gymName = activeGymName,
                                                gymApiUrl = activeGymUrl
                                            )
                                        )

                                        checkInSuccess = true

                                        // Registrar asistencia y actualizar miembro HTTP POST en Google Sheets DB
                                        coroutineScope.launch {
                                            try {
                                                val syncReq = MemberSyncRequest(
                                                    action = "update_member",
                                                    firstName = activeFirstName,
                                                    lastName = activeLastName,
                                                    email = activeEmail,
                                                    phone = activePhone,
                                                    address = activeAddress,
                                                    weight = activeWeight,
                                                    height = activeHeight,
                                                    photoUri = activePhotoUri,
                                                    membershipStatus = "ACTIVO"
                                                )
                                                GymKtorApiClient.postData(activeGymUrl, syncReq)

                                                val checkInReq = CheckInRequest(
                                                    action = "check_in",
                                                    gymId = activeGymName,
                                                    memberEmail = activeEmail,
                                                    timestamp = 0L
                                                )
                                                GymKtorApiClient.postData(activeGymUrl, checkInReq)
                                            } catch (t: Throwable) { }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear", tint = PrimaryTurquoise, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Toca para abrir la cámara trasera", color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!checkInSuccess) {
                        Button(
                            onClick = {
                                checkInSuccess = true
                                coroutineScope.launch {
                                    try {
                                        val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                                        val syncReq = MemberSyncRequest(
                                            action = "update_member",
                                            firstName = activeFirstName,
                                            lastName = activeLastName,
                                            email = activeEmail,
                                            phone = activePhone,
                                            address = activeAddress,
                                            weight = activeWeight,
                                            height = activeHeight,
                                            photoUri = activePhotoUri,
                                            membershipStatus = "ACTIVO"
                                        )
                                        GymKtorApiClient.postData(activeGymUrl, syncReq)

                                        val checkInReq = CheckInRequest(
                                            action = "check_in",
                                            gymId = gymIdVal,
                                            memberEmail = activeEmail,
                                            timestamp = 0L
                                        )
                                        GymKtorApiClient.postData(activeGymUrl, checkInReq)
                                    } catch (t: Throwable) {
                                        println("Error check-in: ${t.message}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)
                        ) {
                            Text("MARCAR PRESENTE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                launchQrScanner { scannedCode ->
                                    val cleanCode = scannedCode.trim()
                                    if (cleanCode.startsWith("MEMBERLINK|")) {
                                        val parts = cleanCode.split("|")
                                        if (parts.size >= 4) {
                                            activeFirstName = parts[1]
                                            activeLastName = parts[2]
                                            activeEmail = parts[3]
                                            if (parts.size >= 5 && parts[4].isNotBlank()) activeGymUrl = parts[4]
                                            if (parts.size >= 6 && parts[5].isNotBlank()) activeGymName = parts[5]
                                        }
                                    } else if (cleanCode.startsWith("MEMBER_LINK:")) {
                                        val parts = cleanCode.split(":")
                                        if (parts.size >= 5) {
                                            activeEmail = parts[2]
                                            activeFirstName = parts[3]
                                            activeLastName = parts[4]
                                        }
                                    } else if (cleanCode.contains("@")) {
                                        activeEmail = cleanCode.trim()
                                    }

                                    saveLocalMemberProfile(
                                        LocalMemberData(
                                            firstName = activeFirstName,
                                            lastName = activeLastName,
                                            email = activeEmail,
                                            phone = activePhone,
                                            address = activeAddress,
                                            weight = activeWeight,
                                            height = activeHeight,
                                            photoUri = activePhotoUri,
                                            gymName = activeGymName,
                                            gymApiUrl = activeGymUrl
                                        )
                                    )

                                    checkInSuccess = true

                                    coroutineScope.launch {
                                        try {
                                            val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                                            val syncReq = MemberSyncRequest(
                                                action = "update_member",
                                                firstName = activeFirstName,
                                                lastName = activeLastName,
                                                email = activeEmail,
                                                phone = activePhone,
                                                address = activeAddress,
                                                weight = activeWeight,
                                                height = activeHeight,
                                                photoUri = activePhotoUri,
                                                membershipStatus = "ACTIVO"
                                            )
                                            GymKtorApiClient.postData(activeGymUrl, syncReq)

                                            val checkInReq = CheckInRequest(
                                                action = "check_in",
                                                gymId = gymIdVal,
                                                memberEmail = activeEmail,
                                                timestamp = 0L
                                            )
                                            GymKtorApiClient.postData(activeGymUrl, checkInReq)
                                        } catch (t: Throwable) { }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                        ) {
                            Text("ESCANEAR QR", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showCheckInDialog = false }) {
                        Text("CERRAR", color = TextWhite)
                    }
                }
            }
        )
    }

    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Cronómetro de Descanso", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text("01:30", fontSize = 48.sp, fontWeight = FontWeight.Black, color = PrimaryTurquoise)
                    Spacer(Modifier.height(8.dp))
                    Text("Tiempo de descanso sugerido entre series", color = TextGray, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { showTimerDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("INICIAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun MemberActionSquareCard(
    title: String,
    iconVector: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.4f)),
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = iconBg,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BroadcastMessageSharedDisplay(broadcastMessage: String) {
    val message = broadcastMessage.ifBlank { "hola" }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .background(
                brush = Brush.horizontalGradient(
                    0.0f to Color(0xFF8A2BE2).copy(alpha = 0.5f),
                    0.5f to Color(0xFF00151C).copy(alpha = 0.9f),
                    1.0f to Color.Red.copy(alpha = 0.5f)
                )
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
