package com.zexo.gymcopilot.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
fun HelpScreen(
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
    
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val assistantEnabled by dataStoreManager.getAssistantEnabled().collectAsState(initial = false)
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val googleToken by dataStoreManager.getGoogleAccessToken().collectAsState(initial = "")
    var showAuthGuide by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 5 })

    val tabsScrollState = rememberScrollState()

    val t = remember(appLanguage, memberDesignation) {
        val plural = when(memberDesignation) {
            "Alumno" -> if (appLanguage == "Español") "alumnos" else "students"
            "Socio" -> if (appLanguage == "Español") "socios" else "partners"
            "Miembro" -> if (appLanguage == "Español") "miembros" else "members"
            "Estudiante" -> if (appLanguage == "Español") "estudiantes" else "students"
            "Cliente" -> if (appLanguage == "Español") "clientes" else "clients"
            "Atleta" -> if (appLanguage == "Español") "atletas" else "athletes"
            else -> "${memberDesignation}s"
        }
        
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Guía del Administrador",
                "assistant_title" to "Ayudante COPI",
                "assistant_desc" to "Un personaje te guiará por las funciones clave de la app.",
                "enabled" to "Ayudante Activado",
                "disabled" to "Ayudante Desactivado",
                "auth_guide" to "Guía de Autorización",
                "auth_guide_desc" to "Pasos visuales para vincular la nube",
                "sec_home" to "DASHBOARD", "sec_home_desc" to "Panel de control principal",
                "sec_store" to "TIENDA", "sec_store_desc" to "Gestión de ventas y catálogo",
                "sec_members" to "SOCIOS", "sec_members_desc" to "Administración de $plural",
                "sec_profs" to "EQUIPO", "sec_profs_desc" to "Gestión de staff y horarios",
                "sec_config" to "SISTEMA", "sec_config_desc" to "Personalización y ajustes",
                "h_header" to "Identidad de Marca", "h_header_d" to "Personaliza el logo y nombre de tu gimnasio desde 'Skin'. Esta información es la que verán tus socios en sus apps para identificarte, fortaleciendo la presencia de tu marca.",
                "h_status" to "Estado del Local", "h_status_d" to "Muestra el indicador dinámico 'ABIERTO'. Este estado se activa automáticamente cuando hay actividad detectada en el dashboard o sesiones de clase iniciadas.",
                "h_cloud" to "Configuración de Nube", "h_cloud_d" to "Panel de acción inicial para vincular Google Drive. Una vez configurado, tus datos se sincronizarán automáticamente, permitiendo que varios dispositivos compartan la misma información al instante.",
                "m_occ" to "Aforo y Capacidad", "m_occ_d" to "Control de ocupación en tiempo real. Muestra cuántas personas hay en el local frente al límite máximo. La barra de progreso cambia de color para advertirte cuando el gimnasio está llegando al tope de su capacidad.",
                "m_inc" to "Ingresos Mensuales", "m_inc_d" to "Muestra el total recaudado en el mes calendario. Incluye una función de seguridad (icono de ojo) para ocultar el monto, permitiéndote consultar tus finanzas de forma privada frente a otras personas.",
                "h_metrics" to "Estadísticas de Población", "h_metrics_d" to "Grilla con los cuatro pilares de tu gimnasio: Total de registrados, socios con pago al día (Activos), personas con deudas (Deudores) e inscripciones recientes (Nuevos).",
                "m_ast" to "Asistencia Diaria", "m_ast_d" to "Contador de accesos únicos del día. Te permite monitorear el flujo de personas que han ingresado al gimnasio durante la jornada de hoy.",
                "h_gst" to "Gestión de Agenda", "h_gst_d" to "Acceso directo a la grilla horaria. Desde aquí puedes organizar las clases públicas, asignar profesores y definir los turnos que verán tus socios en su app.",
                "m_qr" to "QR del Gimnasio", "m_qr_d" to "Muestra el código oficial del local. Al escanearlo, los socios vinculan su app con tu gimnasio y pueden registrar su asistencia de forma autónoma sin intervención del staff.",
                "m_scan" to "Escáner de Socios", "m_scan_d" to "Herramienta de cámara para validación manual. Escanea el código personal de la app del socio para registrar su entrada al local de forma rápida y segura.",
                "h_recent" to "Monitor en Tiempo Real", "h_recent_d" to "Historial vivo de las últimas 10 entradas registradas. Permite ver la foto, el nombre y la hora exacta de ingreso de cada persona que acaba de entrar.",
                "g_plans" to "Planes y Cuotas", "g_plans_d" to "Crea y edita las opciones de suscripción (ej: Pase Libre, 3 veces por semana). Define precios y duraciones que verán tus socios.",
                "g_profs" to "Profesores", "g_profs_d" to "Administra tu equipo de trabajo, asigna colores distintivos y gestiona sus perfiles de acceso.",
                "g_sched" to "Agenda y Horarios", "g_sched_d" to "Define la grilla de clases y eventos. Esta agenda es pública para que tus socios sepan qué actividades hay cada día.",
                "s_skin" to "Skin (Apariencia)", "s_skin_d" to "Personalización total: cambia colores de botones, formas (curvas), tipografías y sube el logo oficial de tu marca.",
                "s_desig" to "Designación", "s_desig_d" to "Cambia cómo la app llama a tus integrantes (ej: llamarlos 'Atletas' en lugar de 'Alumnos' en toda la interfaz).",
                "s_lang" to "Idioma", "s_lang_d" to "Configura el idioma de la interfaz (Español/Inglés) para adaptarla a tus preferencias.",
                "s_net" to "Red", "s_net_d" to "Configura el servidor de asistencia. Esta dirección vincula la app con tu base de datos en la nube.",
                "s_qr" to "Código QR de Asistencia", "s_qr_d" to "Genera el QR oficial de tu gimnasio. Puedes compartirlo para que los socios lo escaneen o mandarlo a imprimir directamente.",
                "s_assist" to "Asistente Virtual", "s_assist_d" to "Desde aquí puedes ver las instrucciones del ayudante o acceder a soporte técnico si necesitas ayuda personalizada.",
                "s_contact" to "Contacto y Soporte", "s_contact_d" to "Acceso directo a nuestros canales oficiales (WhatsApp, Email) y a la pantalla de reporte de fallos para enviarnos problemas técnicos.",
                "s_currency" to "Moneda", "s_currency_d" to "Cambia el símbolo monetario ($, €, etc.) y el nombre de la divisa que se mostrará en los ingresos y planes.",
                "st_cats" to "Categorías", "st_cats_d" to "Organiza tus productos por grupos con colores personalizados. Una estructura clara ayuda a que el administrador gestione el stock y los socios encuentren lo que buscan rápidamente.",
                "st_add" to "Productos", "st_add_d" to "Sube artículos, fotos, precios y controla el stock de los productos que vendes en el gimnasio. Puedes ver una 'Vista Previa' antes de guardar.",
                "st_view" to "Tienda Pública", "st_view_d" to "Visualiza el catálogo tal como lo ven los socios. Verifica que las fotos y precios sean correctos desde la perspectiva del cliente.",
                "m_total" to "Total de Socios", "m_total_d" to "Acceso al listado completo de todas las personas registradas, sin importar su estado de pago actual.",
                "m_new" to "Nuevos del Mes", "m_new_d" to "Identifica rápidamente a las personas que se inscribieron durante este mes calendario para darles seguimiento.",
                "m_debt" to "Deudores / Vencidos", "m_debt_d" to "Listado automático de socios con cuotas vencidas. El sistema los detecta comparando la fecha de hoy con su último pago.",
                "m_search" to "Buscador", "m_search_d" to "Busca por nombre o apellido para encontrar la ficha técnica de cualquier integrante de forma instantánea.",
                "m_rec" to "Actividad Reciente", "m_rec_d" to "Muestra las últimas entradas registradas. Útil para verificar quién acaba de entrar sin tener que abrir el listado completo.",
                "m_act" to "Activos", "m_act_d" to "Conteo de socios que tienen una suscripción vigente y están al día con sus pagos."
            )
        } else {
            mapOf(
                "title" to "Administrator Guide",
                "assistant_title" to "COPI Assistant",
                "assistant_desc" to "A character will guide you through the app.",
                "enabled" to "Assistant Enabled",
                "disabled" to "Assistant Disabled",
                "auth_guide" to "Authorization Guide",
                "auth_guide_desc" to "Visual steps to link the cloud",
                "sec_home" to "DASHBOARD", "sec_home_desc" to "Main control panel",
                "sec_store" to "STORE", "sec_store_desc" to "Sales management",
                "sec_members" to "MEMBERS", "sec_members_desc" to "Management of $plural",
                "sec_profs" to "TEAM", "sec_profs_desc" to "Staff management",
                "sec_config" to "SYSTEM", "sec_config_desc" to "Customization",
                "h_header" to "Brand Identity", "h_header_d" to "Customize your gym's logo and name from 'Skin'. This info is what your members will see in their apps to identify you, strengthening your brand presence.",
                "h_status" to "Venue Status", "h_status_d" to "Shows the dynamic 'OPEN' indicator. This status activates automatically when activity is detected in the dashboard or class sessions are started.",
                "h_cloud" to "Cloud Configuration", "h_cloud_d" to "Initial action panel to link Google Drive. Once configured, your data will sync automatically, allowing multiple devices to share the same info instantly.",
                "m_occ" to "Occupancy & Capacity", "m_occ_d" to "Real-time occupancy control. Shows how many people are in the venue versus the maximum limit. The progress bar changes color to warn you as capacity is reached.",
                "m_inc" to "Monthly Income", "m_inc_d" to "Displays total revenue for the current calendar month. Includes a security feature (eye icon) to hide the amount for private consultation.",
                "h_metrics" to "Population Stats", "h_metrics_d" to "Grid with the four pillars of your gym: Total registered, Active members (paid up), Debtors (overdue), and recent enrollments (New).",
                "m_ast" to "Daily Attendance", "m_ast_d" to "Unique entry counter for the day. Allows you to monitor the flow of people who have entered the gym today.",
                "h_gst" to "Schedule Management", "h_gst_d" to "Direct access to the class grid. Organize public classes, assign teachers, and define the slots your members will see in their app.",
                "m_qr" to "Gym QR", "m_qr_d" to "Display the local's official code. By scanning it, members link their app and can record attendance autonomously without staff intervention.",
                "m_scan" to "Member Scanner", "m_scan_d" to "Camera tool for manual validation. Scan the member's personal QR code to record their entry quickly and securely.",
                "h_recent" to "Real-Time Monitor", "h_recent_d" to "Live history of the last 10 recorded entries. View the photo, name, and exact entry time of each person who just arrived.",
                "g_plans" to "Plans and Fees", "g_plans_d" to "Create and edit subscription options (e.g., Full Pass). Define prices and durations that your members will see.",
                "g_profs" to "Professors", "g_profs_d" to "Manage your staff, assign distinctive colors, and manage their access profiles.",
                "g_sched" to "Schedule and Agenda", "g_sched_d" to "Define the class grid and events. This agenda is public so your members know what activities are available each day.",
                "s_skin" to "Skin", "s_skin_d" to "Full customization: change button colors, shapes (curves), fonts, and upload your official brand logo.",
                "s_desig" to "Designation", "s_desig_d" to "Change how the app calls your members (e.g., calling them 'Athletes' instead of 'Students' across the interface).",
                "s_lang" to "Language", "s_lang_d" to "Configure the interface language (Spanish/English) to suit your preferences.",
                "s_net" to "Network", "s_net_d" to "Configure the attendance server. This address links the app to your cloud database.",
                "s_qr" to "Attendance QR Code", "s_qr_d" to "Generate your gym's official QR. You can share it for members to scan or send it to print directly.",
                "s_assist" to "Virtual Assistant", "s_assist_d" to "From here you can view the assistant instructions or access technical support if you need personalized help.",
                "s_contact" to "Contact and Support", "s_contact_d" to "Direct access to our official channels (WhatsApp, Email) and the bug report screen to send us technical issues.",
                "s_currency" to "Currency", "s_currency_d" to "Change the currency symbol ($, €, etc.) and the name of the currency shown in income and plans.",
                "st_cats" to "Categories", "st_cats_d" to "Organize your products by groups with custom colors. A clear structure helps the admin manage stock and members find what they need quickly.",
                "st_add" to "Products", "st_add_d" to "Upload items, photos, prices, and control the stock of the products you sell at the gym. Real-time preview available.",
                "st_view" to "Public Store", "st_view_d" to "View the catalog exactly as members see it. Verify that photos and prices are correct from the customer's perspective.",
                "m_total" to "Total Members", "m_total_d" to "Access the full list of all registered people, regardless of their current payment status.",
                "m_new" to "New this Month", "m_new_d" to "Quickly identify people who joined during this calendar month for follow-up.",
                "m_debt" to "Debtors / Expired", "m_debt_d" to "Automatic list of members with overdue fees. The system detects them by comparing today's date with their last payment.",
                "m_search" to "Search", "m_search_d" to "Search by first or last name to find the technical sheet of any member instantly.",
                "m_rec" to "Recent Activity", "m_rec_d" to "Shows the latest registered entries. Useful for verifying who just walked in without opening the full list.",
                "m_act" to "Active", "m_act_d" to "Count of members who have a current subscription and are up to date with their payments."
            )
        }
    }

    val homeItems = listOf(
        GuideItemData(t["h_header"] ?: "", t["h_header_d"] ?: "", Icons.Default.AddPhotoAlternate),
        GuideItemData(t["h_status"] ?: "", t["h_status_d"] ?: "", Icons.Default.CheckCircle, Color(0xFF00C853)),
        GuideItemData(t["h_cloud"] ?: "", t["h_cloud_d"] ?: "", Icons.Default.CloudQueue),
        GuideItemData(t["m_occ"] ?: "", t["m_occ_d"] ?: "", Icons.Default.Speed),
        GuideItemData(t["m_inc"] ?: "", t["m_inc_d"] ?: "", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50)),
        GuideItemData(t["h_metrics"] ?: "", t["h_metrics_d"] ?: "", Icons.Default.GridView, accentColor),
        GuideItemData(t["m_ast"] ?: "", t["m_ast_d"] ?: "", Icons.AutoMirrored.Filled.FactCheck, Color(0xFF9C27B0)),
        GuideItemData(t["h_gst"] ?: "", t["h_gst_d"] ?: "", Icons.Default.Schedule, Color(0xFFE91E63)),
        GuideItemData(t["m_qr"] ?: "", t["m_qr_d"] ?: "", Icons.Default.QrCode2),
        GuideItemData(t["m_scan"] ?: "", t["m_scan_d"] ?: "", Icons.Default.QrCodeScanner),
        GuideItemData(t["h_recent"] ?: "", t["h_recent_d"] ?: "", Icons.Default.History)
    )
    val storeItems = listOf(
        GuideItemData(t["st_cats"] ?: "", t["st_cats_d"] ?: "", Icons.Default.Category), 
        GuideItemData(t["st_add"] ?: "", t["st_add_d"] ?: "", Icons.Default.Inventory), 
        GuideItemData(t["st_view"] ?: "", t["st_view_d"] ?: "", Icons.Default.Storefront)
    )
    val membersItems = listOf(
        GuideItemData(t["m_total"] ?: "", t["m_total_d"] ?: "", Icons.Default.Groups), 
        GuideItemData(t["m_new"] ?: "", t["m_new_d"] ?: "", Icons.Default.PersonAdd, Color(0xFFFFC107)), 
        GuideItemData(t["m_act"] ?: "", t["m_act_d"] ?: "", Icons.Default.Person, Color(0xFF4CAF50)),
        GuideItemData(t["m_debt"] ?: "", t["m_debt_d"] ?: "", Icons.Default.Warning, Color(0xFFF44336))
    )
    val profsItems = listOf(
        GuideItemData(t["g_profs"] ?: "", t["g_profs_d"] ?: "", painterResource(R.drawable.assistant)), 
        GuideItemData(t["g_sched"] ?: "", t["g_sched_d"] ?: "", Icons.Default.Schedule)
    )
    val configItems = listOf(
        GuideItemData(t["s_desig"] ?: "", t["s_desig_d"] ?: "", Icons.Default.Badge),
        GuideItemData(t["s_lang"] ?: "", t["s_lang_d"] ?: "", painterResource(R.drawable.language)),
        GuideItemData(t["s_net"] ?: "", t["s_net_d"] ?: "", painterResource(R.drawable.drive)),
        GuideItemData(t["s_qr"] ?: "", t["s_qr_d"] ?: "", Icons.Default.QrCode2),
        GuideItemData(t["s_skin"] ?: "", t["s_skin_d"] ?: "", Icons.Default.TheaterComedy), 
        // GuideItemData(t["s_assist"] ?: "", t["s_assist_d"] ?: "", painterResource(id = R.drawable.ayuda)),
        GuideItemData(t["s_contact"] ?: "", t["s_contact_d"] ?: "", Icons.Default.SupportAgent),
        GuideItemData(t["s_currency"] ?: "", t["s_currency_d"] ?: "", Icons.Default.AttachMoney),
        GuideItemData(t["h_cloud"] ?: "", t["h_cloud_d"] ?: "", Icons.Default.CloudQueue)
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
            AdminBottomNavigation(
                currentRoute = "help",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onMembersClick = onMembersClick,
                onProfessorsClick = onProfessorsClick,
                onSettingsClick = onSettingsClick,
                accentColor = accentColor
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            
            AssistantConfigCard(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                title = t["assistant_title"] ?: "COPI Assistant",
                desc = t["assistant_desc"] ?: "",
                enabledText = t["enabled"] ?: "Enabled",
                disabledText = t["disabled"] ?: "Disabled",
                isEnabled = assistantEnabled,
                accentColor = accentColor,
                shape = shape,
                onToggle = { 
                    scope.launch { 
                        val newState = !assistantEnabled
                        dataStoreManager.setAssistantEnabled(newState)
                        if (newState) {
                            dataStoreManager.setWelcomeDismissed(false)
                            dataStoreManager.setCloudHelpDismissed(false)
                            dataStoreManager.setStep2HelpDismissed(false)
                            dataStoreManager.setDelayHelpDismissed(false)
                            dataStoreManager.setReadyDismissed(false)
                            dataStoreManager.setDashboardTourStep1Dismissed(false)
                            dataStoreManager.setDashboardTourStep1CentralDismissed(false)
                            dataStoreManager.setSettingsTourSkinDismissed(false)
                            dataStoreManager.setAppearanceTourDismissed(false)
                        }
                    } 
                },
                onGuideClick = if (gymApiUrl.isBlank() || googleToken.isNullOrBlank()) {
                    { showAuthGuide = true }
                } else null,
                guideButtonText = t["auth_guide"] ?: "Authorization Guide"
            )
            Spacer(Modifier.height(8.dp))

            

            val tabs = listOf(
                rememberVectorPainter(Icons.Default.Dashboard),
                rememberVectorPainter(Icons.Default.Storefront),
                rememberVectorPainter(Icons.Default.Group),
                painterResource(R.drawable.assistant),
                rememberVectorPainter(Icons.Default.Settings)
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabsScrollState)
                ) {
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
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = tween(1000, easing = FastOutSlowInEasing))
                                            }
                                        } else if (delta > 3f && pagerState.currentPage > 0) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = tween(1000, easing = FastOutSlowInEasing))
                                            }
                                        }
                                    }
                                }
                            )
                        ) {
                            tabs.forEachIndexed { index, icon ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(tabWidth)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { 
                                            scope.launch { 
                                                pagerState.animateScrollToPage(index, animationSpec = tween(1000, easing = FastOutSlowInEasing)) 
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) accentColor else TextGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
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
                    val items = when (page) { 0 -> homeItems; 1 -> storeItems; 2 -> membersItems; 3 -> profsItems; else -> configItems }
                    val sectionTitle = when (page) { 0 -> t["sec_home"] ?: "HOME"; 1 -> t["sec_store"] ?: "STORE"; 2 -> t["sec_members"] ?: "MEMBERS"; 3 -> t["sec_profs"] ?: "TEAM"; else -> t["sec_config"] ?: "SYSTEM" }
                    val sectionDesc = when (page) { 0 -> t["sec_home_desc"] ?: ""; 1 -> t["sec_store_desc"] ?: ""; 2 -> t["sec_members_desc"] ?: ""; 3 -> t["sec_profs_desc"] ?: ""; else -> t["sec_config_desc"] ?: "" }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GuideSectionHeader(sectionTitle, sectionDesc, accentColor)
                        items.forEach { item -> GuideItem(item, accentColor, shape) }
                    }
                }
            }
        }
    }

    if (showAuthGuide) {
        CloudAuthInstructionsDialog(onDismiss = { showAuthGuide = false })
    }
}


@Composable
fun GuideSectionHeader(title: String, subtitle: String, accentColor: Color) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = title, color = accentColor, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Text(text = subtitle, color = TextGray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun GuideItem(data: GuideItemData, accentColor: Color, shape: androidx.compose.ui.graphics.Shape) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val titleColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val descColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.3f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = buttonBgColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, (data.iconColor ?: accentColor).copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                when (data.icon) {
                    is ImageVector -> Icon(imageVector = data.icon, contentDescription = null, tint = data.iconColor ?: accentColor, modifier = Modifier.size(22.dp))
                    is Painter -> Icon(painter = data.icon, contentDescription = null, tint = data.iconColor ?: accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = data.title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(text = data.description, color = descColor, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

data class GuideItemData(val title: String, val description: String, val icon: Any, val iconColor: Color? = null, val isHeader: Boolean = false)

@Composable
fun AssistantConfigCard(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    enabledText: String,
    disabledText: String,
    isEnabled: Boolean,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onToggle: () -> Unit,
    onGuideClick: (() -> Unit)? = null,
    guideButtonText: String = ""
) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val titleColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val descColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(modifier = modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.3f), shape), shape = shape, colors = CardDefaults.cardColors(containerColor = buttonBgColor)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color.LightGray, CircleShape), contentAlignment = Alignment.Center) { 
                    Icon(painter = painterResource(id = R.drawable.ayuda), contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(30.dp)) 
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(desc, color = descColor, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (isEnabled) Color(0xFF4CAF50) else accentColor), shape = shape) { Text(if (isEnabled) enabledText else disabledText, color = if (isEnabled) Color.White else Color.Black, fontWeight = FontWeight.Black) }
            
            if (onGuideClick != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGuideClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                    shape = shape
                ) {
                    Text(guideButtonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CloudAuthInstructionsDialog(onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val images = listOf(R.drawable.cloud_01, R.drawable.cloud_02, R.drawable.cloud_03, R.drawable.cloud_04)
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(500.dp),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF081C24))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Guía de Autorización", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    Image(
                        painter = painterResource(id = images[page]),
                        contentDescription = "Step ${page + 1}",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(4) { iteration ->
                        val color = if (pagerState.currentPage == iteration) PrimaryTurquoise else Color.Gray
                        Box(modifier = Modifier.padding(4.dp).size(8.dp).background(color, CircleShape))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
            }
        }
    }
}
