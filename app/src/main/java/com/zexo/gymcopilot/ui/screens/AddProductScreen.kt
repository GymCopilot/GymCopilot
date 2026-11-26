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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextWhite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddProductScreen(
    productId: String? = null,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    attendanceRepository: AttendanceRepository? = null
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val selectedCategoryIds by dataStoreManager.getStoreCategories().collectAsState(initial = emptySet())
    val customCategoryIds by dataStoreManager.getCustomCategoryIds().collectAsState(initial = emptySet())
    val appCurrencySelection by dataStoreManager.getAppCurrency().collectAsState(initial = "Peso Argentino")
    val currencySymbol = remember(appCurrencySelection) { CurrencyData.getSymbol(appCurrencySelection) }
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val repo = remember(gymApiUrl, attendanceRepository) {
        attendanceRepository ?: AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
    }

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val appAccentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    var selectedContactMethod by remember { mutableStateOf<String?>(null) }
    var contactPhone by remember { mutableStateOf("") }

    var selectedBannerType by remember { mutableStateOf<String?>(null) }
    var selectedBannerText by remember { mutableStateOf("") }
    var showBannerDropdown by remember { mutableStateOf(false) }
    
    var isSaving by remember { mutableStateOf(false) }

    val bannerPhrases = remember(appLanguage) {
        BannerTranslations.getPhrasesForLanguage(appLanguage)
    }

    LaunchedEffect(productId) {
        if (productId != null) {
            val allProducts = dataStoreManager.getProducts().first()
            val product = allProducts.find { it.id == productId }
            product?.let {
                productName = it.name
                productPrice = it.price.toString()
                productDescription = it.description
                selectedCategoryId = it.categoryId
                productImageUri = it.imageUri?.let { uri -> Uri.parse(uri) }
                selectedContactMethod = it.contactMethod
                contactPhone = it.contactPhone ?: ""
                selectedBannerType = it.bannerType
                selectedBannerText = it.bannerText ?: ""
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        productImageUri = uri
    }

    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to if (productId == null) "Nuevo Producto" else "Editar Producto",
                "name" to "Nombre del producto",
                "price" to "Precio",
                "desc" to "Descripción (opcional)",
                "cat" to "Categoría",
                "save" to "GUARDAR PRODUCTO",
                "saving" to "GUARDANDO...",
                "select_cat" to "Selecciona una categoría",
                "contact_info" to "Contacto de Venta (Opcional)",
                "phone" to "Teléfono de contacto",
                "banner_info" to "Banner Promocional",
                "banner_type" to "Tipo de Banner",
                "banner_text" to "Frase del Banner",
                "none" to "Ninguno",
                "preview" to "Vista Previa",
                "success" to "Producto guardado correctamente",
                "error" to "Error al sincronizar con la nube"
            )
            else -> mapOf(
                "title" to if (productId == null) "New Product" else "Edit Product",
                "name" to "Product name",
                "price" to "Price",
                "desc" to "Description (optional)",
                "cat" to "Category",
                "save" to "SAVE PRODUCT",
                "saving" to "SAVING...",
                "select_cat" to "Select a category",
                "contact_info" to "Sales Contact (Optional)",
                "phone" to "Contact Phone",
                "banner_info" to "Promotional Banner",
                "banner_type" to "Banner Type",
                "banner_text" to "Banner Phrase",
                "none" to "None",
                "preview" to "Preview",
                "success" to "Product saved successfully",
                "error" to "Error syncing with cloud"
            )
        }
    }

    val activeCategories = remember(selectedCategoryIds, customCategoryIds) {
        val fixed = GymCategory.entries.filter { it.id in selectedCategoryIds }
        val custom = customCategoryIds.filter { it in selectedCategoryIds }

        fixed.map { it.id to getCategoryTranslation(it, appLanguage) } +
                custom.map { id -> id to id }
    }

    LaunchedEffect(activeCategories) {
        if (selectedCategoryId.isEmpty() && activeCategories.isNotEmpty() && productId == null) {
            selectedCategoryId = activeCategories.first().first
        }
    }

    val categoryColorInt by dataStoreManager.getCategoryColor(selectedCategoryId).collectAsState(initial = null)
    val fixedCat = GymCategory.entries.find { it.id == selectedCategoryId }
    val previewAccentColor = if (categoryColorInt != null) Color(categoryColorInt!!)
    else fixedCat?.bgColor ?: Color(0xFF9575CD)

    val currentMethod = selectedContactMethod ?: "whatsapp"
    val previewContactIcon = when (currentMethod) {
        "whatsapp" -> R.drawable.whatsapp
        "telegram" -> R.drawable.telegram
        "viber" -> R.drawable.viber
        "wechat" -> R.drawable.wechat
        "line" -> R.drawable.line
        else -> R.drawable.whatsapp
    }

    val contactMethods = listOf(
        "whatsapp" to R.drawable.whatsapp,
        "telegram" to R.drawable.telegram,
        "viber" to R.drawable.viber,
        "wechat" to R.drawable.wechat,
        "line" to R.drawable.line
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF00151C)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isSaving) {
                            Image(
                                painter = painterResource(id = R.drawable.back),
                                contentDescription = "Back",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape),
                                colorFilter = ColorFilter.tint(appAccentColor)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                AdminBottomNavigation(
                    currentRoute = "store",
                    onHomeClick = onHomeClick,
                    onStoreClick = onStoreClick,
                    onMembersClick = onMembersClick,
                    onProfessorsClick = onProfessorsClick,
                    onSettingsClick = onSettingsClick,
                    accentColor = appAccentColor
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                // Vista Previa
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = t["preview"] ?: "Vista Previa",
                        color = appAccentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.width(180.dp).align(Alignment.CenterHorizontally)) {
                        ProductCard(
                            product = Product(
                                id = "preview",
                                name = productName,
                                price = productPrice.toDoubleOrNull() ?: 0.0,
                                categoryId = selectedCategoryId,
                                imageUri = productImageUri?.toString(),
                                description = productDescription,
                                contactMethod = selectedContactMethod,
                                contactPhone = contactPhone,
                                bannerType = selectedBannerType,
                                bannerText = selectedBannerText
                            ),
                            accentColor = previewAccentColor,
                            contactIcon = previewContactIcon,
                            currencySymbol = currencySymbol,
                            isPreview = true,
                            appLanguage = appLanguage
                        )
                    }
                }

                // Nombre del producto
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text(t["name"] ?: "", color = appAccentColor) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appAccentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Fila de Imagen y Precio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selector de Imagen
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSaving) { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (productImageUri != null) {
                            AsyncImage(
                                model = productImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = appAccentColor, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    if (appLanguage == "Español") "Foto" else "Photo",
                                    color = appAccentColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Precio
                    OutlinedTextField(
                        value = productPrice,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) productPrice = it },
                        label = { Text(t["price"] ?: "", color = appAccentColor) },
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appAccentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        prefix = { Text("$currencySymbol ", color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Categoría
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedCategoryName = activeCategories.find { it.first == selectedCategoryId }?.second ?: (t["select_cat"] ?: "")

                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(t["cat"] ?: "", color = appAccentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { showCategoryDropdown = true }, enabled = !isSaving) {
                                Icon(Icons.Default.ArrowDropDown, null, tint = appAccentColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appAccentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(Color(0xFF081C24))
                    ) {
                        activeCategories.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name, color = Color.White) },
                                onClick = {
                                    selectedCategoryId = id
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Contacto
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = t["contact_info"] ?: "Sales Contact",
                        color = appAccentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(t["phone"] ?: "Phone", color = appAccentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appAccentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        contactMethods.forEach { (method, iconRes) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedContactMethod == method) appAccentColor else Color.White.copy(alpha = 0.1f))
                                    .clickable(enabled = !isSaving) { selectedContactMethod = method }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = method,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.2f))
                                .clickable(enabled = !isSaving) { selectedContactMethod = null }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = t["banner_info"] ?: "Promotional Banner",
                        color = appAccentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedBannerType == "ribbon",
                            onClick = { if(!isSaving) selectedBannerType = if (selectedBannerType == "ribbon") null else "ribbon" },
                            label = { Text("Banner") },
                            leadingIcon = { Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedContainerColor = appAccentColor
                            )
                        )
                        FilterChip(
                            selected = selectedBannerType == "star",
                            onClick = { if(!isSaving) selectedBannerType = if (selectedBannerType == "star") null else "star" },
                            label = { Text(if (appLanguage == "Español") "Estrella" else "Star") },
                            leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedContainerColor = appAccentColor
                            )
                        )
                        FilterChip(
                            selected = selectedBannerType == "medal",
                            onClick = { if(!isSaving) selectedBannerType = if (selectedBannerType == "medal") null else "medal" },
                            label = { Text(if (appLanguage == "Español") "Medalla" else "Medal") },
                            leadingIcon = { Icon(Icons.Default.MilitaryTech, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedContainerColor = appAccentColor
                            )
                        )
                        FilterChip(
                            selected = selectedBannerType == "medal_ribbon",
                            onClick = { if(!isSaving) selectedBannerType = if (selectedBannerType == "medal_ribbon") null else "medal_ribbon" },
                            label = { Text(if (appLanguage == "Español") "Sello" else "Seal") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedContainerColor = appAccentColor
                            )
                        )
                    }

                    if (selectedBannerType != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedBannerText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(t["banner_text"] ?: "Phrase", color = appAccentColor) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, null, tint = appAccentColor)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color.White.copy(alpha = 0.2f),
                                    disabledLabelColor = appAccentColor,
                                    disabledTextColor = Color.White
                                )
                            )
                            Box(modifier = Modifier.matchParentSize().clickable(enabled = !isSaving) { showBannerDropdown = true })

                            DropdownMenu(
                                expanded = showBannerDropdown,
                                onDismissRequest = { showBannerDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF081C24))
                            ) {
                                bannerPhrases.forEach { phrase ->
                                    DropdownMenuItem(
                                        text = { Text(phrase, color = Color.White) },
                                        onClick = {
                                            selectedBannerText = phrase
                                            showBannerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = productDescription,
                    onValueChange = { productDescription = it },
                    label = { Text(t["desc"] ?: "", color = appAccentColor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    enabled = !isSaving,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appAccentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        if (productName.isNotEmpty() && productPrice.isNotEmpty() && selectedCategoryId.isNotEmpty()) {
                            scope.launch {
                                isSaving = true
                                val persistentUri = productImageUri?.let { uri ->
                                    if (uri.scheme == "content") {
                                        saveImageToInternalStorage(context, uri)
                                    } else {
                                        uri
                                    }
                                }

                                // Base64 ultra-optimizado (120px) para máxima compatibilidad y ligereza
                                val base64Image = persistentUri?.let { getBase64FromUri(context, it) }
                                val imageToSync = if (base64Image != null) "data:image/png;base64,$base64Image" else null

                                val product = Product(
                                    id = productId ?: UUID.randomUUID().toString(),
                                    name = productName,
                                    price = productPrice.toDoubleOrNull() ?: 0.0,
                                    categoryId = selectedCategoryId,
                                    imageUri = persistentUri?.toString(),
                                    description = productDescription,
                                    contactMethod = selectedContactMethod,
                                    contactPhone = if (contactPhone.isNotEmpty()) contactPhone else null,
                                    bannerType = selectedBannerType,
                                    bannerText = if (selectedBannerType != null) selectedBannerText else null
                                )
                                
                                // Guardado Local
                                if (productId != null) {
                                    dataStoreManager.updateProduct(product)
                                } else {
                                    dataStoreManager.addProduct(product)
                                }
                                
                                // Sincronización Remota (Base64 ultra-ligero)
                                val productForSync = product.copy(imageUri = imageToSync)
                                val result = repo.syncProduct(productForSync)
                                
                                if (result.isSuccess) {
                                    Toast.makeText(context, t["success"], Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, t["error"], Toast.LENGTH_LONG).show()
                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = appAccentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(t["saving"] ?: "SAVING...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(t["save"] ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "prod_${System.currentTimeMillis()}.jpg"
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
        val inputStream = if (uri.scheme == "content") {
            context.contentResolver.openInputStream(uri)
        } else {
            val file = File(uri.path ?: "")
            if (file.exists()) java.io.FileInputStream(file) else null
        } ?: return null
        
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        // Tamaño minificado a 120px para asegurar que NO haya problemas de longitud
        val maxDim = 360
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val width = if (aspectRatio > 1) maxDim else (maxDim * aspectRatio).toInt()
            val height = if (aspectRatio > 1) (maxDim / aspectRatio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        // Calidad 100% para PNG (sin pérdida)
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
