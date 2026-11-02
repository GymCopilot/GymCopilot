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

    if (showCreationScreen) {
        RoutineCreationSharedScreen(
            onBack = { showCreationScreen = false },
            apiClient = apiClient
        )
        return
    }

    val mockRoutines = remember {
        listOf(
            Routine(
                id = "rut_1",
                name = "Rutina Hipertrofia A - Pecho y Tríceps",
                description = "Enfoque en desarrollo pectoral y fuerza de empuje ($gymName).",
                objective = "Hipertrofia",
                level = "Intermedio",
                duration = "60 min",
                category = "Hipertrofia",
                isFavorite = true,
                exercises = listOf(
                    Exercise("ex_1", "Press Inclinado con Mancuernas", "4", "10 - 12", "22 kg", "90 seg"),
                    Exercise("ex_2", "Press Plano con Barra", "4", "8 - 10", "65 kg", "120 seg"),
                    Exercise("ex_3", "Aperturas en Polea Baja", "3", "12", "15 kg", "60 seg"),
                    Exercise("ex_4", "Fondos en Paralelas", "3", "10", "Peso Corporal", "90 seg"),
                    Exercise("ex_5", "Extensiones de Tríceps en Polea", "4", "12", "25 kg", "60 seg")
                )
            ),
            Routine(
                id = "rut_2",
                name = "Rutina Pierna Completa - Cuádriceps y Glúteos",
                description = "Trabajo pesado de tren inferior y estabilidad para $userEmail.",
                objective = "Fuerza y Volumen",
                level = "Avanzado",
                duration = "75 min",
                category = "Fuerza",
                isFavorite = false,
                exercises = listOf(
                    Exercise("ex_6", "Sentadilla Libre con Barra", "4", "8 - 10", "80 kg", "120 seg"),
                    Exercise("ex_7", "Prensa 45°", "4", "10 - 12", "140 kg", "90 seg"),
                    Exercise("ex_8", "Extensiones de Cuádriceps", "3", "15", "50 kg", "60 seg"),
                    Exercise("ex_9", "Curl Femoral Tumbado", "3", "12", "40 kg", "60 seg")
                )
            ),
            Routine(
                id = "rut_3",
                name = "Rutina Funcional & Core",
                description = "Resistencia cardiovascular y fortalecimiento abdominal con ${if (apiClient != null) "KtorApiClient" else "Offline Mode"}.",
                objective = "Quema de Grasa",
                level = "Principiante",
                duration = "45 min",
                category = "Cardio",
                isFavorite = true,
                exercises = listOf(
                    Exercise("ex_10", "Plancha Abdominal", "4", "45 seg", "ISO", "45 seg"),
                    Exercise("ex_11", "Burpees", "3", "12", "Libre", "60 seg"),
                    Exercise("ex_12", "Escaladores (Mountain Climbers)", "3", "30 reps", "Libre", "45 seg")
                )
            )
        )
    }

    val categories = listOf("Todas", "Hipertrofia", "Fuerza", "Cardio", "Movilidad")

    val filteredRoutines = remember(mockRoutines, selectedCategory, searchQuery, favoritesFilterActive) {
        var list = mockRoutines
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

    val totalCount = mockRoutines.size
    val completedCount = mockRoutines.count { it.status == "Completada" }
    val assignedCount = mockRoutines.size

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
                        RoutineSharedCard(routine = routine) {
                            selectedRoutine = routine
                        }
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
fun RoutineSharedCard(routine: Routine, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryTurquoise.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = routine.category,
                        color = PrimaryTurquoise,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                    Text(routine.duration, fontSize = 11.sp, color = TextGray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = routine.name,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = routine.description,
                color = TextGray,
                fontSize = 12.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💪 ${routine.exercises.size} Ejercicios",
                    color = PrimaryTurquoise,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("VER RUTINA", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
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
                text = "Detalle de Rutina",
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
                Icon(Icons.Default.Timer, contentDescription = "Timer", tint = PrimaryTurquoise)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(routine.name, color = PrimaryTurquoise, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                Text(routine.description, color = TextWhite, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("⏱️ ${routine.duration}", color = TextGray, fontSize = 12.sp)
                    Text("🎯 ${routine.objective}", color = TextGray, fontSize = 12.sp)
                    Text("📊 ${routine.level}", color = TextGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ejercicios (${routine.exercises.size})",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(routine.exercises) { exercise ->
                val isDone = completedExercises.contains(exercise.id)
                ExerciseItemCard(
                    exercise = exercise,
                    isCompleted = isDone,
                    onToggle = {
                        completedExercises = if (isDone) {
                            completedExercises - exercise.id
                        } else {
                            completedExercises + exercise.id
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseItemCard(
    exercise: Exercise,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isCompleted) Color(0xFF00C853).copy(alpha = 0.4f) else PrimaryTurquoise.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00C853), uncheckedColor = TextGray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = exercise.name,
                        color = if (isCompleted) TextGray else TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Series: ${exercise.series} | Reps: ${exercise.reps} | Descanso: ${exercise.rest}",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SharedTimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer de Descanso", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(text = "00:90", fontSize = 48.sp, fontWeight = FontWeight.Black, color = PrimaryTurquoise)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Descanso entre series activo", color = TextGray, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)
            ) {
                Text("Cerrar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF001F29),
        shape = RoundedCornerShape(20.dp)
    )
}
