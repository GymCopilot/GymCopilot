package com.zexo.gymcopilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.Routine
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDashboardScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onClassesClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onNewRoutineClick: () -> Unit = {},
    onStartRoutine: (String) -> Unit = {},
    onAssignRoutine: (String) -> Unit = {},
    onEditRoutine: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val savedRoutines by dataStoreManager.getRoutines().collectAsState(initial = emptyList())
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var favoritesFilterActive by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    var showCategorySheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todas") }

    var routineToAssign by remember { mutableStateOf<Routine?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    val repository = remember(gymApiUrl) {
        AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
    }

    LaunchedEffect(gymApiUrl, userEmail) {
        if (gymApiUrl.isNotBlank() && userEmail.isNotBlank()) {
            while (isActive) {
                repository.syncRoutinesFromServer()
                repository.syncProfessorsFromServer()
                delay(5000)
            }
        }
    }

    val filteredRoutines = remember(savedRoutines, searchQuery, userRole, userEmail, favoritesFilterActive, selectedCategory) {
        val base = if (userRole?.lowercase() == "professor" || userRole?.lowercase() == "admin") {
            savedRoutines.filter { !it.id.contains("::") }
        } else {
            val normalizedEmail = userEmail.lowercase().trim()
            savedRoutines.filter { it.creatorEmail.lowercase().trim() == normalizedEmail || it.assignedMemberEmails.any { e -> e.lowercase().trim() == normalizedEmail } }
        }

        val categoryFiltered = if (selectedCategory == "Todas") base
        else base.filter {
            val cat = it.category.lowercase().removeSuffix("s")
            val sel = selectedCategory.lowercase().removeSuffix("s")
            cat == sel
        }

        val searched = if (searchQuery.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        if (favoritesFilterActive) {
            searched.sortedWith(compareByDescending<Routine> { it.isFavorite }.thenByDescending { it.createdAt })
        } else {
            searched.sortedByDescending { it.createdAt }
        }
    }

    val isProfessor = userRole?.lowercase() == "professor" || userRole?.lowercase() == "admin"
    val totalCount = if (isProfessor) savedRoutines.count { !it.id.contains("::") } else filteredRoutines.size
    val completedCount = savedRoutines.count { it.status == "Completada" }
    val assignedCount = if (isProfessor) savedRoutines.count { it.type == "Asignada" && !it.id.contains("::") } else filteredRoutines.count { it.type == "Asignada" }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0B1218),
            dragHandle = { BottomSheetDefaults.DragHandle(color = accentColor) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Filtrar por Grupo Muscular",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                CategoryItem(
                    label = "Todas",
                    icon = Icons.Default.AllInclusive,
                    isSelected = selectedCategory == "Todas",
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth(0.9f),
                    onClick = { selectedCategory = "Todas"; showCategorySheet = false }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        "Pecho" to R.drawable.g_categoria_pecho,
                        "Espalda" to R.drawable.g_categoria_espalda,
                        "Fullbody" to R.drawable.g_categoria_cuerpocompleto
                    ).forEach { (label, icon) ->
                        CategoryItem(
                            label = label,
                            icon = icon,
                            isSelected = selectedCategory == label,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f).padding(4.dp),
                            onClick = { selectedCategory = label; showCategorySheet = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        "Piernas" to R.drawable.g_categoria_piernas,
                        "Brazos" to R.drawable.g_categoria_brazos,
                        "Abdomen" to R.drawable.g_categoria_abdominales // Actualizado de Hombros a Abdomen
                    ).forEach { (label, icon) ->
                        CategoryItem(
                            label = label,
                            icon = icon,
                            isSelected = selectedCategory == label,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f).padding(4.dp),
                            onClick = { selectedCategory = label; showCategorySheet = false }
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Rutinas",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
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
                    }
                )
            },
            bottomBar = {
                when (userRole?.lowercase()) {
                    "professor" -> ProfessorBottomNavigation("member_routines", onHomeClick, onStoreClick, onScheduleClick, onMembersClick, onRoutinesClick, onChatClick, accentColor)
                    else -> MemberBottomNavigation("member_routines", onHomeClick, onStoreClick, onClassesClick, onRoutinesClick, onChatClick, accentColor)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val statModifier = Modifier.weight(1f).height(90.dp)
                        StatBox(label = "Total", value = totalCount.toString(), icon = Icons.Outlined.Description, color = accentColor, accentColor = accentColor, shape = containerShape, isSelected = false, onClick = {}, modifier = statModifier)
                        StatBox(label = "Completadas", value = completedCount.toString(), icon = Icons.Default.CheckCircle, color = Color(0xFF4CAF50), accentColor = accentColor, shape = containerShape, isSelected = false, onClick = {}, modifier = statModifier)
                        StatBox(label = "Asignadas", value = assignedCount.toString(), icon = Icons.Default.Group, color = Color(0xFF2196F3), accentColor = accentColor, shape = containerShape, isSelected = false, onClick = {}, modifier = statModifier)
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        placeholder = { Text("Buscar rutinas...", color = TextGray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor) },
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
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    Text(text = "Acciones rápidas", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionItem(
                            label = "Nueva Rutina",
                            accentColor = accentColor,
                            shape = containerShape,
                            modifier = Modifier.weight(1f),
                            onClick = onNewRoutineClick
                        ) {
                            Box(modifier = Modifier.size(38.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = accentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp).align(Alignment.TopStart)
                                )
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0xFF00222E), CircleShape)
                                        .padding(1.dp)
                                )
                            }
                        }
                        QuickActionItem(
                            label = "Favoritas",
                            accentColor = accentColor,
                            shape = containerShape,
                            modifier = Modifier.weight(1f),
                            isSelected = favoritesFilterActive,
                            onClick = { favoritesFilterActive = !favoritesFilterActive }
                        ) {
                            Icon(
                                imageVector = if (favoritesFilterActive) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (favoritesFilterActive) Color(0xFFFFD700) else accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        QuickActionItem(
                            label = "Categorías",
                            accentColor = accentColor,
                            shape = containerShape,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedCategory != "Todas",
                            onClick = { showCategorySheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (selectedCategory != "Todas") accentColor else accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(text = "Mis rutinas", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(10.dp))
                }

                if (filteredRoutines.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron rutinas", color = TextGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    itemsIndexed(filteredRoutines, key = { _, routine -> routine.id }) { index, routine ->
                        val statusColor = when (routine.status) {
                            "Activa" -> Color(0xFF4CAF50)
                            "Completada" -> Color(0xFF4CAF50)
                            "Asignada" -> Color(0xFF2196F3)
                            "Borrador" -> Color(0xFFFFA000)
                            else -> TextGray
                        }

                        val tagColor = when (routine.type) {
                            "Personal" -> Color(0xFF673AB7)
                            "Asignada" -> Color(0xFF2E7D32)
                            else -> Color(0xFF424242)
                        }

                        ExpandableRoutineItem(
                            title = routine.name,
                            statusText = routine.status,
                            statusColor = statusColor,
                            creator = routine.creatorName,
                            exercises = routine.exercises.size.toString(),
                            duration = routine.duration.replace(" min", ""),
                            updated = "recientemente",
                            tagText = routine.type,
                            tagColor = tagColor,
                            isExpanded = expandedIndex == index,
                            isFavorite = routine.isFavorite,
                            accentColor = accentColor,
                            shape = containerShape,
                            days = routine.days,
                            showProfessorActions = isProfessor,
                            notes = routine.notes,
                            onNotesChange = { newNotes ->
                                coroutineScope.launch {
                                    dataStoreManager.updateRoutine(routine.copy(notes = newNotes, updatedAt = System.currentTimeMillis()))
                                }
                            },
                            onAssign = { routineToAssign = routine },
                            onDelete = { routineToDelete = routine },
                            onStart = { onStartRoutine(routine.id) },
                            onEdit = { onEditRoutine(routine.id) },
                            onToggleFavorite = {
                                coroutineScope.launch {
                                    dataStoreManager.updateRoutine(routine.copy(isFavorite = !routine.isFavorite))
                                }
                            },
                            onClick = { expandedIndex = if (expandedIndex == index) -1 else index }
                        )
                    }
                }
            }
        }

        if (routineToAssign != null) {
            AssignMemberDialog(
                routine = routineToAssign!!,
                members = members,
                accentColor = accentColor,
                shape = containerShape,
                onDismiss = { routineToAssign = null },
                onAssign = { selectedEmails ->
                    coroutineScope.launch {
                        val originalEmails = routineToAssign!!.assignedMemberEmails
                        val removedEmails = originalEmails.filter { it !in selectedEmails }

                        val rawProfId = dataStoreManager.getLinkedProfessorId().first()
                        val currentEmail = dataStoreManager.getUserEmail().first().lowercase().trim()
                        val profId = if (rawProfId.isNullOrBlank()) currentEmail else rawProfId.trim()

                        val updatedProfessorIds = if (routineToAssign!!.professorIds.contains(profId)) {
                            routineToAssign!!.professorIds
                        } else {
                            routineToAssign!!.professorIds + profId
                        }

                        val updatedRoutine = routineToAssign!!.copy(
                            assignedMemberEmails = selectedEmails.map { it.lowercase().trim() },
                            professorIds = updatedProfessorIds,
                            type = "Asignada"
                        )
                        dataStoreManager.updateRoutine(updatedRoutine)

                        val routineJson = Json.encodeToString(updatedRoutine)

                        removedEmails.forEach { email ->
                            repository.syncMemberRoutine(
                                professorId = profId,
                                memberEmail = email,
                                routineJson = routineJson,
                                routineId = updatedRoutine.id,
                                action = "delete_routine"
                            )
                        }

                        selectedEmails.forEach { email ->
                            repository.syncMemberRoutine(
                                professorId = profId,
                                memberEmail = email,
                                routineJson = routineJson,
                                routineId = updatedRoutine.id,
                                action = "add_routine"
                            )
                        }

                        routineToAssign = null
                    }
                }
            )
        }

        if (routineToDelete != null) {
            AlertDialog(
                onDismissRequest = { routineToDelete = null },
                title = { Text("Eliminar Rutina", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas eliminar la rutina \"${routineToDelete?.name}\"? Esta acción no se puede deshacer y se eliminará de la planilla.", color = TextWhite) },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val routine = routineToDelete!!
                            val rawProfId = dataStoreManager.getLinkedProfessorId().first()
                            val profId = if (rawProfId.isNullOrBlank()) userEmail.lowercase().trim() else rawProfId.trim()
                            val routineJson = Json.encodeToString(routine)

                            if (routine.assignedMemberEmails.isNotEmpty()) {
                                routine.assignedMemberEmails.forEach { email ->
                                    repository.syncMemberRoutine(
                                        professorId = profId,
                                        memberEmail = email,
                                        routineJson = routineJson,
                                        routineId = routine.id,
                                        action = "delete_routine"
                                    )
                                }
                            } else {
                                repository.syncMemberRoutine(
                                    professorId = profId,
                                    memberEmail = "",
                                    routineJson = routineJson,
                                    routineId = routine.id,
                                    action = "delete_routine"
                                )
                            }

                            dataStoreManager.deleteRoutine(routine.id)
                            routineToDelete = null
                        }
                    }) {
                        Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineToDelete = null }) {
                        Text("CANCELAR", color = TextGray)
                    }
                },
                containerColor = Color(0xFF00151C),
                shape = containerShape
            )
        }
    }
}

@Composable
fun CategoryItem(
    label: String,
    icon: Any,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF1A242D)
    val borderColor = if (isSelected) accentColor else Color.Transparent

    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        val iconPainter = when (icon) {
            is ImageVector -> rememberVectorPainter(icon)
            is Int -> painterResource(id = icon)
            else -> rememberVectorPainter(Icons.Default.Help)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = label,
                tint = if (isSelected) accentColor else Color.White,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = if (isSelected) accentColor else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AssignMemberDialog(
    routine: Routine,
    members: List<Member>,
    accentColor: Color,
    shape: Shape,
    onDismiss: () -> Unit,
    onAssign: (List<String>) -> Unit
) {
    var selectedEmails by remember { mutableStateOf(routine.assignedMemberEmails) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = if (searchQuery.isBlank()) members else members.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = shape,
            color = Color(0xFF00151C),
            modifier = Modifier.padding(16.dp).border(1.dp, accentColor.copy(alpha = 0.5f), shape)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Asignar Rutina - ${routine.name}",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar alumno...", color = TextGray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF081C24),
                        unfocusedContainerColor = Color(0xFF081C24),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (filteredMembers.isEmpty()) {
                        item {
                            Text("No se encontraron alumnos", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        items(filteredMembers) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEmails = if (selectedEmails.contains(member.email)) {
                                            selectedEmails.filter { it != member.email }
                                        } else {
                                            selectedEmails + member.email
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedEmails.contains(member.email),
                                    onCheckedChange = { checked ->
                                        selectedEmails = if (checked) {
                                            selectedEmails + member.email
                                        } else {
                                            selectedEmails.filter { it != member.email }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        uncheckedColor = TextGray,
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(member.fullName, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(member.email, color = TextGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = TextGray)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { onAssign(selectedEmails) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ASIGNAR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LazyItemScope.ExpandableRoutineItem(
    title: String,
    statusText: String,
    statusColor: Color,
    creator: String,
    exercises: String,
    duration: String,
    updated: String,
    tagText: String,
    tagColor: Color,
    isExpanded: Boolean,
    isFavorite: Boolean = false,
    accentColor: Color,
    shape: Shape,
    days: List<String> = emptyList(),
    showProfessorActions: Boolean = false,
    notes: String = "",
    onNotesChange: (String) -> Unit = {},
    onAssign: () -> Unit = {},
    onDelete: () -> Unit = {},
    onStart: () -> Unit = {},
    onEdit: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit
) {
    val titlePaddingTop by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 4.dp,
        label = "titlePadding"
    )

    Card(
        modifier = Modifier
            .animateItem(placementSpec = tween(1200, easing = FastOutSlowInEasing)) 
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .drawBehind {
                val cornerSize = if (isExpanded) size.height * 0.05f else size.height * 0.15f
                val path = Path().apply {
                    moveTo(0f, size.height - cornerSize)
                    lineTo(cornerSize, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = if (statusText == "Completada") Color(0xFF4CAF50) else accentColor)
            }
            .border(
                1.dp,
                if (statusText == "Completada") Color(0xFF4CAF50).copy(alpha = 0.6f)
                else if (isExpanded) accentColor.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.05f),
                shape
            )
            .clickable { onStart() }, // CORRECCIÓN: Al tocar la tarjeta se inicia la rutina
        colors = CardDefaults.cardColors(
            containerColor = if (statusText == "Completada") Color(0xFF1B252D) else Color.White.copy(alpha = 0.03f)
        ),
        shape = shape
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (isExpanded) 16.dp else 12.dp)
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onStart() },
                        shape = CircleShape,
                        color = accentColor,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Iniciar",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = titlePaddingTop)
                        )

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (statusText != "Completada") {
                                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(8.dp))
                                Surface(color = tagColor, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = tagText,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // FILA DE ACCIONES HORIZONTAL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // INDICADOR MOVIDO A LA FILA DE BOTONES
                            if (statusText == "Completada") {
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Finalizada",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            IconButton(onClick = { onToggleFavorite() }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Favorito",
                                    tint = if (isFavorite) Color(0xFFFFD700) else accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(onClick = { onAssign() }, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Asignar", tint = accentColor, modifier = Modifier.size(24.dp))
                            }

                            IconButton(onClick = { onEdit() }, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar", tint = accentColor, modifier = Modifier.size(24.dp))
                            }

                            IconButton(onClick = { onDelete() }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete, 
                                    contentDescription = "Eliminar", 
                                    tint = if (showProfessorActions) Color(0xFFF44336) else Color(0xFFD32F2F), 
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        if (days.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val allDays = listOf("LU", "MA", "MI", "JU", "VI", "SÁ", "DO")
                                allDays.forEach { day ->
                                    val isSelected = days.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            color = if (isSelected) Color.Black else TextWhite.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                        DetailRow(icon = Icons.Outlined.Person, text = creator, accentColor = accentColor)
                        DetailRow(icon = Icons.AutoMirrored.Outlined.Assignment, text = "$exercises ejercicios  •  $duration min", accentColor = accentColor)
                        DetailRow(icon = Icons.Outlined.CalendarToday, text = "Actualizada: recientemente", accentColor = accentColor)
                    }
                }

                // CUADRO DE ANOTACIONES PERSISTENTES
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = onNotesChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            placeholder = { Text("Anotaciones generales de la rutina...", color = TextGray, fontSize = 13.sp) },
                            textStyle = LocalTextStyle.current.copy(color = TextWhite, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                // Espacio extra
                if (isExpanded) Spacer(Modifier.height(8.dp))
            }

            // Zona interactiva para el botón de la esquina
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(40.dp)
                    .clickable { onClick() }
            )
        }
    }
}


@Composable
fun DetailRow(icon: ImageVector, text: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = text, color = TextGray, fontSize = 13.sp)
    }
}

@Composable
fun QuickActionItem(
    label: String,
    accentColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .height(100.dp)
            .clip(shape)
            .background(if (isSelected) accentColor.copy(alpha = 0.1f) else Color(0xFF00222E))
            .border(1.2.dp, if (isSelected) accentColor else accentColor.copy(alpha = 0.2f), shape)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isSelected) accentColor else TextWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}