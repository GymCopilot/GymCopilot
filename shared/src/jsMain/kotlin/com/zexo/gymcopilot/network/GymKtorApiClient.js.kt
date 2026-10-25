package com.zexo.gymcopilot.shared.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun postAppsScriptNative(url: String, jsonBody: String): String = suspendCancellableCoroutine { continuation ->
    try {
        val options = js("{}")
        options.method = "POST"
        options.mode = "no-cors"
        options.body = jsonBody

        val prom = kotlinx.browser.window.fetch(url, options)
        prom.then { _ ->
            continuation.resume("success")
        }.catch { err ->
            continuation.resume("error: $err")
        }
    } catch (e: Throwable) {
        continuation.resume("error: $e")
    }
}
