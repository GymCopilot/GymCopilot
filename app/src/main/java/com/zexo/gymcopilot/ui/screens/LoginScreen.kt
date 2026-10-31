package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.LockConfig
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onRoleSelected: (String) -> Unit, onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val buttonShape = getButtonStyleShape(buttonStyle)
    
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    val showLogoBorder by dataStoreManager.getShowLogoBorder().collectAsState(initial = true)
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = null)

    val isRoleLocked = remember(userRole) { 
        LockConfig.IS_ROLE_LOCK_ENABLED == 1 && !userRole.isNullOrBlank() 
    }

    val welcome: String; val selectRole: String
    val ownerTitle: String; val ownerDesc: String
    val profTitle: String; val profDesc: String
    val memberTitle: String; val memberDesc: String

    when (appLanguage) {
        "Español" -> {
            welcome = "Bienvenido a"; selectRole = "Selecciona tu rol para continuar"
            ownerTitle = "Administrador"; ownerDesc = "Gestiona tu gimnasio, socios y planes"
            profTitle = "Profesor"; profDesc = "Acceso al panel de entrenamiento"
            memberTitle = "Socio del Gimnasio"; memberDesc = "Sigue tu progreso y tabla"
        }
        else -> {
            welcome = "Welcome to"; selectRole = "Select your role to continue"
            ownerTitle = "Administrator"; ownerDesc = "Manage your gym, members, and plans"
            profTitle = "Professor"; profDesc = "Access training panel"
            memberTitle = "Gym Member"; memberDesc = "Track your progress and leaderboard"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = if (gymLogoUri != null) 60.dp else 100.dp, bottom = 40.dp)
        ) {
            item {
                if (gymLogoUri != null) {
                    val logoModel = remember(gymLogoUri) {
                        if (gymLogoUri?.startsWith("data:image") == true) {
                            try {
                                val base64String = gymLogoUri!!.substringAfter("base64,")
                                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) { gymLogoUri }
                        } else gymLogoUri
                    }
                    val logoShape = if (showLogoBorder) buttonShape else RoundedCornerShape(12.dp)
                    AsyncImage(
                        model = logoModel,
                        contentDescription = "Gym Logo",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(logoShape)
                            .then(if (showLogoBorder) {
                                Modifier.border(2.5.dp, accentColor, logoShape)
                            } else Modifier)
                            .padding(if (showLogoBorder) 8.dp else 0.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Text(text = welcome, color = TextGray, fontSize = 18.sp, textAlign = TextAlign.Center)
                val mainTitle = if (gymName.isNullOrBlank()) "GymCopilot" else gymName!!
                Text(text = mainTitle, color = accentColor, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isRoleLocked) {
                    Text(
                        text = if (appLanguage == "Español") "Perfil vinculado permanentemente a este dispositivo" else "Profile permanently linked to this device",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Text(text = selectRole, color = TextWhite, fontSize = 16.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isRoleLocked || userRole == "admin") {
                item {
                    RoleCard(title = ownerTitle, description = ownerDesc, icon = rememberVectorPainter(Icons.Default.AdminPanelSettings), accentColor = accentColor, shape = buttonShape, onClick = {
                        scope.launch {
                            dataStoreManager.setUserRole("admin")
                            dataStoreManager.setAuthToken("admin_session")
                            dataStoreManager.setUserName("Admin")
                            dataStoreManager.setUserEmail("") // Admin no usa email para chat
                            onRoleSelected("admin")
                        }
                    })
                }
            }

            if (!isRoleLocked || userRole == "professor") {
                item {
                    RoleCard(title = profTitle, description = profDesc, icon = painterResource(R.drawable.assistant), accentColor = accentColor, shape = buttonShape, onClick = {
                        scope.launch {
                            dataStoreManager.setUserRole("professor")
                            dataStoreManager.setAuthToken("prof_session")
                            // Limpiar nombres genéricos para obligar a usar el perfil real
                            val currentName = dataStoreManager.getUserName().first()
                            if (currentName == "Admin") dataStoreManager.setUserName("")
                            onRoleSelected("professor")
                        }
                    })
                }
            }

            if (!isRoleLocked || userRole == "member") {
                item {
                    RoleCard(title = memberTitle, description = memberDesc, icon = rememberVectorPainter(Icons.Default.Person), accentColor = accentColor, shape = buttonShape, onClick = {
                        scope.launch {
                            dataStoreManager.setUserRole("member")
                            dataStoreManager.setAuthToken("member_session")
                            val currentName = dataStoreManager.getUserName().first()
                            if (currentName == "Admin") dataStoreManager.setUserName("")
                            onRoleSelected("member")
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun RoleCard(title: String, description: String, icon: Painter, photoUri: String? = null, accentColor: Color, shape: Shape, onClick: () -> Unit) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val contentColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val descColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Box(
        modifier = Modifier.fillMaxWidth().clip(shape).background(buttonBgColor).border(1.5.dp, accentColor, shape).clickable { onClick() }.padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(shape).background(accentColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                if (photoUri != null) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(painter = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = description, color = descColor, fontSize = 14.sp)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        }
    }
}
