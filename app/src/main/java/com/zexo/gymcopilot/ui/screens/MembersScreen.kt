package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.zexo.gymcopilot.ChatMessage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.model.*

import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.ui.theme.*
import com.zexo.gymcopilot.utils.AttendanceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: (String) -> Unit = {},
    isAdmin: Boolean = true,
    isProfessor: Boolean = false,
    onMemberClick: (String) -> Unit, 
    onAddMemberClick: (() -> Unit)? = null,
    initialFilter: String = "ALL",
    attendanceRepository: com.zexo.gymcopilot.repository.AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val plans by dataStoreManager.getSubscriptionPlans().collectAsState(initial = emptyList())
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val allSchedules by dataStoreManager.getProfessorSchedules().collectAsState(initial = emptyMap())
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")
    val linkedProfId by dataStoreManager.getLinkedProfessorId().collectAsState(initial = null)

    val myID = remember(userName, userEmail, userRole) {
        val role = userRole?.lowercase() ?: ""
        if (role == "admin") "Admin" else userEmail.ifBlank { userName }.trim()
    }

    val activeClass = remember(allSchedules, linkedProfId, userName, userRole) {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = if (currentDay == Calendar.SUNDAY) 7 else currentDay - 1
        val currentTime = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val profId = linkedProfId ?: userName
        val schedules = (allSchedules[profId] ?: emptyList()) + (allSchedules["GENERAL_GYM"] ?: emptyList())
        schedules.find { entry ->
            entry.isEnabled && (entry.dayOfWeek == dayOfWeek || entry.dayOfWeek == null) &&
            currentTime.compareTo(entry.startTime) >= 0 && currentTime.compareTo(entry.endTime) <= 0
        }
    }

    LaunchedEffect(Unit) {
        if (isAdmin) {
            dataStoreManager.setLastReadTimestamp("admin_chats_global", System.currentTimeMillis())
        }
    }

    LaunchedEffect(gymApiUrl) {
        if (gymApiUrl.isNotBlank()) {
            val repo = attendanceRepository ?: com.zexo.gymcopilot.repository.AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
            repo.syncMembersFromServer()
        }
    }

    val unreadMap = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var globalUnreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(gymApiUrl, myID, members, isProfessor) {
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
                    var lastGeneralTs = 0L
                    var hasAnyIndividualUnread = false

                    for (i in 1 until rows.size()) {
                        val arr = rows.get(i).asJsonArray
                        if (arr.size() >= 4) {
                            val sender = arr.get(1).asString
                            val recipient = arr.get(2).asString.lowercase().trim()
                            val text = arr.get(3).asString
                            val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                            val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L
                            
                            val isMine = sender.lowercase().trim() == myID.lowercase().trim()
                            allMessages.add(ChatMessage(sender, recipient, text, ts, isMine))

                            if (recipient.contains("chat general")) {
                                if (ts > lastGeneralTs) lastGeneralTs = ts
                            } else if (recipient == myID.lowercase().trim() && !isMine) {
                                val sortedIds = listOf(myID.lowercase().trim(), sender.lowercase().trim()).sorted()
                                val chatKey = "${sortedIds[0]}_${sortedIds[1]}".replace(" ", "_")
                                val lastRead = dataStoreManager.getLastReadTimestamp(chatKey).first()
                                if (ts > lastRead) hasAnyIndividualUnread = true
                            }
                        }
                    }

                    val newUnreadMap = mutableMapOf<String, Boolean>()
                    for (m in members) {
                        val otherID = if (m.email.isNotBlank()) m.email else m.fullName
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
                        newUnreadMap[otherID] = hasNew
                    }
                    unreadMap.value = newUnreadMap

                    if (isProfessor) {
                        val lastReadGeneral = dataStoreManager.getLastReadTimestamp("chat_general_global").first()
                        globalUnreadCount = if (lastGeneralTs > lastReadGeneral || hasAnyIndividualUnread) 1 else 0
                    }
                }
            } catch (e: Exception) { Log.e("MembersScreen", "Sync error: ${e.message}") }
            delay(5000)
        }
    }

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    var selectedProfessor by remember { mutableStateOf<String?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var memberForProfessor by remember { mutableStateOf<Member?>(null) }

    val t = remember(appLanguage, memberDesignation, selectedFilter, isProfessor) {
        val plural = when(memberDesignation) {
            "Alumno" -> if (appLanguage == "Español") "Alumnos" else "Students"
            "Socio" -> if (appLanguage == "Español") "Socios" else "Partners"
            "Miembro" -> if (appLanguage == "Español") "Miembros" else "Members"
            "Estudiante" -> if (appLanguage == "Español") "Estudiantes" else "Students"
            "Cliente" -> if (appLanguage == "Español") "Clientes" else "Clients"
            "Atleta" -> if (appLanguage == "Español") "Atletas" else "Athletes"
            else -> "${memberDesignation}s"
        }
        
        val displayTitle = if (selectedFilter == "PRESENT") {
            if (appLanguage == "Español") "Asistencia" else "Attendance"
        } else if (isProfessor) {
            if (appLanguage == "Español") "Mis $plural" else "My $plural"
        } else {
            val prefix = if (onAddMemberClick != null) {
                if (appLanguage == "Español") "Gestión de" else "Manage"
            } else {
                if (appLanguage == "Español") "Lista de" else "List of"
            }
            "$prefix $memberDesignation"
        }

        if (appLanguage == "Español") {
            mapOf(
                "display_title" to displayTitle,
                "mgmt" to "Gestión de",
                "list" to "Lista de",
                "add" to "Agregar",
                "scan_qr" to "Escanear QR",
                "gen_list" to "Generar Lista",
                "list_updated" to "Lista de $plural actualizada",
                "total" to "TOTAL",
                "active" to (if (selectedFilter == "PRESENT") "PRESENTES" else "ACTIVOS"),
                "debtors" to "DEUDORES",
                "dropouts" to "DESERTORES",
                "search" to "Buscar $plural...",
                "profs" to "Profesores",
                "no_profs" to "Sin profesores registrados",
                "no_results" to "Sin resultados",
                "none_reg" to "Sin $plural registrados",
                "qr_instr" to "Apuntá la cámara al código QR del cliente",
                "cancel" to "CANCELAR",
                "reg_success" to "$memberDesignation registrado",
                "qr_error" to "Formato de QR no reconocido",
                "delete_confirm" to "¿Eliminar $memberDesignation?",
                "delete_msg" to "¿Estás seguro de que quieres eliminar a este $memberDesignation? Esta acción no se puede deshacer.",
                "delete" to "Eliminar",
                "deleted" to "$memberDesignation eliminado",
                "prof" to "Profesor Asignado",
                "no_prof" to "Sin asignar"
            )
        } else {
            mapOf(
                "mgmt" to "Manage",
                "list" to "List of",
                "add" to "Add",
                "scan_qr" to "Scan QR",
                "gen_list" to "Generate List",
                "list_updated" to "$plural list updated",
                "total" to "TOTAL",
                "active" to (if (selectedFilter == "PRESENT") "PRESENT" else "ACTIVE"),
                "debtors" to "DEBTORS",
                "dropouts" to "DROPOUTS",
                "search" to "Search $plural...",
                "profs" to "Professors",
                "no_profs" to "No professors registered",
                "no_results" to "No results",
                "none_reg" to "No $plural registered",
                "qr_instr" to "Point the camera at the client's QR code",
                "cancel" to "CANCEL",
                "reg_success" to "$memberDesignation registered",
                "qr_error" to "QR format not recognized",
                "delete_confirm" to "Delete $memberDesignation?",
                "delete_msg" to "Are you sure you want to delete this $memberDesignation? This action cannot be undone.",
                "delete" to "Delete",
                "deleted" to "$memberDesignation deleted",
                "prof" to "Assigned Professor",
                "no_prof" to "Unassigned"
            )
        }
    }

    val baseMembers = remember(members, isProfessor, isAdmin, userName, linkedProfId, professors) {
        if (isProfessor && !isAdmin) {
            val effectiveProfessorName = if (linkedProfId != null) {
                professors.find { it.id == linkedProfId }?.fullName ?: userName
            } else {
                userName
            }
            members.filter { it.assignedTrainer.equals(effectiveProfessorName, ignoreCase = true) }
        } else {
            members
        }
    }

    val filteredMembers = baseMembers.filter {
        val fullName = "${it.firstName} ${it.lastName}"
        val matchesSearch = fullName.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "DEUDOR" -> it.membershipStatus == "DEUDOR" || it.membershipStatus == "VENCIDO"
            "PRESENT" -> isPresentToday(it.lastVisit)
            else -> it.membershipStatus == selectedFilter
        }
        val matchesProfessor = if (selectedProfessor == null) true else it.assignedTrainer == selectedProfessor
        matchesSearch && matchesFilter && matchesProfessor
    }

    val stats = remember(baseMembers, filteredMembers, selectedFilter, activeClass) {
        if (selectedFilter == "PRESENT") {
            val classMemberEmails = activeClass?.assignedMemberEmails?.map { it.lowercase().trim() } ?: emptyList()
            val presentesEnClase = filteredMembers.count { it.email.lowercase().trim() in classMemberEmails }
            mapOf(
                "total" to filteredMembers.size,
                "active" to presentesEnClase,
                "debtor" to 0,
                "dropout" to 0
            )
        } else {
            mapOf(
                "total" to baseMembers.size,
                "active" to baseMembers.count { it.membershipStatus == "ACTIVO" },
                "debtor" to baseMembers.count { it.membershipStatus == "DEUDOR" || it.membershipStatus == "VENCIDO" },
                "dropout" to baseMembers.count { it.membershipStatus == "DESERTOR" || it.membershipStatus == "DROPOUT" }
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = t["display_title"] ?: "", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(34.dp).clip(CircleShape), colorFilter = ColorFilter.tint(accentColor))
                    }
                },
                actions = {
                    // Botón movido al FAB por solicitud del usuario
                }
            )
        },
        bottomBar = {
            if (isProfessor) {
                ProfessorBottomNavigation(
                    currentRoute = "members",
                    onHomeClick = onHomeClick,
                    onStoreClick = onStoreClick,
                    onScheduleClick = onScheduleClick,
                    onMembersClick = onMembersClick,
                    onRoutinesClick = onRoutinesClick,
                    onChatClick = { onChatClick("Admin") },
                    accentColor = accentColor,
                    unreadCount = globalUnreadCount
                )
            } else {
                AdminBottomNavigation(
                    currentRoute = "members",
                    onHomeClick = onHomeClick,
                    onStoreClick = onStoreClick,
                    onMembersClick = { },
                    onProfessorsClick = onProfessorsClick,
                    onSettingsClick = onSettingsClick,
                    accentColor = accentColor
                )
            }
        },
        floatingActionButton = {
            if (onAddMemberClick != null && isAdmin) {
                FloatingActionButton(
                    onClick = onAddMemberClick,
                    containerColor = accentColor,
                    contentColor = Color(0xFF0C1221), // Color de fondo para simular transparencia
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "${t["add"]} $memberDesignation") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 100.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        label = t["total"] ?: "", 
                        value = stats["total"].toString(), 
                        icon = Icons.Default.Groups, 
                        color = accentColor, 
                        accentColor = accentColor, 
                        shape = containerShape, 
                        isSelected = selectedFilter == "ALL", 
                        onClick = { selectedFilter = "ALL" }, 
                        modifier = Modifier.weight(1f).height(105.dp)
                    )
                    StatBox(
                        label = t["active"] ?: "", 
                        value = stats["active"].toString(), 
                        icon = if (selectedFilter == "PRESENT") painterResource(id = R.drawable.asistencia) else Icons.Default.Person, 
                        color = Color(0xFF4CAF50), 
                        accentColor = accentColor, 
                        shape = containerShape, 
                        isSelected = selectedFilter == "ACTIVO" || selectedFilter == "PRESENT", 
                        onClick = { if (selectedFilter != "PRESENT") selectedFilter = "ACTIVO" }, 
                        modifier = Modifier.weight(1f).height(105.dp),
                        iconSize = if (selectedFilter == "PRESENT") 32.dp else 40.dp
                    )
                    if (isAdmin && selectedFilter != "PRESENT") {
                        StatBox(label = t["debtors"] ?: "", value = stats["debtor"].toString(), icon = Icons.Default.Warning, color = Color(0xFFF44336), accentColor = accentColor, shape = containerShape, isSelected = selectedFilter == "DEUDOR", onClick = { selectedFilter = "DEUDOR" }, modifier = Modifier.weight(1f).height(105.dp))
                        StatBox(label = t["dropouts"] ?: "", value = stats["dropout"].toString(), icon = Icons.Default.PersonOff, color = Color(0xFFFF9800), accentColor = accentColor, shape = containerShape, isSelected = selectedFilter == "DESERTOR", onClick = { selectedFilter = "DESERTOR" }, modifier = Modifier.weight(1f).height(105.dp))
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    placeholder = { Text(t["search"] ?: "", color = TextGray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor) },
                    shape = containerShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF00222E), unfocusedContainerColor = Color(0xFF00222E), focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
                
                if (isAdmin) {
                    Spacer(Modifier.height(20.dp))
                    Text(text = t["profs"] ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(10.dp))
                    if (professors.isEmpty()) {
                        Text(text = t["no_profs"] ?: "", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp))
                    } else {
                        LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(professors) { prof ->
                                val profColor = prof.profileColor?.let { Color(it) } ?: getProfessorColorById(prof.id)
                                val isSelected = selectedProfessor == prof.fullName
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp)) {
                                    Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)).background(profColor.copy(alpha = 0.12f)).border(width = if (isSelected) 3.5.dp else 2.5.dp, color = profColor, shape = RoundedCornerShape(14.dp)).clickable { selectedProfessor = if (isSelected) null else prof.fullName }, contentAlignment = Alignment.Center) {
                                        if (prof.photoUri != null && prof.photoUri!!.isNotBlank()) {
                                            val imgModel = remember(prof.photoUri) {
                                                if (prof.photoUri!!.startsWith("data:image")) {
                                                    try {
                                                        val base64String = prof.photoUri!!.substringAfter("base64,")
                                                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                    } catch (e: Exception) { prof.photoUri }
                                                } else prof.photoUri
                                            }
                                            AsyncImage(model = imgModel, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = profColor, modifier = Modifier.size(36.dp))
                                        }
                                    }
                                    Text(text = prof.firstName, color = if (isSelected) TextWhite else TextGray, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            if (selectedFilter == "PRESENT") {
                val classMemberEmails = activeClass?.assignedMemberEmails?.map { it.lowercase().trim() } ?: emptyList()
                val classFiltered = filteredMembers.filter { it.email.lowercase().trim() in classMemberEmails }
                val othersFiltered = filteredMembers.filter { it.email.lowercase().trim() !in classMemberEmails }

                if (classFiltered.isNotEmpty()) {
                    item {
                        Text(
                            text = if (appLanguage == "Español") "Alumnos de la Clase (${activeClass?.eventName ?: ""})" else "Class Members (${activeClass?.eventName ?: ""})",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    items(classFiltered) { member ->
                        val identifier = if (member.email.isNotBlank()) member.email else member.fullName
                        val profOfMember = professors.find { it.fullName == member.assignedTrainer }
                        val rowBorderColor = profOfMember?.let { prof -> prof.profileColor?.let { Color(it) } ?: getProfessorColorById(prof.id) }
                        
                        val expectedClasses = remember(allSchedules, member.email) {
                            val flatSchedules = allSchedules.values.flatten()
                            AttendanceUtils.calculateExpectedClassesInMonth(flatSchedules, member.email)
                        }

                        MemberItemRow(
                            member = member, 
                            shape = containerShape, 
                            accentColor = accentColor, 
                            profBorderColor = rowBorderColor, 
                            assignedProfessor = profOfMember,
                            hasUnreadMessages = unreadMap.value[identifier] ?: false, 
                            onClick = { onMemberClick(member.email) },
                            onDelete = { memberToDelete = member }, 
                            onChat = { onChatClick(identifier) }, 
                            onAssignProfessor = { memberForProfessor = member },
                            isAdmin = isAdmin,
                            isAttendanceMode = true,
                            plans = plans,
                            expectedClassesInMonth = expectedClasses
                        )
                    }
                }

                if (othersFiltered.isNotEmpty()) {
                    item {
                        Text(
                            text = if (appLanguage == "Español") "Otros Presentes" else "Other Present",
                            color = TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    items(othersFiltered) { member ->
                        val identifier = if (member.email.isNotBlank()) member.email else member.fullName
                        val profOfMember = professors.find { it.fullName == member.assignedTrainer }
                        val rowBorderColor = profOfMember?.let { prof -> prof.profileColor?.let { Color(it) } ?: getProfessorColorById(prof.id) }
                        
                        val expectedClasses = remember(allSchedules, member.email) {
                            val flatSchedules = allSchedules.values.flatten()
                            AttendanceUtils.calculateExpectedClassesInMonth(flatSchedules, member.email)
                        }

                        MemberItemRow(
                            member = member, 
                            shape = containerShape, 
                            accentColor = accentColor, 
                            profBorderColor = rowBorderColor, 
                            assignedProfessor = profOfMember,
                            hasUnreadMessages = unreadMap.value[identifier] ?: false, 
                            onClick = { onMemberClick(member.email) },
                            onDelete = { memberToDelete = member }, 
                            onChat = { onChatClick(identifier) }, 
                            onAssignProfessor = { memberForProfessor = member },
                            isAdmin = isAdmin,
                            isAttendanceMode = true,
                            plans = plans,
                            expectedClassesInMonth = expectedClasses
                        )
                    }
                }
            } else {
                items(filteredMembers) { member ->
                    val identifier = if (member.email.isNotBlank()) member.email else member.fullName
                    val profOfMember = professors.find { it.fullName == member.assignedTrainer }
                    val rowBorderColor = profOfMember?.let { prof -> prof.profileColor?.let { Color(it) } ?: getProfessorColorById(prof.id) }
                    
                    val expectedClasses = remember(allSchedules, member.email) {
                        val flatSchedules = allSchedules.values.flatten()
                        AttendanceUtils.calculateExpectedClassesInMonth(flatSchedules, member.email)
                    }

                    MemberItemRow(
                        member = member, 
                        shape = containerShape, 
                        accentColor = accentColor, 
                        profBorderColor = rowBorderColor, 
                        assignedProfessor = profOfMember,
                        hasUnreadMessages = unreadMap.value[identifier] ?: false, 
                        onClick = { onMemberClick(member.email) },
                        onDelete = { memberToDelete = member }, 
                        onChat = { onChatClick(identifier) }, 
                        onAssignProfessor = { memberForProfessor = member },
                        isAdmin = isAdmin,
                        isAttendanceMode = isProfessor,
                        plans = plans,
                        expectedClassesInMonth = expectedClasses
                    )
                }
            }
            
            if (filteredMembers.isEmpty() && members.isNotEmpty()) {
                item { Text(if (searchQuery.isNotEmpty() || selectedProfessor != null) t["no_results"] ?: "" else t["none_reg"] ?: "", color = TextGray, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center) }
            }
        }
    }

    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text(t["delete_confirm"] ?: "", color = TextWhite) },
            text = { Text(t["delete_msg"] ?: "", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = {
                    memberToDelete?.let { m ->
                        coroutineScope.launch {
                            dataStoreManager.removeMember(m.email, m.fullName)
                            val repo = attendanceRepository ?: com.zexo.gymcopilot.repository.AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
                            repo.deleteMember(email = m.email, firstName = m.firstName, lastName = m.lastName)
                            memberToDelete = null
                        }
                    }
                }) { Text(t["delete"] ?: "Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) { Text(t["cancel"] ?: "CANCELAR", color = TextGray) }
            }
        )
    }

    if (memberForProfessor != null) {
        Dialog(onDismissRequest = { memberForProfessor = null }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF081C24)).padding(24.dp)) {
                Column {
                    Text(t["prof"] ?: "Profesor Asignado", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).clickable {
                            coroutineScope.launch {
                                memberForProfessor?.let { m ->
                                    val updated = m.copy(assignedTrainer = "")
                                    dataStoreManager.updateMember(updated)
                                    
                                    // Sincronización inmediata con el servidor
                                    attendanceRepository?.syncMemberProfile(
                                        updated.firstName, updated.lastName, updated.email,
                                        updated.phone, updated.address, updated.weight, updated.height,
                                        updated.photoUri, updated.membershipStatus, updated.nextRenewalDate,
                                        updated.planId, updated.planType, updated.assignedTrainer
                                    )
                                }
                                memberForProfessor = null
                            }
                        }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonOff, null, tint = TextGray, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(t["no_prof"] ?: "Sin asignar", color = TextGray)
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    professors.forEach { prof ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).clickable {
                                coroutineScope.launch {
                                    memberForProfessor?.let { m ->
                                        val updated = m.copy(assignedTrainer = prof.fullName)
                                        dataStoreManager.updateMember(updated)
                                        
                                        // Sincronización inmediata con el servidor
                                        attendanceRepository?.syncMemberProfile(
                                            updated.firstName, updated.lastName, updated.email,
                                            updated.phone, updated.address, updated.weight, updated.height,
                                            updated.photoUri, updated.membershipStatus, updated.nextRenewalDate,
                                            updated.planId, updated.planType, updated.assignedTrainer
                                        )
                                    }
                                    memberForProfessor = null
                                }
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val profColor = prof.profileColor?.let { Color(it) } ?: getProfessorColorById(prof.id)
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(profColor.copy(alpha = 0.1f)).border(1.dp, profColor, CircleShape), contentAlignment = Alignment.Center) {
                                if (prof.photoUri != null && prof.photoUri!!.isNotBlank()) {
                                    AsyncImage(model = prof.photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null, tint = profColor, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(prof.fullName, color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, icon: Any, color: Color, accentColor: Color, shape: Shape, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, iconSize: androidx.compose.ui.unit.Dp = 40.dp) {
    val bg = if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
    val borderCol = if (isSelected) color else Color.White.copy(alpha = 0.05f)
    Box(modifier = modifier.clip(shape).background(bg).border(1.dp, borderCol, shape).clickable { onClick() }.padding(8.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                when (icon) {
                    is ImageVector -> Icon(icon, null, tint = color, modifier = Modifier.fillMaxSize())
                    is Painter -> Icon(icon, null, tint = color, modifier = Modifier.padding(2.dp).fillMaxSize())
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MemberItemRow(
    member: Member, 
    shape: Shape, 
    accentColor: Color, 
    profBorderColor: Color?, 
    assignedProfessor: Professor? = null,
    hasUnreadMessages: Boolean = false, 
    onClick: () -> Unit,
    onDelete: () -> Unit, 
    onChat: () -> Unit, 
    onAssignProfessor: () -> Unit,
    isAdmin: Boolean = true,
    isAttendanceMode: Boolean = false,
    plans: List<SubscriptionPlan> = emptyList(),
    expectedClassesInMonth: Int = 0
) {
    val statusColor = when (member.membershipStatus) { "ACTIVO", "ACTIVE" -> Color(0xFF4CAF50); "EXPIRED", "DEBTOR", "DEUDOR", "VENCIDO" -> Color(0xFFF44336); "DROPOUT", "DESERTOR" -> Color(0xFFFF9800); else -> TextGray }
    val borderColor = profBorderColor ?: Color.White.copy(alpha = 0.05f)
    val borderWidth = if (profBorderColor != null) 2.dp else 1.dp
    
    val plan = remember(member.planId, plans) { plans.find { it.id == member.planId } }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(borderWidth, borderColor, shape)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (member.photoUri != null && member.photoUri!!.isNotBlank()) {
                    val imgModel = remember(member.photoUri) {
                        if (member.photoUri!!.startsWith("data:image")) {
                            try {
                                val base64String = member.photoUri!!.substringAfter("base64,")
                                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) { member.photoUri }
                        } else member.photoUri
                    }
                    AsyncImage(
                        model = imgModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        member.firstName.take(1).uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.fullName.lowercase(),
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isAttendanceMode) {
                    val attendanceText = if (expectedClassesInMonth > 0) {
                        "${member.monthAttendanceCount} / $expectedClassesInMonth Asistencias"
                    } else {
                        "${member.monthAttendanceCount} Asistencias"
                    }
                    Text(
                        attendanceText,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        member.membershipStatus,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onChat, modifier = Modifier.size(40.dp)) {
                    Box {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = accentColor, modifier = Modifier.size(24.dp))
                        if (hasUnreadMessages) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, CircleShape)
                                    .border(1.5.dp, Color(0xFF081C24), CircleShape)
                            )
                        }
                    }
                }
                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onAssignProfessor, modifier = Modifier.size(40.dp)) {
                        val profPhoto = assignedProfessor?.photoUri.orEmpty()
                        if (profPhoto.isNotBlank()) {
                            val profIcon = remember(profPhoto) {
                                if (profPhoto.startsWith("data:image")) {
                                    try {
                                        val base64String = profPhoto.substringAfter("base64,")
                                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    } catch (e: Exception) { profPhoto }
                                } else profPhoto
                            }
                            AsyncImage(
                                model = profIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, borderColor, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.assistant),
                                contentDescription = "Asignar Profesor",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun isPresentToday(lastVisit: Long?): Boolean {
    if (lastVisit == null) return false
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = lastVisit }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
