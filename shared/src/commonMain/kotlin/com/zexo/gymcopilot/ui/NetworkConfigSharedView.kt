package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import com.zexo.gymcopilot.utils.saveLocalMemberProfile

@Composable
fun NetworkConfigSharedScreen(
    onBack: () -> Unit
) {
    var localData = remember { readLocalMemberProfile() }
    var apiUrlText by remember { mutableStateOf(localData?.gymApiUrl ?: "https://script.google.com/macros/s/...") }
    var wifi1 by remember { mutableStateOf(localData?.wifiSsid ?: "") }
    var wifi2 by remember { mutableStateOf(localData?.wifiSsid2 ?: "") }
    var wifi3 by remember { mutableStateOf(localData?.wifiSsid3 ?: "") }

    var isLocked by remember { mutableStateOf(true) }

    fun persistChanges() {
        val current = readLocalMemberProfile()
        if (current != null) {
            saveLocalMemberProfile(
                current.copy(
                    gymApiUrl = apiUrlText,
                    wifiSsid = wifi1,
                    wifiSsid2 = wifi2,
                    wifiSsid3 = wifi3
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryTurquoise,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                persistChanges()
                                onBack()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Configuración de Red",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. SERVIDOR DE RED (GRATIS)
                item {
                    Text(
                        text = "Servidor de Red (Gratis)",
                        color = PrimaryTurquoise,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = apiUrlText,
                                onValueChange = {
                                    apiUrlText = it
                                    persistChanges()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLocked,
                                leadingIcon = { Icon(Icons.Default.Link, null, tint = PrimaryTurquoise) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isLocked = !isLocked }) {
                                            Icon(
                                                imageVector = if (isLocked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = PrimaryTurquoise
                                            )
                                        }
                                        if (!isLocked && apiUrlText.isNotBlank()) {
                                            IconButton(onClick = {
                                                apiUrlText = ""
                                                persistChanges()
                                            }) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                },
                                visualTransformation = if (isLocked) PasswordVisualTransformation() else VisualTransformation.None,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryTurquoise,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    disabledBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color(0xFF00222E),
                                    unfocusedContainerColor = Color(0xFF00222E),
                                    disabledContainerColor = Color(0xFF00222E),
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    disabledTextColor = TextWhite
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "La red se oculta por seguridad, no la comparta.",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 2. ASISTENCIA AUTOMÁTICA POR WI-FI
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Asistencia Automática por Wi-Fi",
                        color = PrimaryTurquoise,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WifiSsidTextField(value = wifi1, onValueChange = { wifi1 = it; persistChanges() }, label = "Red 1 (SSID)")
                        WifiSsidTextField(value = wifi2, onValueChange = { wifi2 = it; persistChanges() }, label = "Red 2 (SSID)")
                        WifiSsidTextField(value = wifi3, onValueChange = { wifi3 = it; persistChanges() }, label = "Red 3 (SSID)")
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "La app marcará asistencia cuando te conectes a esta red.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WifiSsidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            placeholder = { Text(label, color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Wifi, null, tint = PrimaryTurquoise) },
            trailingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTurquoise) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            )
        )
    }
}
