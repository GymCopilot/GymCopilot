package com.zexo.gymcopilot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.zexo.gymcopilot.navigation.GymNavGraph
import com.zexo.gymcopilot.ui.screens.SplashScreen
import com.zexo.gymcopilot.ui.theme.GymCopilotTheme
import com.zexo.gymcopilot.ui.theme.GymBackgroundGradient
import com.zexo.gymcopilot.ui.theme.DarkBackground
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.LocalBottomNavBackgroundColor
import androidx.compose.runtime.CompositionLocalProvider
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.repository.ScheduleRepository
import com.zexo.gymcopilot.repository.GoogleCloudRepository
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.network.BillingManager
import com.zexo.gymcopilot.ui.components.AppLockOverlay
import com.zexo.gymcopilot.utils.WiFiMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🚨 IMPORTANTE PARA AGENTES DE IA 🚨
        // SI REALIZAS CAMBIOS IMPORTANTES, DEBES DOCUMENTARLOS EN: 
        // app/src/main/java/com/zexo/gymcopilot/utils/UpdatesRegistry.kt
        
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isDataLoaded }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val dataStoreManager = remember { DataStoreManager(context) }
            val scope = rememberCoroutineScope()
            
            val gymBackgroundUri by dataStoreManager.getGymBackgroundUri().collectAsState(initial = null)
            val backgroundColorInt by dataStoreManager.getBackgroundColor().collectAsState(initial = null)
            val gymName by dataStoreManager.getGymName().collectAsState(initial = null)
            val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
            val buttonColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
            val buttonBgColorInt by dataStoreManager.getButtonBackgroundColor().collectAsState(initial = null)
            val buttonBgColor = remember(buttonBgColorInt) {
                if (buttonBgColorInt != null && buttonBgColorInt != 0) {
                    Color(buttonBgColorInt!!)
                } else {
                    Color(0xFF00222E)
                }
            }
            val bottomNavBgColorInt by dataStoreManager.getBottomNavBackgroundColor().collectAsState(initial = null)
            val bottomNavBgColor = remember(bottomNavBgColorInt) {
                if (bottomNavBgColorInt != null && bottomNavBgColorInt != 0) {
                    Color(bottomNavBgColorInt!!)
                } else {
                    Color(0xFF00151C)
                }
            }
            val lockExpirationTime by dataStoreManager.getAppLockExpirationTime().collectAsState(initial = 0L)
            
            var isAppLocked by remember { mutableStateOf(false) }
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(gymName) {
                if (gymName != null) {
                    isDataLoaded = true
                }
            }

            LaunchedEffect(Unit) {
                delay(4500) // Duración total para permitir el fade out completo
                showSplash = false
            }

            LaunchedEffect(lockExpirationTime) {
                if (LockConfig.LOCK_DAYS != 0) {
                    val now = System.currentTimeMillis()
                    if (lockExpirationTime == 0L) {
                        // Primer inicio: establecer expiración
                        val days = LockConfig.LOCK_DAYS.toLong()
                        val initialExpiration = if (days > 0) {
                            now + (days * 24L * 60 * 60 * 1000)
                        } else {
                            now - 1000 // Bloqueo inmediato para valores negativos
                        }
                        dataStoreManager.setAppLockExpirationTime(initialExpiration)
                    } else if (now > lockExpirationTime) {
                        isAppLocked = true
                    }
                }
            }
            
            val apiService = remember { NetworkModule.getApiService("") }
            val attendanceRepository = remember { AttendanceRepository(apiService, dataStoreManager) }
            val scheduleRepository = remember { ScheduleRepository(apiService, dataStoreManager) }
            val googleCloudRepository = remember { GoogleCloudRepository(dataStoreManager) }
            val billingManager = remember { BillingManager(context) }
            val userRole by dataStoreManager.getUserRole().collectAsState(initial = null)
            val isSubscribed by billingManager.isSubscribed.collectAsState()
            val gymWifiSsid by dataStoreManager.getGymWifiSsid().collectAsState(initial = "")
            val gymWifiSsid2 by dataStoreManager.getGymWifiSsid2().collectAsState(initial = "")
            val gymWifiSsid3 by dataStoreManager.getGymWifiSsid3().collectAsState(initial = "")

            // Sistema de Monitoreo Wi-Fi para Asistencia Automática
            val wifiMonitor = remember { WiFiMonitor(context) }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                              permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                if (granted) {
                    wifiMonitor.startMonitoring()
                }
            }

            LaunchedEffect(gymWifiSsid, gymWifiSsid2, gymWifiSsid3) {
                if (gymWifiSsid.isNotBlank() || gymWifiSsid2.isNotBlank() || gymWifiSsid3.isNotBlank()) {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasFine || hasCoarse) {
                        wifiMonitor.startMonitoring()
                    } else {
                        locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    wifiMonitor.stopMonitoring()
                }
            }

            // Sistema de Sincronización Automática
            val syncManager = remember { SyncManager(attendanceRepository, dataStoreManager) }
            LaunchedEffect(Unit) {
                syncManager.startSyncLoop()
            }
            DisposableEffect(Unit) {
                onDispose {
                    syncManager.stopSyncLoop()
                }
            }

            // Validamos que el color no sea 0 (transparente) ni null
            val backgroundColor = remember(backgroundColorInt) {
                if (backgroundColorInt != null && backgroundColorInt != 0) Color(backgroundColorInt!!) else null
            }
            
            val accentColor = remember(buttonColor) {
                if (buttonColor != null && buttonColor != 0) Color(buttonColor!!) else PrimaryTurquoise
            }

            GymCopilotTheme {
                CompositionLocalProvider(
                    LocalButtonBackgroundColor provides buttonBgColor,
                    LocalBottomNavBackgroundColor provides bottomNavBgColor
                ) {
                    val navController = rememberNavController()
                    
                    if (showSplash) {
                        SplashScreen(
                            gymName = gymName,
                            gymLogoUri = gymLogoUri,
                            accentColor = accentColor
                        )
                    } else {
                        // 1. Capa Base: Fondo sólido de seguridad para evitar transparencia al sistema
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground)
                        ) {
                            // 2. Capa Decorativa: Gradiente o Color personalizado
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (backgroundColor != null) {
                                            Modifier.background(backgroundColor)
                                        } else {
                                            Modifier.background(GymBackgroundGradient)
                                        }
                                    )
                            ) {
                                // 3. Imagen de fondo (opcional)
                                if (!gymBackgroundUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = gymBackgroundUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.4f
                                    )
                                }

                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = Color.Transparent
                                ) { innerPadding ->
                                    // Se agrega fillMaxSize() para asegurar que el contenido sea visible
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        GymNavGraph(
                                            navController = navController,
                                            attendanceRepository = attendanceRepository,
                                            scheduleRepository = scheduleRepository,
                                            googleCloudRepository = googleCloudRepository,
                                            dataStoreManager = dataStoreManager,
                                            billingManager = billingManager,
                                            initialUserRole = userRole,
                                            initialIsSubscribed = isSubscribed
                                        )
                                    }
                                }

                                // Capa de Bloqueo
                                if (isAppLocked) {
                                    AppLockOverlay(
                                        dataStoreManager = dataStoreManager,
                                        onUnlock = { isAppLocked = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
