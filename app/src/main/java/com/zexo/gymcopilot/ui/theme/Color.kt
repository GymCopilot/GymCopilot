package com.zexo.gymcopilot.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

val DarkBackground = Color(0xFF0C1221)
val DarkBackgroundTop = Color(0xFF1E4A5E)
val SurfaceColor = Color(0xFF2D2D2D)
val PrimaryTurquoise = Color(0xFF00CED1)
val SecondaryGray = Color(0xFF8E8E93)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFB0B0B0)
val ErrorRed = Color(0xFFD32F2F)

// Estilo exacto de CarFinderAI
val CardBackground = Color(0xFF2D2D2D).copy(alpha = 0.8f)
val ButtonBackground = PrimaryTurquoise
val IconContainerBackground = Color(0xFF444444)

/**
 * Paleta de colores de alto contraste para profesores.
 * Se han excluido estrictamente los colores funcionales de la app:
 * - VERDES (#4CAF50): Socios activos
 * - ROJOS (#F44336): Socios deudores
 * - NARANJAS (#FF9800): Socios desertores
 * - TURQUESAS (#00CED1): Marca y botones principales
 */
val ProfessorColorsPalette = listOf(
    Color(0xFF7C4DFF), // Morado intenso
    Color(0xFFFFFF00), // Amarillo puro
    Color(0xFF00C853), // Verde esmeralda
    Color(0xFFFF80AB), // Rosa chicle
    Color(0xFF2979FF), // Azul brillante
    Color(0xFFFFD600), // Oro
    Color(0xFFAD1457), // Bordó
    Color(0xFF64FFDA), // Aguamarina
    Color(0xFFFF4081), // Rosa fuerte
    Color(0xFF9E9D24), // Verde oliva
    Color(0xFFFF6D00), // Naranja eléctrico
    Color(0xFF00E5FF), // Cian
    Color(0xFFE040FB), // Magenta
    Color(0xFFFF007F), // Rosa Brillante
    Color(0xFFB026FF), // Púrpura Neón
    Color(0xFF0091EA), // Azul Mar
    Color(0xFFD500F9), // Fucsia Neón
    Color(0xFF3D5AFE), // Indigo Intenso
    Color(0xFFAA00FF), // Púrpura
    Color(0xFFEA80FC), // Lavanda
    Color(0xFF536DFE), // Indigo suave
    Color(0xFFC51162), // Rosa Intenso
    Color(0xFF82B1FF), // Azul Cielo
    Color(0xFFFFFFFF), // Blanco
    Color(0xFF651FFF), // Púrpura Eléctrico
    Color(0xFF6A5ACD), // Azul Pizarra
    Color(0xFFF0E68C), // Caqui
    Color(0xFFCE93D8), // Amatista
    Color(0xFFB388FF), // Violeta claro
    Color(0xFF8C9EFF), // Azul Indigo
    Color(0xFF448AFF), // Azul Rey
    Color(0xFF7E57C2), // Violeta medio
    Color(0xFF304FFE), // Azul profundo
    Color(0xFF1A237E), // Azul noche
    Color(0xFFFFF176), // Amarillo claro
    Color(0xFFFFEE58), // Amarillo brillante
    Color(0xFFFBC02D)  // Amarillo ámbar
)

fun getProfessorColorById(profId: String?): Color {
    if (profId.isNullOrEmpty()) return Color.White.copy(alpha = 0.1f)
    // Usamos Long para evitar el desbordamiento (overflow) al multiplicar el hash,
    // lo cual causaba índices negativos y el cierre inesperado de la app.
    val hash = profId.hashCode().toLong()
    val index = ((hash * 7).absoluteValue % ProfessorColorsPalette.size).toInt()
    return ProfessorColorsPalette[index]
}
