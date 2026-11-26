package com.zexo.gymcopilot.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.utils.getCurrentTimeMillis
import com.zexo.gymcopilot.utils.playAudioSound
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class TimerModeShared(
    val title: String,
    val factor: Double,
    val iconRes: DrawableResource
)

@Composable
fun AdaptiveTimerSharedDialog(
    onDismiss: () -> Unit,
    accentColor: Color = PrimaryTurquoise
) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedFactor by remember { mutableDoubleStateOf(1.5) } // Default Hipertrofia
    var calculatedRest by remember { mutableLongStateOf(0L) }
    var restRemaining by remember { mutableLongStateOf(0L) }
    var isRestRunning by remember { mutableStateOf(false) }
    var seriesCount by remember { mutableIntStateOf(0) }

    // Cuenta regresiva inicial de 5s al pulsar INICIAR SERIE
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountdownActive by remember { mutableStateOf(false) }

    val factors = listOf(
        TimerModeShared("FUERZA", 3.0, Res.drawable.force),
        TimerModeShared("HIPERTROFIA", 1.5, Res.drawable.hypertrophy),
        TimerModeShared("RESISTENCIA", 0.5, Res.drawable.g_sprint)
    )

    val timerAnimatedColor = remember { Animatable(TextWhite) }

    // Animación de rotación del degradado de la serpiente
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val isSnakeActive = isCountdownActive || isRunning

    LaunchedEffect(isSnakeActive) {
        if (isSnakeActive) {
            val startTime = getCurrentTimeMillis()
            while (isActive && (isCountdownActive || isRunning)) {
                val current = getCurrentTimeMillis()
                rotationAngle = ((current - startTime) % 4000) / 4000f * 360f
                delay(16)
            }
        } else {
            rotationAngle = 0f
        }
    }

    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive) {
            timerAnimatedColor.animateTo(Color(0xFFFF5252), animationSpec = tween(300))
        } else {
            timerAnimatedColor.animateTo(TextWhite, animationSpec = tween(300))
        }
    }

    // Lógica de la cuenta regresiva de inicio (5 silencioso, 4-3-2 fourthreetwo, 1 one)
    LaunchedEffect(isCountdownActive, countdownSeconds) {
        if (isCountdownActive && countdownSeconds > 0) {
            if (countdownSeconds in 2..4) {
                playAudioSound("fourthreetwo")
            } else if (countdownSeconds == 1) {
                playAudioSound("one")
            }
            delay(1000)
            countdownSeconds--
            if (countdownSeconds == 0) {
                isCountdownActive = false
                isRunning = true
            }
        }
    }

    // Cronómetro de tiempo de serie
    LaunchedEffect(isRunning) {
        if (isRunning) {
            isRestRunning = false // Detener descanso al iniciar nueva serie
            val startTime = getCurrentTimeMillis() - (elapsedTime * 1000)
            while (isActive && isRunning) {
                elapsedTime = (getCurrentTimeMillis() - startTime) / 1000
                delay(200)
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
                playAudioSound("descanso")
            }
        } else if (restRemaining == 0L) {
            isRestRunning = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .clickable(enabled = false) { }
                .drawWithContent {
                    drawContent()
                    if (isSnakeActive) {
                        val strokeWidth = 3.dp.toPx()
                        val shapeRadius = 24.dp.toPx()
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)
                        val fraction = (rotationAngle % 360f) / 360f

                        val snakeBrush = getRotatingSnakeBrush(fraction, accentColor, centerOffset)

                        drawRoundRect(
                            brush = snakeBrush,
                            cornerRadius = CornerRadius(shapeRadius, shapeRadius),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Temporizador Adaptativo",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "TIEMPO DE SERIE",
                    color = TextWhite.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isCountdownActive) formatTimerSeconds(countdownSeconds) else formatTimerSeconds(elapsedTime.toInt()),
                    color = timerAnimatedColor.value,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Thin,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Selector de Modos (Fuerza, Hipertrofia, Resistencia)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    factors.forEach { mode ->
                        val isSelected = selectedFactor == mode.factor
                        Card(
                            onClick = { selectedFactor = mode.factor },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF0F151B)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(80.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(mode.iconRes),
                                    contentDescription = mode.title,
                                    modifier = Modifier.size(26.dp),
                                    colorFilter = ColorFilter.tint(if (isSelected) accentColor else TextWhite)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = mode.title,
                                    color = if (isSelected) accentColor else TextWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Panel de Descanso + Contador de Series
                Row(
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Contador de Series (20%)
                    Box(
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = seriesCount.toString(),
                            color = accentColor,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Panel de Descanso con barra de progreso inferior (75%)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F151B)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(0.75f).fillMaxHeight()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Timer, null, tint = accentColor, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = if (isRestRunning || restRemaining > 0) formatTimerSeconds(restRemaining.toInt()) else "00:00",
                                    color = accentColor,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }

                            if (calculatedRest > 0 && (isRestRunning || restRemaining > 0)) {
                                val restProgress = (calculatedRest.toFloat() - restRemaining.toFloat()) / calculatedRest.toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(restProgress.coerceIn(0f, 1f))
                                        .height(3.dp)
                                        .align(Alignment.BottomStart)
                                        .background(accentColor)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Controles principales: Botón Iniciar/Terminar Serie + Reiniciar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning || isCountdownActive) Color(0xFFFF5252) else accentColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(
                            text = if (isRunning || isCountdownActive) "TERMINAR SERIE" else "INICIAR SERIE",
                            color = if (isRunning || isCountdownActive) Color.White else Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
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
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = TextWhite)
                    }
                }

                Spacer(Modifier.height(16.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextWhite.copy(alpha = 0.6f))
                }
            }
        }
    }
}

fun getRotatingSnakeBrush(t: Float, headColor: Color, center: Offset): Brush {
    val p0 = (0.0f + t) % 1.0f
    val p1 = (0.12f + t) % 1.0f
    val p2 = (0.25f + t) % 1.0f
    val p3 = (0.40f + t) % 1.0f
    val p4 = (0.80f + t) % 1.0f

    val rawStops = listOf(
        p0 to headColor,
        p1 to headColor.copy(alpha = 0.6f),
        p2 to headColor.copy(alpha = 0.15f),
        p3 to Color.Transparent,
        p4 to Color.Transparent
    )

    val sortedStops = rawStops.sortedBy { it.first }
    val firstStop = sortedStops.first()
    val lastStop = sortedStops.last()

    val finalStops = mutableListOf<Pair<Float, Color>>()
    if (firstStop.first > 0f) {
        finalStops.add(0f to lastStop.second)
    }
    finalStops.addAll(sortedStops)
    if (lastStop.first < 1f) {
        finalStops.add(1f to firstStop.second)
    }

    return Brush.sweepGradient(
        *finalStops.toTypedArray(),
        center = center
    )
}

fun formatTimerSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
