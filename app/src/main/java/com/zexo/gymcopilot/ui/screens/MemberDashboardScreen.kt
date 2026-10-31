package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.JsonArray
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.LockConfig
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.ui.theme.getGymFontFamily
import com.zexo.gymcopilot.utils.ShortcutUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.text.SimpleDateFormat
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    onProfileClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onClassesClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onRoutineDetailClick: (String) -> Unit,
    onChatClick: (String?) -> Unit,
    onHelpClick: () -> Unit = {},
    repository: AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymNameFont by dataStoreManager.getGymNameFont().collectAsState(initial = "Default")
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    val showLogoBorder by dataStoreManager.getShowLogoBorder().collectAsState(initial = true)
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")

    val selectedFontFamily = remember(gymNameFont) { getGymFontFamily(gymNameFont) }

    val plans by dataStoreManager.getSubscriptionPlans().collectAsState(initial = emptyList())
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val currentMember = remember(members, userEmail) { members.find { it.email.equals(userEmail, ignoreCase = true) } }

    val myProfessor = remember(currentMember, professors) {
        if (currentMember?.assignedTrainer.isNullOrBlank()) null
        else professors.find { it.fullName.equals(currentMember.assignedTrainer, ignoreCase = true) }
    }

    val myID = remember(userName, userEmail, userRole) {
        val role = userRole?.lowercase() ?: ""
        if (role == "admin") "Admin" else userEmail.ifBlank { userName }.trim()
    }

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val shortcutCreated by dataStoreManager.isShortcutCreated().collectAsState(initial = false)
    val lastShortcutName by dataStoreManager.getLastShortcutName().collectAsState(initial = "")
    val lastShortcutLogoUri by dataStoreManager.getLastShortcutLogoUri().collectAsState(initial = "")

    LaunchedEffect(gymName, gymLogoUri, shortcutCreated, lastShortcutName, lastShortcutLogoUri) {
        if (gymName.isBlank()) return@LaunchedEffect
        
        if (!shortcutCreated) {
            ShortcutUtils.createGymShortcut(context, gymName, gymLogoUri)
            dataStoreManager.setShortcutCreated(true)
            dataStoreManager.setLastShortcutName(gymName)
            dataStoreManager.setLastShortcutLogoUri(gymLogoUri ?: "")
        } else {
            // Actualización silenciosa si hubo cambios
            if (gymName != lastShortcutName || (gymLogoUri ?: "") != lastShortcutLogoUri) {
                ShortcutUtils.updateGymShortcut(context, gymName, gymLogoUri)
                dataStoreManager.setLastShortcutName(gymName)
                dataStoreManager.setLastShortcutLogoUri(gymLogoUri ?: "")
            }
        }
    }

    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val gymIsOpen by dataStoreManager.getGymIsOpen().collectAsState(initial = true)
    val classInSession by dataStoreManager.getClassInSessionManual().collectAsState(initial = false)
    var unreadCount by remember { mutableIntStateOf(0) }

    // 1. Sincronización RÁPIDA (Cada 10 segundos) para el estado del gimnasio y clases
    LaunchedEffect(repository) {
        while (isActive) {
            try {
                repository?.syncGymConfigFromServer()
            } catch (e: Exception) { }
            delay(10000)
        }
    }

    // 2. Sincronización de DATOS (Chats, Rutinas, etc.)
    LaunchedEffect(gymApiUrl, myID, repository) {
        if (gymApiUrl.isBlank()) return@LaunchedEffect
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)
        val syncUrl = if (gymApiUrl.contains("?")) "$gymApiUrl&type=chats" else "$gymApiUrl?type=chats"

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

                    var lastGeneralTs = 0L
                    var hasIndividualUnread = false

                    for (i in 1 until rows.size()) {
                        val arr = rows.get(i).asJsonArray
                        if (arr.size() >= 4) {
                            val sender = arr.get(1).asString
                            val recipient = arr.get(2).asString.lowercase().trim()
                            val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                            val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L

                            if (recipient.contains("chat general")) {
                                if (ts > lastGeneralTs) lastGeneralTs = ts
                            } else if (recipient == myID.lowercase().trim() && sender.lowercase().trim() != myID.lowercase().trim()) {
                                val sortedIds = listOf(myID.lowercase().trim(), sender.lowercase().trim()).sorted()
                                val chatKey = "${sortedIds[0]}_${sortedIds[1]}".replace(" ", "_")
                                val lastRead = dataStoreManager.getLastReadTimestamp(chatKey).first()
                                if (ts > lastRead) hasIndividualUnread = true
                            }
                        }
                    }

                    val lastReadGeneral = dataStoreManager.getLastReadTimestamp("chat_general_global").first()
                    unreadCount = if (lastGeneralTs > lastReadGeneral || hasIndividualUnread) 1 else 0
                }

                repository?.syncProfessorsFromServer()
                repository?.syncRoutinesFromServer()
                repository?.syncMembersFromServer()
                repository?.syncSubscriptionPlansFromServer()

            } catch (e: Exception) { }
            
            delay(30000) // Los datos pesados se actualizan cada 30 segundos
        }
    }

    var showLoginQrDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    val membershipStatus = remember(currentMember) {
        if (LockConfig.IS_ROLE_LOCK_ENABLED == 0) "Vigente"
        else if (currentMember == null) "N/A"
        else if (currentMember.membershipStatus == "DEUDOR" || currentMember.membershipStatus == "DEBTOR") "Deudor"
        else if (currentMember.membershipStatus == "ACTIVO" || currentMember.membershipStatus == "ACTIVE") {
            val isExpired = currentMember.nextRenewalDate < System.currentTimeMillis()
            if (isExpired) "Vencida" else "Vigente"
        }
        else if (currentMember.membershipStatus == "VENCIDO" || currentMember.membershipStatus == "EXPIRED") "Vencida"
        else currentMember.membershipStatus
    }
    
    val nextRenewal = remember(currentMember) {
        if (currentMember == null) "N/A"
        else {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(currentMember.nextRenewalDate))
        }
    }

    val daysRemaining = remember(currentMember) {
        if (currentMember == null) null
        else {
            val now = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val renewal = Calendar.getInstance().apply { 
                timeInMillis = currentMember.nextRenewalDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val diff = renewal - now
            (diff / (1000L * 60 * 60 * 24))
        }
    }

    val activePlanName = remember(currentMember, plans) {
        val plan = plans.find { it.id == currentMember?.planId } ?: plans.find { it.name == currentMember?.planType }
        plan?.name ?: "Plan Mensual"
    }

    val savedRoutines by dataStoreManager.getRoutines().collectAsState(initial = emptyList())
    val todayRoutine = remember(savedRoutines, userEmail) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val todayTag = when (dayOfWeek) {
            Calendar.MONDAY -> "LU"
            Calendar.TUESDAY -> "MA"
            Calendar.WEDNESDAY -> "MI"
            Calendar.THURSDAY -> "JU"
            Calendar.FRIDAY -> "VI"
            Calendar.SATURDAY -> "SÁ"
            Calendar.SUNDAY -> "DO"
            else -> ""
        }
        
        val normalizedEmail = userEmail.lowercase().trim()
        val myRoutines = savedRoutines.filter { 
            it.creatorEmail.lowercase().trim() == normalizedEmail || 
            it.assignedMemberEmails.any { e -> e.lowercase().trim() == normalizedEmail } 
        }
        
        myRoutines.find { it.days.contains(todayTag) }
    }

    if (showLoginQrDialog) {
        val qrData = "ATTENDANCE:$gymName:$myID"
        LoginQRCodeDialog(
            content = qrData,
            accentColor = accentColor,
            onDismiss = { showLoginQrDialog = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // LOGO (130dp)
                    val logoShape = if (showLogoBorder) containerShape else RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(logoShape)
                            .then(if (showLogoBorder) {
                                Modifier.border(2.5.dp, accentColor, logoShape)
                            } else Modifier)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (gymLogoUri != null) {
                            val logoModel = remember(gymLogoUri) {
                                if (gymLogoUri?.startsWith("data:image") == true) {
                                    try {
                                        val base64String = gymLogoUri!!.substringAfter("base64,")
                                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    } catch (e: Exception) { gymLogoUri }
                                } else gymLogoUri
                            }
                            AsyncImage(
                                model = logoModel,
                                contentDescription = "Gym Logo",
                                modifier = Modifier.fillMaxSize().padding(if (showLogoBorder) 8.dp else 0.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                tint = accentColor.copy(0.3f),
                                modifier = Modifier.size(58.dp)
                            )
                        }
                    }

                    // COLUMN FOR ICON AND NAMES
                    Column(
                        modifier = Modifier
                            .height(130.dp)
                            .weight(1f)
                            .clickable { onProfileClick() }
                    ) {
                        // Help & Profile Icons at Top Right
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onProfileClick) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = onHelpClick) {
                                Image(
                                    painter = painterResource(id = R.drawable.information),
                                    contentDescription = "Help",
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                    colorFilter = ColorFilter.tint(accentColor)
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Names Aligned to Bottom of Logo
                        Text(
                            text = gymName,
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = selectedFontFamily,
                            lineHeight = 26.sp
                        )
                        if (userName.isNotBlank()) {
                            Text(
                                text = userName,
                                color = accentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            MemberBottomNavigation(
                currentRoute = "member_dashboard",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onClassesClick = onClassesClick,
                onRoutinesClick = onRoutinesClick,
                onChatClick = { onChatClick(null) },
                accentColor = accentColor,
                unreadCount = unreadCount
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(
                        text = if (gymIsOpen) "GIMNASIO ABIERTO" else "GIMNASIO CERRADO",
                        color = if (gymIsOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        text = if (classInSession) "CLASES EN VIVO" else "SIN CLASES AHORA",
                        color = if (classInSession) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )

                    // Icono del Profesor Asignado
                    if (myProfessor != null) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(containerShape)
                                .clickable { onChatClick(myProfessor.email) },
                            color = accentColor.copy(alpha = 0.1f),
                            shape = containerShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!myProfessor.photoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = myProfessor.photoUri,
                                        contentDescription = myProfessor.fullName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.assistant),
                                        contentDescription = myProfessor.fullName,
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    MembershipStatusCard(
                        status = membershipStatus,
                        renewalDate = nextRenewal,
                        planName = activePlanName,
                        daysRemaining = daysRemaining,
                        accentColor = accentColor,
                        shape = containerShape
                    )
                }
            }

            item {
                BroadcastMessageDisplay(dataStoreManager, accentColor)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Mi QR Login",
                        icon = Icons.Default.QrCode,
                        modifier = Modifier.weight(1f),
                        onClick = { showLoginQrDialog = true },
                        shape = containerShape,
                        accentColor = accentColor,
                        iconColor = accentColor
                    )
                    ActionCard(
                        title = "Check-in",
                        icon = Icons.Default.QrCodeScanner,
                        modifier = Modifier.weight(1f),
                        onClick = onCheckInClick,
                        shape = containerShape,
                        accentColor = accentColor,
                        iconColor = Color(0xFF4CAF50)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Mis Rutinas",
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f),
                        onClick = onRoutinesClick,
                        shape = containerShape,
                        accentColor = accentColor,
                        iconColor = Color(0xFF2196F3)
                    )
                    ActionCard(
                        title = "Timer",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f),
                        onClick = { showTimerDialog = true },
                        shape = containerShape,
                        accentColor = accentColor,
                        iconColor = Color(0xFFFFC107)
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TodayRoutineCard(
                        routine = todayRoutine, 
                        accentColor = accentColor, 
                        onRoutinesClick = onRoutinesClick,
                        onViewDetailClick = onRoutineDetailClick,
                        shape = containerShape
                    )
                }
            }
        }

        if (showTimerDialog) {
            AdaptiveTimerDialog(
                onDismiss = { showTimerDialog = false },
                accentColor = accentColor,
                shape = containerShape
            )
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(48.dp),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MembershipStatusCard(
    status: String,
    renewalDate: String,
    planName: String,
    daysRemaining: Long?,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape
) {
    val statusColor = if (status == "Vigente") Color(0xFF4CAF50) else Color(0xFFF44336)
    
    val countdownColor = when {
        daysRemaining == null -> TextGray
        daysRemaining >= 3L -> Color(0xFF4CAF50) // Verde
        daysRemaining == 2L -> Color(0xFFFFEB3B) // Amarillo
        else -> Color(0xFFF44336) // Rojo (1 día o menos)
    }

    val isWarning = daysRemaining != null && daysRemaining <= 2L
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isWarning) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.5.dp, accentColor, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = LocalButtonBackgroundColor.current)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ESTADO DE MEMBRESÍA", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(status, color = statusColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Plan: $planName", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .graphicsLayer {
                            alpha = pulseAlpha
                        }
                ) {
                    Text("Vence: $renewalDate", color = countdownColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (daysRemaining != null) {
                        val daysTxt = when {
                            daysRemaining < 0L -> " (Vencido)"
                            daysRemaining == 0L -> " (Vence hoy!)"
                            daysRemaining == 1L -> " (Falta 1 día)"
                            else -> " (Quedan $daysRemaining días)"
                        }
                        Text(
                            text = daysTxt,
                            color = countdownColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Box(modifier = Modifier.size(60.dp).background(Color(0xFF0A2F35), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VerifiedUser, null, tint = statusColor, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    accentColor: Color = PrimaryTurquoise,
    iconColor: Color? = null
) {
    val finalIconColor = iconColor ?: accentColor
    val buttonBgColor = LocalButtonBackgroundColor.current
    val titleColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

    Surface(
        modifier = modifier
            .height(120.dp)
            .clip(shape)
            .clickable { onClick() }
            .border(1.5.dp, accentColor, shape),
        color = buttonBgColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(finalIconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = finalIconColor,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BigActionCard(title: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit, shape: androidx.compose.ui.graphics.Shape) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val titleColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else accentColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        color = buttonBgColor,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TodayRoutineCard(
    routine: com.zexo.gymcopilot.Routine?,
    accentColor: Color,
    onRoutinesClick: () -> Unit,
    onViewDetailClick: (String) -> Unit,
    shape: androidx.compose.ui.graphics.Shape
) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val titleColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = buttonBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("RUTINA DE HOY", color = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            if (routine != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(routine.name, color = titleColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onViewDetailClick(routine.id) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, accentColor, shape),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBgColor),
                    shape = shape
                ) {
                    Text("VER DETALLE DE RUTINA", color = titleColor, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("¡Día de Descanso!", color = titleColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onRoutinesClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, accentColor, shape),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBgColor),
                    shape = shape
                ) {
                    Text("VER TODAS MIS RUTINAS", color = titleColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoginQRCodeDialog(content: String, accentColor: Color, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF00222E)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Mi QR Login",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // QR Code placeholder - in real app use a library like ZXing
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val qrBitmap = remember(content) { generateQrCode(content, 512) }
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Muestra este código en la recepción para marcar tu asistencia",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().border(1.dp, accentColor, RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                    Text("CERRAR", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateQrCode(content: String, size: Int): android.graphics.Bitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
