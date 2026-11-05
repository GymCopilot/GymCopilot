package com.zexo.gymcopilot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.Product
import com.zexo.gymcopilot.utils.decodeBase64ToBitmap
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

fun isProductInCategory(productCategoryId: String, selectedCategory: String): Boolean {
    val pCat = productCategoryId.trim().uppercase()
    val sel = selectedCategory.trim().uppercase()

    if (sel.isBlank() || sel == "ALL" || sel == "TODOS") return true

    return when {
        sel.contains("EQUIP") || sel.contains("ACCESORIO") -> {
            pCat.contains("EQUIP") || pCat.contains("ACCESORIO") || pCat == "1" || pCat.isEmpty()
        }
        sel.contains("NUTRIT") || sel.contains("SUPLEMENT") || sel.contains("NUTRICI") -> {
            pCat.contains("NUTRIT") || pCat.contains("SUPLEMENT") || pCat.contains("NUTRICI") || pCat == "2"
        }
        sel.contains("CLOTH") || sel.contains("INDUMENTARIA") || sel.contains("ROPA") -> {
            pCat.contains("CLOTH") || pCat.contains("INDUMENTARIA") || pCat.contains("ROPA") || pCat == "3"
        }
        sel.contains("WELLNESS") || sel.contains("BIENESTAR") || sel.contains("SALUD") -> {
            pCat.contains("WELLNESS") || pCat.contains("BIENESTAR") || pCat.contains("SALUD") || pCat == "4"
        }
        else -> pCat.contains(sel) || sel.contains(pCat) || pCat.isEmpty()
    }
}

@Composable
fun StoreCatalogSharedScreen(
    gymName: String = "Gym 47",
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCartClick: () -> Unit,
    cartItemCount: Int = 0,
    products: List<Product> = emptyList()
) {
    val _unusedProducts = products
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember(products) {
        val catIds = products.map { it.categoryId }.filter { it.isNotBlank() }.distinct()
        if (catIds.isEmpty()) {
            listOf(
                Triple("EQUIPMENT", "EQUIPAMIENTO Y ACCESORIOS", Color(0xFFFF5252)),
                Triple("NUTRITION", "NUTRICIÓN Y SUPLEMENTOS", Color(0xFFFFD700)),
                Triple("CLOTHING", "INDUMENTARIA DEPORTIVA", Color(0xFF7C4DFF)),
                Triple("WELLNESS", "SERVICIOS DE BIENESTAR", Color(0xFF69F0AE))
            )
        } else {
            catIds.mapIndexed { index, id ->
                val color = when (index % 4) {
                    0 -> Color(0xFFFF5252)
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFF7C4DFF)
                    else -> Color(0xFF69F0AE)
                }
                Triple(id, id.uppercase(), color)
            }
        }
    }

    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.second.contains(searchQuery, ignoreCase = true) || it.first.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = PrimaryTurquoise,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = painterResource(Res.drawable.back),
                                contentDescription = "Back",
                                modifier = Modifier.size(34.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(Color.Black)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TIENDA",
                                color = Color.Black,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = gymName,
                                color = Color.Black.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                                Icon(if (isSearchVisible) Icons.Default.Close else Icons.Default.Search, null, tint = Color.Black)
                            }
                            BadgedBox(
                                badge = {
                                    if (cartItemCount > 0) {
                                        Badge(containerColor = Color.Red, contentColor = Color.White) {
                                            Text(cartItemCount.toString())
                                        }
                                    }
                                }
                            ) {
                                IconButton(onClick = onCartClick) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar categoría...", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = PrimaryTurquoise,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredCategories) { (catId, catName, catColor) ->
                    val catIcon = when {
                        catId.contains("NUTRIT", true) || catId.contains("NUTRICI", true) || catName.contains("NUTRIT", true) || catName.contains("SUPLEMENT", true) -> Icons.Default.Restaurant
                        catId.contains("CLOTH", true) || catId.contains("INDUMENTARIA", true) || catName.contains("CLOTH", true) || catName.contains("INDUMENTARIA", true) -> Icons.Default.Checkroom
                        catId.contains("WELLNESS", true) || catId.contains("BIENESTAR", true) || catName.contains("WELLNESS", true) || catName.contains("BIENESTAR", true) -> Icons.Default.Spa
                        catId.contains("EQUIP", true) || catName.contains("EQUIP", true) || catName.contains("ACCESORIO", true) -> Icons.Default.FitnessCenter
                        else -> Icons.Default.Storefront
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(180.dp)
                            .clickable { onCategoryClick(catId) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(catColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    catIcon,
                                    contentDescription = catName,
                                    tint = Color.Black,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF121212))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catName.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
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
fun StoreCategoryProductsSharedScreen(
    categoryName: String = "EQUIPAMIENTO Y ACCESORIOS",
    categoryColor: Color = Color(0xFFFF5252),
    products: List<Product> = emptyList(),
    cartItems: List<Product> = emptyList(),
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onCategorySelected: (String, Color) -> Unit = { _, _ -> }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = categoryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = painterResource(Res.drawable.back),
                                contentDescription = "Back",
                                modifier = Modifier.size(34.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                        }

                        Text(
                            text = categoryName.uppercase(),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Search, null, tint = Color.White)
                            }
                            BadgedBox(
                                badge = {
                                    if (cartItems.isNotEmpty()) {
                                        Badge(containerColor = Color.Red, contentColor = Color.White) {
                                            Text(cartItems.size.toString())
                                        }
                                    }
                                }
                            ) {
                                IconButton(onClick = onCartClick) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Category Tabs Bar below Header (TabRow filling full width, no white frames/borders)
            val categoriesTabList = remember(products) {
                val catIds = products.map { it.categoryId }.filter { it.isNotBlank() }.distinct()
                if (catIds.isEmpty()) {
                    listOf(
                        "EQUIPAMIENTO Y ACCESORIOS" to Color(0xFFFF5252),
                        "INDUMENTARIA DEPORTIVA" to Color(0xFF7C4DFF),
                        "NUTRICIÓN Y SUPLEMENTOS" to Color(0xFFFFD700),
                        "EXTRA" to Color(0xFF9575CD)
                    )
                } else {
                    catIds.map { id ->
                        val color = when (id.uppercase()) {
                            "NUTRITION", "NUTRICION", "NUTRICIÓN Y SUPLEMENTOS" -> Color(0xFFFFD700)
                            "CLOTHING", "INDUMENTARIA", "INDUMENTARIA DEPORTIVA" -> Color(0xFF7C4DFF)
                            "WELLNESS", "BIENESTAR" -> Color(0xFF69F0AE)
                            "EXTRA" -> Color(0xFF9575CD)
                            else -> Color(0xFFFF5252)
                        }
                        id to color
                    }
                }
            }

            TabRow(
                selectedTabIndex = categoriesTabList.indexOfFirst { it.first.equals(categoryName, true) }.coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = {},
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                categoriesTabList.forEach { (catTitle, catColor) ->
                    val isSelected = categoryName.equals(catTitle, true)
                    val lum = catColor.red * 0.299f + catColor.green * 0.587f + catColor.blue * 0.114f
                    val textColor = if (lum > 0.5f) Color.Black else Color.White
                    Tab(
                        selected = isSelected,
                        onClick = { onCategorySelected(catTitle, catColor) },
                        text = {
                            Text(
                                text = catTitle,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier
                            .background(catColor)
                            .height(40.dp)
                    )
                }
            }

            // Products Grid filtered by selected category
            val displayProducts = remember(products, categoryName) {
                products.filter { p ->
                    isProductInCategory(p.categoryId, categoryName)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayProducts) { product ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                val bitmap = remember(product.imageUri) {
                                    if (!product.imageUri.isNullOrBlank()) decodeBase64ToBitmap(product.imageUri) else null
                                }
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = product.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.FitnessCenter, null, tint = PrimaryTurquoise, modifier = Modifier.size(48.dp))
                                }

                                if (!product.bannerType.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFFF9800),
                                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = product.bannerType,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = product.name,
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$${product.price}",
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val contactPainter = when (product.contactMethod?.lowercase()) {
                                            "telegram" -> painterResource(Res.drawable.telegram)
                                            "viber" -> painterResource(Res.drawable.viber)
                                            "wechat" -> painterResource(Res.drawable.wechat)
                                            "line" -> painterResource(Res.drawable.line)
                                            else -> painterResource(Res.drawable.whatsapp)
                                        }
                                        IconButton(
                                            onClick = { /* Contact action */ },
                                            modifier = Modifier.size(32.dp).background(Color.White, CircleShape)
                                        ) {
                                            Image(
                                                painter = contactPainter,
                                                contentDescription = product.contactMethod ?: "whatsapp",
                                                modifier = Modifier.size(24.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        IconButton(
                                            onClick = { onAddToCart(product) },
                                            modifier = Modifier.size(32.dp).background(categoryColor.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.AddShoppingCart, null, tint = categoryColor, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingCartSharedScreen(
    cartItems: List<Product>,
    onBack: () -> Unit,
    onRemoveItem: (Product) -> Unit,
    onCheckoutWhatsApp: () -> Unit
) {
    val totalPrice = cartItems.sumOf { item -> item.price }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(Res.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(34.dp).clip(CircleShape),
                            colorFilter = ColorFilter.tint(PrimaryTurquoise)
                        )
                    }
                    Text(
                        text = "Mi Carrito",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(34.dp))
                }
            }

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Tu carrito está vacío", color = Color.Gray, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEEEEEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bitmap = remember(item.imageUri) {
                                            if (!item.imageUri.isNullOrBlank()) decodeBase64ToBitmap(item.imageUri) else null
                                        }
                                        if (bitmap != null) {
                                            Image(bitmap = bitmap, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Default.FitnessCenter, null, tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = item.name,
                                            color = Color.Black,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "$${item.price}",
                                            color = PrimaryTurquoise,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                IconButton(onClick = { onRemoveItem(item) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            // Checkout Footer
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Estimado", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("$${totalPrice}", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = onCheckoutWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PEDIR POR WHATSAPP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
