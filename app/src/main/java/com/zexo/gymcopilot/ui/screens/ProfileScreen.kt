package com.zexo.gymcopilot.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.*
import com.zexo.gymcopilot.utils.ShortcutUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onScanClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null,
    onSaveSync: (firstName: String, lastName: String, email: String, phone: String, address: String, weight: String, height: String, photoUri: String?) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val userRole by dataStoreManager.getUserRole().collectAsState(initial = null)
    val linkedProfId by dataStoreManager.getLinkedProfessorId().collectAsState(initial = null)
    val linkedMemberEmail by dataStoreManager.getLinkedMemberEmail().collectAsState(initial = null)
    val userEmailStore by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userNameStore by dataStoreManager.getUserName().collectAsState(initial = "")
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")

    val isProfessor = userRole == "professor"
    val isMember = userRole == "member"

    // Find professor by ID, email or Name
    val currentProfessor = remember(professors, linkedProfId, userEmailStore, userNameStore) { 
        professors.find { it.id == linkedProfId } 
            ?: professors.find { it.email.equals(userEmailStore, ignoreCase = true) && it.email.isNotBlank() }
            ?: professors.find { it.fullName.equals(userNameStore, ignoreCase = true) && it.fullName.isNotBlank() }
    }
    val currentMember = remember(members, linkedMemberEmail) { members.find { it.email.equals(linkedMemberEmail, ignoreCase = true) } }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }

    val onSaveProfile = {
        coroutineScope.launch {
            val normalizedEmail = email.lowercase().trim()
            if (isProfessor) {
                val fullName = "$firstName $lastName".trim()
                var existing = professors.find { it.id == linkedProfId }
                if (existing == null && normalizedEmail.isNotBlank()) {
                    existing = professors.find { it.email.equals(normalizedEmail, ignoreCase = true) }
                }
                if (existing == null && fullName.isNotBlank()) {
                    existing = professors.find { it.fullName.equals(fullName, ignoreCase = true) }
                }

                val profToSave = (existing ?: currentProfessor ?: Professor(id = java.util.UUID.randomUUID().toString(), firstName = firstName, lastName = lastName)).copy(
                    id = existing?.id ?: currentProfessor?.id ?: linkedProfId ?: java.util.UUID.randomUUID().toString(),
                    firstName = firstName, lastName = lastName, email = normalizedEmail,
                    phone = phone, address = address, idNumber = idNumber, taxId = taxId,
                    specialty = specialty, emergencyContact = emergencyContact,
                    bankAccount = bankAccount, photoUri = photoUri, profileColor = selectedColor?.toArgb()
                )
                dataStoreManager.updateProfessor(profToSave)
                attendanceRepository?.syncProfessorProfile(profToSave)
                dataStoreManager.setLinkedProfessorId(profToSave.id)
            } else if (isMember) {
                val memberToUpdate = currentMember ?: members.find { it.email.equals(normalizedEmail, ignoreCase = true) }
                if (memberToUpdate != null) {
                    dataStoreManager.updateMember(memberToUpdate.copy(firstName = firstName, lastName = lastName, email = normalizedEmail, phone = phone, weight = weight, height = height, address = address, photoUri = photoUri))
                }
                onSaveSync(firstName, lastName, normalizedEmail, phone, address, weight, height, photoUri)
            }
            
            dataStoreManager.setUserEmail(normalizedEmail)
            dataStoreManager.setUserName("$firstName $lastName")
            if (photoUri != null) dataStoreManager.setUserPhotoUri(photoUri!!)
            
            Toast.makeText(context, if(appLanguage == "Español") "Perfil guardado" else "Profile saved", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(currentProfessor, currentMember, isProfessor, isMember) {
        when {
            isProfessor && currentProfessor != null -> {
                firstName = currentProfessor.firstName
                lastName = currentProfessor.lastName
                email = currentProfessor.email
                phone = currentProfessor.phone
                address = currentProfessor.address
                idNumber = currentProfessor.idNumber
                taxId = currentProfessor.taxId
                specialty = currentProfessor.specialty
                emergencyContact = currentProfessor.emergencyContact
                bankAccount = currentProfessor.bankAccount
                photoUri = currentProfessor.photoUri
                selectedColor = currentProfessor.profileColor?.let { Color(it) }
            }
            isMember -> {
                if (currentMember != null) {
                    firstName = currentMember.firstName
                    lastName = currentMember.lastName
                    email = currentMember.email
                    phone = currentMember.phone
                    weight = currentMember.weight
                    height = currentMember.height
                    address = currentMember.address
                    photoUri = currentMember.photoUri
                } else {
                    val name = dataStoreManager.getUserName().first()
                    firstName = name.split(" ").firstOrNull() ?: ""
                    lastName = name.split(" ").drop(1).joinToString(" ")
                    email = dataStoreManager.getUserEmail().first()
                    phone = dataStoreManager.getUserPhone().first()
                    weight = dataStoreManager.getUserWeight().first()
                    height = dataStoreManager.getUserHeight().first()
                    address = dataStoreManager.getUserAddress().first()
                    photoUri = dataStoreManager.getUserPhotoUri().first()
                }
            }
            else -> {
                val name = dataStoreManager.getUserName().first()
                firstName = name.split(" ").firstOrNull() ?: ""
                lastName = name.split(" ").drop(1).joinToString(" ")
                email = dataStoreManager.getUserEmail().first()
                phone = dataStoreManager.getUserPhone().first()
                photoUri = dataStoreManager.getUserPhotoUri().first()
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        uri?.let {
            val base64 = getBase64FromUri(context, it)
            if (base64 != null) {
                photoUri = "data:image/png;base64,$base64"
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF00151C),
        topBar = {
            TopAppBar(
                title = { Text(if(appLanguage == "Español") "Mi Perfil" else "My Profile", color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(34.dp).clip(CircleShape), colorFilter = ColorFilter.tint(accentColor))
                    }
                },
                actions = {
                    IconButton(onClick = { onSaveProfile() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.checked),
                            contentDescription = "Save",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp)) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val avatarColor = if (isProfessor) (selectedColor ?: accentColor) else accentColor
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).border(2.dp, avatarColor, CircleShape).clickable { photoLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (photoUri != null) {
                            val imgModel = remember(photoUri) {
                                if (photoUri!!.startsWith("data:image")) {
                                    try {
                                        val base64String = photoUri!!.substringAfter("base64,")
                                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    } catch (e: Exception) { photoUri }
                                } else photoUri
                            }
                            AsyncImage(model = imgModel, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = TextGray)
                        }
                    }
                    val photoTxt = if (appLanguage == "Español") "Toca para cambiar foto" else "Tap to change photo"
                    Text(photoTxt, color = avatarColor, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item {
                Text(if(appLanguage == "Español") "Datos Personales" else "Personal Data", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { CustomTextField(value = firstName, onValueChange = { firstName = it }, label = "Nombres", accentColor = accentColor, shape = containerShape) }
                    Box(modifier = Modifier.weight(1f)) { CustomTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido", accentColor = accentColor, shape = containerShape) }
                }
                Spacer(Modifier.height(12.dp))
                CustomTextField(value = email, onValueChange = { email = it }, label = "Email", accentColor = accentColor, shape = containerShape)
                Spacer(Modifier.height(12.dp))
                CustomTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono", accentColor = accentColor, shape = containerShape)
            }
            if (isProfessor) {
                item {
                    Text("Laboral", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    CustomTextField(value = specialty, onValueChange = { specialty = it }, label = "Especialidad", accentColor = accentColor, shape = containerShape)
                    Spacer(Modifier.height(12.dp))
                    CustomTextField(value = address, onValueChange = { address = it }, label = "Dirección", accentColor = accentColor, shape = containerShape)

                    Spacer(Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            if (gymName.isNotBlank()) {
                                ShortcutUtils.createGymShortcut(context, gymName, gymLogoUri)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = containerShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AppShortcut, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if(appLanguage == "Español") "Instalar Icono en Inicio" else "Install Icon on Home", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isMember) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) { CustomTextField(value = weight, onValueChange = { weight = it }, label = "Peso (kg)", accentColor = accentColor, shape = containerShape) }
                        Box(modifier = Modifier.weight(1f)) { CustomTextField(value = height, onValueChange = { height = it }, label = "Altura (cm)", accentColor = accentColor, shape = containerShape) }
                    }
                    Spacer(Modifier.height(12.dp))
                    CustomTextField(value = address, onValueChange = { address = it }, label = "Dirección", accentColor = accentColor, shape = containerShape)

                    Spacer(Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            if (gymName.isNotBlank()) {
                                ShortcutUtils.createGymShortcut(context, gymName, gymLogoUri)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = containerShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AppShortcut, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if(appLanguage == "Español") "Instalar Icono en Inicio" else "Install Icon on Home", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getBase64FromUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()
        val maxDim = 200
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val width = if (aspectRatio > 1) maxDim else (maxDim * aspectRatio).toInt()
            val height = if (aspectRatio > 1) (maxDim / aspectRatio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else bitmap
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) { null }
}
