package com.zexo.gymcopilot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.Exercise
import com.zexo.gymcopilot.model.Routine
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.getCurrentTimeMillis
import com.zexo.gymcopilot.utils.playAudioSound
import com.zexo.gymcopilot.utils.readLocalStorageFlag
import com.zexo.gymcopilot.utils.saveLocalStorageFlag
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

@Composable
fun FullRoutinesSection(
    apiClient: GymKtorApiClient? = null,
    gymName: String = "Gym 26",
    userEmail: String = ""
) {
    val _unused = listOf(gymName, userEmail)
    var selectedCategory by remember { mutableStateOf("Todas") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoutine by remember { mutableStateOf<Routine?>(null) }
    var editingRoutineId by remember { mutableStateOf<String?>(null) }
    var showTimerModal by remember { mutableStateOf(false) }
    var favoritesFilterActive by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCreationScreen by remember { mutableStateOf(false) }

    val userRoutines = remember { mutableStateListOf<Routine>() }

    LaunchedEffect(Unit) {
        try {
            val savedJson = readLocalStorageFlag("gymcopilot_user_routines")
            if (!savedJson.isNullOrBlank()) {
                val parsed = Json.decodeFromString<List<Routine>>(savedJson)
                userRoutines.clear()
                userRoutines.addAll(parsed)
            }
        } catch (e: Exception) {}
    }

    fun persistRoutines() {
        try {
            val jsonStr = Json.encodeToString(userRoutines.toList())
            saveLocalStorageFlag("gymcopilot_user_routines", jsonStr)
        } catch (e: Exception) {}
    }

    if (showCreationScreen || editingRoutineId != null) {
        RoutineCreationSharedScreen(
            routineId = editingRoutineId,
            onBack = {
                showCreationScreen = false
                editingRoutineId = null
            },
            onSaveRoutine = { newOrUpdated ->
                val idx = userRoutines.indexOfFirst { it.id == newOrUpdated.id }
                if (idx != -1) {
                    userRoutines[idx] = newOrUpdated
                } else {
                    userRoutines.add(newOrUpdated)
                }
                persistRoutines()
                showCreationScreen = false
                editingRoutineId = null
            },
            apiClient = apiClient
        )
        return
    }

    val categories = listOf("Todas", "Hipertrofia", "Fuerza", "Cardio", "Movilidad")

    val filteredRoutines = remember(userRoutines, selectedCategory, searchQuery, favoritesFilterActive) {
        var list: List<Routine> = userRoutines
        if (selectedCategory != "Todas") {
            list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        if (favoritesFilterActive) {
            list = list.filter { it.isFavorite }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
        }
        list
    }

    val totalCount = userRoutines.size
    val completedCount = userRoutines.count { it.status == "Completada" }
    val assignedCount = userRoutines.size

    if (selectedRoutine != null) {
        RoutineDetailSharedView(
            routine = selectedRoutine!!,
            onBack = { selectedRoutine = null },
            onStartTimer = { showTimerModal = true },
            onUpdateRoutine = { updated ->
                val idx = userRoutines.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    userRoutines[idx] = updated
                    persistRoutines()
                }
            },
            onEdit = { r ->
                editingRoutineId = r.id
                selectedRoutine = null
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rutinas",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite
                    )
                    IconButton(
                        onClick = { showTimerModal = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryTurquoise.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Timer", tint = PrimaryTurquoise)
                    }
                }
            }

            // Top 3 Stat Cards (Height 120.dp)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val statModifier = Modifier.weight(1f).height(120.dp)
                    StatBoxShared(label = "Total", value = totalCount.toString(), icon = Icons.Outlined.Description, color = PrimaryTurquoise, modifier = statModifier)
                    StatBoxShared(label = "Completadas", value = completedCount.toString(), icon = Icons.Default.CheckCircle, color = Color(0xFF4CAF50), modifier = statModifier)
                    StatBoxShared(label = "Asignadas", value = assignedCount.toString(), icon = Icons.Default.Group, color = Color(0xFF2196F3), modifier = statModifier)
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar rutinas...", color = TextGray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryTurquoise) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF00222E),
                        unfocusedContainerColor = Color(0xFF00222E),
                        focusedBorderColor = PrimaryTurquoise,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
            }

            // Quick Actions Section ("Nueva Rutina", "Favoritas", "Categorías")
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(text = "Acciones rápidas", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionSharedItem(
                            label = "Nueva Rutina",
                            accentColor = PrimaryTurquoise,
                            modifier = Modifier.weight(1f),
                            onClick = { showCreationScreen = true }
                        ) {
                            Icon(painter = painterResource(Res.drawable.assignment_add), contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(28.dp))
                        }
                        QuickActionSharedItem(
                            label = "Favoritas",
                            accentColor = PrimaryTurquoise,
                            modifier = Modifier.weight(1f),
                            isSelected = favoritesFilterActive,
                            onClick = { favoritesFilterActive = !favoritesFilterActive }
                        ) {
                            Icon(
                                imageVector = if (favoritesFilterActive) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (favoritesFilterActive) Color(0xFFFFD700) else PrimaryTurquoise,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        QuickActionSharedItem(
                            label = "Categorías",
                            accentColor = PrimaryTurquoise,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedCategory != "Todas",
                            onClick = { showCategoryDialog = true }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Mis Rutinas section header
            item {
                Text(
                    text = "Mis rutinas",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Routine Cards List
            if (filteredRoutines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No se encontraron rutinas", color = TextGray, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(filteredRoutines) { routine ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        RoutineSharedCard(
                            routine = routine,
                            onClick = { selectedRoutine = routine },
                            onDelete = {
                                userRoutines.remove(routine)
                                persistRoutines()
                            },
                            onToggleFavorite = {
                                val idx = userRoutines.indexOf(routine)
                                if (idx != -1) {
                                    userRoutines[idx] = routine.copy(isFavorite = !routine.isFavorite)
                                    persistRoutines()
                                }
                            },
                            onNotesChange = { newNotes ->
                                val idx = userRoutines.indexOf(routine)
                                if (idx != -1) {
                                    userRoutines[idx] = routine.copy(notes = newNotes, updatedAt = getCurrentTimeMillis())
                                    persistRoutines()
                                }
                            },
                            onEdit = {
                                editingRoutineId = routine.id
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTimerModal) {
        SharedTimerDialog(onDismiss = { showTimerModal = false })
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Filtrar por Categoría", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        TextButton(
                            onClick = {
                                selectedCategory = cat
                                showCategoryDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cat, color = if (selectedCategory == cat) PrimaryTurquoise else TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cerrar", color = PrimaryTurquoise)
                }
            },
            containerColor = Color(0xFF001F29),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun StatBoxShared(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickActionSharedItem(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else SurfaceColor)
            .border(1.2.dp, if (isSelected) accentColor else accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RoutineSharedCard(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNotesChange: (String) -> Unit,
    onEdit: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var notesText by remember(routine.notes) { mutableStateOf(routine.notes) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Rutina", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar esta rutina? Esta acción no se puede deshacer.", color = TextWhite) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = PrimaryTurquoise)
                }
            },
            containerColor = Color(0xFF001F29),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = if (isExpanded) 0.dp else 16.dp
        ),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isExpanded) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(16.dp.toPx(), size.height)
                        lineTo(0f, size.height - 16.dp.toPx())
                        close()
                    }
                    drawPath(path, color = PrimaryTurquoise)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryTurquoise, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = routine.name,
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (routine.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Favorita",
                        tint = if (routine.isFavorite) Color(0xFFFFD700) else TextGray,
                        modifier = Modifier.size(20.dp).clickable { onToggleFavorite() }
                    )
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = "Asignar",
                        tint = PrimaryTurquoise,
                        modifier = Modifier.size(20.dp).clickable { /* assign */ }
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = PrimaryTurquoise,
                        modifier = Modifier.size(20.dp).clickable { onEdit() }
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp).clickable { showDeleteConfirm = true }
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${routine.exercises.size} ejercicios • ${routine.duration}",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Actualizada: recientemente",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = {
                        notesText = it
                        onNotesChange(it)
                    },
                    placeholder = { Text("Anotaciones generales de la rutina...", color = TextGray, fontSize = 12.sp) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F151B),
                        unfocusedContainerColor = Color(0xFF0F151B),
                        focusedBorderColor = PrimaryTurquoise,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RoutineDetailSharedView(
    routine: Routine,
    onBack: () -> Unit,
    onStartTimer: () -> Unit,
    onUpdateRoutine: (Routine) -> Unit = {},
    onEdit: (Routine) -> Unit = {}
) {
    val _unusedTimer = onStartTimer
    var completedExercises by remember { mutableStateOf(setOf<String>()) }

    // Cronómetros
    var partialSeconds by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var isStopwatchRunning by remember { mutableStateOf(false) }

    // Cuenta regresiva inicial de 5s al pulsar Play
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountdownActive by remember { mutableStateOf(false) }

    // Descanso
    var restSeconds by remember { mutableIntStateOf(0) }
    var totalRestSeconds by remember { mutableIntStateOf(0) }
    var isRestRunning by remember { mutableStateOf(false) }

    // Ejercicio activo y series
    var activeExerciseId by remember { mutableStateOf<String?>(routine.exercises.firstOrNull()?.id) }
    var exerciseSeriesMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var isMuted by remember { mutableStateOf(false) }

    // Cuenta regresiva effect con sonidos cuarto-tres-dos y uno
    LaunchedEffect(isCountdownActive, countdownSeconds) {
        if (isCountdownActive && countdownSeconds > 0) {
            if (!isMuted) {
                if (countdownSeconds in 2..5) {
                    playAudioSound("fourthreetwo")
                } else if (countdownSeconds == 1) {
                    playAudioSound("one")
                }
            }
            delay(1000)
            countdownSeconds--
            if (countdownSeconds == 0) {
                isCountdownActive = false
                isStopwatchRunning = true
            }
        }
    }

    // Lógica del temporizador
    LaunchedEffect(isStopwatchRunning, isRestRunning) {
        while (isActive) {
            if (isStopwatchRunning || isRestRunning) {
                delay(1000)
                if (isStopwatchRunning) partialSeconds++
                if (isRestRunning && restSeconds > 0) {
                    restSeconds--
                    if (restSeconds == 0) {
                        isRestRunning = false
                        if (!isMuted) playAudioSound("descanso")
                    }
                }
                totalSeconds++
            } else {
                delay(500)
            }
        }
    }

    val currentSeries = activeExerciseId?.let { exerciseSeriesMap[it] } ?: 0
    val progress = if (routine.exercises.isEmpty()) 0f else completedExercises.size.toFloat() / routine.exercises.size

    // Duración estimada calculada sumando series, tiempos y descansos
    val estimatedTotalSeconds = routine.exercises.sumOf { ex ->
        val series = ex.series.toIntOrNull() ?: 4
        val timePerSerie = if ((ex.time.toIntOrNull() ?: 0) > 0) ex.time.toInt() else 30
        val restPerSerie = ex.rest.toIntOrNull() ?: 60
        (series * timePerSerie) + ((if (series > 1) series - 1 else 0) * restPerSerie)
    }
    val estimatedDurationStr = formatTimer(estimatedTotalSeconds)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Cabecera fija
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceColor, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextWhite)
                }
                Text(
                    text = routine.name,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onEdit(routine) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryTurquoise.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timer header card sin color de fondo (transparente) y contador más fino y grande (76.sp, FontWeight.Thin)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Izquierda: Número de serie y botón Reset
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = PrimaryTurquoise.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, PrimaryTurquoise)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = currentSeries.toString(),
                                            color = PrimaryTurquoise,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "SERIE",
                                            color = TextGray,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                IconButton(
                                    onClick = {
                                        partialSeconds = 0
                                        totalSeconds = 0
                                        isStopwatchRunning = false
                                        isCountdownActive = false
                                        isRestRunning = false
                                        restSeconds = 0
                                        exerciseSeriesMap = emptyMap()
                                        completedExercises = emptySet()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF252D36), CircleShape)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = PrimaryTurquoise, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Centro: Tiempo parcial y total (más finos, FontWeight.Thin, fontSize 76.sp)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Timer, null, tint = PrimaryTurquoise, modifier = Modifier.size(14.dp))
                                        Text("$estimatedDurationStr Duración", color = TextGray, fontSize = 10.sp)
                                    }
                                    Text("Total: ${formatTimer(totalSeconds)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.FitnessCenter, null, tint = PrimaryTurquoise, modifier = Modifier.size(14.dp))
                                        Text("${routine.exercises.size} Ejercicios", color = TextGray, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("TIEMPO PARCIAL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isCountdownActive) countdownSeconds.toString() else formatTimer(partialSeconds),
                                    color = if (isCountdownActive) Color(0xFFFF5252) else TextWhite,
                                    fontSize = 76.sp,
                                    fontWeight = FontWeight.Thin,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Derecha: Barra de progreso vertical en porcentaje (se llena de abajo hacia arriba por cada ejercicio completado)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(85.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0F151B))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(progress.coerceIn(0f, 1f))
                                            .align(Alignment.BottomCenter)
                                            .background(PrimaryTurquoise)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = PrimaryTurquoise,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Tiempo de descanso debajo del contador con indicador inferior que se llena conforme se cumple
                        if (isRestRunning && totalRestSeconds > 0) {
                            Spacer(Modifier.height(12.dp))
                            val restProgress = (totalRestSeconds - restSeconds).toFloat() / totalRestSeconds.toFloat()
                            Surface(
                                color = PrimaryTurquoise.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PrimaryTurquoise),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Timer, null, tint = PrimaryTurquoise, modifier = Modifier.size(18.dp))
                                            Text("DESCANSO", color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text(formatTimer(restSeconds), color = PrimaryTurquoise, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                        IconButton(onClick = { isRestRunning = false; restSeconds = 0 }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    // Indicador inferior que se llena conforme se cumple el tiempo de descanso
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(restProgress.coerceIn(0f, 1f))
                                            .height(3.dp)
                                            .align(Alignment.BottomStart)
                                            .background(PrimaryTurquoise)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de ejercicios deslizándose por debajo de la cabecera fija
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(routine.exercises) { ex ->
                        val isActive = activeExerciseId == ex.id
                        val isCompleted = completedExercises.contains(ex.id)
                        val sCount = exerciseSeriesMap[ex.id] ?: 0

                        ExerciseExecutionCardShared(
                            exercise = ex,
                            isActive = isActive,
                            isRunning = isActive && isStopwatchRunning,
                            isResting = isActive && isRestRunning,
                            isCompleted = isCompleted,
                            seriesCount = sCount,
                            isMuted = isMuted,
                            onToggleMute = { isMuted = !isMuted },
                            onSelect = { activeExerciseId = ex.id },
                            onPlayPause = {
                                activeExerciseId = ex.id
                                if (isActive && isStopwatchRunning) {
                                    isStopwatchRunning = false
                                } else {
                                    val newSeries = sCount + 1
                                    exerciseSeriesMap = exerciseSeriesMap + (ex.id to newSeries)
                                    partialSeconds = 0
                                    countdownSeconds = 5
                                    isCountdownActive = true
                                    isStopwatchRunning = false
                                    isRestRunning = false
                                }
                            },
                            onStartRest = {
                                activeExerciseId = ex.id
                                val restTime = ex.rest.toIntOrNull() ?: 60
                                restSeconds = restTime
                                totalRestSeconds = restTime
                                isRestRunning = true
                                isStopwatchRunning = false
                                isCountdownActive = false
                            },
                            onToggleComplete = {
                                completedExercises = if (isCompleted) completedExercises - ex.id else completedExercises + ex.id
                                val updatedRoutine = routine.copy(
                                    exercises = routine.exercises.map { if (it.id == ex.id) it else it },
                                    updatedAt = getCurrentTimeMillis()
                                )
                                onUpdateRoutine(updatedRoutine)
                            },
                            onNotesChange = { newExNotes ->
                                val updatedExs = routine.exercises.map { if (it.id == ex.id) it.copy(notes = newExNotes) else it }
                                val updatedRoutine = routine.copy(exercises = updatedExs, updatedAt = getCurrentTimeMillis())
                                onUpdateRoutine(updatedRoutine)
                            }
                        )
                    }
                }

                // Floating finish session button
                FloatingActionButton(
                    onClick = {
                        val updatedRoutine = routine.copy(
                            status = "Completada",
                            updatedAt = getCurrentTimeMillis()
                        )
                        onUpdateRoutine(updatedRoutine)
                        onBack()
                    },
                    containerColor = PrimaryTurquoise,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(60.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Finalizar", modifier = Modifier.size(28.dp), tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ExerciseExecutionCardShared(
    exercise: Exercise,
    isActive: Boolean,
    isRunning: Boolean,
    isResting: Boolean,
    isCompleted: Boolean,
    seriesCount: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSelect: () -> Unit,
    onPlayPause: () -> Unit,
    onStartRest: () -> Unit,
    onToggleComplete: () -> Unit,
    onNotesChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var notesText by remember(exercise.notes) { mutableStateOf(exercise.notes) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF4CAF50).copy(alpha = 0.15f) else SurfaceColor
        ),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = if (isExpanded) 0.dp else 16.dp
        ),
        border = BorderStroke(
            1.5.dp,
            if (isActive) Color(0xFF4CAF50) else PrimaryTurquoise.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isExpanded) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(16.dp.toPx(), size.height)
                        lineTo(0f, size.height - 16.dp.toPx())
                        close()
                    }
                    drawPath(path, color = if (isActive) Color(0xFF4CAF50) else PrimaryTurquoise)
                }
            }
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Icono del ejercicio 100% más grande (caja de 90.dp con imagen de 70.dp)
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F151B))
                            .border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(getExerciseIconShared(exercise.name)),
                            contentDescription = exercise.name,
                            modifier = Modifier.size(70.dp),
                            colorFilter = ColorFilter.tint(PrimaryTurquoise)
                        )
                    }
                    Column {
                        Text(
                            text = exercise.name,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (seriesCount > 0) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Series realizadas: $seriesCount",
                                color = PrimaryTurquoise,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isCompleted) PrimaryTurquoise else Color(0xFF252D36), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (isCompleted) Color.Black else PrimaryTurquoise,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botones Play, Descanso y Audio por ejercicio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isRunning) PrimaryTurquoise else Color(0xFF252D36), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isRunning) Color.Black else TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onStartRest,
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isResting) PrimaryTurquoise else Color(0xFF252D36), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Descanso",
                        tint = if (isResting) Color.Black else TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF252D36), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Audio",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterColShared("SERIES", exercise.series)
                ParameterColShared("REPS", exercise.reps)
                ParameterColShared("PESO", exercise.weight)
                ParameterColShared("DESCANSO", exercise.rest)
                ParameterColShared("TIEMPO", exercise.time.ifBlank { "0s" })
            }

            // Botón en el extremo inferior izquierdo con forma de triángulo para desplegar notas suavemente
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { isExpanded = !isExpanded }
                        .drawBehind {
                            val path = Path().apply {
                                moveTo(0f, size.height)
                                lineTo(size.width, size.height)
                                lineTo(0f, 0f)
                                close()
                            }
                            drawPath(path, color = if (isActive) Color(0xFF4CAF50) else PrimaryTurquoise)
                        }
                )

                Text(
                    text = if (isExpanded) "Ocultar notas" else "Anotaciones",
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = {
                            notesText = it
                            onNotesChange(it)
                        },
                        placeholder = { Text("Anotaciones sobre el ejercicio...", color = TextGray, fontSize = 12.sp) },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F151B),
                            unfocusedContainerColor = Color(0xFF0F151B),
                            focusedBorderColor = PrimaryTurquoise,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ParameterColShared(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = PrimaryTurquoise, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SharedTimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temporizador", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("00:00", color = PrimaryTurquoise, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Text("Cronómetro de descanso activo", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = PrimaryTurquoise, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF001F29),
        shape = RoundedCornerShape(20.dp)
    )
}

fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
