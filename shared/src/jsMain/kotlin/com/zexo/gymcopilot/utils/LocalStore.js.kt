package com.zexo.gymcopilot.utils

import kotlinx.browser.localStorage

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
        val gymUrl = localStorage.getItem("gymcopilot_gym_url") ?: "https://script.google.com/macros/s/AKfycbyrN53_uZ4ECfRyp-Qwb2sGSYjv-g7OeR5ERbAQxLdirOL7qvL6rfaxXsjJShc28P-a/exec"

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
            gymApiUrl = gymUrl
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
