package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.*
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun MembersSharedScreen(
    onBack: () -> Unit,
    members: List<Member>,
    professors: List<Professor>,
    plans: List<SubscriptionPlan>,
    onAddMemberClick: () -> Unit,
    onMemberClick: (String) -> Unit,
    onDeleteMember: (String) -> Unit,
    onChatClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedProfessor by remember { mutableStateOf<String?>(null) }

    val filteredMembers = remember(members, searchQuery, selectedFilter, selectedProfessor) {
        members.filter { m ->
            val fullName = "${m.firstName} ${m.lastName}"
            val matchesSearch = fullName.contains(searchQuery, ignoreCase = true) || m.email.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "DEUDOR" -> m.membershipStatus == "DEUDOR" || m.membershipStatus == "VENCIDO"
                "ACTIVO" -> m.membershipStatus == "ACTIVO" || m.membershipStatus == "ACTIVE"
                "DESERTOR" -> m.membershipStatus == "DESERTOR" || m.membershipStatus == "DROPOUT"
                else -> true
            }
            val matchesProf = selectedProfessor == null || m.assignedTrainer == selectedProfessor
            matchesSearch && matchesFilter && matchesProf
        }
    }

    val totalCount = members.size
    val activeCount = members.count { it.membershipStatus == "ACTIVO" || it.membershipStatus == "ACTIVE" }
    val debtorCount = members.count { it.membershipStatus == "DEUDOR" || it.membershipStatus == "VENCIDO" }
    val dropoutCount = members.count { it.membershipStatus == "DESERTOR" || it.membershipStatus == "DROPOUT" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryTurquoise,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Gestión de Alumno",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddMemberClick,
                    containerColor = PrimaryTurquoise,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Añadir Alumno",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 4 TARJETAS DE ESTADÍSTICA / FILTRO SUPERIORES CON VALORES REALES
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemberFilterStatBox("$totalCount", "TOTAL", Icons.Default.Person, PrimaryTurquoise, selectedFilter == "ALL", Modifier.weight(1f)) { selectedFilter = "ALL" }
                        MemberFilterStatBox("$activeCount", "ACTIVOS", Icons.Default.Person, Color(0xFF00C853), selectedFilter == "ACTIVO", Modifier.weight(1f)) { selectedFilter = "ACTIVO" }
                        MemberFilterStatBox("$debtorCount", "DEUDORES", Icons.Default.Warning, Color.Red, selectedFilter == "DEUDOR", Modifier.weight(1f)) { selectedFilter = "DEUDOR" }
                        MemberFilterStatBox("$dropoutCount", "DESERTORES", Icons.Default.Close, Color(0xFFFF9800), selectedFilter == "DESERTOR", Modifier.weight(1f)) { selectedFilter = "DESERTOR" }
                    }
                }

                // BARRA DE BÚSQUEDA
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar Alumnos...", color = TextGray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTurquoise) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color(0xFF001F29),
                            unfocusedContainerColor = Color(0xFF001F29),
                            focusedBorderColor = PrimaryTurquoise,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SECCIÓN PROFESORES
                item {
                    Column {
                        Text(text = "Profesores", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        if (professors.isEmpty()) {
                            Text(text = "Sin profesores registrados", color = TextGray, fontSize = 12.sp)
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(professors) { prof ->
                                    val isSelected = selectedProfessor == prof.fullName
                                    Surface(
                                        color = Color(0xFF001F29),
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
                                        modifier = Modifier.clickable { selectedProfessor = if (isSelected) null else prof.fullName }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(color = PrimaryTurquoise.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(text = prof.firstName, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // LISTA DE ALUMNOS
                if (filteredMembers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se encontraron alumnos registrados", color = TextGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(filteredMembers) { member ->
                        MemberRowItem(
                            member = member,
                            onClick = { onMemberClick(member.email) },
                            onDelete = { onDeleteMember(member.email) },
                            onChat = { onChatClick(member.email) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberFilterStatBox(
    count: String,
    label: String,
    iconVector: ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = iconVector, contentDescription = null, tint = color, modifier = Modifier.size(20.sp.value.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontWeight = FontWeight.Black, color = TextWhite, fontSize = 18.sp)
            Text(text = label, fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MemberRowItem(
    member: Member,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit
) {
    val statusColor = when (member.membershipStatus.uppercase()) {
        "ACTIVO", "ACTIVE" -> Color(0xFF00C853)
        "DEUDOR", "VENCIDO" -> Color.Red
        else -> Color(0xFFFF9800)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFF3E2723),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = member.firstName.take(1).uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = member.fullName,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = member.membershipStatus.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryTurquoise,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onChat() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Email, contentDescription = "Chat", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = PrimaryTurquoise,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Asignar Profesor", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onDelete() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberSharedScreen(
    onBack: () -> Unit,
    plans: List<SubscriptionPlan>,
    professors: List<Professor>,
    onSave: (Member) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var planType by remember { mutableStateOf(plans.firstOrNull()?.name ?: "Mensual") }
    var notes by remember { mutableStateOf("") }
    var routine by remember { mutableStateOf("") }
    var assignedTrainer by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryTurquoise,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Nuevo Alumno",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Row {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Info, contentDescription = "QR", tint = PrimaryTurquoise)
                        }
                        IconButton(onClick = {
                            if (firstName.isNotBlank() && email.isNotBlank()) {
                                val newMember = Member(
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    phone = phone,
                                    address = address,
                                    weight = weight,
                                    height = height,
                                    planType = planType,
                                    notes = notes,
                                    routine = routine,
                                    assignedTrainer = assignedTrainer,
                                    membershipStatus = "DEUDOR",
                                    registrationDate = 0L
                                )
                                onSave(newMember)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = PrimaryTurquoise, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                item {
                    Text(text = "Datos Personales", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSharedTextField(value = firstName, onValueChange = { firstName = it }, label = "Nombres")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSharedTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = email, onValueChange = { email = it }, label = "Correo Electrónico")

                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono / WhatsApp")

                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = address, onValueChange = { address = it }, label = "Dirección")

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSharedTextField(value = weight, onValueChange = { weight = it }, label = "Peso (kg)")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSharedTextField(value = height, onValueChange = { height = it }, label = "Altura (cm)")
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Membresía y Entrenamiento", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    CustomSharedTextField(value = planType, onValueChange = { planType = it }, label = "Plan de Suscripción")
                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = "19/11/2026", onValueChange = {}, label = "Vencimiento", enabled = false)
                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = routine, onValueChange = { routine = it }, label = "Rutina Asignada")
                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = assignedTrainer, onValueChange = { assignedTrainer = it }, label = "Profesor Asignado")
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Notas Internas", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Añade notas sobre el Alumno...", color = TextGray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF00222E),
                            unfocusedContainerColor = Color(0xFF00222E),
                            focusedBorderColor = PrimaryTurquoise,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MemberDetailSharedScreen(
    member: Member,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (Member) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showStudentQrDialog by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf(member.firstName) }
    var lastName by remember { mutableStateOf(member.lastName) }
    var phone by remember { mutableStateOf(member.phone) }
    var address by remember { mutableStateOf(member.address) }
    var weight by remember { mutableStateOf(member.weight) }
    var height by remember { mutableStateOf(member.height) }
    var planType by remember { mutableStateOf(member.planType) }
    var routine by remember { mutableStateOf(member.routine) }
    var assignedTrainer by remember { mutableStateOf(member.assignedTrainer) }
    var notes by remember { mutableStateOf(member.notes) }

    val defaultApiUrl = "https://script.google.com/macros/s/AKfycbx_GymCopilot_Sync/exec"
    val qrToken = "MEMBERLINK|${member.firstName}|${member.lastName}|${member.email}|$defaultApiUrl|Gym 19"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryTurquoise,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { if (isEditing) isEditing = false else onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Detalle del Alumno",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showStudentQrDialog = true }) {
                            Icon(painter = painterResource(Res.drawable.qr_code), contentDescription = "QR Alumno", tint = PrimaryTurquoise, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                        }
                        IconButton(onClick = {
                            if (isEditing) {
                                val updated = member.copy(
                                    firstName = firstName,
                                    lastName = lastName,
                                    phone = phone,
                                    address = address,
                                    weight = weight,
                                    height = height,
                                    planType = planType,
                                    routine = routine,
                                    assignedTrainer = assignedTrainer,
                                    notes = notes
                                )
                                onUpdate(updated)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        }) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Guardar" else "Editar",
                                tint = PrimaryTurquoise,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // AVATAR CIRCULAR SUPERIOR
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(2.dp, PrimaryTurquoise, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(56.dp), tint = TextGray)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (isEditing) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CustomSharedTextField(value = firstName, onValueChange = { firstName = it }, label = "Nombres")
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    CustomSharedTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                                }
                            }
                        } else {
                            Text(member.fullName, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Text(member.email, color = TextGray, fontSize = 13.sp)
                    }
                }

                // DATOS PERSONALES
                item {
                    DetailSharedSection(title = "Datos Personales", icon = Icons.Default.Person) {
                        if (isEditing) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomSharedTextField(value = phone, onValueChange = { phone = it }, label = "WhatsApp/Tel")
                                CustomSharedTextField(value = address, onValueChange = { address = it }, label = "Dirección")
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CustomSharedTextField(value = weight, onValueChange = { weight = it }, label = "Peso (kg)")
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        CustomSharedTextField(value = height, onValueChange = { height = it }, label = "Altura (cm)")
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoSharedRow(icon = Icons.Default.Phone, label = "WhatsApp/Tel:", value = member.phone.ifEmpty { "No especificado" })
                                InfoSharedRow(icon = Icons.Default.LocationOn, label = "Dirección:", value = member.address.ifEmpty { "No especificada" })
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        InfoSharedRow(icon = Icons.Default.Star, label = "Peso:", value = if (member.weight.isNotEmpty()) "${member.weight} kg" else "N/A")
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        InfoSharedRow(icon = Icons.Default.Star, label = "Altura:", value = if (member.height.isNotEmpty()) "${member.height} cm" else "N/A")
                                    }
                                }
                            }
                        }
                    }
                }

                // MEMBRESÍA
                item {
                    DetailSharedSection(title = "Membresía", icon = Icons.Default.Star) {
                        if (isEditing) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomSharedTextField(value = planType, onValueChange = { planType = it }, label = "Plan")
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                                        Spacer(Modifier.width(8.dp))
                                        Text(member.membershipStatus, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Registrar Pago", color = PrimaryTurquoise, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Próxima renovación: 19/11/2026", color = TextWhite, fontSize = 14.sp)
                                Text("Plan: ${member.planType}", color = TextGray, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // ASISTENCIA
                item {
                    DetailSharedSection(title = "Asistencia", icon = Icons.Default.DateRange) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Total visitas: ${member.attendanceCount}", color = TextWhite)
                            Text("Última visita: Sin registros", color = TextGray, fontSize = 13.sp)
                            LinearProgressIndicator(
                                progress = { 0f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = PrimaryTurquoise,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                // RUTINA Y PROGRESO
                item {
                    DetailSharedSection(title = "Rutina y Progreso", icon = Icons.Default.Star) {
                        if (isEditing) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomSharedTextField(value = routine, onValueChange = { routine = it }, label = "Rutina")
                                CustomSharedTextField(value = assignedTrainer, onValueChange = { assignedTrainer = it }, label = "Entrenador")
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Rutina: ${member.routine.ifEmpty { "No asignada" }}", color = TextWhite)
                                Text("Entrenador: ${member.assignedTrainer.ifEmpty { "No asignado" }}", color = TextGray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO CÓDIGO QR DEL ALUMNO (MEMBERLINK PROTOCOL)
    if (showStudentQrDialog) {
        AlertDialog(
            onDismissRequest = { showStudentQrDialog = false },
            title = {
                Text(
                    text = "Código QR del Alumno",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(220.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "QR Alumno",
                                tint = Color.Black,
                                modifier = Modifier.size(170.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pídele al Alumno que escanee este código para completar sus datos.",
                        color = TextWhite,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFF00151C),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = qrToken,
                            color = PrimaryTurquoise,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(
                    onClick = { showStudentQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Cerrar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }
}

@Composable
fun CustomSharedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextGray, fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF00222E),
            unfocusedContainerColor = Color(0xFF00222E),
            disabledContainerColor = Color(0xFF00222E),
            focusedBorderColor = PrimaryTurquoise,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            disabledBorderColor = Color.White.copy(alpha = 0.05f),
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            disabledTextColor = TextGray
        )
    )
}

@Composable
fun InfoSharedRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextGray, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DetailSharedSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}
