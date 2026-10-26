package com.zexo.gymcopilot.shared.network

import kotlinx.browser.window
import kotlinx.coroutines.await

actual suspend fun postAppsScriptNative(url: String, jsonBody: String): String {
    return try {
        val init = js("{}")
        init.method = "POST"
        init.headers = js("({'Content-Type': 'text/plain;charset=utf-8'})")
        init.body = jsonBody
        init.redirect = "follow"

        val response = window.fetch(url, init).await()
        response.text().await().toString()
    } catch (e: Throwable) {
        console.error("Error Apps Script:", e)
        ""
    }
}