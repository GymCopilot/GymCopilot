package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

data class HelpItemData(
    val title: String,
    val description: String,
    val iconVector: ImageVector? = null,
    val iconPainter: Painter? = null,
    val iconTint: Color = PrimaryTurquoise
)

@Composable
fun HelpSharedScreen(
    userRole: String = "admin",
    onBack: () -> Unit
) {
    val isAdmin = userRole.lowercase() == "admin"
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Store, 2: Members, 3: Professors, 4: Config
    var assistantEnabled by remember { mutableStateOf(false) }

    val homeItems = listOf(
        HelpItemData("Identidad de Marca", "Personaliza el logo y nombre de tu gimnasio desde 'Skin'. Esta información es la que verán tus socios en sus apps para identificarte, fortaleciendo la presencia de tu marca.", iconVector = Icons.Default.Star),
        HelpItemData("Estado del Local", "Muestra el indicador dinámico 'ABIERTO'. Este estado se activa automáticamente cuando hay actividad detectada en el dashboard o sesiones de clase iniciadas.", iconVector = Icons.Default.CheckCircle, iconTint = Color(0xFF00C853)),
        HelpItemData("Configuración de Nube", "Panel de acción inicial para vincular Google Drive. Una vez configurado, tus datos se sincronizarán automáticamente, permitiendo que varios dispositivos compartan la misma información al instante.", iconVector = Icons.Default.Cloud),
        HelpItemData("Aforo y Capacidad", "Control de ocupación en tiempo real. Muestra cuántas personas hay en el local frente al límite máximo. La barra de progreso cambia de color para advertirte cuando el gimnasio está llegando al tope.", iconVector = Icons.Default.Star),
        HelpItemData("Ingresos Mensuales", "Muestra el total recaudado en el mes calendario. Incluye una función de seguridad (icono de ojo) para ocultar el monto, permitiéndote consultar tus finanzas de forma privada frente a otras personas.", iconVector = Icons.Default.Star, iconTint = Color(0xFF4CAF50)),
        HelpItemData("Estadísticas de Población", "Grilla con los cuatro pilares de tu gimnasio: Total de registrados, socios con pago al día (Activos), personas con deudas (Deudores) e inscripciones recientes (Nuevos).", iconVector = Icons.Default.Menu, iconTint = PrimaryTurquoise),
        HelpItemData("Asistencia Diaria", "Contador de accesos únicos del día. Te permite monitorear el flujo de personas que han ingresado al gimnasio durante la jornada de hoy.", iconVector = Icons.Default.CheckCircle, iconTint = Color(0xFF9C27B0))
    )

    val storeItems = listOf(
        HelpItemData("Categorías", "Organiza tus productos por grupos con colores personalizados. Una estructura clara ayuda a que el administrador gestione el stock y los socios encuentren lo que buscan rápidamente.", iconVector = Icons.Default.ShoppingCart),
        HelpItemData("Productos", "Sube artículos, fotos, precios y controla el stock de los productos que vendes en el gimnasio. Puedes ver una 'Vista Previa' antes de guardar.", iconVector = Icons.Default.ShoppingCart),
        HelpItemData("Tienda Pública", "Visualiza el catálogo tal como lo ven los socios. Verifica que las fotos y precios sean correctos desde la perspectiva del cliente.", iconVector = Icons.Default.ShoppingCart)
    )

    val membersItems = listOf(
        HelpItemData("Total de Socios", "Acceso al listado completo de todas las personas registradas, sin importar su estado de pago actual.", iconVector = Icons.Default.Person),
        HelpItemData("Nuevos del Mes", "Identifica rápidamente a las personas que se inscribieron durante este mes calendario para darles seguimiento.", iconVector = Icons.Default.Person, iconTint = Color(0xFFFFC107)),
        HelpItemData("Activos", "Conteo de socios que tienen una suscripción vigente y están al día con sus pagos.", iconVector = Icons.Default.Person, iconTint = Color(0xFF00C853)),
        HelpItemData("Deudores / Vencidos", "Listado automático de socios con cuotas vencidas. El sistema los detecta comparando la fecha de hoy con sus últimos pagos.", iconVector = Icons.Default.Warning, iconTint = Color(0xFFF44336))
    )

    val profsItems = listOf(
        HelpItemData("Profesores", "Administra tu equipo de trabajo, asigna colores distintivos y gestiona sus perfiles de acceso.", iconPainter = painterResource(Res.drawable.assistant)),
        HelpItemData("Agenda y Horarios", "Define la grilla de clases y eventos. Esta agenda es pública para que tus socios sepan qué actividades hay cada día.", iconVector = Icons.Default.DateRange)
    )

    val configItems = listOf(
        HelpItemData("Skin (Apariencia)", "Personalización total: cambia colores de botones, formas (curvas), tipografías y sube el logo oficial de tu marca.", iconVector = Icons.Default.Star),
        HelpItemData("Designación", "Cambia cómo la app llama a tus integrantes (ej: llamarlos 'Atletas' en lugar de 'Alumnos' en toda la interfaz).", iconVector = Icons.Default.Person),
        HelpItemData("Idioma", "Configura el idioma de la interfaz (Español/Inglés) para adaptarla a tus preferencias.", iconVector = Icons.Default.Star),
        HelpItemData("Red", "Configura el servidor de asistencia. Esta dirección vincula la app con tu base de datos en la nube.", iconVector = Icons.Default.Cloud)
    )

    val currentItems = when (selectedTab) {
        0 -> homeItems
        1 -> storeItems
        2 -> membersItems
        3 -> profsItems
        else -> configItems
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryTurquoise,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isAdmin) "Guía del Administrador" else "Guía del Alumno",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. TARJETA SUPERIOR "Ayudante COPI" (SOLO PARA ADMIN)
                if (isAdmin) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF1D2636),
                                        shape = CircleShape,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFF7986CB),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Ayudante COPI", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "Un personaje te guiará por las funciones clave de la app.", color = TextGray, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { assistantEnabled = !assistantEnabled },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (assistantEnabled) Color(0xFF00C853) else PrimaryTurquoise
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text(
                                        text = if (assistantEnabled) "Ayudante Activado" else "Ayudante Desactivado",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. PESTAÑAS DE NAVEGACIÓN HORIZONTAL (TABS) CON ICONOS EXACTOS
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HelpTabButton(iconPainter = painterResource(Res.drawable.ic_home), isSelected = selectedTab == 0) { selectedTab = 0 }
                            HelpTabButton(iconPainter = painterResource(Res.drawable.storefront), isSelected = selectedTab == 1) { selectedTab = 1 }
                            HelpTabButton(iconPainter = painterResource(Res.drawable.groups), isSelected = selectedTab == 2) { selectedTab = 2 }
                            HelpTabButton(iconPainter = painterResource(Res.drawable.assistant), isSelected = selectedTab == 3) { selectedTab = 3 }
                            HelpTabButton(iconPainter = painterResource(Res.drawable.settings), isSelected = selectedTab == 4) { selectedTab = 4 }
                        }
                    }
                }

                // 3. TÍTULO DE SECCIÓN ACTIVA
                item {
                    val sectionTitle = when (selectedTab) {
                        0 -> "DASHBOARD"
                        1 -> "TIENDA"
                        2 -> "SOCIOS"
                        3 -> "EQUIPO"
                        else -> "SISTEMA"
                    }
                    val sectionDesc = when (selectedTab) {
                        0 -> "Panel de control principal"
                        1 -> "Gestión de ventas y catálogo"
                        2 -> "Administración de alumnos"
                        3 -> "Gestión de staff y horarios"
                        else -> "Personalización y ajustes"
                    }
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = sectionTitle, color = PrimaryTurquoise, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = sectionDesc, color = TextGray, fontSize = 12.sp)
                    }
                }

                // 4. LISTADO DE GUÍAS DE LA SECCIÓN
                items(currentItems) { item ->
                    HelpSharedCard(item = item)
                }
            }
        }
    }
}

@Composable
fun HelpTabButton(
    iconPainter: Painter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) PrimaryTurquoise.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = if (isSelected) PrimaryTurquoise else TextGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HelpSharedCard(item: HelpItemData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = item.iconTint.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.iconVector != null) {
                        Icon(
                            imageVector = item.iconVector,
                            contentDescription = null,
                            tint = item.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (item.iconPainter != null) {
                        Icon(
                            painter = item.iconPainter,
                            contentDescription = null,
                            tint = item.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = TextGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
