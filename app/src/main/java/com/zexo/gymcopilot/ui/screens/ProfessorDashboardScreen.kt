package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.*
import com.zexo.gymcopilot.utils.ShortcutUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorDashboardScreen(
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onClassesClick: () -> Unit = {},
    onMembersClick: (String?) -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onProfessorClick: (String) -> Unit = {},
    onScanClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val savedRoutines by dataStoreManager.getRoutines().collectAsState(initial = emptyList())
    val allProfessors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val professors = remember(allProfessors) { allProfessors.filter { it.fullName.isNotBlank() } }

    val currentUserName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val linkedProfId by dataStoreManager.getLinkedProfessorId().collectAsState(initial = null)
    
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")
    val classInSession by dataStoreManager.getClassInSessionManual().collectAsState(initial = false)
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymNameFont by dataStoreManager.getGymNameFont().collectAsState(initial = "Default")
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    val showLogoBorder by dataStoreManager.getShowLogoBorder().collectAsState(initial = true)

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    val selectedFontFamily = remember(gymNameFont) { getGymFontFamily(gymNameFont) }

    // Hacer que selectedProfessor sea reactivo a los cambios en allProfessors
    val selectedProfessor = remember(allProfessors, linkedProfId, currentUserName) {
        if (linkedProfId != null) {
            allProfessors.find { it.id == linkedProfId }
        } else {
            allProfessors.find { it.fullName.equals(currentUserName, ignoreCase = true) }
        } ?: allProfessors.firstOrNull()
    }

    var unreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(attendanceRepository) {
        attendanceRepository?.syncProfessorsFromServer()
        while (isActive) {
            attendanceRepository?.syncWithRemote()
            attendanceRepository?.syncGymConfigFromServer()
            delay(30000)
        }
    }

    LaunchedEffect(selectedProfessor) {
        if (selectedProfessor != null) {
            if (linkedProfId != selectedProfessor.id || currentUserName != selectedProfessor.fullName) {
                dataStoreManager.setLinkedProfessorId(selectedProfessor.id)
                dataStoreManager.setUserEmail(selectedProfessor.email)
                dataStoreManager.setUserName(selectedProfessor.fullName)
            }
        }
    }

    val activeProfessorName = selectedProfessor?.fullName ?: currentUserName
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

    val myMembers = remember(members, activeProfessorName) {
        if (activeProfessorName.isBlank()) emptyList()
        else members.filter { it.assignedTrainer.equals(activeProfessorName, ignoreCase = true) }
    }

    val completedCount = remember(savedRoutines) { savedRoutines.count { it.status == "Completada" } }
    val assignedCount = remember(savedRoutines) { savedRoutines.count { it.type == "Asignada" && !it.id.contains("::") } }

    val recentActivity = remember(myMembers) {
        myMembers.filter { it.lastVisit != null }
            .sortedByDescending { it.lastVisit }
            .take(10)
    }

    val t = remember(appLanguage, memberDesignation) {
        val plural = when(memberDesignation) {
            "Alumno" -> "Alumnos"
            "Socio" -> "Socios"
            else -> "${memberDesignation}s"
        }
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to "Panel Profesor", "my_members" to "Mis $plural",
                "attendance" to "Asistencia $plural", "routines" to "Rutinas", "schedule" to "Mis Horarios",
                "prof_attendance" to "Marcar Mi Asistencia",
                "recent" to "Actividad Reciente", "no_activity" to "Sin actividad de tus alumnos",
                "entry_today" to "Entrada hoy", "setup_profile" to "Configura tu nombre en Perfil para ver tus alumnos",
                "class_on" to "Iniciar Clase", "class_off" to "Finalizar Clase", "class_active" to "CLASE ACTIVA",
                "staff" to "Staff de Profesores", "classes" to "Clases"
            )
            else -> mapOf(
                "title" to "Professor Panel", "my_members" to "My Members",
                "attendance" to "Members Attendance", "routines" to "Routines", "schedule" to "My Schedule",
                "prof_attendance" to "Mark My Attendance",
                "recent" to "Recent Activity", "no_activity" to "No activity from your members",
                "entry_today" to "Entry today", "setup_profile" to "Set your name in Profile to see your members",
                "class_on" to "Start Class", "class_off" to "End Class", "class_active" to "CLASS ACTIVE",
                "staff" to "Staff Professors", "classes" to "Classes"
            )
        }
    }

    Scaffold(
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
                            } else Modifier),
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
                    ) {
                        // Help Icon at Top Right
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
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
                        if (activeProfessorName.isNotBlank()) {
                            Text(
                                text = activeProfessorName,
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
            ProfessorBottomNavigation(
                currentRoute = "professor_dashboard",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onScheduleClick = onScheduleClick,
                onMembersClick = { onMembersClick(null) },
                onRoutinesClick = onRoutinesClick,
                onChatClick = onChatClick,
                accentColor = accentColor,
                unreadCount = unreadCount
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Column {
                        Text(t["staff"] ?: "Staff de Profesores", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (professors.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.05f))
                                            .border(1.dp, accentColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            } else {
                                items(professors) { prof ->
                                    val profColor = prof.profileColor?.let { Color(it) } ?: accentColor
                                    val isSelected = selectedProfessor?.id == prof.id
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(enabled = isSelected) { 
                                            onProfileClick()
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) profColor.copy(alpha = 0.2f) else profColor.copy(alpha = 0.05f))
                                                .border(if (isSelected) 3.dp else 1.dp, profColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val photo = prof.photoUri.orEmpty()
                                            if (photo.isNotBlank()) {
                                                val imgModel = remember(photo) {
                                                    if (photo.startsWith("data:image")) {
                                                        try {
                                                            val base64String = photo.substringAfter("base64,")
                                                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                        } catch (e: Exception) { photo }
                                                    } else photo
                                                }
                                                AsyncImage(
                                                    model = imgModel,
                                                    contentDescription = prof.firstName,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(R.drawable.assistant),
                                                    contentDescription = prof.firstName,
                                                    tint = profColor,
                                                    modifier = Modifier.size(38.dp)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Box(Modifier.padding(top = 4.dp).size(6.dp).clip(CircleShape).background(profColor))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val boxModifier = Modifier.weight(1f).height(120.dp)
                        ProfessorActionCard(
                            text = "Rutinas",
                            iconPainter = rememberVectorPainter(Icons.Outlined.Description),
                            modifier = boxModifier,
                            accentColor = accentColor,
                            iconColor = accentColor,
                            shape = containerShape,
                            onClick = onRoutinesClick
                        )
                        ProfessorStatBox(label = "Completadas", value = completedCount.toString(), icon = Icons.Default.CheckCircle, color = Color(0xFF4CAF50), accentColor = accentColor, shape = containerShape, onClick = onRoutinesClick, modifier = boxModifier)
                        ProfessorStatBox(label = "Asignadas", value = assignedCount.toString(), icon = Icons.Default.Group, color = Color(0xFF2196F3), accentColor = accentColor, shape = containerShape, onClick = onRoutinesClick, modifier = boxModifier)
                    }
                }
            }

            item {
                BroadcastMessageDisplay(dataStoreManager, accentColor)
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        ProfessorMetricCard(t["my_members"] ?: "", myMembers.size.toString(), Icons.Default.Groups, accentColor, containerShape, { onMembersClick(null) }, Modifier.weight(2f).fillMaxHeight())
                        Card(
                            modifier = Modifier.weight(1f).fillMaxHeight().border(1.5.dp, if (classInSession) Color(0xFF2196F3) else accentColor, containerShape),
                            shape = containerShape,
                            colors = CardDefaults.cardColors(containerColor = LocalButtonBackgroundColor.current),
                            onClick = { 
                                scope.launch { 
                                    val newState = !classInSession
                                    dataStoreManager.setClassInSessionManual(newState)
                                    attendanceRepository?.syncGymInfo(classInSession = newState)
                                } 
                            }
                        ) {
                            val activeColor = if (classInSession) Color(0xFF2196F3) else accentColor
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(
                                    modifier = Modifier.size(56.dp).background(activeColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (classInSession) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        null,
                                        tint = activeColor,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (classInSession) t["class_off"]!! else t["class_on"]!!,
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfessorActionCard(
                            text = t["attendance"] ?: "", 
                            iconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.FactCheck), 
                            modifier = Modifier.weight(1f), 
                            accentColor = accentColor, 
                            iconColor = Color(0xFF4CAF50), // Verde
                            shape = containerShape, 
                            onClick = { onMembersClick("PRESENT") }
                        )
                        ProfessorActionCard(
                            text = t["prof_attendance"] ?: "", 
                            iconPainter = rememberVectorPainter(Icons.Default.QrCodeScanner), 
                            modifier = Modifier.weight(1f), 
                            accentColor = accentColor, 
                            iconColor = Color(0xFF2196F3), // Azul
                            shape = containerShape,
                            onClick = onScanClick
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfessorActionCard(
                            text = t["schedule"] ?: "", 
                            iconPainter = rememberVectorPainter(Icons.Default.Schedule), 
                            modifier = Modifier.weight(1f), 
                            accentColor = accentColor, 
                            iconColor = Color(0xFFFFC107), // Ámbar
                            shape = containerShape,
                            onClick = onScheduleClick
                        )
                        ProfessorActionCard(
                            text = t["classes"] ?: "",
                            iconPainter = painterResource(R.drawable.gym_class),
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor,
                            iconColor = Color(0xFFE91E63), // Rosa
                            shape = containerShape,
                            onClick = onClassesClick
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(t["recent"] ?: "", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }

            if (recentActivity.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(t["no_activity"] ?: "Sin actividad", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            } else {
                items(recentActivity) { member ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        ProfessorActivityRowItem(member, containerShape, accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessorActivityRowItem(member: Member, shape: Shape, accentColor: Color) {
    val isAttendingToday = remember(member.lastVisit) {
        member.lastVisit?.let {
            val lastVisitCal = Calendar.getInstance().apply { timeInMillis = it }
            val nowCal = Calendar.getInstance()
            lastVisitCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            lastVisitCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        } ?: false
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                if (member.photoUri != null) {
                    AsyncImage(member.photoUri, null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, null, tint = TextGray)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Acceso registrado", color = TextGray, fontSize = 13.sp)
            }
            if (isAttendingToday) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = accentColor)
        }
    }
}

@Composable
fun ProfessorStatBox(label: String, value: String, icon: ImageVector, color: Color, accentColor: Color, shape: Shape, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val valColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val lblColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(
        modifier = modifier.border(1.5.dp, accentColor, shape).clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = buttonBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier.size(50.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = valColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(label, color = lblColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfessorMetricCard(label: String, value: String, icon: ImageVector, color: Color, shape: Shape, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val valColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val lblColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(
        modifier = modifier.border(1.5.dp, color, shape).clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = buttonBgColor)
    ) {
        Row(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = lblColor, fontSize = 12.sp)
                Text(value, color = valColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier.size(64.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun ProfessorActionCard(text: String, iconPainter: Painter, modifier: Modifier, accentColor: Color, iconColor: Color? = null, shape: Shape, onClick: () -> Unit = {}) {
    val finalIconColor = iconColor ?: accentColor
    val buttonBgColor = LocalButtonBackgroundColor.current
    val textColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

    Surface(
        modifier = modifier.height(120.dp).clip(shape).clickable { onClick() }.border(1.5.dp, accentColor, shape),
        color = buttonBgColor
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(56.dp).background(finalIconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconPainter, null, tint = finalIconColor, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
fun ProfessorHeader(logoUri: String?, gymName: String, professorName: String, font: androidx.compose.ui.text.font.FontFamily, accentColor: Color, shape: Shape, showBorder: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val logoShape = if (showBorder) shape else RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(logoShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(2.dp, if (showBorder) accentColor else Color.Transparent, logoShape),
            contentAlignment = Alignment.Center
        ) {
            if (logoUri != null) {
                val logoModel = remember(logoUri) {
                    if (logoUri.startsWith("data:image")) {
                        try {
                            val base64String = logoUri.substringAfter("base64,")
                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) { logoUri }
                    } else logoUri
                }
                AsyncImage(model = logoModel, contentDescription = null, modifier = Modifier.fillMaxSize().padding(if (showBorder) 8.dp else 0.dp), contentScale = ContentScale.Fit)
            }
            else Icon(Icons.Default.AddPhotoAlternate, null, tint = accentColor.copy(0.3f), modifier = Modifier.size(58.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gymName,
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = font,
                lineHeight = 28.sp
            )
            if (professorName.isNotBlank()) {
                Text(
                    text = professorName,
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
