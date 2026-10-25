package com.zexo.gymcopilot.shared.network

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

actual suspend fun postAppsScriptNative(url: String, jsonBody: String): String {
    return try {
        val response = GymKtorApiClient.httpClient.post(url) {
            contentType(ContentType.Text.Plain)
            setBody(jsonBody)
        }
        response.bodyAsText()
    } catch (e: Throwable) {
        ""
    }
}
