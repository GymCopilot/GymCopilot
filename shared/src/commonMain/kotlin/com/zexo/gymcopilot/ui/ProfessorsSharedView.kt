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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.*

val ProfessorColorsSharedPalette = listOf(
    Color(0xFF8A2BE2), // Purple
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF00C853), // Green
    Color(0xFFFF4081), // Pink
    Color(0xFF2979FF), // Blue
    Color(0xFFFFD600), // Gold
    Color(0xFFC2185B)  // Magenta
)

@Composable
fun ManageProfessorsSharedScreen(
    onBack: () -> Unit,
    professors: List<Professor>,
    onAddProfessorClick: () -> Unit,
    onEditProfessor: (String) -> Unit,
    onDeleteProfessor: (String) -> Unit,
    onChatClick: (String) -> Unit
) {
    var professorToDelete by remember { mutableStateOf<Professor?>(null) }
    var showQrForProfessor by remember { mutableStateOf<Professor?>(null) }

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
                        text = "Gestionar Profesores",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
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
                // TARJETA SUPERIOR "AGREGAR NUEVO PROFESOR"
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddProfessorClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PrimaryTurquoise.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(text = "Agregar Nuevo Profesor", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "Registrar un docente al equipo", color = TextGray, fontSize = 12.sp)
                                }
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = PrimaryTurquoise)
                        }
                    }
                }

                if (professors.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay profesores registrados", color = TextGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(professors) { prof ->
                        val identifier = if (prof.email.isNotBlank()) prof.email else prof.fullName
                        ProfessorManagementSharedCard(
                            professor = prof,
                            onEdit = { onEditProfessor(prof.id) },
                            onDelete = { professorToDelete = prof },
                            onChat = { onChatClick(identifier) },
                            onQrClick = { showQrForProfessor = prof }
                        )
                    }
                }
            }
        }
    }

    if (professorToDelete != null) {
        AlertDialog(
            onDismissRequest = { professorToDelete = null },
            title = { Text("¿Eliminar profesor?", color = TextWhite) },
            text = { Text("Esta acción no se puede deshacer.", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = {
                    professorToDelete?.let { prof ->
                        onDeleteProfessor(prof.id)
                        professorToDelete = null
                    }
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { professorToDelete = null }) {
                    Text("Cancelar", color = TextWhite)
                }
            }
        )
    }

    if (showQrForProfessor != null) {
        AlertDialog(
            onDismissRequest = { showQrForProfessor = null },
            title = { Text("VINCULAR PROFESOR", color = TextWhite, fontWeight = FontWeight.Black) },
            text = { Text("Escanea este código con el dispositivo del profesor para vincularlo.", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(
                    onClick = { showQrForProfessor = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProfessorManagementSharedCard(
    professor: Professor,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit,
    onQrClick: () -> Unit
) {
    val profColor = professor.profileColor?.let { Color(it) } ?: PrimaryTurquoise

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, profColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = profColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, profColor),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = profColor, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = professor.fullName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onQrClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "QR", tint = PrimaryTurquoise, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onChat, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Email, contentDescription = "Chat", tint = PrimaryTurquoise, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryTurquoise, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun AddProfessorSharedScreen(
    professor: Professor? = null,
    onBack: () -> Unit,
    onSave: (Professor) -> Unit
) {
    var firstName by remember { mutableStateOf(professor?.firstName ?: "") }
    var lastName by remember { mutableStateOf(professor?.lastName ?: "") }
    var email by remember { mutableStateOf(professor?.email ?: "") }
    var phone by remember { mutableStateOf(professor?.phone ?: "") }
    var address by remember { mutableStateOf(professor?.address ?: "") }
    var specialty by remember { mutableStateOf(professor?.specialty ?: "") }
    var idNumber by remember { mutableStateOf(professor?.idNumber ?: "") }
    var taxId by remember { mutableStateOf(professor?.taxId ?: "") }
    var emergencyContact by remember { mutableStateOf(professor?.emergencyContact ?: "") }
    var bankAccount by remember { mutableStateOf(professor?.bankAccount ?: "") }
    var selectedColor by remember { mutableStateOf(professor?.profileColor?.let { Color(it) } ?: ProfessorColorsSharedPalette[0]) }

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
                            text = if (professor == null) "Nuevo Profesor" else "Editar Profesor",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    IconButton(onClick = {
                        if (firstName.isNotBlank() && lastName.isNotBlank()) {
                            val prof = Professor(
                                id = professor?.id ?: "prof_${(1000..9999).random()}",
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone,
                                address = address,
                                specialty = specialty,
                                idNumber = idNumber,
                                taxId = taxId,
                                emergencyContact = emergencyContact,
                                bankAccount = bankAccount,
                                profileColor = selectedColor.toArgb()
                            )
                            onSave(prof)
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = PrimaryTurquoise, modifier = Modifier.size(28.dp))
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
                // CÍRCULO DE FOTO Y PALETA DE COLORES
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(2.dp, selectedColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = selectedColor)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Toca para subir foto", color = selectedColor, fontSize = 12.sp)
                    }
                }

                item {
                    Text("Color de Perfil", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ProfessorColorsSharedPalette) { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }

                // DATOS PERSONALES
                item {
                    Text("Datos Personales", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    CustomSharedTextField(value = email, onValueChange = { email = it }, label = "Email")

                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono")
                }

                // DATOS LABORALES
                item {
                    Text("Datos Laborales", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    CustomSharedTextField(value = specialty, onValueChange = { specialty = it }, label = "Especialidad")
                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = address, onValueChange = { address = it }, label = "Dirección Residencial")
                }

                // DOCUMENTACIÓN
                item {
                    Text("Documentación", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSharedTextField(value = idNumber, onValueChange = { idNumber = it }, label = "DNI")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSharedTextField(value = taxId, onValueChange = { taxId = it }, label = "CUIT / CUIL")
                        }
                    }
                }

                // INFORMACIÓN ADICIONAL
                item {
                    Text("Información Adicional", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    CustomSharedTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = "Contacto de Emergencia")
                    Spacer(Modifier.height(12.dp))
                    CustomSharedTextField(value = bankAccount, onValueChange = { bankAccount = it }, label = "CBU / Alias Bancario")
                }
            }
        }
    }
}
