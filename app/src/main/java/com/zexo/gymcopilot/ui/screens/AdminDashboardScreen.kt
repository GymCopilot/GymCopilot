package com.zexo.gymcopilot.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.repository.ScheduleRepository
import com.zexo.gymcopilot.repository.GoogleCloudRepository
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import com.zexo.gymcopilot.ui.theme.getGymFontFamily
import com.zexo.gymcopilot.utils.ShortcutUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onAppSettingsClick: () -> Unit = {},
    onAppearanceClick: (String?) -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: (String?) -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onSubscriptionsClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null,
    scheduleRepository: ScheduleRepository? = null,
    googleCloudRepository: GoogleCloudRepository? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val googleToken by dataStoreManager.getGoogleAccessToken().collectAsState(initial = "")
    val assistantEnabled by dataStoreManager.getAssistantEnabled().collectAsState(initial = false)
    val gymIsOpen by dataStoreManager.getGymIsOpen().collectAsState(initial = true)
    
    val welcomeDismissed by dataStoreManager.getWelcomeDismissed().collectAsState(initial = false)
    val cloudHelpDismissed by dataStoreManager.getCloudHelpDismissed().collectAsState(initial = false)
    val step2HelpDismissed by dataStoreManager.getStep2HelpDismissed().collectAsState(initial = false)
    val delayHelpDismissed by dataStoreManager.getDelayHelpDismissed().collectAsState(initial = false)
    val readyDismissed by dataStoreManager.getReadyDismissed().collectAsState(initial = false)
    val dashboardTourStep1Dismissed by dataStoreManager.getDashboardTourStep1Dismissed().collectAsState(initial = false)
    val dashboardTourStep1CentralDismissed by dataStoreManager.getDashboardTourStep1CentralDismissed().collectAsState(initial = false)
    val dashboardTourBroadcastDismissed by dataStoreManager.getDashboardTourBroadcastDismissed().collectAsState(initial = false)
    val dashboardTourPlansDismissed by dataStoreManager.getDashboardTourPlansDismissed().collectAsState(initial = false)
    val dashboardTourIncomeDismissed by dataStoreManager.getDashboardTourIncomeDismissed().collectAsState(initial = false)
    val dashboardTourMetricsDismissed by dataStoreManager.getDashboardTourMetricsDismissed().collectAsState(initial = false)
    val dashboardTourScheduleDismissed by dataStoreManager.getDashboardTourScheduleDismissed().collectAsState(initial = false)
    val dashboardTourQrDismissed by dataStoreManager.getDashboardTourQrDismissed().collectAsState(initial = false)
    val dashboardTourProfessorsDismissed by dataStoreManager.getDashboardTourProfessorsDismissed().collectAsState(initial = false)
    val dashboardTourMembersNavDismissed by dataStoreManager.getDashboardTourMembersNavDismissed().collectAsState(initial = false)
    val dashboardTourStoreNavDismissed by dataStoreManager.getDashboardTourStoreNavDismissed().collectAsState(initial = false)

    val copiFontFamily = remember { FontFamily(Font(R.font.copi)) }
    var showWelcomeBubble by remember { mutableStateOf(false) }
    var showCloudBubble by remember { mutableStateOf(false) }
    var showStep2Bubble by remember { mutableStateOf(false) }
    var showDelayBubble by remember { mutableStateOf(false) }
    var showAuthBubble by remember { mutableStateOf(false) }
    var showReadyBubble by remember { mutableStateOf(false) }
    var showTourStep1Bubble by remember { mutableStateOf(false) }
    var showBroadcastTourBubble by remember { mutableStateOf(false) }
    var showPlansTourBubble by remember { mutableStateOf(false) }
    var showIncomeTourBubble by remember { mutableStateOf(false) }
    var showMetricsTourBubble by remember { mutableStateOf(false) }
    var showScheduleTourBubble by remember { mutableStateOf(false) }
    var showQrTourBubble by remember { mutableStateOf(false) }
    var showProfessorsTourBubble by remember { mutableStateOf(false) }
    var showMembersNavTourBubble by remember { mutableStateOf(false) }
    var showStoreNavTourBubble by remember { mutableStateOf(false) }

    var isSettingUp by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var connectionVerified by remember { mutableStateOf(false) }
    var showGymNameRequiredDialog by remember { mutableStateOf(false) }
    var showConfirmCreateBackendDialog by remember { mutableStateOf(false) }

    // PASO 1: Bienvenida
    LaunchedEffect(assistantEnabled, welcomeDismissed, lifecycleOwner) {
        if (assistantEnabled && !welcomeDismissed) {
            delay(1200)
            showWelcomeBubble = true
        } else {
            showWelcomeBubble = false
        }
    }

    // PASO 2: Vinculación Google (Izquierda)
    LaunchedEffect(assistantEnabled, welcomeDismissed, cloudHelpDismissed, googleToken, gymApiUrl) {
        if (assistantEnabled && welcomeDismissed && !cloudHelpDismissed && googleToken.isEmpty() && gymApiUrl.isBlank()) {
            delay(1200)
            showCloudBubble = true
        } else {
            showCloudBubble = false
        }
    }

    // PASO 3: Crear Backend (Derecha)
    LaunchedEffect(assistantEnabled, welcomeDismissed, step2HelpDismissed, googleToken, gymApiUrl, isSettingUp) {
        if (assistantEnabled && welcomeDismissed && !step2HelpDismissed && googleToken.isNotEmpty() && gymApiUrl.isBlank() && !isSettingUp) {
            delay(1200)
            showStep2Bubble = true
        } else {
            showStep2Bubble = false
        }
    }

    // PASO 3.5: Demora (Derecha)
    LaunchedEffect(assistantEnabled, isSettingUp, delayHelpDismissed) {
        if (assistantEnabled && isSettingUp && !delayHelpDismissed && !showAuthDialog) {
            delay(5000)
            if (isSettingUp) showDelayBubble = true
        } else {
            showDelayBubble = false
        }
    }

    // PASO 5: Autorización (Derecha)
    LaunchedEffect(assistantEnabled, showAuthDialog) {
        if (assistantEnabled && showAuthDialog) {
            delay(800)
            showAuthBubble = true
        } else {
            showAuthBubble = false
        }
    }

    // PASO FINAL: Éxito (Derecha) - Solo si la conexión está verificada y no hay procesos activos
    LaunchedEffect(assistantEnabled, welcomeDismissed, readyDismissed, gymApiUrl, isSettingUp, showAuthDialog, connectionVerified) {
        if (assistantEnabled && welcomeDismissed && !readyDismissed && gymApiUrl.isNotBlank() && !isSettingUp && !showAuthDialog && connectionVerified) {
            delay(1200)
            showReadyBubble = true
        } else {
            showReadyBubble = false
        }
    }

    // TOUR PASO 1: Identidad del Gimnasio (Fase 1: Central)
    LaunchedEffect(assistantEnabled, welcomeDismissed, connectionVerified, readyDismissed, dashboardTourStep1CentralDismissed) {
        if (assistantEnabled && welcomeDismissed && connectionVerified && readyDismissed && !dashboardTourStep1CentralDismissed) {
            delay(1500)
            showTourStep1Bubble = true
        } else {
            showTourStep1Bubble = false
        }
    }

    // TOUR PASO 2: Mensajes Generales (Se activa después del paso de Identidad)
    LaunchedEffect(assistantEnabled, dashboardTourStep1Dismissed, dashboardTourBroadcastDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourStep1Dismissed && !dashboardTourBroadcastDismissed) {
            delay(1500)
            showBroadcastTourBubble = true
        } else {
            showBroadcastTourBubble = false
        }
    }

    // TOUR PASO 3: Planes (Se activa después del paso de Mensajes)
    LaunchedEffect(assistantEnabled, dashboardTourBroadcastDismissed, dashboardTourPlansDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourBroadcastDismissed && !dashboardTourPlansDismissed) {
            delay(1500)
            showPlansTourBubble = true
        } else {
            showPlansTourBubble = false
        }
    }

    // TOUR PASO 4: Ingresos (Se activa después del paso de Planes)
    LaunchedEffect(assistantEnabled, dashboardTourPlansDismissed, dashboardTourIncomeDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourPlansDismissed && !dashboardTourIncomeDismissed) {
            delay(1500)
            showIncomeTourBubble = true
        } else {
            showIncomeTourBubble = false
        }
    }

    // TOUR PASO 5: Métricas (Se activa después del paso de Ingresos)
    LaunchedEffect(assistantEnabled, dashboardTourIncomeDismissed, dashboardTourMetricsDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourIncomeDismissed && !dashboardTourMetricsDismissed) {
            delay(1500)
            showMetricsTourBubble = true
        } else {
            showMetricsTourBubble = false
        }
    }

    // TOUR PASO 6: Horarios (Se activa después del paso de Métricas)
    LaunchedEffect(assistantEnabled, dashboardTourMetricsDismissed, dashboardTourScheduleDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourMetricsDismissed && !dashboardTourScheduleDismissed) {
            delay(1500)
            showScheduleTourBubble = true
        } else {
            showScheduleTourBubble = false
        }
    }

    // TOUR PASO 7: QR Asistencia (Se activa después del paso de Horarios)
    LaunchedEffect(assistantEnabled, dashboardTourScheduleDismissed, dashboardTourQrDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourScheduleDismissed && !dashboardTourQrDismissed) {
            delay(1500)
            showQrTourBubble = true
        } else {
            showQrTourBubble = false
        }
    }

    // TOUR PASO 8: Profesores (Se activa después del paso de QR)
    LaunchedEffect(assistantEnabled, dashboardTourQrDismissed, dashboardTourProfessorsDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourQrDismissed && !dashboardTourProfessorsDismissed) {
            delay(1500)
            showProfessorsTourBubble = true
        } else {
            showProfessorsTourBubble = false
        }
    }

    // TOUR PASO 9: Clientes Nav (Se activa después del paso de Profesores)
    LaunchedEffect(assistantEnabled, dashboardTourProfessorsDismissed, dashboardTourMembersNavDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourProfessorsDismissed && !dashboardTourMembersNavDismissed) {
            delay(1500)
            showMembersNavTourBubble = true
        } else {
            showMembersNavTourBubble = false
        }
    }

    // TOUR PASO 10: Tienda Nav (Se activa después del paso de Clientes)
    LaunchedEffect(assistantEnabled, dashboardTourMembersNavDismissed, dashboardTourStoreNavDismissed, lifecycleOwner) {
        if (assistantEnabled && dashboardTourMembersNavDismissed && !dashboardTourStoreNavDismissed) {
            delay(1500)
            showStoreNavTourBubble = true
        } else {
            showStoreNavTourBubble = false
        }
    }

    val appCurrency by dataStoreManager.getAppCurrency().collectAsState(initial = "Peso Argentino")
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    val showLogoBorder by dataStoreManager.getShowLogoBorder().collectAsState(initial = true)
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymNameFont by dataStoreManager.getGymNameFont().collectAsState(initial = "Default")

    val shortcutCreated by dataStoreManager.isShortcutCreated().collectAsState(initial = false)
    val lastShortcutName by dataStoreManager.getLastShortcutName().collectAsState(initial = "")
    val lastShortcutLogoUri by dataStoreManager.getLastShortcutLogoUri().collectAsState(initial = "")

    LaunchedEffect(gymName, gymLogoUri, shortcutCreated, lastShortcutName, lastShortcutLogoUri) {
        if (gymName.isBlank()) return@LaunchedEffect
        
        if (!shortcutCreated) {
            ShortcutUtils.createGymShortcut(context, gymName, gymLogoUri)
            dataStoreManager.setShortcutCreated(true)
            dataStoreManager.setLastShortcutName(gymName)
            dataStoreManager.setLastShortcutLogoUri(gymLogoUri ?: "")
        } else {
            // Actualización silenciosa si hubo cambios
            if (gymName != lastShortcutName || (gymLogoUri ?: "") != lastShortcutLogoUri) {
                ShortcutUtils.updateGymShortcut(context, gymName, gymLogoUri)
                dataStoreManager.setLastShortcutName(gymName)
                dataStoreManager.setLastShortcutLogoUri(gymLogoUri ?: "")
            }
        }
    }

    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val paymentRecords by dataStoreManager.getPaymentRecords().collectAsState(initial = emptyList())
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val selectedFontFamily = remember(gymNameFont) { getGymFontFamily(gymNameFont) }
    val uriHandler = LocalUriHandler.current

    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var setupErrorMessage by remember { mutableStateOf<String?>(null) }
    var showMonthlyIncome by remember { mutableStateOf(false) }

    var pendingUrl by remember { mutableStateOf("") }

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"), 
                Scope("https://www.googleapis.com/auth/spreadsheets"),
                Scope("https://www.googleapis.com/auth/script.projects"),
                Scope("https://www.googleapis.com/auth/script.deployments"),
                Scope("https://www.googleapis.com/auth/script.external_request")
            )
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (email != null) {
                coroutineScope.launch {
                    try {
                        dataStoreManager.setUserEmail(email)
                        val scope = "oauth2:https://www.googleapis.com/auth/drive.file " +
                                   "https://www.googleapis.com/auth/spreadsheets " +
                                   "https://www.googleapis.com/auth/script.projects " +
                                   "https://www.googleapis.com/auth/script.deployments " +
                                   "https://www.googleapis.com/auth/script.external_request"
                        val token = withContext(Dispatchers.IO) {
                            com.google.android.gms.auth.GoogleAuthUtil.getToken(context, email, scope)
                        }
                        dataStoreManager.setGoogleAccessToken(token)
                        Toast.makeText(context, "Cuenta vinculada con éxito", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("GoogleAuth", "Error al obtener Access Token: ${e.message}")
                        Toast.makeText(context, "Error al obtener permisos de Google", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("GoogleAuth", "Error de Sign-In: Código ${e.statusCode}, Mensaje: ${e.message}")
            val errorMsg = when(e.statusCode) {
                10 -> "Error 10: Revisa el SHA-1 en Google Cloud Console"
                7 -> "Error de red"
                12501 -> "Vinculación cancelada"
                else -> "Error de vinculación (${e.statusCode})"
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error inesperado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(attendanceRepository, scheduleRepository, gymApiUrl) {
        if (gymApiUrl.isNotBlank()) {
            while (isActive) {
                val res1 = attendanceRepository?.syncWithRemote()
                val res2 = attendanceRepository?.syncGymConfigFromServer()
                val res3 = attendanceRepository?.syncMembersFromServer()
                val res4 = attendanceRepository?.syncProfessorsFromServer()
                val res5 = scheduleRepository?.syncSchedules()

                val results = listOf(res1, res2, res3, res4, res5)
                val is403 = results.any { it?.exceptionOrNull()?.message?.contains("403") == true }
                
                if (is403) {
                    connectionVerified = false
                    if (!showAuthDialog) {
                        pendingUrl = gymApiUrl
                        showAuthDialog = true
                    }
                } else if (results.any { it?.isSuccess == true }) {
                    connectionVerified = true
                }
                
                delay(15000)
            }
        } else {
            connectionVerified = false
        }
    }

    val currencySymbol = remember(appCurrency) {
        when (appCurrency) {
            "Peso Argentino", "Dólar", "Peso Mexicano", "Peso Chileno", "Peso Colombiano", "Real" -> "$"
            "Euro" -> "€"
            else -> "$"
        }
    }

    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val monthlyIncomeValue = remember(paymentRecords, currentMonth, currentYear) {
        val total = paymentRecords.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
        if (total == 0.0) "0.0"
        else if (total % 1.0 == 0.0) total.toInt().toString() 
        else String.format(Locale.US, "%.1f", total)
    }

    val t = remember(appLanguage, memberDesignation) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Panel Admin", "occ" to "Aforo", "cloud_title" to "Base de Datos",
                "cloud_desc" to "Crea tu base de datos en Google Drive automáticamente.",
                "cloud_auth" to "1. Vincular Google", "cloud_setup" to "2. CREAR BASE DE DATOS",
                "search" to "Buscar $memberDesignation...", "recent" to "Actividad Reciente",
                "qr_title" to "QR del Gimnasio", "open" to "ABIERTO", "closed" to "CERRADO",
                "total" to "TOTAL", "active" to "ACTIVOS", "debtors" to "DEUDORES",
                "inc_m" to "Ingresos", "subs" to "Suscripciones", "new" to "Nuevos (Mes)", 
                "ast" to "Asistencia Hoy", "gst" to "Horarios",
                "show_qr" to "QR Admin", "scan_member" to "Asistencia",
                "cancel" to "CANCELAR", "qr_instr" to "Apuntá al QR del $memberDesignation",
                "ast_success" to "Asistencia registrada", "not_found" to "${memberDesignation} no encontrado",
                "sync_error" to "Error al subir a la planilla",
                "cloud_help" to "Para empezar toca el botón azul para crear tu base de datos en la nube.",
                "step2_help" to "¡Excelente! Ahora toca el botón verde para crear tu base de datos.",
                "delay_help" to "Esto puede tardar un poquito por ser la primera vez, ¡no te desesperes!",
                "auth_help" to "¡Casi listo! Google requiere tu permiso. Si tienes dudas, ve a Ayudante COPI > Guía de Autorización para ver los pasos gráficos.",
                "panel_ready" to "¡Excelente! Ya tienes todo configurado. ¡A darle con todo!",
                "tour_step1" to "¡Genial! Empecemos configurando la identidad de tu gimnasio que aparecerá acá arriba.",
                "tour_step1_hand" to "Toca el botón de configuración",
                "tour_broadcast" to "Desde aquí puedes escribir un mensaje general para todos tus ${memberDesignation.lowercase()}s, el cual verán al ingresar.",
                "tour_plans" to "Antes de seguir, debes configurar al menos un plan para que se puedan ver los ingresos de dinero.",
                "tour_income" to "Los ingresos se calculan sumando automáticamente todos los pagos que registres en la sección de ${memberDesignation.lowercase()}s durante el mes en curso.",
                "tour_metrics" to "Aquí tienes un resumen rápido de tus ${memberDesignation.lowercase()}s: totales, nuevos, deudores y activos.",
                "tour_schedule" to "Desde aquí puedes gestionar los horarios y clases. Recuerda que tanto tú como los profesores autorizados pueden realizar estas configuraciones.",
                "tour_qr_desc" to "Muestra este QR a tus ${memberDesignation.lowercase()}s. Al escanearlo con su app, se vincularán a tu gimnasio y podrán registrar su asistencia automáticamente.",
                "tour_profs" to "Aquí puedes gestionar a tus profesores, ver sus datos y comunicarte con ellos a través de los chats.",
                "tour_members_nav" to "Aquí puedes gestionar a tus ${memberDesignation.lowercase()}s, ver sus pagos, asistencias y asignarles planes de entrenamiento.",
                "tour_store_nav" to "Desde aquí puedes gestionar los productos de tu tienda, ver las ventas y mostrárselos a tus ${memberDesignation.lowercase()}s.",
                "btn_hi" to "¡Hola COPI!",
                "btn_ok" to "¡Entendido!",
                "btn_letsgo" to "¡Vamos!",
                "btn_great" to "¡Genial!"
            )
        } else {
            mapOf(
                "title" to "Admin Panel", "occ" to "Occupancy", "cloud_title" to "Database",
                "cloud_desc" to "Create your database on Google Drive automatically.",
                "cloud_auth" to "1. Link Google", "cloud_setup" to "2. CREATE DATABASE",
                "search" to "Search $memberDesignation...", "recent" to "Recent Activity",
                "qr_title" to "Gym QR Code", "open" to "OPEN", "closed" to "CLOSED",
                "total" to "TOTAL", "active" to "ACTIVE", "debtors" to "DEBTORS",
                "inc_m" to "Income", "subs" to "Subscriptions", "new" to "New members", 
                "ast" to "Attendance", "gst" to "Schedule",
                "show_qr" to "Gym QR", "scan_member" to "Attendance",
                "cancel" to "CANCEL", "qr_instr" to "Point at the member QR",
                "ast_success" to "Attendance recorded", "not_found" to "${memberDesignation} not found",
                "sync_error" to "Error uploading to sheet",
                "cloud_help" to "To get started, tap the blue button to create your cloud database.",
                "step2_help" to "Excellent! Now tap the green button to create your database.",
                "delay_help" to "This might take a while the first time, don't despair!",
                "auth_help" to "Almost ready! Google needs your permission. If you have questions, go to COPI Assistant > Authorization Guide to see the graphic steps.",
                "panel_ready" to "Excellent! You have everything set up. Let's do this!",
                "tour_step1" to "Great! Let's start by setting up your gym's identity which will appear up here.",
                "tour_step1_hand" to "Tap the settings button",
                "tour_broadcast" to "From here you can write a general message for all your ${memberDesignation.lowercase()}s, which they will see when they enter.",
                "tour_plans" to "Before continuing, you must set up at least one plan so that income can be seen.",
                "tour_income" to "Income is calculated by automatically adding up all the payments you register in the ${memberDesignation.lowercase()}s section during the current month.",
                "tour_metrics" to "Here you have a quick summary of your ${memberDesignation.lowercase()}s: total, new, debtors and active.",
                "tour_schedule" to "From here you can manage schedules and classes. Remember that both you and authorized teachers can make these settings.",
                "tour_qr_desc" to "Show this QR to your ${memberDesignation.lowercase()}s. By scanning it with their app, they will link to your gym and be able to record their attendance automatically.",
                "tour_profs" to "Here you can manage your teachers, see their data and communicate with them through chats.",
                "tour_members_nav" to "Here you can manage your ${memberDesignation.lowercase()}s, see their payments, attendance and assign them training plans.",
                "tour_store_nav" to "From here you can manage the products in your store, see sales and show them to your ${memberDesignation.lowercase()}s.",
                "btn_hi" to "Hi COPI!",
                "btn_ok" to "Got it!",
                "btn_letsgo" to "Let's go!",
                "btn_great" to "Great!"
            )
        }
    }

    val activeCount = members.count { it.membershipStatus == "ACTIVO" || it.membershipStatus == "ACTIVE" }
    val overdueCount = members.count { it.membershipStatus == "DEUDOR" || it.membershipStatus == "DEBTOR" || it.membershipStatus == "VENCIDO" || it.membershipStatus == "EXPIRED" }
    
    val newCount = members.count {
        val regCal = Calendar.getInstance().apply { timeInMillis = it.registrationDate }
        regCal.get(Calendar.MONTH) == currentMonth && regCal.get(Calendar.YEAR) == currentYear
    }

    val attendanceTodayCount = members.count { member ->
        member.lastVisit?.let { last ->
            val lastCal = Calendar.getInstance().apply { timeInMillis = last }
            val todayCal = Calendar.getInstance()
            lastCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
            lastCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        } ?: false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> 
        if (isGranted) showScanner = true 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).statusBarsPadding()) {
                        AdminHeader(
                            logoUri = gymLogoUri,
                            gymName = gymName,
                            font = selectedFontFamily,
                            accentColor = accentColor,
                            shape = containerShape,
                            title = t["title"] ?: "",
                            showBorder = showLogoBorder,
                            statusText = if (gymIsOpen) (t["open"] ?: "OPEN") else (t["closed"] ?: "CLOSED"),
                            statusColor = if (gymIsOpen) Color(0xFF00C853) else Color(0xFFF44336),
                            onStatusClick = {
                                coroutineScope.launch {
                                    val newState = !gymIsOpen
                                    dataStoreManager.setGymIsOpen(newState)
                                    attendanceRepository?.syncGymInfo(gymIsOpen = newState)
                                }
                            }
                        )
                    }
                }
            },
            bottomBar = {
                AdminBottomNavigation(
                    currentRoute = "admin_dashboard",
                    onHomeClick = { },
                    onStoreClick = {
                        if (dashboardTourMembersNavDismissed && !dashboardTourStoreNavDismissed) {
                            coroutineScope.launch { dataStoreManager.setDashboardTourStoreNavDismissed(true) }
                        }
                        onStoreClick()
                    },
                    onMembersClick = { 
                        if (dashboardTourProfessorsDismissed && !dashboardTourMembersNavDismissed) {
                            coroutineScope.launch { dataStoreManager.setDashboardTourMembersNavDismissed(true) }
                        }
                        onMembersClick(null) 
                    },
                    onProfessorsClick = {
                        if (dashboardTourQrDismissed && !dashboardTourProfessorsDismissed) {
                            coroutineScope.launch { dataStoreManager.setDashboardTourProfessorsDismissed(true) }
                        }
                        onProfessorsClick()
                    },
                    onSettingsClick = {
                        // Si la mano está visible, la descartamos al tocar el botón
                        if (dashboardTourStep1CentralDismissed && !dashboardTourStep1Dismissed) {
                            coroutineScope.launch { dataStoreManager.setDashboardTourStep1Dismissed(true) }
                        }
                        onAppSettingsClick()
                    },
                    accentColor = accentColor
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                // --- CONFIGURACIÓN INMEDIATA (SOLO SI NO HAY BACKEND) ---
                if (gymApiUrl.isBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(2.dp, accentColor, containerShape),
                            shape = containerShape,
                            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudQueue, null, tint = accentColor, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(t["cloud_title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(t["cloud_desc"] ?: "", color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))

                                if (googleToken.isNullOrBlank()) {
                                    Button(
                                        onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = containerShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(t["cloud_auth"] ?: "", color = Color.Black, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (gymName.isBlank()) {
                                                showGymNameRequiredDialog = true
                                            } else {
                                                showConfirmCreateBackendDialog = true
                                            }
                                        },
                                        enabled = !isSettingUp,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        shape = containerShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSettingUp) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        else Text(t["cloud_setup"] ?: "2. CREAR BASE DE DATOS", color = Color.White, fontWeight = FontWeight.Black)
                                    }

                                    TextButton(onClick = {
                                        coroutineScope.launch {
                                            try {
                                                googleCloudRepository?.resetGymBackend(gymName)
                                            } catch (e: Exception) {
                                                Log.e("AdminDashboard", "Error reseteando backend: ${e.message}")
                                            }
                                            dataStoreManager.clearAllData()
                                            Toast.makeText(context, "Base de datos eliminada y cuenta desvinculada", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("Desvincular cuenta y borrar datos", color = Color.Red.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item { 
                    Box(modifier = Modifier.fillMaxWidth()) {
                        GymBroadcastEditor(
                            accentColor = accentColor,
                            containerShape = containerShape,
                            dataStoreManager = dataStoreManager,
                            repository = attendanceRepository,
                            coroutineScope = coroutineScope
                        )

                        // --- TOUR COPI: MENSAJES GENERALES (OVERLAY FLOTANTE) ---
                        // Usamos un Box de altura 0 que permite a sus hijos dibujarse sin límites (unbounded)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .height(0.dp)
                                .zIndex(10f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            AnimatedVisibility(
                                visible = assistantEnabled && dashboardTourStep1Dismissed && !dashboardTourBroadcastDismissed,
                                enter = fadeIn(tween(1000)) + scaleIn(tween(1000)),
                                exit = fadeOut(tween(800)) + scaleOut(tween(800)),
                                // Offset negativo para flotar ARRIBA del recuadro sin ocupar espacio
                                modifier = Modifier
                                    .offset(y = (-120).dp)
                                    .wrapContentSize(unbounded = true)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "broadcastHandBounce")
                                val handOffsetY by infiniteTransition.animateValue(
                                    initialValue = 0.dp,
                                    targetValue = 15.dp,
                                    typeConverter = Dp.VectorConverter,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "handOffsetY"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(280.dp)
                                ) {
                                    // GLOBO DE DIÁLOGO (ARRIBA DE LA MANO)
                                    AnimatedVisibility(
                                        visible = showBroadcastTourBubble,
                                        enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                                        exit = fadeOut(tween(500))
                                    ) {
                                        val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                                        Surface(
                                            shape = bubbleShape,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .widthIn(max = 260.dp)
                                                .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp).padding(bottom = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = t["tour_broadcast"] ?: "",
                                                    color = Color.Black,
                                                    fontSize = 13.sp,
                                                    fontFamily = copiFontFamily,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 16.sp
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                Button(
                                                    onClick = { 
                                                        coroutineScope.launch {
                                                            dataStoreManager.setDashboardTourBroadcastDismissed(true)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                    shape = containerShape,
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                                ) {
                                                    Text(t["btn_great"] ?: "¡Genial!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }

                                    // MANO PUNTERO (ABAJO DEL GLOBO, SEÑALANDO AL RECUADRO)
                                    Image(
                                        painter = painterResource(id = R.drawable.copi_07),
                                        contentDescription = "Mano Puntero Mensajes",
                                        modifier = Modifier
                                            .size(75.dp)
                                            .offset(y = handOffsetY)
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MetricsGridSection(listOf(
                            MetricData(t["subs"] ?: "", "PLANES", Icons.Default.CardMembership, accentColor) { onSubscriptionsClick() },
                            MetricData(
                                label = t["inc_m"] ?: "", 
                                value = if (showMonthlyIncome) "$currencySymbol$monthlyIncomeValue" else "****", 
                                icon = Icons.Default.AccountBalanceWallet, 
                                color = Color(0xFF4CAF50),
                                isSecure = true,
                                isVisible = showMonthlyIncome,
                                onToggleVisibility = { showMonthlyIncome = !showMonthlyIncome }
                            ),
                            MetricData(t["total"] ?: "", members.size.toString(), Icons.Default.Groups, accentColor) { onMembersClick(null) },
                            MetricData(t["new"] ?: "", newCount.toString(), Icons.Default.PersonAdd, Color(0xFFFFC107)),
                            MetricData(t["debtors"] ?: "", overdueCount.toString(), Icons.Default.Warning, Color(0xFFF44336)) { onMembersClick("DEUDOR") },
                            MetricData(t["active"] ?: "", activeCount.toString(), Icons.Default.Person, Color(0xFF4CAF50)) { onMembersClick("ACTIVO") },
                            MetricData(t["ast"] ?: "", attendanceTodayCount.toString(), Icons.AutoMirrored.Filled.FactCheck, Color(0xFF9C27B0)),
                            MetricData("Gestión", "HORARIOS", Icons.Default.Schedule, Color(0xFFE91E63)) { onScheduleClick() },
                            MetricData(t["show_qr"] ?: "Gym QR", "ASISTENCIA", Icons.Default.QrCode2, accentColor) { showQrDialog = true },
                            MetricData(t["scan_member"] ?: "Asistencia", "SCAN", Icons.Default.QrCodeScanner, accentColor) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    showScanner = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        ), accentColor, containerShape)

                        // --- TOUR COPI: PLANES (OVERLAY FLOTANTE) ---
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .height(0.dp)
                                .zIndex(10f),
                            contentAlignment = Alignment.TopStart
                        ) {
                            AnimatedVisibility(
                                visible = assistantEnabled && dashboardTourBroadcastDismissed && !dashboardTourPlansDismissed,
                                enter = fadeIn(tween(1000)) + scaleIn(tween(1000)),
                                exit = fadeOut(tween(800)) + scaleOut(tween(800)),
                                modifier = Modifier
                                    .offset(x = 90.dp, y = 160.dp) 
                                    .wrapContentSize(unbounded = true)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(220.dp)
                                ) {
                                    // PERSONAJE COPI (copi_03)
                                    Image(
                                        painter = painterResource(id = R.drawable.copi_03),
                                        contentDescription = "Copi Planes",
                                        modifier = Modifier.size(160.dp)
                                    )

                                    // GLOBO DE DIÁLOGO (ABAJO)
                                    AnimatedVisibility(
                                        visible = showPlansTourBubble,
                                        enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Top),
                                        exit = fadeOut(tween(500))
                                    ) {
                                        val bubbleShape = remember { SpeechBubbleTopShape(cornerRadius = 15.dp.value * 2f) }
                                        Surface(
                                            shape = bubbleShape,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .widthIn(max = 200.dp)
                                                .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp).padding(top = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = t["tour_plans"] ?: "",
                                                    color = Color.Black,
                                                    fontSize = 12.sp,
                                                    fontFamily = copiFontFamily,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 15.sp
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                Button(
                                                    onClick = { 
                                                        coroutineScope.launch {
                                                            dataStoreManager.setDashboardTourPlansDismissed(true)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                    shape = containerShape,
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                ) {
                                                    Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- TOUR COPI: INGRESOS (OVERLAY FLOTANTE) ---
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .height(0.dp)
                                    .zIndex(10f),
                                contentAlignment = Alignment.TopStart
                            ) {
                                    AnimatedVisibility(
                                        visible = assistantEnabled && dashboardTourPlansDismissed && !dashboardTourIncomeDismissed,
                                        enter = fadeIn(tween(1000)) + scaleIn(tween(1000)),
                                        exit = fadeOut(tween(800)) + scaleOut(tween(800)),
                                        modifier = Modifier
                                            .offset(x = 30.dp, y = 160.dp) // MOVIDO 60DP A LA DERECHA (-30 -> 30)
                                            .wrapContentSize(unbounded = true)
                                    ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(220.dp)
                                    ) {
                                        // PERSONAJE COPI (copi_02)
                                        Image(
                                            painter = painterResource(id = R.drawable.copi_02),
                                            contentDescription = "Copi Ingresos",
                                            modifier = Modifier.size(160.dp)
                                        )

                                        // GLOBO DE DIÁLOGO (ABAJO)
                                        AnimatedVisibility(
                                            visible = showIncomeTourBubble,
                                            enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Top),
                                            exit = fadeOut(tween(500))
                                        ) {
                                            val bubbleShape = remember { SpeechBubbleTopShape(cornerRadius = 15.dp.value * 2f) }
                                            Surface(
                                                shape = bubbleShape,
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier
                                                    .widthIn(max = 200.dp)
                                                    .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp).padding(top = 12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = t["tour_income"] ?: "",
                                                        color = Color.Black,
                                                        fontSize = 12.sp,
                                                        fontFamily = copiFontFamily,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 15.sp
                                                    )
                                                    Spacer(Modifier.height(10.dp))
                                                    Button(
                                                        onClick = { 
                                                            coroutineScope.launch {
                                                                dataStoreManager.setDashboardTourIncomeDismissed(true)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                        shape = containerShape,
                                                        modifier = Modifier.height(28.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                    ) {
                                                        Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // --- TOUR COPI: MÉTRICAS (NUEVA ANIMACIÓN) ---
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .height(0.dp)
                                        .zIndex(10f),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    AnimatedVisibility(
                                        visible = assistantEnabled && dashboardTourIncomeDismissed && !dashboardTourMetricsDismissed,
                                        enter = fadeIn(tween(1000)) + scaleIn(tween(1000)),
                                        exit = fadeOut(tween(800)) + scaleOut(tween(800)),
                                        modifier = Modifier
                                            .offset(x = 80.dp, y = (-40).dp) // MOVIDO 10DP MÁS A LA DERECHA (70 -> 80)
                                            .wrapContentSize(unbounded = true)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(220.dp)
                                        ) {
                                            // GLOBO DE DIÁLOGO (ARRIBA)
                                            AnimatedVisibility(
                                                visible = showMetricsTourBubble,
                                                enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                                                exit = fadeOut(tween(500))
                                            ) {
                                                val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                                                Surface(
                                                    shape = bubbleShape,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier
                                                        .widthIn(max = 200.dp)
                                                        .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp).padding(bottom = 12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = t["tour_metrics"] ?: "",
                                                            color = Color.Black,
                                                            fontSize = 12.sp,
                                                            fontFamily = copiFontFamily,
                                                            textAlign = TextAlign.Center,
                                                            lineHeight = 15.sp
                                                        )
                                                        Spacer(Modifier.height(10.dp))
                                                        Button(
                                                            onClick = { 
                                                                coroutineScope.launch {
                                                                    dataStoreManager.setDashboardTourMetricsDismissed(true)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                            shape = containerShape,
                                                            modifier = Modifier.height(28.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                        ) {
                                                            Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            // PERSONAJE COPI (copi_06)
                                            Image(
                                                painter = painterResource(id = R.drawable.copi_06),
                                                contentDescription = "Copi Métricas",
                                                modifier = Modifier.size(160.dp)
                                            )
                                        }
                                    }
                                }

                                // --- TOUR COPI: HORARIOS (NUEVA ANIMACIÓN) ---
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .height(0.dp)
                                        .zIndex(10f),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    AnimatedVisibility(
                                        visible = assistantEnabled && dashboardTourMetricsDismissed && !dashboardTourScheduleDismissed,
                                        enter = fadeIn(tween(1000)) + scaleIn(tween(1000)),
                                        exit = fadeOut(tween(800)) + scaleOut(tween(800)),
                                        modifier = Modifier
                                            .offset(x = 60.dp, y = 250.dp) // BAJADO 10DP MÁS (240 -> 250)
                                            .wrapContentSize(unbounded = true)
                                    ) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "scheduleHandMove")
                                        val handOffsetX by infiniteTransition.animateValue(
                                            initialValue = 0.dp,
                                            targetValue = (-15).dp,
                                            typeConverter = Dp.VectorConverter,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(800, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "handOffsetX"
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(220.dp)
                                        ) {
                                            // GLOBO DE DIÁLOGO (ARRIBA)
                                            AnimatedVisibility(
                                                visible = showScheduleTourBubble,
                                                enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                                                exit = fadeOut(tween(500))
                                            ) {
                                                val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                                                Surface(
                                                    shape = bubbleShape,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier
                                                        .widthIn(max = 200.dp)
                                                        .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp).padding(bottom = 12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = t["tour_schedule"] ?: "",
                                                            color = Color.Black,
                                                            fontSize = 12.sp,
                                                            fontFamily = copiFontFamily,
                                                            textAlign = TextAlign.Center,
                                                            lineHeight = 15.sp
                                                        )
                                                        Spacer(Modifier.height(10.dp))
                                                        Button(
                                                            onClick = { 
                                                                coroutineScope.launch {
                                                                    dataStoreManager.setDashboardTourScheduleDismissed(true)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                            shape = containerShape,
                                                            modifier = Modifier.height(28.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                        ) {
                                                            Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            // PERSONAJE COPI (copi_11) - MANO CON MOVIMIENTO HORIZONTAL
                                            Image(
                                                painter = painterResource(id = R.drawable.copi_11),
                                                contentDescription = "Copi Horarios",
                                                modifier = Modifier
                                                    .size(100.dp) // REDUCIDO 30% (140 -> 100 aprox)
                                                    .offset(x = handOffsetX)
                                            )
                                        }
                                    }
                                }

                                // --- TOUR COPI: QR ASISTENCIA (NUEVA ANIMACIÓN) ---
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .height(0.dp)
                                        .zIndex(10f),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    AnimatedVisibility(
                                        visible = assistantEnabled && dashboardTourScheduleDismissed && !dashboardTourQrDismissed,
                                        enter = fadeIn(tween(1000)) + scaleIn(tween(1000)),
                                        exit = fadeOut(tween(800)) + scaleOut(tween(800)),
                                        modifier = Modifier
                                            .offset(x = 30.dp, y = 250.dp) // MOVIDO 30DP A LA IZQUIERDA (60 -> 30)
                                            .wrapContentSize(unbounded = true)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(220.dp)
                                        ) {
                                            // GLOBO DE DIÁLOGO (ARRIBA)
                                            AnimatedVisibility(
                                                visible = showQrTourBubble,
                                                enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                                                exit = fadeOut(tween(500))
                                            ) {
                                                val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                                                Surface(
                                                    shape = bubbleShape,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier
                                                        .widthIn(max = 200.dp)
                                                        .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp).padding(bottom = 12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = t["tour_qr_desc"] ?: "",
                                                            color = Color.Black,
                                                            fontSize = 12.sp,
                                                            fontFamily = copiFontFamily,
                                                            textAlign = TextAlign.Center,
                                                            lineHeight = 15.sp
                                                        )
                                                        Spacer(Modifier.height(10.dp))
                                                        Button(
                                                            onClick = { 
                                                                coroutineScope.launch {
                                                                    dataStoreManager.setDashboardTourQrDismissed(true)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                            shape = containerShape,
                                                            modifier = Modifier.height(28.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                        ) {
                                                            Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            // PERSONAJE COPI (copi_12)
                                            Image(
                                                painter = painterResource(id = R.drawable.copi_12),
                                                contentDescription = "Copi QR",
                                                modifier = Modifier.size(80.dp) // REDUCIDO AL TAMAÑO DE SETTINGS/PROFESORES
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { SearchBar(t["search"] ?: "Search...", accentColor, containerShape) }

                item { Text(t["recent"] ?: "Activity", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                
                val recentActivity = members.filter { 
                    val last = it.lastVisit
                    last != null && (System.currentTimeMillis() - last) < (24L * 60 * 60 * 1000)
                }.sortedByDescending { it.lastVisit }.take(10)

                if (recentActivity.isNotEmpty()) {
                    items(recentActivity) { member ->
                        ActivityRowItem(member, containerShape, accentColor, professors)
                    }
                } else {
                    item {
                        Text("No hay actividad reciente", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
            }
        }

        // --- TOUR PASO 1: Identidad (CENTRO) ---
        AnimatedVisibility(
            visible = assistantEnabled && welcomeDismissed && connectionVerified && readyDismissed && !dashboardTourStep1CentralDismissed,
            enter = fadeIn(tween(1500)) + scaleIn(tween(1500)),
            exit = fadeOut(tween(1000)) + scaleOut(tween(1000)),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-150).dp) // Bajado 50dp respecto al anterior (-200 + 50)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // PERSONAJE COPI (CENTRO)
                Image(
                    painter = painterResource(id = R.drawable.copi_03),
                    contentDescription = "Copi Tour 1",
                    modifier = Modifier.size(176.dp) // Reducido un 20% (220 -> 176)
                )

                // GLOBO DE DIÁLOGO (ABAJO)
                AnimatedVisibility(
                    visible = showTourStep1Bubble,
                    enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut(tween(500))
                ) {
                    val bubbleShape = remember { SpeechBubbleTopShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(
                        shape = bubbleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(top = 0.dp)
                            .widthIn(max = 280.dp)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t["tour_step1"] ?: "",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontFamily = copiFontFamily,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { 
                                    coroutineScope.launch {
                                        dataStoreManager.setDashboardTourStep1CentralDismissed(true)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = containerShape,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- MANO PUNTERO (ABAJO DERECHA) ---
        AnimatedVisibility(
            visible = assistantEnabled && welcomeDismissed && connectionVerified && readyDismissed && dashboardTourStep1CentralDismissed && !dashboardTourStep1Dismissed,
            enter = fadeIn(tween(1000)),
            exit = fadeOut(tween(800)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 70.dp, end = 0.dp) // 20dp a la derecha respecto a end=20dp
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "handBounce")
            val offsetY by infiniteTransition.animateValue(
                initialValue = 0.dp,
                targetValue = 15.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End) {
                // GLOBO PEQUEÑO (IZQUIERDA DE LA MANO - ESTÁTICO)
                val bubbleShape = remember { SpeechBubbleRightShape(cornerRadius = 15.dp.value * 2f) }
                Surface(
                    shape = bubbleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(bottom = 20.dp) // Alineación visual con la mano
                        .widthIn(max = 160.dp)
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                ) {
                    Text(
                        text = t["tour_step1_hand"] ?: "",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontFamily = copiFontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp).padding(end = 12.dp),
                        lineHeight = 13.sp
                    )
                }
                
                // MANO (DERECHA - ANIMADA)
                Image(
                    painter = painterResource(id = R.drawable.copi_07),
                    contentDescription = "Mano Puntero",
                    modifier = Modifier.size(80.dp).offset(y = offsetY)
                )
            }
        }

        // --- TOUR PASO 8: PROFESORES (ABAJO DERECHA) ---
        AnimatedVisibility(
            visible = assistantEnabled && dashboardTourQrDismissed && !dashboardTourProfessorsDismissed,
            enter = fadeIn(tween(1000)),
            exit = fadeOut(tween(800)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 70.dp, end = 25.dp) 
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "profsHandBounce")
            val offsetY by infiniteTransition.animateValue(
                initialValue = 0.dp,
                targetValue = 15.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // GLOBO DE DIÁLOGO (ARRIBA)
                AnimatedVisibility(
                    visible = showProfessorsTourBubble,
                    enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut(tween(500))
                ) {
                    val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                    Surface(
                        shape = bubbleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t["tour_profs"] ?: "",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontFamily = copiFontFamily,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { 
                                    coroutineScope.launch {
                                        dataStoreManager.setDashboardTourProfessorsDismissed(true)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = containerShape,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                // MANO (ABAJO - ANIMADA)
                Image(
                    painter = painterResource(id = R.drawable.copi_07),
                    contentDescription = "Mano Puntero Profesores",
                    modifier = Modifier.size(80.dp).offset(y = offsetY)
                )
            }
        }

        // --- TOUR PASO 9: CLIENTES NAV (ABAJO CENTRO) ---
        AnimatedVisibility(
            visible = assistantEnabled && dashboardTourProfessorsDismissed && !dashboardTourMembersNavDismissed,
            enter = fadeIn(tween(1000)),
            exit = fadeOut(tween(800)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp).offset(x = 10.dp) 
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "membersNavHandBounce")
            val offsetY by infiniteTransition.animateValue(
                initialValue = 0.dp,
                targetValue = 15.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // GLOBO DE DIÁLOGO (ARRIBA)
                AnimatedVisibility(
                    visible = showMembersNavTourBubble,
                    enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut(tween(500))
                ) {
                    val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                    Surface(
                        shape = bubbleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t["tour_members_nav"] ?: "",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontFamily = copiFontFamily,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { 
                                    coroutineScope.launch {
                                        dataStoreManager.setDashboardTourMembersNavDismissed(true)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = containerShape,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                // PERSONAJE COPI (copi_12) - REDUCIDO AL MISMO TAMAÑO QUE SETTINGS/PROFESORES
                Image(
                    painter = painterResource(id = R.drawable.copi_12),
                    contentDescription = "Copi Clientes Nav",
                    modifier = Modifier.size(80.dp).offset(y = offsetY)
                )
            }
        }

        // --- TOUR PASO 10: TIENDA NAV (ABAJO IZQUIERDA) ---
        AnimatedVisibility(
            visible = assistantEnabled && dashboardTourMembersNavDismissed && !dashboardTourStoreNavDismissed,
            enter = fadeIn(tween(1000)),
            exit = fadeOut(tween(800)),
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 70.dp).padding(start = 25.dp) 
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "storeNavHandBounce")
            val offsetY by infiniteTransition.animateValue(
                initialValue = 0.dp,
                targetValue = 15.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // GLOBO DE DIÁLOGO (ARRIBA)
                AnimatedVisibility(
                    visible = showStoreNavTourBubble,
                    enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut(tween(500))
                ) {
                    val bubbleShape = remember { SpeechBubbleBottomShape(cornerRadius = 15.dp.value * 2f) }
                    Surface(
                        shape = bubbleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t["tour_store_nav"] ?: "",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontFamily = copiFontFamily,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { 
                                    coroutineScope.launch {
                                        dataStoreManager.setDashboardTourStoreNavDismissed(true)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = containerShape,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                // PERSONAJE COPI (copi_12)
                Image(
                    painter = painterResource(id = R.drawable.copi_12),
                    contentDescription = "Copi Tienda Nav",
                    modifier = Modifier.size(80.dp).offset(y = offsetY)
                )
            }
        }

        // --- AYUDANTE COPI (Flujo Secuencial Maestro) ---

        // PASO 1: BIENVENIDA (DERECHA)
        AnimatedVisibility(
            visible = assistantEnabled && !welcomeDismissed,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(1500)) + fadeIn(tween(1500)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(1000)) + fadeOut(tween(1000)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                AnimatedVisibility(visible = showWelcomeBubble, enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut(tween(500))) {
                    val bubbleShape = remember { SpeechBubbleShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(shape = bubbleShape, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.widthIn(max = 240.dp).border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)) {
                        Column(modifier = Modifier.padding(16.dp).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (appLanguage == "Español") "¡Hola! Bienvenido al Panel Admin. Soy COPI, tu ayudante virtual." else "Hi! Welcome to the Admin Panel. I'm COPI, your virtual assistant.", color = Color.Black, fontSize = 14.sp, fontFamily = copiFontFamily, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { coroutineScope.launch { dataStoreManager.setWelcomeDismissed(true) } }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = containerShape, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                                Text(t["btn_hi"] ?: "¡Hola COPI!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Image(painter = painterResource(id = R.drawable.copi_01), contentDescription = "Copi", modifier = Modifier.size(180.dp))
            }
        }

        // PASO 2: VINCULACIÓN NUBE (IZQUIERDA) - Aparece si falta vincular Google
        AnimatedVisibility(
            visible = assistantEnabled && welcomeDismissed && !cloudHelpDismissed && googleToken.isEmpty() && gymApiUrl.isBlank(),
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(1500)) + fadeIn(tween(1500)),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(1000)) + fadeOut(tween(1000)),
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 150.dp, start = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Bottom) {
                AnimatedVisibility(visible = showCloudBubble, enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut(tween(500))) {
                    val bubbleShape = remember { SpeechBubbleLeftShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(shape = bubbleShape, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.widthIn(max = 240.dp).border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)) {
                        Column(modifier = Modifier.padding(16.dp).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = t["cloud_help"] ?: "", color = Color.Black, fontSize = 14.sp, fontFamily = copiFontFamily, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { coroutineScope.launch { dataStoreManager.setCloudHelpDismissed(true) } }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = containerShape, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                                Text(t["btn_ok"] ?: "Entendido", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Image(painter = painterResource(id = R.drawable.copi_02), contentDescription = "Copi Ayuda", modifier = Modifier.size(180.dp))
            }
        }

        // PASO 3: CREACIÓN BACKEND (DERECHA) - Aparece si ya vinculó pero falta el backend
        AnimatedVisibility(
            visible = assistantEnabled && welcomeDismissed && !step2HelpDismissed && googleToken.isNotEmpty() && gymApiUrl.isBlank() && !isSettingUp,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(1500)) + fadeIn(tween(1500)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(1000)) + fadeOut(tween(1000)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                AnimatedVisibility(visible = showStep2Bubble, enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut(tween(500))) {
                    val bubbleShape = remember { SpeechBubbleShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(shape = bubbleShape, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.widthIn(max = 240.dp).border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)) {
                        Column(modifier = Modifier.padding(16.dp).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = t["step2_help"] ?: "", color = Color.Black, fontSize = 14.sp, fontFamily = copiFontFamily, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { coroutineScope.launch { dataStoreManager.setStep2HelpDismissed(true) } }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = containerShape, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                                Text(t["btn_letsgo"] ?: "¡Vamos!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Image(painter = painterResource(id = R.drawable.copi_03), contentDescription = "Copi Backend", modifier = Modifier.size(180.dp))
            }
        }

        // PASO 3.5: DEMORA (DERECHA) - Aparece si isSettingUp tarda
        AnimatedVisibility(
            visible = assistantEnabled && isSettingUp && !delayHelpDismissed && !showAuthDialog,
            enter = fadeIn(tween(1500)),
            exit = fadeOut(tween(1000)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                AnimatedVisibility(visible = showDelayBubble, enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut(tween(500))) {
                    val bubbleShape = remember { SpeechBubbleShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(shape = bubbleShape, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.widthIn(max = 240.dp).border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)) {
                        Column(modifier = Modifier.padding(16.dp).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = t["delay_help"] ?: "", color = Color.Black, fontSize = 14.sp, fontFamily = copiFontFamily, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { coroutineScope.launch { dataStoreManager.setDelayHelpDismissed(true) } }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = containerShape, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                                Text(t["btn_ok"] ?: "Entendido", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Image(painter = painterResource(id = R.drawable.copi_04), contentDescription = "Copi Demora", modifier = Modifier.size(180.dp))
            }
        }

        // PASO 5: AUTORIZACIÓN (Se movió dentro del Diálogo de Autorización)

        // PASO FINAL: Éxito (Derecha) - Solo si la conexión está verificada y no hay procesos activos
        AnimatedVisibility(
            visible = assistantEnabled && welcomeDismissed && !readyDismissed && gymApiUrl.isNotBlank() && !isSettingUp && !showAuthDialog && connectionVerified,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(1500)) + fadeIn(tween(1500)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(1000)) + fadeOut(tween(1000)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                AnimatedVisibility(visible = showReadyBubble, enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut(tween(500))) {
                    val bubbleShape = remember { SpeechBubbleShape(cornerRadius = 15.dp.value * 2.5f) }
                    Surface(shape = bubbleShape, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.widthIn(max = 240.dp).border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)) {
                        Column(modifier = Modifier.padding(16.dp).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = t["panel_ready"] ?: "", color = Color.Black, fontSize = 14.sp, fontFamily = copiFontFamily, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { coroutineScope.launch { dataStoreManager.setReadyDismissed(true) } }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = containerShape, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                                Text(t["btn_great"] ?: "¡Genial!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Image(painter = painterResource(id = R.drawable.copi_01), contentDescription = "Copi Éxito", modifier = Modifier.size(180.dp))
            }
        }
    }


    if (showGymNameRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showGymNameRequiredDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
                    Spacer(Modifier.width(8.dp))
                    Text("Nombre del Gimnasio Requerido", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Para crear la base de datos debes asignar primero el nombre de tu gimnasio.",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2010)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Atención: El nombre con el que crees la base de datos quedará registrado permanentemente en ella, aunque posteriormente cambies el nombre del gimnasio en la app.",
                            color = Color(0xFFFFE0B2),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGymNameRequiredDialog = false
                        onAppearanceClick("name")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = containerShape
                ) {
                    Text("Asignar Nombre", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGymNameRequiredDialog = false }) {
                    Text("Cancelar", color = TextGray)
                }
            },
            containerColor = Color(0xFF1E2630)
        )
    }

    if (showConfirmCreateBackendDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCreateBackendDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = accentColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Base de Datos", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Se creará la base de datos para el gimnasio: \"$gymName\".",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2010)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Nota: El nombre \"$gymName\" quedará registrado permanentemente en la base de datos, aunque luego cambies el nombre del gimnasio en la app.",
                            color = Color(0xFFFFE0B2),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCreateBackendDialog = false
                        coroutineScope.launch {
                            isSettingUp = true
                            dataStoreManager.saveMembers(emptyList())
                            dataStoreManager.saveProfessors(emptyList())
                            dataStoreManager.saveSubscriptionPlans(emptyList())
                            dataStoreManager.saveAllProducts(emptyList())

                            val result = googleCloudRepository?.setupGymBackend(gymName)
                            isSettingUp = false
                            if (result?.isSuccess == true) {
                                Toast.makeText(context, "¡Base de datos creada con éxito!", Toast.LENGTH_LONG).show()
                                attendanceRepository?.syncGymInfo()
                            } else {
                                setupErrorMessage = result?.exceptionOrNull()?.message ?: "Error desconocido"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = containerShape
                ) {
                    Text("Confirmar y Crear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showConfirmCreateBackendDialog = false
                        onAppearanceClick("name")
                    }) {
                        Text("Cambiar Nombre", color = accentColor)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { showConfirmCreateBackendDialog = false }) {
                        Text("Cancelar", color = TextGray)
                    }
                }
            },
            containerColor = Color(0xFF1E2630)
        )
    }

    if (showQrDialog) {
        QrDisplayDialog(onDismiss = { showQrDialog = false }, gymName = gymName, gymApiUrl = gymApiUrl, accentColor = accentColor, shape = containerShape, title = t["qr_title"] ?: "")
    }

    if (showScanner) {
        AdminScannerDialog(
            t = t,
            onDismiss = { showScanner = false },
            onScanned = { qrContent ->
                showScanner = false
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
                
                coroutineScope.launch {
                    val emailScanned = parseEmailFromAdminQr(qrContent).lowercase().trim()
                    val professor = professors.find { it.email.lowercase().trim() == emailScanned }
                    val existingMember = members.find { it.email.lowercase().trim() == emailScanned }
                        ?: members.find { "${it.firstName} ${it.lastName}".lowercase().trim() == emailScanned }
                    
                    if (existingMember != null || professor != null) {
                        val updated = existingMember?.copy(
                            firstName = professor?.firstName ?: existingMember.firstName,
                            lastName = professor?.lastName ?: existingMember.lastName,
                            lastVisit = System.currentTimeMillis(),
                            attendanceCount = existingMember.attendanceCount + 1
                        ) ?: Member(
                            firstName = professor!!.firstName,
                            lastName = professor.lastName,
                            email = emailScanned,
                            lastVisit = System.currentTimeMillis(),
                            attendanceCount = 1
                        )

                        dataStoreManager.updateMember(updated)
                        val gymId = gymName.ifBlank { "Gym_Copilot" }.replace(" ", "_")
                        val result = attendanceRepository?.performCheckIn(gymId, emailScanned)
                        
                        if (result?.isSuccess == true) {
                            Toast.makeText(context, "${t["ast_success"]}: ${updated.fullName}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "${t["sync_error"]} - Local OK", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, t["not_found"] ?: "", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Diálogo de Autorización Manual (Rediseñado para incluir a COPI)
    if (showAuthDialog) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 400.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00222E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(t["auth_req"] ?: "Autorización", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(t["auth_desc"] ?: "Google requiere tu permiso manual.\n\n1. Toca 'Autorizar'\n2. Selecciona tu cuenta\n3. Toca 'Avanzado' -> 'Ir a GymProject (no seguro)'\n4. Toca 'Permitir'")
                        Spacer(Modifier.height(16.dp))
                        Text("Importante: Verifica que la opción 'Google Apps Script API' esté activada en script.google.com/home/settings", fontSize = 11.sp, color = Color.Yellow)
                        Spacer(Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        showAuthDialog = false
                                        val email = dataStoreManager.getUserEmail().first()
                                        val testUrl = if (pendingUrl.contains("?")) "$pendingUrl&action=setup&login_hint=$email" else "$pendingUrl?action=setup&login_hint=$email"

                                        withContext(Dispatchers.IO) {
                                            try {
                                                val response = NetworkModule.getApiServiceForGet(testUrl).getAttendanceRaw(testUrl, null)
                                                if (response.isSuccessful || response.code() == 302) {
                                                    connectionVerified = true
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "¡Listo! Conexión exitosa", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } catch (e: Exception) { }
                                        }
                                    }
                                }
                            ) {
                                Text("Ya lo hice / Continuar", color = accentColor)
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val email = dataStoreManager.getUserEmail().first()
                                        val finalUrl = if (pendingUrl.contains("?")) "$pendingUrl&login_hint=$email" else "$pendingUrl?login_hint=$email"
                                        uriHandler.openUri(finalUrl)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = containerShape
                            ) { Text("Autorizar", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                // COPI Guía dentro del diálogo para evitar ser tapado (Z-INDEX 1)
                if (assistantEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 40.dp, end = 16.dp)
                            .zIndex(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                            AnimatedVisibility(
                                visible = showAuthBubble,
                                enter = fadeIn(tween(800)) + expandVertically(expandFrom = Alignment.Bottom),
                                exit = fadeOut(tween(500))
                            ) {
                                val bubbleShape = remember { SpeechBubbleShape(cornerRadius = 15.dp.value * 2.5f) }
                                Surface(
                                    shape = bubbleShape,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .border(1.dp, Color.Black.copy(alpha = 0.1f), bubbleShape)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp).padding(bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = t["auth_help"] ?: "",
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            fontFamily = copiFontFamily,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = { showAuthBubble = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            shape = containerShape,
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                        ) {
                                            Text(t["btn_ok"] ?: "¡Entendido!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Image(
                                painter = painterResource(id = R.drawable.copi_05),
                                contentDescription = "Copi Autorización",
                                modifier = Modifier.size(180.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Error de Setup
    if (setupErrorMessage != null) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { setupErrorMessage = null },
            confirmButton = {
                Button(onClick = { setupErrorMessage = null }) { Text("Cerrar") }
            },
            title = { Text("Error al Crear Base de Datos", fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text(setupErrorMessage!!)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Verifica la conexión a internet e intenta presionar '2. CREAR BASE DE DATOS' nuevamente.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            },
            containerColor = Color(0xFF00222E),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }
}

@Composable
fun AdminHeader(
    logoUri: String?,
    gymName: String,
    font: FontFamily,
    accentColor: Color,
    shape: Shape,
    title: String,
    showBorder: Boolean = true,
    statusText: String? = null,
    statusColor: Color = Color.Green,
    onStatusClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Izquierda: Textos y Botón
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gymName,
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = font,
                textAlign = TextAlign.Start,
                lineHeight = 28.sp
            )
            
            Spacer(Modifier.height(12.dp))
            
            if (statusText != null && onStatusClick != null) {
                StatusBadge(text = statusText, color = statusColor, onClick = onStatusClick)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text("by GymCopilot", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        }

        // Columna Derecha: Logo
        val logoShape = if (showBorder) shape else RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(logoShape)
                .then(if (showBorder) {
                    Modifier.border(2.5.dp, accentColor, logoShape)
                } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (logoUri != null) {
                val logoModel = remember(logoUri) {
                    if (logoUri.startsWith("data:image")) {
                        try {
                            val base64String = logoUri.substringAfter("base64,")
                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) { logoUri }
                    } else logoUri
                }
                AsyncImage(model = logoModel, contentDescription = null, modifier = Modifier.fillMaxSize().padding(if (showBorder) 8.dp else 0.dp), contentScale = ContentScale.Fit)
            }
            else Icon(Icons.Default.AddPhotoAlternate, null, tint = accentColor.copy(0.3f), modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun GymBroadcastEditor(
    accentColor: Color,
    containerShape: Shape,
    dataStoreManager: DataStoreManager,
    repository: AttendanceRepository?,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentMessage by dataStoreManager.getBroadcastMessage().collectAsState(initial = "")
    var messageText by remember { mutableStateOf(currentMessage) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(currentMessage) {
        if (!isSaving) {
            messageText = currentMessage
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.5.dp, accentColor, containerShape),
        shape = containerShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00222E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("MENSAJE", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.weight(1f))
                
                if (isSaving) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(16.dp))
                } else {
                    IconButton(
                        onClick = {
                            val textToSend = messageText
                            coroutineScope.launch {
                                isSaving = true
                                dataStoreManager.setBroadcastMessage(textToSend)
                                val result = repository?.syncGymInfo(broadcastMessage = textToSend)
                                isSaving = false
                                if (result?.isSuccess == true) {
                                    Toast.makeText(context, "Mensaje guardado correctamente", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al guardar el mensaje", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = messageText != currentMessage,
                        modifier = Modifier.size(28.dp).background(
                            if (messageText != currentMessage) accentColor else accentColor.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.paperplane),
                            contentDescription = "Actualizar",
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(if (messageText != currentMessage) Color.Black else TextGray)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe un mensaje para todos...", color = TextGray.copy(alpha = 0.5f), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 2,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun QrDisplayDialog(onDismiss: () -> Unit, gymName: String, gymApiUrl: String, accentColor: Color, shape: Shape, title: String) {
    val qrBitmap = remember(gymName, gymApiUrl) {
        val json = JSONObject()
        json.put("gym_id", gymName.ifBlank { "Gym_Copilot" }.replace(" ", "_"))
        json.put("gym_name", gymName)
        json.put("gym_api_url", gymApiUrl)
        json.put("action", "check_in")
        val qrContent = json.toString()
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) for (y in 0 until 512) bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            bitmap
        } catch (e: Exception) { null }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp).border(2.dp, accentColor, shape), shape = shape, colors = CardDefaults.cardColors(containerColor = Color(0xFF00222E))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(gymName, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.size(250.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                    qrBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Los socios deben escanear este QR para integrarse y marcar asistencia.", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                    shape = shape,
                    modifier = Modifier.fillMaxWidth().border(1.dp, accentColor, shape)
                ) {
                    val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                    Text("Cerrar", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OccupancyCard(label: String, accentColor: Color, shape: Shape) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val valColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val lblColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(modifier = Modifier.fillMaxWidth().border(1.5.dp, accentColor, shape), shape = shape, colors = CardDefaults.cardColors(containerColor = buttonBgColor)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(label, color = lblColor, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("32", color = valColor, fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Text(" / 60", color = lblColor, fontSize = 22.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                    }
                }
                Icon(Icons.Default.Speed, null, tint = accentColor, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { 0.53f }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = accentColor, trackColor = Color.White.copy(alpha = 0.1f))
        }
    }
}

@Composable
fun MetricsGridSection(metrics: List<MetricData>, accentColor: Color, shape: Shape) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in metrics.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallMetricCard(metrics[i], Modifier.weight(1f), accentColor, shape)
                if (i + 1 < metrics.size) SmallMetricCard(metrics[i + 1], Modifier.weight(1f), accentColor, shape)
                else Spacer(Modifier.weight(1f))
            }
            if (i == 0 || i == 4) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
            }
        }
    }
}

data class MetricData(
    val label: String, 
    val value: String, 
    val icon: androidx.compose.ui.graphics.vector.ImageVector, 
    val color: Color, 
    val isSecure: Boolean = false,
    val isVisible: Boolean = true,
    val onToggleVisibility: (() -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SmallMetricCard(data: MetricData, modifier: Modifier, accentColor: Color, shape: Shape) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val valColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val lblColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(modifier = modifier.border(1.5.dp, accentColor.copy(alpha = 0.4f), shape).clickable(enabled = data.onClick != null) { data.onClick?.invoke() }, shape = shape, colors = CardDefaults.cardColors(containerColor = buttonBgColor)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val contentWeight = if (data.isSecure) 3f else 1f
            Row(modifier = Modifier.weight(contentWeight), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(data.color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(data.icon, null, tint = data.color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(data.value, color = valColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(data.label, color = lblColor, fontSize = 10.sp, lineHeight = 11.sp)
                }
            }
            if (data.isSecure) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { data.onToggleVisibility?.invoke() }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = if (data.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = TextGray.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(label: String, accentColor: Color, shape: Shape) {
    OutlinedTextField(
        value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(label, color = TextGray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor) },
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color(0xFF00222E),
            unfocusedContainerColor = Color(0xFF00222E)
        )
    )
}

@Composable
fun ActivityRowItem(member: Member, shape: Shape, accentColor: Color, professors: List<Professor> = emptyList()) {
    val professor = professors.find { it.email.lowercase().trim() == member.email.lowercase().trim() }
    val displayName = professor?.fullName ?: member.fullName
    val photoSource = professor?.photoUri ?: member.photoUri
    val profColor = professor?.profileColor?.let { Color(it) } ?: accentColor
    val isProfessor = professor != null
    val borderColor = if (isProfessor) profColor else Color.White.copy(alpha = 0.05f)
    val borderWidth = if (isProfessor) 2.5.dp else 1.dp
    val containerBg = if (isProfessor) profColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f)

    Card(modifier = Modifier.fillMaxWidth().border(borderWidth, borderColor, shape), shape = shape, colors = CardDefaults.cardColors(containerBg)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isProfessor) profColor.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                if (photoSource != null) AsyncImage(model = photoSource, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else Icon(if (isProfessor) Icons.Default.FitnessCenter else Icons.Default.Person, null, tint = if (isProfessor) profColor else TextGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                val timeStr = remember(member.lastVisit) {
                    member.lastVisit?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: ""
                }
                Text("Acceso registrado • $timeStr", color = TextGray, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = if (isProfessor) profColor else accentColor)
        }
    }
}

@Composable
fun AdminScannerDialog(t: Map<String, String>, onDismiss: () -> Unit, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isProcessed = remember { AtomicBoolean(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    val reader = remember { MultiFormatReader() }

    LaunchedEffect(Unit) {
        while (isActive && !isProcessed.get()) {
            delay(300)
            val pv = previewViewRef.value ?: continue
            val bitmap = pv.bitmap ?: continue
            val result = decodeAdminBitmap(reader, bitmap)
            if (result != null && isProcessed.compareAndSet(false, true)) {
                onScanned(result)
                break
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                previewViewRef.value = previewView
                ProcessCameraProvider.getInstance(ctx).also { future ->
                    future.addListener({
                        val cameraProvider = future.get()
                        val preview = androidx.camera.core.Preview.Builder().build().apply { 
                            setSurfaceProvider(previewView.surfaceProvider) 
                        }
                        try {
                            cameraProvider.unbindAll()
                            val camera: Camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) { Log.e("Scanner", "Error cámara: ${e.message}") }
                    }, ContextCompat.getMainExecutor(ctx))
                }
                previewView
            }, modifier = Modifier.fillMaxSize())
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scanSize = 300.dp.toPx()
                val left = (size.width - scanSize) / 2
                val top = (size.height - scanSize) / 2
                val rect = Rect(Offset(left, top), Size(scanSize, scanSize))
                with(drawContext.canvas.nativeCanvas) {
                    val checkpoint = saveLayer(null, null)
                    drawRect(Color.Black.copy(alpha = 0.7f))
                    drawRoundRect(Color.Transparent, rect.topLeft, rect.size, CornerRadius(30.dp.toPx()), blendMode = BlendMode.Clear)
                    restoreToCount(checkpoint)
                }
                drawRoundRect(PrimaryTurquoise, rect.topLeft, rect.size, CornerRadius(30.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
            }
            IconButton(onClick = { flashEnabled = !flashEnabled; cameraControl?.enableTorch(flashEnabled) }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, "Flash", tint = Color.White) }
            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t["qr_instr"] ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(30.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))) { Text(t["cancel"] ?: "", color = Color.White) }
            }
        }
    }
}

private fun decodeAdminBitmap(reader: MultiFormatReader, bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    return try { reader.decodeWithState(binaryBitmap).text } catch (e: Exception) { null } finally { reader.reset() }
}

private fun parseEmailFromAdminQr(raw: String): String {
    if (raw.startsWith("ATTENDANCE:")) {
        val parts = raw.split(":")
        if (parts.size >= 2) return parts.last().trim()
    }
    try { val json = JSONObject(raw); return json.optString("email", json.optString("Email", raw)) } catch (e: Exception) { }
    val emailMatch = Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}""").find(raw)?.value
    return emailMatch ?: raw
}

class SpeechBubbleRightShape(private val cornerRadius: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val rectWidth = size.width - 20f 
            val rectHeight = size.height
            
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            )
            
            // --- COLA DEL GLOBO DERECHA ---
            val tipX = size.width
            val tipY = rectHeight * 0.75f
            
            moveTo(rectWidth, rectHeight * 0.55f)
            quadraticTo(
                rectWidth + 10f, rectHeight * 0.60f,
                tipX, tipY
            )
            quadraticTo(
                rectWidth + 10f, rectHeight * 0.70f,
                rectWidth, rectHeight * 0.85f
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

class SpeechBubbleTopShape(private val cornerRadius: Float) : Shape {

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
            
            // --- COLA DEL GLOBO SUPERIOR (BOCADILLO) ---
            // POSICIÓN: Centro superior, apuntando HACIA ARRIBA (hacia la cabeza de COPI)
            val tipX = rectWidth * 0.5f
            val tipY = 0f
            
            moveTo(rectWidth * 0.40f, arrowHeight)
            // Lado izquierdo de la flecha: Cóncavo
            quadraticTo(
                rectWidth * 0.42f, arrowHeight - 15f,
                tipX, tipY
            )
            // Lado derecho de la flecha
            quadraticTo(
                rectWidth * 0.58f, arrowHeight - 15f,
                rectWidth * 0.60f, arrowHeight
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

class SpeechBubbleLeftShape(private val cornerRadius: Float) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val rectWidth = size.width
            val rectHeight = size.height - 30f 
            
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            )
            
            // --- COLA DEL GLOBO IZQUIERDA (CORREGIDA) ---
            // Nace en la izquierda y apunta hacia la DERECHA (cabeza de COPI)
            // PUNTA: Apuntando hacia abajo-derecha a la cabeza de copi_02.
            val tipX = rectWidth * 0.15f 
            val tipY = rectHeight + 40f 
            
            moveTo(rectWidth * 0.10f, rectHeight)
            // Lado izquierdo: Cóncavo apuntando a la derecha
            quadraticTo(
                rectWidth * 0.12f, rectHeight + 20f,
                tipX, tipY
            )
            // Lado derecho: Convexo hacia la derecha
            quadraticTo(
                rectWidth * 0.22f, rectHeight + 15f,
                rectWidth * 0.25f, rectHeight
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

class SpeechBubbleShape(private val cornerRadius: Float) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val rectWidth = size.width
            val rectHeight = size.height - 30f // Espacio reservado para la cola curva
            
            // Rectángulo redondeado principal: Base del globo de diálogo
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            )
            
            // --- REFERENCIA DE LA COLA DEL GLOBO (BOCADILLO) ---
            // POSICIÓN: Se encuentra en el lado derecho inferior del globo.
            // PUNTA: Desplazada 20dp a la derecha y 10dp abajo desde la base del globo (rectHeight).
            // FORMA: Lado izquierdo CÓNCAVO (curva hacia adentro) para estilo cómic.
            val tipX = rectWidth * 0.60f + with(density) { 20.dp.toPx() }
            val tipY = rectHeight + 40f + with(density) { 10.dp.toPx() }
            
            moveTo(rectWidth * 0.88f, rectHeight)
            // Lado derecho de la cola: Curvatura convexa estándar
            quadraticTo(
                rectWidth * 0.92f, rectHeight + 15f,
                tipX, tipY
            )
            // Lado izquierdo de la cola: Curvatura cóncava (punto de control desplazado a 0.82f)
            // Esta forma señala con precisión la cabeza de COPI.
            quadraticTo(
                rectWidth * 0.82f, rectHeight + 20f,
                rectWidth * 0.75f, rectHeight
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

class SpeechBubbleBottomShape(private val cornerRadius: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val rectWidth = size.width
            val rectHeight = size.height - 30f 
            
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            )
            
            // --- COLA DEL GLOBO INFERIOR ---
            val tipX = rectWidth * 0.5f
            val tipY = size.height
            
            moveTo(rectWidth * 0.40f, rectHeight)
            quadraticTo(
                rectWidth * 0.45f, rectHeight + 15f,
                tipX, tipY
            )
            quadraticTo(
                rectWidth * 0.55f, rectHeight + 15f,
                rectWidth * 0.60f, rectHeight
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
