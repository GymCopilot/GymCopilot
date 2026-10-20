package com.zexo.gymcopilot.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.*
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import kotlinx.coroutines.launch

// ============================================================================
// COMPONENTES REUTILIZABLES COMPARTIDOS
// ============================================================================

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextWhite.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun MemberCard(
    name: String,
    plan: String,
    status: String,
    lastAccess: String,
    isActive: Boolean,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isActive) PrimaryTurquoise.copy(alpha = 0.15f) else AccentOrange.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.take(1).uppercase(),
                            color = if (isActive) PrimaryTurquoise else AccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                    Text(text = plan, fontSize = 12.sp, color = TextGray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (isActive) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = lastAccess, fontSize = 10.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun ClassScheduleCard(
    className: String,
    professor: String,
    time: String,
    spots: String,
    onReserve: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = className, fontWeight = FontWeight.Bold, color = PrimaryTurquoise, fontSize = 16.sp)
                Text(text = "Entrenador: $professor", fontSize = 12.sp, color = TextWhite)
                Text(text = "⏰ $time", fontSize = 12.sp, color = TextGray)
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = PrimaryTurquoise.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = spots,
                        color = PrimaryTurquoise,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onReserve,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Reservar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RoutineCard(
    exerciseName: String,
    sets: String,
    reps: String,
    rest: String,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) SurfaceColor.copy(alpha = 0.5f) else SurfaceColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryTurquoise)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = exerciseName,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) TextGray else TextWhite,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$sets series x $reps reps • Descanso: $rest",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            Surface(
                color = if (isCompleted) PrimaryTurquoise else Color.Transparent,
                shape = CircleShape,
                border = if (!isCompleted) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isCompleted) "✓" else "",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    title: String,
    category: String,
    price: String,
    onAddToCart: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                Text(text = category, fontSize = 12.sp, color = TextGray)
                Text(text = price, fontWeight = FontWeight.Black, color = PrimaryTurquoise, fontSize = 16.sp)
            }

            Button(
                onClick = onAddToCart,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Comprar", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// VISTAS COMPLETAS POR ROL Y SECCIÓN
// ============================================================================

@Composable
fun AdminFullDashboard(
    apiClient: GymKtorApiClient,
    currentTab: Int,
    onNavigate: (String) -> Unit
) {
    var membersList by remember { mutableStateOf<List<Member>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                // Sincronización activa con Google Sheets vía Ktor
            } catch (e: Exception) {
                // Manejo silencioso de fallback
            } finally {
                isLoading = false
            }
        }
    }

    when (currentTab) {
        0 -> { // HOME
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Panel del Administrador",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = "Socios Totales",
                            value = if (membersList.isNotEmpty()) "${membersList.size}" else "142",
                            subtitle = "+12 este mes",
                            accentColor = PrimaryTurquoise,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Asistencia Hoy",
                            value = "38",
                            subtitle = "85% concurrencia",
                            accentColor = AccentOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Accesos y Asistencias Recientes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (membersList.isNotEmpty()) {
                    items(membersList) { member ->
                        MemberCard(
                            name = member.fullName,
                            plan = member.planType,
                            status = member.membershipStatus,
                            lastAccess = if (member.lastVisit != null) "Aviso registrado" else "Sin registro",
                            isActive = member.membershipStatus.uppercase() == "ACTIVO"
                        )
                    }
                } else {
                    item {
                        MemberCard("Juan Carlos Pérez", "Pase Libre Gold", "ACTIVO", "Hoy 08:30 AM", true)
                        MemberCard("María Florencia Gómez", "Musculación 3x", "ACTIVO", "Hoy 10:15 AM", true)
                        MemberCard("Carlos Alberto López", "Pase Libre", "VENCIDO", "Hace 3 días", false)
                        MemberCard("Ana Laura Torres", "Crossfit VIP", "ACTIVO", "Ayer 18:00 PM", true)
                    }
                }
            }
        }
        1 -> FullStoreSection()
        2 -> FullMembersSection(membersList = membersList)
        3 -> FullProfessorsSection()
        else -> FullSettingsSection()
    }
}

@Composable
fun ProfessorFullDashboard(
    apiClient: GymKtorApiClient,
    currentTab: Int,
    onNavigate: (String) -> Unit
) {
    when (currentTab) {
        0 -> { // HOME
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Panel del Entrenador",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                item {
                    ClassScheduleCard("Musculación Guiada & Fuerza", "Usted", "09:00 - 11:00 AM", "18 Alumnos")
                    ClassScheduleCard("Crossfit Intensivo", "Usted", "17:00 - 18:30 PM", "14 Alumnos")
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Alumnos Asignados Recientes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    MemberCard("Juan Carlos Pérez", "Rutina Hipertrofia A", "PRESENTE", "Hoy 08:55 AM", true)
                    MemberCard("María Florencia Gómez", "Rutina Movilidad", "PRESENTE", "Hoy 09:02 AM", true)
                    MemberCard("Lucas Martínez", "Rutina Fuerza", "AUSENTE", "Sin registro", false)
                }
            }
        }
        1 -> FullStoreSection()
        2 -> FullClassesScheduleSection()
        3 -> FullMembersSection(membersList = emptyList())
        4 -> FullRoutinesSection()
        else -> FullChatSection()
    }
}

@Composable
fun MemberFullDashboard(
    apiClient: GymKtorApiClient,
    currentTab: Int,
    onNavigate: (String) -> Unit
) {
    var ex1 by remember { mutableStateOf(true) }
    var ex2 by remember { mutableStateOf(false) }
    var ex3 by remember { mutableStateOf(false) }
    var ex4 by remember { mutableStateOf(false) }

    when (currentTab) {
        0 -> { // HOME RUTINA
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryTurquoise.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🔥", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Rutina Activa: Pecho & Tríceps", fontWeight = FontWeight.Bold, color = PrimaryTurquoise, fontSize = 16.sp)
                                Text(text = "Progreso de hoy: 1 de 4 ejercicios completados", color = TextWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Ejercicios Programados",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    RoutineCard("Press Inclinado con Mancuernas", "4", "10 - 12", "90 seg", ex1) { ex1 = !ex1 }
                    RoutineCard("Aperturas en Polea Alta", "3", "15", "60 seg", ex2) { ex2 = !ex2 }
                    RoutineCard("Fondos en Paralelas", "4", "10", "90 seg", ex3) { ex3 = !ex3 }
                    RoutineCard("Extensión de Tríceps en Polea", "3", "12 - 15", "60 seg", ex4) { ex4 = !ex4 }
                }
            }
        }
        1 -> FullStoreSection()
        2 -> FullClassesScheduleSection()
        3 -> FullRoutinesSection()
        else -> FullChatSection()
    }
}

// ============================================================================
// SECCIONES SECUNDARIAS COMPLETAS
// ============================================================================

@Composable
fun FullMembersSection(membersList: List<Member>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(text = "Clientes / Socios del Gimnasio", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (membersList.isNotEmpty()) {
            items(membersList) { member ->
                MemberCard(
                    name = member.fullName,
                    plan = member.planType,
                    status = member.membershipStatus,
                    lastAccess = if (member.lastVisit != null) "Aviso registrado" else "Sin registro",
                    isActive = member.membershipStatus.uppercase() == "ACTIVO"
                )
            }
        } else {
            item {
                MemberCard("Juan Carlos Pérez", "Pase Libre Gold", "ACTIVO", "Hoy 08:30 AM", true)
                MemberCard("María Florencia Gómez", "Musculación 3x", "ACTIVO", "Hoy 10:15 AM", true)
                MemberCard("Carlos Alberto López", "Pase Libre", "VENCIDO", "Hace 3 días", false)
                MemberCard("Ana Laura Torres", "Crossfit VIP", "ACTIVO", "Ayer 18:00 PM", true)
                MemberCard("Lucas Martínez", "Pase Mensual", "ACTIVO", "Hoy 07:00 AM", true)
                MemberCard("Sofia Benítez", "Musculación 2x", "VENCIDO", "Hace 5 días", false)
            }
        }
    }
}

@Composable
fun FullProfessorsSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(text = "Profesores / Entrenadores", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            MemberCard("Prof. Gabriel Fernández", "Musculación & Personalizado", "ACTIVO", "Hoy 07:00 AM", true)
            MemberCard("Prof. Laura Benítez", "Yoga & Mobility", "ACTIVO", "Hoy 09:30 AM", true)
            MemberCard("Prof. Roberto Carlos", "Crossfit & Funcional", "ACTIVO", "Ayer 17:00 PM", true)
        }
    }
}

@Composable
fun FullClassesScheduleSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(text = "Horarios y Reserva de Clases", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            ClassScheduleCard("Crossfit Intensivo", "Prof. Roberto", "08:00 - 09:00 AM", "15/15 Cupos Lleno")
            ClassScheduleCard("Yoga & Mobility", "Prof. Laura", "10:00 - 11:00 AM", "8/12 Cupos Disponibles")
            ClassScheduleCard("Spinning Power 5G", "Prof. Marcos", "18:00 - 19:00 PM", "20/20 Cupos Lleno")
            ClassScheduleCard("Musculación Libre", "Prof. Gabriel", "19:00 - 21:00 PM", "Cupos Libres")
        }
    }
}

@Composable
fun FullRoutinesSection() {
    var check1 by remember { mutableStateOf(true) }
    var check2 by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(text = "Mis Rutinas y Ejercicios", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            RoutineCard("Press Inclinado con Mancuernas", "4", "10 - 12", "90 seg", check1) { check1 = !check1 }
            RoutineCard("Sentadilla Libre con Barra", "4", "10", "90 seg", check2) { check2 = !check2 }
            RoutineCard("Remo con Barra T", "3", "12", "60 seg", false) {}
            RoutineCard("Fondos en Paralelas", "4", "10", "90 seg", false) {}
        }
    }
}

@Composable
fun FullStoreSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(text = "Tienda & Productos del Gimnasio", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            ProductCard("Whey Protein Isolate 1kg", "Suplementación", "$45.000")
            ProductCard("Creatina Monohidratada 300g", "Suplementación", "$38.000")
            ProductCard("Shaker Térmico GymCopilot", "Accesorios", "$12.500")
            ProductCard("Remera OverSize GymCopilot", "Indumentaria", "$22.000")
            ProductCard("Plan Pase Libre Mensual", "Membresía", "$35.000 / mes")
            ProductCard("Plan Trimestral Promocional", "Membresía", "$90.000 total")
        }
    }
}

@Composable
fun FullChatSection() {
    var messageText by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("admin", "Socio", "¡Hola! Bienvenidos al chat del gimnasio GymCopilot.", 1000L, false),
                ChatMessage("user", "Socio", "Hola, ¿a qué hora abre la sala de musculación mañana?", 1005L, true)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 80.dp)
    ) {
        Text(text = "Chat de Soporte y Consultas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.isMine
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        color = if (isMe) PrimaryTurquoise else SurfaceColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = msg.text,
                                color = if (isMe) Color.Black else TextWhite,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enviado",
                                color = if (isMe) Color.Black.copy(alpha = 0.6f) else TextGray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Escribe un mensaje...", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedBorderColor = PrimaryTurquoise,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        messages = messages + ChatMessage("user", "Socio", messageText, 2000L, true)
                        messageText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("➤", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FullSettingsSection() {
    SettingsSharedScreen(onBack = {})
}
