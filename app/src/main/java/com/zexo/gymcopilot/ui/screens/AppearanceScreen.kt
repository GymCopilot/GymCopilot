package com.zexo.gymcopilot.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AppShortcut
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.platform.LocalUriHandler
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.ui.theme.getGymFontFamily
import com.zexo.gymcopilot.ui.theme.gymFonts
import com.zexo.gymcopilot.utils.ShortcutUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    initialExpandedSection: String? = null,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")

    val assistantEnabled by dataStoreManager.getAssistantEnabled().collectAsState(initial = false)
    val appearanceTourDismissed by dataStoreManager.getAppearanceTourDismissed().collectAsState(initial = false)
    val copiFontFamily = remember { FontFamily(Font(R.font.copi)) }

    var nameState by remember { mutableStateOf("") }
    var phoneState by remember { mutableStateOf("") }
    var addressState by remember { mutableStateOf("") }
    var cityState by remember { mutableStateOf("") }
    var zipState by remember { mutableStateOf("") }
    var countryState by remember { mutableStateOf("") }
    var isDataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val initialName = dataStoreManager.getGymName().first()
        val initialPhone = dataStoreManager.getGymPhone().first()
        val initialAddress = dataStoreManager.getGymAddress().first()
        val initialCity = dataStoreManager.getGymCity().first()
        val initialZip = dataStoreManager.getGymPostalCode().first()
        val initialCountry = dataStoreManager.getGymCountry().first()

        nameState = initialName
        phoneState = initialPhone
        addressState = initialAddress
        cityState = initialCity
        zipState = initialZip
        countryState = initialCountry
        isDataLoaded = true
    }

    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val gymNameFont by dataStoreManager.getGymNameFont().collectAsState(initial = "Default")
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val showLogoBorder by dataStoreManager.getShowLogoBorder().collectAsState(initial = true)
    val containerShape = getButtonStyleShape(buttonStyle)

    var expandedSection by remember { mutableStateOf<String?>(initialExpandedSection) }

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val pUri = saveImageToInternalStorage(context, it, "gym_logo")
                if (pUri != null) {
                    dataStoreManager.setGymLogoUri(pUri.toString())
                    // Sync to cloud using Base64
                    val base64 = getBase64FromUri(context, it)
                    if (base64 != null) {
                        attendanceRepository?.syncGymInfo(logoUri = "data:image/png;base64,$base64")
                    }
                }
            }
        }
    }

    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to "Apariencia", "logo" to "Logo", "logo_desc" to "Selecciona una imagen",
                "bg" to "Fondo", "colors" to "Colores de botones", "style" to "Estilo de botones",
                "main_col" to "Colores", "gray_col" to "B / N", "rainbow_col" to "Arcoiris",
                "gym_name" to "Nombre del Gimnasio", "font" to "Tipografía del Nombre",
                "phone" to "Teléfono", "address" to "Dirección", "city" to "Ciudad",
                "zip" to "Código Postal", "country" to "País", "reset" to "Fábrica",
                "st_straight" to "Recto", "st_rounded" to "Redondo", "st_more" to "Más Redondo", "st_total" to "Total",
                "skin_tour" to "En esta sección puedes subir el logo de tu gimnasio, elegir un fondo de pantalla y personalizar el color y estilo de los botones para que coincidan con tu marca.",
                "ai_logo_tip" to "¿Necesitas un logo profesional? ✨\nUsa estas herramientas gratuitas para crear uno o quitarle el fondo a tu foto actual.",
                "ai_tool_photoroom" to "Photoroom (Limpiar fondo)",
                "ai_tool_canva" to "Canva (Crear Logo)",
                "ai_tool_adobe" to "Adobe (Fondo transparente)"
            )
            else -> mapOf(
                "title" to "Appearance", "logo" to "Logo", "logo_desc" to "Select image",
                "bg" to "Background", "colors" to "Button Colors", "style" to "Button Style",
                "main_col" to "Colors", "gray_col" to "B / W", "rainbow_col" to "Rainbow",
                "gym_name" to "Gym Name", "font" to "Name Typography", "phone" to "Phone",
                "address" to "Address", "city" to "City", "zip" to "Postal Code",
                "country" to "Country", "reset" to "Factory", "st_straight" to "Straight",
                "st_rounded" to "Rounded", "st_more" to "More Rounded", "st_total" to "Total",
                "skin_tour" to "In this section you can upload your gym logo, choose a wallpaper and customize the color and style of the buttons to match your brand.",
                "ai_logo_tip" to "Need a professional logo? ✨\nUse these free tools to create one or remove the background from your current photo.",
                "ai_tool_photoroom" to "Photoroom (BG Remover)",
                "ai_tool_canva" to "Canva (Create Logo)",
                "ai_tool_adobe" to "Adobe (Transparent BG)"
            )
        }
    }

    val mainColors = listOf(Color(0xFFFF0000), Color(0xFFFFA500), Color(0xFFFFFF00), Color(0xFF0000FF), Color(0xFF008000), Color(0xFF800080), Color(0xFF87CEEB), Color(0xFF40E0D0), Color(0xFFC0C0C0), Color(0xFFFFD700), Color(0xFF8B4513), Color(0xFFF5F5DC), Color(0xFFFFC0CB), Color(0xFF32CD32), Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFF800000), Color(0xFF808000), Color(0xFFFFFFF0))
    val grayScale = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFC0C0C0), Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF616161), Color(0xFF424242), Color(0xFF212121), Color(0xFF000000))
    val rainbowColors = listOf(Color(0xFF8F00FF), Color(0xFF4B0082), Color(0xFF0000FF), Color(0xFF007FFF), Color(0xFF00FFFF), Color(0xFF00FF00), Color(0xFFFFFF00), Color(0xFFFF7F00), Color(0xFFFF4500), Color(0xFFFF0000))

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t["title"] ?: "Appearance", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(34.dp).clip(CircleShape), colorFilter = ColorFilter.tint(accentColor))
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 100.dp)
            ) {
                item {
                    ExpandableAppearanceSection(title = t["logo"] ?: "Logo", icon = Icons.Default.AddPhotoAlternate, isExpanded = expandedSection == "logo", onToggle = { expandedSection = if (expandedSection == "logo") null else "logo" }, accentColor = accentColor, shape = containerShape) {
                        ImagePickerBox(
                            uri = gymLogoUri,
                            label = t["logo_desc"] ?: "",
                            accentColor = accentColor,
                            shape = containerShape,
                            aspectRatio = 1f,
                            contentScale = ContentScale.Fit,
                            isEditable = true,
                            showBorder = showLogoBorder,
                            onToggleBorder = {
                                scope.launch {
                                    val newVal = !showLogoBorder
                                    dataStoreManager.setShowLogoBorder(newVal)
                                    attendanceRepository?.syncGymInfo()
                                }
                            },
                            onClick = { logoLauncher.launch("image/*") },
                            onSaveAdjustments = { userScale, userOffset, containerSize ->
                                scope.launch {
                                    val currentUri = gymLogoUri ?: return@launch
                                    val adjustedUri = processAndSaveAdjustedImage(
                                        context,
                                        currentUri,
                                        userScale,
                                        userOffset,
                                        containerSize
                                    )
                                    if (adjustedUri != null) {
                                        dataStoreManager.setGymLogoUri(adjustedUri.toString())
                                        val base64 = getBase64FromUri(context, adjustedUri)
                                        if (base64 != null) {
                                            attendanceRepository?.syncGymInfo(logoUri = "data:image/png;base64,$base64")
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        // --- TARJETA DE RECOMENDACIONES IA ---
                        AIToolsCard(
                            tipText = t["ai_logo_tip"] ?: "",
                            accentColor = accentColor,
                            shape = containerShape,
                            tools = listOf(
                                AITool("Photoroom", t["ai_tool_photoroom"] ?: "", "https://www.photoroom.com/"),
                                AITool("Canva", t["ai_tool_canva"] ?: "", "https://www.canva.com/"),
                                AITool("Adobe", t["ai_tool_adobe"] ?: "", "https://www.adobe.com/express/feature/image/remove-background")
                            ),
                            onToolClick = { url -> uriHandler.openUri(url) }
                        )
                    }
                }
                item {
                    ExpandableAppearanceSection(title = t["bg"] ?: "Background", icon = Icons.Default.Wallpaper, isExpanded = expandedSection == "bg", onToggle = { expandedSection = if (expandedSection == "bg") null else "bg" }, accentColor = accentColor, shape = containerShape) {
                        Column {
                            ColorSelectorGrid(t, mainColors, grayScale, rainbowColors, accentColor = accentColor, onReset = {
                                scope.launch {
                                    dataStoreManager.resetBackgroundColor()
                                    attendanceRepository?.syncGymInfo(backgroundColor = 0) // Usamos 0 como señal de reset
                                }
                            }) { color ->
                                scope.launch {
                                    dataStoreManager.setBackgroundColor(color.toArgb())
                                    attendanceRepository?.syncGymInfo(backgroundColor = color.toArgb())
                                }
                            }
                        }
                    }
                }
                item {
                    ExpandableAppearanceSection(title = t["colors"] ?: "Colors", icon = Icons.Default.Palette, isExpanded = expandedSection == "colors", onToggle = { expandedSection = if (expandedSection == "colors") null else "colors" }, accentColor = accentColor, shape = containerShape) {
                        ColorSelectorGrid(t, mainColors, grayScale, rainbowColors, accentColor = accentColor, onReset = {
                            scope.launch {
                                val defaultColor = PrimaryTurquoise.toArgb()
                                dataStoreManager.setButtonColor(defaultColor)
                                attendanceRepository?.syncGymInfo(accentColor = defaultColor)
                            }
                        }) { color ->
                            scope.launch {
                                dataStoreManager.setButtonColor(color.toArgb())
                                attendanceRepository?.syncGymInfo(accentColor = color.toArgb())
                            }
                        }
                    }
                }
                item {
                    ExpandableAppearanceSection(title = t["style"] ?: "Style", painter = painterResource(id = R.drawable.call_to_action), isExpanded = expandedSection == "style", onToggle = { expandedSection = if (expandedSection == "style") null else "style" }, accentColor = accentColor, shape = containerShape) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ButtonStyleOption(0, t["st_straight"] ?: "Straight", getButtonStyleShape(0), buttonStyle, accentColor) {
                                    scope.launch {
                                        dataStoreManager.setButtonStyle(0)
                                        attendanceRepository?.syncGymInfo(buttonStyle = 0)
                                    }
                                }
                                ButtonStyleOption(1, t["st_rounded"] ?: "Rounded", getButtonStyleShape(1), buttonStyle, accentColor) {
                                    scope.launch {
                                        dataStoreManager.setButtonStyle(1)
                                        attendanceRepository?.syncGymInfo(buttonStyle = 1)
                                    }
                                }
                                ButtonStyleOption(2, t["st_more"] ?: "More", getButtonStyleShape(2), buttonStyle, accentColor) {
                                    scope.launch {
                                        dataStoreManager.setButtonStyle(2)
                                        attendanceRepository?.syncGymInfo(buttonStyle = 2)
                                    }
                                }
                                ButtonStyleOption(3, t["st_total"] ?: "Total", getButtonStyleShape(3), buttonStyle, accentColor) {
                                    scope.launch {
                                        dataStoreManager.setButtonStyle(3)
                                        attendanceRepository?.syncGymInfo(buttonStyle = 3)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    ExpandableAppearanceSection(title = t["gym_name"] ?: "Gym Name", icon = Icons.Default.Edit, isExpanded = expandedSection == "name", onToggle = { expandedSection = if (expandedSection == "name") null else "name" }, accentColor = accentColor, shape = containerShape) {
                        Column {
                            if (gymApiUrl.isBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2010)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (appLanguage == "Español")
                                                "Aviso importante: El nombre con el que crees la base de datos quedará registrado permanentemente en ella, aunque posteriormente cambies el nombre en esta pantalla."
                                            else
                                                "Important notice: The name used when creating the database will be permanently registered in it, even if you change the name in this screen later.",
                                            color = Color(0xFFFFE0B2),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                            if (isDataLoaded) {
                                AppearanceTextField(value = nameState, onValueChange = {
                                    nameState = it
                                    scope.launch {
                                        dataStoreManager.setGymName(it)
                                        attendanceRepository?.syncGymInfo(name = it)
                                    }
                                }, label = t["gym_name"] ?: "Name", accentColor = accentColor, shape = containerShape)
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(t["font"] ?: "Font", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(gymFonts) { font ->
                                    FontOptionItem(fontName = font, isSelected = gymNameFont == font, accentColor = accentColor, shape = containerShape, onClick = { scope.launch { dataStoreManager.setGymNameFont(font) } })
                                }
                            }
                        }
                    }
                }
                item {
                    ExpandableAppearanceSection(title = if (appLanguage == "Español") "Contacto" else "Contact", icon = Icons.Default.LocationOn, isExpanded = expandedSection == "contact", onToggle = { expandedSection = if (expandedSection == "contact") null else "contact" }, accentColor = accentColor, shape = containerShape) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (isDataLoaded) {
                                AppearanceTextField(phoneState, {
                                    phoneState = it
                                    scope.launch {
                                        dataStoreManager.setGymPhone(it)
                                        attendanceRepository?.syncGymInfo(phone = it)
                                    }
                                }, t["phone"] ?: "Phone", accentColor, shape = containerShape)
                                AppearanceTextField(addressState, {
                                    addressState = it
                                    scope.launch {
                                        dataStoreManager.setGymAddress(it)
                                        attendanceRepository?.syncGymInfo(address = it)
                                    }
                                }, t["address"] ?: "Address", accentColor, shape = containerShape)
                                AppearanceTextField(cityState, {
                                    cityState = it
                                    scope.launch {
                                        dataStoreManager.setGymCity(it)
                                        attendanceRepository?.syncGymInfo(city = it)
                                    }
                                }, t["city"] ?: "City", accentColor, shape = containerShape)
                                AppearanceTextField(zipState, {
                                    zipState = it
                                    scope.launch {
                                        dataStoreManager.setGymPostalCode(it)
                                        attendanceRepository?.syncGymInfo(zip = it)
                                    }
                                }, t["zip"] ?: "Zip", accentColor, shape = containerShape)
                                AppearanceTextField(countryState, {
                                    countryState = it
                                    scope.launch {
                                        dataStoreManager.setGymCountry(it)
                                        attendanceRepository?.syncGymInfo(country = it)
                                    }
                                }, t["country"] ?: "Country", accentColor, shape = containerShape)
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentColor)
                            }
                        }
                    }
                }
                item {
                    Surface(
                        onClick = {
                            if (nameState.isNotBlank()) {
                                ShortcutUtils.createGymShortcut(context, nameState, gymLogoUri)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF081C24),
                        shape = containerShape
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AppShortcut,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (appLanguage == "Español") "Instalar Icono en Inicio" else "Install Icon on Home",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // --- OVERLAY DE COPI ---
        if (assistantEnabled && !appearanceTourDismissed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp) // Un poco más arriba para dar espacio al globo abajo
                    .zIndex(10f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // PERSONAJE COPI (COPI_03)
                    Image(
                        painter = painterResource(id = R.drawable.copi_03),
                        contentDescription = "Copi Tour Apariencia",
                        modifier = Modifier.size(180.dp)
                    )

                    // GLOBO DE DIÁLOGO (ABAJO): Flecha apuntando hacia arriba
                    val bubbleShape = remember { AppearanceSpeechBubbleTopShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(
                        shape = bubbleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t["skin_tour"] ?: "¡Aquí puedes personalizar todo!",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontFamily = copiFontFamily,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            dataStoreManager.setAppearanceTourDismissed(true)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = containerShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            dataStoreManager.setAppearanceTourDismissed(true)
                                            onHomeClick()
                                        }
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                    shape = containerShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (appLanguage == "Español") "Panel Admin" else "Admin Panel", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class AppearanceSpeechBubbleTopShape(private val cornerRadius: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val rectWidth = size.width
            val rectHeight = size.height 
            val arrowHeight = 30f
            
            // Rectángulo redondeado principal desplazado hacia abajo para dar espacio a la flecha arriba
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, arrowHeight, rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            )
            
            val tipX = rectWidth * 0.5f
            val tipY = 0f
            
            moveTo(rectWidth * 0.40f, arrowHeight)
            quadraticTo(
                rectWidth * 0.42f, arrowHeight - 15f,
                tipX, tipY
            )
            quadraticTo(
                rectWidth * 0.58f, arrowHeight - 15f,
                rectWidth * 0.60f, arrowHeight
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}


@Composable
fun AIToolsCard(
    tipText: String,
    accentColor: Color,
    shape: Shape,
    tools: List<AITool>,
    onToolClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tipText,
                color = TextWhite.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            tools.forEach { tool ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onToolClick(tool.url) },
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = tool.label,
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

data class AITool(val name: String, val label: String, val url: String)

@Composable
fun ExpandableAppearanceSection(
    title: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF081C24))
            .border(1.dp, if (isExpanded) accentColor else Color.Transparent, shape)
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            } else if (painter != null) {
                Icon(painter, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint = TextGray
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ImagePickerBox(
    uri: String?,
    label: String,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(12.dp),
    aspectRatio: Float? = null,
    contentScale: ContentScale = ContentScale.Fit,
    isEditable: Boolean = false,
    showBorder: Boolean = true,
    onToggleBorder: (() -> Unit)? = null,
    onClick: () -> Unit,
    onSaveAdjustments: ((Float, Offset, IntSize) -> Unit)? = null
) {
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val logoShape = if (showBorder) shape else RoundedCornerShape(12.dp)
    val containerModifier = Modifier
        .fillMaxWidth()
        .then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio) else Modifier.height(150.dp))
        .onGloballyPositioned { containerSize = it.size }
        .clip(logoShape)
        .background(if (uri != null) Color.Transparent else Color(0xFF00151C))
        .then(if (showBorder) {
            Modifier.border(
                width = if (aspectRatio != null) 2.5.dp else 1.dp,
                color = if (aspectRatio != null) accentColor else accentColor.copy(alpha = 0.5f),
                shape = logoShape
            )
        } else Modifier)
        .clickable { onClick() }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            val imageModel = remember(uri) {
                if (uri.startsWith("data:image")) {
                    try {
                        val base64String = uri.substringAfter("base64,")
                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) { uri }
                } else uri
            }

            Box(
                modifier = (if (isEditable) {
                    Modifier.pointerInput(uri) {
                        detectTapGestures(
                            onTap = { onClick() }
                        )
                    }.pointerInput(uri) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset += pan
                        }
                    }
                } else Modifier)
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = contentScale
                )
            }

            if (isEditable) {
                // Botón para resetear posición o cambiar imagen
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (onToggleBorder != null) {
                        IconButton(
                            onClick = onToggleBorder,
                            modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                if (showBorder) Icons.Default.AspectRatio else Icons.Default.Square,
                                null,
                                tint = if (showBorder) accentColor else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { scale = 1f; offset = Offset.Zero },
                        modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    if (scale != 1f || offset != Offset.Zero) {
                        IconButton(
                            onClick = { onSaveAdjustments?.invoke(scale, offset, containerSize) },
                            modifier = Modifier.size(36.dp).background(Color(0xFF4CAF50).copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(36.dp).background(accentColor.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = accentColor, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(label, color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ColorSelectorGrid(
    t: Map<String, String>,
    mainColors: List<Color>,
    grayScale: List<Color>,
    rainbowColors: List<Color>,
    accentColor: Color,
    onReset: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t["main_col"] ?: "Colors", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) {
                Text(t["reset"] ?: "Factory", color = Color.Red, fontSize = 12.sp)
            }
        }
        ColorRow(mainColors, onColorSelected)
        Spacer(Modifier.height(12.dp))
        Text(t["gray_col"] ?: "B / W", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        ColorRow(grayScale, onColorSelected)
        Spacer(Modifier.height(12.dp))
        Text(t["rainbow_col"] ?: "Rainbow", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        ColorRow(rainbowColors, onColorSelected)
    }
}

@Composable
fun ColorRow(colors: List<Color>, onColorSelected: (Color) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun RowScope.ButtonStyleOption(
    index: Int,
    label: String,
    shape: Shape,
    selectedIndex: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isSelected = index == selectedIndex
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .background(if (isSelected) accentColor else Color(0xFF00151C))
                .border(1.dp, if (isSelected) Color.Transparent else accentColor.copy(alpha = 0.3f), shape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("Abc", color = if (isSelected) Color.Black else TextWhite, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (isSelected) accentColor else TextGray, fontSize = 10.sp)
    }
}

@Composable
fun AppearanceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = accentColor.copy(alpha = 0.5f),
            focusedLabelColor = accentColor,
            unfocusedLabelColor = TextGray,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        ),
        shape = shape,
        singleLine = true
    )
}

@Composable
fun FontOptionItem(
    fontName: String,
    isSelected: Boolean,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(8.dp),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isSelected) accentColor else Color(0xFF081C24))
            .border(1.dp, if (isSelected) Color.Transparent else accentColor.copy(alpha = 0.3f), shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fontName,
            color = if (isSelected) Color.Black else TextWhite,
            fontFamily = getGymFontFamily(fontName),
            fontSize = 14.sp
        )
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
        Uri.fromFile(file)
    } catch (e: Exception) { null }
}

private fun getBase64FromUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        val maxDim = 200
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val width = if (aspectRatio > 1) maxDim else (maxDim * aspectRatio).toInt()
            val height = if (aspectRatio > 1) (maxDim / aspectRatio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

private fun processAndSaveAdjustedImage(
    context: Context,
    imageSource: String,
    userScale: Float,
    userOffset: Offset,
    containerSize: IntSize
): Uri? {
    return try {
        val originalBitmap = if (imageSource.startsWith("data:image")) {
            val base64String = imageSource.substringAfter("base64,")
            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } else {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(imageSource)) ?: return null
            val bmp = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bmp
        } ?: return null

        val cWidth = containerSize.width.toFloat()
        val cHeight = containerSize.height.toFloat()
        if (cWidth <= 0 || cHeight <= 0) return null

        // 1. Calcular la escala base de "Crop"
        val scaleBase = Math.max(cWidth / originalBitmap.width, cHeight / originalBitmap.height)
        val baseWidth = originalBitmap.width * scaleBase
        val baseHeight = originalBitmap.height * scaleBase

        // 2. Crear el bitmap de salida con el tamaño del contenedor (pero alta calidad)
        // Escalamos todo por un factor para no perder resolución si el contenedor es pequeño en pantalla
        val qualityFactor = 2f 
        val outWidth = (cWidth * qualityFactor).toInt()
        val outHeight = (cHeight * qualityFactor).toInt()
        
        val outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        
        val matrix = Matrix()
        
        // Centrar base (equivalente a ContentScale.Crop)
        matrix.postTranslate(-originalBitmap.width / 2f, -originalBitmap.height / 2f)
        matrix.postScale(scaleBase, scaleBase)
        matrix.postTranslate(cWidth / 2f, cHeight / 2f)
        
        // Aplicar transformaciones del usuario
        matrix.postTranslate(-cWidth / 2f, -cHeight / 2f)
        matrix.postScale(userScale, userScale)
        matrix.postTranslate(cWidth / 2f + userOffset.x, cHeight / 2f + userOffset.y)
        
        // Escalar al tamaño final de salida
        matrix.postScale(qualityFactor, qualityFactor)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(originalBitmap, matrix, paint)

        val fileName = "gym_logo_adjusted_${System.currentTimeMillis()}.png"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
        
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}
