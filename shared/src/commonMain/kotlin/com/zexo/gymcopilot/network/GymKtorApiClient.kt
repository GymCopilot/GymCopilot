package com.zexo.gymcopilot.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object GymKtorApiClient {

    val jsonInstance = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(jsonInstance)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 90_000L
                connectTimeoutMillis = 60_000L
                socketTimeoutMillis = 90_000L
            }
            followRedirects = true
        }
    }

    suspend fun postAppsScriptData(
        url: String,
        jsonBody: String,
        action: String? = null,
        gymId: String? = null,
        email: String? = null
    ): String {
        return try {
            val targetUrl = url.ifBlank { "https://script.google.com/macros/s/AKfycbx_GymCopilot_Sync/exec" }
            val paramChar = if (targetUrl.contains("?")) "&" else "?"
            val queryParams = StringBuilder("$targetUrl${paramChar}")
            if (!action.isNullOrBlank()) queryParams.append("action=$action&")
            if (!gymId.isNullOrBlank()) queryParams.append("gymId=${gymId.encodeURLParameter()}&")
            if (!email.isNullOrBlank()) queryParams.append("memberEmail=${email.encodeURLParameter()}&email=${email.encodeURLParameter()}&")

            val finalUrl = queryParams.toString().removeSuffix("&").removeSuffix("?")

            val response = httpClient.post(finalUrl) {
                contentType(ContentType.Text.Plain)
                setBody(jsonBody)
            }
            response.bodyAsText()
        } catch (e: Throwable) {
            ""
        }
    }

    suspend inline fun <reified T : Any> postData(
        url: String,
        body: T,
        token: String? = null
    ): String {
        val targetUrl = url.ifBlank { "https://script.google.com/macros/s/AKfycbx_GymCopilot_Sync/exec" }
        return try {
            if (targetUrl.contains("script.google.com")) {
                val jsonPayload = jsonInstance.encodeToString(serializer<T>(), body)
                postAppsScriptData(targetUrl, jsonPayload)
            } else {
                val response = httpClient.post(targetUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    if (!token.isNullOrBlank()) {
                        header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
                    }
                }
                response.bodyAsText()
            }
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun getData(
        url: String,
        token: String? = null
    ): String {
        val targetUrl = url.ifBlank { "https://script.google.com/macros/s/AKfycbx_GymCopilot_Sync/exec" }
        return try {
            val response = httpClient.get(targetUrl) {
                if (!token.isNullOrBlank()) {
                    header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
                }
            }
            response.bodyAsText()
        } catch (e: Throwable) {
            ""
        }
    }
}
