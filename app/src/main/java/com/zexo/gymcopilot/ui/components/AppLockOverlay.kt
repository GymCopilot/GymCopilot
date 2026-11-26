package com.zexo.gymcopilot.ui.components

import androidx.compose.animation.core.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.LockConfig
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.utils.SecurityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockOverlay(
    dataStoreManager: DataStoreManager,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    var codeInput by remember { mutableStateOf("") }
    val dynamicCode = remember { SecurityUtils.generateDynamicCode() }

    var isError by remember { mutableStateOf(false) }
    val pulseProgress = remember { Animatable(0f) }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Red.copy(alpha = 0f),
            Color.Red.copy(alpha = pulseProgress.value),
            Color.Red.copy(alpha = 0f)
        )
    )

    val pulsatingErrorColor = Color.Red.copy(alpha = pulseProgress.value)
    
    // Color dinámico para elementos (Icono, Botón) transicionando a rojo
    val dynamicElementColor = Color(
        red = accentColor.red + (Color.Red.red - accentColor.red) * pulseProgress.value,
        green = accentColor.green + (Color.Red.green - accentColor.green) * pulseProgress.value,
        blue = accentColor.blue + (Color.Red.blue - accentColor.blue) * pulseProgress.value,
        alpha = 1f
    )

    LaunchedEffect(isError) {
        if (isError) {
            pulseProgress.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            pulseProgress.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            isError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00151C))
            .zIndex(1000f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .border(2.dp, gradientBrush, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF081C24))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = dynamicElementColor,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Licencia Expirada",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "El periodo de uso gratuito ha finalizado. Por favor, ingrese el código de activación para continuar.",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Código de Activación", color = TextGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isError) pulsatingErrorColor else accentColor,
                        unfocusedBorderColor = if (isError) pulsatingErrorColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (codeInput.trim().lowercase() == dynamicCode) {
                            scope.launch {
                                // Extender por el valor absoluto de los días configurados
                                val days = kotlin.math.abs(LockConfig.LOCK_DAYS).toLong()
                                val extensionMillis = if (days > 0) days * 24L * 60 * 60 * 1000 else 24L * 60 * 60 * 1000 // Mínimo 1 día si es 0 (aunque 0 desactiva)
                                val newExpiration = System.currentTimeMillis() + extensionMillis
                                dataStoreManager.setAppLockExpirationTime(newExpiration)
                                onUnlock()
                            }
                        } else {
                            isError = true
                            Toast.makeText(context, "Código incorrecto", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().border(if(isError) 2.dp else 0.dp, pulsatingErrorColor, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(isError) dynamicElementColor.copy(alpha = 0.8f) else accentColor)
                ) {
                    Text(
                        text = "ACTIVAR AHORA",
                        color = if (accentColor.luminance() > 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
