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

expect suspend fun postAppsScriptNative(url: String, jsonBody: String): String

object GymKtorApiClient {

    const val MASTER_SCRIPT_URL = "https://script.google.com/macros/s/AKfycby0bEX37-HRGn9M8dbO8ZBGhHY2riK_AEVhby8ESmPWwiIgNRzqv5z0I78Lvg7Dwk0p/exec"

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
            val targetUrl = if (url.isBlank() || url.contains("AKfycbx_GymCopilot_Sync")) MASTER_SCRIPT_URL else url
            val paramChar = if (targetUrl.contains("?")) "&" else "?"
            val queryParams = StringBuilder("$targetUrl${paramChar}")
            if (!action.isNullOrBlank()) queryParams.append("action=$action&")
            if (!gymId.isNullOrBlank()) queryParams.append("gymId=${gymId.encodeURLParameter()}&")
            if (!email.isNullOrBlank()) queryParams.append("memberEmail=${email.encodeURLParameter()}&email=${email.encodeURLParameter()}&")

            val finalUrl = queryParams.toString().removeSuffix("&").removeSuffix("?")
            postAppsScriptNative(finalUrl, jsonBody)
        } catch (e: Throwable) {
            println("GymKtorApiClient.postAppsScriptData ERROR: $e")
            ""
        }
    }

    suspend inline fun <reified T : Any> postData(
        url: String,
        body: T,
        token: String? = null
    ): String {
        val targetUrl = if (url.isBlank() || url.contains("AKfycbx_GymCopilot_Sync")) MASTER_SCRIPT_URL else url
        return try {
            val jsonPayload = jsonInstance.encodeToString(serializer<T>(), body)
            if (targetUrl.contains("script.google.com")) {
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
            println("GymKtorApiClient.postData ERROR: $e")
            ""
        }
    }

    suspend fun getData(
        url: String,
        token: String? = null
    ): String {
        val targetUrl = if (url.isBlank() || url.contains("AKfycbx_GymCopilot_Sync")) MASTER_SCRIPT_URL else url
        return try {
            val response = httpClient.get(targetUrl) {
                if (!token.isNullOrBlank()) {
                    header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
                }
            }
            response.bodyAsText()
        } catch (e: Throwable) {
            println("GymKtorApiClient.getData ERROR: $e")
            ""
        }
    }
}