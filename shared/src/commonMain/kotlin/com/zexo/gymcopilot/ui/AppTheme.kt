package com.zexo.gymcopilot.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0C1221)
val DarkBackgroundTop = Color(0xFF1E4A5E)
val SurfaceColor = Color(0xFF2D2D2D)
val CardBackground = Color(0xFF081C24)
val PrimaryTurquoise = Color(0xFF00CED1)
val SecondaryGray = Color(0xFF8E8E93)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFB0B0B0)
val ErrorRed = Color(0xFFD32F2F)
val AccentOrange = Color(0xFFFF9800)

val GymBackgroundGradient = Brush.verticalGradient(
    0.0f to DarkBackgroundTop,
    0.25f to DarkBackground,
    1.0f to DarkBackground
)

private val GymColorScheme = darkColorScheme(
    primary = PrimaryTurquoise,
    secondary = SecondaryGray,
    tertiary = PrimaryTurquoise,
    background = DarkBackground,
    surface = SurfaceColor,
    onPrimary = Color.Black,
    onSecondary = TextWhite,
    onTertiary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = ErrorRed
)

@Composable
fun GymCopilotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GymColorScheme,
        content = content
    )
}
