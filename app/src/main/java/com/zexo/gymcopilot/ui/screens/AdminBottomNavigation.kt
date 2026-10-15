package com.zexo.gymcopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.zexo.gymcopilot.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonArray
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

@Composable
fun AdminBottomNavigation(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onMembersClick: () -> Unit,
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    accentColor: Color,
    unreadCount: Int = 0, // Parámetro para Clientes
    unreadCountProfessors: Int = 0 // Parámetro para Profesores
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    
    // Marcadores de lectura del administrador
    val lastReadMembers by dataStoreManager.getLastReadTimestamp("admin_chats_global").collectAsState(initial = 0L)
    val lastReadProfs by dataStoreManager.getLastReadTimestamp("admin_professors_chats_global").collectAsState(initial = 0L)
    
    // Marcadores del servidor persistentes (sobreviven a la navegación entre pantallas)
    val serverMaxMembers by dataStoreManager.getLastReadTimestamp("admin_members_server_max").collectAsState(initial = 0L)
    val serverMaxProfs by dataStoreManager.getLastReadTimestamp("admin_professors_server_max").collectAsState(initial = 0L)
    
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())

    // Sincronización autónoma de mensajes en segundo plano
    LaunchedEffect(gymApiUrl, professors) {
        if (gymApiUrl.isBlank()) return@LaunchedEffect
        val gymNameVal = dataStoreManager.getGymName().first()
        val gymIdVal = gymNameVal.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val emailVal = dataStoreManager.getUserEmail().first()
        val ssIdVal = dataStoreManager.getGymSpreadsheetId().first()
        val paramChar = if (gymApiUrl.contains("?")) "&" else "?"
        val ssParam = if (ssIdVal.isNotBlank()) "&spreadsheetId=$ssIdVal" else ""
        val syncUrl = "$gymApiUrl${paramChar}type=chats&gymId=$gymIdVal&email=${java.net.URLEncoder.encode(emailVal, "UTF-8")}$ssParam"
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)

        // Inicialización para evitar notificar mensajes históricos si es la primera vez
        if (dataStoreManager.getLastReadTimestamp("admin_chats_global").first() == 0L) {
            dataStoreManager.setLastReadTimestamp("admin_chats_global", System.currentTimeMillis())
        }
        if (dataStoreManager.getLastReadTimestamp("admin_professors_chats_global").first() == 0L) {
            dataStoreManager.setLastReadTimestamp("admin_professors_chats_global", System.currentTimeMillis())
        }

        while (isActive) {
            try {
                val response = apiServiceGet.getChatsRaw(syncUrl, "Bearer session_active")
                if (response.isSuccessful) {
                    val jsonElement = response.body()
                    val rows = when {
                        jsonElement?.isJsonArray == true -> jsonElement.asJsonArray
                        jsonElement?.isJsonObject == true -> jsonElement.asJsonObject.getAsJsonArray("data") ?: JsonArray()
                        else -> JsonArray()
                    }

                    var currentMaxMember = 0L
                    var currentMaxProf = 0L
                    val profIds = professors.flatMap { 
                        listOf(it.email.lowercase().trim(), it.fullName.lowercase().trim()) 
                    }.filter { it.isNotBlank() }.toSet()

                    for (i in 1 until rows.size()) {
                        val arr = rows.get(i).asJsonArray
                        if (arr.size() >= 4) {
                            val sender = arr.get(1).asString.lowercase().trim()
                            if (sender != "admin") {
                                val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                                val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L
                                if (profIds.contains(sender)) {
                                    if (ts > currentMaxProf) currentMaxProf = ts
                                } else {
                                    if (ts > currentMaxMember) currentMaxMember = ts
                                }
                            }
                        }
                    }
                    // Actualizamos los estados en el DataStore para persistencia global
                    dataStoreManager.setLastReadTimestamp("admin_members_server_max", currentMaxMember)
                    dataStoreManager.setLastReadTimestamp("admin_professors_server_max", currentMaxProf)
                }
            } catch (e: Exception) { }
            delay(10000)
        }
    }

    NavigationBar(
        modifier = Modifier.height(56.dp),
        containerColor = Color(0xFF00151C),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val items = listOf(
            NavigationItemData("admin_dashboard", rememberVectorPainter(Icons.Default.Dashboard), "Home", onHomeClick),
            NavigationItemData("store", rememberVectorPainter(Icons.Default.Storefront), "Store", onStoreClick),
            NavigationItemData("members", rememberVectorPainter(Icons.Default.Group), "Clientes", onMembersClick),
            NavigationItemData("manage_professors", painterResource(R.drawable.assistant), "Profesores", onProfessorsClick),
            NavigationItemData("app_settings", rememberVectorPainter(Icons.Default.Settings), "Config", onSettingsClick)
        )

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { item.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) accentColor else TextGray
                        )
                        
                        // El punto persiste si el mensaje más reciente del servidor supera la última lectura del usuario
                        // Y solo se oculta si estamos exactamente en la pantalla correspondiente.
                        val showRedDot = when (item.route) {
                            "members" -> (serverMaxMembers > lastReadMembers || unreadCount > 0) && currentRoute != "members"
                            "manage_professors" -> (serverMaxProfs > lastReadProfs || unreadCountProfessors > 0) && currentRoute != "manage_professors"
                            else -> false
                        }

                        if (showRedDot) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .background(Color.Red, CircleShape)
                                    .border(1.5.dp, Color(0xFF00151C), CircleShape)
                            )
                        }
                    }
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        color = if (isSelected) accentColor else TextGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class NavigationItemData(
    val route: String,
    val icon: Painter,
    val label: String,
    val onClick: () -> Unit
)
