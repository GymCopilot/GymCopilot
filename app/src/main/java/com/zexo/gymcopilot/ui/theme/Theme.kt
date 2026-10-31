package com.zexo.gymcopilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
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

val GymBackgroundGradient = Brush.verticalGradient(
    0.0f to DarkBackgroundTop,
    0.25f to DarkBackground,
    1.0f to DarkBackground
)

val LocalButtonBackgroundColor = compositionLocalOf { Color(0xFF00222E) }
val LocalBottomNavBackgroundColor = compositionLocalOf { Color(0xFF00151C) }

fun getButtonStyleShape(style: Int): Shape {
    return when (style) {
        0 -> RoundedCornerShape(0.dp) // Recto
        1 -> RoundedCornerShape(14.dp) // Redondeado (actual)
        2 -> RoundedCornerShape(24.dp) // Más redondeado
        3 -> CircleShape // Totalmente redondeado
        else -> RoundedCornerShape(14.dp)
    }
}

@Composable
fun GymCopilotTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackgroundTop.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
