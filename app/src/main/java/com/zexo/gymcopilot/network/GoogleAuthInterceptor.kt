package com.zexo.gymcopilot.network

import android.util.Log
import com.zexo.gymcopilot.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class GoogleAuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { dataStoreManager.getGoogleAccessToken().first() }
        val originalRequest = chain.request()
        
        // Solo inyectamos en dominios de Google
        if (originalRequest.url.host.contains("googleapis.com")) {
            if (token.isBlank()) {
                Log.e("GoogleAuthInterceptor", "Petición a ${originalRequest.url.encodedPath} ABORTADA: Token vacío")
                return chain.proceed(originalRequest)
            }
            
            Log.d("GoogleAuthInterceptor", "Enviando petición a: ${originalRequest.url}")

            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
            
            return chain.proceed(authenticatedRequest)
        }
        
        return chain.proceed(originalRequest)
    }
}
