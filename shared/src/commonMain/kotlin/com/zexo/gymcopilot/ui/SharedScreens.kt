package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.zexo.gymcopilot.model.*
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.getCurrentTimeMillis
import com.zexo.gymcopilot.LockConfig
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    onNavigate: (String) -> Unit,
    onTabSelected: (Int) -> Unit = {}
) {
    when (currentTab) {
        0 -> MemberDashboardSharedScreen("Gym 19", "socio@gymcopilot.com", null, apiClient, currentTab, onTabSelected)
        1 -> RealPublicStoreView(onTabSelected = onTabSelected)
        2 -> FullClassesScheduleSection()
        3 -> FullRoutinesSection()
        4 -> ChatSharedScreen(apiClient = apiClient)
        else -> MemberDashboardSharedScreen("Gym 19", "socio@gymcopilot.com", null, apiClient, currentTab, onTabSelected)
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
    var viewMode by remember { mutableStateOf(2) } // 0: Día, 1: Semana, 2: Mes
    var selectedDateMillis by remember { mutableStateOf(getCurrentTimeMillis()) }
    var schedulesMap by remember { mutableStateOf<Map<String, List<ScheduleEntry>>>(emptyMap()) }
    var assignedTrainer by remember { mutableStateOf("") }
    var gymName by remember { mutableStateOf("Gym 47") }

    LaunchedEffect(Unit) {
        try {
            val localProfile = readLocalMemberProfile()
            val gymUrl = localProfile?.gymApiUrl.orEmpty().ifBlank { LockConfig.activeApiUrl }
            gymName = localProfile?.gymName.orEmpty().ifBlank { "Gym 47" }
            val gymIdVal = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = localProfile?.spreadsheetId.orEmpty()
            if (localProfile?.assignedTrainer != null) {
                assignedTrainer = localProfile.assignedTrainer
            }

            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val url = "$gymUrl${paramChar}type=schedules&gymId=$gymIdVal$ssParam"
            val responseText = GymKtorApiClient.getData(url)
            if (responseText.isNotBlank() && responseText.contains("success")) {
                val jsonElement = GymKtorApiClient.jsonInstance.parseToJsonElement(responseText)
                val jsonObject = jsonElement as? JsonObject
                val dataArray = jsonObject?.get("data") as? JsonArray
                if (dataArray != null) {
                    val result = mutableMapOf<String, MutableList<ScheduleEntry>>()
                    for (i in 1 until dataArray.size) {
                        val row = dataArray[i] as? JsonArray
                        if (row != null && row.size >= 5) {
                            val profId = row[0].jsonPrimitive.content.trim().lowercase()
                            val dayOrDate = row[1].jsonPrimitive.content.trim()
                            val start = row[2].jsonPrimitive.content.trim()
                            val end = row[3].jsonPrimitive.content.trim()
                            val name = row[4].jsonPrimitive.content.trim()
                            
                            val entry = ScheduleEntry(
                                startTime = start,
                                endTime = end,
                                eventName = name,
                                dayOfWeek = dayOrDate.toIntOrNull(),
                                date = if (dayOrDate.contains("-") || dayOrDate.contains("/")) dayOrDate else null
                            )
                            result.getOrPut(profId) { mutableListOf() }.add(entry)
                        }
                    }
                    schedulesMap = result
                }
            }
        } catch (_: Throwable) {}
    }

    val dayMs = 24L * 60 * 60 * 1000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Horarios de Clases", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
        Spacer(Modifier.height(12.dp))

        if (assignedTrainer.isNotBlank() && assignedTrainer != "Sin asignar") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryTurquoise.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = PrimaryTurquoise)
                    Spacer(Modifier.width(8.dp))
                    Text("Mi Profesor: $assignedTrainer", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF001F29))
                    .border(1.dp, PrimaryTurquoise.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { selectedDateMillis -= (if (viewMode == 2) 30 * dayMs else if (viewMode == 1) 7 * dayMs else dayMs) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, null, tint = PrimaryTurquoise)
                }
                Text("HOY", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { selectedDateMillis = getCurrentTimeMillis() }.padding(horizontal = 6.dp))
                IconButton(onClick = { selectedDateMillis += (if (viewMode == 2) 30 * dayMs else if (viewMode == 1) 7 * dayMs else dayMs) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, null, tint = PrimaryTurquoise)
                }
            }

            Text("Noviembre de 2026", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00151C))
                .border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Día", "Semana", "Mes")
            tabs.forEachIndexed { index, label ->
                val isSelected = viewMode == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryTurquoise else Color.Transparent)
                        .clickable { viewMode = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (viewMode) {
                0 -> {
                    Column {
                        Text("Eventos del día", color = PrimaryTurquoise, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                        val generalEntries = schedulesMap["general_gym"] ?: schedulesMap["GENERAL_GYM"] ?: emptyList()
                        if (generalEntries.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay eventos programados para hoy", color = TextGray, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(generalEntries) { entry ->
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF001F29)).border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(16.dp)) {
                                        Column {
                                            Text("${entry.startTime} - ${entry.endTime}", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Spacer(Modifier.height(4.dp))
                                            Text(entry.eventName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val daysOfWeek = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
                            daysOfWeek.forEachIndexed { idx, dayName ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (idx == 0) PrimaryTurquoise.copy(alpha = 0.3f) else Color(0xFF001F29))
                                        .border(1.dp, if (idx == 0) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dayName, color = if (idx >= 5) Color(0xFFE53935) else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Text("Patrón Semanal de Clases", color = PrimaryTurquoise, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                        val generalEntries = schedulesMap["general_gym"] ?: schedulesMap["GENERAL_GYM"] ?: emptyList()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(generalEntries) { entry ->
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF001F29)).border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(16.dp)) {
                                    Column {
                                        Text("${entry.startTime} - ${entry.endTime}", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(entry.eventName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val cells = remember { generateMonthGrid(2026, 10) }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM").forEachIndexed { idx, day ->
                                Text(
                                    day,
                                    color = if (idx >= 5) Color(0xFFE57373) else TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cells) { cell ->
                                val weekendRed = Color(0xFFE57373)
                                val isSelected = cell.monthOffset == 0 && cell.dayNum == 10
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.85f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF001F29))
                                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .clickable { if (cell.monthOffset == 0) viewMode = 0 },
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    if (cell.isWeekend) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val step = 10.dp.toPx()
                                            for (i in -size.height.toInt()..size.width.toInt() step step.toInt()) {
                                                drawLine(
                                                    color = weekendRed.copy(alpha = 0.25f),
                                                    start = Offset(i.toFloat(), 0f),
                                                    end = Offset(i + size.height, size.height),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text(
                                            text = cell.dayNum.toString(),
                                            color = when {
                                                isSelected -> PrimaryTurquoise
                                                cell.monthOffset != 0 -> TextGray.copy(alpha = 0.3f)
                                                cell.isWeekend -> weekendRed
                                                else -> TextWhite
                                            },
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarCell(
    val dayNum: Int,
    val monthOffset: Int,
    val isWeekend: Boolean
)

private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        0, 2, 4, 6, 7, 9, 11 -> 31
        3, 5, 8, 10 -> 30
        1 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        else -> 31
    }
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    var y = year
    var m = month + 1
    if (m < 3) {
        m += 12
        y -= 1
    }
    val q = 1
    val K = y % 100
    val J = y / 100
    val h = (q + (13 * (m + 1)) / 5 + K + (K / 4) + (J / 4) + (5 * J)) % 7
    return when (h) {
        0 -> 5
        1 -> 6
        2 -> 0
        3 -> 1
        4 -> 2
        5 -> 3
        6 -> 4
        else -> 0
    }
}

private fun generateMonthGrid(year: Int, month: Int): List<CalendarCell> {
    val firstDayOfWeek = getFirstDayOfWeek(year, month)
    val daysInCurrMonth = getDaysInMonth(year, month)
    val prevMonth = if (month == 0) 11 else month - 1
    val prevYear = if (month == 0) year - 1 else year
    val daysInPrevMonth = getDaysInMonth(prevYear, prevMonth)

    val cells = mutableListOf<CalendarCell>()

    for (i in (firstDayOfWeek - 1) downTo 0) {
        val dayNum = daysInPrevMonth - i
        val col = cells.size % 7
        cells.add(CalendarCell(dayNum, -1, col >= 5))
    }

    for (dayNum in 1..daysInCurrMonth) {
        val col = cells.size % 7
        cells.add(CalendarCell(dayNum, 0, col >= 5))
    }

    var nextDayNum = 1
    while (cells.size % 7 != 0 || cells.size < 35) {
        val col = cells.size % 7
        cells.add(CalendarCell(nextDayNum++, 1, col >= 5))
    }

    return cells
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
    ChatSharedScreen()
}

@Composable
fun FullSettingsSection() {
    SettingsSharedScreen(onBack = {})
}
