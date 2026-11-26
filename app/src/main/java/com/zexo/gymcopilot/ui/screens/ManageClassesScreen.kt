package com.zexo.gymcopilot.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.ScheduleEntry
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.ui.viewmodels.ScheduleViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// Estructura para agrupar clases en la UI por Horario, Nombre y Alumnos
data class UIClassGroup(
    val eventName: String,
    val startTime: String,
    val endTime: String,
    val days: List<Int>,
    val assignedMemberEmails: List<String>,
    val originalEntries: List<ScheduleEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageClassesScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onMembersClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val allSchedules by dataStoreManager.getProfessorSchedules().collectAsState(initial = emptyMap())

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    val myMembers = remember(members, userEmail) {
        members.filter { it.assignedTrainer.isNotBlank() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    val currentSchedules = remember(allSchedules, userEmail) {
        allSchedules[userEmail.lowercase()] ?: emptyList()
    }

    // Agrupamiento inteligente de clases para la visualización unificada
    val groupedSchedules = remember(currentSchedules) {
        val groups = mutableListOf<UIClassGroup>()
        currentSchedules.forEach { entry ->
            val existingIndex = groups.indexOfFirst { 
                it.eventName.trim().equals(entry.eventName.trim(), ignoreCase = true) && 
                it.startTime == entry.startTime && 
                it.endTime == entry.endTime &&
                it.assignedMemberEmails.sorted() == entry.assignedMemberEmails.sorted()
            }
            
            if (existingIndex != -1) {
                val existing = groups[existingIndex]
                val day = entry.dayOfWeek
                val updatedDays = if (day != null && !existing.days.contains(day)) {
                    (existing.days + (day as Int)).sortedBy { d -> (d + 5) % 7 }
                } else {
                    existing.days
                }
                groups[existingIndex] = existing.copy(
                    days = updatedDays,
                    originalEntries = existing.originalEntries + entry
                )
            } else {
                groups.add(UIClassGroup(
                    eventName = entry.eventName,
                    startTime = entry.startTime,
                    endTime = entry.endTime,
                    days = listOfNotNull(entry.dayOfWeek),
                    assignedMemberEmails = entry.assignedMemberEmails,
                    originalEntries = listOf(entry)
                ))
            }
        }
        groups.sortedBy { it.startTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Clases", 
                        color = TextWhite, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            ProfessorBottomNavigation(
                currentRoute = "",
                onHomeClick = onHomeClick, onStoreClick = onStoreClick, onScheduleClick = {},
                onMembersClick = onMembersClick, onRoutinesClick = onRoutinesClick, onChatClick = onChatClick, accentColor = accentColor
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = accentColor,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Clase", modifier = Modifier.size(32.dp))
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(groupedSchedules) { group ->
                ClassEntryCard(
                    group = group,
                    allMembers = myMembers,
                    accentColor = accentColor,
                    shape = shape,
                    onUpdate = { updatedGroup ->
                        val newList = currentSchedules.toMutableList()
                        newList.removeAll { old -> group.originalEntries.any { it === old } }
                        
                        val updatedEntries = updatedGroup.days.map { day ->
                            ScheduleEntry(
                                startTime = updatedGroup.startTime,
                                endTime = updatedGroup.endTime,
                                eventName = updatedGroup.eventName,
                                dayOfWeek = day,
                                assignedMemberEmails = updatedGroup.assignedMemberEmails
                            )
                        }
                        newList.addAll(updatedEntries)
                        
                        scope.launch {
                            dataStoreManager.saveProfessorSchedule(userEmail.lowercase(), newList)
                            viewModel.saveAndUploadSchedule(userEmail.lowercase(), newList)
                        }
                    },
                    onDelete = {
                        val newList = currentSchedules.filter { old -> group.originalEntries.none { it === old } }
                        scope.launch {
                            dataStoreManager.saveProfessorSchedule(userEmail.lowercase(), newList)
                            viewModel.saveAndUploadSchedule(userEmail.lowercase(), newList)
                        }
                    }
                )
            }
        }

        if (showAddDialog) {
            ClassDialog(
                title = "Nueva Clase",
                onDismiss = { showAddDialog = false },
                accentColor = accentColor,
                onConfirm = { startTime, endTime, name, days ->
                    val newEntries = days.map { day ->
                        ScheduleEntry(
                            startTime = startTime,
                            endTime = endTime,
                            eventName = name,
                            dayOfWeek = day
                        )
                    }
                    val newList = currentSchedules + newEntries
                    scope.launch {
                        dataStoreManager.saveProfessorSchedule(userEmail.lowercase(), newList)
                        viewModel.saveAndUploadSchedule(userEmail.lowercase(), newList)
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassEntryCard(
    group: UIClassGroup,
    allMembers: List<Member>,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onUpdate: (UIClassGroup) -> Unit,
    onDelete: () -> Unit
) {
    var showMemberPicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val daysMap = listOf(
        "LU" to Calendar.MONDAY,
        "MA" to Calendar.TUESDAY,
        "MI" to Calendar.WEDNESDAY,
        "JU" to Calendar.THURSDAY,
        "VI" to Calendar.FRIDAY,
        "SÁ" to Calendar.SATURDAY,
        "DO" to Calendar.SUNDAY
    )

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.2f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                // COLUMNA IZQUIERDA: Nombre, Días y Horario verticalmente
                Column(modifier = Modifier.weight(1f)) {
                    // 1. Nombre
                    Text(
                        text = group.eventName,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // 2. Días (como círculos)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        daysMap.forEach { (label, value) ->
                            val isSelected = group.days.contains(value)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) (if (accentColor.luminance() > 0.5f) Color.Black else Color.White) else TextGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3. Horario
                    Text(
                        text = "${group.startTime} - ${group.endTime}",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
                
                // Botones de acción a la derecha
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar Clase",
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = { showMemberPicker = true }) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Asignar Alumnos",
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFD32F2F), // Rojo sólido
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            
            if (group.assignedMemberEmails.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                // Lista de alumnos debajo
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.assignedMemberEmails.forEach { email ->
                        val member = allMembers.find { it.email == email }
                        val isPresent = remember(member?.lastVisit) {
                            member?.lastVisit?.let {
                                val lastVisitCal = Calendar.getInstance().apply { timeInMillis = it }
                                val nowCal = Calendar.getInstance()
                                lastVisitCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                                lastVisitCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                            } ?: false
                        }

                        AssistBadge(
                            name = member?.firstName ?: email, 
                            color = if (isPresent) Color(0xFF4CAF50) else Color.Gray,
                            onDelete = {
                                val newList = group.assignedMemberEmails.filter { it != email }
                                onUpdate(group.copy(assignedMemberEmails = newList))
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMemberPicker) {
        MemberPickerActionSheet(
            allMembers = allMembers,
            selectedEmails = group.assignedMemberEmails,
            accentColor = accentColor,
            onDismiss = { showMemberPicker = false },
            onMemberToggled = { email ->
                val current = group.assignedMemberEmails.toMutableList()
                if (current.contains(email)) current.remove(email) else current.add(email)
                onUpdate(group.copy(assignedMemberEmails = current))
            }
        )
    }

    if (showEditDialog) {
        ClassDialog(
            title = "Editar Clase",
            initialName = group.eventName,
            initialStartTime = group.startTime,
            initialEndTime = group.endTime,
            initialDays = group.days,
            onDismiss = { showEditDialog = false },
            accentColor = accentColor,
            onConfirm = { startTime, endTime, name, days ->
                onUpdate(group.copy(
                    eventName = name,
                    startTime = startTime,
                    endTime = endTime,
                    days = days
                ))
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AssistBadge(name: String, color: Color, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Close, 
                null, 
                tint = Color.Red.copy(alpha = 0.6f), 
                modifier = Modifier.size(14.dp).clickable { onDelete() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPickerActionSheet(
    allMembers: List<Member>,
    selectedEmails: List<String>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onMemberToggled: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF00222E)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text("Inscribir Alumnos", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allMembers) { member ->
                    val isSelected = selectedEmails.contains(member.email)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMemberToggled(member.email) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onMemberToggled(member.email) },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(member.fullName, color = TextWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun ClassDialog(
    title: String,
    initialName: String = "",
    initialStartTime: String = "08:00",
    initialEndTime: String = "09:00",
    initialDays: List<Int> = emptyList(),
    onDismiss: () -> Unit,
    accentColor: Color,
    onConfirm: (String, String, String, List<Int>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    val selectedDays = remember { mutableStateListOf<Int>() }
    val context = LocalContext.current

    LaunchedEffect(initialDays) {
        selectedDays.clear()
        selectedDays.addAll(initialDays)
    }

    val daysMap = listOf(
        "LU" to Calendar.MONDAY,
        "MA" to Calendar.TUESDAY,
        "MI" to Calendar.WEDNESDAY,
        "JU" to Calendar.THURSDAY,
        "VI" to Calendar.FRIDAY,
        "SÁ" to Calendar.SATURDAY,
        "DO" to Calendar.SUNDAY
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF081C24),
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la clase", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Text("Días de la semana", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysMap.forEach { (label, value) ->
                        val isSelected = selectedDays.contains(value)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.1f), CircleShape)
                                .clickable {
                                    if (isSelected) selectedDays.remove(value) else selectedDays.add(value)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) (if (accentColor.luminance() > 0.5f) Color.Black else Color.White) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val parts = startTime.split(":")
                            val h = parts[0].toIntOrNull() ?: 8
                            val m = parts[1].toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, selectedH, selectedM -> startTime = String.format(Locale.getDefault(), "%02d:%02d", selectedH, selectedM) }, h, m, true).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Text(startTime, color = Color.White)
                    }
                    Button(
                        onClick = {
                            val parts = endTime.split(":")
                            val h = parts[0].toIntOrNull() ?: 9
                            val m = parts[1].toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, selectedH, selectedM -> endTime = String.format(Locale.getDefault(), "%02d:%02d", selectedH, selectedM) }, h, m, true).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Text(endTime, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && selectedDays.isNotEmpty()) onConfirm(startTime, endTime, name, selectedDays.toList()) },
                enabled = name.isNotBlank() && selectedDays.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                modifier = Modifier.border(1.dp, accentColor, RoundedCornerShape(14.dp))
            ) {
                val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                Text("ACEPTAR", color = contentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = accentColor)
            }
        }
    )
}
