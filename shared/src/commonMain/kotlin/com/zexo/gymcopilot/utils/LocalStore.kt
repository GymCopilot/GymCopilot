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
    val gymApiUrl: String = "https://script.google.com/macros/s/AKfycbyrN53_uZ4ECfRyp-Qwb2sGSYjv-g7OeR5ERbAQxLdirOL7qvL6rfaxXsjJShc28P-a/exec"
)

expect fun saveLocalMemberProfile(data: LocalMemberData)

expect fun readLocalMemberProfile(): LocalMemberData?

expect fun saveLocalStorageFlag(key: String, value: String)
expect fun readLocalStorageFlag(key: String): String?
