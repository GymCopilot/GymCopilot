package com.zexo.gymcopilot.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberHelpScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onClassesClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    val pagerState = rememberPagerState(pageCount = { 5 })
    val tabsScrollState = rememberScrollState()

    val t = remember(appLanguage) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Guía del Alumno",
                "sec_dash" to "DASHBOARD", "sec_dash_d" to "Tu resumen diario",
                "sec_store" to "TIENDA", "sec_store_d" to "Catálogo del gimnasio",
                "sec_sched" to "CLASES", "sec_sched_d" to "Grilla de actividades",
                "sec_ruts" to "RUTINAS", "sec_ruts_d" to "Tus planes de entrenamiento",
                "sec_chat" to "CHAT", "sec_chat_d" to "Contacto con administración",
                
                "h_status" to "Estado de Membresía", "h_status_d" to "Muestra si tu cuota está 'Vigente' o 'Vencida', el nombre de tu plan y la próxima fecha de renovación.",
                "h_qr" to "Login QR", "h_qr_d" to "Genera tu código personal. Muéstralo en la recepción para que el staff registre tu entrada rápidamente.",
                "h_checkin" to "Check-in Manual", "h_checkin_d" to "Usa el escáner para leer el código QR del gimnasio y registrar tu asistencia de forma autónoma.",
                "h_gym_status" to "Estado del Gimnasio", "h_gym_status_d" to "Indicadores en tiempo real que te informan si el local está abierto y si hay clases en sesión.",
                
                "st_view" to "Catálogo Público", "st_view_d" to "Explora los productos disponibles. Puedes ver precios, categorías y contactar por WhatsApp para compras.",
                
                "sch_grid" to "Grilla de Actividades", "sch_grid_d" to "Consulta los horarios de todas las clases disponibles. Planifica tu semana viendo qué profesor dicta cada actividad.",
                
                "rut_list" to "Mis Rutinas", "rut_list_d" to "Acceso a todos los planes asignados por tus profesores. Puedes ver el detalle de cada ejercicio.",
                "rut_exec" to "Modo Entrenamiento", "rut_exec_d" to "Guía paso a paso con cronómetro, series, repeticiones y videos para realizar cada ejercicio correctamente.",
                
                "chat_admin" to "Mensajería Directa", "chat_admin_d" to "Habla directamente con la administración para dudas sobre pagos, horarios o servicios."
            )
        } else {
            mapOf(
                "title" to "Student Guide",
                "sec_dash" to "DASHBOARD", "sec_dash_d" to "Your daily summary",
                "sec_store" to "STORE", "sec_store_d" to "Gym catalog",
                "sec_sched" to "CLASSES", "sec_sched_d" to "Activity grid",
                "sec_ruts" to "ROUTINES", "sec_ruts_d" to "Your training plans",
                "sec_chat" to "CHAT", "sec_chat_d" to "Contact administration",
                
                "h_status" to "Membership Status", "h_status_d" to "Shows if your fee is 'Active' or 'Expired', your plan name, and next renewal date.",
                "h_qr" to "Login QR", "h_qr_d" to "Generates your personal code. Show it at the front desk for quick check-in.",
                "h_checkin" to "Manual Check-in", "h_checkin_d" to "Use the scanner to read the gym's QR code and record your attendance autonomously.",
                "h_gym_status" to "Gym Status", "h_gym_status_d" to "Real-time indicators informing if the gym is open and if there are classes in session.",
                
                "st_view" to "Public Catalog", "st_view_d" to "Browse available products. View prices, categories, and contact via WhatsApp for purchases.",
                
                "sch_grid" to "Activity Grid", "sch_grid_d" to "Check the schedule for all available classes. Plan your week by seeing which teacher leads each activity.",
                
                "rut_list" to "My Routines", "rut_list_d" to "Access to all plans assigned by your teachers. View exercise details.",
                "rut_exec" to "Workout Mode", "rut_exec_d" to "Step-by-step guide with timer, sets, reps, and videos to perform exercises correctly.",
                
                "chat_admin" to "Direct Messaging", "chat_admin_d" to "Talk directly with administration for questions about payments, schedules, or services."
            )
        }
    }

    val dashItems = listOf(
        GuideItemData(t["h_status"]!!, t["h_status_d"]!!, Icons.Default.VerifiedUser, Color(0xFF4CAF50)),
        GuideItemData(t["h_qr"]!!, t["h_qr_d"]!!, Icons.Default.QrCode, PrimaryTurquoise),
        GuideItemData(t["h_checkin"]!!, t["h_checkin_d"]!!, Icons.Default.QrCodeScanner, accentColor),
        GuideItemData(t["h_gym_status"]!!, t["h_gym_status_d"]!!, Icons.Default.CheckCircle, Color(0xFF00C853))
    )
    val storeItems = listOf(
        GuideItemData(t["st_view"]!!, t["st_view_d"]!!, Icons.Default.Storefront),
        GuideItemData("Categorías", "Filtra por Suplementos, Ropa o Equipo.", Icons.Default.Category, accentColor)
    )
    val schedItems = listOf(
        GuideItemData(t["sch_grid"]!!, t["sch_grid_d"]!!, Icons.Default.CalendarMonth, Color(0xFFE91E63))
    )
    val rutItems = listOf(
        GuideItemData(t["rut_list"]!!, t["rut_list_d"]!!, Icons.Default.FitnessCenter, accentColor),
        GuideItemData(t["rut_exec"]!!, t["rut_exec_d"]!!, Icons.Default.Timer, Color(0xFF00C853))
    )
    val chatItems = listOf(
        GuideItemData(t["chat_admin"]!!, t["chat_admin_d"]!!, Icons.AutoMirrored.Filled.Chat, accentColor)
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(34.dp).clip(CircleShape), colorFilter = ColorFilter.tint(accentColor))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            MemberBottomNavigation(
                currentRoute = "member_help",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onClassesClick = onClassesClick,
                onRoutinesClick = onRoutinesClick,
                onChatClick = onChatClick,
                accentColor = accentColor
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))

            val tabs = listOf(
                rememberVectorPainter(Icons.Default.Dashboard),
                rememberVectorPainter(Icons.Default.Storefront),
                rememberVectorPainter(Icons.Default.CalendarMonth),
                rememberVectorPainter(Icons.Default.FitnessCenter),
                rememberVectorPainter(Icons.AutoMirrored.Filled.Chat)
            )
            val tabWidth = 60.dp
            val spacing = 8.dp
            val density = LocalDensity.current

            LaunchedEffect(pagerState.currentPage) {
                val targetScroll = (pagerState.currentPage * (60 + 8))
                tabsScrollState.animateScrollTo(
                    with(density) { targetScroll.dp.roundToPx() },
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                )
            }
            
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(tabsScrollState)) {
                    Box(modifier = Modifier.width((tabWidth + spacing) * tabs.size)) {
                        val tabWidthPx = with(density) { tabWidth.toPx() }
                        val spacingPx = with(density) { spacing.toPx() }
                        Box(
                            modifier = Modifier
                                .offset {
                                    val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                    IntOffset(x = (position * (tabWidthPx + spacingPx)).roundToInt(), y = 0)
                                }
                                .height(68.dp)
                                .width(tabWidth)
                                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                                .background(Color(0xFF081C24))
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            modifier = Modifier.draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    if (!pagerState.isScrollInProgress) {
                                        if (delta < -3f && pagerState.currentPage < tabs.size - 1) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = tween(1000, easing = FastOutSlowInEasing)) }
                                        } else if (delta > 3f && pagerState.currentPage > 0) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = tween(1000, easing = FastOutSlowInEasing)) }
                                        }
                                    }
                                }
                            )
                        ) {
                            tabs.forEachIndexed { index, icon ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier.height(48.dp).width(tabWidth).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                                        scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(1000, easing = FastOutSlowInEasing)) }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painter = icon, contentDescription = null, tint = if (isSelected) accentColor else TextGray, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().offset(y = (-20).dp).padding(horizontal = 24.dp).padding(bottom = 20.dp),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF081C24)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    userScrollEnabled = true,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val items = when (page) { 0 -> dashItems; 1 -> storeItems; 2 -> schedItems; 3 -> rutItems; 4 -> chatItems; else -> emptyList() }
                    val sectionTitle = when (page) { 0 -> t["sec_dash"]!!; 1 -> t["sec_store"]!!; 2 -> t["sec_sched"]!!; 3 -> t["sec_ruts"]!!; 4 -> t["sec_chat"]!!; else -> "" }
                    val sectionDesc = when (page) { 0 -> t["sec_dash_d"]!!; 1 -> t["sec_store_d"]!!; 2 -> t["sec_sched_d"]!!; 3 -> t["sec_ruts_d"]!!; 4 -> t["sec_chat_d"]!!; else -> "" }
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GuideSectionHeader(sectionTitle, sectionDesc, accentColor)
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 8.dp),
                            thickness = 1.dp,
                            color = accentColor.copy(alpha = 0.2f)
                        )
                        items.forEach { item -> GuideItem(item, accentColor, shape) }
                    }
                }
            }
        }
    }
}
