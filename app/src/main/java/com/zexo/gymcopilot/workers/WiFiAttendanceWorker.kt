package com.zexo.gymcopilot.workers

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Worker que realiza el check-in automático cuando el usuario se conecta al Wi-Fi del gimnasio.
 */
class WiFiAttendanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dataStoreManager = DataStoreManager(applicationContext)
        val ssid1 = dataStoreManager.getGymWifiSsid().first()
        val ssid2 = dataStoreManager.getGymWifiSsid2().first()
        val ssid3 = dataStoreManager.getGymWifiSsid3().first()
        
        val authorizedSsids = listOf(ssid1, ssid2, ssid3).filter { it.isNotBlank() }
        
        if (authorizedSsids.isEmpty()) {
            Log.d("WiFiWorker", "SSIDs del Gimnasio no configurados. Omitiendo.")
            return Result.success()
        }

        val currentSsid = getCurrentSsid(applicationContext)
        Log.d("WiFiWorker", "Verificando red Wi-Fi. Actual: $currentSsid, Autorizadas: $authorizedSsids")

        // Normalizar SSID actual
        val normalizedCurrent = currentSsid?.removeSurrounding("\"")
        
        // Verificar si coincide con alguna autorizada (también normalizada)
        val isAuthorized = authorizedSsids.any { 
            it.removeSurrounding("\"") == normalizedCurrent 
        }

        if (normalizedCurrent != null && isAuthorized) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastCheckinDate = dataStoreManager.getLastWifiCheckinDate().first()

            if (lastCheckinDate == today) {
                Log.d("WiFiWorker", "Asistencia ya registrada hoy según DataStore ($today). Omitiendo.")
                return Result.success()
            }
            
            // Segunda capa de seguridad: verificar el perfil del alumno
            val userEmail = dataStoreManager.getUserEmail().first().lowercase().trim()
            val members = dataStoreManager.getMembers().first()
            val me = members.find { it.email.lowercase().trim() == userEmail }
            
            val lastVisit = me?.lastVisit
            if (lastVisit != null) {
                val cal1 = Calendar.getInstance()
                val cal2 = Calendar.getInstance().apply { timeInMillis = lastVisit as Long }
                val isAlreadyPresent = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                     cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                
                if (isAlreadyPresent) {
                    Log.d("WiFiWorker", "Asistencia ya detectada en el perfil del alumno hoy. Sincronizando flag y omitiendo.")
                    dataStoreManager.setLastWifiCheckinDate(today)
                    return Result.success()
                }
            }

            val gymName = dataStoreManager.getGymName().first().ifBlank { "WiFi-Auto" }
            Log.d("WiFiWorker", "SSID coincidente. Intentando check-in automático para el gimnasio: $gymName")
            
            // Usamos una URL vacía inicialmente, el repositorio la obtendrá del DataStore
            val apiService = NetworkModule.getApiService("")
            val repository = AttendanceRepository(apiService, dataStoreManager)
            
            val checkinResult = repository.performCheckIn(gymName)
            
            return if (checkinResult.isSuccess) {
                Log.d("WiFiWorker", "¡Asistencia automática registrada con éxito!")
                dataStoreManager.setLastWifiCheckinDate(today)
                Result.success()
            } else {
                val errorMsg = checkinResult.exceptionOrNull()?.message
                Log.e("WiFiWorker", "Error al registrar asistencia: $errorMsg")
                
                // Si es un error de red (DNS/Host), reintentamos más tarde
                if (errorMsg?.contains("resolve host", ignoreCase = true) == true) {
                    Result.retry()
                } else {
                    Result.success() // Otros errores no reintentar para evitar bucles
                }
            }
        }
else {
            Log.d("WiFiWorker", "El SSID actual no coincide con el del gimnasio.")
            return Result.success()
        }
    }

    private fun getCurrentSsid(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info: WifiInfo? = wifiManager.connectionInfo
        
        if (info != null && info.networkId != -1) {
            val ssid = info.ssid
            if (ssid != "<unknown ssid>") return ssid
        }
        return null
    }
}
