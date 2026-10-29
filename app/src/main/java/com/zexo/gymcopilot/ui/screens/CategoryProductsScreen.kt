package com.zexo.gymcopilot.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.gson.JsonArray
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryProductsScreen(
    categoryId: String,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onEditProduct: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onClassesClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    isAdmin: Boolean = true,
    isProfessor: Boolean = false
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val appCurrencySelection by dataStoreManager.getAppCurrency().collectAsState(initial = "Peso Argentino")
    val currencySymbol = remember(appCurrencySelection) { CurrencyData.getSymbol(appCurrencySelection) }

    val categoryName by dataStoreManager.getCategoryName(categoryId).collectAsState(initial = null)
    val categoryColorInt by dataStoreManager.getCategoryColor(categoryId).collectAsState(initial = null)
    val globalContactMethod by dataStoreManager.getPreferredContactMethod().collectAsState(initial = "whatsapp")
    val globalContactPhone by dataStoreManager.getContactPhone().collectAsState(initial = "")
    val cartItems by dataStoreManager.getCartItems().collectAsState(initial = emptyList())
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")

    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val myID = remember(userName, userEmail, userRole) {
        val role = userRole?.lowercase() ?: ""
        if (role == "admin") "Admin" else userEmail.ifBlank { userName }.trim()
    }

    var unreadCount by remember { mutableIntStateOf(0) }

    // Sincronización de Chats y Tienda
    LaunchedEffect(gymApiUrl, myID, isProfessor) {
        if (!isProfessor || gymApiUrl.isBlank()) return@LaunchedEffect
        val gymNameVal = dataStoreManager.getGymName().first()
        val gymIdVal = gymNameVal.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val emailVal = dataStoreManager.getUserEmail().first()
        val ssIdVal = dataStoreManager.getGymSpreadsheetId().first()
        val paramChar = if (gymApiUrl.contains("?")) "&" else "?"
        val ssParam = if (ssIdVal.isNotBlank()) "&spreadsheetId=$ssIdVal" else ""
        val syncUrl = "$gymApiUrl${paramChar}type=chats&gymId=$gymIdVal&email=${java.net.URLEncoder.encode(emailVal, "UTF-8")}$ssParam"
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)
        while (isActive) {
            try {
                val response = apiServiceGet.getChatsRaw(syncUrl, "Bearer session_active")
                if (response.isSuccessful) {
                    val jsonElement = response.body()
                    val rows = when {
                        jsonElement?.isJsonArray == true -> jsonElement.asJsonArray
                        jsonElement?.isJsonObject == true -> jsonElement.asJsonObject.getAsJsonArray("data") ?: JsonArray()
                        else -> JsonArray()
                    }
                    var lastGeneralTs = 0L
                    var hasIndividualUnread = false
                    for (i in 1 until rows.size()) {
                        val arr = rows.get(i).asJsonArray
                        if (arr.size() >= 4) {
                            val sender = arr.get(1).asString
                            val recipient = arr.get(2).asString.lowercase().trim()
                            val tsStr = if (arr.size() > 4) arr.get(4).asString else "0"
                            val ts = tsStr.toDoubleOrNull()?.toLong() ?: tsStr.toLongOrNull() ?: 0L
                            if (recipient.contains("chat general")) {
                                if (ts > lastGeneralTs) lastGeneralTs = ts
                            } else if (recipient == myID.lowercase().trim() && sender.lowercase().trim() != myID.lowercase().trim()) {
                                val sortedIds = listOf(myID.lowercase().trim(), sender.lowercase().trim()).sorted()
                                val chatKey = "${sortedIds[0]}_${sortedIds[1]}".replace(" ", "_")
                                val lastRead = dataStoreManager.getLastReadTimestamp(chatKey).first()
                                if (ts > lastRead) hasIndividualUnread = true
                            }
                        }
                    }
                    val lastReadGeneral = dataStoreManager.getLastReadTimestamp("chat_general_global").first()
                    unreadCount = if (lastGeneralTs > lastReadGeneral || hasIndividualUnread) 1 else 0
                }
            } catch (e: Exception) { }
            delay(5000)
        }
    }

    LaunchedEffect(gymApiUrl) {
        if (gymApiUrl.isBlank()) return@LaunchedEffect
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)
        val syncUrl = if (gymApiUrl.contains("?")) "$gymApiUrl&type=store" else "$gymApiUrl?type=store"
        while (isActive) {
            try {
                val response = apiServiceGet.getStoreRaw(syncUrl, "Bearer session_active")
                if (response.isSuccessful) {
                    val jsonElement = response.body()
                    val rows = when {
                        jsonElement?.isJsonArray == true -> jsonElement.asJsonArray
                        jsonElement?.isJsonObject == true -> jsonElement.asJsonObject.getAsJsonArray("data") ?: JsonArray()
                        else -> JsonArray()
                    }
                    val remoteProducts = mutableListOf<Product>()
                    for (i in 1 until rows.size()) {
                        val arr = rows.get(i).asJsonArray
                        if (arr.size() >= 3) {
                            remoteProducts.add(Product(
                                id = arr.get(0).asString,
                                name = arr.get(1).asString,
                                price = arr.get(2).asString.toDoubleOrNull() ?: 0.0,
                                categoryId = if (arr.size() > 3) arr.get(3).asString else "",
                                imageUri = if (arr.size() > 4 && !arr.get(4).isJsonNull) arr.get(4).asString else null,
                                description = if (arr.size() > 5 && !arr.get(5).isJsonNull) arr.get(5).asString else "",
                                contactMethod = if (arr.size() > 6 && !arr.get(6).isJsonNull) arr.get(6).asString else null,
                                contactPhone = if (arr.size() > 7 && !arr.get(7).isJsonNull) arr.get(7).asString else null,
                                bannerType = if (arr.size() > 8 && !arr.get(8).isJsonNull) arr.get(8).asString else null,
                                bannerText = if (arr.size() > 9 && !arr.get(9).isJsonNull) arr.get(9).asString else null
                            ))
                        }
                    }
                    if (remoteProducts.isNotEmpty()) {
                        val currentLocal = dataStoreManager.getProducts().first()
                        val merged = (currentLocal + remoteProducts).distinctBy { it.id }
                        dataStoreManager.saveAllProducts(merged)
                    }
                }
            } catch (e: Exception) { }
            delay(5000)
        }
    }

    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    val selectedCategoryIds by dataStoreManager.getStoreCategories().collectAsState(initial = emptySet())
    val customCategoryIds by dataStoreManager.getCustomCategoryIds().collectAsState(initial = emptySet())
    val allActiveCategoryIds = remember(selectedCategoryIds, customCategoryIds) {
        val fixed = GymCategory.entries.filter { it.id in selectedCategoryIds }.map { it.id }
        val custom = customCategoryIds.filter { it in selectedCategoryIds }
        fixed + custom
    }

    val fixedCat = GymCategory.entries.find { it.id == categoryId }
    val accentColor = if (categoryColorInt != null) Color(categoryColorInt!!) else fixedCat?.bgColor ?: Color(0xFF9575CD)
    val displayName = if (!categoryName.isNullOrEmpty()) categoryName!! else if (fixedCat != null) getCategoryTranslation(fixedCat, appLanguage) else "Extra"
    val contentColor = if (accentColor.luminance() > 0.5f) Color.Black else Color.White

    var productToDelete by remember { mutableStateOf<Product?>(null) }
    val allProducts by dataStoreManager.getProducts().collectAsState(initial = emptyList())
    val products = remember(allProducts, categoryId, searchQuery) {
        allProducts.filter { it.categoryId == categoryId && it.name.contains(searchQuery, ignoreCase = true) }
    }

    val attendanceRepository = remember(gymApiUrl) {
        com.zexo.gymcopilot.repository.AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(if(appLanguage == "Español") "Eliminar Producto" else "Delete Product") },
            text = { Text(if(appLanguage == "Español") "¿Estás seguro de que deseas eliminar ${productToDelete?.name}?" else "Are you sure you want to delete ${productToDelete?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    val prod = productToDelete!!
                    scope.launch {
                        dataStoreManager.deleteProduct(prod.id)
                        attendanceRepository.syncProduct(prod, isDelete = true)
                        productToDelete = null
                    }
                }) {
                    Text(if(appLanguage == "Español") "Eliminar" else "Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text(if(appLanguage == "Español") "Cancelar" else "Cancel") }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = displayName.uppercase(), color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(30.dp).clip(CircleShape), colorFilter = ColorFilter.tint(contentColor))
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchVisible = !isSearchVisible; if (!isSearchVisible) searchQuery = "" }) {
                            Icon(if (isSearchVisible) Icons.Default.Close else Icons.Default.Search, null, tint = contentColor)
                        }
                        BadgedBox(
                            badge = { if (cartItems.isNotEmpty()) Badge(containerColor = Color.Red, contentColor = Color.White) { Text(cartItems.size.toString()) } },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = onCartClick) { Icon(Icons.Default.ShoppingCart, null, tint = contentColor) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = accentColor)
                )
            },
            bottomBar = {
                when {
                    isProfessor -> ProfessorBottomNavigation(currentRoute = "store", onHomeClick = onHomeClick, onStoreClick = onStoreClick, onScheduleClick = onScheduleClick, onMembersClick = onMembersClick, onRoutinesClick = onRoutinesClick, onChatClick = onChatClick, accentColor = accentColor, unreadCount = unreadCount)
                    isAdmin -> AdminBottomNavigation(currentRoute = "store", onHomeClick = onHomeClick, onStoreClick = onStoreClick, onMembersClick = onMembersClick, onProfessorsClick = onProfessorsClick, onSettingsClick = onSettingsClick, accentColor = accentColor)
                    else -> MemberBottomNavigation(currentRoute = "member_store", onHomeClick = onHomeClick, onStoreClick = onStoreClick, onClassesClick = onClassesClick, onRoutinesClick = onRoutinesClick, onChatClick = onChatClick, accentColor = accentColor)
                }
            },
            containerColor = Color.White
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                InfiniteCategoryBanner(allActiveCategoryIds = allActiveCategoryIds.toList(), appLanguage = appLanguage, dataStoreManager = dataStoreManager, onCategoryClick = onCategoryClick)

                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically(animationSpec = tween(1000)) + fadeIn(animationSpec = tween(1000)),
                    exit = shrinkVertically(animationSpec = tween(1000)) + fadeOut(animationSpec = tween(1000))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(if (appLanguage == "Español") "Buscar producto..." else "Search product...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
                        } else null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = if (appLanguage == "Español") "No hay productos coincidentes" else "No matching products", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(products) { product ->
                            val method = product.contactMethod ?: globalContactMethod
                            val phone = product.contactPhone ?: globalContactPhone
                            val contactIcon = when (method) {
                                "whatsapp" -> R.drawable.whatsapp
                                "telegram" -> R.drawable.telegram
                                else -> R.drawable.whatsapp
                            }
                            ProductCard(
                                product = product,
                                accentColor = accentColor,
                                contactIcon = contactIcon,
                                currencySymbol = currencySymbol,
                                onAddToCart = { scope.launch { dataStoreManager.addToCart(product) } },
                                onContactClick = {
                                    val intent = when (method) {
                                        "whatsapp" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone"))
                                        "telegram" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+${phone.removePrefix("+")}"))
                                        else -> Intent(Intent.ACTION_VIEW, Uri.parse("tel:$phone"))
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) {}
                                },
                                onEditProduct = onEditProduct,
                                onDeleteClick = { productToDelete = product },
                                appLanguage = appLanguage,
                                isAdmin = isAdmin
                            )
                        }
                    }
                }
            }
        }
    }
}

private val StarBannerShape = GenericShape { size, _ ->
    val points = 40
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.width / 2f
    val minRadius = maxRadius * 0.85f
    val angleStep = (2 * Math.PI / (points * 2)).toFloat()
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) maxRadius else minRadius
        val angle = i * angleStep - (Math.PI / 2).toFloat()
        val x = center.x + radius * cos(angle.toDouble()).toFloat()
        val y = center.y + radius * sin(angle.toDouble()).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val FivePointedStarShape = GenericShape { size, _ ->
    val points = 5
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.width / 2f
    val minRadius = maxRadius * 0.45f
    val angleStep = (Math.PI / points).toFloat()
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) maxRadius else minRadius
        val angle = i * angleStep - (Math.PI / 2).toFloat()
        val x = center.x + radius * cos(angle.toDouble()).toFloat()
        val y = center.y + radius * sin(angle.toDouble()).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val BannerPointedShape = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width * 0.88f, 0f); lineTo(size.width, size.height / 2f); lineTo(size.width * 0.88f, size.height); lineTo(0f, size.height); close()
}

private val RibbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(size.width / 2f, size.height * 0.75f); lineTo(0f, size.height); close()
}

@Composable
fun ProductCard(
    product: Product,
    accentColor: Color,
    contactIcon: Int,
    currencySymbol: String = "$",
    onAddToCart: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onEditProduct: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isPreview: Boolean = false,
    appLanguage: String = "English",
    isAdmin: Boolean = true
) {
    val translatedBannerText = remember(product.bannerText, appLanguage) { BannerTranslations.translate(product.bannerText, appLanguage) }
    val imageModel = remember(product.imageUri) {
        val uri = product.imageUri
        if (uri != null && uri.startsWith("data:image")) {
            try { val base64String = uri.substringAfter("base64,"); Base64.decode(base64String, Base64.DEFAULT) } catch (e: Exception) { uri }
        } else uri
    }

    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, Color.Black, RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)) // Fondo gris claro para los datos (Column de abajo)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentAlignment = Alignment.Center) {
                if (product.imageUri != null) {
                    AsyncImage(model = imageModel, contentDescription = product.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                }
                if (product.bannerType != null && !translatedBannerText.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                        when (product.bannerType) {
                            "ribbon" -> {
                                Box(modifier = Modifier.align(Alignment.TopStart).width(140.dp).offset(x = (-35).dp, y = 22.dp).rotate(-45f).background(Color.Red).padding(horizontal = 24.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                    Text(text = translatedBannerText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                                }
                            }
                            "star" -> RosetteBanner(translatedBannerText, hasRibbons = false, isStar = true)
                            "medal" -> RosetteBanner(translatedBannerText, hasRibbons = true)
                            "medal_ribbon" -> {
                                Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp).background(Color(0xFFFF9800), BannerPointedShape).padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp).widthIn(min = 70.dp, max = 120.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(text = translatedBannerText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Start, lineHeight = 12.sp, maxLines = 2, softWrap = true)
                                }
                            }
                        }
                    }
                }
                if (!isPreview) {
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isAdmin) {
                            Surface(modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onDeleteClick() }, color = Color.Red.copy(alpha = 0.8f)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.White) }
                            }
                            Surface(modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onEditProduct(product.id) }, color = Color.White.copy(alpha = 0.9f)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.Black) }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = product.name.ifEmpty { "Nombre del Producto" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2, lineHeight = 18.sp, color = Color.Black)
                if (product.description.isNotEmpty()) {
                    Text(text = product.description, fontSize = 11.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "$currencySymbol${String.format("%.2f", product.price)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(34.dp).clip(CircleShape).clickable { onContactClick() }, color = Color(0xFFE6E6E6)) {
                            Box(contentAlignment = Alignment.Center) { Image(painter = painterResource(id = contactIcon), contentDescription = "Contact", modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(modifier = Modifier.size(34.dp).clip(CircleShape).clickable { onAddToCart() }, color = accentColor) {
                            Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, tint = if (accentColor.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RosetteBanner(text: String, hasRibbons: Boolean, isStar: Boolean = false) {
    Box(modifier = Modifier.padding(8.dp).size(60.dp), contentAlignment = Alignment.TopStart) {
        Box(contentAlignment = Alignment.Center) {
            if (hasRibbons) {
                Row(modifier = Modifier.offset(y = 20.dp), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    Box(modifier = Modifier.size(width = 18.dp, height = 32.dp).rotate(15f).background(Color(0xFFB71C1C), RibbonShape))
                    Box(modifier = Modifier.size(width = 18.dp, height = 32.dp).rotate(-15f).background(Color(0xFFB71C1C), RibbonShape))
                }
            }
            if (!isStar) Box(modifier = Modifier.size(56.dp).background(Color(0xFFD32F2F), StarBannerShape))
            Box(modifier = Modifier.size(if (isStar) 56.dp else 46.dp).background(Color(0xFFFFD700), if (isStar) FivePointedStarShape else StarBannerShape), contentAlignment = Alignment.Center) {
                Text(text = text, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.width(if (isStar) 42.dp else 36.dp), lineHeight = 9.sp, maxLines = 2, softWrap = true)
            }
        }
    }
}
