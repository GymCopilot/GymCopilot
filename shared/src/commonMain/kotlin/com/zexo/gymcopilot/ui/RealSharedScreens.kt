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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.*
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
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
                            ) {}
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
                            RealGridButton("1", "TOTAL", painterResource(Res.drawable.ic_clients), PrimaryTurquoise, Modifier.weight(1f)) { onTabSelected(2) }
                            RealGridButton("1", "Nuevos (Mes)", painterResource(Res.drawable.ic_professors), Color(0xFFFFC107), Modifier.weight(1f)) {}
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RealGridButton("1", "DEUDORES", painterResource(Res.drawable.warning), Color.Red, Modifier.weight(1f)) { onTabSelected(2) }
                            RealGridButton("0", "ACTIVOS", painterResource(Res.drawable.person), Color(0xFF00C853), Modifier.weight(1f)) { onTabSelected(2) }
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
        2 -> RealMembersManagementView(onTabSelected = onTabSelected)
        3 -> RealProfessorsManagementView()
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
fun RealPublicStoreView(onTabSelected: (Int) -> Unit) {
    var selectedCategoryTab by remember { mutableStateOf(2) } // 2: Nutrición

    val sampleProducts = remember {
        listOf(
            StoreProductItem("1", "PROTEÍNA WHEY", "WHEY, la proteína más utilizada por los deportistas de vanguard...", "$55000.00", "Oferta relámpago", Color(0xFFFF6D00), "🥛"),
            StoreProductItem("2", "Proteína 1kg", "Ahorra con este producto por cantidad", "$180000.00", "Stock limitado", Color(0xFFFFD600), "🏋️"),
            StoreProductItem("3", "BARRA PROTEÍCA", "Barra rica en proteínas y baja en azúcares", "$3000.00", "2x1", Color(0xFFFFD600), "🍫"),
            StoreProductItem("4", "ENERGY GEL ENA", "Poderoso gel para suplemento energético", "$25000.00", "Unidades contadas", Color.Red, "⚡")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 80.dp)
    ) {
        // BARRA DE CABECERA
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "NUTRICIÓN Y SUPLEMENTOS", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
            Row {
                Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                Icon(Icons.Default.ShoppingCart, null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }
        }

        // PESTAÑAS DE CATEGORÍAS EN COLORES
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selectedCategoryTab == 0) Color(0xFFFF5252) else Color(0xFFFF8A80))
                    .clickable { selectedCategoryTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text("EQUIPAMIENTO Y ACCESORIOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selectedCategoryTab == 1) Color(0xFF7C4DFF) else Color(0xFFB388FF))
                    .clickable { selectedCategoryTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text("INDUMENTARIA DEPORTIVA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selectedCategoryTab == 2) Color(0xFFFFD600) else Color(0xFFFFE57F))
                    .clickable { selectedCategoryTab = 2 },
                contentAlignment = Alignment.Center
            ) {
                Text("NUTRICIÓN Y SUPLEMENTOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // GRILLA DE PRODUCTOS DE 2 COLUMNAS
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sampleProducts) { prod ->
                RealProductGridCard(prod)
            }
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
// VISTA REAL DE GESTIÓN DE ALUMNOS (IDÉNTICA A ANDROID)
// ============================================================================

@Composable
fun RealMembersManagementView(onTabSelected: (Int) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, bottom = 80.dp)
    ) {
        // BARRA DE TÍTULO
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Surface(
                color = PrimaryTurquoise,
                shape = CircleShape,
                modifier = Modifier.size(36.dp).clickable { onTabSelected(0) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.Black)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Gestión de Alumno", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
        }

        // BLOQUES DE FILTRO SUPERIOR
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterStatBlock("1", "TOTAL", Icons.Default.Person, PrimaryTurquoise, selectedFilter == "ALL", Modifier.weight(1f)) { selectedFilter = "ALL" }
            FilterStatBlock("0", "ACTIVOS", Icons.Default.Person, Color(0xFF00C853), selectedFilter == "ACTIVE", Modifier.weight(1f)) { selectedFilter = "ACTIVE" }
            FilterStatBlock("1", "DEUDORES", painterResource(Res.drawable.warning), Color.Red, selectedFilter == "DEBTOR", Modifier.weight(1f)) { selectedFilter = "DEBTOR" }
            FilterStatBlock("0", "DESERTORES", Icons.Default.Close, Color(0xFFFF9800), selectedFilter == "DROPOUT", Modifier.weight(1f)) { selectedFilter = "DROPOUT" }
        }

        // CAMPO DE BÚSQUEDA
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar Alumnos...", color = TextGray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTurquoise) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = Color(0xFF001F29),
                unfocusedContainerColor = Color(0xFF001F29),
                focusedBorderColor = PrimaryTurquoise,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        Text(text = "Profesores", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = "Sin profesores registrados", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

        // FILA DE ALUMNO REAL (IDÉNTICA)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF3E2723),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "R", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "r t", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 16.sp)
                        Text(text = "DEUDOR", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = PrimaryTurquoise, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Email, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = PrimaryTurquoise, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = Color.Red, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterStatBlock(
    count: String,
    label: String,
    iconPainter: Painter,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = iconPainter, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = count, fontWeight = FontWeight.Black, color = TextWhite, fontSize = 16.sp)
            Text(text = label, fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
        }
    }
}

// Overload for ImageVector
@Composable
fun FilterStatBlock(
    count: String,
    label: String,
    iconVector: ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = iconVector, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = count, fontWeight = FontWeight.Black, color = TextWhite, fontSize = 16.sp)
            Text(text = label, fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
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
