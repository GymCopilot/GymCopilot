package com.zexo.gymcopilot.ui.screens

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Exercise
import com.zexo.gymcopilot.R
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
fun RoutineDetailScreen(
    routineId: String,
    attendanceRepository: AttendanceRepository,
    onBack: () -> Unit,
    onEditRoutine: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val routines by dataStoreManager.getRoutines().collectAsState(initial = emptyList())
    val routine = routines.find { it.id == routineId || it.id.startsWith("${routineId}::") }

    // Sonidos con SoundPool
    val soundPool = remember {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
    }
    val soundCountdownId = remember { soundPool.load(context, R.raw.fourthreetwo, 1) }
    val soundOneId = remember { soundPool.load(context, R.raw.one, 1) }
    val soundDescansoId = remember { soundPool.load(context, R.raw.descanso, 1) }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    // Estados de los cronómetros
    var partialSeconds by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var isStopwatchRunning by remember { mutableStateOf(false) }

    // Estados de la cuenta regresiva inicial (5 segundos)
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountdownActive by remember { mutableStateOf(false) }

    // Estados del temporizador de descanso
    var restSeconds by remember { mutableIntStateOf(0) }
    var totalRestSeconds by remember { mutableIntStateOf(0) }
    var isRestRunning by remember { mutableStateOf(false) }

    // Estados del descanso adaptativo post-ejercicio
    var adaptiveRestSeconds by remember { mutableIntStateOf(0) }
    var totalAdaptiveRestSeconds by remember { mutableIntStateOf(0) }
    var isAdaptiveRestRunning by remember { mutableStateOf(false) }
    var adaptiveRestExerciseId by remember { mutableStateOf<String?>(null) }

    // Control del ejercicio activo
    var activeExerciseId by remember { mutableStateOf<String?>(null) }
    
    // Seguimiento de series por ejercicio
    var exerciseSeriesMap by remember { mutableStateOf(mapOf<String, Int>()) }

    // Seguimiento de ejercicios realizados para el indicador de progreso
    val realizedExerciseIds = remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(activeExerciseId) {
        activeExerciseId?.let { id ->
            realizedExerciseIds.value = realizedExerciseIds.value + id
        }
    }

    // Estado de silencio
    var isMuted by remember { mutableStateOf(false) }
    
    // Estado del Scroll
    val listState = rememberLazyListState()

    // Lógica de la cuenta regresiva de inicio
    LaunchedEffect(isCountdownActive, countdownSeconds) {
        if (isCountdownActive && countdownSeconds > 0) {
            if (!isMuted) {
                if (countdownSeconds in 2..4) {
                    soundPool.play(soundCountdownId, 1f, 1f, 0, 0, 1f)
                } else if (countdownSeconds == 1) {
                    soundPool.play(soundOneId, 1f, 1f, 0, 0, 1f)
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

    // Lógica de los cronómetros
    LaunchedEffect(isStopwatchRunning, isRestRunning, isAdaptiveRestRunning) {
        while (isActive) {
            if (isStopwatchRunning || isRestRunning || isAdaptiveRestRunning) {
                delay(1000)
                if (isStopwatchRunning) partialSeconds++
                if (isAdaptiveRestRunning && adaptiveRestSeconds > 0) {
                    adaptiveRestSeconds--
                    if (adaptiveRestSeconds == 0) {
                        isAdaptiveRestRunning = false
                        if (!isMuted) soundPool.play(soundDescansoId, 1f, 1f, 0, 0, 1f)
                    }
                }
                totalSeconds++
            } else {
                delay(500)
            }
        }
    }

    // Lógica del descanso
    LaunchedEffect(isRestRunning, restSeconds) {
        if (isRestRunning && restSeconds > 0) {
            delay(1000)
            restSeconds--
            if (restSeconds == 0) {
                isRestRunning = false
                if (!isMuted) soundPool.play(soundDescansoId, 1f, 1f, 0, 0, 1f)
            }
        } else if (restSeconds == 0) {
            isRestRunning = false
        }
    }

    // Lógica para detectar fin de ejercicio y preparar descanso adaptativo
    LaunchedEffect(exerciseSeriesMap) {
        val currentExId = activeExerciseId ?: return@LaunchedEffect
        val ex = routine?.exercises?.find { it.id == currentExId } ?: return@LaunchedEffect
        val target = ex.series.toIntOrNull() ?: 0
        val current = exerciseSeriesMap[currentExId] ?: 0
        
        if (target > 0 && current >= target && adaptiveRestExerciseId != currentExId) {
            val factor = calculateRestFactor(routine.objective)
            val weightVal = ex.weight.toDoubleOrNull() ?: 0.0
            val baseRest = ex.rest.toIntOrNull() ?: 0
            
            // Usamos partialSeconds de la serie finalizada
            val calculatedTime = (partialSeconds * factor + weightVal * 0.1 + baseRest).toInt()
            
            adaptiveRestSeconds = calculatedTime.coerceAtLeast(baseRest)
            totalAdaptiveRestSeconds = adaptiveRestSeconds
            adaptiveRestExerciseId = currentExId
            isAdaptiveRestRunning = false // Inicia pausado
        }
    }

    // Lógica para detener descanso adaptativo al activar nuevo ejercicio
    LaunchedEffect(activeExerciseId) {
        if (activeExerciseId != null && activeExerciseId != adaptiveRestExerciseId) {
            isAdaptiveRestRunning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(text = routine?.name ?: "Detalle de Rutina", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp)) },
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Image(
                                painter = painterResource(id = R.drawable.back),
                                contentDescription = "Atrás",
                                modifier = Modifier.size(34.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { routine?.let { onEditRoutine(it.id) } }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Rutina",
                                tint = accentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            routine?.let { currentRoutine ->
                                val updatedRoutine = currentRoutine.copy(
                                    status = "Completada",
                                    updatedAt = System.currentTimeMillis()
                                )
                                dataStoreManager.updateRoutine(updatedRoutine)
                                
                                val userRole = dataStoreManager.getUserRole().first()
                                val userEmail = dataStoreManager.getUserEmail().first()
                                val rawProfId = dataStoreManager.getLinkedProfessorId().first()
                                
                                // ID real de la rutina (sin el sufijo ::email si existe)
                                val baseRoutineId = updatedRoutine.id.substringBefore("::")
                                
                                // Sincronizar con el servidor
                                val routineJson = Json.encodeToString(updatedRoutine)
                                
                                val targetProfId = if (userRole?.lowercase() == "member") {
                                    updatedRoutine.professorIds.firstOrNull() ?: 
                                    (if (updatedRoutine.creatorEmail.contains("@") && updatedRoutine.creatorEmail != userEmail) updatedRoutine.creatorEmail else null) ?:
                                    rawProfId ?: userEmail
                                } else {
                                    rawProfId ?: userEmail
                                }
                                
                                attendanceRepository.syncMemberRoutine(
                                    professorId = targetProfId,
                                    memberEmail = userEmail,
                                    routineJson = routineJson,
                                    routineId = baseRoutineId,
                                    action = "update_routine"
                                )
                            }
                            onBack()
                        }
                    },
                    containerColor = accentColor,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp).size(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.finalrutina),
                        contentDescription = "Finalizar Rutina",
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                }
            }
        ) { padding ->
            if (routine == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accentColor) }
            } else {
                Column(modifier = Modifier.padding(padding)) {
                    // Calcular progreso basado en ejercicios únicos realizados
                    val progress = if (routine.exercises.isEmpty()) 0f 
                                   else realizedExerciseIds.value.size.toFloat() / routine.exercises.size

                    // Obtener tiempo objetivo y series del ejercicio activo
                    val activeExercise = routine.exercises.find { it.id == activeExerciseId }
                    val activeTargetTime = activeExercise?.time?.toIntOrNull() ?: 0
                    val activeTargetSeries = activeExercise?.series?.toIntOrNull() ?: 0

                    // Lógica para sonar al terminar el tiempo del ejercicio
                    LaunchedEffect(partialSeconds) {
                        if (activeTargetTime > 0 && partialSeconds == activeTargetTime && isStopwatchRunning) {
                            if (!isMuted) soundPool.play(soundOneId, 1f, 1f, 0, 0, 1f)
                        }
                    }

                    // Banner de Tiempos y Estadísticas (Fijo)
                    val estimatedTotalSeconds = routine.exercises.sumOf { ex ->
                        val series = ex.series.toIntOrNull() ?: 0
                        val timePerSerie = if ((ex.time.toIntOrNull() ?: 0) > 0) ex.time.toInt() else 30
                        val restPerSerie = ex.rest.toIntOrNull() ?: 0
                        
                        val executionTime = series * timePerSerie
                        val setRestTime = if (series > 1) (series - 1) * restPerSerie else 0
                        
                        // Descanso adaptativo post-ejercicio (estimado usando el tiempo configurado o 30s)
                        val factor = calculateRestFactor(routine.objective)
                        val adaptiveRest = (timePerSerie * factor + (ex.weight.toDoubleOrNull() ?: 0.0) * 0.1 + restPerSerie).toInt()
                        
                        executionTime + setRestTime + adaptiveRest
                    }

                    RoutineTimerBar(
                        partialSeconds = partialSeconds,
                        totalSeconds = totalSeconds,
                        countdownSeconds = countdownSeconds,
                        isCountdownActive = isCountdownActive,
                        restSeconds = restSeconds,
                        totalRestSeconds = totalRestSeconds,
                        isRestRunning = isRestRunning,
                        isStopwatchRunning = isStopwatchRunning,
                        accentColor = accentColor,
                        duration = formatTime(estimatedTotalSeconds),
                        exerciseCount = routine.exercises.size,
                        days = routine.days,
                        activeTargetTime = activeTargetTime,
                        progress = progress,
                        currentSeries = activeExerciseId?.let { exerciseSeriesMap[it] } ?: 0,
                        targetSeries = activeTargetSeries,
                        onToggleStopwatch = {
                            if (isStopwatchRunning) {
                                isStopwatchRunning = false
                            } else if (!isCountdownActive) {
                                countdownSeconds = 5
                                isCountdownActive = true
                            }
                        },
                        onStopRest = { isRestRunning = false; restSeconds = 0; totalRestSeconds = 0 },
                        onResetRoutine = {
                            partialSeconds = 0
                            totalSeconds = 0
                            isStopwatchRunning = false
                            isCountdownActive = false
                            countdownSeconds = 0
                            restSeconds = 0
                            totalRestSeconds = 0
                            isRestRunning = false
                            adaptiveRestSeconds = 0
                            totalAdaptiveRestSeconds = 0
                            isAdaptiveRestRunning = false
                            adaptiveRestExerciseId = null
                            activeExerciseId = null
                            realizedExerciseIds.value = emptySet()
                            exerciseSeriesMap = emptyMap()
                        }
                    )

                    // Descripción Fija (si existe)
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            color = TextGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp) 
                        ) {
                            itemsIndexed(routine.exercises, key = { _, ex -> ex.id }) { index, exercise ->
                                val isActive = activeExerciseId == exercise.id
                                Column {
                                    ExerciseDetailCard(
                                        exercise = exercise, 
                                        accentColor = accentColor, 
                                        shape = containerShape,
                                        isCurrentActive = isActive,
                                        isRunning = if (isActive) isStopwatchRunning else false,
                                        isResting = if (isActive) isRestRunning else false,
                                        isMuted = isMuted,
                                        isRealized = realizedExerciseIds.value.contains(exercise.id),
                                        onToggleMute = { isMuted = !isMuted },
                                        onClick = {
                                            if (activeExerciseId != exercise.id) {
                                                activeExerciseId = exercise.id
                                                partialSeconds = 0
                                                isStopwatchRunning = false
                                                isCountdownActive = false
                                            }
                                            scope.launch {
                                                delay(100)
                                                listState.animateScrollToItem(index = index, scrollOffset = 0)
                                            }
                                        },
                                        onPlayPause = {
                                            if (isActive && isStopwatchRunning) {
                                                isStopwatchRunning = false
                                            } else {
                                                // Incrementar serie al presionar play si no estaba corriendo
                                                val currentCount = exerciseSeriesMap[exercise.id] ?: 0
                                                exerciseSeriesMap = exerciseSeriesMap + (exercise.id to currentCount + 1)
                                                partialSeconds = 0 // Reiniciar tiempo de la serie
                                                
                                                val isChangingExercise = activeExerciseId != exercise.id
                                                if (isChangingExercise) {
                                                    activeExerciseId = exercise.id
                                                    // Cada ejercicio comienza con cuenta regresiva sonora
                                                    isStopwatchRunning = false
                                                    countdownSeconds = 5
                                                    isCountdownActive = true
                                                } else {
                                                    // Nueva serie del mismo ejercicio
                                                    if (!isCountdownActive && !isStopwatchRunning) {
                                                        countdownSeconds = 5
                                                        isCountdownActive = true
                                                    } else {
                                                        isStopwatchRunning = true
                                                    }
                                                }
                                                
                                                // Scroll al ejercicio seleccionado
                                                scope.launch {
                                                    delay(250)
                                                    listState.animateScrollToItem(
                                                        index = index,
                                                        scrollOffset = 0
                                                    )
                                                }
                                            }
                                        },
                                        onStartRest = {
                                            // Revertido a manual original: usa el tiempo configurado por el usuario
                                            val time = exercise.rest.toIntOrNull() ?: 60
                                            restSeconds = time
                                            totalRestSeconds = time
                                            isRestRunning = true
                                            isStopwatchRunning = false
                                        },
                                        onReset = {
                                            exerciseSeriesMap = exerciseSeriesMap - exercise.id
                                            realizedExerciseIds.value = realizedExerciseIds.value - exercise.id
                                            if (isActive) {
                                                partialSeconds = 0
                                                isStopwatchRunning = false
                                                isCountdownActive = false
                                            }
                                        },
                                        onNotesChange = { newNotes ->
                                            scope.launch {
                                                val updatedExercises = routine.exercises.map { ex ->
                                                    if (ex.id == exercise.id) ex.copy(notes = newNotes) else ex
                                                }
                                                val updatedRoutine = routine.copy(exercises = updatedExercises, updatedAt = System.currentTimeMillis())
                                                dataStoreManager.updateRoutine(updatedRoutine)
                                            }
                                        }
                                    )
                                    
                                    // Insertar el timer horizontal adaptativo entre ejercicios
                                    // Aparece solo si se completaron las series del ejercicio
                                    val currentSeriesCount = exerciseSeriesMap[exercise.id] ?: 0
                                    val targetSeriesCount = exercise.series.toIntOrNull() ?: 0
                                    
                                    AnimatedVisibility(
                                        visible = adaptiveRestExerciseId == exercise.id && currentSeriesCount >= targetSeriesCount,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        HorizontalRestTimer(
                                            restSeconds = adaptiveRestSeconds,
                                            totalRestSeconds = totalAdaptiveRestSeconds,
                                            isRunning = isAdaptiveRestRunning,
                                            accentColor = accentColor,
                                            onToggle = { 
                                                isAdaptiveRestRunning = !isAdaptiveRestRunning 
                                                // Si se inicia el descanso adaptativo, detener el cronómetro del ejercicio
                                                if (isAdaptiveRestRunning) {
                                                    isStopwatchRunning = false
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Espacio extra al final para permitir que el último ejercicio suba hasta el banner
                            item {
                                Spacer(modifier = Modifier.height(600.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineTimerBar(
    partialSeconds: Int,
    totalSeconds: Int,
    countdownSeconds: Int,
    isCountdownActive: Boolean,
    restSeconds: Int,
    totalRestSeconds: Int,
    isRestRunning: Boolean,
    isStopwatchRunning: Boolean,
    accentColor: Color,
    duration: String,
    exerciseCount: Int,
    days: List<String> = emptyList(),
    activeTargetTime: Int,
    progress: Float,
    currentSeries: Int,
    targetSeries: Int,
    onToggleStopwatch: () -> Unit,
    onStopRest: () -> Unit,
    onResetRoutine: () -> Unit
) {
    // Lógica para el color latiendo (blanco a rojo) en los últimos 5 segundos
    val isNearEnd = activeTargetTime > 0 && 
                    isStopwatchRunning && 
                    !isCountdownActive && 
                    partialSeconds >= (activeTargetTime - 5) && 
                    partialSeconds < activeTargetTime

    // Lógica para quedarse en rojo al finalizar el tiempo del ejercicio
    val isFinished = activeTargetTime > 0 && 
                     !isCountdownActive && 
                     partialSeconds >= activeTargetTime

    val timerAnimatedColor = remember { Animatable(TextWhite) }

    LaunchedEffect(partialSeconds, isCountdownActive, isNearEnd, isFinished) {
        if (isFinished) {
            timerAnimatedColor.animateTo(Color.Red, animationSpec = tween(300))
        } else if (isNearEnd) {
            timerAnimatedColor.snapTo(TextWhite)
            timerAnimatedColor.animateTo(Color.Red, animationSpec = tween(1000, easing = LinearEasing))
        } else {
            timerAnimatedColor.animateTo(if (isCountdownActive) Color.Red else TextWhite, animationSpec = tween(300))
        }
    }

    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 40.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Fila de Estadísticas superior
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duración
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(duration, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("Duración Estimada", color = TextGray, fontSize = 11.sp)
                }

                // Total
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total:", color = TextGray, fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(formatTime(totalSeconds), color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Ejercicios
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$exerciseCount", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("Ejercicios", color = TextGray, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Fila con IntrinsicSize.Max para que la barra lateral tenga la altura exacta del contenido central
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(horizontal = 25.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Espacio izquierdo donde va el número de serie y el botón de Reiniciar
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(), 
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        if (currentSeries > 0) {
                            val seriesColor = when {
                                targetSeries > 0 && currentSeries > targetSeries -> Color.Red
                                targetSeries > 0 && currentSeries == targetSeries -> Color(0xFF4CAF50)
                                else -> accentColor
                            }
                            val seriesFontSize = when {
                                currentSeries < 10 -> 30.sp
                                currentSeries < 100 -> 16.sp
                                else -> 12.sp
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(42.dp) 
                                    .weight(0.75f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(seriesColor.copy(alpha = 0.1f))
                                    .border(1.dp, seriesColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentSeries.toString(),
                                    color = seriesColor,
                                    fontSize = seriesFontSize,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text("SERIE", color = seriesColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Spacer(Modifier.weight(0.75f))
                        }
                        
                        Spacer(Modifier.height(9.dp))
                        
                        IconButton(
                            onClick = onResetRoutine,
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar Rutina",
                                tint = accentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.weight(0.25f))
                    }
                }

                // Columna Central: Días + Label + Cronómetro
                Column(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (days.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val allDays = listOf("LU", "MA", "MI", "JU", "VI", "SÁ", "DO")
                            allDays.forEach { day ->
                                val isSelected = days.contains(day)
                                val bgColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f)
                                val textColor = if (isSelected) Color.Black else TextWhite.copy(alpha = 0.5f)
                                
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .border(1.dp, Color.White.copy(alpha = if (isSelected) 0f else 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day,
                                        color = textColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Text("TIEMPO PARCIAL", color = TextWhite.copy(alpha = 0.3f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.clickable { onToggleStopwatch() }) {
                        Text(
                            text = if (isCountdownActive) String.format("00:%02d", countdownSeconds) else formatTime(partialSeconds),
                            color = timerAnimatedColor.value,
                            fontSize = 100.sp, fontWeight = FontWeight.Thin, letterSpacing = (-6).sp, lineHeight = 100.sp,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }

                // Indicador de Progreso a la Derecha (10dp a la derecha del contenido central)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 10.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Barra de Nivel
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .weight(1f) 
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedProgress)
                                    .background(accentColor)
                            )
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        // Porcentaje alineado a la base de la columna (debajo de la barra)
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            AnimatedVisibility(visible = restSeconds > 0 || isRestRunning, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 20.dp, end = 20.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.weight(0.9f).padding(horizontal = 24.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("DESCANSO", color = accentColor.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Spacer(Modifier.weight(1f))
                        Text(text = formatTime(restSeconds), color = accentColor, fontSize = 38.sp, fontWeight = FontWeight.Light)
                        Spacer(Modifier.width(15.dp))
                        IconButton(onClick = onStopRest, modifier = Modifier.size(32.dp)) { 
                            Icon(Icons.Default.Close, null, tint = accentColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) 
                        }
                    }
                    
                    if (totalRestSeconds > 0) {
                        val progressValue = (totalRestSeconds.toFloat() - restSeconds.toFloat()) / totalRestSeconds.toFloat()
                        val animatedProgress by animateFloatAsState(targetValue = progressValue.coerceIn(0f, 1f), animationSpec = tween(1000, easing = LinearEasing), label = "restProgress")
                        
                        Box(
                            modifier = Modifier
                                .weight(0.1f)
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun calculateRestFactor(objective: String): Double {
    return when (objective.lowercase()) {
        "fuerza" -> 3.0
        "hipertrofia", "definición", "definicion" -> 1.5
        "resistencia", "cardio", "rehabilitación", "rehabilitacion" -> 0.5
        else -> 1.5 // Default
    }
}

@Composable
fun HorizontalRestTimer(
    restSeconds: Int,
    totalRestSeconds: Int,
    isRunning: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.05f))
            .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.weight(0.9f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón integrado: 1/10 parte del recuadro
            Box(
                modifier = Modifier
                    .weight(0.12f)
                    .fillMaxHeight()
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, 
                    contentDescription = null, 
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Separador sutil
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(accentColor.copy(alpha = 0.15f)))

            // Información: 9/10 partes restantes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.88f).padding(horizontal = 16.dp)
            ) {
                Text(
                    "DESCANSO ADAPTATIVO", 
                    color = accentColor.copy(alpha = 0.8f), 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    letterSpacing = 1.sp
                )
                
                Spacer(Modifier.weight(1f))
                
                Text(
                    text = formatTime(restSeconds), 
                    color = accentColor, 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Light,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }

        if (totalRestSeconds > 0) {
            val progressValue = (totalRestSeconds.toFloat() - restSeconds.toFloat()) / totalRestSeconds.toFloat()
            val animatedProgress by animateFloatAsState(
                targetValue = progressValue.coerceIn(0f, 1f), 
                animationSpec = tween(1000, easing = LinearEasing), 
                label = "restProgress"
            )

            Box(
                modifier = Modifier
                    .weight(0.1f)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
fun LazyItemScope.ExerciseDetailCard(
    exercise: Exercise, 
    accentColor: Color, 
    shape: Shape,
    isCurrentActive: Boolean,
    isRunning: Boolean,
    isResting: Boolean,
    isMuted: Boolean,
    isRealized: Boolean,
    onToggleMute: () -> Unit,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onStartRest: () -> Unit,
    onReset: () -> Unit,
    onNotesChange: (String) -> Unit
) {
    var showNotes by rememberSaveable { mutableStateOf(false) }
    val realizedColor = Color(0xFF4CAF50)

    // Estado local para evitar lag y desorden al escribir (Debounce)
    var localNotes by remember { mutableStateOf(exercise.notes) }

    // Sincronizar localNotes si el ejercicio cambia externamente o se carga uno nuevo
    LaunchedEffect(exercise.id) {
        localNotes = exercise.notes
    }

    // Efecto para guardar con retraso (500ms)
    LaunchedEffect(localNotes) {
        if (localNotes != exercise.notes) {
            delay(500)
            onNotesChange(localNotes)
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isRealized -> realizedColor.copy(alpha = 0.1f)
            isCurrentActive -> accentColor.copy(0.08f)
            else -> Color.White.copy(alpha = 0.03f)
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), 
        label = "bgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isRealized -> realizedColor.copy(alpha = 0.6f)
            isCurrentActive -> accentColor.copy(0.5f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .animateItem(placementSpec = tween(1200, easing = FastOutSlowInEasing)) 
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .drawBehind {
                val cornerSize = 22.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, size.height - cornerSize)
                    lineTo(cornerSize, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = if (isRealized) Color(0xFF4CAF50) else accentColor)
            }
            .border(1.dp, animatedBorderColor, shape)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        shape = shape
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp).animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)).background(accentColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = getExerciseIcon(exercise.name)), contentDescription = null, modifier = Modifier.fillMaxSize(0.8f), colorFilter = ColorFilter.tint(accentColor))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.name, 
                                color = TextWhite, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onReset,
                                modifier = Modifier
                                    .padding(end = 4.dp) // Pequeño margen para que no se corte con el borde de la tarjeta
                                    .size(45.6.dp)
                                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reiniciar Ejercicio",
                                    tint = accentColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            IconButton(
                                onClick = onPlayPause,
                                enabled = !isResting,
                                modifier = Modifier
                                    .size(45.6.dp)
                                    .alpha(if (isResting) 0.5f else 1f)
                                    .background(if (isCurrentActive) accentColor else Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, if (isCurrentActive) accentColor.copy(0.3f) else Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(imageVector = if (isCurrentActive && isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = if (isCurrentActive) Color.Black else accentColor)
                            }
                            IconButton(
                                onClick = onStartRest,
                                modifier = Modifier
                                    .size(45.6.dp)
                                    .background(if (isResting) accentColor else Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, if (isResting) accentColor.copy(0.3f) else Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Timer, null, tint = if (isResting) Color.Black else TextGray)
                            }
                            IconButton(
                                onClick = onToggleMute, 
                                modifier = Modifier
                                    .size(45.6.dp)
                                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, 
                                    contentDescription = null, 
                                    tint = if (isMuted) Color.Red.copy(0.7f) else TextGray
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ExerciseStatItem("SERIES", exercise.series, accentColor)
                    ExerciseStatItem("REPS", exercise.reps, accentColor)
                    ExerciseStatItem("PESO", "${exercise.weight}kg", accentColor)
                    ExerciseStatItem("DESCANSO", "${exercise.rest}s", accentColor)
                    ExerciseStatItem("TIEMPO", "${exercise.time}s", accentColor)
                }

                AnimatedVisibility(
                    visible = showNotes,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = localNotes,
                            onValueChange = { localNotes = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            placeholder = { Text("Anotaciones sobre el ejercicio...", color = TextGray, fontSize = 13.sp) },
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
                
                // Espacio extra para que el contenido no tape la esquinita clickable si es necesario
                Spacer(Modifier.height(8.dp))
            }

            // Zona interactiva para el botón de la esquina
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(40.dp)
                    .clickable { showNotes = !showNotes }
            )
        }
    }
}

@Composable
fun ExerciseStatItem(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
