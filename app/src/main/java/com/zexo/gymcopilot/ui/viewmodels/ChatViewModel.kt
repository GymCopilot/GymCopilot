package com.zexo.gymcopilot.ui.viewmodels

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.zexo.gymcopilot.ChatMessage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class ChatViewModel(
    private val dataStoreManager: DataStoreManager,
    private val context: Context
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private var lastProcessedTimestamp = 0L
    private var isPolling = false

    init {
        startPolling()
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        viewModelScope.launch {
            while (isActive) {
                try {
                    val gymApiUrl = dataStoreManager.getGymApiUrl().first()
                    val userEmail = dataStoreManager.getUserEmail().first()
                    val userName = dataStoreManager.getUserName().first()
                    val userRole = dataStoreManager.getUserRole().first()

                    if (gymApiUrl.isNotBlank()) {
                        val myID = if (userRole?.lowercase() == "admin") "Admin" else {
                            val id = userEmail.ifBlank { userName }.trim()
                            if (id.isEmpty()) "Usuario" else id
                        }
                        
                        pollMessages(gymApiUrl, myID)
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error in polling: ${e.message}")
                }
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    private suspend fun pollMessages(gymApiUrl: String, myID: String) {
        val gymNameVal = dataStoreManager.getGymName().first()
        val gymIdVal = gymNameVal.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val emailVal = dataStoreManager.getUserEmail().first()
        val ssIdVal = dataStoreManager.getGymSpreadsheetId().first()
        val paramChar = if (gymApiUrl.contains("?")) "&" else "?"
        val ssParam = if (ssIdVal.isNotBlank()) "&spreadsheetId=$ssIdVal" else ""
        val syncUrl = "$gymApiUrl${paramChar}type=chats&gymId=$gymIdVal&email=${java.net.URLEncoder.encode(emailVal, "UTF-8")}$ssParam"
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)
        
        try {
            val response = apiServiceGet.getChatsRaw(syncUrl, "Bearer session_active")
            if (response.isSuccessful) {
                val jsonElement = response.body()
                val rows = when {
                    jsonElement?.isJsonArray == true -> jsonElement.asJsonArray
                    jsonElement?.isJsonObject == true -> jsonElement.asJsonObject.getAsJsonArray("data") ?: JsonArray()
                    else -> JsonArray()
                }

                val myL = myID.lowercase().trim()
                var hasNewMessage = false
                var currentMaxTs = lastProcessedTimestamp

                // Skip header row
                for (i in 1 until rows.size()) {
                    val arr = rows.get(i).asJsonArray
                    if (arr.size() >= 4) {
                        val sender = arr.get(1).asString.lowercase().trim()
                        val recipient = arr.get(2).asString.lowercase().trim()
                        val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                        val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L

                        // Only care about messages NOT from me
                        if (sender != myL) {
                            val isForMe = recipient == myL || recipient.contains("chat general")
                            if (isForMe) {
                                if (lastProcessedTimestamp > 0 && ts > lastProcessedTimestamp) {
                                    hasNewMessage = true
                                }
                                if (ts > currentMaxTs) currentMaxTs = ts
                            }
                        }
                    }
                }

                if (hasNewMessage) {
                    playSound()
                }
                lastProcessedTimestamp = currentMaxTs
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error polling messages: ${e.message}")
        }
    }

    private fun playSound() {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.mensaje)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error playing sound: ${e.message}")
        }
    }
}
