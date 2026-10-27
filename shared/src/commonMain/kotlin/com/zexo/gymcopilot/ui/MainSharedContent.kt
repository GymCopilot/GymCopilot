package com.zexo.gymcopilot.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.decodeBase64ToBitmap
import com.zexo.gymcopilot.utils.isIOSDevice
import com.zexo.gymcopilot.utils.isStandaloneMode
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import com.zexo.gymcopilot.utils.readLocalStorageFlag
import com.zexo.gymcopilot.utils.saveLocalStorageFlag
import com.zexo.gymcopilot.utils.triggerPwaInstall
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

enum class AppScreenState {
    SPLASH,
    LOGIN,
    MAIN_APP
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    iconPainter: Painter,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = PrimaryTurquoise,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = description, fontSize = 12.sp, color = TextGray)
                }
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = PrimaryTurquoise,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SharedGymCopilotApp(
    initialRole: String = "admin",
    gymName: String = "Tu Gimnasio"
) {
    var screenState by remember { mutableStateOf(AppScreenState.SPLASH) }
    var currentRole by remember { mutableStateOf(initialRole.lowercase()) }
    var activeGymName by remember { mutableStateOf(gymName) }
    var showInstallBanner by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var gymLogoUri by remember { mutableStateOf<String?>(null) }
    val isIOS = remember { isIOSDevice() }

    LaunchedEffect(Unit) {
        val isInstalledFlag = readLocalStorageFlag("gymcopilot_pwa_installed") == "true"
        if (!isStandaloneMode() && !isInstalledFlag) {
            showInstallBanner = true
        }
        val localData = readLocalMemberProfile()
        if (localData != null) {
            if (localData.gymName.isNotBlank() && localData.gymName != "Tu Gimnasio") {
                activeGymName = localData.gymName
            }
            if (localData.photoUri != null) {
                gymLogoUri = localData.photoUri
            }
        }
    }

    GymCopilotTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GymBackgroundGradient)
        ) {
            when (screenState) {
                AppScreenState.SPLASH -> {
                    SplashScreenView(
                        gymName = activeGymName,
                        onTimeout = { screenState = AppScreenState.LOGIN }
                    )
                }
                AppScreenState.LOGIN -> {
                    LoginScreenView(
                        gymName = activeGymName,
                        onRoleSelected = { selectedRole ->
                            currentRole = selectedRole
                            screenState = AppScreenState.MAIN_APP
                        }
                    )
                }
                AppScreenState.MAIN_APP -> {
                    MainAppView(
                        gymName = activeGymName,
                        userRole = currentRole,
                        apiClient = GymKtorApiClient,
                        onChangeRole = { screenState = AppScreenState.LOGIN }
                    )
                }
            }

            if (showInstallBanner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(2.dp, PrimaryTurquoise),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00151C))
                                    .border(BorderStroke(2.dp, PrimaryTurquoise), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val logoBm = remember(gymLogoUri) { if (gymLogoUri != null) decodeBase64ToBitmap(gymLogoUri!!) else null }
                                if (logoBm != null) {
                                    Image(bitmap = logoBm, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(40.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Instalar $activeGymName",
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    saveLocalStorageFlag("gymcopilot_pwa_installed", "true")
                                    showInstallBanner = false
                                    val triggered = triggerPwaInstall()
                                    if (!triggered || isIOS) {
                                        showInstructionsDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("📲 INSTALAR AHORA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            if (showInstructionsDialog) {
                AlertDialog(
                    onDismissRequest = { showInstructionsDialog = false },
                    title = { Text("Cómo instalar la app", color = TextWhite, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isIOS) {
                                Text("1. Toca el botón **Compartir** 📤 en la barra inferior de Safari.", color = TextWhite, fontSize = 14.sp)
                                Text("2. Desplázate hacia abajo y selecciona **'Añadir a la pantalla de inicio'** ➕.", color = TextWhite, fontSize = 14.sp)
                            } else {
                                Text("1. Toca el menú de los 3 puntos (⋮) en la esquina superior derecha de tu navegador.", color = TextWhite, fontSize = 14.sp)
                                Text("2. Selecciona **'Instalar aplicación'** o **'Agregar a la pantalla principal'**.", color = TextWhite, fontSize = 14.sp)
                            }
                        }
                    },
                    containerColor = Color(0xFF001F29),
                    confirmButton = {
                        Button(
                            onClick = { showInstructionsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)
                        ) {
                            Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreenView(
    gymName: String,
    onTimeout: () -> Unit
) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(Unit) {
        delay(1200)
        currentOnTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable { currentOnTimeout() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                color = PrimaryTurquoise.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_home),
                    contentDescription = "GymCopilot",
                    tint = PrimaryTurquoise,
                    modifier = Modifier.padding(24.dp).size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = gymName.ifBlank { "GymCopilot" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryTurquoise
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GESTIÓN DE GIMNASIOS Y ASISTENCIA",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp),
                color = PrimaryTurquoise,
                trackColor = SurfaceColor
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Las Malvinas son Argentinas",
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LoginScreenView(
    gymName: String,
    onRoleSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 40.dp)
        ) {
            item {
                Surface(
                    color = PrimaryTurquoise.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.admin_panel_settings),
                        contentDescription = "Logo",
                        tint = PrimaryTurquoise,
                        modifier = Modifier.padding(20.dp).size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Bienvenido a",
                    color = TextGray,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = gymName.ifBlank { "Tu Gimnasio" },
                    color = PrimaryTurquoise,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Selecciona tu rol para continuar",
                    color = TextWhite,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                RoleCard(
                    title = "Administrador",
                    description = "Gestiona tu gimnasio, socios y planes",
                    iconPainter = painterResource(Res.drawable.admin_panel_settings),
                    accentColor = PrimaryTurquoise,
                    onClick = { onRoleSelected("admin") }
                )
            }

            item {
                RoleCard(
                    title = "Profesor",
                    description = "Acceso al panel de entrenamiento",
                    iconPainter = painterResource(Res.drawable.assistant),
                    accentColor = PrimaryTurquoise,
                    onClick = { onRoleSelected("profesor") }
                )
            }

            item {
                RoleCard(
                    title = "Socio del Gimnasio",
                    description = "Sigue tu progreso y tabla",
                    iconPainter = painterResource(Res.drawable.person),
                    accentColor = PrimaryTurquoise,
                    onClick = { onRoleSelected("member") }
                )
            }
        }
    }
}

@Composable
fun MainAppView(
    gymName: String,
    userRole: String,
    apiClient: GymKtorApiClient,
    onChangeRole: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                HeaderBar(
                    gymName = gymName,
                    userRole = userRole,
                    onRoleClick = onChangeRole
                )
            }
        },
        bottomBar = {
            RoleNavigationBar(
                userRole = userRole,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (userRole) {
                "admin" -> RealAdminDashboardView(gymName = gymName, apiClient = apiClient, currentTab = selectedTab, onTabSelected = { selectedTab = it })
                "profesor" -> ProfessorFullDashboard(apiClient = apiClient, currentTab = selectedTab, onNavigate = {})
                else -> MemberFullDashboard(apiClient = apiClient, currentTab = selectedTab, onNavigate = {})
            }
        }
    }
}

@Composable
fun HeaderBar(
    gymName: String,
    userRole: String,
    onRoleClick: () -> Unit
) {
    Surface(
        color = Color(0xFF00151C),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = gymName.ifBlank { "Tu Gimnasio" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTurquoise
                )
                Text(
                    text = "SISTEMA GYMCOPILOT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )
            }

            Surface(
                color = PrimaryTurquoise.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onRoleClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimaryTurquoise)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (userRole) {
                            "admin" -> "ADMIN"
                            "profesor" -> "PROFESOR"
                            else -> "SOCIO"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTurquoise
                    )
                }
            }
        }
    }
}

@Composable
fun RoleNavigationBar(
    userRole: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = when (userRole) {
        "admin" -> listOf("Home", "Store", "Clientes", "Profesores", "Config")
        "profesor" -> listOf("Home", "Tienda", "Horarios", "Alumnos", "Rutinas", "Chat")
        else -> listOf("Home", "Tienda", "Clases", "Rutinas", "Chat")
    }

    NavigationBar(
        modifier = Modifier.height(56.dp),
        containerColor = Color(0xFF00151C),
        contentColor = TextWhite,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            val iconPainter: Painter = when (label) {
                "Home" -> painterResource(Res.drawable.ic_home)
                "Store", "Tienda" -> painterResource(Res.drawable.storefront)
                "Clientes", "Alumnos" -> painterResource(Res.drawable.groups)
                "Profesores" -> painterResource(Res.drawable.assistant)
                "Clases" -> painterResource(Res.drawable.calendar)
                "Rutinas" -> painterResource(Res.drawable.fitness_center)
                "Chat" -> painterResource(Res.drawable.chat)
                "Config" -> painterResource(Res.drawable.settings)
                else -> painterResource(Res.drawable.ic_home)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = label,
                        tint = if (isSelected) PrimaryTurquoise else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = if (isSelected) PrimaryTurquoise else TextGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
