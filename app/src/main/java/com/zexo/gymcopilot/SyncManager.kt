package com.zexo.gymcopilot

import android.util.Log
import com.zexo.gymcopilot.repository.AttendanceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SyncManager(
    private val attendanceRepository: AttendanceRepository,
    private val dataStoreManager: DataStoreManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    fun startSyncLoop() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            while (isActive) {
                try {
                    val gymUrl = dataStoreManager.getGymApiUrl().first()
                    if (gymUrl.isNotBlank()) {
                        Log.d("SyncManager", "Iniciando sincronización automática...")
                        attendanceRepository.syncEverythingRemote()
                    }
                } catch (e: Exception) {
                    Log.e("SyncManager", "Error en el bucle de sincronización: ${e.message}")
                }
                delay(20000) // 20 segundos
            }
        }
    }

    fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }
}
