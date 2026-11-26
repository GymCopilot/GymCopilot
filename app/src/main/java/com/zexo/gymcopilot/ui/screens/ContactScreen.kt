package com.zexo.gymcopilot.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    onBack: () -> Unit,
    onReportProblemClick: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val uriHandler = LocalUriHandler.current

    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val shape = getButtonStyleShape(buttonStyle)

    val t = remember(appLanguage) {
        if (appLanguage == "Español") {
            mapOf(
                "title" to "Contacto",
                "desc" to "Estamos aquí para ayudarte. Contacta con nosotros por cualquiera de estas vías.",
                "support" to "Soporte Técnico",
                "support_desc" to "Reportar un problema de la app",
                "email" to "Correo Electrónico",
                "email_desc" to "zexo.apps.ok@gmail.com",
                "web" to "Sitio Web",
                "web_desc" to "sites.google.com/view/zexo-apps/gymcopilot",
                "privacy" to "Privacidad",
                "privacy_desc" to "Políticas de privacidad de nuestras apps",
                "whatsapp" to "WhatsApp",
                "whatsapp_desc" to "+54 2266486794"
            )
        } else {
            mapOf(
                "title" to "Contact",
                "desc" to "We are here to help. Contact us through any of these channels.",
                "support" to "Technical Support",
                "support_desc" to "Report an app issue",
                "email" to "Email",
                "email_desc" to "zexo.apps.ok@gmail.com",
                "web" to "Website",
                "web_desc" to "sites.google.com/view/zexo-apps/gymcopilot",
                "privacy" to "Privacy",
                "privacy_desc" to "Privacy policies of our apps",
                "whatsapp" to "WhatsApp",
                "whatsapp_desc" to "+54 2266486794"
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                item {
                    Text(
                        text = t["desc"]!!,
                        color = TextGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    ContactCard(
                        title = t["support"]!!,
                        description = t["support_desc"]!!,
                        icon = Icons.Default.BugReport,
                        accentColor = accentColor,
                        iconColor = Color.Red,
                        shape = shape,
                        onClick = onReportProblemClick
                    )
                }

                item {
                    ContactCard(
                        title = t["whatsapp"]!!,
                        description = t["whatsapp_desc"]!!,
                        icon = painterResource(id = R.drawable.whatsapp),
                        accentColor = accentColor,
                        iconColor = Color.Unspecified,
                        shape = shape,
                        onClick = { uriHandler.openUri("https://wa.me/542266486794") } 
                    )
                }

                item {
                    ContactCard(
                        title = t["email"]!!,
                        description = t["email_desc"]!!,
                        icon = Icons.Default.Email,
                        accentColor = accentColor,
                        iconColor = Color.White,
                        shape = shape,
                        onClick = { 
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:zexo.apps.ok@gmail.com")
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    ContactCard(
                        title = t["web"]!!,
                        description = t["web_desc"]!!,
                        icon = Icons.Default.Language,
                        accentColor = accentColor,
                        iconColor = Color(0xFFFF9800),
                        shape = shape,
                        onClick = { uriHandler.openUri("https://sites.google.com/view/zexo-apps/gymcopilot") }
                    )
                }

                item {
                    ContactCard(
                        title = t["privacy"]!!,
                        description = t["privacy_desc"]!!,
                        icon = Icons.Default.PrivacyTip,
                        accentColor = accentColor,
                        shape = shape,
                        onClick = { uriHandler.openUri("https://sites.google.com/view/zexo-apps/pol%C3%ADticas-de-privacidad-de-las-apps?authuser=0") }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactCard(
    title: String,
    description: String,
    icon: Any,
    accentColor: Color,
    iconColor: Color? = null,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    val finalIconColor = iconColor ?: accentColor
    val backgroundCircleColor = if (finalIconColor == Color.Unspecified) Color.White.copy(alpha = 0.1f) else finalIconColor.copy(alpha = 0.1f)

    val buttonBgColor = LocalButtonBackgroundColor.current
    val titleColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite
    val descColor = if (buttonBgColor.luminance() > 0.5f) Color.DarkGray else TextGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.3f), shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = buttonBgColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(backgroundCircleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (icon) {
                    is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, null, tint = finalIconColor, modifier = Modifier.size(24.dp))
                    is ImageVector -> Icon(icon, null, tint = finalIconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, color = descColor, fontSize = 12.sp)
            }
        }
    }
}
