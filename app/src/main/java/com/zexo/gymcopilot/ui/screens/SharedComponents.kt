package com.zexo.gymcopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite

data class MetricData(
    val label: String, 
    val value: String, 
    val icon: ImageVector, 
    val color: Color, 
    val isSecure: Boolean = false,
    val isVisible: Boolean = true,
    val onToggleVisibility: (() -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SmallMetricCard(data: MetricData, modifier: Modifier, accentColor: Color, shape: Shape) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val valColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val lblColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(
        modifier = modifier
            .border(1.5.dp, accentColor.copy(alpha = 0.4f), shape)
            .clickable(enabled = data.onClick != null) { data.onClick?.invoke() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = buttonBgColor)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val contentWeight = if (data.isSecure) 3f else 1f
            Row(modifier = Modifier.weight(contentWeight), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(data.color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(data.icon, null, tint = data.color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(data.value, color = valColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(data.label, color = lblColor, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (data.isSecure) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { data.onToggleVisibility?.invoke() }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = if (data.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = TextGray.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BroadcastMessageDisplay(dataStoreManager: DataStoreManager, accentColor: Color) {
    val message by dataStoreManager.getBroadcastMessage().collectAsState(initial = "")
    
    if (message.isNotBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f))
                .background(
                    brush = Brush.horizontalGradient(
                        0.0f to Color(0xFF8A2BE2).copy(alpha = 0.3f), // Violeta
                        0.3f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1.0f to Color.Red.copy(alpha = 0.3f) // Rojo
                    )
                )
                .padding(vertical = 12.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 1000
                )
            )
        }
    }
}
