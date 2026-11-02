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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

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

data class LevelItem(val name: String, val stars: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCreationSharedScreen(
    routineId: String? = null,
    onBack: () -> Unit,
    apiClient: GymKtorApiClient? = null
) {
    val _unusedClient = apiClient
    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }
    var selectedObjective by remember { mutableStateOf("Hipertrofia") }
    var selectedLevel by remember { mutableStateOf("Intermedio") }
    val selectedDuration by remember { mutableStateOf("60 min") }
    var selectedCategory by remember { mutableStateOf("FullBody") }

    val selectedDays = remember { mutableStateListOf<String>("LU", "MI", "VI") }
    var routineType by remember { mutableStateOf("Personal") }
    var routineStatus by remember { mutableStateOf("Activa") }
    val selectedProfessorIds = remember { mutableStateListOf<String>() }

    val exercises = remember {
        mutableStateListOf(
            ExerciseEntryShared(name = "Press Inclinado con Mancuernas", series = "4", reps = "12", weight = "0 kg", rest = "60 seg", category = "PECHO")
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
                onClick = { onBack() },
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
                        val muscleGroups = listOf(
                            Triple("Pecho", "Pecho", Res.drawable.g_categoria_pecho),
                            Triple("Espalda", "Espalda", Res.drawable.g_categoria_espalda),
                            Triple("FullBody", "FullBody", Res.drawable.g_categoria_cuerpocompleto),
                            Triple("Pierna", "Pierna", Res.drawable.g_categoria_piernas),
                            Triple("Brazo", "Brazo", Res.drawable.g_categoria_brazos),
                            Triple("Abdomen", "Abdomen", Res.drawable.g_categoria_abdominales)
                        )
                        muscleGroups.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (group, label, iconRes) ->
                                    val isSelected = selectedCategory == group
                                    Card(
                                        onClick = { selectedCategory = group },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(75.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Image(
                                                painter = painterResource(iconRes),
                                                contentDescription = label,
                                                modifier = Modifier.size(30.dp),
                                                colorFilter = ColorFilter.tint(if (isSelected) Color.Black else TextWhite)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.Black else TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text("Objetivo", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val objectives = listOf(
                            Triple("Hipertrofia", "Hipertrofia", Res.drawable.hypertrophy),
                            Triple("Fuerza", "Fuerza", Res.drawable.force),
                            Triple("Resistencia", "Resistencia", Res.drawable.g_sprint),
                            Triple("Definición", "Definición", Res.drawable.definition),
                            Triple("Cardio", "Cardio", Res.drawable.cardio),
                            Triple("Rehabilitación", "Rehabilitación", Res.drawable.rehabilitation)
                        )
                        objectives.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (obj, label, iconRes) ->
                                    val isSelected = selectedObjective == obj
                                    Card(
                                        onClick = { selectedObjective = obj },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(75.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Image(
                                                painter = painterResource(iconRes),
                                                contentDescription = label,
                                                modifier = Modifier.size(28.dp),
                                                colorFilter = ColorFilter.tint(if (isSelected) Color.Black else TextWhite)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.Black else TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text("Nivel", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val levels = listOf(
                            LevelItem("Principiante", 1),
                            LevelItem("Intermedio", 3),
                            LevelItem("Avanzado", 5)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            levels.forEach { (lvl, starsCount) ->
                                val isSelected = selectedLevel == lvl
                                Card(
                                    onClick = { selectedLevel = lvl },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(65.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(starsCount) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.Black else PrimaryTurquoise,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = lvl,
                                            color = if (isSelected) Color.Black else TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
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
                        val daysMap = listOf("LU" to "L", "MA" to "M", "MI" to "M", "JU" to "J", "VI" to "V", "SÁ" to "S", "DO" to "D")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysMap.forEach { (fullKey, shortLetter) ->
                                val isSelected = selectedDays.contains(fullKey)
                                Card(
                                    onClick = {
                                        if (isSelected) selectedDays.remove(fullKey) else selectedDays.add(fullKey)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = shortLetter,
                                            color = if (isSelected) Color.Black else TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Text("Tipo de rutina", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Personal", "Asignable").forEach { type ->
                                val isSelected = routineType == type
                                Card(
                                    onClick = { routineType = type },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = type,
                                            color = if (isSelected) Color.Black else TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Text("Estado", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Borrador", "Activa", "Archivada").forEach { st ->
                                val isSelected = routineStatus == st
                                Card(
                                    onClick = { routineStatus = st },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = st,
                                            color = if (isSelected) Color.Black else TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Prof. Gabriel Fernández", "Prof. Laura Benítez").forEach { profName ->
                                val isProfSelected = selectedProfessorIds.contains(profName)
                                FilterChip(
                                    selected = isProfSelected,
                                    onClick = {
                                        if (isProfSelected) selectedProfessorIds.remove(profName) else selectedProfessorIds.add(profName)
                                    },
                                    label = { Text(profName, fontSize = 11.sp) },
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
            // Sección superior con Icono de Ejercicio (izq) y Selector de regiones del cuerpo + botón Personal (der)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Cuadro izquierdo con icono de ejercicio / silueta
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F151B))
                        .border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.fitness_center),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        colorFilter = ColorFilter.tint(PrimaryTurquoise)
                    )
                }

                // Cuadro derecho con selector de región muscular + botón PERSONAL
                Column(
                    modifier = Modifier.weight(1f).height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F151B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CUÁDRICEPS", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(exercise.category, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(2.dp))
                                Text("ABDOMINALES", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { /* Personal action */ },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F151B)),
                        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = PrimaryTurquoise, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PERSONAL", color = PrimaryTurquoise, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Tarjeta central que indica el nombre del ejercicio
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF252D36)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Seleccione un ejercicio de ${exercise.category}",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 5 campos de parámetros: Series, Reps, Peso, Descanso, Tiempo
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ParameterBox("Series", exercise.series, Modifier.weight(1f))
                ParameterBox("Reps", exercise.reps, Modifier.weight(1f))
                ParameterBox("Peso", exercise.weight, Modifier.weight(1.1f))
                ParameterBox("Descanso", exercise.rest, Modifier.weight(1.2f))
                ParameterBox("Tiempo", exercise.time, Modifier.weight(1f))
            }

            // Fila inferior: Botones de posición (^ y v), Botón "Listo" (con check) y Botón de basura
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = PrimaryTurquoise)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = PrimaryTurquoise)
                }

                Button(
                    onClick = { onUpdate(exercise.copy(isLocked = true)) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252D36)),
                    border = BorderStroke(1.dp, PrimaryTurquoise),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.checked),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Listo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun ParameterBox(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F151B)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(value, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}
