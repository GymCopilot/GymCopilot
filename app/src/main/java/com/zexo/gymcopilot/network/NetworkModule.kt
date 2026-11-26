package com.zexo.gymcopilot.network

import com.zexo.gymcopilot.DataStoreManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val userAgentInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) GymCopilot/1.0")
            .build()
        chain.proceed(requestWithUserAgent)
    }

    /**
     * Interceptor de redirecciones "quirúrgico":
     * - Si la redirección apunta a un servidor de Apps Script (script.google.com,
     *   script.googleusercontent.com), se sigue con GET, tal como exige el flujo
     *   de ejecución de Web Apps de Apps Script.
     * - Para cualquier otro destino (Drive, otras APIs de Google), se conserva
     *   el método y el cuerpo originales de la petición, evitando los 403
     *   ("Servicio Deshabilitado") que aparecían al forzar POST en todas partes.
     */
    private val postRedirectInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        // Seguimos hasta 5 redirecciones
        var redirectCount = 0
        while ((response.code == 301 || response.code == 302 || response.code == 303 || response.code == 307 || response.code == 308)
            && redirectCount < 5
        ) {
            val location = response.header("Location")
            if (location.isNullOrBlank()) break

            response.close()

            val locationHost = location.toHttpUrlOrNull()?.host.orEmpty()
            val isAppsScriptRedirect = locationHost.contains("script.google", ignoreCase = true) ||
                    locationHost.contains("googleusercontent.com", ignoreCase = true)

            val redirectedRequest = if (isAppsScriptRedirect) {
                // Apps Script: la redirección de ejecución debe seguirse con GET.
                originalRequest.newBuilder()
                    .url(location)
                    .get()
                    .build()
            } else {
                // Drive y otras APIs: mantener el método y el cuerpo originales.
                originalRequest.newBuilder()
                    .url(location)
                    .method(originalRequest.method, originalRequest.body)
                    .build()
            }

            response = chain.proceed(redirectedRequest)
            redirectCount++
        }
        response
    }

    private fun buildPostClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    private fun buildGetClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    fun getApiService(baseUrl: String): GymApiService =
        createRetrofit(baseUrl, buildPostClient())

    fun getApiServiceForGet(baseUrl: String): GymApiService =
        createRetrofit(baseUrl, buildGetClient())

    fun getGoogleCloudService(dataStoreManager: DataStoreManager): GoogleCloudService {
        val client = OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(GoogleAuthInterceptor(dataStoreManager))
            .addInterceptor(postRedirectInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://script.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleCloudService::class.java)
    }

    private fun createRetrofit(baseUrl: String, client: OkHttpClient): GymApiService {
        val finalBaseUrl = if (baseUrl.isBlank()) "https://placeholder.com/"
        else if (baseUrl.endsWith("/")) baseUrl
        else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GymApiService::class.java)
    }
}