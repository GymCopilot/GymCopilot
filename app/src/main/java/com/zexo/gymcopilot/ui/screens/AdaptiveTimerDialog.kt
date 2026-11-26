package com.zexo.gymcopilot.ui.screens

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun AdaptiveTimerDialog(
    onDismiss: () -> Unit,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape
) {
    val context = LocalContext.current
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedFactor by remember { mutableDoubleStateOf(1.5) } // Default Hipertrofia
    var calculatedRest by remember { mutableLongStateOf(0L) }
    var restRemaining by remember { mutableLongStateOf(0L) }
    var isRestRunning by remember { mutableStateOf(false) }
    var seriesCount by remember { mutableIntStateOf(0) }

    // Estados de la cuenta regresiva inicial (5 segundos)
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountdownActive by remember { mutableStateOf(false) }
    
    val factors = listOf(
        TimerMode("FUERZA", 3.0, R.drawable.force),
        TimerMode("HIPERTROFIA", 1.5, R.drawable.hypertrophy),
        TimerMode("RESISTENCIA", 0.5, R.drawable.g_sprint)
    )

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

    val timerAnimatedColor = remember { Animatable(TextWhite) }
    val infiniteTransition = rememberInfiniteTransition(label = "timer")
    
    // Animación para el parpadeo rojo
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Animación para el giro de la serpiente
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Animación para la visibilidad de la serpiente
    val snakeAlpha by animateFloatAsState(
        targetValue = if (isCountdownActive || isRunning) 1f else 0f,
        animationSpec = tween(500),
        label = "snakeAlpha"
    )

    LaunchedEffect(isCountdownActive, isRunning) {
        if (isCountdownActive || isRunning) {
            val startTime = System.currentTimeMillis()
            while (isActive && (isCountdownActive || isRunning)) {
                val current = System.currentTimeMillis()
                // Una vuelta cada 10 segundos
                rotationAngle = ((current - startTime) % 10000) / 10000f * 360f
                delay(16)
            }
        } else {
            rotationAngle = 0f
        }
    }

    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive) {
            timerAnimatedColor.animateTo(Color.Red, animationSpec = tween(300))
        } else {
            timerAnimatedColor.animateTo(TextWhite, animationSpec = tween(300))
        }
    }

    // Lógica de la cuenta regresiva de inicio
    LaunchedEffect(isCountdownActive, countdownSeconds) {
        if (isCountdownActive && countdownSeconds > 0) {
            if (countdownSeconds in 2..4) {
                soundPool.play(soundCountdownId, 1f, 1f, 0, 0, 1f)
            } else if (countdownSeconds == 1) {
                soundPool.play(soundOneId, 1f, 1f, 0, 0, 1f)
            }
            delay(1000)
            countdownSeconds--
            if (countdownSeconds == 0) {
                isCountdownActive = false
                isRunning = true
            }
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            isRestRunning = false // Detener descanso si se inicia una nueva serie
            val startTime = System.currentTimeMillis() - (elapsedTime * 1000)
            while (isActive && isRunning) {
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000
                delay(100)
            }
        }
    }

    // Lógica del descanso
    LaunchedEffect(isRestRunning, restRemaining) {
        if (isRestRunning && restRemaining > 0) {
            delay(1000)
            restRemaining--
            if (restRemaining == 0L) {
                isRestRunning = false
                soundPool.play(soundDescansoId, 1f, 1f, 0, 0, 1f)
            }
        } else if (restRemaining == 0L) {
            isRestRunning = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .drawWithContent {
                    drawContent()
                    val strokeWidth = 3.dp.toPx()
                    
                    // Usamos la API de Android para rotar el shader de forma precisa
                    // Invertimos el orden de colores para que el brillante (head) lidere el giro
                    val colors = intArrayOf(android.graphics.Color.TRANSPARENT, accentColor.copy(alpha = snakeAlpha).toArgb())
                    val shader = android.graphics.SweepGradient(
                        size.width / 2f,
                        size.height / 2f,
                        colors,
                        null
                    )
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(rotationAngle, size.width / 2f, size.height / 2f)
                    shader.setLocalMatrix(matrix)
                    
                    drawOutline(
                        outline = shape.createOutline(size, layoutDirection, this),
                        brush = ShaderBrush(shader),
                        style = Stroke(width = strokeWidth)
                    )
                },
            shape = shape,
            color = Color(0xFF00222E)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temporizador Adaptativo", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))

                // Cronómetro Principal (Estilo RoutineDetail)
                Text(
                    text = "TIEMPO DE SERIE",
                    color = TextWhite.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isCountdownActive) String.format(Locale.getDefault(), "00:%02d", countdownSeconds) else formatTime(elapsedTime),
                    color = timerAnimatedColor.value,
                    fontSize = 100.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-6).sp,
                    lineHeight = 100.sp,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.graphicsLayer {
                        if (isCountdownActive) alpha = blinkAlpha
                    }
                )

                Spacer(Modifier.height(32.dp))

                // Selector de Modos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    factors.forEach { mode ->
                        ModeButton(
                            mode = mode,
                            isSelected = selectedFactor == mode.factor,
                            accentColor = accentColor,
                            shape = shape,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFactor = mode.factor }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Panel de Descanso + Contador de Series al costado
                Row(
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contador de Series (20%)
                    Box(
                        modifier = Modifier
                            .weight(0.2f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(accentColor.copy(alpha = 0.1f))
                            .border(1.dp, accentColor.copy(alpha = 0.2f), shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (seriesCount > 0) seriesCount.toString() else "0",
                            color = accentColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Panel de Descanso (80%)
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(accentColor.copy(alpha = 0.1f))
                            .border(1.dp, accentColor.copy(alpha = 0.2f), shape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.9f).padding(horizontal = 24.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (isRestRunning || restRemaining > 0) formatTime(restRemaining) else "00:00",
                                color = accentColor,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Light
                            )
                        }

                        if (calculatedRest > 0 && (isRestRunning || restRemaining > 0)) {
                            val progressValue = (calculatedRest.toFloat() - restRemaining.toFloat()) / calculatedRest.toFloat()
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

                Spacer(Modifier.height(32.dp))

                // Controles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRunning) {
                                isRunning = false
                                calculatedRest = (elapsedTime * selectedFactor).toLong()
                                restRemaining = calculatedRest
                                isRestRunning = true
                            } else if (!isCountdownActive) {
                                elapsedTime = 0
                                calculatedRest = 0
                                restRemaining = 0
                                isRestRunning = false
                                countdownSeconds = 5
                                isCountdownActive = true
                                seriesCount++
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning || isCountdownActive) Color(0xFFF44336) else accentColor
                        ),
                        shape = shape
                    ) {
                        Text(
                            text = if (isRunning || isCountdownActive) "TERMINAR SERIE" else "INICIAR SERIE",
                            color = if (isRunning || isCountdownActive) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            isRunning = false
                            isRestRunning = false
                            isCountdownActive = false
                            countdownSeconds = 0
                            elapsedTime = 0
                            calculatedRest = 0
                            restRemaining = 0
                            seriesCount = 0
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.1f), shape)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = TextWhite)
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextWhite.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun ModeButton(
    mode: TimerMode,
    isSelected: Boolean,
    accentColor: Color,
    shape: Shape,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(id = mode.iconRes),
                contentDescription = mode.name,
                tint = if (isSelected) accentColor else TextGray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(mode.name, color = if (isSelected) accentColor else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class TimerMode(
    val name: String,
    val factor: Double,
    val iconRes: Int
)

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
