package com.zexo.gymcopilot.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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

    suspend inline fun <reified T : Any> postData(
        url: String,
        body: T,
        token: String? = null
    ): String {
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            header("User-Agent", "Mozilla/5.0 (GymCopilot KMP; iOS/Android)")
            if (!token.isNullOrBlank()) {
                header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
            }
        }
        return response.bodyAsText()
    }

    suspend fun getData(
        url: String,
        token: String? = null
    ): String {
        val response = httpClient.get(url) {
            header("User-Agent", "Mozilla/5.0 (GymCopilot KMP; iOS/Android)")
            if (!token.isNullOrBlank()) {
                header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
            }
        }
        return response.bodyAsText()
    }
}
