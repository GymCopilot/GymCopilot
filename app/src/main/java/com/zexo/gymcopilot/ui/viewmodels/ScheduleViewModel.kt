package com.zexo.gymcopilot.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.gymcopilot.ScheduleEntry
import com.zexo.gymcopilot.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScheduleSyncState {
    object Idle : ScheduleSyncState()
    object Loading : ScheduleSyncState()
    object Success : ScheduleSyncState()
    data class Error(val message: String) : ScheduleSyncState()
}

class ScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _syncState = MutableStateFlow<ScheduleSyncState>(ScheduleSyncState.Idle)
    val syncState = _syncState.asStateFlow()

    fun syncSchedules() {
        viewModelScope.launch {
            if (_syncState.value is ScheduleSyncState.Loading) return@launch
            _syncState.value = ScheduleSyncState.Loading
            try {
                repository.syncSchedules()
                _syncState.value = ScheduleSyncState.Success
            } catch (e: Exception) {
                _syncState.value = ScheduleSyncState.Error(e.message ?: "Error al sincronizar")
            }
        }
    }

    fun saveAndUploadSchedule(professorId: String, schedules: List<ScheduleEntry>) {
        viewModelScope.launch {
            _syncState.value = ScheduleSyncState.Loading
            val result = repository.uploadSchedule(professorId, schedules)
            result.fold(
                onSuccess = { _syncState.value = ScheduleSyncState.Success },
                onFailure = { _syncState.value = ScheduleSyncState.Error(it.message ?: "Error al subir") }
            )
        }
    }

    fun resetState() { _syncState.value = ScheduleSyncState.Idle }
}
