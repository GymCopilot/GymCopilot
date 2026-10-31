package com.zexo.gymcopilot.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    memberEmail: String? = null,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val plans by dataStoreManager.getSubscriptionPlans().collectAsState(initial = emptyList())
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    
    var planId by remember { mutableStateOf<String?>(null) }
    var planType by remember { mutableStateOf(if (appLanguage == "Español") "Mensual" else "Monthly") }
    var nextRenewalDate by remember { mutableLongStateOf(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)) }
    var notes by remember { mutableStateOf("") }
    var routine by remember { mutableStateOf("") }
    var assignedTrainer by remember { mutableStateOf("") }

    val isEditMode = memberEmail != null
    var expandedProf by remember { mutableStateOf(false) }
    var expandedPlans by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    val t = remember(appLanguage, memberDesignation) {
        if (appLanguage == "Español") {
            mapOf(
                "edit" to "Editar",
                "new" to "Nuevo",
                "personal_data" to "Datos Personales",
                "names" to "Nombres",
                "last_name" to "Apellido",
                "email" to "Correo Electrónico",
                "phone" to "Teléfono / WhatsApp",
                "address" to "Dirección",
                "weight" to "Peso (kg)",
                "height" to "Altura (cm)",
                "membership" to "Membresía y Entrenamiento",
                "plan" to "Plan de Suscripción",
                "routine" to "Rutina Asignada",
                "prof" to "Profesor Asignado",
                "no_prof" to "Sin asignar",
                "notes" to "Notas Internas",
                "notes_placeholder" to "Añade notas sobre el $memberDesignation...",
                "req_fields" to "Nombre y Email son obligatorios",
                "saved" to "$memberDesignation guardado",
                "changes_saved" to "Cambios guardados",
                "save" to "Guardar",
                "qr_title" to "Código QR del $memberDesignation",
                "qr_desc" to "Pídele al $memberDesignation que escanee este código para completar sus datos.",
                "close" to "Cerrar"
            )
        } else {
            mapOf(
                "edit" to "Edit",
                "new" to "New",
                "personal_data" to "Personal Data",
                "names" to "First Name",
                "last_name" to "Last Name",
                "email" to "Email Address",
                "phone" to "Phone / WhatsApp",
                "address" to "Address",
                "weight" to "Weight (kg)",
                "height" to "Height (cm)",
                "membership" to "Membership & Training",
                "plan" to "Subscription Plan",
                "routine" to "Assigned Routine",
                "prof" to "Assigned Professor",
                "no_prof" to "Unassigned",
                "notes" to "Internal Notes",
                "notes_placeholder" to "Add notes about the $memberDesignation...",
                "req_fields" to "Name and Email are required",
                "saved" to "$memberDesignation saved",
                "changes_saved" to "Changes saved",
                "save" to "Save",
                "qr_title" to "$memberDesignation QR Code",
                "qr_desc" to "Ask the $memberDesignation to scan this code to complete their data.",
                "close" to "Close"
            )
        }
    }

    LaunchedEffect(memberEmail) {
        if (isEditMode) {
            val members = dataStoreManager.getMembers().first()
            val m = members.find { it.email == memberEmail }
            if (m != null) {
                firstName = m.firstName
                lastName = m.lastName
                email = m.email
                phone = m.phone
                address = m.address
                weight = m.weight
                height = m.height
                planId = m.planId
                planType = m.planType
                nextRenewalDate = m.nextRenewalDate
                notes = m.notes
                routine = m.routine
                assignedTrainer = m.assignedTrainer
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    val prefix = if (isEditMode) t["edit"] else t["new"]
                    Text("$prefix $memberDesignation", color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) 
                },
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
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (firstName.isBlank() || email.isBlank()) {
                                Toast.makeText(context, t["req_fields"] ?: "", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            showQrDialog = true
                        }
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Generar QR", tint = accentColor)
                    }
                    IconButton(
                        onClick = {
                            if (firstName.isBlank() || email.isBlank()) {
                                Toast.makeText(context, t["req_fields"] ?: "", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            coroutineScope.launch {
                                val currentMembers = dataStoreManager.getMembers().first()
                                val existingMember = if (isEditMode) currentMembers.find { it.email == memberEmail } else null
                                
                                val memberData = Member(
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    phone = phone,
                                    address = address,
                                    weight = weight,
                                    height = height,
                                    planId = planId,
                                    planType = planType,
                                    notes = notes,
                                    routine = routine,
                                    assignedTrainer = assignedTrainer,
                                    registrationDate = existingMember?.registrationDate ?: System.currentTimeMillis(),
                                    membershipStatus = existingMember?.membershipStatus ?: "DEUDOR",
                                    nextRenewalDate = nextRenewalDate,
                                    attendanceCount = existingMember?.attendanceCount ?: 0,
                                    lastVisit = existingMember?.lastVisit,
                                    photoUri = existingMember?.photoUri
                                )
                                if (isEditMode) {
                                    dataStoreManager.updateMember(memberData)
                                } else {
                                    dataStoreManager.addMember(memberData)
                                }

                                // Sincronización inmediata con la planilla de Google
                                attendanceRepository?.syncMemberProfile(
                                    memberData.firstName, memberData.lastName, memberData.email,
                                    memberData.phone, memberData.address, memberData.weight, memberData.height,
                                    memberData.photoUri, memberData.membershipStatus, memberData.nextRenewalDate,
                                    memberData.planId, memberData.planType, memberData.assignedTrainer
                                )

                                // Sync routine if current user is professor
                                val role = dataStoreManager.getUserRole().first()
                                if (role == "professor") {
                                    val linkedProfId = dataStoreManager.getLinkedProfessorId().first()
                                    if (linkedProfId != null) {
                                        attendanceRepository?.syncMemberRoutine(linkedProfId, email, routine)
                                    }
                                }

                                Toast.makeText(context, if (isEditMode) t["changes_saved"] ?: "" else t["saved"] ?: "", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.checked), contentDescription = t["save"], tint = accentColor, modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        bottomBar = {
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(0.dp, 20.dp, 0.dp, 100.dp)
        ) {
            item {
                Text(t["personal_data"] ?: "", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = firstName, onValueChange = { firstName = it }, label = t["names"] ?: "", accentColor = accentColor, shape = containerShape)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = lastName, onValueChange = { lastName = it }, label = t["last_name"] ?: "", accentColor = accentColor, shape = containerShape)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                CustomTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = t["email"] ?: "",
                    accentColor = accentColor,
                    shape = containerShape,
                    enabled = !isEditMode
                )
                
                Spacer(Modifier.height(12.dp))
                
                CustomTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = t["phone"] ?: "",
                    accentColor = accentColor,
                    shape = containerShape
                )

                Spacer(Modifier.height(12.dp))
                
                CustomTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = t["address"] ?: "",
                    accentColor = accentColor,
                    shape = containerShape
                )

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = weight, onValueChange = { weight = it }, label = t["weight"] ?: "", accentColor = accentColor, shape = containerShape)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = height, onValueChange = { height = it }, label = t["height"] ?: "", accentColor = accentColor, shape = containerShape)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(t["membership"] ?: "", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                
                // Selector de Plan
                var showDatePicker by remember { mutableStateOf(false) }
                val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }

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
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlans) },
                        shape = containerShape,
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
                        modifier = Modifier.background(Color(0xFF00222E)).border(1.dp, accentColor.copy(alpha = 0.5f), containerShape)
                    ) {
                        plans.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text(plan.name, color = TextWhite) },
                                onClick = {
                                    planId = plan.id
                                    planType = plan.name
                                    // Actualizar fecha automáticamente
                                    val duration = plan.durationDays.toLong()
                                    nextRenewalDate = System.currentTimeMillis() + (duration * 24 * 60 * 60 * 1000)
                                    expandedPlans = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = sdf.format(java.util.Date(nextRenewalDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if(appLanguage == "Español") "Vencimiento" else "Expiration", color = TextGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    enabled = false,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentColor)
                        }
                    },
                    shape = containerShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Color(0xFF00222E),
                        disabledBorderColor = Color.White.copy(alpha = 0.1f),
                        disabledTextColor = TextWhite,
                        disabledLabelColor = TextGray
                    )
                )

                Spacer(Modifier.height(12.dp))
                
                CustomTextField(
                    value = routine,
                    onValueChange = { routine = it },
                    label = t["routine"] ?: "",
                    accentColor = accentColor,
                    shape = containerShape
                )

                Spacer(Modifier.height(12.dp))

                // Selector de Profesor
                ExposedDropdownMenuBox(
                    expanded = expandedProf,
                    onExpandedChange = { expandedProf = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = assignedTrainer,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(t["prof"] ?: "", color = TextGray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProf) },
                        shape = containerShape,
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
                        modifier = Modifier.background(Color(0xFF00222E)).border(1.dp, accentColor.copy(alpha = 0.5f), containerShape)
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
                                text = { Text(prof.fullName, color = TextWhite) },
                                onClick = {
                                    assignedTrainer = prof.fullName
                                    expandedProf = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(t["notes"] ?: "", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(t["notes_placeholder"] ?: "", color = TextGray, fontSize = 14.sp) },
                    shape = containerShape,
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
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showQrDialog) {
        val qrContent = "MEMBERLINK|$firstName|$lastName|$email|$gymApiUrl|$gymName"
        val qrBitmap = remember(qrContent) { generateQrCode(qrContent, 512) }

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
                        colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                        shape = containerShape,
                        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor, containerShape)
                    ) {
                        val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                        Text(t["close"] ?: "", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun generateQrCode(content: String, size: Int): Bitmap? {
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

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextGray, fontSize = 12.sp) },
        shape = shape,
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF00222E),
            unfocusedContainerColor = Color(0xFF00222E),
            disabledContainerColor = Color(0xFF00222E),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            disabledBorderColor = Color.White.copy(alpha = 0.05f),
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            disabledTextColor = TextGray
        )
    )
}
