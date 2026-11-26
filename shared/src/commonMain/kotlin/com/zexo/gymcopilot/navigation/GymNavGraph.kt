@file:JvmName("SharedGymNavGraphKt")

package com.zexo.gymcopilot.shared.navigation

import kotlin.jvm.JvmName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zexo.gymcopilot.shared.network.GymKtorApiClient

sealed class SharedScreen(val route: String) {
    object Login : SharedScreen("login")
    object AdminDashboard : SharedScreen("admin_dashboard")
    object MemberDashboard : SharedScreen("member_dashboard")
    object ProfessorDashboard : SharedScreen("professor_dashboard")
    object Members : SharedScreen("members")
    object Store : SharedScreen("store")
    object PublicStore : SharedScreen("public_store")
    object Chat : SharedScreen("chat")
    object Settings : SharedScreen("settings")
}

@Composable
fun SharedGymNavGraph(
    initialUserRole: String = "admin",
    gymName: String = "Tu Gimnasio",
    apiClient: GymKtorApiClient = remember { GymKtorApiClient }
) {
    com.zexo.gymcopilot.ui.SharedGymCopilotApp(
        initialRole = initialUserRole,
        gymName = gymName
    )
}
