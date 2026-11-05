package com.zexo.gymcopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.*
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import gymcopilot.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

// ============================================================================
// VISTA REAL DEL PANEL ADMINISTRADOR (IDÉNTICA A ANDROID CON ICONOS VECTORIALES)
// ============================================================================

@Composable
fun RealAdminDashboardView(
    gymName: String,
    apiClient: GymKtorApiClient,
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    var showMonthlyIncome by remember { mutableStateOf(false) }
    var broadcastMsg by remember { mutableStateOf("Hoy abrimos a las 8:00 AM") }
    var isGymOpen by remember { mutableStateOf(true) }
    var showSubscriptionPlans by remember { mutableStateOf(false) }
    var plansList by remember {
        mutableStateOf(
            listOf(
                SubscriptionPlan(id = "1", name = "Mensual", price = 40.0, durationDays = 30, description = "Pase libre mensual de entrenamiento")
            )
        )
    }
    var membersList by remember {
        mutableStateOf(
            listOf(
                Member(
                    firstName = "Pedro",
                    lastName = "Picapiedras",
                    email = "pe@gmail.com",
                    phone = "123456789",
                    membershipStatus = "DEUDOR",
                    planType = "Monthly"
                )
            )
        )
    }
    var professorsList by remember {
        mutableStateOf(
            listOf(
                Professor(id = "1", firstName = "aldio", lastName = "Bonzi", email = "aldio@gmail.com", specialty = "Musculación")
            )
        )
    }
    var currentSubScreen by remember { mutableStateOf<String?>(null) } // null, "add_member", "detail_{email}", "add_professor"

    if (showSubscriptionPlans) {
        SubscriptionPlansSharedScreen(
            onBack = { showSubscriptionPlans = false },
            plans = plansList,
            onAddPlan = { newPlan -> plansList = plansList + newPlan },
            onDeletePlan = { planId -> plansList = plansList.filter { it.id != planId } }
        )
        return
    }

    if (currentSubScreen == "add_member") {
        AddMemberSharedScreen(
            onBack = { currentSubScreen = null },
            plans = plansList,
            professors = professorsList,
            onSave = { newMember -> membersList = membersList + newMember }
        )
        return
    }

    if (currentSubScreen?.startsWith("detail_") == true) {
        val email = currentSubScreen!!.substringAfter("detail_")
        val member = membersList.find { it.email == email } ?: membersList.firstOrNull()
        if (member != null) {
            MemberDetailSharedScreen(
                member = member,
                onBack = { currentSubScreen = null },
                onDelete = {
                    membersList = membersList.filter { it.email != member.email }
                    currentSubScreen = null
                },
                onUpdate = { updated ->
                    membersList = membersList.map { if (it.email == updated.email) updated else it }
                }
            )
            return
        }
    }

    if (currentSubScreen == "add_professor") {
        AddProfessorSharedScreen(
            onBack = { currentSubScreen = null },
            onSave = { newProf -> professorsList = professorsList + newProf }
        )
        return
    }

    if (currentTab == 2) {
        MembersSharedScreen(
            onBack = { onTabSelected(0) },
            members = membersList,
            professors = professorsList,
            plans = plansList,
            onAddMemberClick = { currentSubScreen = "add_member" },
            onMemberClick = { email -> currentSubScreen = "detail_$email" },
            onDeleteMember = { email -> membersList = membersList.filter { it.email != email } },
            onChatClick = { email -> }
        )
        return
    }

    if (currentTab == 3) {
        ManageProfessorsSharedScreen(
            onBack = { onTabSelected(0) },
            professors = professorsList,
            onAddProfessorClick = { currentSubScreen = "add_professor" },
            onEditProfessor = { id -> },
            onDeleteProfessor = { id -> professorsList = professorsList.filter { it.id != id } },
            onChatClick = { email -> }
        )
        return
    }

    when (currentTab) {
        0 -> { // PANEL PRINCIPAL ADMIN
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ENCABEZADO REAL CON EL LEÓN Y ESTADO
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gymName.ifBlank { "Tu Gimnasio" },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = if (isGymOpen) Color(0xFF00C853).copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { isGymOpen = !isGymOpen }
                            ) {
                                Text(
                                    text = if (isGymOpen) "ABIERTO" else "CERRADO",
                                    color = if (isGymOpen) Color(0xFF00C853) else Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "by GymCopilot", color = PrimaryTurquoise, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Panel Admin", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }

                        // LION LOGO ICON
                        Surface(
                            color = Color(0xFF00222E),
                            shape = RoundedCornerShape(20.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🦁", fontSize = 64.sp)
                            }
                        }
                    }
                }

                // CUADRO BROADCAST MENSAJE
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📢 MENSAJE", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = PrimaryTurquoise,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(28.dp).clickable { }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Send, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = broadcastMsg,
                                onValueChange = { broadcastMsg = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = PrimaryTurquoise,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // GRILLA DE BOTONES Y MÉTRICAS REALES CON ICONOS VECTORIALES EXACTOS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RealGridButton(
                                title = "PLANES",
                                subtitle = "Suscripciones",
                                iconPainter = painterResource(Res.drawable.ic_plans),
                                color = PrimaryTurquoise,
                                modifier = Modifier.weight(1f)
                            ) {
                                showSubscriptionPlans = true
                            }
                            RealGridButton(
                                title = if (showMonthlyIncome) "$35.000" else "****",
                                subtitle = "Ingresos",
                                iconPainter = painterResource(Res.drawable.ic_wallet),
                                color = Color(0xFF00C853),
                                modifier = Modifier.weight(1f),
                                trailingIconPainter = if (showMonthlyIncome) painterResource(Res.drawable.ic_visibility_on) else painterResource(Res.drawable.ic_visibility_off)
                            ) {
                                showMonthlyIncome = !showMonthlyIncome
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RealGridButton("${membersList.size}", "TOTAL", painterResource(Res.drawable.ic_clients), PrimaryTurquoise, Modifier.weight(1f)) { onTabSelected(2) }
                            RealGridButton("${professorsList.size}", "PROFESORES", painterResource(Res.drawable.ic_professors), Color(0xFFFFC107), Modifier.weight(1f)) { onTabSelected(3) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RealGridButton("${membersList.count { it.membershipStatus == "DEUDOR" }}", "DEUDORES", painterResource(Res.drawable.warning), Color.Red, Modifier.weight(1f)) { onTabSelected(2) }
                            RealGridButton("${membersList.count { it.membershipStatus == "ACTIVO" }}", "ACTIVOS", painterResource(Res.drawable.person), Color(0xFF00C853), Modifier.weight(1f)) { onTabSelected(2) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RealGridButton("0", "Asistencia Hoy", painterResource(Res.drawable.ic_fact_check), Color(0xFFAB47BC), Modifier.weight(1f)) {}
                            RealGridButton("HORARIOS", "Gestión", painterResource(Res.drawable.ic_schedule), Color(0xFFEC407A), Modifier.weight(1f)) {}
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RealGridButton("ASISTENCIA", "QR Admin", painterResource(Res.drawable.ic_qr), PrimaryTurquoise, Modifier.weight(1f)) {}
                            RealGridButton("SCAN", "Asistencia", painterResource(Res.drawable.ic_scan), PrimaryTurquoise, Modifier.weight(1f)) {}
                        }
                    }
                }
            }
        }
        1 -> RealPublicStoreView(onTabSelected = onTabSelected)
        2 -> MembersSharedScreen(
            onBack = { onTabSelected(0) },
            members = membersList,
            professors = professorsList,
            plans = plansList,
            onAddMemberClick = { currentSubScreen = "add_member" },
            onMemberClick = { email -> currentSubScreen = "detail_$email" },
            onDeleteMember = { email -> membersList = membersList.filter { it.email != email } },
            onChatClick = { email -> }
        )
        3 -> ManageProfessorsSharedScreen(
            onBack = { onTabSelected(0) },
            professors = professorsList,
            onAddProfessorClick = { currentSubScreen = "add_professor" },
            onEditProfessor = { id -> },
            onDeleteProfessor = { id -> professorsList = professorsList.filter { it.id != id } },
            onChatClick = { email -> }
        )
        else -> FullSettingsSection()
    }
}

@Composable
fun RealGridButton(
    title: String,
    subtitle: String,
    iconPainter: Painter,
    color: Color,
    modifier: Modifier = Modifier,
    trailingIconPainter: Painter? = null,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painter = iconPainter, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                    Text(text = subtitle, fontSize = 10.sp, color = TextGray)
                }
            }

            if (trailingIconPainter != null) {
                Icon(
                    painter = trailingIconPainter,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================================
// VISTA REAL DE LA TIENDA DE NUTRICIÓN Y SUPLEMENTOS (IDÉNTICA A ANDROID)
// ============================================================================

@Composable
fun RealPublicStoreView(apiClient: GymKtorApiClient = GymKtorApiClient, onTabSelected: (Int) -> Unit) {
    var currentScreen by remember { mutableStateOf("CATALOG") }
    var selectedCategoryName by remember { mutableStateOf("EQUIPAMIENTO Y ACCESORIOS") }
    var selectedCategoryColor by remember { mutableStateOf(Color(0xFFFF5252)) }
    val cartItems = remember { mutableStateListOf<Product>() }
    var productsList by remember {
        mutableStateOf(
            listOf(
                Product(id = "1", name = "Proteinaaaaaaaaa", price = 12000.0, categoryId = "EQUIPAMIENTO", description = "Proteína en polvo", bannerType = "2x1"),
                Product(id = "2", name = "otra proteinaeina", price = 7888.0, categoryId = "NUTRITION", description = "Whey protein 100% ENA", bannerType = "Tiempo limitado")
            )
        )
    }

    LaunchedEffect(Unit) {
        try {
            val localProfile = com.zexo.gymcopilot.utils.readLocalMemberProfile()
            val gymUrl = localProfile?.gymApiUrl.orEmpty().ifBlank { "https://script.google.com/macros/s/AKfycbzXYYMu43-ATeyxQmwdF3CuRYXi7KbpChyTQEBFIsWh7SCHv2XugxsWen61oBeDdzw/exec" }
            val gymName = localProfile?.gymName.orEmpty().ifBlank { "Gym 47" }
            val gymIdVal = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = localProfile?.spreadsheetId.orEmpty()
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val storeUrl = "$gymUrl${paramChar}type=store&gymId=$gymIdVal$ssParam"

            val responseText = apiClient.getData(storeUrl)
            if (responseText.isNotBlank() && responseText.contains("success")) {
                val jsonElement = apiClient.jsonInstance.parseToJsonElement(responseText)
                val jsonObject = jsonElement as? JsonObject
                val dataArray = jsonObject?.get("data") as? JsonArray
                if (dataArray != null && dataArray.size > 1) {
                    val remoteProducts = mutableListOf<Product>()
                    for (i in 1 until dataArray.size) {
                        val row = dataArray[i] as? JsonArray
                        if (row != null && row.size >= 3) {
                            val id = row[0].jsonPrimitive.content
                            val name = row[1].jsonPrimitive.content
                            val price = row[2].jsonPrimitive.content.toDoubleOrNull() ?: 0.0
                            val catId = if (row.size > 3) row[3].jsonPrimitive.content else ""
                            val imageUri = if (row.size > 4 && row[4].jsonPrimitive.content != "null") row[4].jsonPrimitive.content else null
                            val desc = if (row.size > 5 && row[5].jsonPrimitive.content != "null") row[5].jsonPrimitive.content else ""
                            val bannerType = if (row.size > 8 && row[8].jsonPrimitive.content != "null") row[8].jsonPrimitive.content else null

                            remoteProducts.add(
                                Product(
                                    id = id,
                                    name = name,
                                    price = price,
                                    categoryId = catId,
                                    imageUri = imageUri,
                                    description = desc,
                                    bannerType = bannerType
                                )
                            )
                        }
                    }
                    if (remoteProducts.isNotEmpty()) {
                        productsList = remoteProducts
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    when (currentScreen) {
        "CART" -> {
            ShoppingCartSharedScreen(
                cartItems = cartItems,
                onBack = { currentScreen = "CATALOG" },
                onRemoveItem = { cartItems.remove(it) },
                onCheckoutWhatsApp = {
                    cartItems.clear()
                    currentScreen = "CATALOG"
                }
            )
        }
        "CATEGORY" -> {
            StoreCategoryProductsSharedScreen(
                categoryName = selectedCategoryName,
                categoryColor = selectedCategoryColor,
                products = productsList,
                cartItems = cartItems,
                onBack = { currentScreen = "CATALOG" },
                onCartClick = { currentScreen = "CART" },
                onAddToCart = { prod -> cartItems.add(prod) },
                onCategorySelected = { name, color ->
                    selectedCategoryName = name
                    selectedCategoryColor = color
                }
            )
        }
        else -> {
            StoreCatalogSharedScreen(
                gymName = "GYM 47",
                onBack = { onTabSelected(0) },
                onCategoryClick = { catId ->
                    when (catId) {
                        "NUTRITION" -> {
                            selectedCategoryName = "NUTRICIÓN Y SUPLEMENTOS"
                            selectedCategoryColor = Color(0xFFFFD700)
                        }
                        "CLOTHING" -> {
                            selectedCategoryName = "INDUMENTARIA DEPORTIVA"
                            selectedCategoryColor = Color(0xFF7C4DFF)
                        }
                        "WELLNESS" -> {
                            selectedCategoryName = "SERVICIOS DE BIENESTAR"
                            selectedCategoryColor = Color(0xFF69F0AE)
                        }
                        else -> {
                            selectedCategoryName = "EQUIPAMIENTO Y ACCESORIOS"
                            selectedCategoryColor = Color(0xFFFF5252)
                        }
                    }
                    currentScreen = "CATEGORY"
                },
                onCartClick = { currentScreen = "CART" },
                cartItemCount = cartItems.size,
                products = productsList
            )
        }
    }
}

data class StoreProductItem(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val tagText: String,
    val tagColor: Color,
    val emojiImage: String
)

@Composable
fun RealProductGridCard(product: StoreProductItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = Modifier.fillMaxWidth().height(280.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                // TAG BANNER DE OFERTA
                Surface(
                    color = product.tagColor,
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.tagText,
                        color = if (product.tagColor == Color(0xFFFFD600)) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // ICONOS EDITAR / BORRAR
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(color = Color.Black, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Text(text = product.emojiImage, fontSize = 54.sp)
            }

            Column(
                modifier = Modifier.padding(10.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = product.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = product.price, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.Black)
                    Row {
                        Surface(color = Color(0xFF25D366), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = Color(0xFFFFD600), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingCart, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// GESTIÓN DE PROFESORES REAL
// ============================================================================

@Composable
fun RealProfessorsManagementView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(text = "Profesores / Entrenadores", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            MemberCard("Prof. Gabriel Fernández", "Musculación & Personalizado", "ACTIVO", "Hoy 07:00 AM", true)
            MemberCard("Prof. Laura Benítez", "Yoga & Mobility", "ACTIVO", "Hoy 09:30 AM", true)
            MemberCard("Prof. Roberto Carlos", "Crossfit & Funcional", "ACTIVO", "Ayer 17:00 PM", true)
        }
    }
}
