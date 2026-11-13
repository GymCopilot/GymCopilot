package com.zexo.gymcopilot.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.gymcopilot.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class CheckInState {
    object Idle : CheckInState()
    object Loading : CheckInState()
    data class Success(val message: String) : CheckInState()
    data class Error(val error: String) : CheckInState()
}

class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    private val _state = MutableStateFlow<CheckInState>(CheckInState.Idle)
    val state = _state.asStateFlow()

    fun onQrScanned(qrContent: String) {
        if (_state.value is CheckInState.Loading || _state.value is CheckInState.Success) return

        viewModelScope.launch {
            _state.value = CheckInState.Loading
            
            try {
                val clean = qrContent.trim()
                Log.d("AttendanceViewModel", "QR Scanned: $clean")

                val isValid = clean.startsWith("PROFLINK|") ||
                        clean.startsWith("MEMBERLINK|") ||
                        clean.startsWith("MEMBER_LINK:") ||
                        clean.startsWith("ATTENDANCE:") ||
                        clean.startsWith("gym_id:") ||
                        try {
                            val json = JSONObject(clean)
                            json.has("gym_id") || json.has("id") || json.has("action") || json.has("gym_api_url")
                        } catch (e: Exception) {
                            false
                        }

                if (!isValid) {
                    _state.value = CheckInState.Error("Debe escanear el código QR oficial del gimnasio")
                    return@launch
                }
                
                // 1. Vinculación de Perfiles (Links)
                if (clean.startsWith("PROFLINK|") || clean.startsWith("MEMBERLINK|") || clean.startsWith("MEMBER_LINK:")) {
                    processLinkQr(clean)
                    return@launch
                }

                // 2. Proceso de Asistencia (Check-in)
                var gymId = ""
                var emailOverride: String? = null

                if (clean.startsWith("ATTENDANCE:")) {
                    val parts = clean.split(":")
                    if (parts.size >= 3) {
                        gymId = parts[1]
                        emailOverride = parts[2]
                    } else if (parts.size >= 2) {
                        emailOverride = parts[1]
                    }
                } else if (clean.startsWith("gym_id:")) {
                    gymId = clean.removePrefix("gym_id:").trim()
                } else {
                    try {
                        val json = JSONObject(clean)
                        gymId = json.optString("gym_id", json.optString("id", ""))
                        emailOverride = if (json.has("email")) json.optString("email") else if (json.has("memberEmail")) json.optString("memberEmail") else null
                    } catch (e: Exception) {
                        gymId = "Gym_Default"
                    }
                }
                
                if (gymId.isBlank()) gymId = "Gym_Default"

                val result = repository.performCheckIn(gymId, emailOverride = emailOverride)

                result.fold(
                    onSuccess = { 
                        _state.value = CheckInState.Success("¡Asistencia registrada!") 
                    },
                    onFailure = { 
                        _state.value = CheckInState.Error(it.message ?: "Error de conexión con la planilla") 
                    }
                )
            } catch (e: Exception) {
                _state.value = CheckInState.Error("Error al procesar el código")
            }
        }
    }

    private suspend fun processLinkQr(qrContent: String) {
        val parts = qrContent.split("|")
        if (qrContent.startsWith("PROFLINK|") && parts.size >= 6) {
            val colorStr = parts.getOrNull(7)
            val colorInt = colorStr?.toIntOrNull()
            repository.linkProfessor(
                id = parts[1],
                firstName = parts[2],
                lastName = parts[3],
                email = parts[4],
                url = parts[5],
                gymName = parts.getOrNull(6) ?: "",
                color = colorInt
            )
            _state.value = CheckInState.Success("¡Perfil de Profesor vinculado!")
        } else if (qrContent.startsWith("MEMBERLINK|") && parts.size >= 5) {
            repository.linkMember(parts[1], parts[2], parts[3], parts[4], parts.getOrNull(5) ?: "")
            _state.value = CheckInState.Success("¡Bienvenido! Cuenta vinculada.")
        } else {
            _state.value = CheckInState.Error("QR de vinculación inválido")
        }
    }
    
    fun resetState() { _state.value = CheckInState.Idle }
}
