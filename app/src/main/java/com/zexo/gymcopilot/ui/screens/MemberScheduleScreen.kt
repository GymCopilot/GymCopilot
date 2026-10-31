package com.zexo.gymcopilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ScheduleEntry
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.ui.viewmodels.ScheduleViewModel
import com.zexo.gymcopilot.ui.viewmodels.ScheduleSyncState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberScheduleScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onClassesClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }

    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val allSchedules by dataStoreManager.getProfessorSchedules().collectAsState(initial = emptyMap())
    val syncState by viewModel.syncState.collectAsState()

    val locale = remember(appLanguage) {
        if (appLanguage == "Español") Locale("es", "ES") else Locale.getDefault()
    }

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    var viewMode by remember { mutableStateOf(ScheduleViewMode.MONTH) }
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }

    val currentMember = remember(userEmail, members) { members.find { it.email.equals(userEmail, ignoreCase = true) } }
    val assignedProfessor = remember(currentMember, professors) {
        professors.find { it.fullName == currentMember?.assignedTrainer }
    }

    LaunchedEffect(Unit) { viewModel.syncSchedules() }

    val coloredEntries = remember(allSchedules, assignedProfessor, accentColor) {
        val list = mutableListOf<ColoredEntry>()
        // Clases generales del gimnasio (color de acento)
        allSchedules["GENERAL_GYM"]?.forEach { list.add(ColoredEntry(it, accentColor)) }

        // Clases del profesor asignado (su color de perfil)
        assignedProfessor?.let { prof ->
            val profColor = prof.profileColor?.let { Color(it) } ?: Color(0xFF2196F3)
            val key = prof.email.lowercase().takeIf { it.isNotBlank() } ?: prof.id
            allSchedules[key]?.forEach { list.add(ColoredEntry(it, profColor)) }
        }
        list
    }

    val t = remember(appLanguage) {
        if (appLanguage == "Español") {
            mapOf("title" to "Horarios de Clases", "hoy" to "HOY", "day" to "Día", "week" to "Semana", "month" to "Mes")
        } else {
            mapOf("title" to "Class Schedule", "hoy" to "TODAY", "day" to "Day", "week" to "Week", "month" to "Month")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold) },
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
                    if (syncState is ScheduleSyncState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = accentColor, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.syncSchedules() }) { Icon(Icons.Default.Refresh, null, tint = TextWhite) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            MemberBottomNavigation(
                currentRoute = "member_classes",
                onHomeClick = onHomeClick, onStoreClick = onStoreClick,
                onClassesClick = onClassesClick, onRoutinesClick = onRoutinesClick,
                onChatClick = onChatClick, accentColor = accentColor
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            assignedProfessor?.let {
                val profColor = it.profileColor?.let { c -> Color(c) } ?: accentColor
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = profColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, profColor.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = profColor)
                        Spacer(Modifier.width(8.dp))
                        Text("Mi Profesor: ${it.fullName}", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            ScheduleHeader(
                calendar = calendar, viewMode = viewMode, t = t, accentColor = accentColor, shape = shape, locale = locale,
                onViewModeChange = { viewMode = it },
                onPrev = { calendar = (calendar.clone() as Calendar).apply { add(if (viewMode == ScheduleViewMode.MONTH) Calendar.MONTH else Calendar.DAY_OF_YEAR, if (viewMode == ScheduleViewMode.WEEK) -7 else -1) } },
                onNext = { calendar = (calendar.clone() as Calendar).apply { add(if (viewMode == ScheduleViewMode.MONTH) Calendar.MONTH else Calendar.DAY_OF_YEAR, if (viewMode == ScheduleViewMode.WEEK) 7 else 1) } },
                onToday = { calendar = Calendar.getInstance() }
            )

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (viewMode) {
                    ScheduleViewMode.DAY -> DayViewReadOnly(calendar, coloredEntries, shape, locale)
                    ScheduleViewMode.WEEK -> WeekViewReadOnly(calendar, coloredEntries, accentColor, shape, locale, onDaySelected = { calendar = (calendar.clone() as Calendar).apply { time = it } })
                    ScheduleViewMode.MONTH -> MonthView(calendar, coloredEntries, accentColor, shape, t, locale) { calendar = (calendar.clone() as Calendar).apply { time = it }; viewMode = ScheduleViewMode.DAY }
                }
            }
        }
    }
}

@Composable
fun DayViewReadOnly(calendar: Calendar, allColoredEntries: List<ColoredEntry>, shape: androidx.compose.ui.graphics.Shape, locale: Locale) {
    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val dayEntries = allColoredEntries.filter {
        it.entry.date == dateStr || (it.entry.dayOfWeek == dayOfWeek && it.entry.date == null) || (it.entry.dayOfWeek == null && it.entry.date == null)
    }.sortedBy { it.entry.startTime }

    val dayLabel = java.text.SimpleDateFormat("d 'de' MMMM", locale).format(calendar.time)
    
    Column {
        Text("Eventos del $dayLabel", color = PrimaryTurquoise, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dayEntries) { item ->
                Row(modifier = Modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = 0.03f)).border(1.dp, item.color.copy(alpha = 0.2f), shape).padding(16.dp)) {
                    Column {
                        Text("${item.entry.startTime} - ${item.entry.endTime}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(item.entry.eventName, color = item.color, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun WeekViewReadOnly(
    calendar: Calendar,
    allColoredEntries: List<ColoredEntry>,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    locale: Locale,
    onDaySelected: (Date) -> Unit
) {
    val weekCalendar = calendar.clone() as Calendar
    weekCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val days = (0..6).map { val d = weekCalendar.time; weekCalendar.add(Calendar.DAY_OF_YEAR, 1); d }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { date ->
                val cal = Calendar.getInstance().apply { time = date }
                val isSelected = cal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

                val dayName = java.text.SimpleDateFormat("EEE", locale).format(date).uppercase()

                val buttonBgColor = LocalButtonBackgroundColor.current
                val textNormalColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.25f) else buttonBgColor)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        color = when {
                            isSelected -> accentColor
                            isWeekend -> Color(0xFFE53935)
                            else -> textNormalColor
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        DayViewReadOnly(calendar, allColoredEntries, shape, locale)
    }
}
