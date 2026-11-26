package com.zexo.gymcopilot.utils

actual fun saveLocalMemberProfile(data: LocalMemberData) {}
actual fun readLocalMemberProfile(): LocalMemberData? = null
actual fun saveLocalStorageFlag(key: String, value: String) {}
actual fun readLocalStorageFlag(key: String): String? = null
actual fun getCurrentTimeMillis(): Long = kotlin.system.getTimeMillis()
actual fun formatEpochDate(timestamp: Long): String = "N/A"
