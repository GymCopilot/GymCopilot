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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import kotlinx.coroutines.delay

enum class AppScreenState {
    SPLASH,
    LOGIN,
    MAIN_APP
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    iconEmoji: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = iconEmoji, fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(text = description, fontSize = 12.sp, color = TextGray)
            }
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

    GymCopilotTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GymBackgroundGradient)
        ) {
            when (screenState) {
                AppScreenState.SPLASH -> {
                    SplashScreenView(
                        gymName = gymName,
                        onTimeout = { screenState = AppScreenState.LOGIN }
                    )
                }
                AppScreenState.LOGIN -> {
                    LoginScreenView(
                        gymName = gymName,
                        onRoleSelected = { selectedRole ->
                            currentRole = selectedRole
                            screenState = AppScreenState.MAIN_APP
                        }
                    )
                }
                AppScreenState.MAIN_APP -> {
                    MainAppView(
                        gymName = gymName,
                        userRole = currentRole,
                        apiClient = GymKtorApiClient,
                        onChangeRole = { screenState = AppScreenState.LOGIN }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreenView(
    gymName: String,
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
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
                Text(
                    text = "🏋️‍♂️",
                    fontSize = 54.sp,
                    modifier = Modifier.padding(24.dp)
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
                text = "🇦🇷 Las Malvinas son Argentinas",
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
                    Text(
                        text = "🦁",
                        fontSize = 64.sp,
                        modifier = Modifier.padding(20.dp)
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
                    iconEmoji = "🛡️",
                    accentColor = PrimaryTurquoise,
                    onClick = { onRoleSelected("admin") }
                )
            }

            item {
                RoleCard(
                    title = "Profesor",
                    description = "Acceso al panel de entrenamiento",
                    iconEmoji = "💻",
                    accentColor = PrimaryTurquoise,
                    onClick = { onRoleSelected("profesor") }
                )
            }

            item {
                RoleCard(
                    title = "Socio del Gimnasio",
                    description = "Sigue tu progreso y tabla",
                    iconEmoji = "👤",
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
            val iconVector: ImageVector = when (label) {
                "Home" -> Icons.Default.Home
                "Store", "Tienda" -> Icons.Default.ShoppingCart
                "Clientes", "Alumnos" -> Icons.Default.Person
                "Profesores" -> Icons.Default.Person
                "Horarios", "Clases" -> Icons.Default.DateRange
                "Rutinas" -> Icons.Default.Star
                "Chat" -> Icons.Default.Email
                else -> Icons.Default.Settings
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
                        imageVector = iconVector,
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
