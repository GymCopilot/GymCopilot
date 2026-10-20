package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

@Composable
fun AppearanceSharedScreen(
    onBack: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>("logo") } // Iniciar abierto en Logo como la captura
    var gymNameInput by remember { mutableStateOf("Tu Gimnasio") }
    var phoneInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var zipInput by remember { mutableStateOf("") }
    var countryInput by remember { mutableStateOf("") }
    var showLogoBorder by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(PrimaryTurquoise) }
    var selectedButtonStyle by remember { mutableStateOf(1) } // 1: Redondeado

    val colorPalette = listOf(
        PrimaryTurquoise,
        Color(0xFFFF0000), // Rojo
        Color(0xFFFFA500), // Naranja
        Color(0xFFFFFF00), // Amarillo
        Color(0xFF0000FF), // Azul
        Color(0xFF008000), // Verde
        Color(0xFF800080), // Violeta
        Color(0xFFFF00FF)  // Magenta
    )

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
                        text = "Apariencia",
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. LOGO
                item {
                    AccordionCard(
                        title = "Logo",
                        iconPainter = painterResource(Res.drawable.add_photo),
                        isExpanded = expandedSection == "logo",
                        onToggle = { expandedSection = if (expandedSection == "logo") null else "logo" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // ÁREA DE CARGA DE LOGO
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF00151C))
                                    .border(BorderStroke(1.5.dp, PrimaryTurquoise), RoundedCornerShape(16.dp))
                                    .clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(Res.drawable.add_photo),
                                        contentDescription = "Subir Logo",
                                        tint = PrimaryTurquoise,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Selecciona una imagen",
                                        color = TextGray,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // SECCIÓN DE HERRAMIENTAS DE LOGO IA
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF00151C)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "¿Necesitas un logo profesional? ✨\nUsa estas herramientas gratuitas para crear uno o quitarle el fondo a tu foto actual.",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )

                                    ExternalToolButton("Photoroom (Limpiar fondo)")
                                    ExternalToolButton("Canva (Crear Logo)")
                                    ExternalToolButton("Adobe (Fondo transparente)")
                                }
                            }
                        }
                    }
                }

                // 2. FONDO
                item {
                    AccordionCard(
                        title = "Fondo",
                        iconPainter = painterResource(Res.drawable.wallpaper),
                        isExpanded = expandedSection == "bg",
                        onToggle = { expandedSection = if (expandedSection == "bg") null else "bg" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Personaliza el fondo de pantalla:", color = TextGray, fontSize = 13.sp)
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CAMBIAR FONDO", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. COLORES DE BOTONES
                item {
                    AccordionCard(
                        title = "Colores de botones",
                        iconPainter = painterResource(Res.drawable.palette),
                        isExpanded = expandedSection == "colors",
                        onToggle = { expandedSection = if (expandedSection == "colors") null else "colors" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Selecciona el color corporativo:", color = TextGray, fontSize = 13.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(colorPalette) { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == color) 3.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = color }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. ESTILO DE BOTONES
                item {
                    AccordionCard(
                        title = "Estilo de botones",
                        iconPainter = painterResource(Res.drawable.call_to_action),
                        isExpanded = expandedSection == "style",
                        onToggle = { expandedSection = if (expandedSection == "style") null else "style" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Elige la forma de los botones:", color = TextGray, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ButtonStyleChip("Recto", selectedButtonStyle == 0) { selectedButtonStyle = 0 }
                                ButtonStyleChip("Redondo", selectedButtonStyle == 1) { selectedButtonStyle = 1 }
                                ButtonStyleChip("Total", selectedButtonStyle == 2) { selectedButtonStyle = 2 }
                            }
                        }
                    }
                }

                // 5. NOMBRE DEL GIMNASIO
                item {
                    AccordionCard(
                        title = "Nombre del Gimnasio",
                        iconPainter = painterResource(Res.drawable.edit),
                        isExpanded = expandedSection == "name",
                        onToggle = { expandedSection = if (expandedSection == "name") null else "name" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = gymNameInput,
                                onValueChange = { gymNameInput = it },
                                label = { Text("Nombre del Gimnasio", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryTurquoise),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 6. CONTACTO
                item {
                    AccordionCard(
                        title = "Contacto",
                        iconPainter = painterResource(Res.drawable.handshake),
                        isExpanded = expandedSection == "contact",
                        onToggle = { expandedSection = if (expandedSection == "contact") null else "contact" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CustomSharedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = "Teléfono")
                            CustomSharedTextField(value = addressInput, onValueChange = { addressInput = it }, label = "Dirección")
                            CustomSharedTextField(value = cityInput, onValueChange = { cityInput = it }, label = "Ciudad")
                            CustomSharedTextField(value = zipInput, onValueChange = { zipInput = it }, label = "Código Postal")
                            CustomSharedTextField(value = countryInput, onValueChange = { countryInput = it }, label = "País")
                        }
                    }
                }

                // 7. INSTALAR ICONO EN INICIO
                item {
                    AccordionCard(
                        title = "Instalar Icono en Inicio",
                        iconPainter = painterResource(Res.drawable.mobile_arrow_down),
                        isExpanded = expandedSection == "shortcut",
                        onToggle = { expandedSection = if (expandedSection == "shortcut") null else "shortcut" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Crea un acceso directo o instala la aplicación en el inicio de tu dispositivo o navegador:", color = TextGray, fontSize = 13.sp)
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CREAR ACCESO DIRECTO", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalToolButton(label: String) {
    Surface(
        color = Color(0xFF00222E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryTurquoise,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AccordionCard(
    title: String,
    iconPainter: Painter,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryTurquoise.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painter = iconPainter, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryTurquoise,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun ButtonStyleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) PrimaryTurquoise else Color.Transparent,
        border = BorderStroke(1.dp, PrimaryTurquoise),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
