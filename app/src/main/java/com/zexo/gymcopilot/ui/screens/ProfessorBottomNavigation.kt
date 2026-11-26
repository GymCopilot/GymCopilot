package com.zexo.gymcopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.zexo.gymcopilot.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.ui.theme.LocalBottomNavBackgroundColor
import com.zexo.gymcopilot.ui.theme.TextGray

@Composable
fun ProfessorBottomNavigation(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onStoreClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onMembersClick: () -> Unit,
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
            ProfessorNavigationItemData("professor_dashboard", rememberVectorPainter(Icons.Default.Dashboard), "Home", onHomeClick),
            ProfessorNavigationItemData("store", rememberVectorPainter(Icons.Default.Storefront), "Tienda", onStoreClick),
            ProfessorNavigationItemData("manage_schedule", rememberVectorPainter(Icons.Default.CalendarMonth), "Horarios", onScheduleClick),
            ProfessorNavigationItemData("members", rememberVectorPainter(Icons.Default.Group), "Alumnos", onMembersClick),
            ProfessorNavigationItemData("member_routines", rememberVectorPainter(Icons.Default.FitnessCenter), "Rutinas", onRoutinesClick),
            ProfessorNavigationItemData("member_chat", rememberVectorPainter(Icons.Default.Chat), "Chat", onChatClick)
        )

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp, vertical = 4.dp)
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
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) accentColor else TextGray
                        )
                        // Punto rojo de notificación estilo administrador
                        if (item.route == "member_chat" && unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .background(Color.Red, CircleShape)
                                    .border(1.dp, LocalBottomNavBackgroundColor.current, CircleShape)
                            )
                        }
                    }
                    Text(
                        text = item.label,
                        fontSize = 8.sp,
                        color = if (isSelected) accentColor else TextGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class ProfessorNavigationItemData(
    val route: String,
    val icon: Painter,
    val label: String,
    val onClick: () -> Unit
)
