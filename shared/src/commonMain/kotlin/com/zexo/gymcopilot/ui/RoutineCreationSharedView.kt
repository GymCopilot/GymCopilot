package com.zexo.gymcopilot.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.GymKtorApiClient

data class ExerciseEntryShared(
    val id: String = "ex_${kotlin.random.Random.nextLong()}",
    var name: String,
    var series: String,
    var reps: String,
    var weight: String,
    var rest: String,
    var category: String = "PECHO",
    var time: String = "0",
    var notes: String = "",
    var isLocked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCreationSharedScreen(
    routineId: String? = null,
    onBack: () -> Unit,
    apiClient: GymKtorApiClient? = null
) {
    val apiClientRef = apiClient
    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }
    var selectedObjective by remember { mutableStateOf("Hipertrofia") }
    var selectedLevel by remember { mutableStateOf("Intermedio") }
    val selectedDuration by remember { mutableStateOf("60 min") }
    var selectedCategory by remember { mutableStateOf("FullBody") }

    val selectedDays = remember { mutableStateListOf<String>("LU", "MI", "VI") }
    var routineType by remember { mutableStateOf("Personal") }
    var routineStatus by remember { mutableStateOf("Activa") }

    val exercises = remember {
        mutableStateListOf(
            ExerciseEntryShared(name = "Press Inclinado con Mancuernas", series = "4", reps = "10 - 12", weight = "22 kg", rest = "90 seg", category = "PECHO")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
            .padding(vertical = 12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
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
                text = if (routineId == null) "Constructor de Rutinas" else "Editar Rutina",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    if (routineName.isNotBlank() && apiClientRef != null) {
                        onBack()
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryTurquoise.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = PrimaryTurquoise)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Información General
            item {
                RoutineSectionHeader("1. Información General")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = routineName,
                            onValueChange = { routineName = it },
                            placeholder = { Text("Nombre de la rutina *", color = TextGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F151B),
                                unfocusedContainerColor = Color(0xFF0F151B),
                                focusedBorderColor = PrimaryTurquoise,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = routineDescription,
                            onValueChange = { routineDescription = it },
                            placeholder = { Text("Descripción", color = TextGray) },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F151B),
                                unfocusedContainerColor = Color(0xFF0F151B),
                                focusedBorderColor = PrimaryTurquoise,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Grupo Muscular", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val muscleGroups = listOf("Pecho", "Espalda", "FullBody", "Pierna", "Brazo", "Abdomen")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            muscleGroups.take(3).forEach { group ->
                                FilterChip(
                                    selected = selectedCategory == group,
                                    onClick = { selectedCategory = group },
                                    label = { Text(group, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTurquoise,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0F151B),
                                        labelColor = TextWhite
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            muscleGroups.drop(3).forEach { group ->
                                FilterChip(
                                    selected = selectedCategory == group,
                                    onClick = { selectedCategory = group },
                                    label = { Text(group, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTurquoise,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0F151B),
                                        labelColor = TextWhite
                                    )
                                )
                            }
                        }

                        Text("Objetivo", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val objectives = listOf("Hipertrofia", "Fuerza", "Resistencia", "Definición", "Cardio", "Rehabilitación")
                        objectives.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { obj ->
                                    FilterChip(
                                        selected = selectedObjective == obj,
                                        onClick = { selectedObjective = obj },
                                        label = { Text(obj, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryTurquoise,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF0F151B),
                                            labelColor = TextWhite
                                        )
                                    )
                                }
                            }
                        }

                        Text("Nivel", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val levels = listOf("Principiante", "Intermedio", "Avanzado")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            levels.forEach { lvl ->
                                FilterChip(
                                    selected = selectedLevel == lvl,
                                    onClick = { selectedLevel = lvl },
                                    label = { Text(lvl, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTurquoise,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0F151B),
                                        labelColor = TextWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Configuración
            item {
                RoutineSectionHeader("2. Configuración")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Días recomendados", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val days = listOf("LU", "MA", "MI", "JU", "VI", "SÁ", "DO")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            days.forEach { day ->
                                val isSelected = selectedDays.contains(day)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                                    },
                                    label = { Text(day, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTurquoise,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0F151B),
                                        labelColor = TextWhite
                                    )
                                )
                            }
                        }

                        Text("Tipo de rutina", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Personal", "Asignable").forEach { type ->
                                FilterChip(
                                    selected = routineType == type,
                                    onClick = { routineType = type },
                                    label = { Text(type) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTurquoise,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0F151B),
                                        labelColor = TextWhite
                                    )
                                )
                            }
                        }

                        Text("Estado", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Borrador", "Activa", "Archivada").forEach { st ->
                                FilterChip(
                                    selected = routineStatus == st,
                                    onClick = { routineStatus = st },
                                    label = { Text(st) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTurquoise,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0F151B),
                                        labelColor = TextWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Profesores responsables
            item {
                RoutineSectionHeader("3. Profesores responsables")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profesor asignado principal (Tú)", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Se asignará automáticamente al creador de la rutina.", color = TextGray, fontSize = 12.sp)
                    }
                }
            }

            // 4. Constructor de ejercicios
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoutineSectionHeader("4. Constructor de ejercicios")
                    IconButton(
                        onClick = {
                            exercises.add(ExerciseEntryShared(name = "Nuevo Ejercicio", series = "4", reps = "12", weight = "0 kg", rest = "60 seg"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryTurquoise, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir Ejercicio", tint = Color.Black)
                    }
                }
            }

            itemsIndexed(exercises) { index, exercise ->
                RoutineExerciseItemCard(
                    exercise = exercise,
                    onUpdate = { updated -> exercises[index] = updated },
                    onDelete = { exercises.removeAt(index) },
                    onMoveUp = { if (index > 0) { val e = exercises.removeAt(index); exercises.add(index - 1, e) } },
                    onMoveDown = { if (index < exercises.size - 1) { val e = exercises.removeAt(index); exercises.add(index + 1, e) } }
                )
            }

            // 5. Resumen final
            item {
                RoutineSectionHeader("5. Resumen final")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen automático", color = PrimaryTurquoise, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ejercicios", color = TextGray)
                            Text("${exercises.size}", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Series totales", color = TextGray)
                            Text("${exercises.sumOf { it.series.toIntOrNull() ?: 4 }}", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Duración estimada", color = TextGray)
                            Text(selectedDuration, color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estado", color = TextGray)
                            Text(routineStatus, color = PrimaryTurquoise, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineSectionHeader(title: String) {
    Text(
        text = title,
        color = PrimaryTurquoise,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun RoutineExerciseItemCard(
    exercise: ExerciseEntryShared,
    onUpdate: (ExerciseEntryShared) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = exercise.name,
                    onValueChange = { onUpdate(exercise.copy(name = it)) },
                    placeholder = { Text("Nombre del ejercicio", color = TextGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F151B),
                        unfocusedContainerColor = Color(0xFF0F151B),
                        focusedBorderColor = PrimaryTurquoise,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = exercise.series,
                    onValueChange = { onUpdate(exercise.copy(series = it)) },
                    label = { Text("Series", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF0F151B), unfocusedContainerColor = Color(0xFF0F151B), focusedBorderColor = PrimaryTurquoise, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
                OutlinedTextField(
                    value = exercise.reps,
                    onValueChange = { onUpdate(exercise.copy(reps = it)) },
                    label = { Text("Reps", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF0F151B), unfocusedContainerColor = Color(0xFF0F151B), focusedBorderColor = PrimaryTurquoise, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
                OutlinedTextField(
                    value = exercise.weight,
                    onValueChange = { onUpdate(exercise.copy(weight = it)) },
                    label = { Text("Peso", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF0F151B), unfocusedContainerColor = Color(0xFF0F151B), focusedBorderColor = PrimaryTurquoise, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
                OutlinedTextField(
                    value = exercise.rest,
                    onValueChange = { onUpdate(exercise.copy(rest = it)) },
                    label = { Text("Descanso", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF0F151B), unfocusedContainerColor = Color(0xFF0F151B), focusedBorderColor = PrimaryTurquoise, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = PrimaryTurquoise)
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = PrimaryTurquoise)
                    }
                }
                Surface(
                    color = Color(0xFF00C853).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Configurado",
                        color = Color(0xFF00C853),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
