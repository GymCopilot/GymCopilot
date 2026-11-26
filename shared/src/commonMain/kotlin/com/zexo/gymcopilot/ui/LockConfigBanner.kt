package com.zexo.gymcopilot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.LockConfig

/**
 * Banner superior desplegable para mostrar el estado de configuración (Modo Prueba / Edición).
 * Al tocarlo o pasar el puntero sobre él, se despliega un segundo banner en rojo transparente por debajo
 * mostrando con letra muy pequeña los estados de LockConfig.kt.
 */
@Composable
fun LockConfigBanner(
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    val isExpanded = isHovered || isTapped

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .clickable { isTapped = !isTapped }
    ) {
        // Banner Superior Principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F).copy(alpha = 0.95f))
                .padding(vertical = 6.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚠️ MODO PRUEBA (TEST) - AISLADO",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Sub-banner Desplegable (Rojo transparente con el fondo)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD32F2F).copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Roles Web: " + if (LockConfig.WEB_ROLE_LOCK_ENABLED == 1) "Solo Alumno (1)" else "Todos Visibles (0)",
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• Roles Android: " + if (LockConfig.ANDROID_ROLE_LOCK_ENABLED == 1) "Bloqueados (1)" else "Todos Visibles (0)",
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• PWA Web: " + if (LockConfig.WEB_IOS_ONLY_ENABLED == 1) "Exclusivo iOS (1)" else "Cualquier Disp. (0)",
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• MercadoPago: " + if (LockConfig.MP_ENABLED) "ON" else "OFF",
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Pago Licencia: " + if (LockConfig.LICENSE_MODEL_PAYER == "admin") "Admin (Gimnasio)" else "Member (Socio)",
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• Bloqueo Días: " + if (LockConfig.LOCK_DAYS != 0) "SI (${LockConfig.LOCK_DAYS}d)" else "NO",
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
