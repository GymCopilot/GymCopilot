package com.zexo.gymcopilot.utils

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
    val gymApiUrl: String = "https://script.google.com/macros/s/AKfycbzXYYMu43-ATeyxQmwdF3CuRYXi7KbpChyTQEBFIsWh7SCHv2XugxsWen61oBeDdzw/exec",
    val spreadsheetId: String = "",
    val membershipStatus: String = "ACTIVO",
    val nextRenewalDate: Long = 0L,
    val planName: String = "Plan Mensual"
)

expect fun saveLocalMemberProfile(data: LocalMemberData)

expect fun readLocalMemberProfile(): LocalMemberData?

expect fun saveLocalStorageFlag(key: String, value: String)
expect fun readLocalStorageFlag(key: String): String?
