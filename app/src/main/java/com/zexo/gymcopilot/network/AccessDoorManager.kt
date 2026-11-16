package com.zexo.gymcopilot.network

import android.util.Log
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AccessDoorManager(
    private val dataStoreManager: DataStoreManager
) {
    suspend fun triggerDoorRelease(isManualTest: Boolean = false): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val enabled = dataStoreManager.getTuyaEnabled().first()
                if (!enabled && !isManualTest) {
                    return@withContext Result.failure(Exception("Módulo de apertura desactivado"))
                }

                val webhookUrl = dataStoreManager.getTuyaWebhookUrl().first().trim()
                val clientId = dataStoreManager.getTuyaClientId().first().trim()
                val clientSecret = dataStoreManager.getTuyaClientSecret().first().trim()
                val deviceId = dataStoreManager.getTuyaDeviceId().first().trim()
                val region = dataStoreManager.getTuyaRegion().first().trim().lowercase().ifBlank { "us" }
                val pulseSeconds = dataStoreManager.getTuyaPulseSeconds().first().coerceAtLeast(1)

                // Opción 1: Disparo directo por URL / Webhook
                if (webhookUrl.isNotBlank()) {
                    Log.d("AccessDoorManager", "Ejecutando apertura por Webhook: $webhookUrl")
                    return@withContext executeWebhookRelease(webhookUrl, pulseSeconds)
                }

                // Opción 2: Tuya / Smart Switch Cloud OpenAPI v1.0
                if (clientId.isNotBlank() && clientSecret.isNotBlank() && deviceId.isNotBlank()) {
                    Log.d("AccessDoorManager", "Ejecutando apertura por Smart Switch Cloud API. DeviceId: $deviceId")
                    return@withContext executeTuyaOpenApiRelease(clientId, clientSecret, deviceId, region, pulseSeconds)
                }

                Result.failure(Exception("Sin credenciales de Switch o Webhook configurados."))
            } catch (e: Exception) {
                Log.e("AccessDoorManager", "Error en apertura de puerta: ${e.message}")
                Result.failure(e)
            }
        }
    }

    private suspend fun executeWebhookRelease(webhookUrl: String, pulseSeconds: Int): Result<String> {
        return try {
            val res1 = GymKtorApiClient.getData(webhookUrl)
            Log.d("AccessDoorManager", "Respuesta Webhook (ON): $res1")

            // Esperar el tiempo de pulso para la cerradura magnética/eléctrica
            delay(pulseSeconds * 1000L)

            // Intentar des-activar el pulso si la URL soporta el cambio de estado
            val offUrl = when {
                webhookUrl.contains("turn=on", ignoreCase = true) -> webhookUrl.replace("turn=on", "turn=off", ignoreCase = true)
                webhookUrl.contains("state=on", ignoreCase = true) -> webhookUrl.replace("state=on", "state=off", ignoreCase = true)
                webhookUrl.contains("value=true", ignoreCase = true) -> webhookUrl.replace("value=true", "value=false", ignoreCase = true)
                else -> null
            }
            if (!offUrl.isNullOrBlank()) {
                GymKtorApiClient.getData(offUrl)
            }

            Result.success("¡Puerta abierta por Webhook!")
        } catch (e: Exception) {
            Result.failure(Exception("Error al conectar con el Webhook: ${e.message}"))
        }
    }

    private fun executeTuyaOpenApiRelease(
        clientId: String,
        clientSecret: String,
        deviceId: String,
        region: String,
        pulseSeconds: Int
    ): Result<String> {
        return try {
            val baseUrl = getTuyaBaseUrl(region)

            // 1. Obtener Token de Acceso Cloud API
            val tToken = System.currentTimeMillis().toString()
            val tokenPath = "/v1.0/token?grant_type=1"
            val signToken = calculateTuyaSign(clientId, clientSecret, "GET", tokenPath, tToken, "", "")
            val tokenUrl = "$baseUrl$tokenPath"

            val tokenResponseText = tuyaGet(tokenUrl, clientId, signToken, tToken, "")
            Log.d("AccessDoorManager", "Respuesta Token Tuya: $tokenResponseText")

            val tokenJson = JSONObject(tokenResponseText)
            val success = tokenJson.optBoolean("success", false)
            if (!success) {
                val msg = tokenJson.optString("msg", tokenJson.optString("message", "Error al obtener token de Tuya"))
                return Result.failure(Exception("Auth Cloud ($region): $msg"))
            }

            val resultObj = tokenJson.optJSONObject("result")
            val accessToken = resultObj?.optString("access_token", "") ?: ""
            if (accessToken.isBlank()) {
                return Result.failure(Exception("Token de acceso de Tuya vacío."))
            }

            // 2. Enviar comando ON al switch de la cerradura
            val commandPath = "/v1.0/devices/$deviceId/commands"
            val commandBodyOn = JSONObject().apply {
                put("commands", JSONArray().apply {
                    put(JSONObject().apply {
                        put("code", "switch_1")
                        put("value", true)
                    })
                    put(JSONObject().apply {
                        put("code", "switch")
                        put("value", true)
                    })
                })
            }.toString()

            val tCmd = System.currentTimeMillis().toString()
            val signCmdOn = calculateTuyaSign(clientId, clientSecret, "POST", commandPath, tCmd, accessToken, commandBodyOn)
            val cmdUrl = "$baseUrl$commandPath"

            val cmdResponseText = tuyaPost(cmdUrl, clientId, signCmdOn, tCmd, accessToken, commandBodyOn)
            Log.d("AccessDoorManager", "Respuesta comando ON: $cmdResponseText")

            val cmdJson = JSONObject(cmdResponseText)
            val cmdSuccess = cmdJson.optBoolean("success", false)
            if (!cmdSuccess) {
                val cmdMsg = cmdJson.optString("msg", cmdJson.optString("message", "Error al ejecutar comando de puerta"))
                return Result.failure(Exception("Comando Tuya: $cmdMsg"))
            }

            // 3. Esperar pulso de apertura (ej. 3 segundos) y apagar el interruptor
            Thread.sleep(pulseSeconds * 1000L)

            val commandBodyOff = JSONObject().apply {
                put("commands", JSONArray().apply {
                    put(JSONObject().apply {
                        put("code", "switch_1")
                        put("value", false)
                    })
                    put(JSONObject().apply {
                        put("code", "switch")
                        put("value", false)
                    })
                })
            }.toString()

            val tOff = System.currentTimeMillis().toString()
            val signCmdOff = calculateTuyaSign(clientId, clientSecret, "POST", commandPath, tOff, accessToken, commandBodyOff)
            tuyaPost(cmdUrl, clientId, signCmdOff, tOff, accessToken, commandBodyOff)

            Result.success("¡Puerta liberada exitosamente!")
        } catch (e: Exception) {
            Result.failure(Exception("Error Cloud API: ${e.message}"))
        }
    }

    private fun tuyaGet(
        urlStr: String,
        clientId: String,
        sign: String,
        t: String,
        accessToken: String = ""
    ): String {
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("client_id", clientId)
        conn.setRequestProperty("sign", sign)
        conn.setRequestProperty("t", t)
        conn.setRequestProperty("sign_method", "HMAC-SHA256")
        if (accessToken.isNotBlank()) {
            conn.setRequestProperty("access_token", accessToken)
        }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        return stream.bufferedReader().use { it.readText() }
    }

    private fun tuyaPost(
        urlStr: String,
        clientId: String,
        sign: String,
        t: String,
        accessToken: String,
        jsonBody: String
    ): String {
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("client_id", clientId)
        conn.setRequestProperty("sign", sign)
        conn.setRequestProperty("t", t)
        conn.setRequestProperty("sign_method", "HMAC-SHA256")
        if (accessToken.isNotBlank()) {
            conn.setRequestProperty("access_token", accessToken)
        }
        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
        }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        return stream.bufferedReader().use { it.readText() }
    }

    private fun getTuyaBaseUrl(region: String): String {
        return when (region.lowercase()) {
            "eu" -> "https://openapi.tuyaeu.com"
            "cn" -> "https://openapi.tuyacn.com"
            "in" -> "https://openapi.tuyain.com"
            else -> "https://openapi.tuyaus.com"
        }
    }

    private fun calculateTuyaSign(
        clientId: String,
        secret: String,
        method: String,
        path: String,
        t: String,
        accessToken: String,
        body: String
    ): String {
        val contentSha256 = if (body.isNotBlank()) sha256(body) else "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val stringToSign = "$method\n$contentSha256\n\n$path"
        val strToHash = clientId + accessToken + t + stringToSign
        return hmacSha256(secret, strToHash).uppercase()
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(secret: String, data: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256HMAC.init(secretKey)
        val bytes = sha256HMAC.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
