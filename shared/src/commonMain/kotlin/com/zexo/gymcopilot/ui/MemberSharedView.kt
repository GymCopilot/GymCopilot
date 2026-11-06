package com.zexo.gymcopilot.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.zexo.gymcopilot.utils.formatEpochDate
import com.zexo.gymcopilot.utils.getCurrentTimeMillis
import com.zexo.gymcopilot.utils.generateQrBitmap
import com.zexo.gymcopilot.utils.launchQrScanner
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import com.zexo.gymcopilot.utils.saveLocalMemberProfile
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    var gymIsOpen by remember { mutableStateOf(true) }
    var classInSession by remember { mutableStateOf(false) }
    var showLogoBorder by remember { mutableStateOf(true) }
    var showCheckInSuccessBanner by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
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
    var activeGymUrl by remember { mutableStateOf("https://script.google.com/macros/s/AKfycbzXYYMu43-ATeyxQmwdF3CuRYXi7KbpChyTQEBFIsWh7SCHv2XugxsWen61oBeDdzw/exec") }
    var activeMembershipStatus by remember { mutableStateOf("ACTIVO") }
    var activeNextRenewalDate by remember { mutableLongStateOf(0L) }
    var activePlanName by remember { mutableStateOf("Plan Mensual") }
    var activeAssignedTrainer by remember { mutableStateOf("Prof. Gabriel Fernández") }

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
            activeMembershipStatus = localData.membershipStatus
            activeNextRenewalDate = localData.nextRenewalDate
            if (localData.planName.isNotBlank()) activePlanName = localData.planName
            if (localData.assignedTrainer.isNotBlank()) activeAssignedTrainer = localData.assignedTrainer
        }
    }

    LaunchedEffect(activeGymUrl, activeEmail) {
        if (activeGymUrl.isNotBlank() && activeEmail.isNotBlank()) {
            while (true) {
                try {
                    val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    val localProfile = readLocalMemberProfile()
                    val ssId = localProfile?.spreadsheetId.orEmpty()
                    val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                    val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                    val url = "$activeGymUrl${paramChar}type=members&gymId=$gymIdVal$ssParam"
                    val responseText = apiClient.getData(url)
                    if (responseText.isNotBlank() && responseText.contains("success")) {
                        val jsonElement = apiClient.jsonInstance.parseToJsonElement(responseText)
                        val jsonObject = jsonElement as? JsonObject
                        val dataArray = jsonObject?.get("data") as? JsonArray
                        if (dataArray != null) {
                            val myEmailTrim = activeEmail.lowercase().trim()
                            for (i in 1 until dataArray.size) {
                                val row = dataArray[i] as? JsonArray
                                if (row != null && row.size > 3) {
                                    val emailInRow = row[2].jsonPrimitive.content.lowercase().trim()
                                    if (emailInRow == myEmailTrim) {
                                        val status = row[3].jsonPrimitive.content
                                        if (status.isNotBlank()) activeMembershipStatus = status
                                        if (row.size > 10) {
                                            val renewalStr = row[10].jsonPrimitive.content
                                            val renewalTs = renewalStr.toLongOrNull() ?: 0L
                                            if (renewalTs > 0L) activeNextRenewalDate = renewalTs
                                        }
                                        if (row.size > 12) {
                                            val plan = row[12].jsonPrimitive.content
                                            if (plan.isNotBlank()) activePlanName = plan
                                        }
                                        if (row.size > 13) {
                                            val trainerInCol = row[13].jsonPrimitive.content
                                            if (trainerInCol.isNotBlank() && trainerInCol != "null") {
                                                activeAssignedTrainer = trainerInCol
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {}
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    var broadcastMessage by remember { mutableStateOf("") }
    var serverGymLogoUri by remember { mutableStateOf<String?>(null) }

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
                                    if (row.size > 1) {
                                        val rawGymName = row[1].jsonPrimitive.content
                                        if (rawGymName.isNotBlank() && rawGymName != "null") {
                                            activeGymName = rawGymName
                                        }
                                    }
                                    if (row.size > 2) {
                                        val rawLogo = row[2].jsonPrimitive.content
                                        if (rawLogo.isNotBlank() && rawLogo != "null") {
                                            serverGymLogoUri = rawLogo
                                        }
                                    }
                                    if (row.size > 11) {
                                        val rawBorder = row[11].jsonPrimitive.content
                                        if (rawBorder != "null") {
                                            showLogoBorder = rawBorder == "true" || rawBorder == "1"
                                        }
                                    }
                                    if (row.size > 12) {
                                        val rawClass = row[12].jsonPrimitive.content
                                        if (rawClass != "null") {
                                            classInSession = rawClass == "true" || rawClass == "1"
                                        }
                                    }
                                    if (row.size > 13) {
                                        val rawMsg = row[13].jsonPrimitive.content
                                        val isIsoDate = rawMsg.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.*"""))
                                        if (!isIsoDate && rawMsg != "null") {
                                            msgFound = rawMsg
                                        }
                                    }
                                    if (row.size > 14) {
                                        val rawOpen = row[14].jsonPrimitive.content
                                        if (rawOpen != "null") {
                                            gymIsOpen = rawOpen == "true" || rawOpen == "1"
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

    val gymLogoBitmap = remember(serverGymLogoUri, customLogoUri) {
        val uri = serverGymLogoUri ?: customLogoUri
        if (uri != null) decodeBase64ToBitmap(uri) else null
    }

    var assignedProfPhotoUri by remember { mutableStateOf<String?>(null) }
    var assignedProfColorInt by remember { mutableStateOf<Int?>(null) }
    var assignedProfSpecialty by remember { mutableStateOf("") }

    LaunchedEffect(activeGymUrl, activeAssignedTrainer) {
        if (activeGymUrl.isNotBlank() && activeAssignedTrainer.isNotBlank() && !activeAssignedTrainer.equals("Sin asignar", true) && !activeAssignedTrainer.equals("N/A", true)) {
            try {
                val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                val localProfile = readLocalMemberProfile()
                val ssId = localProfile?.spreadsheetId.orEmpty()
                val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                val url = "$activeGymUrl${paramChar}type=professors&gymId=$gymIdVal$ssParam"
                val responseText = apiClient.getData(url)
                if (responseText.isNotBlank() && responseText.contains("success")) {
                    val jsonElement = apiClient.jsonInstance.parseToJsonElement(responseText)
                    val jsonObject = jsonElement as? JsonObject
                    val dataArray = jsonObject?.get("data") as? JsonArray
                    if (dataArray != null) {
                        val searchTrainer = activeAssignedTrainer.lowercase().trim()
                        for (i in 1 until dataArray.size) {
                            val row = dataArray[i] as? JsonArray
                            if (row != null && row.size > 2) {
                                val fn = row[1].jsonPrimitive.content.trim()
                                val ln = row[2].jsonPrimitive.content.trim()
                                val fullName = "$fn $ln".trim().lowercase()
                                val email = if (row.size > 3) row[3].jsonPrimitive.content.trim().lowercase() else ""
                                
                                if (searchTrainer.contains(fn.lowercase()) || searchTrainer.contains(ln.lowercase()) || fullName.contains(searchTrainer) || searchTrainer.contains(fullName) || (email.isNotBlank() && searchTrainer.contains(email))) {
                                    if (row.size > 4) {
                                        val spec = row[4].jsonPrimitive.content
                                        if (spec.isNotBlank() && spec != "null") assignedProfSpecialty = spec
                                    }
                                    if (row.size > 5) {
                                        val colorStr = row[5].jsonPrimitive.content
                                        val colorVal = colorStr.toIntOrNull()
                                        if (colorVal != null && colorVal != 0) assignedProfColorInt = colorVal
                                    }
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
            } catch (_: Throwable) {}
        }
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
                        gymApiUrl = activeGymUrl,
                        assignedTrainer = activeAssignedTrainer
                    )
                )

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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // 1. HEADER CON RECUADRO DE LOGO Y DATOS DE SOCIO
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(121.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF00151C))
                                .then(
                                    if (showLogoBorder) Modifier.border(BorderStroke(2.dp, PrimaryTurquoise), RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                                .clickable { showMemberProfileScreen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (gymLogoBitmap != null) {
                                Image(
                                    bitmap = gymLogoBitmap,
                                    contentDescription = "Gym Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.gymcopilot),
                                    contentDescription = "Logo",
                                    tint = PrimaryTurquoise,
                                    modifier = Modifier.size(54.dp)
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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

            // 3. TARJETA ESTADO DE MEMBRESÍA
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SharedMembershipStatusCard(
                        membershipStatus = activeMembershipStatus,
                        nextRenewalDate = activeNextRenewalDate,
                        planName = activePlanName
                    )
                }
            }

            if (broadcastMessage.isNotBlank() && broadcastMessage != "null") {
                item {
                    BroadcastMessageSharedDisplay(broadcastMessage)
                }
            }

            // 4. GRILLA DE ACCIONES RÁPIDAS
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MemberActionSquareCard(
                            title = "QR Login",
                            iconPainter = painterResource(Res.drawable.qr_code),
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
                                        gymApiUrl = activeGymUrl,
                                        assignedTrainer = activeAssignedTrainer
                                    )
                                )

                                coroutineScope.launch {
                                    try {
                                        val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                                        val localProfile = readLocalMemberProfile()
                                        val ssId = localProfile?.spreadsheetId.orEmpty()
                                        val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                                        val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                                        val checkInUrl = "$activeGymUrl${paramChar}action=check_in&gymId=$gymIdVal&email=${activeEmail}&memberEmail=${activeEmail}$ssParam"
                                        val checkInReq = CheckInRequest(
                                            action = "check_in",
                                            gymId = gymIdVal,
                                            memberEmail = activeEmail,
                                            timestamp = getCurrentTimeMillis(),
                                            spreadsheetId = ssId
                                        )
                                        apiClient.postData(checkInUrl, checkInReq)
                                    } catch (_: Throwable) {}
                                }

                                showCheckInSuccessBanner = true
                            }
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
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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

            // 6. TARJETA REAL "MI PROFESOR ASIGNADO" (DESDE BASE DE DATOS / PLANILLA)
            if (activeAssignedTrainer.isNotBlank() && !activeAssignedTrainer.equals("Sin asignar", true) && !activeAssignedTrainer.equals("N/A", true) && activeAssignedTrainer != "null") {
                item {
                    val profColor = if (assignedProfColorInt != null && assignedProfColorInt != 0) Color(assignedProfColorInt!!) else PrimaryTurquoise
                    val profPhotoBitmap = remember(assignedProfPhotoUri) {
                        if (!assignedProfPhotoUri.isNullOrBlank()) decodeBase64ToBitmap(assignedProfPhotoUri!!) else null
                    }
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, profColor.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        color = profColor.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        border = BorderStroke(2.dp, profColor),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (profPhotoBitmap != null) {
                                                Image(
                                                    bitmap = profPhotoBitmap,
                                                    contentDescription = "Foto Profesor",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "Profesor Asignado",
                                                    tint = profColor,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "MI PROFESOR ASIGNADO",
                                            color = TextGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = activeAssignedTrainer,
                                            color = TextWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (assignedProfSpecialty.isNotBlank()) assignedProfSpecialty else "Entrenador Personal",
                                            color = profColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onTabSelected(4) }, // Redirige al chat del profesor
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(profColor.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = profColor, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TEMPORIZADOR ADAPTATIVO MODAL INTERACTIVO
    if (showTimerDialog) {
        AdaptiveTimerSharedDialog(
            onDismiss = { showTimerDialog = false }
        )
    }

    if (showQrDialog) {
        val qrContent = "ATTENDANCE:$activeEmail"
        val qrBitmap: ImageBitmap? = remember(qrContent) { generateQrBitmap(qrContent, 512) }
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
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "QR Login",
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Default.QrCode2, contentDescription = "QR Login", tint = Color.Black, modifier = Modifier.size(140.dp))
                            }
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

    if (showCheckInSuccessBanner) {
        AlertDialog(
            onDismissRequest = { showCheckInSuccessBanner = false },
            title = { Text("Asistencia Registrada", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
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
                        Text("¡Check-in realizado con éxito!", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCheckInSuccessBanner = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("ACEPTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF081C24)
        )
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SharedMembershipStatusCard(
    membershipStatus: String,
    nextRenewalDate: Long,
    planName: String
) {
    val isActivo = membershipStatus.uppercase() == "ACTIVO" || membershipStatus.uppercase() == "ACTIVE"
    val statusText = if (isActivo) "Activo" else "Pendiente de pago"
    val statusColor = if (isActivo) Color(0xFF00C853) else Color(0xFFFF3D00)

    val now = getCurrentTimeMillis()
    val daysLeft = if (nextRenewalDate > 0) ((nextRenewalDate - now) / (24 * 60 * 60 * 1000L)).toInt() else -1
    
    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val (venceColor, venceAlpha) = when {
        daysLeft in 0..3 || daysLeft < 0 -> Color(0xFFFF3D00) to blinkAlpha
        daysLeft in 4..5 -> Color(0xFFFFD54F) to blinkAlpha
        else -> Color(0xFF00C853) to 1f
    }

    val formattedRenewal = if (nextRenewalDate > 0) formatEpochDate(nextRenewalDate) else "N/A"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, statusColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
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
                Spacer(Modifier.height(6.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Plan: $planName",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Vence: $formattedRenewal",
                    color = venceColor.copy(alpha = venceAlpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = if (isActivo) Color(0xFF0B2914) else Color(0xFF380808),
                shape = CircleShape,
                border = BorderStroke(1.5.dp, statusColor),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Badge",
                        tint = statusColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BroadcastMessageSharedDisplay(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF002F3D)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun MemberActionSquareCard(
    title: String,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, PrimaryTurquoise),
        modifier = modifier
            .height(125.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = iconBg,
                shape = CircleShape,
                modifier = Modifier.size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
                    } else if (iconVector != null) {
                        Icon(imageVector = iconVector, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
