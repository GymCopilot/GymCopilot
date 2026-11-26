package com.zexo.gymcopilot.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
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
        return try {
            val jsonBody = jsonInstance.encodeToString(body)
            val response = httpClient.post(url) {
                contentType(ContentType.Text.Plain)
                setBody(jsonBody)
                if (!token.isNullOrBlank()) {
                    header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
                }
            }
            response.bodyAsText()
        } catch (e: Throwable) {
            println("Error GymKtorApiClient.postData: ${e.message}")
            ""
        }
    }

    suspend fun getData(
        url: String,
        token: String? = null
    ): String {
        return try {
            val response = httpClient.get(url) {
                if (!token.isNullOrBlank()) {
                    header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
                }
            }
            response.bodyAsText()
        } catch (e: Throwable) {
            println("Error GymKtorApiClient.getData: ${e.message}")
            ""
        }
    }
}
