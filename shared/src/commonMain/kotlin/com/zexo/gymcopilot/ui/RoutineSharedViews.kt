package com.zexo.gymcopilot.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.Exercise
import com.zexo.gymcopilot.model.Routine
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun FullRoutinesSection(
    apiClient: GymKtorApiClient? = null,
    gymName: String = "Gym 26",
    userEmail: String = ""
) {
    var selectedCategory by remember { mutableStateOf("Todas") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoutine by remember { mutableStateOf<Routine?>(null) }
    var showTimerModal by remember { mutableStateOf(false) }
    var favoritesFilterActive by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCreationScreen by remember { mutableStateOf(false) }

    val userRoutines = remember { mutableStateListOf<Routine>() }

    if (showCreationScreen) {
        RoutineCreationSharedScreen(
            onBack = { showCreationScreen = false },
            onSaveRoutine = { newRoutine ->
                userRoutines.add(newRoutine)
                showCreationScreen = false
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
            onStartTimer = { showTimerModal = true }
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
                            onDelete = { userRoutines.remove(routine) },
                            onToggleFavorite = {
                                val idx = userRoutines.indexOf(routine)
                                if (idx != -1) {
                                    userRoutines[idx] = routine.copy(isFavorite = !routine.isFavorite)
                                }
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
    onToggleFavorite: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Text(
                        text = routine.name,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${routine.exercises.size} Ejercicios • ${routine.duration}",
                        color = TextGray,
                        fontSize = 11.sp
                    )
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
                    modifier = Modifier.size(20.dp).clickable { onClick() }
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp).clickable { onDelete() }
                )
            }
        }
    }
}

@Composable
fun RoutineDetailSharedView(
    routine: Routine,
    onBack: () -> Unit,
    onStartTimer: () -> Unit
) {
    var completedExercises by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                onClick = onStartTimer,
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryTurquoise.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryTurquoise)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer header card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Timer, null, tint = PrimaryTurquoise, modifier = Modifier.size(16.dp))
                        Text("${routine.duration} Duración Estimada", color = TextGray, fontSize = 11.sp)
                    }
                    Text("Total: 00:00", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.FitnessCenter, null, tint = PrimaryTurquoise, modifier = Modifier.size(16.dp))
                        Text("${routine.exercises.size} Ejercicios", color = TextGray, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("TIEMPO PARCIAL", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("00:00", color = TextWhite, fontSize = 52.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routine.exercises) { ex ->
                val isCompleted = completedExercises.contains(ex.id)
                ExerciseDetailCardShared(
                    exercise = ex,
                    isCompleted = isCompleted,
                    onToggle = {
                        completedExercises = if (isCompleted) completedExercises - ex.id else completedExercises + ex.id
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseDetailCardShared(
    exercise: Exercise,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(65.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F151B))
                            .border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(getExerciseIconShared(exercise.name)),
                            contentDescription = exercise.name,
                            modifier = Modifier.size(45.dp),
                            colorFilter = ColorFilter.tint(PrimaryTurquoise)
                        )
                    }
                    Text(
                        text = exercise.name,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onToggle,
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
