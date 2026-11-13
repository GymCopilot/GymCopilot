package com.zexo.gymcopilot.utils

import kotlinx.browser.localStorage
import com.zexo.gymcopilot.LockConfig

actual fun saveLocalMemberProfile(data: LocalMemberData) {
    try {
        localStorage.setItem("gymcopilot_member_firstname", data.firstName)
        localStorage.setItem("gymcopilot_member_lastname", data.lastName)
        localStorage.setItem("gymcopilot_member_email", data.email)
        localStorage.setItem("gymcopilot_member_phone", data.phone)
        localStorage.setItem("gymcopilot_member_address", data.address)
        localStorage.setItem("gymcopilot_member_weight", data.weight)
        localStorage.setItem("gymcopilot_member_height", data.height)
        if (data.photoUri != null) localStorage.setItem("gymcopilot_member_photo", data.photoUri)
        localStorage.setItem("gymcopilot_gym_name", data.gymName)
        localStorage.setItem("gymcopilot_gym_url", data.gymApiUrl)
        localStorage.setItem("gymcopilot_gym_ssid", data.spreadsheetId)
        localStorage.setItem("gymcopilot_member_status", data.membershipStatus)
        localStorage.setItem("gymcopilot_member_renewal", data.nextRenewalDate.toString())
        localStorage.setItem("gymcopilot_member_plan", data.planName)
    } catch (e: Throwable) { }
}

actual fun readLocalMemberProfile(): LocalMemberData? {
    return try {
        val email = localStorage.getItem("gymcopilot_member_email") ?: return null
        val firstName = localStorage.getItem("gymcopilot_member_firstname") ?: ""
        val lastName = localStorage.getItem("gymcopilot_member_lastname") ?: ""
        val phone = localStorage.getItem("gymcopilot_member_phone") ?: ""
        val address = localStorage.getItem("gymcopilot_member_address") ?: ""
        val weight = localStorage.getItem("gymcopilot_member_weight") ?: ""
        val height = localStorage.getItem("gymcopilot_member_height") ?: ""
        val photo = localStorage.getItem("gymcopilot_member_photo")
        val gymName = localStorage.getItem("gymcopilot_gym_name") ?: "Gym 19"
        val gymUrl = localStorage.getItem("gymcopilot_gym_url") ?: LockConfig.activeApiUrl
        val spreadsheetId = localStorage.getItem("gymcopilot_gym_ssid") ?: ""
        val membershipStatus = localStorage.getItem("gymcopilot_member_status") ?: "ACTIVO"
        val nextRenewalDate = localStorage.getItem("gymcopilot_member_renewal")?.toLongOrNull() ?: 0L
        val planName = localStorage.getItem("gymcopilot_member_plan") ?: "Plan Mensual"

        LocalMemberData(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            address = address,
            weight = weight,
            height = height,
            photoUri = photo,
            gymName = gymName,
            gymApiUrl = gymUrl,
            spreadsheetId = spreadsheetId,
            membershipStatus = membershipStatus,
            nextRenewalDate = nextRenewalDate,
            planName = planName
        )
    } catch (e: Throwable) {
        null
    }
}

actual fun saveLocalStorageFlag(key: String, value: String) {
    try {
        localStorage.setItem(key, value)
    } catch (e: Throwable) {}
}

actual fun readLocalStorageFlag(key: String): String? {
    return try {
        localStorage.getItem(key)
    } catch (e: Throwable) {
        null
    }
}

actual fun getCurrentTimeMillis(): Long = kotlin.js.Date.now().toLong()

actual fun formatEpochDate(timestamp: Long): String {
    if (timestamp <= 0L) return "N/A"
    val date = kotlin.js.Date(timestamp)
    val months = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    val day = date.getDate().toString().padStart(2, '0')
    val monthStr = months[date.getMonth()]
    val year = date.getFullYear()
    return "$day $monthStr $year"
}
