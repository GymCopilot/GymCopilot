package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    gymName: String?,
    gymLogoUri: String?,
    accentColor: Color = PrimaryTurquoise
) {
    if (gymName == null) {
        // Mientras carga, mantenemos la pantalla vacía (coincide con el splash del sistema)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081C24))
        )
        return
    }

    val alpha = remember { Animatable(0f) }
    val malvinasAlpha = remember { Animatable(0f) }
    
    val malvinasSans = remember {
        FontFamily(Font(R.font.malvinassans))
    }

    LaunchedEffect(Unit) {
        // Animación principal (logo del gym)
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
            delay(2500)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        
        // Animación sutil para Malvinas (aparece y desaparece)
        launch {
            delay(400)
            malvinasAlpha.animateTo(1f, tween(600))
            delay(2000) // Tus 2 segundos de visibilidad total
            malvinasAlpha.animateTo(0f, tween(800))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF081C24)), // Fondo oscuro consistente con la app
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            // Logo dinámico o por defecto
            if (!gymLogoUri.isNullOrBlank()) {
                val logoModel = remember(gymLogoUri) {
                    if (gymLogoUri.startsWith("data:image")) {
                        try {
                            val base64String = gymLogoUri!!.substringAfter("base64,")
                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) { gymLogoUri }
                    } else gymLogoUri
                }
                
                AsyncImage(
                    model = logoModel,
                    contentDescription = "Gym Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.gymcopilot),
                    contentDescription = "Default Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre dinámico o por defecto
            val displayName = if (gymName.isBlank()) "GymCopilot" else gymName
            Text(
                text = displayName,
                color = accentColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            
            Text(
                text = if (gymName.isBlank()) "Tu asistente de entrenamiento" else "Bienvenido",
                color = TextGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Homenaje a las Malvinas en la parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(malvinasAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.malvinas),
                    contentDescription = "Malvinas",
                    modifier = Modifier.size(45.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Son Argentinas",
                color = TextWhite.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontFamily = malvinasSans,
                textAlign = TextAlign.Center
            )
        }
    }
}
