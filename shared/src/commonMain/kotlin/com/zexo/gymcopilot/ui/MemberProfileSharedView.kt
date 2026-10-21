package com.zexo.gymcopilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.utils.decodeBase64ToBitmap
import com.zexo.gymcopilot.utils.launchImagePicker
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun MemberProfileSharedScreen(
    initialFirstName: String = "",
    initialLastName: String = "",
    initialEmail: String = "",
    initialPhone: String = "",
    initialAddress: String = "",
    initialWeight: String = "",
    initialHeight: String = "",
    initialPhotoUri: String? = null,
    onBack: () -> Unit,
    onSaveProfile: (firstName: String, lastName: String, email: String, phone: String, address: String, weight: String, height: String, photoUri: String?) -> Unit
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }
    var weight by remember { mutableStateOf(initialWeight) }
    var height by remember { mutableStateOf(initialHeight) }
    var photoUri by remember { mutableStateOf<String?>(initialPhotoUri) }

    val photoBitmap = remember(photoUri) {
        if (photoUri != null) decodeBase64ToBitmap(photoUri!!) else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryTurquoise,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Mi Perfil",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    // BOTÓN GUARDAR (CHECK CIRCULAR)
                    Surface(
                        color = PrimaryTurquoise,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                onSaveProfile(firstName, lastName, email, phone, address, weight, height, photoUri)
                                onBack()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. FOTO CIRCULAR DE PERFIL
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(2.dp, PrimaryTurquoise, CircleShape)
                                .clickable {
                                    launchImagePicker { pickedPhoto ->
                                        photoUri = pickedPhoto
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Cambiar Foto",
                                    tint = TextGray,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Toca para cambiar foto",
                            color = PrimaryTurquoise,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. DATOS PERSONALES
                item {
                    Text(
                        text = "Datos Personales",
                        color = PrimaryTurquoise,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CustomSharedTextField(value = firstName, onValueChange = { firstName = it }, label = "Nombres")
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CustomSharedTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                            }
                        }

                        CustomSharedTextField(value = email, onValueChange = { email = it }, label = "Email")
                        CustomSharedTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CustomSharedTextField(value = weight, onValueChange = { weight = it }, label = "Peso (kg)")
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CustomSharedTextField(value = height, onValueChange = { height = it }, label = "Altura (cm)")
                            }
                        }

                        CustomSharedTextField(value = address, onValueChange = { address = it }, label = "Dirección")
                    }
                }

                // 3. SEPARADOR Y BOTÓN "INSTALAR ICONO EN INICIO"
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFF001F29),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryTurquoise),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.mobile_arrow_down),
                                contentDescription = null,
                                tint = PrimaryTurquoise,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Instalar Icono en Inicio",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
