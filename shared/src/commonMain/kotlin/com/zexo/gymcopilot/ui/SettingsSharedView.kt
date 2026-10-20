package com.zexo.gymcopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun SettingsSharedScreen(
    onBack: () -> Unit
) {
    var selectedDialog by remember { mutableStateOf<String?>(null) }
    var appLanguage by remember { mutableStateOf("Español") }
    var appCurrency by remember { mutableStateOf("Peso Argentino") }
    var memberDesignation by remember { mutableStateOf("Alumno") }
    var serverUrl by remember { mutableStateOf("https://script.google.com/macros/s/...") }

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
                        text = "Configuración",
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
                // 1. AYUDA
                item {
                    SettingsSharedCard(
                        title = "Ayuda",
                        subtitle = "Instrucciones y guía del asistente",
                        iconPainter = painterResource(Res.drawable.ayuda),
                        iconBg = Color(0xFF1A237E),
                        onClick = { selectedDialog = "help" }
                    )
                }

                // 2. ÚLTIMAS MEJORAS
                item {
                    SettingsSharedCard(
                        title = "Últimas mejoras",
                        subtitle = "Historial de cambios y novedades",
                        iconPainter = painterResource(Res.drawable.report),
                        iconBg = Color(0xFFFF5722),
                        onClick = { selectedDialog = "updates" }
                    )
                }

                // 3. SKIN
                item {
                    SettingsSharedCard(
                        title = "Skin",
                        subtitle = "Personalizar apariencia",
                        iconPainter = painterResource(Res.drawable.theater_comedy),
                        iconBg = Color(0xFF7B1FA2),
                        onClick = { selectedDialog = "skin" }
                    )
                }

                // 4. CÓDIGO QR
                item {
                    SettingsSharedCard(
                        title = "Código QR",
                        subtitle = "Generar QR para asistencia",
                        iconPainter = painterResource(Res.drawable.qr_code),
                        iconBg = Color(0xFF00897B),
                        onClick = { selectedDialog = "qr" }
                    )
                }

                // 5. CONTACTO
                item {
                    SettingsSharedCard(
                        title = "Contacto",
                        subtitle = "Soporte y canales de comunicación",
                        iconPainter = painterResource(Res.drawable.support_agent),
                        iconBg = Color(0xFFC2185B),
                        onClick = { selectedDialog = "contact" }
                    )
                }

                // 6. RED
                item {
                    SettingsSharedCard(
                        title = "Red",
                        subtitle = "Configurar servidor",
                        iconPainter = painterResource(Res.drawable.drive),
                        iconBg = Color(0xFFE64A19),
                        onClick = { selectedDialog = "network" }
                    )
                }

                // 7. DESIGNACIÓN
                item {
                    SettingsSharedCard(
                        title = "Designación",
                        subtitle = memberDesignation,
                        iconPainter = painterResource(Res.drawable.badge),
                        iconBg = Color(0xFF33691E),
                        onClick = { selectedDialog = "designation" }
                    )
                }

                // 8. IDIOMA
                item {
                    SettingsSharedCard(
                        title = "Idioma",
                        subtitle = appLanguage,
                        iconPainter = painterResource(Res.drawable.g_translate),
                        iconBg = Color(0xFFF57F17),
                        onClick = { selectedDialog = "language" }
                    )
                }

                // 9. MONEDA
                item {
                    SettingsSharedCard(
                        title = "Moneda",
                        subtitle = appCurrency,
                        iconPainter = painterResource(Res.drawable.attach_money),
                        iconBg = Color(0xFF0D47A1),
                        onClick = { selectedDialog = "currency" }
                    )
                }
            }
        }
    }

    // DIÁLOGOS MODALES
    when (selectedDialog) {
        "help" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Ayuda de GymCopilot", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Bienvenido a GymCopilot. Utiliza el panel de administrador para gestionar tus socios, registrar pagos, crear planes de suscripción y controlar la asistencia mediante códigos QR de forma sencilla.", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { selectedDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
        "updates" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Últimas Mejoras", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("• Versión 3.5.0 KMP Multiplatform\n• Soporte total para Web (PWA), Android e iOS\n• Sincronización en tiempo real con Google Sheets\n• Gestión avanzada de alumnos y profesores", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { selectedDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
        "skin" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Personalización de Skin", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("La apariencia visual y la paleta de colores corporativos se encuentran configuradas con el estilo turquesa Zexo predeterminado.", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { selectedDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("ACEPTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
        "qr" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Código QR General", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Genera y exporta el código QR institucional para el fichaje y control de asistencia de los socios en la entrada del gimnasio.", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { selectedDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("GENERAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
        "contact" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Soporte y Contacto", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("¿Necesitas asistencia técnica o soporte personalizado? Escríbenos a soporte@gymcopilot.com o por Telegram oficial.", color = TextGray) },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { selectedDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
        "network" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Configuración de Red / Servidor", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("URL del API Backend o Apps Script:", color = TextGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryTurquoise),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { selectedDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
        "designation" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Designación de Miembros", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf("Alumno", "Socio", "Miembro", "Estudiante", "Cliente", "Atleta")
                    options.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { memberDesignation = opt; selectedDialog = null }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = memberDesignation == opt, onClick = { memberDesignation = opt; selectedDialog = null })
                            Spacer(Modifier.width(8.dp))
                            Text(opt, color = TextWhite)
                        }
                    }
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = { selectedDialog = null }) { Text("CANCELAR", color = TextWhite) }
            }
        )
        "language" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Seleccionar Idioma", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val langs = listOf("Español", "English")
                    langs.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { appLanguage = lang; selectedDialog = null }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = appLanguage == lang, onClick = { appLanguage = lang; selectedDialog = null })
                            Spacer(Modifier.width(8.dp))
                            Text(lang, color = TextWhite)
                        }
                    }
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = { selectedDialog = null }) { Text("CANCELAR", color = TextWhite) }
            }
        )
        "currency" -> AlertDialog(
            onDismissRequest = { selectedDialog = null },
            title = { Text("Seleccionar Moneda", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currencies = listOf("Peso Argentino", "Dólar Estadounidense (USD)", "Euro (€)")
                    currencies.forEach { cur ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { appCurrency = cur; selectedDialog = null }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = appCurrency == cur, onClick = { appCurrency = cur; selectedDialog = null })
                            Spacer(Modifier.width(8.dp))
                            Text(cur, color = TextWhite)
                        }
                    }
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                TextButton(onClick = { selectedDialog = null }) { Text("CANCELAR", color = TextWhite) }
            }
        )
    }
}

@Composable
fun SettingsSharedCard(
    title: String,
    subtitle: String,
    iconPainter: Painter,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = iconBg,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 12.sp, color = TextGray)
                }
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = PrimaryTurquoise,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
