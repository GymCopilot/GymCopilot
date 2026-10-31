package com.zexo.gymcopilot.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ScheduleEntry
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.ui.viewmodels.ScheduleViewModel
import com.zexo.gymcopilot.ui.viewmodels.ScheduleSyncState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ScheduleViewMode { DAY, WEEK, MONTH }
enum class EditingScope { GLOBAL, WEEKLY, SPECIFIC }

data class ColoredEntry(val entry: ScheduleEntry, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScheduleScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onMembersClick: () -> Unit,
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    isAdmin: Boolean = true,
    isProfessor: Boolean = false
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val linkedProfId by dataStoreManager.getLinkedProfessorId().collectAsState(initial = null)

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    var selectedProfessor by remember { mutableStateOf<Professor?>(null) }
    var selectedTab by remember { mutableIntStateOf(if (isProfessor) 1 else 0) }

    var viewMode by remember { mutableStateOf(ScheduleViewMode.MONTH) }
    var editingScope by remember { mutableStateOf(EditingScope.GLOBAL) }
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }

    val allSchedules by dataStoreManager.getProfessorSchedules().collectAsState(initial = emptyMap())
    val syncState by viewModel.syncState.collectAsState()

    val locale = remember(appLanguage) {
        if (appLanguage == "Español") Locale("es", "ES") else Locale.getDefault()
    }

    LaunchedEffect(Unit) { viewModel.syncSchedules() }

    LaunchedEffect(isProfessor, linkedProfId, professors) {
        if (isProfessor && linkedProfId != null) {
            selectedProfessor = professors.find { it.id == linkedProfId }
            selectedTab = 1
        }
    }

    LaunchedEffect(syncState) {
        if (syncState is ScheduleSyncState.Error) {
            snackbarHostState.showSnackbar((syncState as ScheduleSyncState.Error).message)
            viewModel.resetState()
        }
    }

    val targetIdForEdit = if (selectedTab == 0) "GENERAL_GYM" else {
        selectedProfessor?.email?.lowercase()?.takeIf { it.isNotBlank() } ?: selectedProfessor?.id ?: ""
    }

    val coloredEntries = remember(selectedTab, selectedProfessor, allSchedules, professors, accentColor) {
        val list = mutableListOf<ColoredEntry>()
        if (isAdmin && selectedTab == 0) {
            allSchedules["GENERAL_GYM"]?.forEach { list.add(ColoredEntry(it, accentColor)) }
            professors.forEach { prof ->
                val profColor = prof.profileColor?.let { Color(it) } ?: accentColor
                val key = prof.email.lowercase().takeIf { it.isNotBlank() } ?: prof.id
                allSchedules[key]?.forEach { list.add(ColoredEntry(it, profColor)) }
            }
        } else {
            val profColor = selectedProfessor?.profileColor?.let { Color(it) } ?: accentColor
            allSchedules[targetIdForEdit]?.forEach { list.add(ColoredEntry(it, profColor)) }
        }
        list
    }

    val t = remember(appLanguage) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Gestión de Horarios", "gym" to "Gimnasio", "profs" to "Profesores",
                "day" to "Día", "week" to "Semana", "month" to "Mes", "save" to "Guardar Cambios",
                "select_prof" to "Selecciona tu Perfil", "add_event" to "Nuevo Evento",
                "time" to "Hora", "event" to "Evento", "hoy" to "HOY", "syncing" to "Sincronizando...",
                "link_desc" to "Como profesor, selecciona tu perfil para gestionar tus horarios. Esta acción es necesaria una sola vez.",
                "pattern" to "Patrón Semanal: Todos los "
            )
        } else {
            mapOf(
                "title" to "Schedule Management", "gym" to "Gym", "profs" to "Professors",
                "day" to "Day", "week" to "Week", "month" to "Month", "save" to "Save Changes",
                "select_prof" to "Select your Profile", "add_event" to "New Event",
                "time" to "Time", "event" to "Event", "hoy" to "TODAY", "syncing" to "Syncing...",
                "link_desc" to "As a professor, select your profile to manage your schedules. This action is required only once.",
                "pattern" to "Weekly Pattern: Every "
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            if (isProfessor) {
                ProfessorBottomNavigation(
                    currentRoute = "manage_schedule",
                    onHomeClick = onHomeClick, onStoreClick = onStoreClick, onScheduleClick = onScheduleClick,
                    onMembersClick = onMembersClick, onRoutinesClick = onRoutinesClick, onChatClick = onChatClick, accentColor = accentColor
                )
            } else {
                AdminBottomNavigation(
                    currentRoute = "",
                    onHomeClick = onHomeClick, onStoreClick = onStoreClick, onMembersClick = onMembersClick,
                    onProfessorsClick = onProfessorsClick, onSettingsClick = onSettingsClick, accentColor = accentColor
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (isAdmin) {
                val buttonBgColor = LocalButtonBackgroundColor.current
                Row(modifier = Modifier.fillMaxWidth().background(buttonBgColor, RoundedCornerShape(12.dp)).border(1.dp, accentColor, RoundedCornerShape(12.dp)).padding(4.dp)) {
                    listOf(t["gym"], t["profs"]).forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val tabBg = if (isSelected) accentColor else Color.Transparent
                        val tabText = if (isSelected) (if (accentColor.luminance() > 0.5f) Color.Black else Color.White)
                                      else (if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite)
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(tabBg)
                                .clickable { selectedTab = index; if (index == 0) selectedProfessor = null }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title ?: "", color = tabText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (selectedTab == 1 && selectedProfessor == null) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t["select_prof"] ?: "Seleccionar", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(t["link_desc"] ?: "", color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(24.dp))
                    ProfessorList(professors, accentColor, shape) { prof ->
                        if (isProfessor) {
                            scope.launch { dataStoreManager.setLinkedProfessorId(prof.id); selectedProfessor = prof }
                        } else { selectedProfessor = prof }
                    }
                }
            } else {
                ScheduleHeader(calendar, viewMode, t, accentColor, shape, locale,
                    onViewModeChange = { mode ->
                        viewMode = mode
                        if (mode == ScheduleViewMode.DAY) editingScope = EditingScope.GLOBAL
                    },
                    onPrev = { calendar = (calendar.clone() as Calendar).apply { add(if (viewMode == ScheduleViewMode.MONTH) Calendar.MONTH else Calendar.DAY_OF_YEAR, if (viewMode == ScheduleViewMode.WEEK) -7 else -1) } },
                    onNext = { calendar = (calendar.clone() as Calendar).apply { add(if (viewMode == ScheduleViewMode.MONTH) Calendar.MONTH else Calendar.DAY_OF_YEAR, if (viewMode == ScheduleViewMode.WEEK) 7 else 1) } },
                    onToday = { calendar = Calendar.getInstance() }
                )

                if (selectedProfessor != null) {
                    Text("Perfil: ${selectedProfessor?.fullName}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = viewMode,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                            } else {
                                slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                            }
                        }, label = "ModeTransition"
                    ) { mode ->
                        when (mode) {
                            ScheduleViewMode.DAY -> DayView(calendar, editingScope, coloredEntries, accentColor, shape, t, locale) { updatedForDay ->
                                scope.launch {
                                    val fullList = allSchedules[targetIdForEdit] ?: emptyList()
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                                    
                                    // Fusión inteligente: Mantener lo que no es de hoy/este alcance
                                    val merged = fullList.filterNot {
                                        when (editingScope) {
                                            EditingScope.GLOBAL -> it.date == null && it.dayOfWeek == null
                                            EditingScope.WEEKLY -> it.date == null && it.dayOfWeek == dayOfWeek
                                            EditingScope.SPECIFIC -> it.date == dateStr
                                        }
                                    } + updatedForDay
                                    
                                    dataStoreManager.saveProfessorSchedule(targetIdForEdit, merged)
                                    viewModel.saveAndUploadSchedule(targetIdForEdit, merged)
                                }
                            }
                            ScheduleViewMode.WEEK -> WeekView(calendar, coloredEntries, accentColor, shape, t, locale,
                                onDaySelected = { date -> calendar = (calendar.clone() as Calendar).apply { time = date } },
                                onSave = { updatedForSpecificDayOfWeek ->
                                    scope.launch {
                                        val fullList = allSchedules[targetIdForEdit] ?: emptyList()
                                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                                        
                                        // Fusión inteligente: Mantener todo excepto lo del patrón semanal de este día
                                        val merged = fullList.filterNot { it.date == null && it.dayOfWeek == dayOfWeek } + updatedForSpecificDayOfWeek
                                        
                                        dataStoreManager.saveProfessorSchedule(targetIdForEdit, merged)
                                        viewModel.saveAndUploadSchedule(targetIdForEdit, merged)
                                    }
                                }
                            )
                            ScheduleViewMode.MONTH -> MonthView(calendar, coloredEntries, accentColor, shape, t, locale) { date ->
                                calendar = (calendar.clone() as Calendar).apply { time = date }
                                editingScope = EditingScope.SPECIFIC
                                viewMode = ScheduleViewMode.DAY
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleHeader(calendar: Calendar, viewMode: ScheduleViewMode, t: Map<String, String>, accentColor: Color, shape: Shape, locale: Locale, onViewModeChange: (ScheduleViewMode) -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val monthName = SimpleDateFormat("MMMM 'de' yyyy", locale).format(calendar.time)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    val buttonBgColor = LocalButtonBackgroundColor.current
    val navTextColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(buttonBgColor, RoundedCornerShape(8.dp)).border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))) {
                IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, null, tint = navTextColor) }
                Text(t["hoy"] ?: "HOY", color = navTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { onToday() }.padding(horizontal = 8.dp))
                IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, null, tint = navTextColor) }
            }
            Text(monthName, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        val buttonBgColor = LocalButtonBackgroundColor.current
        Row(modifier = Modifier.fillMaxWidth().background(buttonBgColor, RoundedCornerShape(12.dp)).border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(4.dp)) {
            ScheduleViewMode.entries.forEach { mode ->
                val label = when(mode) { ScheduleViewMode.DAY -> t["day"]; ScheduleViewMode.WEEK -> t["week"]; ScheduleViewMode.MONTH -> t["month"] }
                val isSelected = viewMode == mode
                val modeTextColor = if (isSelected) accentColor else (if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray)
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent).border(if (isSelected) 1.dp else 0.dp, if (isSelected) accentColor else Color.Transparent, RoundedCornerShape(8.dp)).clickable { onViewModeChange(mode) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(label ?: "", color = modeTextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun DayView(
    calendar: Calendar,
    scope: EditingScope,
    allColoredEntries: List<ColoredEntry>,
    accentColor: Color,
    shape: Shape,
    t: Map<String, String>,
    locale: Locale,
    onSave: (List<ScheduleEntry>) -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val dayColoredEntries = remember(allColoredEntries, dateStr, dayOfWeek, scope) {
        allColoredEntries.filter {
            when (scope) {
                EditingScope.GLOBAL -> it.entry.date == null && it.entry.dayOfWeek == null
                EditingScope.WEEKLY -> it.entry.date == null && it.entry.dayOfWeek == dayOfWeek
                EditingScope.SPECIFIC -> {
                    // Mostrar TODO lo que aplica a hoy: Único, Semanal Recurrente y Global
                    it.entry.date == dateStr || 
                    (it.entry.date == null && it.entry.dayOfWeek == dayOfWeek) ||
                    (it.entry.date == null && it.entry.dayOfWeek == null)
                }
            }
        }.sortedBy { it.entry.startTime }
    }

    var editingEntries by remember(dayColoredEntries) { mutableStateOf(dayColoredEntries) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        val scopeLabel = when(scope) {
            EditingScope.GLOBAL -> "Configuración General (Todos los días)"
            EditingScope.WEEKLY -> (t["pattern"] ?: "") + SimpleDateFormat("EEEE", locale).format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            EditingScope.SPECIFIC -> "Eventos del " + SimpleDateFormat("d 'de' MMMM", locale).format(calendar.time)
        }
        Text(scopeLabel, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(t["time"] ?: "Hora", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(t["event"] ?: "Evento", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(editingEntries) { coloredEntry ->
                val entry = coloredEntry.entry
                val buttonBgColor = LocalButtonBackgroundColor.current
                val entryTextColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.4f), shape),
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = buttonBgColor)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${entry.startTime} - ${entry.endTime}", color = entryTextColor, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.2f))
                        Text(entry.eventName, color = coloredEntry.color, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        IconButton(
                            onClick = {
                                editingEntries = editingEntries - coloredEntry
                                onSave(editingEntries.map { it.entry })
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFF44336), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            item {
                val buttonBgColor = LocalButtonBackgroundColor.current
                val contentColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else accentColor

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().border(1.5.dp, accentColor, shape),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBgColor),
                    shape = shape
                ) {
                    Icon(Icons.Default.Add, null, tint = contentColor); Spacer(Modifier.width(8.dp)); Text(t["add_event"] ?: "Nuevo Evento", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleEventDialog(
            t = t,
            accentColor = accentColor,
            shape = shape,
            scope = scope,
            currentDayOfWeek = dayOfWeek,
            currentDate = dateStr,
            onDismiss = { showAddDialog = false },
            onConfirm = { newEntries ->
                showAddDialog = false
                // Actualizamos localmente con los que correspondan al día actual para que se vea el cambio al instante
                editingEntries = editingEntries + newEntries.filter { 
                    (scope == EditingScope.GLOBAL && it.dayOfWeek == null && it.date == null) ||
                    (scope == EditingScope.WEEKLY && it.dayOfWeek == dayOfWeek) ||
                    (scope == EditingScope.SPECIFIC && it.date == dateStr)
                }.map { ColoredEntry(it, accentColor) }
                
                onSave(newEntries) 
            }
        )
    }
}

@Composable
fun AddScheduleEventDialog(
    t: Map<String, String>,
    accentColor: Color,
    shape: Shape,
    scope: EditingScope,
    currentDayOfWeek: Int,
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (List<ScheduleEntry>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:00") }
    
    val dayNames = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
    val dayToCalendar = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
    
    val selectedDays = remember { mutableStateListOf<Int>().apply { 
        if (scope == EditingScope.WEEKLY) add(currentDayOfWeek)
    }}

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C1221),
        title = { Text(t["add_event"] ?: "Nuevo Evento", color = TextWhite, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre del evento", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = accentColor)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inicio", color = TextGray, fontSize = 12.sp)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val parts = startTime.split(":")
                                TimePickerDialog(context, { _, h, m -> startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m) }, parts[0].toInt(), parts[1].toInt(), true).show()
                            },
                            color = Color.White.copy(alpha = 0.05f), shape = shape, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(startTime, color = TextWhite, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fin", color = TextGray, fontSize = 12.sp)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val parts = endTime.split(":")
                                TimePickerDialog(context, { _, h, m -> endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m) }, parts[0].toInt(), parts[1].toInt(), true).show()
                            },
                            color = Color.White.copy(alpha = 0.05f), shape = shape, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(endTime, color = TextWhite, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                        }
                    }
                }

                if (scope == EditingScope.WEEKLY) {
                    Text("Repetir los días:", color = TextGray, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        dayNames.forEachIndexed { index, dayName ->
                            val calDay = dayToCalendar[index]
                            val isSelected = selectedDays.contains(calDay)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.05f))
                                    .clickable { if (isSelected) selectedDays.remove(calDay) else selectedDays.add(calDay) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dayName, color = if (isSelected) Color.Black else TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = when (scope) {
                        EditingScope.GLOBAL -> listOf(ScheduleEntry(startTime = startTime, endTime = endTime, eventName = name))
                        EditingScope.SPECIFIC -> listOf(ScheduleEntry(startTime = startTime, endTime = endTime, eventName = name, date = currentDate))
                        EditingScope.WEEKLY -> {
                            if (selectedDays.isEmpty()) listOf(ScheduleEntry(startTime = startTime, endTime = endTime, eventName = name, dayOfWeek = currentDayOfWeek))
                            else selectedDays.map { ScheduleEntry(startTime = startTime, endTime = endTime, eventName = name, dayOfWeek = it) }
                        }
                    }
                    onConfirm(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                shape = shape,
                enabled = name.isNotBlank(),
                modifier = Modifier.border(1.dp, accentColor, shape)
            ) { 
                val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                Text("Agregar", color = contentColor, fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextGray) }
        }
    )
}

@Composable
fun WeekView(calendar: Calendar, allColoredEntries: List<ColoredEntry>, accentColor: Color, shape: Shape, t: Map<String, String>, locale: Locale, onDaySelected: (Date) -> Unit, onSave: (List<ScheduleEntry>) -> Unit) {
    val weekCalendar = calendar.clone() as Calendar
    weekCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val days = (0..6).map { val d = weekCalendar.time; weekCalendar.add(Calendar.DAY_OF_YEAR, 1); d }
    val currentDayIndex = calendar.get(Calendar.DAY_OF_WEEK)

    val weekendRed = Color(0xFFE57373)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { date ->
                val cal = Calendar.getInstance().apply { time = date }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val isSelected = dayOfWeek == currentDayIndex
                val dayName = SimpleDateFormat("EEE", locale).format(date).uppercase()
                val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

                val dayHasActivity = allColoredEntries.any { it.entry.dayOfWeek == dayOfWeek && it.entry.date == null }

                Column(
                    modifier = Modifier.weight(1f).clip(shape)
                        .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                        .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.05f), shape)
                        .clickable { onDaySelected(date) }.padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayName,
                        color = if (isWeekend) weekendRed else if (isSelected) accentColor else TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (dayHasActivity) {
                        Spacer(Modifier.height(6.dp))
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accentColor))
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = calendar.time,
                transitionSpec = {
                    val targetCal = Calendar.getInstance().apply { time = targetState }
                    val targetIdx = (targetCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val initialIdx = (Calendar.getInstance().apply { time = initialState }.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    if (targetIdx > initialIdx) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                }, label = "DaySlide"
            ) { date ->
                val dayCal = Calendar.getInstance().apply { time = date }
                DayView(dayCal, EditingScope.WEEKLY, allColoredEntries, accentColor, shape, t, locale, onSave)
            }
        }
    }
}

@Composable
fun MonthView(calendar: Calendar, allEntries: List<ColoredEntry>, accentColor: Color, shape: Shape, t: Map<String, String>, locale: Locale, onDayClick: (Date) -> Unit) {
    val monthCal = calendar.clone() as Calendar
    monthCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevMonthCal = monthCal.clone() as Calendar
    prevMonthCal.add(Calendar.MONTH, -1)
    val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dayNames = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
    val weekendRed = Color(0xFFE57373)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            dayNames.forEachIndexed { index, name ->
                Text(
                    text = name,
                    color = if (index >= 5) weekendRed else TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        for (row in 0..5) {
            Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0..6) {
                    val dayIndex = row * 7 + col
                    val dayNum: Int
                    val isCurrentMonth: Boolean

                    if (dayIndex < firstDayOfWeek) {
                        dayNum = daysInPrevMonth - firstDayOfWeek + dayIndex + 1
                        isCurrentMonth = false
                    } else if (dayIndex < firstDayOfWeek + daysInMonth) {
                        dayNum = dayIndex - firstDayOfWeek + 1
                        isCurrentMonth = true
                    } else {
                        dayNum = dayIndex - (firstDayOfWeek + daysInMonth) + 1
                        isCurrentMonth = false
                    }

                    val cellDate = (monthCal.clone() as Calendar).apply {
                        if (!isCurrentMonth) { if (dayIndex < firstDayOfWeek) add(Calendar.MONTH, -1) else add(Calendar.MONTH, 1) }
                        set(Calendar.DAY_OF_MONTH, dayNum)
                    }.time

                    val cellCal = Calendar.getInstance().apply { time = cellDate }
                    val isWeekend = cellCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cellCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

                    val dayActivities = allEntries.filter {
                        val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cellDate)
                        it.entry.date == dStr || (it.entry.dayOfWeek == cellCal.get(Calendar.DAY_OF_WEEK) && it.entry.date == null) || (it.entry.dayOfWeek == null && it.entry.date == null)
                    }

                    val today = Calendar.getInstance()
                    val isToday = today.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == cellCal.get(Calendar.DAY_OF_YEAR)

                    MonthDayCell(
                        dayNum = dayNum, isCurrentMonth = isCurrentMonth, isWeekend = isWeekend,
                        activityColors = dayActivities.map { it.color }.distinct(), isToday = isToday, accentColor = accentColor,
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDayClick(cellDate) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun MonthDayCell(dayNum: Int, isCurrentMonth: Boolean, isWeekend: Boolean, activityColors: List<Color>, isToday: Boolean, accentColor: Color, modifier: Modifier = Modifier) {
    val weekendRed = Color(0xFFE57373)
    val buttonBgColor = LocalButtonBackgroundColor.current
    val dayTextColor = when {
        isToday -> accentColor
        !isCurrentMonth -> TextGray.copy(alpha = 0.3f)
        isWeekend -> weekendRed
        else -> if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    }

    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))
        .background(when { 
            isToday -> accentColor.copy(alpha = 0.25f)
            isCurrentMonth -> buttonBgColor
            else -> Color.Transparent 
        })
        .border(if (isToday) 2.dp else 1.dp, if (isToday) accentColor else if (isCurrentMonth) accentColor.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(4.dp))) {

        if (isWeekend) {
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

        Column(modifier = Modifier.padding(4.dp)) {
            Text(dayNum.toString(), color = dayTextColor, fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold)
            if (activityColors.isNotEmpty() && isCurrentMonth) {
                Spacer(Modifier.height(4.dp))
                activityColors.take(4).forEach { color ->
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).padding(bottom = 1.dp).clip(RoundedCornerShape(1.dp)).background(color.copy(alpha = 0.7f)))
                }
            }
        }
    }
}

@Composable
fun ProfessorList(professors: List<Professor>, accentColor: Color, shape: Shape, onSelect: (Professor) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(professors) { prof ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(prof) }.border(1.dp, Color.White.copy(alpha = 0.1f), shape), shape = shape, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val profColor = prof.profileColor?.let { Color(it) } ?: accentColor
                    Box(modifier = Modifier.size(40.dp).background(profColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = profColor) }
                    Spacer(modifier = Modifier.width(12.dp)); Text(prof.fullName, color = TextWhite, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
