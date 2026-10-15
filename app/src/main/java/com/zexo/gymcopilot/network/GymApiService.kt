package com.zexo.gymcopilot.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class CheckInRequest(
    val action: String = "check_in",
    val gymId: String,
    val memberEmail: String,
    val timestamp: Long,
    val spreadsheetId: String? = null
)

data class ScheduleSyncRequest(
    val action: String = "update_schedule",
    val gymId: String,
    val professorId: String,
    val schedulesJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

data class ProfileSyncRequest(
    val action: String = "update_profile",
    val professorId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val specialty: String,
    val profileColor: Int? = null,
    val photoUri: String? = null,
    val gymId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

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
    val gymId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

data class PlansSyncRequest(
    val action: String = "update_plans",
    val plans: List<com.zexo.gymcopilot.ui.screens.SubscriptionPlan>,
    val gymId: String? = null,
    val email: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

data class RoutineSyncRequest(
    val action: String = "update_routine",
    val professorId: String,
    val memberEmail: String,
    val routine: String,
    val routineId: String = "",
    val status: String? = null,
    val gymId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

data class ChatSyncRequest(
    val action: String = "send_chat_message",
    val gymId: String = "",
    val sender: String,
    val recipient: String,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val senderRole: String = "",
    val spreadsheetId: String? = null
)

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
    val gymId: String? = null,
    val email: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

data class ChatMessage(
    val gymId: String,
    val sender: String,
    val recipient: String,
    val text: String,
    val timestamp: Long
)

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
    val wifiSsid2: String? = null,
    val wifiSsid3: String? = null,
    val gymId: String? = null,
    val email: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val spreadsheetId: String? = null
)

data class AttendanceRecord(
    @SerializedName("date", alternate = ["Fecha", "A"]) val date: String?,
    @SerializedName("description", alternate = ["B"]) val gymId: String?,
    @SerializedName("email", alternate = ["memberEmail", "Email", "usuario", "user", "D", "email_address"]) val email: String?,
    @SerializedName("timestamp", alternate = ["Timestamp", "C"]) val timestamp: Long
)

interface GymApiService {
    @POST
    suspend fun postCheckIn(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: CheckInRequest
    ): Response<Unit>

    @POST
    suspend fun postScheduleSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: ScheduleSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postProfileSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: ProfileSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postMemberSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: MemberSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postPlansSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: PlansSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postRoutineSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: RoutineSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postChatMessage(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: ChatSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postProductSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: ProductSyncRequest
    ): Response<Unit>

    @POST
    suspend fun postGymSync(
        @Url url: String,
        @Header("Authorization") token: String?,
        @Body request: GymSyncRequest
    ): Response<Unit>

    @GET
    suspend fun getMegaSyncRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<MegaSyncResponse>

    @GET
    suspend fun getAttendanceRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<JsonElement>

    @GET
    suspend fun getSchedulesRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<JsonElement>

    @GET
    suspend fun getRoutinesRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<JsonElement>

    @GET
    suspend fun getChatsRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<JsonElement>

    @GET
    suspend fun getStoreRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<JsonElement>

    @GET
    suspend fun getConfigRaw(
        @Url url: String,
        @Header("Authorization") token: String?
    ): Response<JsonElement>
}
