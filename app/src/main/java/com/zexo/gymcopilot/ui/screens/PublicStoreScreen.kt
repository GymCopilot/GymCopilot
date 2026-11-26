package com.zexo.gymcopilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.getGymFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PublicStoreScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onClassesClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isMember: Boolean = false,
    isProfessor: Boolean = false
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "Español")
    val selectedCategoryIds by dataStoreManager.getStoreCategories().collectAsState(initial = emptySet())
    val customCategoryIds by dataStoreManager.getCustomCategoryIds().collectAsState(initial = emptySet())
    val gymName by dataStoreManager.getGymName().collectAsState(initial = "")
    val gymNameFont by dataStoreManager.getGymNameFont().collectAsState(initial = "Default")
    val gymLogoUri by dataStoreManager.getGymLogoUri().collectAsState(initial = null)
    val storeAnnouncement by dataStoreManager.getStoreAnnouncement().collectAsState(initial = "")
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

    LaunchedEffect(gymApiUrl) {
        if (gymApiUrl.isBlank()) return@LaunchedEffect
        val gymNameVal = dataStoreManager.getGymName().first()
        val gymIdVal = gymNameVal.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val emailVal = dataStoreManager.getUserEmail().first()
        val ssIdVal = dataStoreManager.getGymSpreadsheetId().first()
        val paramChar = if (gymApiUrl.contains("?")) "&" else "?"
        val ssParam = if (ssIdVal.isNotBlank()) "&spreadsheetId=$ssIdVal" else ""
        val storeSyncUrl = "$gymApiUrl${paramChar}type=store&gymId=$gymIdVal&email=${java.net.URLEncoder.encode(emailVal, "UTF-8")}$ssParam"
        val apiServiceGet = NetworkModule.getApiServiceForGet(gymApiUrl)

        while (isActive) {
            try {
                val storeResponse = apiServiceGet.getStoreRaw(storeSyncUrl, "Bearer session_active")
                if (storeResponse.isSuccessful) {
                    val body = storeResponse.body()
                    val rows = when {
                        body?.isJsonArray == true -> body.asJsonArray
                        body?.isJsonObject == true -> body.asJsonObject.getAsJsonArray("data")
                        else -> null
                    }
                    rows?.let {
                        val remoteProducts = mutableListOf<Product>()
                        val remoteCategoryIds = mutableSetOf<String>()
                        
                        for (i in 1 until it.size()) {
                            val arr = it.get(i).asJsonArray
                            if (arr.size() >= 3) {
                                val catId = if (arr.size() > 3) arr.get(3).asString else ""
                                remoteProducts.add(Product(
                                    id = arr.get(0).asString,
                                    name = arr.get(1).asString,
                                    price = arr.get(2).asString.toDoubleOrNull() ?: 0.0,
                                    categoryId = catId,
                                    imageUri = if (arr.size() > 4 && !arr.get(4).isJsonNull) arr.get(4).asString else null,
                                    description = if (arr.size() > 5 && !arr.get(5).isJsonNull) arr.get(5).asString else "",
                                    contactMethod = if (arr.size() > 6 && !arr.get(6).isJsonNull) arr.get(6).asString else null,
                                    contactPhone = if (arr.size() > 7 && !arr.get(7).isJsonNull) arr.get(7).asString else null,
                                    bannerType = if (arr.size() > 8 && !arr.get(8).isJsonNull) arr.get(8).asString else null,
                                    bannerText = if (arr.size() > 9 && !arr.get(9).isJsonNull) arr.get(9).asString else null
                                ))
                                if (catId.isNotEmpty()) remoteCategoryIds.add(catId)
                            }
                        }
                        val currentLocal = dataStoreManager.getProducts().first()
                        val merged = (currentLocal + remoteProducts).distinctBy { it.id }
                        dataStoreManager.saveAllProducts(merged)
                        val currentActive = dataStoreManager.getStoreCategories().first()
                        val newActive = currentActive.toMutableSet()
                        var changed = false
                        remoteCategoryIds.forEach { id -> if (newActive.add(id)) changed = true }
                        if (changed) dataStoreManager.setStoreCategories(newActive)
                    }
                }
            } catch (_: Exception) {}
            delay(10000)
        }
    }

    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            delay(300) // Esperar un poco a que la animación inicie
            focusRequester.requestFocus()
        }
    }
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val appAccentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val contentColor = if (appAccentColor.luminance() > 0.5f) Color.Black else Color.White
    val cartItems by dataStoreManager.getCartItems().collectAsState(initial = emptyList())

    val allActiveCategoryIds = remember(selectedCategoryIds, customCategoryIds) {
        val fixed = GymCategory.entries.filter { it.id in selectedCategoryIds }.map { it.id }
        val custom = customCategoryIds.filter { it in selectedCategoryIds }
        (fixed + custom).distinct()
    }

    val selectedFontFamily = remember(gymNameFont) { getGymFontFamily(gymNameFont) }
    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf("store" to "TIENDA", "search" to "Buscar categoría...")
            else -> mapOf("store" to "STORE", "search" to "Search category...")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = t["store"] ?: "STORE", color = contentColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text(text = gymName, color = contentColor.copy(alpha = 0.8f), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, fontFamily = selectedFontFamily, textAlign = TextAlign.Center)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(34.dp).clip(CircleShape), colorFilter = ColorFilter.tint(contentColor))
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) searchQuery = ""
                        }) {
                            Icon(if (isSearchVisible) Icons.Default.Close else Icons.Default.Search, null, tint = contentColor)
                        }
                        BadgedBox(
                            badge = {
                                if (cartItems.isNotEmpty()) {
                                    Badge(containerColor = Color.Red, contentColor = Color.White) {
                                        Text(cartItems.size.toString())
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = onCartClick) {
                                Icon(Icons.Default.ShoppingCart, null, tint = contentColor)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = appAccentColor)
                )
            },
            bottomBar = {
                when {
                    isProfessor -> ProfessorBottomNavigation(currentRoute = "store", onHomeClick = onHomeClick, onStoreClick = onStoreClick, onScheduleClick = onScheduleClick, onMembersClick = onMembersClick, onRoutinesClick = onRoutinesClick, onChatClick = onChatClick, accentColor = appAccentColor, unreadCount = unreadCount)
                    isMember -> MemberBottomNavigation(currentRoute = "member_store", onHomeClick = onHomeClick, onStoreClick = onStoreClick, onClassesClick = onClassesClick, onRoutinesClick = onRoutinesClick, onChatClick = onChatClick, accentColor = appAccentColor, unreadCount = unreadCount)
                    else -> AdminBottomNavigation(currentRoute = "store", onHomeClick = onHomeClick, onStoreClick = onStoreClick, onMembersClick = onMembersClick, onProfessorsClick = onProfessorsClick, onSettingsClick = onSettingsClick, accentColor = appAccentColor, unreadCount = unreadCount)
                }
            },
            containerColor = Color.White
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (storeAnnouncement.isNotEmpty()) {
                    ScrollingAnnouncementBanner(text = storeAnnouncement, backgroundColor = Color.Black, textColor = Color.White)
                }

                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically(animationSpec = tween(1200)) + fadeIn(animationSpec = tween(1200)),
                    exit = shrinkVertically(animationSpec = tween(1200)) + fadeOut(animationSpec = tween(1200))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(t["search"] ?: "") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
                        } else null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = appAccentColor,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (allActiveCategoryIds.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = appAccentColor)
                    }
                } else {
                    val filteredCategories = allActiveCategoryIds.filter { id ->
                        val fixedCat = GymCategory.entries.find { it.id == id }
                        val displayName = if (fixedCat != null) getCategoryTranslation(fixedCat, appLanguage) else "Extra"
                        displayName.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredCategories.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(if (appLanguage == "Español") "No se encontraron categorías" else "No categories found", color = Color.Gray)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                        ) {
                            items(filteredCategories) { categoryId ->
                                val categoryUri by dataStoreManager.getCategoryUri(categoryId).collectAsState(initial = null)
                                val categorySavedColor by dataStoreManager.getCategoryColor(categoryId).collectAsState(initial = null)
                                val customName by dataStoreManager.getCategoryName(categoryId).collectAsState(initial = null)
                                val fixedCat = GymCategory.entries.find { it.id == categoryId }
                                val accentColor = if (categorySavedColor != null) Color(categorySavedColor!!) else fixedCat?.bgColor ?: Color(0xFF9575CD)
                                val displayName = if (!customName.isNullOrEmpty()) customName!! else if (fixedCat != null) getCategoryTranslation(fixedCat, appLanguage) else "Extra"
                                
                                PublicCategoryCard(
                                    displayName = displayName,
                                    accentColor = accentColor,
                                    customUri = categoryUri,
                                    defaultIconRes = fixedCat?.defaultIconRes ?: R.drawable.gymcopilot,
                                    onClick = { onCategoryClick(categoryId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicCategoryCard(
    displayName: String,
    accentColor: Color,
    customUri: String?,
    defaultIconRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.8f)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                if (customUri != null) {
                    AsyncImage(
                        model = customUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = defaultIconRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.75f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}
