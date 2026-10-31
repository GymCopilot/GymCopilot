package com.zexo.gymcopilot.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.GymBackgroundGradient
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.utils.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConfigScreen(
    attendanceRepository: com.zexo.gymcopilot.repository.AttendanceRepository,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val gymWifiSsid by dataStoreManager.getGymWifiSsid().collectAsState(initial = "")
    val gymWifiSsid2 by dataStoreManager.getGymWifiSsid2().collectAsState(initial = "")
    val gymWifiSsid3 by dataStoreManager.getGymWifiSsid3().collectAsState(initial = "")
    val currentLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    
    var apiUrlText by remember(gymApiUrl) { mutableStateOf(gymApiUrl) }
    var wifiSsidText by remember(gymWifiSsid) { mutableStateOf(gymWifiSsid) }
    var wifiSsid2Text by remember(gymWifiSsid2) { mutableStateOf(gymWifiSsid2) }
    var wifiSsid3Text by remember(gymWifiSsid3) { mutableStateOf(gymWifiSsid3) }
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    var isLocked by remember { mutableStateOf(true) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Estado para el selector de redes
    var availableNetworks by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var targetFieldForScan by remember { mutableStateOf(1) }

    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    fun scanNetworks() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            // Se pedirá el permiso
        } else {
            isScanning = true
            val results = wifiManager.scanResults
                .map { it.SSID }
                .filter { it.isNotBlank() && !it.contains("<unknown ssid>") }
                .distinct()
                .sorted()
            
            availableNetworks = results
            showNetworkDialog = true
            isScanning = false

            if (results.isEmpty()) {
                Toast.makeText(context, if (currentLanguage == "Español") "No se detectaron redes. Asegúrate de tener el GPS activo." else "No networks detected. Make sure GPS is on.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanNetworks()
        } else {
            Toast.makeText(context, if (currentLanguage == "Español") "Permiso denegado" else "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val startScan = { fieldIndex: Int ->
        targetFieldForScan = fieldIndex
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            scanNetworks()
        }
    }

    val dynamicCode = remember { SecurityUtils.generateDynamicCode() }

    val title = if (currentLanguage == "Español") "Configuración de Red" else "Network Config"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                attendanceRepository.syncGymInfo()
                                onBack()
                            }
                        }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (currentLanguage == "Español") "Servidor de Red (Gratis)" else "Network Server (Free)",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = apiUrlText,
                    onValueChange = {
                        apiUrlText = it
                        scope.launch { dataStoreManager.setGymApiUrl(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLocked,
                    placeholder = { Text("https://script.google.com/macros/s/...", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Link, null, tint = accentColor) },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            AnimatedContent(
                                targetState = isLocked,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f))
                                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f))
                                },
                                label = "LockIconAnimation"
                            ) { locked ->
                                IconButton(onClick = {
                                    if (locked) {
                                        showValidationDialog = true
                                    } else {
                                        isLocked = true
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (locked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = accentColor
                                    )
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = !isLocked && apiUrlText.isNotBlank(),
                                enter = expandHorizontally() + fadeIn(),
                                exit = shrinkHorizontally() + fadeOut()
                            ) {
                                IconButton(onClick = {
                                    showDeleteConfirm = true
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    },
                    visualTransformation = if (isLocked) PasswordVisualTransformation() else VisualTransformation.None,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        disabledBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        disabledContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        disabledTextColor = TextWhite,
                        disabledPlaceholderColor = TextGray
                    )
                )

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        containerColor = Color(0xFF081C24),
                        title = { Text("Eliminar Configuración", color = TextWhite, fontWeight = FontWeight.Bold) },
                        text = { Text("¿Estás seguro de que deseas borrar la URL del servidor? Esta acción desconectará la app de la nube.", color = TextGray) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val gymName = dataStoreManager.getGymName().first()
                                        val googleRepo = com.zexo.gymcopilot.repository.GoogleCloudRepository(dataStoreManager)
                                        try {
                                            googleRepo.resetGymBackend(gymName)
                                        } catch (e: Exception) {
                                            android.util.Log.e("NetworkConfigScreen", "Error reseteando gym backend: ${e.message}")
                                        }
                                        apiUrlText = ""
                                        dataStoreManager.setGymApiUrl("")
                                        dataStoreManager.setGymSpreadsheetId("")
                                        showDeleteConfirm = false
                                        isLocked = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("BORRAR", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("CANCELAR", color = TextWhite)
                            }
                        }
                    )
                }

                if (showValidationDialog) {
                    var enteredCode by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showValidationDialog = false },
                        containerColor = Color(0xFF081C24),
                        title = { Text("Seguridad Requerida", color = TextWhite, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Ingrese el código diario para revelar la URL.", color = TextGray, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = enteredCode,
                                    onValueChange = { enteredCode = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (enteredCode.trim().lowercase() == dynamicCode) {
                                        isLocked = false
                                        showValidationDialog = false
                                    } else {
                                        Toast.makeText(context, "Código incorrecto", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                                modifier = Modifier.border(1.dp, accentColor, RoundedCornerShape(14.dp))
                            ) {
                                val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                                Text("VALIDAR", color = contentColor, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showValidationDialog = false }) {
                                Text("CANCELAR", color = TextWhite)
                            }
                        }
                    )
                }
                Text(
                    if (currentLanguage == "Español") "La red se oculta por seguridad, no la comparta." else "The network is hidden for security, do not share it.",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (currentLanguage == "Español") "Asistencia Automática por Wi-Fi" else "Automatic Wi-Fi Attendance",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Red 1
                OutlinedTextField(
                    value = wifiSsidText,
                    onValueChange = {
                        wifiSsidText = it
                        scope.launch { 
                            dataStoreManager.setGymWifiSsid(it) 
                            attendanceRepository.syncGymInfo()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (currentLanguage == "Español") "Red 1 (SSID)" else "Network 1 (SSID)", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Wifi, null, tint = accentColor) },
                    trailingIcon = {
                        if (isScanning && targetFieldForScan == 1) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accentColor)
                        } else {
                            IconButton(onClick = { startScan(1) }) {
                                Icon(Icons.Default.Search, contentDescription = "Scan", tint = accentColor)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                // Red 2
                OutlinedTextField(
                    value = wifiSsid2Text,
                    onValueChange = {
                        wifiSsid2Text = it
                        scope.launch { 
                            dataStoreManager.setGymWifiSsid2(it) 
                            attendanceRepository.syncGymInfo()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (currentLanguage == "Español") "Red 2 (SSID)" else "Network 2 (SSID)", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Wifi, null, tint = accentColor) },
                    trailingIcon = {
                        if (isScanning && targetFieldForScan == 2) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accentColor)
                        } else {
                            IconButton(onClick = { startScan(2) }) {
                                Icon(Icons.Default.Search, contentDescription = "Scan", tint = accentColor)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                // Red 3
                OutlinedTextField(
                    value = wifiSsid3Text,
                    onValueChange = {
                        wifiSsid3Text = it
                        scope.launch { 
                            dataStoreManager.setGymWifiSsid3(it) 
                            attendanceRepository.syncGymInfo()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (currentLanguage == "Español") "Red 3 (SSID)" else "Network 3 (SSID)", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Wifi, null, tint = accentColor) },
                    trailingIcon = {
                        if (isScanning && targetFieldForScan == 3) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accentColor)
                        } else {
                            IconButton(onClick = { startScan(3) }) {
                                Icon(Icons.Default.Search, contentDescription = "Scan", tint = accentColor)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF111926).copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                Text(
                    if (currentLanguage == "Español") "La app marcará asistencia cuando te conectes a esta red." else "The app will mark attendance when you connect to this network.",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (showNetworkDialog) {
            Dialog(onDismissRequest = { showNetworkDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF081C24)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == "Español") "Seleccionar Red" else "Select Network",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (availableNetworks.isEmpty()) {
                            Text(
                                text = if (currentLanguage == "Español") "No se encontraron redes." else "No networks found.",
                                color = TextGray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(availableNetworks) { ssid ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when (targetFieldForScan) {
                                                    1 -> {
                                                        wifiSsidText = ssid
                                                        scope.launch { 
                                                            dataStoreManager.setGymWifiSsid(ssid) 
                                                            attendanceRepository.syncGymInfo()
                                                        }
                                                    }
                                                    2 -> {
                                                        wifiSsid2Text = ssid
                                                        scope.launch { 
                                                            dataStoreManager.setGymWifiSsid2(ssid) 
                                                            attendanceRepository.syncGymInfo()
                                                        }
                                                    }
                                                    3 -> {
                                                        wifiSsid3Text = ssid
                                                        scope.launch { 
                                                            dataStoreManager.setGymWifiSsid3(ssid) 
                                                            attendanceRepository.syncGymInfo()
                                                        }
                                                    }
                                                }
                                                showNetworkDialog = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Wifi, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Text(ssid, color = TextWhite)
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { showNetworkDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (currentLanguage == "Español") "CERRAR" else "CLOSE", color = accentColor)
                        }
                    }
                }
            }
        }
    }
}
