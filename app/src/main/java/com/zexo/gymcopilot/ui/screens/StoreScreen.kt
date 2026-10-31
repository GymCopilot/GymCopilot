package com.zexo.gymcopilot.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.LocalButtonBackgroundColor
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextGray
import com.zexo.gymcopilot.ui.theme.TextWhite
import com.zexo.gymcopilot.ui.theme.getButtonStyleShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAddCategoryClick: () -> Unit,
    onViewPublicStoreClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val selectedCategoryIds by dataStoreManager.getStoreCategories().collectAsState(initial = emptySet())
    val customCategoryIds by dataStoreManager.getCustomCategoryIds().collectAsState(initial = emptySet())
    val lastSelectedId by dataStoreManager.getLastSelectedCategory().collectAsState(initial = null)
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val appAccentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val buttonShape = getButtonStyleShape(buttonStyle)

    val activeCategories = remember(selectedCategoryIds, customCategoryIds) {
        val fixed = GymCategory.entries.filter { it.id in selectedCategoryIds }
        val custom = customCategoryIds.filter { it in selectedCategoryIds }
        fixed.map { CategoryDisplayData(it.id, it.bgColor, it.defaultIconRes) } +
                custom.map { CategoryDisplayData(it, Color(0xFF9575CD), R.drawable.gymcopilot, true) }
    }

    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to "Configuración Tienda",
                "add_cat" to "Gestionar Categorías",
                "add_prod" to "Añadir Producto",
                "view_store" to "Ver Tienda Pública",
                "active_cats" to "Categorías Activas",
                "select_info" to "Toca una categoría abajo para editar"
            )
            else -> mapOf(
                "title" to "Store Config",
                "add_cat" to "Manage Categories",
                "add_prod" to "Add Product",
                "view_store" to "View Public Store",
                "active_cats" to "Active Categories",
                "select_info" to "Tap a category below to edit"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00151C))) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t["title"] ?: "", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = painterResource(id = R.drawable.back),
                                contentDescription = "Back",
                                modifier = Modifier.size(34.dp).clip(CircleShape),
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 100.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StoreConfigButton(
                            text = t["add_cat"] ?: "",
                            icon = Icons.Default.Category,
                            modifier = Modifier.weight(1f),
                            accentColor = appAccentColor,
                            shape = buttonShape,
                            onClick = onAddCategoryClick
                        )
                        StoreConfigButton(
                            text = t["add_prod"] ?: "",
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f),
                            accentColor = appAccentColor,
                            shape = buttonShape,
                            onClick = onAddProductClick
                        )
                    }
                }

                item {
                    val buttonBgColor = LocalButtonBackgroundColor.current
                    val contentColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .clip(buttonShape)
                            .clickable { onViewPublicStoreClick() }
                            .border(1.5.dp, appAccentColor, buttonShape),
                        color = buttonBgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Storefront, null, tint = appAccentColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(t["view_store"] ?: "", color = contentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Text(
                        text = t["active_cats"] ?: "",
                        color = appAccentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (activeCategories.isEmpty()) {
                    item {
                        Text(
                            text = if (appLanguage == "Español") "No hay categorías seleccionadas." else "No categories selected.",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    items(activeCategories) { category ->
                        val customName by dataStoreManager.getCategoryName(category.id).collectAsState(initial = null)
                        val fixed = GymCategory.entries.find { it.id == category.id }
                        val displayName = if (!customName.isNullOrEmpty()) customName!!
                        else if (fixed != null) getCategoryTranslation(fixed, appLanguage)
                        else if (appLanguage == "Español") "Extra" else "Extra"

                        ActiveCategoryRow(
                            categoryId = category.id,
                            displayName = displayName,
                            defaultColor = category.defaultColor,
                            shape = buttonShape,
                            accentColor = appAccentColor,
                            isSelected = category.id == lastSelectedId,
                            onClick = {
                                // Guarda selección en background y navega inmediatamente
                                scope.launch { dataStoreManager.setLastSelectedCategory(category.id) }
                                onCategoryClick(category.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

data class CategoryDisplayData(
    val id: String,
    val defaultColor: Color,
    val defaultIcon: Int,
    val isCustom: Boolean = false
)

@Composable
fun ActiveCategoryRow(
    categoryId: String,
    displayName: String,
    defaultColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categorySavedColor by DataStoreManager(LocalContext.current).getCategoryColor(categoryId).collectAsState(initial = null)
    val bgColor = if (categorySavedColor != null) Color(categorySavedColor!!) else defaultColor
    val buttonBgColor = LocalButtonBackgroundColor.current
    val contentColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.5f),
                shape = shape
            )
            .clickable { onClick() },
        color = buttonBgColor
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(bgColor, RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = displayName,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isSelected) accentColor else accentColor.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun StoreConfigButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier,
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val contentColor = if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite

    Surface(
        modifier = modifier
            .height(110.dp)
            .clip(shape)
            .clickable { onClick() }
            .border(1.5.dp, accentColor, shape),
        color = buttonBgColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
