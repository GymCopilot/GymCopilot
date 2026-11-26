package com.zexo.gymcopilot.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    var problemDescription by remember { mutableStateOf("") }

    val t = remember(appLanguage) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Reportar Problema",
                "desc" to "Describe el error o inconveniente que encontraste. Incluiremos datos técnicos de tu dispositivo para ayudarnos a resolverlo mejor.",
                "label" to "Descripción del problema",
                "placeholder" to "Ej: La app se cierra al abrir la tienda...",
                "button" to "Enviar Reporte",
                "subject" to "Reporte de Fallo - GymCopilot"
            )
        } else {
            mapOf(
                "title" to "Report Problem",
                "desc" to "Describe the error or issue you found. We will include technical data from your device to help us solve it better.",
                "label" to "Problem description",
                "placeholder" to "Ex: The app closes when opening the store...",
                "button" to "Send Report",
                "subject" to "Bug Report - GymCopilot"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00151C))) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t["title"]!!, color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = painterResource(id = R.drawable.back),
                                contentDescription = "Back",
                                modifier = Modifier.size(34.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))
                Text(t["desc"]!!, color = TextGray, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = { problemDescription = it },
                    label = { Text(t["label"]!!, color = accentColor) },
                    placeholder = { Text(t["placeholder"]!!, color = TextGray.copy(0.5f)) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = shape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = accentColor.copy(0.3f),
                        cursorColor = accentColor,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        val deviceData = "\n\n-- DATOS TÉCNICOS --\n" +
                                "Marca: ${Build.MANUFACTURER}\n" +
                                "Modelo: ${Build.MODEL}\n" +
                                "Versión Android: ${Build.VERSION.RELEASE}\n" +
                                "SDK: ${Build.VERSION.SDK_INT}"
                        
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("zexo.apps.ok@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, t["subject"])
                            putExtra(Intent.EXTRA_TEXT, problemDescription + deviceData)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, accentColor, shape),
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                    enabled = problemDescription.isNotBlank()
                ) {
                    val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                    Text(t["button"]!!, color = contentColor, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
