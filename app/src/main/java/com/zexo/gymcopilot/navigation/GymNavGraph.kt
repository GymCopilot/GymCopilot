package com.zexo.gymcopilot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.LockConfig
import com.zexo.gymcopilot.network.BillingManager
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.repository.ScheduleRepository
import com.zexo.gymcopilot.repository.GoogleCloudRepository
import com.zexo.gymcopilot.ui.screens.*
import com.zexo.gymcopilot.ui.viewmodels.AttendanceViewModel
import com.zexo.gymcopilot.ui.viewmodels.AttendanceViewModelFactory
import com.zexo.gymcopilot.ui.viewmodels.ChatViewModel
import com.zexo.gymcopilot.ui.viewmodels.ChatViewModelFactory
import com.zexo.gymcopilot.ui.viewmodels.ScheduleViewModel
import com.zexo.gymcopilot.ui.viewmodels.ScheduleViewModelFactory
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object AdminDashboard : Screen("admin_dashboard")
    object MemberDashboard : Screen("member_dashboard")
    object ProfessorDashboard : Screen("professor_dashboard")
    object AttendanceScanner : Screen("attendance_scanner")
    object Profile : Screen("profile")
    object Members : Screen("members?filter={filter}") {
        fun createRoute(filter: String? = null) = if (filter != null) "members?filter=$filter" else "members"
    }
    object MemberDetail : Screen("member_detail/{email}") {
        fun createRoute(email: String) = "member_detail/$email"
    }
    object AddMember : Screen("add_member")
    object ManageProfessors : Screen("manage_professors/{isAdmin}") {
        fun createRoute(isAdmin: Boolean) = "manage_professors/$isAdmin"
    }
    object EditProfessor : Screen("edit_professor/{id}") {
        fun createRoute(id: String) = "edit_professor/$id"
    }
    object ManageSchedule : Screen("manage_schedule")
    object ProfessorSchedule : Screen("professor_schedule")
    object ManageClasses : Screen("manage_classes")
    object MemberSchedule : Screen("member_schedule")
    object Routines : Screen("routines")
    object RoutineCreation : Screen("routine_creation")
    object RoutineEdit : Screen("routine_edit/{id}") {
        fun createRoute(id: String) = "routine_edit/$id"
    }
    object RoutineDetail : Screen("routine_detail/{id}") {
        fun createRoute(id: String) = "routine_detail/$id"
    }
    object Chat : Screen("chat/{recipientId}") {
        fun createRoute(recipientId: String) = "chat/$recipientId"
    }
    object Store : Screen("store")
    object PublicStore : Screen("public_store")
    object Category : Screen("category")
    object CategoryProducts : Screen("category_products/{categoryId}") {
        fun createRoute(categoryId: String) = "category_products/$categoryId"
    }
    object AddProduct : Screen("add_product")
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }
    object Cart : Screen("cart")
    object AppSettings : Screen("app_settings")
    object Appearance : Screen("appearance?expandSection={expandSection}") {
        fun createRoute(expandSection: String? = null) =
            if (expandSection != null) "appearance?expandSection=$expandSection" else "appearance"
    }
    object Currency : Screen("currency")
    object GymConfig : Screen("gym_config")
    object NetworkConfig : Screen("network_config")
    object SubscriptionPlans : Screen("subscription_plans")
    object Help : Screen("help")
    object ProfessorHelp : Screen("professor_help")
    object MemberHelp : Screen("member_help")
    object Contact : Screen("contact")
    object ReportProblem : Screen("report_problem")
    object Paywall : Screen("paywall")
}

@Composable
fun GymNavGraph(
    navController: NavHostController = rememberNavController(),
    attendanceRepository: AttendanceRepository,
    scheduleRepository: ScheduleRepository,
    googleCloudRepository: GoogleCloudRepository,
    dataStoreManager: DataStoreManager,
    billingManager: BillingManager,
    initialUserRole: String? = null,
    initialIsSubscribed: Boolean = false
) {
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = initialUserRole)
    val isSubscribed by billingManager.isSubscribed.collectAsState(initial = initialIsSubscribed)
    val coroutineScope = rememberCoroutineScope()
    
    val attendanceViewModel: AttendanceViewModel = viewModel(
        factory = AttendanceViewModelFactory(attendanceRepository, dataStoreManager)
    )
    
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(scheduleRepository)
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(dataStoreManager, context)
    )

    val onAdminHome = { navController.navigate(Screen.AdminDashboard.route) { popUpTo(0) } }
    val onAdminStore = { navController.navigate(Screen.Store.route) }
    val onAdminMembers = { filter: String? -> navController.navigate(Screen.Members.createRoute(filter)) }
    val onAdminProfessors = { navController.navigate(Screen.ManageProfessors.createRoute(true)) }
    val onAdminSettings = { navController.navigate(Screen.AppSettings.route) }

    val onMemberHome = { 
        if (isSubscribed || LockConfig.IS_ROLE_LOCK_ENABLED == 0) {
            navController.navigate(Screen.MemberDashboard.route) { popUpTo(0) }
        } else {
            navController.navigate(Screen.Paywall.route)
        }
    }
    val onMemberStore = { navController.navigate(Screen.PublicStore.route) }
    val onMemberClasses = { navController.navigate(Screen.MemberSchedule.route) }
    val onMemberRoutines = { navController.navigate(Screen.Routines.route) }
    val onMemberChat: (String?) -> Unit = { recipient -> navController.navigate(Screen.Chat.createRoute(recipient ?: "admin")) }

    val onProfessorHome = { navController.navigate(Screen.ProfessorDashboard.route) { popUpTo(0) } }
    val onProfessorStore = { navController.navigate(Screen.PublicStore.route) }
    val onProfessorSchedule = { navController.navigate(Screen.ProfessorSchedule.route) }
    val onProfessorClasses = { navController.navigate(Screen.ManageClasses.route) }
    val onProfessorMembers = { filter: String? -> navController.navigate(Screen.Members.createRoute(filter)) }
    val onProfessorRoutines = { navController.navigate(Screen.Routines.route) }
    val onProfessorChat = { navController.navigate(Screen.Chat.createRoute("admin")) }

    val onNewRoutine = { navController.navigate(Screen.RoutineCreation.route) }

    NavHost(
        navController = navController,
        startDestination = remember(initialUserRole, initialIsSubscribed) {
            if (LockConfig.IS_ROLE_LOCK_ENABLED == 0) {
                Screen.Login.route
            } else {
                when (initialUserRole) {
                    "admin" -> Screen.AdminDashboard.route
                    "professor" -> Screen.ProfessorDashboard.route
                    "member" -> if (initialIsSubscribed) Screen.MemberDashboard.route else Screen.Paywall.route
                    else -> Screen.Login.route
                }
            }
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onRoleSelected = { role ->
                    when(role) {
                        "admin" -> onAdminHome()
                        "professor" -> onProfessorHome()
                        else -> onMemberHome()
                    }
                },
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.MemberDashboard.route) {
            MemberDashboardScreen(
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onCheckInClick = { navController.navigate(Screen.AttendanceScanner.route) },
                onHomeClick = onMemberHome,
                onStoreClick = onMemberStore,
                onClassesClick = onMemberClasses,
                onRoutinesClick = onMemberRoutines,
                onRoutineDetailClick = { id -> navController.navigate(Screen.RoutineDetail.createRoute(id)) },
                onChatClick = onMemberChat,
                onHelpClick = { navController.navigate(Screen.MemberHelp.route) }
            )
        }
        composable(Screen.MemberSchedule.route) {
            MemberScheduleScreen(
                viewModel = scheduleViewModel,
                onBack = { navController.popBackStack() },
                onHomeClick = onMemberHome,
                onStoreClick = onMemberStore,
                onClassesClick = onMemberClasses,
                onRoutinesClick = onMemberRoutines,
                onChatClick = { onMemberChat(null) }
            )
        }
        composable(Screen.AttendanceScanner.route) {
            AttendanceScannerScreen(
                viewModel = attendanceViewModel,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onAppSettingsClick = onAdminSettings,
                onAppearanceClick = { section -> navController.navigate(Screen.Appearance.createRoute(section)) },
                onStoreClick = onAdminStore,
                onMembersClick = onAdminMembers,
                onProfessorsClick = onAdminProfessors,
                onScheduleClick = { navController.navigate(Screen.ManageSchedule.route) },
                onSubscriptionsClick = { navController.navigate(Screen.SubscriptionPlans.route) },
                attendanceRepository = attendanceRepository,
                scheduleRepository = scheduleRepository,
                googleCloudRepository = googleCloudRepository
            )
        }
        composable(Screen.ProfessorDashboard.route) {
            ProfessorDashboardScreen(
                onHomeClick = onProfessorHome,
                onStoreClick = onProfessorStore,
                onScheduleClick = onProfessorSchedule,
                onClassesClick = onProfessorClasses,
                onMembersClick = onProfessorMembers,
                onRoutinesClick = onProfessorRoutines,
                onChatClick = onProfessorChat,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onHelpClick = { navController.navigate(Screen.ProfessorHelp.route) },
                onProfessorClick = { id -> navController.navigate(Screen.EditProfessor.createRoute(id)) },
                onScanClick = { navController.navigate(Screen.AttendanceScanner.route) },
                attendanceRepository = attendanceRepository
            )
        }
        composable(Screen.ManageClasses.route) {
            ManageClassesScreen(
                viewModel = scheduleViewModel,
                onBack = { navController.popBackStack() },
                onHomeClick = onProfessorHome,
                onStoreClick = onProfessorStore,
                onMembersClick = { onProfessorMembers(null) },
                onRoutinesClick = onProfessorRoutines,
                onChatClick = onProfessorChat
            )
        }
        composable(Screen.Store.route) {
            StoreScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                onAddCategoryClick = { navController.navigate(Screen.Category.route) },
                onViewPublicStoreClick = { navController.navigate(Screen.PublicStore.route) },
                onAddProductClick = { navController.navigate(Screen.AddProduct.route) },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.CategoryProducts.createRoute(categoryId))
                }
            )
        }
        composable(Screen.AddProduct.route) {
            AddProductScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                attendanceRepository = attendanceRepository
            )
        }
        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            AddProductScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                attendanceRepository = attendanceRepository
            )
        }
        composable(Screen.Cart.route) {
            CartScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PublicStore.route) {
            PublicStoreScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.CategoryProducts.createRoute(categoryId))
                },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onHomeClick = when (userRole) {
                    "admin" -> onAdminHome
                    "professor" -> onProfessorHome
                    else -> onMemberHome
                },
                onStoreClick = when (userRole) {
                    "admin" -> onAdminStore
                    else -> onMemberStore
                },
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                onScheduleClick = onProfessorSchedule,
                onClassesClick = onMemberClasses,
                onRoutinesClick = onMemberRoutines,
                onChatClick = { onMemberChat(null) },
                isMember = userRole == "member",
                isProfessor = userRole == "professor"
            )
        }
        composable(
            route = Screen.CategoryProducts.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            CategoryProductsScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct.createRoute(productId))
                },
                onHomeClick = when (userRole) {
                    "admin" -> onAdminHome
                    "professor" -> onProfessorHome
                    else -> onMemberHome
                },
                onStoreClick = when (userRole) {
                    "admin" -> onAdminStore
                    else -> onMemberStore
                },
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                onScheduleClick = onProfessorSchedule,
                onClassesClick = onMemberClasses,
                onRoutinesClick = onMemberRoutines,
                onChatClick = { onMemberChat(null) },
                isAdmin = userRole == "admin",
                isProfessor = userRole == "professor",
                onCategoryClick = { newCategoryId ->
                    if (newCategoryId != categoryId) {
                        navController.navigate(Screen.CategoryProducts.createRoute(newCategoryId)) {
                            popUpTo(Screen.CategoryProducts.createRoute(categoryId)) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(Screen.Category.route) {
            CategoryScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.GymConfig.route) {
            GymConfigScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.NetworkConfig.route) {
            NetworkConfigScreen(
                attendanceRepository = attendanceRepository,
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.AppSettings.route) {
            AppSettingsScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onLanguageClick = { navController.navigate(Screen.GymConfig.route) },
                onNetworkConfigClick = { navController.navigate(Screen.NetworkConfig.route) },
                onCurrencyClick = { navController.navigate(Screen.Currency.route) },
                onManageProfessorsClick = { navController.navigate(Screen.ManageProfessors.createRoute(true)) },
                onAppearanceClick = { navController.navigate(Screen.Appearance.route) },
                onHelpClick = { navController.navigate(Screen.Help.route) },
                onContactClick = { navController.navigate(Screen.Contact.route) }
            )
        }
        composable(
            route = Screen.Appearance.route,
            arguments = listOf(navArgument("expandSection") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val expandSection = backStackEntry.arguments?.getString("expandSection")
            AppearanceScreen(
                initialExpandedSection = expandSection,
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                attendanceRepository = attendanceRepository
            )
        }
        composable(Screen.Currency.route) {
            CurrencySelectionScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onScanClick = { navController.navigate(Screen.AttendanceScanner.route) },
                attendanceRepository = attendanceRepository,
                onSaveSync = { f, l, e, p, a, w, h, photo ->
                    coroutineScope.launch {
                        attendanceRepository.syncMemberProfile(f, l, e, p, a, w, h, photo)
                    }
                }
            )
        }
        composable(
            route = Screen.Members.route,
            arguments = listOf(navArgument("filter") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter")
            MembersScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = when (userRole) {
                    "admin" -> onAdminHome
                    "professor" -> onProfessorHome
                    else -> onMemberHome
                },
                onStoreClick = when (userRole) {
                    "admin" -> onAdminStore
                    else -> onMemberStore
                },
                onSettingsClick = onAdminSettings,
                onScheduleClick = onProfessorSchedule,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onRoutinesClick = onMemberRoutines,
                onChatClick = { recipientId ->
                    navController.navigate(Screen.Chat.createRoute(recipientId))
                },
                isAdmin = userRole == "admin",
                isProfessor = userRole == "professor",
                onMemberClick = { email ->
                    navController.navigate(Screen.MemberDetail.createRoute(email))
                },
                onAddMemberClick = {
                    navController.navigate(Screen.AddMember.route)
                },
                initialFilter = filter ?: "ALL",
                attendanceRepository = attendanceRepository
            )
        }
        composable(
            route = Screen.MemberDetail.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            MemberDetailScreen(
                memberEmail = email,
                onBack = { navController.popBackStack() },
                attendanceRepository = attendanceRepository,
                onHomeClick = when (userRole) {
                    "admin" -> onAdminHome
                    "professor" -> onProfessorHome
                    else -> onMemberHome
                },
                onStoreClick = when (userRole) {
                    "admin" -> onAdminStore
                    else -> onMemberStore
                },
                onSettingsClick = onAdminSettings,
                onScheduleClick = onProfessorSchedule,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onRoutinesClick = onMemberRoutines,
                onChatClick = {
                    navController.navigate(Screen.Chat.createRoute(email))
                },
                isAdmin = userRole == "admin",
                isProfessor = userRole == "professor"
            )
        }
        composable(Screen.AddMember.route) {
            AddMemberScreen(
                onBack = { navController.popBackStack() },
                attendanceRepository = attendanceRepository,
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(
            route = Screen.ManageProfessors.route,
            arguments = listOf(navArgument("isAdmin") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isAdminParam = backStackEntry.arguments?.getBoolean("isAdmin") ?: false
            ManageProfessorsScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = if (isAdminParam) onAdminHome else onProfessorHome,
                onStoreClick = if (isAdminParam) onAdminStore else onProfessorStore,
                onMembersClick = if (isAdminParam) { { onAdminMembers(null) } } else { { onProfessorMembers(null) } },
                onProfessorsClick = if (isAdminParam) onAdminProfessors else { { onProfessorMembers(null) } },
                onSettingsClick = if (isAdminParam) onAdminSettings else onProfessorHome,
                onChatClick = { profId -> navController.navigate(Screen.Chat.createRoute(profId)) },
                onAddProfessor = { navController.navigate(Screen.EditProfessor.createRoute("new")) },
                onEditProfessor = { id -> navController.navigate(Screen.EditProfessor.createRoute(id)) }
            )
        }
        composable(
            route = Screen.EditProfessor.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            AddProfessorScreen(
                professorId = if (id == "new") null else id,
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings,
                attendanceRepository = attendanceRepository
            )
        }
        composable(Screen.ManageSchedule.route) {
            ManageScheduleScreen(
                viewModel = scheduleViewModel,
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.ProfessorSchedule.route) {
            ManageScheduleScreen(
                viewModel = scheduleViewModel,
                onBack = { navController.popBackStack() },
                onHomeClick = onProfessorHome,
                onStoreClick = onProfessorStore,
                onMembersClick = { onProfessorMembers(null) },
                onProfessorsClick = { onProfessorMembers(null) },
                onSettingsClick = onProfessorHome,
                onScheduleClick = onProfessorSchedule,
                onRoutinesClick = onProfessorRoutines,
                onChatClick = onProfessorChat,
                isAdmin = false,
                isProfessor = true
            )
        }
        composable(Screen.Routines.route) {
            RoutineDashboardScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = when (userRole) {
                    "admin" -> onAdminHome
                    "professor" -> onProfessorHome
                    else -> onMemberHome
                },
                onStoreClick = when (userRole) {
                    "admin" -> onAdminStore
                    else -> onMemberStore
                },
                onClassesClick = onMemberClasses,
                onScheduleClick = onProfessorSchedule,
                onRoutinesClick = { /* Ya estamos aquí */ },
                onChatClick = { onMemberChat(null) },
                onMembersClick = { onAdminMembers(null) },
                onNewRoutineClick = onNewRoutine,
                onStartRoutine = { id: String -> navController.navigate(Screen.RoutineDetail.createRoute(id)) },
                onAssignRoutine = { id: String -> navController.navigate(Screen.Members.createRoute()) },
                onEditRoutine = { id: String -> navController.navigate(Screen.RoutineEdit.createRoute(id)) }
            )
        }
        composable(Screen.RoutineCreation.route) {
            RoutineCreationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.RoutineEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            RoutineCreationScreen(
                routineId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            RoutineDetailScreen(
                routineId = id,
                attendanceRepository = attendanceRepository,
                onBack = { navController.popBackStack() },
                onEditRoutine = { routineId ->
                    navController.navigate(Screen.RoutineEdit.createRoute(routineId))
                }
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            ChatScreen(
                recipientName = recipientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SubscriptionPlans.route) {
            SubscriptionPlansScreen(
                onBack = { navController.popBackStack() },
                attendanceRepository = attendanceRepository,
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onAdminHome,
                onStoreClick = onAdminStore,
                onMembersClick = { onAdminMembers(null) },
                onProfessorsClick = onAdminProfessors,
                onSettingsClick = onAdminSettings
            )
        }
        composable(Screen.ProfessorHelp.route) {
            ProfessorHelpScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onProfessorHome,
                onStoreClick = onProfessorStore,
                onMembersClick = { onProfessorMembers(null) },
                onScheduleClick = onProfessorSchedule,
                onRoutinesClick = onProfessorRoutines,
                onChatClick = onProfessorChat
            )
        }
        composable(Screen.MemberHelp.route) {
            MemberHelpScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = onMemberHome,
                onStoreClick = onMemberStore,
                onClassesClick = onMemberClasses,
                onRoutinesClick = onMemberRoutines,
                onChatClick = { onMemberChat(null) }
            )
        }
        composable(Screen.Contact.route) {
            ContactScreen(
                onBack = { navController.popBackStack() },
                onReportProblemClick = { navController.navigate(Screen.ReportProblem.route) }
            )
        }
        composable(Screen.ReportProblem.route) {
            ReportProblemScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Paywall.route) {
            if (LockConfig.IS_ROLE_LOCK_ENABLED == 0) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.MemberDashboard.route) {
                        popUpTo(Screen.Paywall.route) { inclusive = true }
                    }
                }
            } else {
                PaywallScreen(
                    billingManager = billingManager,
                    onBack = { 
                        if (userRole == null) navController.popBackStack()
                        else navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        }
    }
}
