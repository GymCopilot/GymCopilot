package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.LockConfig

data class LocalMemberData(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val weight: String = "",
    val height: String = "",
    val photoUri: String? = null,
    val gymName: String = "Gym 19",
    val gymApiUrl: String = LockConfig.activeApiUrl,
    val spreadsheetId: String = "",
    val membershipStatus: String = "DEUDOR",
    val nextRenewalDate: Long = 0L,
    val planName: String = "Plan Mensual",
    val assignedTrainer: String = ""
)

expect fun saveLocalMemberProfile(data: LocalMemberData)

expect fun readLocalMemberProfile(): LocalMemberData?

expect fun saveLocalStorageFlag(key: String, value: String)
expect fun readLocalStorageFlag(key: String): String?
expect fun getCurrentTimeMillis(): Long
expect fun formatEpochDate(timestamp: Long): String
