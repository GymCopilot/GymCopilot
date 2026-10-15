package com.zexo.gymcopilot.shared.network

import com.zexo.gymcopilot.model.SubscriptionPlan
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@Serializable
data class CheckInRequest(
    val action: String = "check_in",
    val gymId: String,
    val memberEmail: String,
    val timestamp: Long,
    val spreadsheetId: String? = null
)

@Serializable
data class ScheduleSyncRequest(
    val action: String = "update_schedule",
    val gymId: String,
    val professorId: String,
    val schedulesJson: String,
    val timestamp: Long = 0L,
    val spreadsheetId: String? = null
)

@Serializable
data class ProfileSyncRequest(
    val action: String = "update_profile",
    val professorId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val specialty: String,
    val profileColor: Int? = null,
    val photoUri: String? = null,
    val timestamp: Long = 0L,
    val spreadsheetId: String? = null
)

@Serializable
data class MemberSyncRequest(
    val action: String = "update_member",
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String,
    val weight: String,
    val height: String,
    val photoUri: String? = null,
    val membershipStatus: String? = null,
    val nextRenewalDate: Long? = null,
    val planId: String? = null,
    val planType: String? = null,
    val assignedTrainer: String? = null,
    val timestamp: Long = 0L,
    val spreadsheetId: String? = null
)

@Serializable
data class PlansSyncRequest(
    val action: String = "update_plans",
    val plans: List<SubscriptionPlan>,
    val timestamp: Long = 0L,
    val spreadsheetId: String? = null
)

@Serializable
data class RoutineSyncRequest(
    val action: String = "update_routine",
    val professorId: String,
    val memberEmail: String,
    val routine: String,
    val routineId: String = "",
    val status: String? = null,
    val timestamp: Long = 0L,
    val spreadsheetId: String? = null
)

@Serializable
data class ChatSyncRequest(
    val action: String = "send_chat_message",
    val gymId: String = "",
    val sender: String,
    val recipient: String,
    val text: String = "",
    val timestamp: Long = 0L,
    val senderRole: String = "",
    val spreadsheetId: String? = null
)

@Serializable
data class ProductSyncRequest(
    val action: String,
    val id: String,
    val name: String? = null,
    val price: Double? = null,
    val categoryId: String? = null,
    val imageUri: String? = null,
    val description: String? = null,
    val contactMethod: String? = null,
    val contactPhone: String? = null,
    val bannerType: String? = null,
    val bannerText: String? = null,
    val timestamp: Long = 0L,
    val spreadsheetId: String? = null
)

@Serializable
data class GymSyncRequest(
    val action: String = "update_gym_info",
    val name: String? = null,
    val logoUri: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val zip: String? = null,
    val country: String? = null,
    val accentColor: Int? = null,
    val backgroundColor: Int? = null,
    val buttonStyle: Int? = null,
    val showLogoBorder: Boolean? = null,
    val broadcastMessage: String? = null,
    val classInSession: Boolean? = null,
    val gymIsOpen: Boolean? = null,
    val wifiSsid: String? = null,
    val spreadsheetId: String? = null,
    val wifiSsid2: String? = null,
    val wifiSsid3: String? = null,
    val timestamp: Long = 0L
)

@Serializable
data class AttendanceRecord(
    val date: String? = null,
    val gymId: String? = null,
    val email: String? = null,
    val timestamp: Long = 0L
)

@Serializable
data class MegaSyncResponse(
    val status: String? = null,
    val data: MegaSyncData? = null
)

@Serializable
data class MegaSyncData(
    val config: JsonArray? = null,
    val plans: JsonArray? = null,
    val members: JsonArray? = null,
    val professors: JsonArray? = null,
    val schedules: JsonArray? = null,
    val routines: JsonArray? = null,
    val store: JsonArray? = null
)
