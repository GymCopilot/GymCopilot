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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
fun ProfessorHelpScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val memberDesignation by dataStoreManager.getMemberDesignation().collectAsState(initial = "Alumno")
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    val pagerState = rememberPagerState(pageCount = { 5 })
    val tabsScrollState = rememberScrollState()

    val t = remember(appLanguage, memberDesignation) {
        val plural = when(memberDesignation) {
            "Alumno" -> if (appLanguage == "Español") "alumnos" else "students"
            "Socio" -> if (appLanguage == "Español") "socios" else "partners"
            else -> "${memberDesignation}s"
        }
        
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Guía del Profesor",
                "sec_dash" to "DASHBOARD", "sec_dash_d" to "Panel de control personal",
                "sec_store" to "TIENDA", "sec_store_d" to "Catálogo para socios",
                "sec_sched" to "HORARIOS", "sec_sched_d" to "Gestión de clases",
                "sec_mems" to "MIS $plural", "sec_mems_d" to "Tus socios asignados",
                "sec_ruts" to "RUTINAS", "sec_ruts_d" to "Planes de entrenamiento",
                
                "h_staff" to "Staff de Profesores", "h_staff_d" to "Muestra a todo el equipo docente. Puedes deslizar para ver a todos y tocar su foto para vincular su perfil y ver sus horarios si están autorizados.",
                "h_stats" to "Métricas de Rutinas", "h_stats_d" to "Panel rápido que indica cuántas rutinas han sido completadas frente a las asignadas por ti durante el mes actual.",
                "h_mems" to "Mis Estudiantes", "h_mems_d" to "Tarjeta central que muestra la cantidad de alumnos asignados directamente a tu supervisión. Al tocarla accedes a su listado exclusivo.",
                "h_session" to "Sesión de Clase", "h_session_d" to "Botón dinámico para 'Iniciar Clase'. Al activarlo, el sistema marca que estás en el local y permite supervisar el aforo.",
                "h_ast_std" to "Asistencia Estudiantes", "h_ast_std_d" to "Acceso directo para validar manualmente la entrada de los alumnos. Ideal para cuando un socio olvida su teléfono.",
                "h_ast_my" to "Marcar Mi Asistencia", "h_ast_my_d" to "Escáner personal para registrar tu propia entrada al gimnasio mediante el código QR de administración.",
                "h_sched" to "Mis Horarios", "h_sched_d" to "Consulta tu agenda personal de clases asignadas para organizar tu semana.",
                "h_classes" to "Grilla de Clases", "h_classes_d" to "Acceso a la grilla pública de actividades del gimnasio para ver la disponibilidad de salones y turnos.",
                "h_recent" to "Actividad Reciente", "h_recent_d" to "Monitor en tiempo real de los últimos alumnos que han ingresado al local bajo tu supervisión.",

                "r_dash" to "Dashboard de Rutinas", "r_dash_d" to "Resumen estadístico de tus planes: total de rutinas creadas, nivel de éxito y asignaciones vigentes.",
                "r_search" to "Buscador y Filtros", "r_search_d" to "Encuentra rápidamente cualquier plan de entrenamiento por nombre o usa las categorías para filtrar por objetivo.",
                "r_actions" to "Acciones Rápidas", "r_actions_d" to "Crea nuevas rutinas, accede a tus favoritas o gestiona las categorías de entrenamiento (Pecho, Espalda, etc.).",
                "r_item" to "Ficha de Rutina", "r_item_d" to "Cada tarjeta te permite iniciar la ejecución (Play), marcar como favorita, asignar a un alumno, editar ejercicios o eliminarla.",
                "r_builder" to "Constructor (Paso 1)", "r_builder_d" to "Define el nombre, descripción, grupo muscular predominante y el objetivo principal del entrenamiento (Hipertrofia, Fuerza, etc.).",
                "r_exec" to "Modo Ejecución", "r_exec_d" to "Cronómetro inteligente que guía la rutina. Muestra series, repeticiones, peso sugerido y tiempos de descanso automáticos.",
                "r_prog" to "Progreso Vivo", "r_prog_d" to "Barra de porcentaje que indica cuánto falta para terminar la sesión y monitor de ejercicios completados.",

                "m_staff" to "Staff de Profesores", "m_staff_d" to "Visualiza a tus colegas. Al tocar su foto, puedes vincular su perfil para ver sus horarios si eres administrador o colega autorizado.",
                "m_stats" to "Estadísticas de Rutinas", "m_stats_d" to "Controla cuántas rutinas has asignado y cuántas han sido completadas por tus $plural este mes.",
                "m_class" to "Iniciar/Finalizar Clase", "m_class_d" to "Usa el botón central para marcar el inicio de tu jornada. Esto notificará al sistema que estás activo en el local.",
                "m_scan" to "Marcar Mi Asistencia", "m_scan_d" to "Usa el escáner para registrar tu propio ingreso leyendo el QR de administración si el gimnasio lo requiere.",
                
                "st_view" to "Tienda Pública", "st_view_d" to "Acceso rápido al catálogo que ven los socios para que puedas asesorarlos sobre suplementos o equipo.",
                "st_cats" to "Navegación por Categorías", "st_cats_d" to "Explora el catálogo filtrando por grupos (Suplementos, Ropa, Equipo). Ayuda a tus alumnos a encontrar productos que complementen su entrenamiento.",
                "st_req" to "Publicación de Productos", "st_req_d" to "Los profesores tienen acceso de consulta. Para añadir nuevos artículos, cambiar precios o subir stock, debes solicitarlo directamente al administrador.",
                "st_search" to "Buscador de Artículos", "st_search_d" to "Encuentra rápidamente cualquier producto del catálogo escribiendo su nombre o marca en la barra de búsqueda superior.",
                "st_cart" to "Carrito de Compras", "st_cart_d" to "Permite previsualizar los artículos seleccionados, verificar cantidades y el monto total antes de coordinar la entrega con el local.",
                
                "sch_grid" to "Grilla de Clases", "sch_grid_d" to "Organiza tus bloques horarios. Lo que configures aquí será visible para todos los socios en tiempo real.",
                "sch_title" to "Gestión de Horarios",
                "sch_nav" to "Navegación Temporal", "sch_nav_d" to "Usa el botón 'HOY' para volver a la fecha actual y las flechas laterales para desplazarte entre días, semanas o meses.",
                "sch_views" to "Selector de Vistas", "sch_views_d" to "Alterna entre vistas de Día (detalle por hora), Semana (panorama semanal) o Mes (calendario completo) para organizar tu staff.",
                "sch_sync" to "Sincronización", "sch_sync_d" to "Botón de refresco (esquina superior derecha). Úsalo para actualizar la agenda con los últimos cambios realizados por la administración.",
                "sch_active" to "Perfil Vinculado", "sch_active_d" to "Indica de quién es la agenda que estás visualizando. Asegúrate de que figure tu nombre para gestionar tus propias clases.",

                "mem_list" to "Mis $plural", "mem_list_d" to "Listado exclusivo de los socios que tienes bajo tu supervisión. Accede a sus fichas para ver su progreso.",
                "mem_title" to "Gestión de Estudiantes",
                "mem_metrics" to "Grilla de Estados", "mem_metrics_d" to "Visualiza rápidamente el total de alumnos asignados desglosados en: Activos, Deudores y Desertores.",
                "mem_search" to "Buscador de Alumnos", "mem_search_d" to "Filtra el listado por nombre para encontrar la ficha técnica de cualquier integrante de forma instantánea.",
                "mem_quick" to "Acciones de Lista", "mem_quick_d" to "Iconos directos en cada socio: abre un chat privado (icono burbuja) o asigna una rutina (icono mancuerna) sin salir del listado.",
                
                "det_title" to "Detalle del Estudiante",
                "det_photo" to "Foto y QR", "det_photo_d" to "Visualiza el perfil del socio. El botón QR permite que el alumno escanee tu pantalla para vincularse si aún no lo ha hecho.",
                "det_info" to "Ficha Física y Contacto", "det_info_d" to "Información clave: WhatsApp/Teléfono, Dirección, Peso y Altura actuales del deportista.",
                "det_member" to "Estatus de Membresía", "det_member_d" to "Muestra el estado de pago (ACTIVO/VENCIDO), la fecha de la próxima renovación, el plan contratado y el precio.",
                "det_stats" to "Monitor de Asistencia", "det_stats_d" to "Contador histórico de visitas y fecha/hora exacta de la última entrada registrada al gimnasio.",

                "mem_ast" to "Asistencia de $plural", "mem_ast_d" to "Valida la entrada de tus socios asignados de forma manual si es necesario.",
                
                "rut_create" to "Crear Rutinas", "rut_create_d" to "Diseña planes personalizados. Puedes añadir ejercicios, series, repeticiones y videos demostrativos.",
                "rut_assign" to "Asignar Planes", "rut_assign_d" to "Vincula una rutina creada a uno o varios $plural. Ellos recibirán una notificación al instante."
            )
        } else {
            mapOf(
                "title" to "Professor Guide",
                "sec_dash" to "DASHBOARD", "sec_dash_d" to "Personal control panel",
                "sec_store" to "STORE", "sec_store_d" to "Catalog for members",
                "sec_sched" to "SCHEDULE", "sec_sched_d" to "Class management",
                "sec_mems" to "MY $plural", "sec_mems_d" to "Your assigned members",
                "sec_ruts" to "ROUTINES", "sec_ruts_d" to "Training plans",

                "h_staff" to "Staff Professors", "h_staff_d" to "View your colleagues. Tap a photo to link their profile and see their schedule.",
                "h_stats" to "Routine Stats", "h_stats_d" to "Track how many routines you've assigned and completed this month.",
                "h_mems" to "My Students", "h_mems_d" to "Central card showing assigned students. Tap to access their exclusive list.",
                "h_session" to "Class Session", "h_session_d" to "Dynamic 'Start Class' button. Notifies the system you are active in the gym.",
                "h_ast_std" to "Student Attendance", "h_ast_std_d" to "Manual entry validation for students. Perfect if they forget their phone.",
                "h_ast_my" to "Mark My Attendance", "h_ast_my_d" to "Personal scanner to record your own entry using the admin QR.",
                "h_sched" to "My Schedule", "h_sched_d" to "Check your assigned class agenda to organize your week.",
                "h_classes" to "Class Grid", "h_classes_d" to "Access to the public activity grid to see room availability.",
                "h_recent" to "Recent Activity", "h_recent_d" to "Real-time monitor of the latest students who entered under your supervision.",

                "r_dash" to "Routine Dashboard", "r_dash_d" to "Statistical summary of your plans: total created, success rate, and active assignments.",
                "r_search" to "Search & Filters", "r_search_d" to "Quickly find any plan by name or use categories to filter by goal.",
                "r_actions" to "Quick Actions", "r_actions_d" to "Create new routines, access favorites, or manage training categories.",
                "r_item" to "Routine Card", "r_item_d" to "Allows you to start execution (Play), mark as favorite, assign, edit, or delete.",
                "r_builder" to "Builder (Step 1)", "r_builder_d" to "Set the name, description, muscle group, and main goal of the workout.",
                "r_exec" to "Execution Mode", "r_exec_d" to "Smart timer that guides the routine. Shows sets, reps, weight, and rest times.",
                "r_prog" to "Live Progress", "r_prog_d" to "Percentage bar indicating session completion and completed exercises monitor.",
                
                "m_staff" to "Staff Professors", "m_staff_d" to "View your colleagues. Tap a photo to link their profile and see their schedule.",
                "m_stats" to "Routine Stats", "m_stats_d" to "Track how many routines you've assigned and completed this month.",
                "m_class" to "Start/End Class", "m_class_d" to "Use the center button to mark your work session. This notifies the system you are active.",
                "m_scan" to "Mark My Attendance", "m_scan_d" to "Use the scanner to record your own entry by reading the admin QR.",
                
                "st_view" to "Public Store", "st_view_d" to "Quick access to the member catalog to advise them on supplements or gear.",
                "st_cats" to "Category Navigation", "st_cats_d" to "Explore the catalog filtering by groups (Supplements, Apparel, Gear). Help your students find products that complement their training.",
                "st_req" to "Product Publishing", "st_req_d" to "Professors have view-only access. To add new items, change prices, or update stock, you must request it directly from the administrator.",
                "st_search" to "Item Search", "st_search_d" to "Quickly find any product in the catalog by typing its name or brand in the top search bar.",
                "st_cart" to "Shopping Cart", "st_cart_d" to "Allows previewing selected items, verifying quantities, and the total amount before coordinating delivery with the store.",
                
                "sch_grid" to "Class Grid", "sch_grid_d" to "Organize your time slots. Your changes here will be visible to all members.",
                "sch_title" to "Schedule Management",
                "sch_nav" to "Time Navigation", "sch_nav_d" to "Use the 'TODAY' button to return to the current date and the side arrows to move between days, weeks, or months.",
                "sch_views" to "View Selector", "sch_views_d" to "Switch between Day (hourly detail), Week (weekly overview), or Month (full calendar) views to organize your staff.",
                "sch_sync" to "Synchronization", "sch_sync_d" to "Refresh button (top right corner). Use it to update the agenda with the latest changes made by administration.",
                "sch_active" to "Linked Profile", "sch_active_d" to "Indicates whose agenda you are viewing. Make sure your name appears to manage your own classes.",

                "mem_list" to "My $plural", "mem_list_d" to "Exclusive list of members under your supervision. Access their files to see progress.",
                "mem_title" to "Student Management",
                "mem_metrics" to "Status Grid", "mem_metrics_d" to "Quickly view assigned members broken down into: Active, Debtors, and Leavers.",
                "mem_search" to "Member Search", "mem_search_d" to "Filter the list by name to find any member's technical sheet instantly.",
                "mem_quick" to "List Actions", "mem_quick_d" to "Direct icons for each member: open a private chat (bubble icon) or assign a routine (dumbbell icon).",

                "det_title" to "Student Detail",
                "det_photo" to "Photo & QR", "det_photo_d" to "View the member's profile. The QR button allows the student to scan your screen to link up.",
                "det_info" to "Physical File & Contact", "det_info_d" to "Key info: WhatsApp/Phone, Address, current Weight and Height of the athlete.",
                "det_member" to "Membership Status", "det_member_d" to "Shows payment status (ACTIVE/EXPIRED), next renewal date, contracted plan, and price.",
                "det_stats" to "Attendance Monitor", "det_stats_d" to "Historical visit counter and exact date/time of the last recorded entry.",

                "mem_ast" to "Member Attendance", "mem_ast_d" to "Manually validate entries for your assigned members if needed.",
                
                "rut_create" to "Create Routines", "rut_create_d" to "Design custom plans with exercises, sets, reps, and demo videos.",
                "rut_assign" to "Assign Plans", "rut_assign_d" to "Link a routine to one or multiple members. They will receive an instant notice."
            )
        }
    }

    val dashItems = listOf(
        GuideItemData(t["h_staff"]!!, t["h_staff_d"]!!, Icons.Default.Groups),
        GuideItemData(t["h_stats"]!!, t["h_stats_d"]!!, Icons.Default.Analytics, Color(0xFF2196F3)),
        GuideItemData(t["h_mems"]!!, t["h_mems_d"]!!, Icons.Default.Group, accentColor),
        GuideItemData(t["h_session"]!!, t["h_session_d"]!!, Icons.Default.PlayCircle, Color(0xFF00C853)),
        GuideItemData(t["h_ast_std"]!!, t["h_ast_std_d"]!!, Icons.AutoMirrored.Filled.FactCheck, accentColor),
        GuideItemData(t["h_ast_my"]!!, t["h_ast_my_d"]!!, Icons.Default.QrCodeScanner, accentColor),
        GuideItemData(t["h_sched"]!!, t["h_sched_d"]!!, Icons.Default.Schedule, Color(0xFFE91E63)),
        GuideItemData(t["h_classes"]!!, t["h_classes_d"]!!, painterResource(R.drawable.gym_class), accentColor),
        GuideItemData(t["h_recent"]!!, t["h_recent_d"]!!, Icons.Default.History, TextGray)
    )
    val storeItems = listOf(
        GuideItemData(t["st_view"]!!, t["st_view_d"]!!, Icons.Default.Storefront),
        GuideItemData(t["st_cats"]!!, t["st_cats_d"]!!, Icons.Default.Category, accentColor),
        GuideItemData(t["st_search"]!!, t["st_search_d"]!!, Icons.Default.Search),
        GuideItemData(t["st_cart"]!!, t["st_cart_d"]!!, Icons.Default.ShoppingCart, Color(0xFFFFC107)),
        GuideItemData(t["st_req"]!!, t["st_req_d"]!!, Icons.Default.CloudUpload, Color(0xFF2196F3))
    )
    val schedItems = listOf(
        GuideItemData(t["sch_title"]!!, "", Icons.Default.Title, isHeader = true),
        GuideItemData(t["sch_nav"]!!, t["sch_nav_d"]!!, Icons.Default.Today, accentColor),
        GuideItemData(t["sch_views"]!!, t["sch_views_d"]!!, Icons.Default.CalendarViewWeek, Color(0xFF2196F3)),
        GuideItemData(t["sch_sync"]!!, t["sch_sync_d"]!!, Icons.Default.Sync, Color(0xFF00C853)),
        GuideItemData(t["sch_active"]!!, t["sch_active_d"]!!, Icons.Default.Badge, Color(0xFFFFC107)),
        GuideItemData(t["sch_grid"]!!, t["sch_grid_d"]!!, Icons.Default.CalendarMonth, Color(0xFFE91E63))
    )
    val memItems = listOf(
        GuideItemData(t["mem_title"]!!, "", Icons.Default.Title, isHeader = true),
        GuideItemData(t["mem_metrics"]!!, t["mem_metrics_d"]!!, Icons.Default.GridView, Color(0xFF2196F3)),
        GuideItemData(t["mem_search"]!!, t["mem_search_d"]!!, Icons.Default.Search),
        GuideItemData(t["mem_list"]!!, t["mem_list_d"]!!, Icons.Default.Group, accentColor),
        GuideItemData(t["mem_quick"]!!, t["mem_quick_d"]!!, Icons.Default.FlashOn, Color(0xFFFFC107)),
        
        GuideItemData(t["det_title"]!!, "", Icons.Default.Title, isHeader = true),
        GuideItemData(t["det_photo"]!!, t["det_photo_d"]!!, Icons.Default.AccountCircle, accentColor),
        GuideItemData(t["det_info"]!!, t["det_info_d"]!!, Icons.Default.ContactPhone, Color(0xFF2196F3)),
        GuideItemData(t["det_member"]!!, t["det_member_d"]!!, Icons.Default.CardMembership, Color(0xFF00C853)),
        GuideItemData(t["det_stats"]!!, t["det_stats_d"]!!, Icons.Default.History, Color(0xFF9C27B0))
    )
    val rutItems = listOf(
        GuideItemData(t["r_dash"]!!, t["r_dash_d"]!!, Icons.Default.Dashboard, accentColor),
        GuideItemData(t["r_actions"]!!, t["r_actions_d"]!!, Icons.Default.FlashOn, Color(0xFFFFC107)),
        GuideItemData(t["r_item"]!!, t["r_item_d"]!!, Icons.AutoMirrored.Filled.ListAlt, accentColor),
        GuideItemData(t["r_builder"]!!, t["r_builder_d"]!!, Icons.Default.Build, accentColor),
        GuideItemData(t["r_exec"]!!, t["r_exec_d"]!!, Icons.Default.Timer, Color(0xFF00C853)),
        GuideItemData(t["r_prog"]!!, t["r_prog_d"]!!, Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF2196F3))
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
            ProfessorBottomNavigation(
                currentRoute = "professor_help",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onScheduleClick = onScheduleClick,
                onMembersClick = onMembersClick,
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
                rememberVectorPainter(Icons.Default.Group),
                rememberVectorPainter(Icons.Default.FitnessCenter)
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
                    val items = when (page) { 0 -> dashItems; 1 -> storeItems; 2 -> schedItems; 3 -> memItems; else -> rutItems }
                    val sectionTitle = when (page) { 0 -> t["sec_dash"]!!; 1 -> t["sec_store"]!!; 2 -> t["sec_sched"]!!; 3 -> t["sec_mems"]!!; else -> t["sec_ruts"]!! }
                    val sectionDesc = when (page) { 0 -> t["sec_dash_d"]!!; 1 -> t["sec_store_d"]!!; 2 -> t["sec_sched_d"]!!; 3 -> t["sec_mems_d"]!!; else -> t["sec_ruts_d"]!! }
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GuideSectionHeader(sectionTitle, sectionDesc, accentColor)
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 8.dp),
                            thickness = 1.dp,
                            color = accentColor.copy(alpha = 0.2f)
                        )
                        items.forEach { item ->
                            if (item.isHeader) {
                                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                    Text(
                                        text = item.title,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 4.dp),
                                        thickness = 0.5.dp,
                                        color = accentColor.copy(alpha = 0.3f)
                                    )
                                }
                            } else {
                                GuideItem(item, accentColor, shape)
                            }
                        }
                    }
                }
            }
        }
    }
}
