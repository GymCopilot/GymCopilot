package com.zexo.gymcopilot.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.AccessDoorManager
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessDoorConfigScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val accessDoorManager = remember { AccessDoorManager(dataStoreManager) }
    val scope = rememberCoroutineScope()

    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val buttonShape = getButtonStyleShape(buttonStyle)

    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val freeQrMembers by dataStoreManager.getFreeQrMembers().collectAsState(initial = emptySet())
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "GymCopilot")
    val gymAddress by dataStoreManager.getGymAddress().collectAsState(initial = "")
    val gymPhone by dataStoreManager.getGymPhone().collectAsState(initial = "")

    var enabled by remember { mutableStateOf(false) }
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("us") }
    var webhookUrl by remember { mutableStateOf("") }
    var pulseSeconds by remember { mutableStateOf("3") }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        enabled = dataStoreManager.getTuyaEnabled().first()
        clientId = dataStoreManager.getTuyaClientId().first()
        clientSecret = dataStoreManager.getTuyaClientSecret().first()
        deviceId = dataStoreManager.getTuyaDeviceId().first()
        region = dataStoreManager.getTuyaRegion().first()
        webhookUrl = dataStoreManager.getTuyaWebhookUrl().first()
        pulseSeconds = dataStoreManager.getTuyaPulseSeconds().first().toString()
    }

    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to "Apertura de Puerta",
                "subtitle" to "Control de Interruptor / Cerradura Magnética",
                "enable" to "Habilitar Apertura Automática por QR",
                "enable_desc" to "Al registrar la asistencia, se liberará el interruptor de puerta automáticamente.",
                "openapi_sec" to "Credenciales Cloud API Interruptor Smart",
                "client_id" to "Client ID / Access ID",
                "client_secret" to "Client Secret / Access Secret",
                "device_id" to "Device ID (ID del Interruptor)",
                "region" to "Región (us, eu, cn, in)",
                "webhook_sec" to "URL de Disparo Directo (Opcional)",
                "webhook_desc" to "Si prefieres usar una URL de disparo directo / Webhook sin API Keys.",
                "pulse_sec" to "Duración del Pulso de Apertura (Segundos)",
                "save" to "Guardar Configuración",
                "test" to "PROBAR APERTURA DE PUERTA",
                "saved" to "Configuración de puerta guardada"
            )
            else -> mapOf(
                "title" to "Door Opening",
                "subtitle" to "Smart Switch / Magnetic Lock Control",
                "enable" to "Enable Auto QR Door Release",
                "enable_desc" to "When attendance is recorded, the door switch will release automatically.",
                "openapi_sec" to "Smart Switch Cloud API Credentials",
                "client_id" to "Client ID / Access ID",
                "client_secret" to "Client Secret / Access Secret",
                "device_id" to "Device ID (Switch ID)",
                "region" to "Region (us, eu, cn, in)",
                "webhook_sec" to "Direct Webhook URL (Optional)",
                "webhook_desc" to "If you prefer to use a direct trigger URL / Webhook without API Keys.",
                "pulse_sec" to "Opening Pulse Duration (Seconds)",
                "save" to "Save Settings",
                "test" to "TEST DOOR OPENING",
                "saved" to "Door settings saved"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00151C))) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(t["subtitle"] ?: "", color = TextGray, fontSize = 12.sp)
                        }
                    },
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // TARJETA DE ACTIVACIÓN DE APERTURA
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002733)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t["enable"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(t["enable_desc"] ?: "", color = TextGray, fontSize = 12.sp)
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { enabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }
                    }
                }

                // SECCIÓN PUERTA DE ACCESO - QR LIBRE POR INTEGRANTE
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002733)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.opendoor),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    colorFilter = ColorFilter.tint(accentColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Puerta de acceso - QR Libre", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Habilita o deshabilita las restricciones del QR de apertura de puerta para cada integrante:", color = TextGray, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            if (members.isEmpty()) {
                                Text("No hay usuarios registrados.", color = TextGray, fontSize = 13.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    members.forEach { member ->
                                        val isFreeQr = freeQrMembers.contains(member.email.lowercase().trim())
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF00151C), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${member.firstName} ${member.lastName}".trim().ifBlank { member.email },
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = member.email,
                                                    color = TextGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Switch(
                                                checked = isFreeQr,
                                                onCheckedChange = { checked ->
                                                    scope.launch {
                                                        dataStoreManager.setMemberFreeQr(member.email, checked)
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = accentColor,
                                                    checkedTrackColor = accentColor.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val gymApiUrl = dataStoreManager.getGymApiUrl().first()
                                        val pdfFile = generateDoorQrPdf(context, gymName, gymAddress, gymPhone, gymApiUrl)
                                        withContext(Dispatchers.Main) {
                                            if (pdfFile != null) {
                                                shareFile(context, pdfFile)
                                            } else {
                                                Toast.makeText(context, t["qr_error"] ?: "Error al generar QR", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = buttonShape,
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.qr_code_download),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    colorFilter = ColorFilter.tint(Color.Black)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("QR Acceso", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // TIEMPO DE PULSO DE APERTURA
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002733)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(t["pulse_sec"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pulseSeconds,
                                onValueChange = { pulseSeconds = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = TextGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // CREDENCIALES SMART SWITCH API
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002733)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(t["openapi_sec"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            OutlinedTextField(
                                value = clientId,
                                onValueChange = { clientId = it },
                                label = { Text(t["client_id"] ?: "", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = TextGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = clientSecret,
                                onValueChange = { clientSecret = it },
                                label = { Text(t["client_secret"] ?: "", color = TextGray) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = TextGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = deviceId,
                                onValueChange = { deviceId = it },
                                label = { Text(t["device_id"] ?: "", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = TextGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = region,
                                onValueChange = { region = it },
                                label = { Text(t["region"] ?: "", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = TextGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // DISPARO DIRECTO WEBHOOK
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002733)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(t["webhook_sec"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(t["webhook_desc"] ?: "", color = TextGray, fontSize = 12.sp)

                            OutlinedTextField(
                                value = webhookUrl,
                                onValueChange = { webhookUrl = it },
                                placeholder = { Text("https://maker.ifttt.com/trigger/...", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = TextGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // BOTONES DE GUARDADO Y PRUEBA MANUAL
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    dataStoreManager.setTuyaEnabled(enabled)
                                    dataStoreManager.setTuyaClientId(clientId)
                                    dataStoreManager.setTuyaClientSecret(clientSecret)
                                    dataStoreManager.setTuyaDeviceId(deviceId)
                                    dataStoreManager.setTuyaRegion(region)
                                    dataStoreManager.setTuyaWebhookUrl(webhookUrl)
                                    dataStoreManager.setTuyaPulseSeconds(pulseSeconds.toIntOrNull() ?: 3)

                                    Toast.makeText(context, t["saved"] ?: "", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = buttonShape
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t["save"] ?: "", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTesting = true
                                    testResult = null
                                    val res = accessDoorManager.triggerDoorRelease(isManualTest = true)
                                    res.fold(
                                        onSuccess = { msg ->
                                            testResult = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        },
                                        onFailure = { err ->
                                            val errMsg = "Error: ${err.message}"
                                            testResult = errMsg
                                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                    isTesting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !isTesting,
                            border = BorderStroke(1.5.dp, accentColor),
                            shape = buttonShape
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(t["test"] ?: "", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        testResult?.let { resText ->
                            Text(
                                text = resText,
                                color = if (resText.startsWith("Error")) Color.Red else Color.Green,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun generateDoorQrPdf(
    context: Context,
    name: String,
    address: String,
    phone: String,
    gymApiUrl: String
): File? = withContext(Dispatchers.IO) {
    try {
        val json = JSONObject()
        json.put("action", "abrir_puerta")
        json.put("gym_id", name.ifBlank { "Gym_Copilot" }.replace(" ", "_"))
        json.put("puertaId", "eb40d0a2a38db6ac95gdt2")
        json.put("gym_api_url", gymApiUrl)
        val qrContent = json.toString()
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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("${name.uppercase()} - ACCESO", 100f, 100f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText(address, 100f, 130f, paint)
        canvas.drawText(phone, 100f, 150f, paint)

        val qrRect = Rect(100, 200, 500, 600)
        canvas.drawBitmap(bitmap, null, qrRect, null)

        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Escanea para abrir la puerta y registrar asistencia", 297f, 650f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "QR_Puerta_${name.replace(" ", "_")}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareFile(context: Context, file: File) {
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
    context.startActivity(Intent.createChooser(intent, "Compartir QR de Puerta"))
}
