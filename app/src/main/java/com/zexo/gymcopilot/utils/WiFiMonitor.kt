package com.zexo.gymcopilot.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.work.*
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.workers.WiFiAttendanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Monitor de red que detecta cuando el dispositivo se conecta a una red Wi-Fi
 * y programa el Worker de asistencia automática.
 */
class WiFiMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val dataStoreManager = DataStoreManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("WiFiMonitor", "Red Wi-Fi disponible.")
            scheduleAttendanceWorker()
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            Log.d("WiFiMonitor", "Sistema de monitoreo Wi-Fi activado.")
            
            // Verificación inicial por si ya estamos conectados
            checkCurrentNetwork()
        } catch (e: Exception) {
            Log.e("WiFiMonitor", "Error al iniciar monitoreo: ${e.message}")
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d("WiFiMonitor", "Sistema de monitoreo Wi-Fi desactivado.")
        } catch (e: Exception) {
            // Ignorar si no estaba registrado
        }
    }

    private fun checkCurrentNetwork() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            Log.d("WiFiMonitor", "Ya conectado a Wi-Fi al iniciar.")
            scheduleAttendanceWorker()
        }
    }

    private fun scheduleAttendanceWorker() {
        scope.launch {
            val ssid1 = dataStoreManager.getGymWifiSsid().first()
            val ssid2 = dataStoreManager.getGymWifiSsid2().first()
            val ssid3 = dataStoreManager.getGymWifiSsid3().first()
            
            if (ssid1.isBlank() && ssid2.isBlank() && ssid3.isBlank()) {
                Log.d("WiFiMonitor", "No hay SSIDs configurados, se omite la programación.")
                return@launch
            }

            Log.d("WiFiMonitor", "Programando verificación de asistencia instantánea.")

            // Restricciones para el Worker
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

            // Creamos la petición de trabajo con 10 segundos de retraso inicial para pruebas
            val workRequest = OneTimeWorkRequestBuilder<WiFiAttendanceWorker>()
                .setInitialDelay(901, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag("WiFiAttendanceWork")
                .build()

            // Encolamos el trabajo de forma única para no duplicar si hay micro-cortes
            WorkManager.getInstance(context).enqueueUniqueWork(
                "WiFiAttendanceCheck",
                ExistingWorkPolicy.KEEP, // Mantenemos el que ya está corriendo si lo hay
                workRequest
            )
        }
    }
}
