package com.zexo.gymcopilot.repository

import android.util.Log
import com.zexo.gymcopilot.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GoogleCloudRepository(
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        var MASTER_BACKEND_URL = "https://script.google.com/macros/s/AKfycbwuPNzuzUp-gMs51zv1z0pYD49qzvhk2AXoaYxfU2nOFP4SFeq8Lic1VbX91eOVS3wW/exec"
    }

    suspend fun setupGymBackend(gymName: String, customUrl: String? = null): Result<String> {
        return try {
            val storedUrl = dataStoreManager.getGymApiUrl().first()
            val url = customUrl?.takeIf { it.isNotBlank() }
                ?: storedUrl.takeIf { it.isNotBlank() }
                ?: MASTER_BACKEND_URL

            if (url.isBlank()) {
                return Result.failure(Exception("URL del Backend Maestro no configurada."))
            }

            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val email = dataStoreManager.getUserEmail().first().ifBlank { "admin@gymcopilot.com" }
            val request = mapOf(
                "action" to "create_gym",
                "gymName" to gymName,
                "email" to email,
                "gymId" to gymId
            )

            val encodedGymName = java.net.URLEncoder.encode(gymName, "UTF-8")
            val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
            val encodedGymId = java.net.URLEncoder.encode(gymId, "UTF-8")
            val paramChar = if (url.contains("?")) "&" else "?"
            val requestUrl = "$url${paramChar}action=create_gym&gymName=$encodedGymName&email=$encodedEmail&gymId=$encodedGymId"

            val responseText = withContext(Dispatchers.IO) {
                com.zexo.gymcopilot.shared.network.GymKtorApiClient.postData(requestUrl, request)
            }

            Log.d("GoogleCloudRepo", "Respuesta de create_gym: $responseText")

            val isHtmlError = responseText.trim().startsWith("<!DOCTYPE", ignoreCase = true) ||
                    responseText.trim().startsWith("<html", ignoreCase = true) ||
                    responseText.contains("Authorization required", ignoreCase = true) ||
                    responseText.contains("DriveApp", ignoreCase = true)

            if (isHtmlError) {
                Log.e("GoogleCloudRepo", "Respuesta de error HTML/Autorización de Google: $responseText")
                return Result.failure(
                    Exception("Google requiere autorización de permisos de Drive. Ejecuta la función 'authorizeAndSetup' en Google Apps Script y realiza un 'Nuevo Despliegue'.")
                )
            }

            val isSuccess = responseText.contains("spreadsheetId", ignoreCase = true) ||
                    responseText.contains("success", ignoreCase = true) ||
                    responseText.contains("GymCopilot_DB", ignoreCase = true)

            if (isSuccess || (!responseText.contains("\"status\":\"error\"") && !responseText.contains("\"status\": \"error\""))) {
                Log.i("GoogleCloudRepo", "¡Creación de backend exitosa! Guardando URL: $url")
                dataStoreManager.setGymApiUrl(url)

                val extractedSsId = try {
                    val regex = Regex("\"spreadsheetId\"\\s*:\\s*\"([^\"]+)\"")
                    regex.find(responseText)?.groupValues?.get(1) ?: ""
                } catch (e: Exception) { "" }

                if (extractedSsId.isNotBlank()) {
                    dataStoreManager.setGymSpreadsheetId(extractedSsId)
                }

                return Result.success(url)
            }

            Log.e("GoogleCloudRepo", "Error en create_gym: $responseText")
            val extractedMessage = try {
                val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
                regex.find(responseText)?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            } ?: "Error al crear la planilla en la nube. Verifica el despliegue del script maestro en Google Apps Script."
            return Result.failure(Exception(extractedMessage))
        } catch (e: Exception) {
            Log.e("GoogleCloudRepo", "Error en setupGymBackend: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun resetGymBackend(gymName: String): Result<Unit> {
        return try {
            val storedUrl = dataStoreManager.getGymApiUrl().first()
            val url = storedUrl.takeIf { it.isNotBlank() } ?: MASTER_BACKEND_URL
            if (url.isBlank()) return Result.failure(Exception("URL del Backend Maestro no configurada."))

            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val email = dataStoreManager.getUserEmail().first().ifBlank { "admin@gymcopilot.com" }
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val request = mapOf(
                "action" to "reset_gym",
                "gymId" to gymId,
                "email" to email,
                "spreadsheetId" to ssId
            )

            val encodedGymId = java.net.URLEncoder.encode(gymId, "UTF-8")
            val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
            val encodedSsId = java.net.URLEncoder.encode(ssId, "UTF-8")
            val paramChar = if (url.contains("?")) "&" else "?"
            val requestUrl = "$url${paramChar}action=reset_gym&gymId=$encodedGymId&email=$encodedEmail&spreadsheetId=$encodedSsId"

            val responseText = withContext(Dispatchers.IO) {
                com.zexo.gymcopilot.shared.network.GymKtorApiClient.postData(requestUrl, request)
            }

            Log.d("GoogleCloudRepo", "Respuesta reset_gym: $responseText")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GoogleCloudRepo", "Error en resetGymBackend: ${e.message}")
            Result.failure(e)
        }
    }
}
