package com.zexo.gymcopilot.model

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val weight: String = "",
    val height: String = "",
    val registrationDate: Long = 0L,
    val membershipStatus: String = "ACTIVO",
    val planId: String? = null,
    val planType: String = "Mensual",
    val nextRenewalDate: Long = 0L,
    val notes: String = "",
    val routine: String = "",
    val progress: String = "",
    val attendanceCount: Int = 0,
    val currentAttendanceCount: Int = 0,
    val monthAttendanceCount: Int = 0,
    val lastVisit: Long? = null,
    val category: String = "General",
    val photoUri: String? = null,
    val assignedTrainer: String = ""
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Serializable
data class Professor(
    val id: String = "",
    val firstName: String,
    val lastName: String,
    val email: String = "",
    val address: String = "",
    val phone: String = "",
    val idNumber: String = "",
    val taxId: String = "",
    val photoUri: String? = null,
    val profileColor: Int? = null,
    val specialty: String = "",
    val emergencyContact: String = "",
    val bankAccount: String = ""
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val series: String,
    val reps: String,
    val weight: String,
    val rest: String,
    val time: String = "0",
    val notes: String = ""
)

@Serializable
data class Routine(
    val id: String = "",
    val name: String,
    val description: String = "",
    val objective: String = "",
    val level: String = "",
    val duration: String = "",
    val days: List<String> = emptyList(),
    val type: String = "Personal",
    val status: String = "Activa",
    val category: String = "FullBody",
    val professorIds: List<String> = emptyList(),
    val assignedMemberEmails: List<String> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val creatorName: String = "Tú",
    val creatorEmail: String = "",
    val isFavorite: Boolean = false,
    val notes: String = ""
)

@Serializable
data class ScheduleEntry(
    val dayOfWeek: Int? = null,
    val date: String? = null,
    val startTime: String,
    val endTime: String,
    val eventName: String = "",
    val isEnabled: Boolean = true,
    val assignedMemberEmails: List<String> = emptyList()
)

@Serializable
data class ChatMessage(
    val sender: String,
    val recipient: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val isPending: Boolean = false
)

@Serializable
data class PaymentRecord(
    val id: String = "",
    val memberEmail: String,
    val planId: String,
    val planName: String,
    val amount: Double,
    val timestamp: Long = 0L,
    val method: String = "Efectivo"
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val categoryId: String,
    val imageUri: String? = null,
    val description: String = "",
    val contactMethod: String? = null,
    val contactPhone: String? = null,
    val bannerType: String? = null,
    val bannerText: String? = null
)

@Serializable
data class SubscriptionPlan(
    val id: String = "",
    val name: String,
    val price: Double,
    val durationDays: Int = 30,
    val description: String = "",
    val color: Int? = null,
    val totalClasses: Int? = null
)
