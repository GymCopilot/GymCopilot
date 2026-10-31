package com.zexo.gymcopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.ui.theme.LocalBottomNavBackgroundColor
import com.zexo.gymcopilot.ui.theme.TextGray

@Composable
fun MemberBottomNavigation(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onClassesClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onChatClick: () -> Unit,
    accentColor: Color,
    unreadCount: Int = 0
) {
    NavigationBar(
        modifier = Modifier.height(56.dp),
        containerColor = LocalBottomNavBackgroundColor.current,
        contentColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val items = listOf(
            MemberNavigationItemData("member_home", Icons.Default.Dashboard, "Home", onHomeClick),
            MemberNavigationItemData("member_store", Icons.Default.Storefront, "Tienda", onStoreClick),
            MemberNavigationItemData("member_classes", Icons.Default.CalendarMonth, "Clases", onClassesClick),
            MemberNavigationItemData("member_routines", Icons.Default.FitnessCenter, "Rutinas", onRoutinesClick),
            MemberNavigationItemData("member_chat", Icons.AutoMirrored.Filled.Chat, "Chat", onChatClick)
        )

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) accentColor else TextGray
                        )
                        if (item.route == "member_chat" && unreadCount > 0) {
                            // Punto rojo estilo administrador/profesor: pequeño, circular y sin número
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .background(Color.Red, CircleShape)
                                    .border(1.5.dp, LocalBottomNavBackgroundColor.current, CircleShape)
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

private data class MemberNavigationItemData(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)
