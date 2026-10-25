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
        localStorage.setItem("gymcopilot_wifi_ssid", data.wifiSsid)
        localStorage.setItem("gymcopilot_wifi_ssid2", data.wifiSsid2)
        localStorage.setItem("gymcopilot_wifi_ssid3", data.wifiSsid3)
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
        var gymUrl = localStorage.getItem("gymcopilot_gym_url") ?: "https://script.google.com/macros/s/AKfycby0bEX37-HRGn9M8dbO8ZBGhHY2riK_AEVhby8ESmPWwiIgNRzqv5z0I78Lvg7Dwk0p/exec"
        if (gymUrl.contains("AKfycbx_GymCopilot_Sync")) {
            gymUrl = "https://script.google.com/macros/s/AKfycby0bEX37-HRGn9M8dbO8ZBGhHY2riK_AEVhby8ESmPWwiIgNRzqv5z0I78Lvg7Dwk0p/exec"
        }
        val ssid = localStorage.getItem("gymcopilot_wifi_ssid") ?: ""
        val ssid2 = localStorage.getItem("gymcopilot_wifi_ssid2") ?: ""
        val ssid3 = localStorage.getItem("gymcopilot_wifi_ssid3") ?: ""

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
            wifiSsid = ssid,
            wifiSsid2 = ssid2,
            wifiSsid3 = ssid3
        )
    } catch (e: Throwable) {
        null
    }
}
