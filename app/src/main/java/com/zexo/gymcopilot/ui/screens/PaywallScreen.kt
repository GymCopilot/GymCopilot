package com.zexo.gymcopilot.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.network.BillingManager
import com.zexo.gymcopilot.ui.theme.*

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val accentColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val color = if (accentColor != null) Color(accentColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
        ) {
            item {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Acceso Premium",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Desbloquea todo el potencial de tu gimnasio",
                    color = TextGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureItem("Rutinas personalizadas", Icons.Default.FitnessCenter, color)
                    FeatureItem("Seguimiento de progreso", Icons.Default.CheckCircle, color)
                    FeatureItem("Chat con tus profesores", Icons.Default.CheckCircle, color)
                    FeatureItem("Gestión de asistencia QR", Icons.Default.CheckCircle, color)
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val activity = context as? Activity
                        activity?.let {
                            billingManager.launchBillingFlow(it, "premium_subscription")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = shape
                ) {
                    Text(
                        "SUSCRIBIRSE AHORA",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                
                TextButton(onClick = onBack) {
                    Text("Volver", color = TextGray)
                }
            }
        }
    }
}

@Composable
fun FeatureItem(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
