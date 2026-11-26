package com.zexo.gymcopilot.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val selectedCategoryIds by dataStoreManager.getStoreCategories().collectAsState(initial = emptySet())
    val customCategoryIds by dataStoreManager.getCustomCategoryIds().collectAsState(initial = emptySet())
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val appAccentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    val fixedCategories = GymCategory.entries.filter { it != GymCategory.EXTRAS }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (appLanguage == "Español") "Gestionar Categorías" else "Manage Categories", 
                            color = Color.Black, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ) 
                    },
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
                    actions = {
                        TextButton(
                            onClick = {
                                val newId = "CUSTOM_${UUID.randomUUID()}"
                                scope.launch {
                                    val updatedCustomIds = customCategoryIds + newId
                                    dataStoreManager.setCustomCategoryIds(updatedCustomIds)
                                    dataStoreManager.setStoreCategories(selectedCategoryIds + newId)
                                    dataStoreManager.setLastSelectedCategory(newId)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, null, tint = appAccentColor)
                            Text(if (appLanguage == "Español") "Nueva" else "New", color = appAccentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
            containerColor = Color.White
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                Text(
                    text = if (appLanguage == "Español") "Selecciona las categorías que deseas vender:" else "Select categories you want to sell:",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(0.dp, 8.dp, 0.dp, 100.dp)
                ) {
                    items(fixedCategories) { category ->
                        val isSelected = category.id in selectedCategoryIds
                        CategoryItem(
                            id = category.id,
                            defaultName = getCategoryTranslation(category, appLanguage),
                            defaultIcon = category.defaultIconRes,
                            defaultColor = category.bgColor,
                            isSelected = isSelected,
                            dataStoreManager = dataStoreManager,
                            onToggle = {
                                scope.launch {
                                    val newSelection = if (isSelected) selectedCategoryIds - category.id else selectedCategoryIds + category.id
                                    dataStoreManager.setStoreCategories(newSelection)
                                    if (!isSelected) dataStoreManager.setLastSelectedCategory(category.id)
                                }
                            }
                        )
                    }

                    items(customCategoryIds.toList()) { customId ->
                        val isSelected = customId in selectedCategoryIds
                        CategoryItem(
                            id = customId,
                            defaultName = if (appLanguage == "Español") "Extra" else "Extra",
                            defaultIcon = R.drawable.gymcopilot,
                            defaultColor = Color(0xFF9575CD),
                            isSelected = isSelected,
                            dataStoreManager = dataStoreManager,
                            isCustom = true,
                            onToggle = {
                                scope.launch {
                                    val newSelection = if (isSelected) selectedCategoryIds - customId else selectedCategoryIds + customId
                                    dataStoreManager.setStoreCategories(newSelection)
                                    if (!isSelected) dataStoreManager.setLastSelectedCategory(customId)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    dataStoreManager.setCustomCategoryIds(customCategoryIds - customId)
                                    dataStoreManager.setStoreCategories(selectedCategoryIds - customId)
                                    dataStoreManager.resetCategory(customId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    id: String,
    defaultName: String,
    defaultIcon: Int,
    defaultColor: Color,
    isSelected: Boolean,
    dataStoreManager: DataStoreManager,
    isCustom: Boolean = false,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val categoryUri by dataStoreManager.getCategoryUri(id).collectAsState(initial = null)
    val categorySavedColor by dataStoreManager.getCategoryColor(id).collectAsState(initial = null)
    val customName by dataStoreManager.getCategoryName(id).collectAsState(initial = null)
    
    val accentColor = if (categorySavedColor != null) Color(categorySavedColor!!) else defaultColor
    val displayName = if (!customName.isNullOrEmpty()) customName!! else defaultName

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .then(
                if (isSelected) Modifier.border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1.8f).background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryUri != null) {
                        AsyncImage(model = categoryUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Image(painter = painterResource(id = defaultIcon), contentDescription = null, modifier = Modifier.fillMaxSize(0.91f), contentScale = ContentScale.Fit)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF262626)).padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp, maxLines = 2)
                }
            }
            
            if (isSelected) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).background(Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            if (isCustom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                        .clickable { onDelete?.invoke() }, 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
