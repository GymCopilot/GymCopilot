package com.zexo.gymcopilot.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
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
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.PaymentRecord
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    memberEmail: String,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    isAdmin: Boolean = true,
    isProfessor: Boolean = false,
    attendanceRepository: com.zexo.gymcopilot.repository.AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val member = members.find { it.email == memberEmail }
    
    val plans by dataStoreManager.getSubscriptionPlans().collectAsState(initial = emptyList())
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val paymentRecords by dataStoreManager.getPaymentRecords().collectAsState(initial = emptyList())

    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Socio")
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")

    val myID = remember(userName, userEmail, userRole) {
        val role = userRole?.lowercase() ?: ""
        if (role == "admin") "Admin" else userEmail.ifBlank { userName }.trim()
    }

    var isEditing by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var planId by remember { mutableStateOf<String?>(null) }
    var planType by remember { mutableStateOf("") }
    var nextRenewalDate by remember { mutableLongStateOf(0L) }
    var routine by remember { mutableStateOf("") }
    var assignedTrainer by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var expandedPlans by remember { mutableStateOf(false) }
    var expandedProf by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(member, isEditing) {
        if (member != null && !isEditing) {
            firstName = member.firstName
            lastName = member.lastName
            phone = member.phone
            address = member.address
            weight = member.weight
            height = member.height
            planId = member.planId
            planType = member.planType
            nextRenewalDate = member.nextRenewalDate
            routine = member.routine
            assignedTrainer = member.assignedTrainer
            notes = member.notes
        }
    }

    val memberPlan = plans.find { it.id == planId } ?: plans.find { it.name == planType }

    var unreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(gymApiUrl, myID, isProfessor) {
        if (!isProfessor || gymApiUrl.isBlank()) return@LaunchedEffect
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
            } catch (e: Exception) { }
            delay(5000)
        }
    }

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAlreadyPaidDialog by remember { mutableStateOf(false) }
    var showNoPlanWarning by remember { mutableStateOf(false) }

    val t = remember(appLanguage, memberDesignation) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Detalle del $memberDesignation",
                "personal" to "Datos Personales",
                "membership" to "Membresía",
                "attendance" to "Asistencia",
                "routine" to "Rutina y Progreso",
                "notes" to "Notas del Entrenador",
                "delete_confirm" to "Eliminar $memberDesignation?",
                "delete_msg" to "Estás seguro de que quieres eliminar a este $memberDesignation? Esta acción no se puede deshacer.",
                "delete" to "Eliminar",
                "cancel" to "Cancelar",
                "deleted" to "$memberDesignation eliminado",
                "last_visit" to "Última visita",
                "no_records" to "Sin registros",
                "visits" to "Total visitas",
                "renewal" to "Próxima renovación",
                "plan" to "Plan",
                "no_notes" to "No hay notas para este $memberDesignation.",
                "trainer" to "Entrenador",
                "register_pay" to "Registrar Pago",
                "pay_success" to "Pago registrado con éxito",
                "pay_confirm" to "Registrar Pago de Membresía",
                "pay_msg" to "Se registrará el pago para el plan seleccionado y se actualizará la fecha de vencimiento.",
                "save" to "Guardar",
                "names" to "Nombres",
                "last_name" to "Apellido",
                "phone" to "WhatsApp/Tel",
                "address" to "Dirección",
                "weight" to "Peso (kg)",
                "height" to "Altura (cm)",
                "no_prof" to "Sin asignar",
                "changes_saved" to "Cambios guardados",
                "edit_date" to "Editar Fecha",
                "qr_title" to "Código QR del $memberDesignation",
                "qr_desc" to "Pídele al $memberDesignation que escanee este código para completar sus datos.",
                "close" to "Cerrar"
            )
        } else {
            mapOf(
                "title" to "$memberDesignation Details",
                "personal" to "Personal Data",
                "membership" to "Membership",
                "attendance" to "Attendance",
                "routine" to "Routine & Progress",
                "notes" to "Trainer Notes",
                "delete_confirm" to "Delete $memberDesignation?",
                "delete_msg" to "Are you sure you want to delete this $memberDesignation? This action cannot be undone.",
                "delete" to "Delete",
                "cancel" to "Cancel",
                "deleted" to "$memberDesignation deleted",
                "last_visit" to "Last visit",
                "no_records" to "No records",
                "visits" to "Total visits",
                "renewal" to "Next renewal",
                "plan" to "Plan",
                "no_notes" to "No notes for this $memberDesignation.",
                "trainer" to "Trainer",
                "register_pay" to "Register Payment",
                "pay_success" to "Payment registered successfully",
                "pay_confirm" to "Register Membership Payment",
                "pay_msg" to "Payment will be registered for the selected plan and the expiration date will be updated.",
                "save" to "Save",
                "names" to "First Name",
                "last_name" to "Last Name",
                "phone" to "Phone/WhatsApp",
                "address" to "Address",
                "weight" to "Weight (kg)",
                "height" to "Height (cm)",
                "no_prof" to "Unassigned",
                "changes_saved" to "Changes saved",
                "edit_date" to "Edit Date",
                "qr_title" to "$memberDesignation QR Code",
                "qr_desc" to "Ask the $memberDesignation to scan this code to complete their data.",
                "close" to "Close"
            )
        }
    }

    if (member == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentColor)
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { if(isEditing) isEditing = false else onBack() }) {
                        Image(
                            painter = painterResource(id = R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(34.dp).clip(CircleShape),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin) {
                            IconButton(onClick = { showQrDialog = true }) {
                                Icon(Icons.Default.QrCode, contentDescription = "QR", tint = accentColor)
                            }

                            AnimatedContent(
                                targetState = isEditing,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                        .togetherWith(fadeOut(animationSpec = tween(90)))
                                },
                                label = "TopBarActions"
                            ) { editing ->
                                Row {
                                    if (editing) {
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                val updatedMember = member.copy(
                                                    firstName = firstName,
                                                    lastName = lastName,
                                                    phone = phone,
                                                    address = address,
                                                    weight = weight,
                                                    height = height,
                                                    planId = planId,
                                                    planType = planType,
                                                    nextRenewalDate = nextRenewalDate,
                                                    routine = routine,
                                                    assignedTrainer = assignedTrainer,
                                                    notes = notes
                                                )
                                                dataStoreManager.updateMember(updatedMember)
                                                
                                                attendanceRepository?.syncMemberProfile(
                                                    updatedMember.firstName, updatedMember.lastName, updatedMember.email,
                                                    updatedMember.phone, updatedMember.address, updatedMember.weight, updatedMember.height,
                                                    updatedMember.photoUri, updatedMember.membershipStatus, updatedMember.nextRenewalDate,
                                                    updatedMember.planId, updatedMember.planType, updatedMember.assignedTrainer
                                                )

                                                Toast.makeText(context, t["changes_saved"] ?: "Guardado", Toast.LENGTH_SHORT).show()
                                                isEditing = false
                                            }
                                        }) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.checked),
                                                contentDescription = t["save"],
                                                tint = accentColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = { showDeleteDialog = true }) {
                                            Icon(Icons.Default.Delete, contentDescription = t["delete"], tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                        IconButton(onClick = { isEditing = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                    onChatClick = onChatClick,
                    accentColor = accentColor,
                    unreadCount = unreadCount
                )
            } else {
                AdminBottomNavigation(
                    currentRoute = "members",
                    onHomeClick = onHomeClick,
                    onStoreClick = onStoreClick,
                    onMembersClick = onMembersClick,
                    onProfessorsClick = onProfessorsClick,
                    onSettingsClick = onSettingsClick,
                    accentColor = accentColor
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 100.dp)
        ) {
            // Header: Foto y Nombre
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(2.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (member.photoUri != null) {
                            AsyncImage(
                                model = member.photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = TextGray)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    AnimatedContent(
                        targetState = isEditing,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "HeaderNames"
                    ) { editing ->
                        if (editing) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CustomTextFieldSmall(value = firstName, onValueChange = { firstName = it }, label = t["names"] ?: "", accentColor = accentColor)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    CustomTextFieldSmall(value = lastName, onValueChange = { lastName = it }, label = t["last_name"] ?: "", accentColor = accentColor)
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(member.fullName, color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(member.email, color = TextGray, fontSize = 14.sp)
                }
            }

            // Información Personal
            item {
                DetailSection(title = t["personal"] ?: "", icon = Icons.Default.Badge, accentColor = accentColor, shape = containerShape) {
                    AnimatedContent(
                        targetState = isEditing,
                        transitionSpec = {
                            fadeIn().togetherWith(fadeOut())
                        },
                        modifier = Modifier.animateContentSize(),
                        label = "PersonalInfoTransition"
                    ) { editing ->
                        if (editing) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CustomTextFieldSmall(value = phone, onValueChange = { phone = it }, label = t["phone"] ?: "", accentColor = accentColor)
                                CustomTextFieldSmall(value = address, onValueChange = { address = it }, label = t["address"] ?: "", accentColor = accentColor)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CustomTextFieldSmall(value = weight, onValueChange = { weight = it }, label = t["weight"] ?: "", accentColor = accentColor)
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        CustomTextFieldSmall(value = height, onValueChange = { height = it }, label = t["height"] ?: "", accentColor = accentColor)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow(icon = Icons.Default.Phone, label = "WhatsApp/Tel:", value = member.phone)
                                InfoRow(icon = Icons.Default.LocationOn, label = "Dirección:", value = member.address.ifEmpty { "No especificada" })
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        InfoRow(icon = Icons.Default.MonitorWeight, label = "Peso:", value = if(member.weight.isNotEmpty()) "${member.weight} kg" else "N/A")
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        InfoRow(icon = Icons.Default.Height, label = "Altura:", value = if(member.height.isNotEmpty()) "${member.height} cm" else "N/A")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Estado de Membresía
            item {
                DetailSection(title = t["membership"] ?: "", icon = Icons.Default.CardMembership, accentColor = accentColor, shape = containerShape) {
                    var showDatePicker by remember { mutableStateOf(false) }
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextRenewalDate)
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { nextRenewalDate = it }
                                    showDatePicker = false
                                }) { Text("OK", color = accentColor) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text(t["cancel"] ?: "Cancelar", color = TextWhite) }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    AnimatedContent(
                        targetState = isEditing,
                        modifier = Modifier.animateContentSize(),
                        label = "MembershipTransition"
                    ) { editing ->
                        if (editing) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = expandedPlans,
                                    onExpandedChange = { expandedPlans = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = planType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(t["plan"] ?: "", color = TextGray, fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlans) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF00222E),
                                            unfocusedContainerColor = Color(0xFF00222E),
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedPlans,
                                        onDismissRequest = { expandedPlans = false },
                                        modifier = Modifier.background(Color(0xFF00222E)).border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    ) {
                                        plans.forEach { plan ->
                                            DropdownMenuItem(
                                                text = { Text(plan.name, color = TextWhite) },
                                                onClick = {
                                                    planId = plan.id
                                                    planType = plan.name
                                                    // Actualización automática al cambiar plan en edición
                                                    val duration = plan.durationDays.toLong()
                                                    nextRenewalDate = System.currentTimeMillis() + (duration * 24 * 60 * 60 * 1000)
                                                    expandedPlans = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = sdf.format(Date(nextRenewalDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(t["renewal"] ?: "", color = TextGray, fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                    enabled = false, // Para que el click lo maneje el Box o Modifier si es necesario, o usar interactionSource
                                    trailingIcon = { 
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentColor)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledContainerColor = Color(0xFF00222E),
                                        disabledBorderColor = Color.White.copy(alpha = 0.1f),
                                        disabledTextColor = TextWhite,
                                        disabledLabelColor = TextGray
                                    )
                                )
                            }
                        } else {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val isExpired = member.nextRenewalDate < System.currentTimeMillis()
                            
                            val statusText = if (member.membershipStatus == "DEUDOR") "DEUDOR" 
                                            else if (isExpired) "VENCIDO" 
                                            else "ACTIVO"
                            val statusColor = if (statusText == "ACTIVO") Color(0xFF4CAF50) else Color(0xFFF44336)
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                    Spacer(Modifier.width(8.dp))
                                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    if (isAdmin) {
                                        Button(
                                            onClick = { 
                                                if (memberPlan == null) {
                                                    showNoPlanWarning = true
                                                } else {
                                                    // Verificamos si ya hay un pago registrado recientemente (últimos 25 días)
                                                    val hasRecentPayment = paymentRecords.any { 
                                                        it.memberEmail == member.email && 
                                                        (System.currentTimeMillis() - it.timestamp) < (25L * 24 * 60 * 60 * 1000) 
                                                    }
                                                    if (hasRecentPayment) {
                                                        showAlreadyPaidDialog = true
                                                    } else {
                                                        showPaymentDialog = true 
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(t["register_pay"] ?: "Pay", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text("${t["renewal"]}: ${sdf.format(Date(member.nextRenewalDate))}", color = TextWhite, fontSize = 14.sp)
                                Text("${t["plan"]}: ${memberPlan?.name ?: member.planType}", color = TextGray, fontSize = 13.sp)
                                if (memberPlan != null) {
                                    Text("Precio: ${memberPlan.price}", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Historial de Asistencia (Solo lectura)
            if (!isEditing) {
                item {
                    DetailSection(title = t["attendance"] ?: "", icon = Icons.Default.History, accentColor = accentColor, shape = containerShape) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${t["visits"]}: ${member.attendanceCount}", color = TextWhite)
                            val lastVisitText = member.lastVisit?.let { 
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                            } ?: t["no_records"]
                            Text("${t["last_visit"]}: $lastVisitText", color = TextGray, fontSize = 13.sp)
                            
                            LinearProgressIndicator(
                                progress = { (member.attendanceCount % 20) / 20f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = accentColor,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }

            // Rutina y Progreso
            item {
                DetailSection(title = t["routine"] ?: "", icon = Icons.Default.FitnessCenter, accentColor = accentColor, shape = containerShape) {
                    AnimatedContent(
                        targetState = isEditing,
                        modifier = Modifier.animateContentSize(),
                        label = "RoutineTransition"
                    ) { editing ->
                        if (editing) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CustomTextFieldSmall(value = routine, onValueChange = { routine = it }, label = "Rutina", accentColor = accentColor)
                                
                                ExposedDropdownMenuBox(
                                    expanded = expandedProf,
                                    onExpandedChange = { expandedProf = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = assignedTrainer,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(t["trainer"] ?: "", color = TextGray, fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProf) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF00222E),
                                            unfocusedContainerColor = Color(0xFF00222E),
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedProf,
                                        onDismissRequest = { expandedProf = false },
                                        modifier = Modifier.background(Color(0xFF00222E)).border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(t["no_prof"] ?: "", color = TextWhite) },
                                            onClick = {
                                                assignedTrainer = ""
                                                expandedProf = false
                                            }
                                        )
                                        professors.forEach { prof ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (prof.photoUri != null) {
                                                            AsyncImage(
                                                                model = prof.photoUri,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(24.dp).clip(CircleShape),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Icon(
                                                                painter = painterResource(R.drawable.assistant),
                                                                contentDescription = null,
                                                                tint = accentColor,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(prof.fullName, color = TextWhite)
                                                    }
                                                },
                                                onClick = {
                                                    assignedTrainer = prof.fullName
                                                    expandedProf = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Rutina: ${member.routine.ifEmpty { "No asignada" }}", color = TextWhite)
                                Text("${t["trainer"]}: ${member.assignedTrainer.ifEmpty { "No asignado" }}", color = TextGray, fontSize = 13.sp)
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = containerShape
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Progreso", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(member.progress.ifEmpty { "Sin actualizaciones de progreso" }, color = TextGray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notas Internas
            item {
                DetailSection(title = t["notes"] ?: "", icon = Icons.AutoMirrored.Filled.Notes, accentColor = accentColor, shape = containerShape) {
                    AnimatedContent(
                        targetState = isEditing,
                        modifier = Modifier.animateContentSize(),
                        label = "NotesTransition"
                    ) { editing ->
                        if (editing) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF00222E),
                                    unfocusedContainerColor = Color(0xFF00222E),
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )
                        } else {
                            Text(
                                member.notes.ifEmpty { t["no_notes"] ?: "" },
                                color = TextWhite,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(t["delete_confirm"] ?: "", color = TextWhite) },
            text = { Text(t["delete_msg"] ?: "", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        dataStoreManager.removeMember(member.email, member.fullName)
                        val repo = attendanceRepository ?: com.zexo.gymcopilot.repository.AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
                        repo.deleteMember(email = member.email, firstName = member.firstName, lastName = member.lastName)
                        showDeleteDialog = false
                        Toast.makeText(context, if (appLanguage == "Español") "Alumno eliminado" else "Student deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }) {
                    Text(t["delete"] ?: "", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(t["cancel"] ?: "", color = TextWhite)
                }
            }
        )
    }

    if (showPaymentDialog && memberPlan != null) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text(t["pay_confirm"] ?: "Payment", color = TextWhite) },
            text = { 
                Column {
                    Text(t["pay_msg"] ?: "", color = TextGray)
                    Spacer(Modifier.height(8.dp))
                    Text("Plan: ${memberPlan.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("Monto: ${memberPlan.price}", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val now = System.currentTimeMillis()
                            // Si el plan ya venció, empezamos desde hoy. Si no, extendemos la fecha actual.
                            val baseDate = if (member.nextRenewalDate > now) member.nextRenewalDate else now
                            val duration = memberPlan.durationDays.toLong()
                            val newRenewalDate = baseDate + (duration * 24 * 60 * 60 * 1000)
                            
                            val updatedMember = member.copy(
                                planId = planId,
                                planType = planType,
                                nextRenewalDate = newRenewalDate,
                                membershipStatus = "ACTIVO"
                            )
                            dataStoreManager.updateMember(updatedMember)

                            // Sincronización con el servidor
                            attendanceRepository?.syncMemberProfile(
                                updatedMember.firstName, updatedMember.lastName, updatedMember.email,
                                updatedMember.phone, updatedMember.address, updatedMember.weight, updatedMember.height,
                                updatedMember.photoUri, updatedMember.membershipStatus, updatedMember.nextRenewalDate,
                                updatedMember.planId, updatedMember.planType
                            )

                            val payment = PaymentRecord(
                                memberEmail = member.email,
                                planId = memberPlan.id,
                                planName = memberPlan.name,
                                amount = memberPlan.price,
                                timestamp = now
                            )
                            dataStoreManager.addPaymentRecord(payment)

                            showPaymentDialog = false
                            val successMsg = if(appLanguage == "Español") "Pago registrado: +$duration días" else "Payment registered: +$duration days"
                            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    val contentColor = if (accentColor.luminance() > 0.5f) Color.Black else Color.White
                    Text("REGISTRAR", color = contentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text(t["cancel"] ?: "", color = TextWhite)
                }
            }
        )
    }

    if (showAlreadyPaidDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyPaidDialog = false },
            confirmButton = {
                Button(
                    onClick = { showAlreadyPaidDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Pago Detectado", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("El pago para este $memberDesignation ya estaba registrado.", color = TextGray) },
            containerColor = Color(0xFF081C24)
        )
    }

    if (showNoPlanWarning) {
        AlertDialog(
            onDismissRequest = { showNoPlanWarning = false },
            confirmButton = {
                Button(
                    onClick = { showNoPlanWarning = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Plan no seleccionado", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Debes seleccionar un plan en el perfil para registrar un pago.", color = TextGray) },
            containerColor = Color(0xFF081C24)
        )
    }

    if (showQrDialog) {
        val qrContent = "MEMBERLINK|$firstName|$lastName|${member.email}|$gymApiUrl|$gymName"
        val qrBitmap = remember(qrContent) { generateQrCodeUnified(qrContent, 512) }

        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = containerShape,
                color = Color(0xFF00151C),
                modifier = Modifier.padding(16.dp).border(1.dp, accentColor.copy(alpha = 0.5f), containerShape)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t["qr_title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(250.dp).background(Color.White).padding(8.dp)
                        )
                    } else {
                        Text("Error al generar QR", color = Color.Red)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        t["qr_desc"] ?: "",
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = containerShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val contentColor = if (accentColor.luminance() > 0.5f) Color.Black else Color.White
                        Text(t["close"] ?: "", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextGray, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DetailSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    shape: Shape,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.05f), shape)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun CustomTextFieldSmall(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accentColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextGray, fontSize = 11.sp) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF00222E),
            unfocusedContainerColor = Color(0xFF00222E),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        )
    )
}

private fun generateQrCodeUnified(content: String, size: Int): Bitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
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
