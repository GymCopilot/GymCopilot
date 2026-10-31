package com.zexo.gymcopilot.utils

actual fun saveLocalMemberProfile(data: LocalMemberData) {}
actual fun readLocalMemberProfile(): LocalMemberData? = null
actual fun saveLocalStorageFlag(key: String, value: String) {}
actual fun readLocalStorageFlag(key: String): String? = null
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
actual fun formatEpochDate(timestamp: Long): String {
    if (timestamp <= 0L) return "N/A"
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
