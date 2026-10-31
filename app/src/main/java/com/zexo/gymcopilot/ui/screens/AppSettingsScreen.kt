package com.zexo.gymcopilot.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.utils.UpdatesRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLanguageClick: () -> Unit,
    onNetworkConfigClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onManageProfessorsClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onHelpClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    val assistantEnabled by dataStoreManager.getAssistantEnabled().collectAsState(initial = false)
    val settingsTourSkinDismissed by dataStoreManager.getSettingsTourSkinDismissed().collectAsState(initial = false)
    val copiFontFamily = remember { FontFamily(Font(R.font.copi)) }

    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val appCurrency by dataStoreManager.getAppCurrency().collectAsState(initial = "$")
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymAddress by dataStoreManager.getGymAddress().collectAsState(initial = "")
    val gymPhone by dataStoreManager.getGymPhone().collectAsState(initial = "")

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val buttonShape = getButtonStyleShape(buttonStyle)

    val listState = rememberLazyListState()

    var showDesignationDialog by remember { mutableStateOf(false) }
    var showUpdatesDialog by remember { mutableStateOf(false) }

    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to "Configuración", 
                "lang" to "Idioma", "lang_desc" to "Cambiar el idioma de la app",
                "net" to "Red", "net_desc" to "Configurar servidor",
                "qr" to "Código QR", "qr_desc" to "Generar QR para asistencia", "currency" to "Moneda",
                "currency_desc" to "Cambiar el símbolo de moneda",
                "qr_success" to "PDF guardado en Descargas", "qr_error" to "Error al generar PDF",
                "manage_profs" to "Gestionar Profesores", "manage_profs_desc" to "Ver, editar o eliminar docentes",
                "desig" to "Designación", "desig_desc" to "Cómo llamar a los integrantes",
                "skin" to "Skin", "skin_desc" to "Personalizar apariencia",
                "skin_tour" to "¡Aquí es donde ocurre la magia! Toca en 'Skin' para personalizar los colores, el logo y el estilo visual de tu gimnasio.",
                "assistant" to "Ayuda", "assistant_desc" to "Instrucciones y guía del asistente"
            )
            else -> mapOf(
                "title" to "Settings", 
                "lang" to "Language", "lang_desc" to "Change app language",
                "net" to "Network", "net_desc" to "Configure attendance server",
                "qr" to "QR Code", "qr_desc" to "Generate QR for attendance", "currency" to "Currency",
                "currency_desc" to "Change currency symbol",
                "qr_success" to "PDF saved in Downloads", "qr_error" to "Error generating PDF",
                "manage_profs" to "Manage Professors", "manage_profs_desc" to "View, edit or delete teachers",
                "desig" to "Designation", "desig_desc" to "How to call the members",
                "skin" to "Skin", "skin_desc" to "Customize appearance",
                "skin_tour" to "This is where the magic happens! Tap on 'Skin' to customize the colors, logo, and visual style of your gym.",
                "assistant" to "Assistant", "assistant_desc" to "Instructions and assistant guide"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t["title"] ?: "Settings", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = painterResource(id = R.drawable.back),
                                contentDescription = "Back",
                                modifier = Modifier.size(34.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                AdminBottomNavigation(
                    currentRoute = "app_settings",
                    onHomeClick = onHomeClick,
                    onStoreClick = onStoreClick,
                    onMembersClick = onMembersClick,
                    onProfessorsClick = onProfessorsClick,
                    onSettingsClick = onSettingsClick,
                    accentColor = accentColor
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 100.dp)
                ) {
                    item {
                        SettingsCard(
                            title = t["assistant"] ?: "Assistant",
                            description = t["assistant_desc"] ?: "",
                            painter = painterResource(id = R.drawable.ayuda),
                            accentColor = accentColor,
                            cardColor = Color(0xFF1A237E), // Azul oscuro
                            circleColor = Color.LightGray, // Fondo gris claro
                            shape = buttonShape,
                            onClick = onHelpClick
                        )
                    }

                    item {
                        SettingsCard(
                            title = if (appLanguage == "Español") "Últimas mejoras" else "Latest improvements",
                            description = if (appLanguage == "Español") "Historial de cambios y novedades" else "Change log and news",
                            icon = Icons.Default.NewReleases,
                            accentColor = accentColor,
                            cardColor = Color(0xFFFF5722), // Naranja vibrante
                            shape = buttonShape,
                            onClick = { showUpdatesDialog = true }
                        )
                    }

                    item {
                        SettingsCard(
                            title = t["skin"] ?: "Skin",
                            description = t["skin_desc"] ?: "",
                            icon = Icons.Default.TheaterComedy,
                            accentColor = accentColor,
                            cardColor = Color(0xFF9C27B0),
                            shape = buttonShape,
                            onClick = onAppearanceClick
                        )
                    }

                    item {
                        SettingsCard(
                            title = t["qr"] ?: "QR Code",
                            description = t["qr_desc"] ?: "",
                            icon = Icons.Default.QrCode2,
                            accentColor = accentColor,
                            cardColor = accentColor,
                            shape = buttonShape,
                            onClick = {
                                scope.launch {
                                    val pdfFile = generateGymQrPdf(context, gymName, gymAddress, gymPhone)
                                    withContext(Dispatchers.Main) {
                                        if (pdfFile != null) {
                                            shareFile(context, pdfFile)
                                        } else {
                                            Toast.makeText(context, t["qr_error"], Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    item {
                        SettingsCard(
                            title = "Contacto",
                            description = "Soporte y canales de comunicación",
                            icon = Icons.Default.SupportAgent,
                            accentColor = accentColor,
                            cardColor = Color(0xFFE91E63),
                            shape = buttonShape,
                            onClick = onContactClick
                        )
                    }

                    item {
                        SettingsCard(
                            title = t["net"] ?: "Network",
                            description = t["net_desc"] ?: "",
                            painter = painterResource(id = R.drawable.drive),
                            accentColor = accentColor,
                            cardColor = Color(0xFFF44336),
                            shape = buttonShape,
                            onClick = onNetworkConfigClick
                        )
                    }

                    item {
                        SettingsCard(
                            title = t["desig"] ?: "Designation",
                            description = memberDesignation,
                            icon = Icons.Default.Badge,
                            accentColor = accentColor,
                            cardColor = Color(0xFF8BC34A),
                            shape = buttonShape,
                            onClick = { showDesignationDialog = true }
                        )
                    }

                    item {
                        SettingsCard(
                            title = t["lang"] ?: "Language",
                            description = appLanguage,
                            painter = painterResource(id = R.drawable.language),
                            accentColor = accentColor,
                            cardColor = Color(0xFFFFC107),
                            shape = buttonShape,
                            onClick = onLanguageClick
                        )
                    }

                    item {
                        SettingsCard(
                            title = t["currency"] ?: "Currency",
                            description = appCurrency,
                            icon = Icons.Default.AttachMoney,
                            accentColor = accentColor,
                            cardColor = Color(0xFF2196F3),
                            shape = buttonShape,
                            onClick = onCurrencyClick
                        )
                    }
                }

                // --- OVERLAY DE COPI ---
                if (assistantEnabled && !settingsTourSkinDismissed) {
                    val skinItemIndex = 2 // "Skin" (Ahora es el tercero, índice 2)
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == skinItemIndex }
                    
                    if (itemInfo != null) {
                        val density = LocalDensity.current
                        val verticalOffsetDp = with(density) { (itemInfo.offset + padding.calculateTopPadding().toPx()).toDp() }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = 100.dp, y = verticalOffsetDp + 30.dp) // Movido otros 50dp a la derecha (50 + 50)
                                .zIndex(10f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // PERSONAJE COPI (ARRIBA)
                                Image(
                                    painter = painterResource(id = R.drawable.copi_03),
                                    contentDescription = "Copi Tour Skin",
                                    modifier = Modifier.size(180.dp)
                                )

                                // GLOBO DE DIÁLOGO (ABAJO)
                                val bubbleShape = remember { SettingsSpeechBubbleTopShape(cornerRadius = 15.dp.value * 2.5f) }
                                Surface(
                                    shape = bubbleShape,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .offset(x = (-50).dp) // Movido 50dp a la izquierda
                                        .widthIn(max = 280.dp)
                                        .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp).padding(top = 12.dp), // Padding superior para la flecha
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = t["skin_tour"] ?: "",
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            fontFamily = copiFontFamily,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = { 
                                                scope.launch {
                                                    dataStoreManager.setSettingsTourSkinDismissed(true)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                                            shape = buttonShape,
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            modifier = Modifier.height(32.dp).border(1.dp, accentColor, buttonShape)
                                        ) {
                                            val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                                            Text("¡Entendido!", color = contentColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDesignationDialog) {
        val designations = listOf("Alumno", "Socio", "Miembro", "Estudiante", "Cliente", "Atleta")
        AlertDialog(
            onDismissRequest = { showDesignationDialog = false },
            title = { Text("Elegir Designación", color = TextWhite) },
            containerColor = Color(0xFF081C24),
            text = {
                Column {
                    designations.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        dataStoreManager.setMemberDesignation(d)
                                        showDesignationDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (memberDesignation == d),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = d, color = TextWhite)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showUpdatesDialog) {
        AlertDialog(
            onDismissRequest = { showUpdatesDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = accentColor)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (appLanguage == "Español") "Últimas Mejoras" else "Latest Improvements", 
                        color = TextWhite,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            text = {
                Column {
                    Text(
                        UpdatesRegistry.LATEST_UPDATES.trimIndent(),
                        color = TextWhite.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            },
            containerColor = Color(0xFF00222E),
            confirmButton = {
                Button(
                    onClick = { showUpdatesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                    shape = buttonShape,
                    modifier = Modifier.border(1.dp, accentColor, buttonShape)
                ) {
                    val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                    Text(if (appLanguage == "Español") "ENTENDIDO" else "GOT IT", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private suspend fun generateGymQrPdf(
    context: android.content.Context,
    name: String,
    address: String,
    phone: String
): File? = withContext(Dispatchers.IO) {
    try {
        val qrContent = "gym_id:${name.replace(" ", "_")}"
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(name.uppercase(), 100f, 100f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText(address, 100f, 130f, paint)
        canvas.drawText(phone, 100f, 150f, paint)

        val qrRect = android.graphics.Rect(100, 200, 500, 600)
        canvas.drawBitmap(bitmap, null, qrRect, null)

        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Escanea para registrar tu asistencia", 297f, 650f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "QR_Asistencia_${name.replace(" ", "_")}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir QR del Gimnasio"))
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    accentColor: Color,
    cardColor: Color,
    circleColor: Color? = null,
    shape: Shape,
    buttonBgColor: Color = LocalButtonBackgroundColor.current,
    onClick: () -> Unit
) {
    val contentColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val descColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(buttonBgColor)
            .border(
                width = 1.5.dp,
                color = accentColor,
                shape = shape
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(circleColor ?: cardColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(cardColor)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = description, color = descColor, fontSize = 14.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

class SettingsSpeechBubbleTopShape(private val cornerRadius: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val rectWidth = size.width
            val rectHeight = size.height 
            val arrowHeight = 30f
            
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, arrowHeight, rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            )
            
            // --- COLA DEL GLOBO SUPERIOR (Apunta a Copi) ---
            val tipX = rectWidth * 0.5f
            val tipY = 0f
            
            moveTo(rectWidth * 0.40f, arrowHeight)
            quadraticTo(
                rectWidth * 0.45f, arrowHeight - 15f,
                tipX, tipY
            )
            quadraticTo(
                rectWidth * 0.55f, arrowHeight - 15f,
                rectWidth * 0.60f, arrowHeight
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
