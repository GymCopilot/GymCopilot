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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfessorScreen(
    professorId: String? = null,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val professors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = null)
    val isProfessor = userRole == "professor"

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    var currentId by remember { mutableStateOf(professorId ?: java.util.UUID.randomUUID().toString()) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }

    val usedColors = remember(professors, currentId) {
        professors.filter { it.id != currentId }.mapNotNull { it.profileColor?.let { c -> Color(c) } }.toSet()
    }

    LaunchedEffect(professorId) {
        if (professorId != null) {
            val profs = dataStoreManager.getProfessors().first()
            val prof = profs.find { it.id == professorId }
            if (prof != null) {
                currentId = prof.id
                firstName = prof.firstName
                lastName = prof.lastName
                email = prof.email
                address = prof.address
                phone = prof.phone
                idNumber = prof.idNumber
                taxId = prof.taxId
                specialty = prof.specialty
                emergencyContact = prof.emergencyContact
                bankAccount = prof.bankAccount
                photoUri = prof.photoUri
                selectedColor = prof.profileColor?.let { Color(it) }
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val persistentUri = if (it.scheme == "content") {
                saveImageToInternalStorage(context, it)
            } else {
                it
            }
            photoUri = persistentUri?.toString() ?: it.toString()
        }
    }

    val screenTitle = if (professorId == null) {
        if (appLanguage == "Español") "Nuevo Profesor" else "New Professor"
    } else {
        if (appLanguage == "Español") "Editar Profesor" else "Edit Professor"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                actions = {
                    if (isProfessor) {
                        IconButton(onClick = onScanClick) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner, 
                                contentDescription = "Scan QR", 
                                tint = accentColor
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (firstName.isBlank() || lastName.isBlank()) {
                                val msg = if (appLanguage == "Español") "Nombre y Apellido son obligatorios" else "First and Last name are required"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            coroutineScope.launch {
                                val persistentUri = photoUri?.let { uriStr ->
                                    val uri = Uri.parse(uriStr)
                                    if (uri.scheme == "content") {
                                        saveImageToInternalStorage(context, uri)
                                    } else {
                                        uri
                                    }
                                }

                                val base64Image = persistentUri?.let { getBase64FromUri(context, it) }
                                val imageToSync = if (base64Image != null) "data:image/png;base64,$base64Image" else null

                                val professor = Professor(
                                    id = currentId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    address = address,
                                    phone = phone,
                                    idNumber = idNumber,
                                    taxId = taxId,
                                    specialty = specialty,
                                    emergencyContact = emergencyContact,
                                    bankAccount = bankAccount,
                                    photoUri = persistentUri?.toString() ?: photoUri,
                                    profileColor = selectedColor?.toArgb()
                                )
                                if (professorId != null) {
                                    dataStoreManager.updateProfessor(professor)
                                } else {
                                    dataStoreManager.addProfessor(professor)
                                }
                                
                                // Bidirectional sync with spreadsheet (using optimized imageToSync)
                                val professorForSync = professor.copy(photoUri = imageToSync)
                                attendanceRepository?.syncProfessorProfile(professorForSync)
                                
                                val savedMsg = if (appLanguage == "Español") "Profesor guardado" else "Professor saved"
                                Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.checked), contentDescription = "Guardar", tint = accentColor, modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        bottomBar = {
            AdminBottomNavigation(
                currentRoute = "app_settings",
                onHomeClick = onHomeClick,
                onStoreClick = onStoreClick,
                onMembersClick = onMembersClick,
                onProfessorsClick = onProfessorsClick,
                onSettingsClick = onSettingsClick,
                accentColor = accentColor
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(0.dp, 20.dp, 0.dp, 100.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(2.dp, selectedColor ?: accentColor, CircleShape)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
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
                            AsyncImage(
                                model = imgModel,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = selectedColor ?: TextGray)
                        }
                    }
                    val photoTxt = if (appLanguage == "Español") "Toca para subir foto" else "Tap to upload photo"
                    Text(photoTxt, color = selectedColor ?: accentColor, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            item {
                val colorLabel = if (appLanguage == "Español") "Color de Perfil" else "Profile Color"
                Text(colorLabel, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(ProfessorColorsPalette) { color ->
                        val isUsed = usedColors.contains(color)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isUsed) color.copy(alpha = 0.2f) else color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable(enabled = !isUsed) { selectedColor = color }
                        ) {
                            if (isUsed) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                val personalDataLabel = if (appLanguage == "Español") "Datos Personales" else "Personal Data"
                Text(personalDataLabel, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val namesLabel = if (appLanguage == "Español") "Nombres" else "Names"
                    val lastNameLabel = if (appLanguage == "Español") "Apellido" else "Last Name"
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = firstName, onValueChange = { firstName = it }, label = namesLabel, accentColor = accentColor, shape = containerShape)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = lastName, onValueChange = { lastName = it }, label = lastNameLabel, accentColor = accentColor, shape = containerShape)
                    }
                }
                
                Spacer(Modifier.height(12.dp))

                val emailLabel = if (appLanguage == "Español") "Email" else "Email"
                CustomTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = emailLabel,
                    accentColor = accentColor,
                    shape = containerShape
                )

                Spacer(Modifier.height(12.dp))
                
                val phoneLabel = if (appLanguage == "Español") "Teléfono" else "Phone"
                CustomTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = phoneLabel,
                    accentColor = accentColor,
                    shape = containerShape
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                val jobDataLabel = if (appLanguage == "Español") "Datos Laborales" else "Job Data"
                Text(jobDataLabel, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                val specialtyLabel = if (appLanguage == "Español") "Especialidad" else "Specialty"
                CustomTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = specialtyLabel,
                    accentColor = accentColor,
                    shape = containerShape
                )

                Spacer(Modifier.height(12.dp))

                val addressLabel = if (appLanguage == "Español") "Dirección Residencial" else "Home Address"
                CustomTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = addressLabel,
                    accentColor = accentColor,
                    shape = containerShape
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                val docLabel = if (appLanguage == "Español") "Documentación" else "Documentation"
                Text(docLabel, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                
                val idLabel = if (appLanguage == "Español") "DNI" else "ID Number"
                val taxLabel = if (appLanguage == "Español") "CUIT / CUIL" else "Tax ID"
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = idNumber, onValueChange = { idNumber = it }, label = idLabel, accentColor = accentColor, shape = containerShape)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(value = taxId, onValueChange = { taxId = it }, label = taxLabel, accentColor = accentColor, shape = containerShape)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                val additionalLabel = if (appLanguage == "Español") "Información Adicional" else "Additional Info"
                Text(additionalLabel, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                val emergencyLabel = if (appLanguage == "Español") "Contacto de Emergencia" else "Emergency Contact"
                CustomTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = emergencyLabel,
                    accentColor = accentColor,
                    shape = containerShape
                )

                Spacer(Modifier.height(12.dp))

                val bankLabel = if (appLanguage == "Español") "CBU / Alias Bancario" else "Bank Account / Alias"
                CustomTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it },
                    label = bankLabel,
                    accentColor = accentColor,
                    shape = containerShape
                )
            }
            
            item {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "prof_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
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
