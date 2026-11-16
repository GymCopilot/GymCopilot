package com.zexo.gymcopilot.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Member
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.model.Exercise
import com.zexo.gymcopilot.model.Routine
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun getRoutineKey(routine: Routine): String = routine.id.ifBlank { "${routine.name}_${routine.createdAt}" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDashboardScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onClassesClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onRoutinesClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onNewRoutineClick: () -> Unit = {},
    onStartRoutine: (String) -> Unit = {},
    onAssignRoutine: (String) -> Unit = {},
    onEditRoutine: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val savedRoutines by dataStoreManager.getRoutines().collectAsState(initial = emptyList())
    val members by dataStoreManager.getMembers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var favoritesFilterActive by remember { mutableStateOf(false) }
    var selectedPdfRoutineIds by remember { mutableStateOf(setOf<String>()) }

    var showPrintConfigDialog by remember { mutableStateOf(false) }
    var routineToConfigurePrint by remember { mutableStateOf<Routine?>(null) }
    var customPrintTitle by remember { mutableStateOf("") }
    var customPrintObjective by remember { mutableStateOf("") }
    var customPrintSubObjective by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()
    var showCategorySheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todas") }

    var routineToAssign by remember { mutableStateOf<Routine?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    val linkedMemberEmail by dataStoreManager.getLinkedMemberEmail().collectAsState(initial = "")
    val effectiveEmail = remember(userEmail, linkedMemberEmail) { userEmail.takeIf { it.isNotBlank() } ?: linkedMemberEmail }

    val repository = remember(gymApiUrl) {
        AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
    }

    LaunchedEffect(gymApiUrl, effectiveEmail) {
        if (gymApiUrl.isNotBlank() && !effectiveEmail.isNullOrBlank()) {
            while (isActive) {
                repository.syncRoutinesFromServer()
                repository.syncProfessorsFromServer()
                delay(5000)
            }
        }
    }

    val filteredRoutines = remember(savedRoutines, searchQuery, userRole, effectiveEmail, favoritesFilterActive, selectedCategory) {
        val base = if (userRole?.lowercase() == "professor" || userRole?.lowercase() == "admin") {
            savedRoutines.filter { !it.id.contains("::") }
        } else {
            val normalizedEmail = effectiveEmail.orEmpty().lowercase().trim()
            savedRoutines.filter { it.creatorEmail.lowercase().trim() == normalizedEmail || it.assignedMemberEmails.any { e -> e.lowercase().trim() == normalizedEmail } }
        }

        val categoryFiltered = if (selectedCategory == "Todas") base
        else base.filter {
            val cat = it.category.lowercase().removeSuffix("s")
            val sel = selectedCategory.lowercase().removeSuffix("s")
            cat == sel
        }

        val searched = if (searchQuery.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        if (favoritesFilterActive) {
            searched.sortedWith(compareByDescending<Routine> { it.isFavorite }.thenByDescending { it.createdAt })
        } else {
            searched.sortedByDescending { it.createdAt }
        }
    }

    val isProfessor = userRole?.lowercase() == "professor" || userRole?.lowercase() == "admin"
    val totalCount = if (isProfessor) savedRoutines.count { !it.id.contains("::") } else filteredRoutines.size
    val completedCount = savedRoutines.count { it.status == "Completada" }
    val assignedCount = if (isProfessor) {
        savedRoutines.count { it.type == "Asignada" || it.assignedMemberEmails.isNotEmpty() }
    } else {
        val normalizedEmail = effectiveEmail.orEmpty().lowercase().trim()
        savedRoutines.count { it.assignedMemberEmails.any { e -> e.lowercase().trim() == normalizedEmail } }
    }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0B1218),
            dragHandle = { BottomSheetDefaults.DragHandle(color = accentColor) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Filtrar por Grupo Muscular",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                CategoryItem(
                    label = "Todas",
                    icon = Icons.Default.AllInclusive,
                    isSelected = selectedCategory == "Todas",
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth(0.9f),
                    onClick = { selectedCategory = "Todas"; showCategorySheet = false }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        "Pecho" to R.drawable.g_categoria_pecho,
                        "Espalda" to R.drawable.g_categoria_espalda,
                        "Fullbody" to R.drawable.g_categoria_cuerpocompleto
                    ).forEach { (label, icon) ->
                        CategoryItem(
                            label = label,
                            icon = icon,
                            isSelected = selectedCategory == label,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f).padding(4.dp),
                            onClick = { selectedCategory = label; showCategorySheet = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        "Piernas" to R.drawable.g_categoria_piernas,
                        "Brazos" to R.drawable.g_categoria_brazos,
                        "Abdomen" to R.drawable.g_categoria_abdominales // Actualizado de Hombros a Abdomen
                    ).forEach { (label, icon) ->
                        CategoryItem(
                            label = label,
                            icon = icon,
                            isSelected = selectedCategory == label,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f).padding(4.dp),
                            onClick = { selectedCategory = label; showCategorySheet = false }
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Rutinas",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
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
                    }
                )
            },
            bottomBar = {
                when (userRole?.lowercase()) {
                    "professor" -> ProfessorBottomNavigation("member_routines", onHomeClick, onStoreClick, onScheduleClick, onMembersClick, onRoutinesClick, onChatClick, accentColor)
                    else -> MemberBottomNavigation("member_routines", onHomeClick, onStoreClick, onClassesClick, onRoutinesClick, onChatClick, accentColor)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val statModifier = Modifier.weight(1f).height(120.dp)
                        StatBox(label = "Total", value = totalCount.toString(), icon = Icons.Outlined.Description, color = accentColor, accentColor = accentColor, shape = containerShape, isSelected = false, onClick = {}, modifier = statModifier)
                        StatBox(label = "Completadas", value = completedCount.toString(), icon = Icons.Default.CheckCircle, color = Color(0xFF4CAF50), accentColor = accentColor, shape = containerShape, isSelected = false, onClick = {}, modifier = statModifier)
                        StatBox(label = "Asignadas", value = assignedCount.toString(), icon = Icons.Default.Group, color = Color(0xFF2196F3), accentColor = accentColor, shape = containerShape, isSelected = false, onClick = {}, modifier = statModifier)
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        placeholder = { Text("Buscar rutinas...", color = TextGray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor) },
                        shape = containerShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF00222E),
                            unfocusedContainerColor = Color(0xFF00222E),
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    Text(text = "Acciones rápidas", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionItem(
                            label = "Nueva Rutina",
                            accentColor = accentColor,
                            shape = containerShape,
                            modifier = Modifier.weight(1f),
                            onClick = onNewRoutineClick
                        ) {
                            Box(modifier = Modifier.size(38.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = accentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp).align(Alignment.TopStart)
                                )
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0xFF00222E), CircleShape)
                                        .padding(1.dp)
                                )
                            }
                        }
                        QuickActionItem(
                            label = "Favoritas",
                            accentColor = accentColor,
                            shape = containerShape,
                            modifier = Modifier.weight(1f),
                            isSelected = favoritesFilterActive,
                            onClick = { favoritesFilterActive = !favoritesFilterActive }
                        ) {
                            Icon(
                                imageVector = if (favoritesFilterActive) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (favoritesFilterActive) Color(0xFFFFD700) else accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        QuickActionItem(
                            label = "Categorías",
                            accentColor = accentColor,
                            shape = containerShape,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedCategory != "Todas",
                            onClick = { showCategorySheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (selectedCategory != "Todas") accentColor else accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mis rutinas",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (selectedPdfRoutineIds.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50))
                                ) {
                                    Text(
                                        text = "${selectedPdfRoutineIds.size} sel.",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val targetRoutine = if (selectedPdfRoutineIds.isNotEmpty()) {
                                        savedRoutines.firstOrNull { getRoutineKey(it) in selectedPdfRoutineIds }
                                    } else {
                                        savedRoutines.firstOrNull()
                                    }
                                    if (targetRoutine != null) {
                                        routineToConfigurePrint = targetRoutine
                                        customPrintTitle = ""
                                        customPrintObjective = ""
                                        customPrintSubObjective = ""
                                        showPrintConfigDialog = true
                                    } else {
                                        Toast.makeText(context, "No hay rutinas para configurar", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.text_ad_icon),
                                    contentDescription = "Configurar datos de impresión",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val routinesToExport = if (selectedPdfRoutineIds.isNotEmpty()) {
                                        savedRoutines.filter { getRoutineKey(it) in selectedPdfRoutineIds }
                                    } else {
                                        savedRoutines
                                    }
                                    if (routinesToExport.isNotEmpty()) {
                                        Toast.makeText(context, "Generando PDF...", Toast.LENGTH_SHORT).show()
                                        generateAndShareMultiRoutinePdf(
                                            context = context,
                                            routines = routinesToExport,
                                            dataStoreManager = dataStoreManager,
                                            scope = coroutineScope,
                                            customTitle = customPrintTitle,
                                            customObjective = customPrintObjective,
                                            customSubObjective = customPrintSubObjective
                                        )
                                    } else {
                                        Toast.makeText(context, "No hay rutinas para imprimir", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.download_pdf),
                                    contentDescription = "Imprimir Rutinas en PDF",
                                    tint = if (selectedPdfRoutineIds.isNotEmpty()) Color(0xFF4CAF50) else accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (filteredRoutines.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron rutinas", color = TextGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    itemsIndexed(filteredRoutines, key = { _, routine -> getRoutineKey(routine) }) { index, routine ->
                        val statusColor = when (routine.status) {
                            "Activa" -> Color(0xFF4CAF50)
                            "Completada" -> Color(0xFF4CAF50)
                            "Asignada" -> Color(0xFF2196F3)
                            "Borrador" -> Color(0xFFFFA000)
                            else -> TextGray
                        }

                        val tagColor = when (routine.type) {
                            "Personal" -> Color(0xFF673AB7)
                            "Asignada" -> Color(0xFF2E7D32)
                            else -> Color(0xFF424242)
                        }

                        val routineKey = getRoutineKey(routine)
                        val isPdfSel = routineKey in selectedPdfRoutineIds

                        ExpandableRoutineItem(
                            title = routine.name,
                            statusText = routine.status,
                            statusColor = statusColor,
                            creator = routine.creatorName,
                            exercises = routine.exercises.size.toString(),
                            duration = routine.duration.replace(" min", ""),
                            updated = "recientemente",
                            tagText = routine.type,
                            tagColor = tagColor,
                            isExpanded = expandedIndex == index,
                            isFavorite = routine.isFavorite,
                            isPdfSelected = isPdfSel,
                            accentColor = accentColor,
                            shape = containerShape,
                            days = routine.days,
                            showProfessorActions = isProfessor,
                            notes = routine.notes,
                            onNotesChange = { newNotes ->
                                coroutineScope.launch {
                                    dataStoreManager.updateRoutine(routine.copy(notes = newNotes, updatedAt = System.currentTimeMillis()))
                                }
                            },
                            onAssign = { routineToAssign = routine },
                            onDelete = { routineToDelete = routine },
                            onStart = { onStartRoutine(routine.id) },
                            onEdit = { onEditRoutine(routine.id) },
                            onExportPdf = {
                                selectedPdfRoutineIds = if (isPdfSel) {
                                    selectedPdfRoutineIds - routineKey
                                } else {
                                    selectedPdfRoutineIds + routineKey
                                }
                            },
                            onToggleFavorite = {
                                coroutineScope.launch {
                                    dataStoreManager.updateRoutine(routine.copy(isFavorite = !routine.isFavorite))
                                }
                            },
                            onClick = { expandedIndex = if (expandedIndex == index) -1 else index }
                        )
                    }
                }
            }
        }

        if (routineToAssign != null) {
            AssignMemberDialog(
                routine = routineToAssign!!,
                members = members,
                accentColor = accentColor,
                shape = containerShape,
                onDismiss = { routineToAssign = null },
                onAssign = { selectedEmails ->
                    coroutineScope.launch {
                        val originalEmails = routineToAssign!!.assignedMemberEmails
                        val removedEmails = originalEmails.filter { it !in selectedEmails }

                        val rawProfId = dataStoreManager.getLinkedProfessorId().first()
                        val currentEmail = dataStoreManager.getUserEmail().first().lowercase().trim()
                        val profId = if (rawProfId.isNullOrBlank()) currentEmail else rawProfId.trim()

                        val updatedProfessorIds = if (routineToAssign!!.professorIds.contains(profId)) {
                            routineToAssign!!.professorIds
                        } else {
                            routineToAssign!!.professorIds + profId
                        }

                        val updatedRoutine = routineToAssign!!.copy(
                            assignedMemberEmails = selectedEmails.map { it.lowercase().trim() },
                            professorIds = updatedProfessorIds,
                            type = "Asignada"
                        )
                        dataStoreManager.updateRoutine(updatedRoutine)

                        val routineJson = Json.encodeToString(updatedRoutine)

                        removedEmails.forEach { email ->
                            repository.syncMemberRoutine(
                                professorId = profId,
                                memberEmail = email,
                                routineJson = routineJson,
                                routineId = updatedRoutine.id,
                                action = "delete_routine"
                            )
                        }

                        selectedEmails.forEach { email ->
                            repository.syncMemberRoutine(
                                professorId = profId,
                                memberEmail = email,
                                routineJson = routineJson,
                                routineId = updatedRoutine.id,
                                action = "add_routine"
                            )
                        }

                        routineToAssign = null
                    }
                }
            )
        }

        if (routineToDelete != null) {
            AlertDialog(
                onDismissRequest = { routineToDelete = null },
                title = { Text("Eliminar Rutina", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas eliminar la rutina \"${routineToDelete?.name}\"? Esta acción no se puede deshacer y se eliminará de la planilla.", color = TextWhite) },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val routine = routineToDelete!!
                            val rawProfId = dataStoreManager.getLinkedProfessorId().first()
                            val profId = if (rawProfId.isNullOrBlank()) userEmail.lowercase().trim() else rawProfId.trim()
                            val routineJson = Json.encodeToString(routine)

                            if (routine.assignedMemberEmails.isNotEmpty()) {
                                routine.assignedMemberEmails.forEach { email ->
                                    repository.syncMemberRoutine(
                                        professorId = profId,
                                        memberEmail = email,
                                        routineJson = routineJson,
                                        routineId = routine.id,
                                        action = "delete_routine"
                                    )
                                }
                            } else {
                                repository.syncMemberRoutine(
                                    professorId = profId,
                                    memberEmail = "",
                                    routineJson = routineJson,
                                    routineId = routine.id,
                                    action = "delete_routine"
                                )
                            }

                            dataStoreManager.deleteRoutine(routine.id)
                            routineToDelete = null
                        }
                    }) {
                        Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineToDelete = null }) {
                        Text("CANCELAR", color = TextGray)
                    }
                },
                containerColor = Color(0xFF00151C),
                shape = containerShape
            )
        }

        if (showPrintConfigDialog && routineToConfigurePrint != null) {
            RoutinePrintConfigDialog(
                accentColor = accentColor,
                shape = containerShape,
                initialTitle = customPrintTitle,
                initialObjective = customPrintObjective,
                initialSubObjective = customPrintSubObjective,
                onDismiss = { showPrintConfigDialog = false },
                onConfirm = { t, o, s ->
                    customPrintTitle = t
                    customPrintObjective = o
                    customPrintSubObjective = s
                    showPrintConfigDialog = false
                    Toast.makeText(context, "Datos de impresión guardados", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun CategoryItem(
    label: String,
    icon: Any,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.25f) else buttonBgColor
    val borderColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.4f)
    val textColor = if (isSelected) accentColor else (if (buttonBgColor.luminance() > 0.5f) Color.Black else Color.LightGray)

    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        val iconPainter = when (icon) {
            is ImageVector -> rememberVectorPainter(icon)
            is Int -> painterResource(id = icon)
            else -> rememberVectorPainter(Icons.Default.Help)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = label,
                tint = if (isSelected) accentColor else (if (buttonBgColor.luminance() > 0.5f) Color.Black else Color.White),
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AssignMemberDialog(
    routine: Routine,
    members: List<Member>,
    accentColor: Color,
    shape: Shape,
    onDismiss: () -> Unit,
    onAssign: (List<String>) -> Unit
) {
    var selectedEmails by remember { mutableStateOf(routine.assignedMemberEmails) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = if (searchQuery.isBlank()) members else members.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = shape,
            color = Color(0xFF00151C),
            modifier = Modifier.padding(16.dp).border(1.dp, accentColor.copy(alpha = 0.5f), shape)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Asignar Rutina - ${routine.name}",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar alumno...", color = TextGray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF081C24),
                        unfocusedContainerColor = Color(0xFF081C24),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (filteredMembers.isEmpty()) {
                        item {
                            Text("No se encontraron alumnos", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        items(filteredMembers) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEmails = if (selectedEmails.contains(member.email)) {
                                            selectedEmails.filter { it != member.email }
                                        } else {
                                            selectedEmails + member.email
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedEmails.contains(member.email),
                                    onCheckedChange = { checked ->
                                        selectedEmails = if (checked) {
                                            selectedEmails + member.email
                                        } else {
                                            selectedEmails.filter { it != member.email }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        uncheckedColor = TextGray,
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(member.fullName, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(member.email, color = TextGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = TextGray)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { onAssign(selectedEmails) },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, accentColor, RoundedCornerShape(8.dp))
                    ) {
                        val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                        Text("ASIGNAR", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoutinePrintConfigDialog(
    accentColor: Color,
    shape: Shape,
    initialTitle: String,
    initialObjective: String,
    initialSubObjective: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, objective: String, subObjective: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var objective by remember { mutableStateOf(initialObjective) }
    var subObjective by remember { mutableStateOf(initialSubObjective) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Datos de Impresión", fontWeight = FontWeight.Bold, color = TextWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la rutina (Encabezado)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = TextGray
                    )
                )
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("Objetivo de la rutina") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = TextGray
                    )
                )
                OutlinedTextField(
                    value = subObjective,
                    onValueChange = { subObjective = it },
                    label = { Text("Sub objetivo / Programa enfocado") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = TextGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, objective, subObjective) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ACEPTAR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = TextGray)
            }
        },
        containerColor = Color(0xFF1B252D),
        shape = shape
    )
}

@Composable
fun LazyItemScope.ExpandableRoutineItem(
    title: String,
    statusText: String,
    statusColor: Color,
    creator: String,
    exercises: String,
    duration: String,
    updated: String,
    tagText: String,
    tagColor: Color,
    isExpanded: Boolean,
    isFavorite: Boolean = false,
    isPdfSelected: Boolean = false,
    accentColor: Color,
    shape: Shape,
    days: List<String> = emptyList(),
    showProfessorActions: Boolean = false,
    notes: String = "",
    onNotesChange: (String) -> Unit = {},
    onAssign: () -> Unit = {},
    onDelete: () -> Unit = {},
    onStart: () -> Unit = {},
    onEdit: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit
) {
    val titlePaddingTop by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 4.dp,
        label = "titlePadding"
    )

    var notesValue by remember { mutableStateOf(TextFieldValue(notes)) }
    val currentOnNotesChange by rememberUpdatedState(onNotesChange)
    LaunchedEffect(notesValue.text) {
        if (notesValue.text != notes) {
            delay(500)
            currentOnNotesChange(notesValue.text)
        }
    }

    Card(
        modifier = Modifier
            .animateItem(placementSpec = tween(1200, easing = FastOutSlowInEasing)) 
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .drawBehind {
                val cornerSize = if (isExpanded) size.height * 0.05f else size.height * 0.15f
                val path = Path().apply {
                    moveTo(0f, size.height - cornerSize)
                    lineTo(cornerSize, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = if (statusText == "Completada") Color(0xFF4CAF50) else accentColor)
            }
            .border(
                1.dp,
                if (statusText == "Completada") Color(0xFF4CAF50).copy(alpha = 0.6f)
                else if (isExpanded) accentColor.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.05f),
                shape
            )
            .clickable { onStart() }, // CORRECCIÓN: Al tocar la tarjeta se inicia la rutina
        colors = CardDefaults.cardColors(
            containerColor = if (statusText == "Completada") Color(0xFF1B252D) else Color.White.copy(alpha = 0.03f)
        ),
        shape = shape
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (isExpanded) 16.dp else 12.dp)
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onStart() },
                        shape = CircleShape,
                        color = accentColor,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Iniciar",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = titlePaddingTop)
                        )

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (statusText != "Completada") {
                                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(8.dp))
                                Surface(color = tagColor, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = tagText,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // FILA DE ACCIONES HORIZONTAL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // INDICADOR MOVIDO A LA FILA DE BOTONES
                            if (statusText == "Completada") {
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Finalizada",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            IconButton(onClick = { onExportPdf() }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.download_pdf),
                                    contentDescription = "Descargar PDF",
                                    tint = if (isPdfSelected) Color(0xFF4CAF50) else accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = { onToggleFavorite() }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Favorito",
                                    tint = if (isFavorite) Color(0xFFFFD700) else accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (showProfessorActions) {
                                IconButton(onClick = { onAssign() }, modifier = Modifier.size(36.dp)) {
                                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Asignar", tint = accentColor, modifier = Modifier.size(24.dp))
                                }
                            }

                            IconButton(onClick = { onEdit() }, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar", tint = accentColor, modifier = Modifier.size(24.dp))
                            }

                            IconButton(onClick = { onDelete() }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete, 
                                    contentDescription = "Eliminar", 
                                    tint = if (showProfessorActions) Color(0xFFF44336) else Color(0xFFD32F2F), 
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        if (days.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val allDays = listOf("LU", "MA", "MI", "JU", "VI", "SÁ", "DO")
                                allDays.forEach { day ->
                                    val isSelected = days.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            color = if (isSelected) Color.Black else TextWhite.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                        DetailRow(icon = Icons.Outlined.Person, text = creator, accentColor = accentColor)
                        DetailRow(icon = Icons.AutoMirrored.Outlined.Assignment, text = "$exercises ejercicios  •  $duration min", accentColor = accentColor)
                        DetailRow(icon = Icons.Outlined.CalendarToday, text = "Actualizada: recientemente", accentColor = accentColor)
                    }
                }

                // CUADRO DE ANOTACIONES PERSISTENTES
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = notesValue,
                            onValueChange = { notesValue = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            placeholder = { Text("Anotaciones generales de la rutina...", color = TextGray, fontSize = 13.sp) },
                            textStyle = LocalTextStyle.current.copy(color = TextWhite, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                // Espacio extra
                if (isExpanded) Spacer(Modifier.height(8.dp))
            }

            // Zona interactiva para el botón de la esquina
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(40.dp)
                    .clickable { onClick() }
            )
        }
    }
}


@Composable
fun DetailRow(icon: ImageVector, text: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = text, color = TextGray, fontSize = 13.sp)
    }
}

@Composable
fun QuickActionItem(
    label: String,
    accentColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val textColor = if (isSelected) accentColor
                    else (if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite)

    Column(
        modifier = modifier
            .height(100.dp)
            .clip(shape)
            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else buttonBgColor)
            .border(1.2.dp, if (isSelected) accentColor else accentColor.copy(alpha = 0.4f), shape)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

private fun generateAndShareRoutinePdf(
    context: Context,
    routine: com.zexo.gymcopilot.model.Routine,
    dataStoreManager: DataStoreManager,
    scope: CoroutineScope,
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = ""
) {
    scope.launch(Dispatchers.IO) {
        try {
            val gymName = dataStoreManager.getGymName().first().ifBlank { "GIMNASIO COPILOT" }
            val gymAddress = dataStoreManager.getGymAddress().first().ifBlank { "Av. Principal 1234, Centro" }
            val gymCity = dataStoreManager.getGymCity().first().ifBlank { gymAddress.substringAfterLast(",").trim().ifBlank { "BUENOS AIRES" } }
            val gymCountry = dataStoreManager.getGymCountry().first().ifBlank { "" }
            val gymPhone = dataStoreManager.getGymPhone().first()
            val userName = dataStoreManager.getUserName().first().ifBlank { "Socio / Alumno" }
            val logoUriStr = dataStoreManager.getGymLogoUri().first()
            val template = dataStoreManager.getRoutinePdfTemplate().first()
            val gymSlogan = dataStoreManager.getGymSlogan().first().ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }
            val gymInstagram = dataStoreManager.getGymInstagram().first().ifBlank { "@tugimnasio" }

            val logoBitmap: Bitmap? = try {
                if (!logoUriStr.isNullOrBlank()) {
                    if (logoUriStr.startsWith("data:image")) {
                        val base64 = logoUriStr.substringAfter(",")
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else {
                        val uri = Uri.parse(logoUriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                } else null
            } catch (e: Exception) { null }

            val anatomyBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.anatomia_muscular)
            } catch (e: Exception) { null }

            val currentDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595 x 842 pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            when (template) {
                "Profesional", "Alfa Balcarce" -> {
                    drawProfesionalPdfTemplate(
                        canvas = canvas,
                        paint = paint,
                        routines = listOf(routine),
                        gymName = gymName,
                        gymAddress = gymAddress,
                        gymCity = gymCity,
                        gymPhone = gymPhone,
                        userName = userName,
                        dateStr = currentDateStr,
                        logoBitmap = logoBitmap,
                        gymSlogan = gymSlogan,
                        gymInstagram = gymInstagram,
                        customTitle = customTitle,
                        customObjective = customObjective,
                        customSubObjective = customSubObjective,
                        anatomyBitmap = anatomyBitmap
                    )
                }
                "Ejecutiva" -> {
                    val gymZip = dataStoreManager.getGymPostalCode().first().ifBlank { "" }
                    val gymCountry = dataStoreManager.getGymCountry().first().ifBlank { "" }
                    val gymFacebook = dataStoreManager.getGymFacebook().first().ifBlank { "" }
                    val gymTikTok = dataStoreManager.getGymTikTok().first().ifBlank { "" }
                    drawEjecutivaPdfTemplate(
                        canvas = canvas,
                        paint = paint,
                        routine = routine,
                        gymName = gymName,
                        gymAddress = gymAddress,
                        userName = userName,
                        dateStr = currentDateStr,
                        logoBitmap = logoBitmap,
                        gymCity = gymCity,
                        gymPhone = gymPhone,
                        gymZip = gymZip,
                        gymCountry = gymCountry,
                        gymSlogan = gymSlogan,
                        gymInstagram = gymInstagram,
                        gymFacebook = gymFacebook,
                        gymTikTok = gymTikTok,
                        customTitle = customTitle,
                        customObjective = customObjective,
                        customSubObjective = customSubObjective
                    )
                }
                "Moderna" -> {
                    drawModernaPdfTemplate(
                        canvas = canvas,
                        paint = paint,
                        routines = listOf(routine),
                        routine = routine,
                        gymName = gymName,
                        gymAddress = gymAddress,
                        gymCity = gymCity,
                        gymCountry = gymCountry,
                        gymSlogan = gymSlogan,
                        gymInstagram = gymInstagram,
                        userName = userName,
                        dateStr = currentDateStr,
                        logoBitmap = logoBitmap,
                        customTitle = customTitle,
                        customObjective = customObjective,
                        customSubObjective = customSubObjective
                    )
                }
                "Minimalista" -> {
                    drawMinimalistaPdfTemplate(
                        canvas = canvas,
                        paint = paint,
                        routines = listOf(routine),
                        routine = routine,
                        gymName = gymName,
                        gymAddress = gymAddress,
                        gymCity = gymCity,
                        gymCountry = gymCountry,
                        gymSlogan = gymSlogan,
                        gymInstagram = gymInstagram,
                        userName = userName,
                        dateStr = currentDateStr,
                        logoBitmap = logoBitmap,
                        customTitle = customTitle,
                        customObjective = customObjective,
                        customSubObjective = customSubObjective
                    )
                }
                else -> { // Clásica
                    drawClasicaPdfTemplate(
                        canvas = canvas,
                        paint = paint,
                        routines = listOf(routine),
                        routine = routine,
                        gymName = gymName,
                        gymAddress = gymAddress,
                        gymCity = gymCity,
                        gymCountry = gymCountry,
                        gymSlogan = gymSlogan,
                        gymInstagram = gymInstagram,
                        userName = userName,
                        dateStr = currentDateStr,
                        logoBitmap = logoBitmap,
                        customTitle = customTitle,
                        customObjective = customObjective,
                        customSubObjective = customSubObjective
                    )
                }
            }

            pdfDocument.finishPage(page)

            val safeRoutineName = routine.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Rutina" }
            val safeTemplate = template.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "Rutina_${safeRoutineName}_$safeTemplate.pdf"
            val file = File(context.cacheDir, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                shareRoutinePdfFile(context, file, routine.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun drawProfesionalPdfTemplate(
    canvas: Canvas,
    paint: Paint,
    routines: List<com.zexo.gymcopilot.model.Routine>,
    gymName: String,
    gymAddress: String,
    gymCity: String,
    gymPhone: String,
    userName: String,
    dateStr: String,
    logoBitmap: Bitmap?,
    gymSlogan: String = "DISCIPLINA HOY, RESULTADOS SIEMPRE.",
    gymInstagram: String = "@tugimnasio",
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = "",
    anatomyBitmap: Bitmap? = null
) {
    paint.isAntiAlias = true
    val charcoalDark = android.graphics.Color.parseColor("#1E1E1E")
    val headerGray = android.graphics.Color.parseColor("#333333")
    val borderGray = android.graphics.Color.parseColor("#1E293B") // 50% más oscurecidas para que se noten más
    val rowAltGray = android.graphics.Color.parseColor("#F1F5F9")
    val accentLight = android.graphics.Color.parseColor("#E2E8F0")

    // 1. BANNER CABECERA NEGRA (50% más grande: alto 105pt, x=20 a 575)
    paint.color = charcoalDark
    paint.style = Paint.Style.FILL
    canvas.drawRect(20f, 10f, 575f, 115f, paint)

    // Logo / Escudo Izquierdo (50% más grande y manteniendo aspecto sin deformar)
    if (logoBitmap != null) {
        try {
            val maxWidth = 90f
            val maxHeight = 95f
            val bmpW = logoBitmap.width.toFloat()
            val bmpH = logoBitmap.height.toFloat()
            if (bmpW > 0f && bmpH > 0f) {
                val scale = (maxWidth / bmpW).coerceAtMost(maxHeight / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val left = 22f + (maxWidth - scaledW) / 2f
                val top = 12f + (maxHeight - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            } else {
                val destRect = RectF(22f, 12f, 112f, 107f)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            }
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        // Escudo TU TG GIMNASIO (50% más grande)
        paint.color = android.graphics.Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(22f, 12f, 112f, 107f), 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("TU", 62f, 38f, paint)
        paint.textSize = 30f
        paint.isFakeBoldText = true
        canvas.drawText("TG", 52f, 72f, paint)
        paint.textSize = 8.5f
        paint.color = accentLight
        canvas.drawText("GIMNASIO", 40f, 94f, paint)
    }

    // Textos Centrales
    val headerX = 297.5f // Centro de la página
    paint.textAlign = Paint.Align.CENTER

    // Nombre Gym (50% más grande: 27f)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 27f
    paint.isFakeBoldText = true
    canvas.drawText(gymName.uppercase(), headerX, 44f, paint)

    paint.textSize = 12f
    paint.color = accentLight
    paint.isFakeBoldText = true
    canvas.drawText("★  ${gymCity.uppercase()}  ★", headerX, 64f, paint)

    paint.textSize = 13.5f
    paint.color = android.graphics.Color.WHITE
    paint.isFakeBoldText = true
    val firstRoutine = routines.firstOrNull()
    val titleText = customTitle.ifBlank { firstRoutine?.name?.uppercase() ?: "GENERAL" }
    canvas.drawText(titleText, headerX, 86f, paint)

    paint.textSize = 10.5f
    paint.color = accentLight
    paint.isFakeBoldText = false
    val subObjFallback = routines.map { it.objective }.firstOrNull { it.isNotBlank() } ?: "GENERAL"
    val subObjText = customObjective.ifBlank { subObjFallback }
    canvas.drawText(subObjText.uppercase(), headerX, 105f, paint)

    // Escudo Derecho FITNESS H PRO con borde punteado (escalado)
    paint.textAlign = Paint.Align.LEFT
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.8f
    // Ilustración de anatomía muscular superior derecha (proporcional completa, sin cortes)
    val rightBoxRect = RectF(470f, 10f, 575f, 115f)
    if (anatomyBitmap != null) {
        try {
            val targetW = rightBoxRect.width()
            val targetH = rightBoxRect.height()
            val bmpW = anatomyBitmap.width.toFloat()
            val bmpH = anatomyBitmap.height.toFloat()
            if (bmpW > 0f && bmpH > 0f) {
                val scale = (targetW / bmpW).coerceAtMost(targetH / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val left = rightBoxRect.left + (targetW - scaledW) / 2f
                val top = rightBoxRect.top + (targetH - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)

                paint.alpha = 200
                canvas.drawBitmap(anatomyBitmap, null, destRect, paint)
                paint.alpha = 255
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Cinta secundaria de enfoque (x=20 a 575)
    paint.color = headerGray
    canvas.drawRect(20f, 118f, 575f, 138f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 9f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val fallbackDesc = routines.map { it.description }.firstOrNull { it.isNotBlank() } ?: "FUERZA FUNCIONAL – ESTABILIDAD – MOVILIDAD – PROTECCIÓN ARTICULAR"
    val focusProgramText = "PROGRAMA ENFOCADO EN: " + customSubObjective.ifBlank { fallbackDesc }.uppercase()
    canvas.drawText(focusProgramText, headerX, 131f, paint)

    // 2. BARRA DE DATOS DEL ALUMNO (x=20 a 575)
    paint.textAlign = Paint.Align.LEFT
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(20f, 142f, 575f, 164f, paint)
    paint.color = charcoalDark
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(20f, 142f, 575f, 164f, paint)

    paint.style = Paint.Style.FILL
    paint.textSize = 9.5f
    paint.isFakeBoldText = true
    canvas.drawText("NOMBRE:", 26f, 156f, paint)
    paint.isFakeBoldText = false
    canvas.drawText(userName, 76f, 156f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("FECHA:", 210f, 156f, paint)
    paint.isFakeBoldText = false
    canvas.drawText(dateStr, 255f, 156f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("OBJETIVO:", 340f, 156f, paint)
    paint.isFakeBoldText = false
    val fallbackObj = routines.map { it.objective }.firstOrNull { it.isNotBlank() } ?: "General"
    val objText = customObjective.ifBlank { fallbackObj }
    canvas.drawText(objText, 398f, 156f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("NIVEL:", 485f, 156f, paint)
    paint.isFakeBoldText = false
    val levelText = routines.map { it.level }.firstOrNull { it.isNotBlank() } ?: "General"
    canvas.drawText(levelText, 525f, 156f, paint)

    // 3. BLOQUES DÍAS Y TABLAS (DÍA 1, DÍA 2, DÍA 3...)
    var currentY = 170f

    data class DayBlockData(
        val dayNumber: Int,
        val dayLabel: String,
        val subTitle: String,
        val exercises: List<Exercise>
    )

    val blocks = mutableListOf<DayBlockData>()

    if (routines.size > 1) {
        // Múltiples rutinas seleccionadas (p. ej. Lunes, Miércoles, Viernes): Cada rutina es un bloque de día
        routines.forEachIndexed { idx, r ->
            val dayName = if (r.days.isNotEmpty()) r.days.joinToString(" ") else r.name.uppercase()
            val subTitle = if (r.description.isNotBlank()) r.description else r.name
            blocks.add(DayBlockData(idx + 1, dayName, subTitle, r.exercises))
        }
    } else if (routines.isNotEmpty()) {
        val single = routines.first()
        if (single.exercises.size > 7 && single.days.isEmpty()) {
            val chunkSize = (single.exercises.size + 2) / 3
            val chunks = single.exercises.chunked(chunkSize.coerceAtLeast(1))
            val defaultSubtitles = listOf("PIERNAS /\nGLÚTEOS", "TREN SUPERIOR\n+\nABDOMINALES", "GLÚTEOS +\nPIERNAS")
            chunks.forEachIndexed { idx, chunk ->
                val sub = defaultSubtitles.getOrElse(idx) { single.name }
                blocks.add(DayBlockData(idx + 1, "DÍA ${idx + 1}", sub, chunk))
            }
        } else {
            val dayName = if (single.days.isNotEmpty()) single.days.joinToString(" ") else single.name.uppercase()
            blocks.add(DayBlockData(1, dayName, single.name, single.exercises))
        }
    }

    blocks.forEachIndexed { dIdx, block ->
        if (currentY > 640f) return@forEachIndexed

        val chunk = block.exercises
        val tableHeight = (chunk.size * 18f + 18f).coerceAtLeast(50f)

        // Borde exterior del bloque del día
        paint.color = charcoalDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(20f, currentY, 575f, currentY + tableHeight, paint)

        // Columna Izquierda Oscura (DÍA X)
        paint.style = Paint.Style.FILL
        paint.color = charcoalDark
        canvas.drawRect(20f, currentY, 98f, currentY + tableHeight, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 12f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("DÍA ${block.dayNumber}", 59f, currentY + 20f, paint)

        paint.textSize = 7.5f
        paint.color = accentLight
        paint.isFakeBoldText = true
        val lines = block.dayLabel.split("\n", " - ", " / ")
        var subY = currentY + 33f
        lines.take(2).forEach { line ->
            canvas.drawText(line.take(12), 59f, subY, paint)
            subY += 9f
        }

        // Tabla al lado derecho (x=98f a 575f)
        paint.textAlign = Paint.Align.LEFT
        // Cabecera Tabla
        paint.color = headerGray
        canvas.drawRect(98f, currentY, 575f, currentY + 18f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas.drawText("#", 105f, currentY + 12f, paint)
        canvas.drawText("EJERCICIO", 125f, currentY + 12f, paint)
        canvas.drawText("SERIES", 320f, currentY + 12f, paint)
        canvas.drawText("REPETICIONES", 375f, currentY + 12f, paint)
        canvas.drawText("NOTAS TÉCNICAS", 450f, currentY + 12f, paint)

        var rowY = currentY + 18f
        chunk.forEachIndexed { idx, ex ->
            paint.color = if (idx % 2 == 1) rowAltGray else android.graphics.Color.WHITE
            canvas.drawRect(98f, rowY, 575f, rowY + 18f, paint)

            paint.color = borderGray
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(98f, rowY, 575f, rowY + 18f, paint)

            paint.style = Paint.Style.FILL
            paint.color = charcoalDark
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("${idx + 1}", 105f, rowY + 12f, paint)

            canvas.drawText(ex.name, 125f, rowY + 12f, paint)

            paint.isFakeBoldText = false
            canvas.drawText(ex.series, 330f, rowY + 12f, paint)
            canvas.drawText(ex.reps, 385f, rowY + 12f, paint)
            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 8f
            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 450f, rowY + 12f, paint)

            rowY += 18f
        }

        currentY += tableHeight + 6f
    }

    // 4. SECCIÓN INFERIOR (INDICACIONES + PROGRESO + NOTAS)
    val footerY = currentY.coerceAtLeast(650f)
    val footerBoxHeight = 100f

    // 1. INDICACIONES GENERALES (x=20 a 195)
    paint.color = charcoalDark
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(20f, footerY, 195f, footerY + footerBoxHeight, paint)

    paint.style = Paint.Style.FILL
    paint.color = charcoalDark
    canvas.drawRect(20f, footerY, 195f, footerY + 16f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 8f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("INDICACIONES GENERALES", 107.5f, footerY + 11f, paint)

    paint.textAlign = Paint.Align.LEFT
    paint.color = charcoalDark
    paint.textSize = 7.5f
    paint.isFakeBoldText = false
    val bullets = listOf(
        "• Realizar entrada en calor de 10 minutos.",
        "• Priorizar técnica antes que peso.",
        "• Aumentar cargas progresivamente.",
        "• Mantener buena hidratación y descanso.",
        "• Evitar ejercicios que generen dolor.",
        "• Consultar con profesional ante dudas."
    )
    var bulletY = footerY + 28f
    bullets.forEach { txt ->
        canvas.drawText(txt, 25f, bulletY, paint)
        bulletY += 11f
    }

    // 2. REGISTRO DE PROGRESO (x=200 a 395)
    paint.color = charcoalDark
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(200f, footerY, 395f, footerY + footerBoxHeight, paint)

    paint.style = Paint.Style.FILL
    paint.color = charcoalDark
    canvas.drawRect(200f, footerY, 395f, footerY + 16f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 8f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("REGISTRO DE PROGRESO", 297.5f, footerY + 11f, paint)

    // Cabecera Registro
    paint.color = accentLight
    canvas.drawRect(200f, footerY + 16f, 395f, footerY + 28f, paint)
    paint.color = borderGray
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.5f
    canvas.drawRect(200f, footerY + 16f, 395f, footerY + 28f, paint)

    paint.style = Paint.Style.FILL
    paint.color = charcoalDark
    paint.textSize = 6.5f
    paint.isFakeBoldText = true
    canvas.drawText("FECHA", 218f, footerY + 24f, paint)
    canvas.drawText("DOLOR (0-10)", 255f, footerY + 24f, paint)
    canvas.drawText("RENDIMIENTO", 305f, footerY + 24f, paint)
    canvas.drawText("OBSERVACIONES", 360f, footerY + 24f, paint)

    // 5 Filas para rellenar a mano
    var lineY = footerY + 28f
    repeat(5) {
        lineY += 14f
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawLine(200f, lineY, 395f, lineY, paint)
        canvas.drawLine(238f, lineY - 14f, 238f, lineY, paint)
        canvas.drawLine(280f, lineY - 14f, 280f, lineY, paint)
        canvas.drawLine(335f, lineY - 14f, 335f, lineY, paint)
    }

    // 3. NOTAS (x=400 a 575)
    paint.color = charcoalDark
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(400f, footerY, 575f, footerY + footerBoxHeight, paint)

    paint.style = Paint.Style.FILL
    paint.color = charcoalDark
    canvas.drawRect(400f, footerY, 575f, footerY + 16f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 8f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("NOTAS", 487.5f, footerY + 11f, paint)

    paint.style = Paint.Style.FILL
    paint.textAlign = Paint.Align.LEFT
    paint.color = charcoalDark
    paint.textSize = 7.5f
    val notesText = routines.map { it.notes }.firstOrNull { it.isNotBlank() } ?: ""
    if (notesText.isNotBlank()) {
        canvas.drawText(notesText.take(35), 405f, footerY + 28f, paint)
    }
    var noteLineY = footerY + 28f
    repeat(5) {
        noteLineY += 14f
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawLine(405f, noteLineY, 570f, noteLineY, paint)
    }

    // 5. PIE INSTITUCIONAL (x=20 a 575)
    paint.style = Paint.Style.FILL
    paint.color = charcoalDark
    canvas.drawRect(20f, 775f, 575f, 800f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 7.5f
    paint.isFakeBoldText = true

    paint.textAlign = Paint.Align.LEFT
    val addressText = gymAddress.ifBlank { "CALLE 13 ENTRE 16 Y 14, CHAVES N° 564" }
    canvas.drawText(addressText.take(38), 26f, 790f, paint)

    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(gymInstagram.ifBlank { "@tugimnasio" }, headerX, 790f, paint)

    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(gymSlogan.ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }, 569f, 790f, paint)

    // Sub-pie blanco con paginación
    paint.textAlign = Paint.Align.LEFT
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 8f
    paint.isFakeBoldText = false
    canvas.drawText("Plantilla Ejemplo Adaptada — $gymName", 20f, 812f, paint)

    paint.textAlign = Paint.Align.RIGHT
    paint.isFakeBoldText = true
    canvas.drawText("Página 1 de 1", 575f, 812f, paint)
}

private fun drawEjecutivaPdfTemplate(
    canvas: Canvas,
    paint: Paint,
    routines: List<com.zexo.gymcopilot.model.Routine> = emptyList(),
    routine: com.zexo.gymcopilot.model.Routine? = null,
    gymName: String,
    gymAddress: String,
    userName: String,
    dateStr: String,
    logoBitmap: Bitmap?,
    gymCity: String = "",
    gymPhone: String = "",
    gymZip: String = "",
    gymCountry: String = "",
    gymSlogan: String = "DISCIPLINA HOY, RESULTADOS SIEMPRE.",
    gymInstagram: String = "@tugimnasio",
    gymFacebook: String = "",
    gymTikTok: String = "",
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = ""
) {
    val allRoutines = if (routines.isNotEmpty()) routines else listOfNotNull(routine)
    val firstRoutine = allRoutines.firstOrNull() ?: Routine(id = "", name = "", description = "", level = "", objective = "", exercises = emptyList(), days = emptyList())

    paint.isAntiAlias = true
    val greenColor = android.graphics.Color.parseColor("#1B4D3E") // PrimaryGreen
    val lightGreenBg = android.graphics.Color.parseColor("#E8F5E9")
    val borderGray = android.graphics.Color.parseColor("#4B5563") // DarkGray
    val lightGray = android.graphics.Color.parseColor("#E0E0E0")
    val rowAltGray = android.graphics.Color.parseColor("#F9F9F9")

    // 1. ENCABEZADO SUPERIOR (Logo y Encabezado 100% más grandes)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = borderGray
    canvas.drawRect(25f, 15f, 570f, 105f, paint)

    val logoRect = RectF(33f, 22f, 155f, 98f) // Logo +100%
    if (logoBitmap != null) {
        try {
            val bmpW = logoBitmap.width.toFloat()
            val bmpH = logoBitmap.height.toFloat()
            if (bmpW > 0f && bmpH > 0f) {
                val scale = (logoRect.width() / bmpW).coerceAtMost(logoRect.height() / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val left = logoRect.left + (logoRect.width() - scaledW) / 2f
                val top = logoRect.top + (logoRect.height() - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            } else {
                canvas.drawBitmap(logoBitmap, null, logoRect, paint)
            }
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        paint.style = Paint.Style.FILL
        paint.color = greenColor
        canvas.drawRect(logoRect, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 32f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("TG", 94f, 68f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    // Slogan debajo del logo
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 7f
    paint.isFakeBoldText = false
    val sloganText = gymSlogan.ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }
    canvas.drawText(sloganText.take(28), 33f, 102f, paint)

    val headerX = 168f
    // Nombre del gym 100% más grande (38f)
    paint.color = greenColor
    paint.textSize = 38f
    paint.isFakeBoldText = true
    canvas.drawText(gymName.uppercase(), headerX, 55f, paint)

    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    val locationStr = listOf(gymCity, gymCountry).filter { it.isNotBlank() }.joinToString(" - ").uppercase().ifBlank { "BALCARCE - BUENOS AIRES" }
    canvas.drawText(locationStr, headerX, 78f, paint)

    // Título y Subtítulo a la derecha
    paint.textAlign = Paint.Align.RIGHT
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 10f
    paint.isFakeBoldText = true
    val rutinaTitle = customTitle.ifBlank { "RUTINA 3 DÍAS – FORTALECIMIENTO Y REHABILITACIÓN" }
    canvas.drawText(rutinaTitle.take(45), 562f, 45f, paint)

    paint.textSize = 8f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.DKGRAY
    val rutinaSub = customObjective.ifBlank { firstRoutine.objective.ifBlank { "Tratamiento rodillas en valgo y hernia de disco" } }
    canvas.drawText(rutinaSub.take(55), 562f, 62f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 2. ENFOQUE DEL PROGRAMA
    paint.color = lightGreenBg
    paint.style = Paint.Style.FILL
    canvas.drawRect(25f, 110f, 570f, 130f, paint)
    paint.color = android.graphics.Color.parseColor("#2E7D32")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(25f, 110f, 570f, 130f, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.parseColor("#1B5E20")
    paint.textSize = 8f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val focusText = "PROGRAMA ENFOCADO EN: " + customSubObjective.ifBlank { firstRoutine.description.ifBlank { "FUERZA FUNCIONAL – ESTABILIDAD – MOVILIDAD – PROTECCIÓN ARTICULAR – SALUD DE LA COLUMNA" } }.uppercase()
    canvas.drawText(focusText, 297.5f, 123f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 3. DATOS DEL ALUMNO
    paint.color = lightGray
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(25f, 135f, 570f, 157f, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 8.5f
    paint.isFakeBoldText = true
    canvas.drawText("NOMBRE: $userName", 33f, 150f, paint)
    canvas.drawText("FECHA: $dateStr", 220f, 150f, paint)
    val objStr = customObjective.ifBlank { firstRoutine.objective.ifBlank { "General" } }
    canvas.drawText("OBJETIVO: $objStr", 355f, 150f, paint)
    val lvlStr = firstRoutine.level.ifBlank { "General" }
    canvas.drawText("NIVEL: $lvlStr", 485f, 150f, paint)

    // 4. MÓDULO DE EJERCICIOS
    var currentY = 164f

    allRoutines.forEachIndexed { rIdx, r ->
        if (currentY > 640f) return@forEachIndexed

        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        paint.color = greenColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(25f, currentY, 570f, currentY + 20f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("DÍA ${rIdx + 1} · $dayName", 33f, currentY + 14f, paint)

        paint.textAlign = Paint.Align.RIGHT
        val moduloName = when (rIdx) {
            0 -> "MÓDULO I"
            1 -> "MÓDULO II"
            2 -> "MÓDULO III"
            3 -> "MÓDULO IV"
            else -> "MÓDULO ${rIdx + 1}"
        }
        canvas.drawText(moduloName, 562f, currentY + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 20f

        // Tabla Header
        paint.color = lightGray
        canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas.drawText("#", 33f, currentY + 12f, paint)
        canvas.drawText("EJERCICIO", 65f, currentY + 12f, paint)
        canvas.drawText("SERIES | REPETICIONES", 320f, currentY + 12f, paint)
        canvas.drawText("NOTAS TÉCNICAS", 440f, currentY + 12f, paint)

        currentY += 18f

        r.exercises.forEachIndexed { idx, ex ->
            if (currentY > 640f) return@forEachIndexed
            val rowBg = if (idx % 2 == 0) android.graphics.Color.WHITE else rowAltGray
            paint.style = Paint.Style.FILL
            paint.color = rowBg
            canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = lightGray
            paint.strokeWidth = 0.5f
            canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("${idx + 1}", 33f, currentY + 12f, paint)
            canvas.drawText(ex.name, 65f, currentY + 12f, paint)
            val serRep = "${ex.series} | ${ex.reps}"
            canvas.drawText(serRep, 320f, currentY + 12f, paint)
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(ex.notes.ifBlank { "Controlar postura y técnica." }, 440f, currentY + 12f, paint)

            currentY += 18f
        }

        currentY += 10f
    }

    // 5. SECCIÓN INFERIOR: INDICACIONES, REGISTRO, NOTAS
    val footerY = currentY.coerceAtLeast(650f)
    val footerH = 95f

    // Indicaciones Generales (x=25 to 195)
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    paint.strokeWidth = 1f
    canvas.drawRect(25f, footerY, 195f, footerY + footerH, paint)
    paint.style = Paint.Style.FILL
    paint.color = lightGray
    canvas.drawRect(25f, footerY, 195f, footerY + 16f, paint)
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 7.5f
    paint.isFakeBoldText = true
    canvas.drawText("INDICACIONES GENERALES", 30f, footerY + 11f, paint)

    val indList = listOf(
        "• Realizar entrada en calor de 10 min.",
        "• Priorizar técnica antes que peso.",
        "• Aumentar cargas progresivamente.",
        "• Mantener buena hidratación.",
        "• Evitar ejercicios que generen dolor.",
        "• Consultar con profesional ante dudas."
    )
    paint.isFakeBoldText = false
    paint.textSize = 6.5f
    var indY = footerY + 26f
    indList.forEach { ind ->
        canvas.drawText(ind, 28f, indY, paint)
        indY += 10f
    }

    // Registro de Progreso (x=200 to 395)
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    canvas.drawRect(200f, footerY, 395f, footerY + footerH, paint)
    paint.style = Paint.Style.FILL
    paint.color = lightGray
    canvas.drawRect(200f, footerY, 395f, footerY + 16f, paint)
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 7.5f
    paint.isFakeBoldText = true
    canvas.drawText("REGISTRO DE PROGRESO", 245f, footerY + 11f, paint)

    // Tabla Progreso sub-header
    paint.color = greenColor
    canvas.drawRect(200f, footerY + 16f, 395f, footerY + 28f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 6f
    paint.isFakeBoldText = true
    canvas.drawText("FECHA", 215f, footerY + 24f, paint)
    canvas.drawText("DOLOR (0-10)", 255f, footerY + 24f, paint)
    canvas.drawText("RENDIMIENTO", 305f, footerY + 24f, paint)
    canvas.drawText("OBSERVACIONES", 355f, footerY + 24f, paint)

    var progLineY = footerY + 28f
    repeat(4) {
        progLineY += 14f
        paint.color = lightGray
        paint.style = Paint.Style.STROKE
        canvas.drawRect(200f, progLineY - 14f, 395f, progLineY, paint)
    }

    // Notas (x=400 to 570)
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    canvas.drawRect(400f, footerY, 570f, footerY + footerH, paint)
    paint.style = Paint.Style.FILL
    paint.color = lightGray
    canvas.drawRect(400f, footerY, 570f, footerY + 16f, paint)
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 7.5f
    paint.isFakeBoldText = true
    canvas.drawText("NOTAS", 475f, footerY + 11f, paint)

    // 6. PIE CON REDES SOCIALES
    val footerPieY = 765f
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    paint.strokeWidth = 1f
    canvas.drawRect(25f, footerPieY, 570f, footerPieY + 52f, paint)

    // 4 Redes sociales
    paint.style = Paint.Style.FILL
    paint.textSize = 7f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Instagram: ${gymInstagram.ifBlank { "@tugimnasio" }}", 45f, footerPieY + 16f, paint)
    canvas.drawText("Facebook: ${gymFacebook.ifBlank { "/tugimnasio" }}", 180f, footerPieY + 16f, paint)
    canvas.drawText("TikTok: ${gymTikTok.ifBlank { "@tugimnasio_ok" }}", 315f, footerPieY + 16f, paint)
    canvas.drawText("WhatsApp: ${gymPhone.ifBlank { "+54 9 ..." }}", 450f, footerPieY + 16f, paint)

    paint.color = lightGray
    canvas.drawLine(33f, footerPieY + 24f, 562f, footerPieY + 24f, paint)

    // Dirección y Slogan
    paint.textSize = 7f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.DKGRAY
    val addressComplete = listOf(gymAddress, gymCity, gymCountry).filter { it.isNotBlank() }.joinToString(" • ").uppercase().ifBlank { "CALLE 13 ENTRE 16 Y 14, CHAVES N° 564 · BALCARCE, BUENOS AIRES" }
    canvas.drawText(addressComplete.take(65), 33f, footerPieY + 38f, paint)

    paint.textAlign = Paint.Align.RIGHT
    paint.isFakeBoldText = true
    paint.color = greenColor
    canvas.drawText(gymSlogan.ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }, 562f, footerPieY + 38f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawModernaPdfTemplate(
    canvas: Canvas,
    paint: Paint,
    routines: List<Routine> = emptyList(),
    routine: Routine? = null,
    gymName: String,
    gymAddress: String = "",
    gymCity: String = "",
    gymCountry: String = "",
    gymSlogan: String = "DISCIPLINA HOY, RESULTADOS SIEMPRE.",
    gymInstagram: String = "@tugimnasio",
    userName: String,
    dateStr: String,
    logoBitmap: Bitmap? = null,
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = ""
) {
    val allRoutines = if (routines.isNotEmpty()) routines else listOfNotNull(routine)
    val firstRoutine = allRoutines.firstOrNull() ?: Routine(id = "", name = "", description = "", level = "", objective = "", exercises = emptyList(), days = emptyList())

    paint.isAntiAlias = true
    val darkGrayHeader = android.graphics.Color.parseColor("#333333")
    val orangeAccent = android.graphics.Color.parseColor("#E65100")
    val subTextColor = android.graphics.Color.parseColor("#B0BEC5")
    val lightOrangeBg = android.graphics.Color.parseColor("#FFF3E0")
    val tableSubHeaderBg = android.graphics.Color.parseColor("#ECEFF1")
    val borderGray = android.graphics.Color.parseColor("#CBD5E1")
    val rowAltBg = android.graphics.Color.parseColor("#F9F9F9")

    // 1. ENCABEZADO PRINCIPAL (Agrandado 100%, DarkGrayHeader #333333)
    paint.style = Paint.Style.FILL
    paint.color = darkGrayHeader
    canvas.drawRect(25f, 15f, 570f, 115f, paint)

    // Logo +100% Proporcional en caja naranja accent #E65100 (x = 33f to 133f, y = 22f to 102f)
    val logoRect = RectF(33f, 22f, 133f, 102f)
    paint.color = orangeAccent
    canvas.drawRect(logoRect, paint)

    if (logoBitmap != null) {
        try {
            val bmpW = logoBitmap.width.toFloat()
            val bmpH = logoBitmap.height.toFloat()
            if (bmpW > 0f && bmpH > 0f) {
                val scale = (logoRect.width() / bmpW).coerceAtMost(logoRect.height() / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val left = logoRect.left + (logoRect.width() - scaledW) / 2f
                val top = logoRect.top + (logoRect.height() - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            }
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 32f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("TG", 83f, 68f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    val headerX = 142f
    // Nombre del gimnasio +100% más grande (34f)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 34f
    paint.isFakeBoldText = true
    canvas.drawText(gymName.uppercase(), headerX, 58f, paint)

    paint.textSize = 9f
    paint.color = subTextColor
    paint.isFakeBoldText = false
    val locationStr = listOf("CENTRO DE ENTRENAMIENTO", gymCity, gymCountry).filter { it.isNotBlank() }.joinToString(" · ").uppercase()
    canvas.drawText(locationStr.take(55), headerX, 82f, paint)

    // Título y Subtítulo de Rutina (Alineados a la derecha, x = 560f)
    paint.textAlign = Paint.Align.RIGHT
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 11f
    paint.isFakeBoldText = true
    val rutinaTitle = customTitle.ifBlank { "RUTINA DE ENTRENAMIENTO — ${firstRoutine.name.uppercase()}" }
    canvas.drawText(rutinaTitle.take(45), 560f, 48f, paint)

    paint.textSize = 8.5f
    paint.color = subTextColor
    paint.isFakeBoldText = false
    val rutinaSub = customObjective.ifBlank { firstRoutine.objective.ifBlank { "TRATAMIENTO RODILLAS EN VALGO Y HERNIA DE DISCO" } }
    canvas.drawText(rutinaSub.uppercase().take(55), 560f, 68f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 2. ENFOQUE DEL PROGRAMA
    paint.color = lightOrangeBg
    paint.style = Paint.Style.FILL
    canvas.drawRect(25f, 121f, 570f, 141f, paint)

    paint.color = orangeAccent
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(25f, 121f, 570f, 141f, paint)

    paint.style = Paint.Style.FILL
    paint.color = orangeAccent
    paint.textSize = 8f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val focusText = "PROGRAMA ENFOCADO EN: " + customSubObjective.ifBlank { firstRoutine.description.ifBlank { "FUERZA FUNCIONAL • ESTABILIDAD • MOVILIDAD • PROTECCIÓN ARTICULAR • SALUD DE LA COLUMNA" } }.uppercase()
    canvas.drawText(focusText, 297.5f, 134f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 3. DATOS DEL ALUMNO
    paint.color = borderGray
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.8f
    canvas.drawRect(25f, 146f, 570f, 168f, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 8.5f
    paint.isFakeBoldText = true
    canvas.drawText("• NOMBRE: $userName", 33f, 160f, paint)
    canvas.drawText("• FECHA: $dateStr", 220f, 160f, paint)
    val objStr = customObjective.ifBlank { firstRoutine.objective.ifBlank { "General" } }
    canvas.drawText("• OBJETIVO: $objStr", 355f, 160f, paint)
    val lvlStr = firstRoutine.level.ifBlank { "General" }
    canvas.drawText("• NIVEL: $lvlStr", 485f, 160f, paint)

    // 4. MÓDULOS DE EJERCICIOS
    var currentY = 175f

    allRoutines.forEachIndexed { rIdx, r ->
        if (currentY > 640f) return@forEachIndexed

        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        paint.color = darkGrayHeader
        paint.style = Paint.Style.FILL
        canvas.drawRect(25f, currentY, 570f, currentY + 20f, paint)

        // Badge Naranja DÍA X
        paint.color = orangeAccent
        canvas.drawRect(29f, currentY + 3f, 68f, currentY + 17f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("DÍA ${rIdx + 1}", 33f, currentY + 13f, paint)

        paint.textSize = 9f
        canvas.drawText(dayName, 74f, currentY + 14f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = subTextColor
        paint.textSize = 8f
        canvas.drawText("${r.exercises.size} EJERCICIOS", 562f, currentY + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 20f

        // Subtítulos de la Tabla
        paint.color = tableSubHeaderBg
        canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas.drawText("#", 33f, currentY + 12f, paint)
        canvas.drawText("EJERCICIO", 65f, currentY + 12f, paint)
        canvas.drawText("SERIES", 310f, currentY + 12f, paint)
        canvas.drawText("REPETICIONES", 365f, currentY + 12f, paint)
        canvas.drawText("NOTAS TÉCNICAS", 440f, currentY + 12f, paint)

        currentY += 18f

        r.exercises.forEachIndexed { idx, ex ->
            if (currentY > 640f) return@forEachIndexed

            val bg = if (idx % 2 == 0) android.graphics.Color.WHITE else rowAltBg
            paint.style = Paint.Style.FILL
            paint.color = bg
            canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = borderGray
            paint.strokeWidth = 0.5f
            canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

            paint.style = Paint.Style.FILL
            paint.color = orangeAccent
            paint.textSize = 8f
            paint.isFakeBoldText = true
            val exNumStr = String.format("%02d", idx + 1)
            canvas.drawText(exNumStr, 33f, currentY + 12f, paint)

            paint.color = android.graphics.Color.BLACK
            paint.isFakeBoldText = false
            canvas.drawText(ex.name, 65f, currentY + 12f, paint)
            canvas.drawText(ex.series, 310f, currentY + 12f, paint)
            canvas.drawText(ex.reps, 365f, currentY + 12f, paint)
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(ex.notes.ifBlank { "Controlar postura y técnica adecuada." }, 440f, currentY + 12f, paint)

            currentY += 18f
        }

        currentY += 10f
    }

    // 5. SECCIÓN INFERIOR: 3 RECUADROS SEPARADOS
    val footerY = currentY.coerceAtLeast(650f)
    val footerH = 95f

    // Bloque 1: Indicaciones Generales (x = 25f to 195f)
    paint.style = Paint.Style.STROKE
    paint.color = darkGrayHeader
    paint.strokeWidth = 0.8f
    canvas.drawRect(25f, footerY, 195f, footerY + footerH, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("INDICACIONES GENERALES", 30f, footerY + 12f, paint)

    val indList = listOf(
        "• Realizar entrada en calor de 10 minutos.",
        "• Priorizar técnica antes que peso.",
        "• Aumentar cargas progresivamente.",
        "• Mantener buena hidratación y descanso.",
        "• Evitar ejercicios que generen dolor.",
        "• Consultar con profesional ante dudas."
    )
    paint.isFakeBoldText = false
    paint.textSize = 6.8f
    paint.color = android.graphics.Color.DKGRAY
    var indY = footerY + 25f
    indList.forEach { ind ->
        canvas.drawText(ind, 28f, indY, paint)
        indY += 11f
    }

    // Bloque 2: Registro de Progreso (x = 200f to 390f)
    paint.style = Paint.Style.STROKE
    paint.color = darkGrayHeader
    canvas.drawRect(200f, footerY, 390f, footerY + footerH, paint)

    paint.style = Paint.Style.FILL
    paint.color = darkGrayHeader
    canvas.drawRect(200f, footerY, 390f, footerY + 15f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 7.5f
    paint.isFakeBoldText = true
    canvas.drawText("FECHA", 205f, footerY + 11f, paint)
    canvas.drawText("DOLOR (0-10)", 242f, footerY + 11f, paint)
    canvas.drawText("RENDIMIENTO", 295f, footerY + 11f, paint)
    canvas.drawText("OBSERVACIONES", 345f, footerY + 11f, paint)

    var progY = footerY + 15f
    repeat(4) {
        progY += 18f
        paint.style = Paint.Style.STROKE
        paint.color = borderGray
        canvas.drawRect(200f, progY - 18f, 390f, progY, paint)
    }

    // Bloque 3: Notas (x = 395f to 570f)
    paint.style = Paint.Style.STROKE
    paint.color = darkGrayHeader
    canvas.drawRect(395f, footerY, 570f, footerY + footerH, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("NOTAS", 400f, footerY + 12f, paint)

    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 7f
    paint.isFakeBoldText = false
    val notesText = firstRoutine.notes.ifBlank { "Consultar cualquier duda con tu profesor." }
    canvas.drawText(notesText.take(40), 400f, footerY + 28f, paint)

    // 6. PIE DE PÁGINA INSTITUCIONAL
    val footerPieY = 770f
    paint.style = Paint.Style.FILL
    paint.color = darkGrayHeader
    canvas.drawRect(25f, footerPieY, 570f, footerPieY + 22f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 7.5f
    paint.isFakeBoldText = false
    val fullAddressText = listOf(gymAddress, gymCity).filter { it.isNotBlank() }.joinToString(" · ").uppercase().ifBlank { "CALLE 13 ENTRE 16 Y 14, CHAVES N° 564 · BALCARCE, BUENOS AIRES" }
    canvas.drawText(fullAddressText.take(55), 30f, footerPieY + 14f, paint)

    paint.color = android.graphics.Color.parseColor("#80CBC4") // Teal
    paint.isFakeBoldText = true
    canvas.drawText(gymInstagram.ifBlank { "@tugimnasio" }, 310f, footerPieY + 14f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(gymSlogan.ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }, 565f, footerPieY + 14f, paint)
    paint.textAlign = Paint.Align.LEFT

    paint.color = android.graphics.Color.GRAY
    paint.textSize = 7f
    paint.isFakeBoldText = false
    canvas.drawText("Plantilla Moderna — $gymName", 25f, footerPieY + 34f, paint)

    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Página 1 de 1", 570f, footerPieY + 34f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawMinimalistaPdfTemplate(
    canvas: Canvas,
    paint: Paint,
    routines: List<Routine> = emptyList(),
    routine: Routine? = null,
    gymName: String,
    gymAddress: String = "",
    gymCity: String = "",
    gymCountry: String = "",
    gymSlogan: String = "Disciplina hoy, resultados siempre.",
    gymInstagram: String = "@tugimnasio",
    userName: String,
    dateStr: String,
    logoBitmap: Bitmap? = null,
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = ""
) {
    val allRoutines = if (routines.isNotEmpty()) routines else listOfNotNull(routine)
    val firstRoutine = allRoutines.firstOrNull() ?: Routine(id = "", name = "", description = "", level = "", objective = "", exercises = emptyList(), days = emptyList())

    paint.isAntiAlias = true
    val blackColor = android.graphics.Color.BLACK
    val darkGray = android.graphics.Color.parseColor("#374151")
    val lineGray = android.graphics.Color.parseColor("#D1D5DB")
    val borderLight = android.graphics.Color.parseColor("#CBD5E1")

    // 1. ENCABEZADO CON LOGO CENTRADO Y TEXTOS A LOS COSTADOS
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = borderLight
    canvas.drawRect(25f, 15f, 570f, 82f, paint)

    // Bloque Izquierdo: Nombre del Gimnasio y Ubicación
    paint.style = Paint.Style.FILL
    paint.color = blackColor
    paint.textSize = 16f
    paint.isFakeBoldText = true
    canvas.drawText(gymName.uppercase(), 35f, 42f, paint)

    paint.textSize = 8.5f
    paint.color = darkGray
    paint.isFakeBoldText = false
    val locationText = listOf(gymCity, gymCountry).filter { it.isNotBlank() }.joinToString(" · ").uppercase().ifBlank { "BALCARCE · BUENOS AIRES" }
    canvas.drawText(locationText, 35f, 58f, paint)

    // Centro: Espacio reservado para el Logo CENTRADO (x = 252.5f to 342.5f, y = 20f to 75f)
    val logoRect = RectF(252.5f, 20f, 342.5f, 75f)
    if (logoBitmap != null) {
        try {
            val bmpW = logoBitmap.width.toFloat()
            val bmpH = logoBitmap.height.toFloat()
            if (bmpW > 0f && bmpH > 0f) {
                val scale = (logoRect.width() / bmpW).coerceAtMost(logoRect.height() / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val left = logoRect.left + (logoRect.width() - scaledW) / 2f
                val top = logoRect.top + (logoRect.height() - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            } else {
                canvas.drawBitmap(logoBitmap, null, logoRect, paint)
            }
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#F5F5F5")
        canvas.drawRect(logoRect, paint)
        paint.style = Paint.Style.STROKE
        paint.color = darkGray
        paint.strokeWidth = 0.5f
        canvas.drawRect(logoRect, paint)

        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 9f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("[ LOGO ]", 297.5f, 52f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    // Bloque Derecho: Título de Rutina y Subtítulo Clínico
    paint.textAlign = Paint.Align.RIGHT
    paint.color = blackColor
    paint.textSize = 9.5f
    paint.isFakeBoldText = true
    val rutinaTitle = customTitle.ifBlank { "RUTINA DE ENTRENAMIENTO — ${firstRoutine.name.uppercase()}" }
    canvas.drawText(rutinaTitle.take(45), 560f, 40f, paint)

    paint.textSize = 8f
    paint.isFakeBoldText = false
    paint.color = darkGray
    val rutinaSub = customObjective.ifBlank { firstRoutine.objective.ifBlank { "Tratamiento rodillas en valgo y hernia de disco" } }
    canvas.drawText(rutinaSub.take(55), 560f, 56f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 2. ENFOQUE DEL PROGRAMA
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.5f
    paint.color = darkGray
    canvas.drawRect(25f, 88f, 570f, 106f, paint)

    paint.style = Paint.Style.FILL
    paint.color = darkGray
    paint.textSize = 7.5f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val focusText = "PROGRAMA ENFOCADO EN: " + customSubObjective.ifBlank { firstRoutine.description.ifBlank { "FUERZA FUNCIONAL – ESTABILIDAD – MOVILIDAD – PROTECCIÓN ARTICULAR – SALUD DE LA COLUMNA" } }.uppercase()
    canvas.drawText(focusText, 297.5f, 100f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 3. DATOS DEL ALUMNO
    paint.style = Paint.Style.FILL
    paint.color = blackColor
    paint.textSize = 8.5f
    paint.isFakeBoldText = false
    canvas.drawText("Nombre: $userName", 25f, 120f, paint)
    canvas.drawText("Fecha: $dateStr", 220f, 120f, paint)
    val objStr = customObjective.ifBlank { firstRoutine.objective.ifBlank { "General" } }
    canvas.drawText("Objetivo: $objStr", 355f, 120f, paint)
    val lvlStr = firstRoutine.level.ifBlank { "General" }
    canvas.drawText("Nivel: $lvlStr", 485f, 120f, paint)

    // 4. MÓDULOS DE EJERCICIOS
    var currentY = 130f

    allRoutines.forEachIndexed { rIdx, r ->
        if (currentY > 640f) return@forEachIndexed

        paint.style = Paint.Style.STROKE
        paint.color = darkGray
        paint.strokeWidth = 0.5f
        canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

        paint.style = Paint.Style.FILL
        paint.color = blackColor
        paint.textSize = 9f
        paint.isFakeBoldText = true
        val dayNumStr = String.format("%02d", rIdx + 1)
        canvas.drawText("DÍA $dayNumStr", 33f, currentY + 13f, paint)

        paint.color = darkGray
        paint.textAlign = Paint.Align.RIGHT
        val subSection = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        canvas.drawText(subSection, 562f, currentY + 13f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 18f
        paint.color = darkGray
        paint.strokeWidth = 1f
        canvas.drawLine(25f, currentY, 570f, currentY, paint)

        // Header Tabla
        paint.style = Paint.Style.FILL
        paint.color = blackColor
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas.drawText("#", 33f, currentY + 12f, paint)
        canvas.drawText("EJERCICIO", 65f, currentY + 12f, paint)
        canvas.drawText("SERIES", 320f, currentY + 12f, paint)
        canvas.drawText("REPETICIONES", 370f, currentY + 12f, paint)
        canvas.drawText("NOTAS TÉCNICAS", 440f, currentY + 12f, paint)

        currentY += 16f
        paint.color = lineGray
        paint.strokeWidth = 0.5f
        canvas.drawLine(25f, currentY, 570f, currentY, paint)

        r.exercises.forEachIndexed { idx, ex ->
            if (currentY > 640f) return@forEachIndexed

            paint.style = Paint.Style.FILL
            paint.color = blackColor
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("${idx + 1}", 33f, currentY + 12f, paint)
            canvas.drawText(ex.name, 65f, currentY + 12f, paint)
            canvas.drawText(ex.series, 320f, currentY + 12f, paint)
            canvas.drawText(ex.reps, 370f, currentY + 12f, paint)
            paint.color = darkGray
            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 440f, currentY + 12f, paint)

            currentY += 16f
            paint.color = lineGray
            canvas.drawLine(25f, currentY, 570f, currentY, paint)
        }

        currentY += 10f
    }

    // 5. SECCIÓN INFERIOR: INDICACIONES, REGISTRO Y NOTAS (CON LÍNEAS VERTICALES)
    val footerY = currentY.coerceAtLeast(650f)
    val footerH = 95f

    // Borde exterior
    paint.style = Paint.Style.STROKE
    paint.color = darkGray
    paint.strokeWidth = 0.5f
    canvas.drawRect(25f, footerY, 570f, footerY + footerH, paint)

    // Indicaciones Generales (x = 25f to 195f)
    paint.style = Paint.Style.FILL
    paint.color = blackColor
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("INDICACIONES GENERALES", 30f, footerY + 12f, paint)

    val indList = listOf(
        "• Realizar entrada en calor de 10 minutos.",
        "• Priorizar técnica antes que peso.",
        "• Aumentar cargas progresivamente.",
        "• Mantener buena hidratación y descanso.",
        "• Evitar ejercicios que generen dolor.",
        "• Consultar con profesional ante dudas."
    )
    paint.isFakeBoldText = false
    paint.textSize = 6.8f
    paint.color = darkGray
    var indY = footerY + 25f
    indList.forEach { ind ->
        canvas.drawText(ind, 28f, indY, paint)
        indY += 11f
    }

    // Línea divisoria vertical 1
    paint.color = lineGray
    paint.strokeWidth = 0.5f
    canvas.drawLine(198f, footerY, 198f, footerY + footerH, paint)

    // Registro de Progreso (x = 200f to 390f)
    paint.color = blackColor
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("REGISTRO DE PROGRESO", 205f, footerY + 12f, paint)

    paint.textSize = 6.5f
    paint.isFakeBoldText = true
    paint.color = darkGray
    canvas.drawText("FECHA", 205f, footerY + 25f, paint)
    canvas.drawText("DOLOR (0-10)", 242f, footerY + 25f, paint)
    canvas.drawText("RENDIMIENTO", 295f, footerY + 25f, paint)
    canvas.drawText("OBSERVACIONES", 345f, footerY + 25f, paint)

    var progY = footerY + 28f
    repeat(3) {
        progY += 18f
        canvas.drawLine(202f, progY, 390f, progY, paint)
    }

    // Línea divisoria vertical 2
    canvas.drawLine(393f, footerY, 393f, footerY + footerH, paint)

    // Notas (x = 398f to 570f)
    paint.color = blackColor
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("NOTAS", 400f, footerY + 12f, paint)

    paint.color = darkGray
    paint.textSize = 7f
    paint.isFakeBoldText = false
    val notesText = firstRoutine.notes.ifBlank { "Consultar cualquier duda con tu profesor." }
    canvas.drawText(notesText.take(40), 400f, footerY + 28f, paint)

    // 6. PIE DE PÁGINA MINIMALISTA
    val footerPieY = 785f
    paint.textSize = 7f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.GRAY

    val addressLine = gymAddress.ifBlank { "Calle 13 entre 16 y 14, Chaves N° 564 Balcarce, Buenos Aires" }
    canvas.drawText(addressLine.take(50), 25f, footerPieY, paint)

    canvas.drawText(gymInstagram.ifBlank { "@tugimnasio" }, 250f, footerPieY, paint)

    paint.color = blackColor
    paint.isFakeBoldText = true
    val sloganText = gymSlogan.ifBlank { "Disciplina hoy, resultados siempre." }
    canvas.drawText(sloganText, 335f, footerPieY, paint)

    paint.color = android.graphics.Color.GRAY
    paint.isFakeBoldText = false
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Página 1 de 1", 570f, footerPieY, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawClasicaPdfTemplate(
    canvas: Canvas,
    paint: Paint,
    routines: List<Routine> = emptyList(),
    routine: Routine? = null,
    gymName: String,
    gymAddress: String = "",
    gymCity: String = "",
    gymCountry: String = "",
    gymSlogan: String = "DISCIPLINA HOY, RESULTADOS SIEMPRE.",
    gymInstagram: String = "@tugimnasio",
    userName: String,
    dateStr: String,
    logoBitmap: Bitmap? = null,
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = ""
) {
    val allRoutines = if (routines.isNotEmpty()) routines else listOfNotNull(routine)
    val firstRoutine = allRoutines.firstOrNull() ?: Routine(id = "", name = "", description = "", level = "", objective = "", exercises = emptyList(), days = emptyList())

    paint.isAntiAlias = true
    val classicBlue = android.graphics.Color.parseColor("#1A365D") // ClassicBlue
    val borderGray = android.graphics.Color.parseColor("#4A5568") // DarkGray
    val lightBorder = android.graphics.Color.parseColor("#CBD5E1")
    val tableHeaderBg = android.graphics.Color.parseColor("#F7FAFC")
    val rowAltBg = android.graphics.Color.parseColor("#F8FAFC")

    // 1. ENCABEZADO AMPLIADO 100% (Logo, Nombre y Altura al doble)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = borderGray
    canvas.drawRect(25f, 15f, 570f, 115f, paint)

    // Bloque Logo (+100% tamaño: width = 90, height = 80) x = 33f to 123f, y = 22f to 102f
    val logoRect = RectF(33f, 22f, 123f, 102f)
    if (logoBitmap != null) {
        try {
            val bmpW = logoBitmap.width.toFloat()
            val bmpH = logoBitmap.height.toFloat()
            if (bmpW > 0f && bmpH > 0f) {
                val scale = (logoRect.width() / bmpW).coerceAtMost(logoRect.height() / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val left = logoRect.left + (logoRect.width() - scaledW) / 2f
                val top = logoRect.top + (logoRect.height() - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            }
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        paint.style = Paint.Style.FILL
        paint.color = classicBlue
        canvas.drawRect(logoRect, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 32f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("TG", 78f, 68f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    val headerX = 138f
    // Nombre del gimnasio ampliado 100% (32sp -> 32f font size)
    paint.style = Paint.Style.FILL
    paint.color = classicBlue
    paint.textSize = 32f
    paint.isFakeBoldText = true
    canvas.drawText(gymName.uppercase(), headerX, 58f, paint)

    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 10f
    paint.isFakeBoldText = false
    val locationText = listOf(gymCity, gymCountry).filter { it.isNotBlank() }.joinToString(" · ").uppercase().ifBlank { "BALCARCE · BUENOS AIRES" }
    canvas.drawText(locationText, headerX, 78f, paint)

    // Título y Subtítulo de Rutina (Alineados a la derecha, x = 560f)
    paint.textAlign = Paint.Align.RIGHT
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 10.5f
    paint.isFakeBoldText = true
    val rutinaTitle = customTitle.ifBlank { "RUTINA 3 DÍAS – FORTALECIMIENTO Y REHABILITACIÓN" }
    canvas.drawText(rutinaTitle.take(45), 560f, 48f, paint)

    paint.textSize = 8.5f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.DKGRAY
    val rutinaSub = customObjective.ifBlank { firstRoutine.objective.ifBlank { "TRATAMIENTO RODILLAS EN VALGO Y HERNIA DE DISCO" } }
    canvas.drawText(rutinaSub.uppercase().take(55), 560f, 68f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 2. ENFOQUE DEL PROGRAMA
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.5f
    paint.color = borderGray
    canvas.drawRect(25f, 121f, 570f, 139f, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 7.5f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val focusText = "PROGRAMA ENFOCADO EN: " + customSubObjective.ifBlank { firstRoutine.description.ifBlank { "FUERZA FUNCIONAL - ESTABILIDAD - MOVILIDAD - PROTECCIÓN ARTICULAR - SALUD DE LA COLUMNA" } }.uppercase()
    canvas.drawText(focusText, 297.5f, 133f, paint)
    paint.textAlign = Paint.Align.LEFT

    // 3. DATOS DEL ALUMNO
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.5f
    paint.color = lightBorder
    canvas.drawRect(25f, 144f, 570f, 166f, paint)

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 8.5f
    paint.isFakeBoldText = true
    canvas.drawText("NOMBRE: $userName", 33f, 158f, paint)
    canvas.drawText("FECHA: $dateStr", 220f, 158f, paint)
    val objStr = customObjective.ifBlank { firstRoutine.objective.ifBlank { "General" } }
    canvas.drawText("OBJETIVO: $objStr", 355f, 158f, paint)
    val lvlStr = firstRoutine.level.ifBlank { "General" }
    canvas.drawText("NIVEL: $lvlStr", 485f, 158f, paint)

    // 4. MÓDULOS DE EJERCICIOS (DÍA 1, DÍA 2 Y DÍA 3)
    var currentY = 173f

    allRoutines.forEachIndexed { rIdx, r ->
        if (currentY > 640f) return@forEachIndexed

        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        paint.color = classicBlue
        paint.style = Paint.Style.FILL
        canvas.drawRect(25f, currentY, 570f, currentY + 20f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("DÍA ${rIdx + 1} — $dayName", 33f, currentY + 14f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = android.graphics.Color.parseColor("#90CDF4") // Light blue
        paint.textSize = 8f
        val enfoqueText = r.description.ifBlank { "ENFOQUE: TREN INFERIOR Y ESTABILIDAD" }
        canvas.drawText(enfoqueText.uppercase().take(40), 562f, currentY + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 20f

        // Encabezado Tabla
        paint.color = tableHeaderBg
        canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas.drawText("#", 33f, currentY + 12f, paint)
        canvas.drawText("EJERCICIO", 65f, currentY + 12f, paint)
        canvas.drawText("SERIES", 310f, currentY + 12f, paint)
        canvas.drawText("REPETICIONES", 365f, currentY + 12f, paint)
        canvas.drawText("NOTAS TÉCNICAS", 440f, currentY + 12f, paint)

        currentY += 18f

        r.exercises.forEachIndexed { idx, ex ->
            if (currentY > 640f) return@forEachIndexed

            val bg = if (idx % 2 == 0) android.graphics.Color.WHITE else rowAltBg
            paint.style = Paint.Style.FILL
            paint.color = bg
            canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = lightBorder
            paint.strokeWidth = 0.5f
            canvas.drawRect(25f, currentY, 570f, currentY + 18f, paint)

            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 8f
            paint.isFakeBoldText = true
            canvas.drawText("${idx + 1}", 33f, currentY + 12f, paint)

            paint.isFakeBoldText = false
            canvas.drawText(ex.name, 65f, currentY + 12f, paint)
            canvas.drawText(ex.series, 310f, currentY + 12f, paint)
            canvas.drawText(ex.reps, 365f, currentY + 12f, paint)
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(ex.notes.ifBlank { "Controla la postura y técnica." }, 440f, currentY + 12f, paint)

            currentY += 18f
        }

        currentY += 10f
    }

    // 5. SECCIÓN INFERIOR: INDICACIONES, REGISTRO Y NOTAS
    val footerY = currentY.coerceAtLeast(650f)
    val footerH = 95f

    // Indicaciones Generales (x = 25f to 195f)
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    paint.strokeWidth = 0.5f
    canvas.drawRect(25f, footerY, 195f, footerY + footerH, paint)

    paint.style = Paint.Style.FILL
    paint.color = classicBlue
    canvas.drawRect(25f, footerY, 195f, footerY + 16f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("INDICACIONES GENERALES", 30f, footerY + 12f, paint)

    val indList = listOf(
        "✓ Realizar entrada en calor de 10 minutos.",
        "✓ Priorizar técnica antes que peso.",
        "✓ Aumentar cargas progresivamente.",
        "✓ Mantener buena hidratación y descanso.",
        "✓ Evitar ejercicios que generen dolor.",
        "✓ Consultar con profesional ante dudas."
    )
    paint.isFakeBoldText = false
    paint.textSize = 6.8f
    paint.color = android.graphics.Color.DKGRAY
    var indY = footerY + 28f
    indList.forEach { ind ->
        canvas.drawText(ind, 28f, indY, paint)
        indY += 11f
    }

    // Registro de Progreso (x = 200f to 390f)
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    canvas.drawRect(200f, footerY, 390f, footerY + footerH, paint)

    paint.style = Paint.Style.FILL
    paint.color = classicBlue
    canvas.drawRect(200f, footerY, 390f, footerY + 16f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("REGISTRO DE PROGRESO", 212f, footerY + 12f, paint)

    // Subheader
    paint.color = android.graphics.Color.parseColor("#E2E8F0")
    canvas.drawRect(200f, footerY + 16f, 390f, footerY + 28f, paint)
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 6.2f
    paint.isFakeBoldText = true
    canvas.drawText("FECHA", 205f, footerY + 24f, paint)
    canvas.drawText("DOLOR (0-10)", 242f, footerY + 24f, paint)
    canvas.drawText("RENDIMIENTO", 295f, footerY + 24f, paint)
    canvas.drawText("OBSERVACIONES", 345f, footerY + 24f, paint)

    var progY = footerY + 28f
    repeat(4) {
        progY += 16f
        paint.style = Paint.Style.STROKE
        paint.color = lightBorder
        paint.strokeWidth = 0.3f
        canvas.drawRect(200f, progY - 16f, 390f, progY, paint)
    }

    // Notas (x = 395f to 570f)
    paint.style = Paint.Style.STROKE
    paint.color = borderGray
    paint.strokeWidth = 0.5f
    canvas.drawRect(395f, footerY, 570f, footerY + footerH, paint)

    paint.style = Paint.Style.FILL
    paint.color = classicBlue
    canvas.drawRect(395f, footerY, 570f, footerY + 16f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 8f
    paint.isFakeBoldText = true
    canvas.drawText("NOTAS", 400f, footerY + 12f, paint)

    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 7f
    paint.isFakeBoldText = false
    val notesText = firstRoutine.notes.ifBlank { "Consultar cualquier duda con tu profesor." }
    canvas.drawText(notesText.take(40), 400f, footerY + 28f, paint)

    // 6. PIE DE PÁGINA CLÁSICO
    val footerPieY = 770f
    paint.style = Paint.Style.STROKE
    paint.color = lightBorder
    paint.strokeWidth = 0.5f
    canvas.drawRect(25f, footerPieY, 570f, footerPieY + 22f, paint)

    paint.style = Paint.Style.FILL
    paint.textSize = 7f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.DKGRAY
    val addressComplete = listOf(gymAddress, gymCity).filter { it.isNotBlank() }.joinToString(" · ").uppercase().ifBlank { "CALLE 13 ENTRE 16 Y 14, CHAVES N° 564 · BALCARCE, BUENOS AIRES" }
    canvas.drawText(addressComplete.take(55), 30f, footerPieY + 14f, paint)

    paint.color = classicBlue
    paint.isFakeBoldText = true
    canvas.drawText(gymInstagram.ifBlank { "@tugimnasio" }, 310f, footerPieY + 14f, paint)

    paint.color = android.graphics.Color.BLACK
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(gymSlogan.ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }, 565f, footerPieY + 14f, paint)
    paint.textAlign = Paint.Align.LEFT

    paint.color = android.graphics.Color.GRAY
    paint.textSize = 7f
    paint.isFakeBoldText = false
    canvas.drawText("Plantilla Clásica Ampliada — $gymName", 25f, footerPieY + 34f, paint)

    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Página 1 de 1", 570f, footerPieY + 34f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun getWeekdayOrderIndex(routine: com.zexo.gymcopilot.model.Routine): Int {
    val dayMap = mapOf(
        "lunes" to 1, "lun" to 1, "monday" to 1, "día 1" to 1, "dia 1" to 1,
        "martes" to 2, "mar" to 2, "tuesday" to 2, "día 2" to 2, "dia 2" to 2,
        "miércoles" to 3, "miercoles" to 3, "mié" to 3, "wednesday" to 3, "día 3" to 3, "dia 3" to 3,
        "jueves" to 4, "jue" to 4, "thursday" to 4, "día 4" to 4, "dia 4" to 4,
        "viernes" to 5, "vie" to 5, "friday" to 5, "día 5" to 5, "dia 5" to 5,
        "sábado" to 6, "sabado" to 6, "sáb" to 6, "saturday" to 6, "día 6" to 6, "dia 6" to 6,
        "domingo" to 7, "dom" to 7, "sunday" to 7, "día 7" to 7, "dia 7" to 7
    )

    for (d in routine.days) {
        val key = d.lowercase().trim()
        if (dayMap.containsKey(key)) return dayMap[key]!!
    }

    val nameKey = routine.name.lowercase().trim()
    for ((key, index) in dayMap) {
        if (nameKey.contains(key)) return index
    }

    return 99
}

private fun generateAndShareMultiRoutinePdf(
    context: Context,
    routines: List<com.zexo.gymcopilot.model.Routine>,
    dataStoreManager: DataStoreManager,
    scope: CoroutineScope,
    customTitle: String = "",
    customObjective: String = "",
    customSubObjective: String = ""
) {
    scope.launch(Dispatchers.IO) {
        try {
            val gymName = dataStoreManager.getGymName().first().ifBlank { "GIMNASIO COPILOT" }
            val gymAddress = dataStoreManager.getGymAddress().first().ifBlank { "Av. Principal 1234, Centro" }
            val gymCity = dataStoreManager.getGymCity().first().ifBlank { gymAddress.substringAfterLast(",").trim().ifBlank { "BUENOS AIRES" } }
            val gymPhone = dataStoreManager.getGymPhone().first()
            val gymSlogan = dataStoreManager.getGymSlogan().first().ifBlank { "DISCIPLINA HOY, RESULTADOS SIEMPRE." }
            val gymInstagram = dataStoreManager.getGymInstagram().first().ifBlank { "@tugimnasio" }
            val userName = dataStoreManager.getUserName().first().ifBlank { "Socio / Alumno" }
            val logoUriStr = dataStoreManager.getGymLogoUri().first()
            val template = dataStoreManager.getRoutinePdfTemplate().first()

            val logoBitmap: Bitmap? = try {
                if (!logoUriStr.isNullOrBlank()) {
                    if (logoUriStr.startsWith("data:image")) {
                        val base64 = logoUriStr.substringAfter(",")
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else {
                        val uri = Uri.parse(logoUriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                } else null
            } catch (e: Exception) { null }

            val anatomyBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.anatomia_muscular)
            } catch (e: Exception) { null }

            val currentDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val sortedRoutines = routines.sortedWith(
                compareBy<com.zexo.gymcopilot.model.Routine> { getWeekdayOrderIndex(it) }
                    .thenBy { it.name }
            )

            val safeUserName = userName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Socio" }
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

            if (template == "Ejecutiva") {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.isAntiAlias = true

                val gymZip = dataStoreManager.getGymPostalCode().first().ifBlank { "" }
                val gymCountry = dataStoreManager.getGymCountry().first().ifBlank { "" }
                val gymFacebook = dataStoreManager.getGymFacebook().first().ifBlank { "" }
                val gymTikTok = dataStoreManager.getGymTikTok().first().ifBlank { "" }

                drawEjecutivaPdfTemplate(
                    canvas = canvas,
                    paint = paint,
                    routines = sortedRoutines,
                    gymName = gymName,
                    gymAddress = gymAddress,
                    userName = userName,
                    dateStr = currentDateStr,
                    logoBitmap = logoBitmap,
                    gymCity = gymCity,
                    gymPhone = gymPhone,
                    gymZip = gymZip,
                    gymCountry = gymCountry,
                    gymSlogan = gymSlogan,
                    gymInstagram = gymInstagram,
                    gymFacebook = gymFacebook,
                    gymTikTok = gymTikTok,
                    customTitle = customTitle,
                    customObjective = customObjective,
                    customSubObjective = customSubObjective
                )
                pdfDocument.finishPage(page)

                val fileName = "Rutinas_Semana_$safeUserName.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    shareRoutinePdfFile(context, file, "Rutinas Semanales")
                }
                return@launch
            }

            if (template == "Minimalista") {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.isAntiAlias = true

                val gymCountry = dataStoreManager.getGymCountry().first().ifBlank { "" }

                drawMinimalistaPdfTemplate(
                    canvas = canvas,
                    paint = paint,
                    routines = sortedRoutines,
                    gymName = gymName,
                    gymAddress = gymAddress,
                    gymCity = gymCity,
                    gymCountry = gymCountry,
                    gymSlogan = gymSlogan,
                    gymInstagram = gymInstagram,
                    userName = userName,
                    dateStr = currentDateStr,
                    logoBitmap = logoBitmap,
                    customTitle = customTitle,
                    customObjective = customObjective,
                    customSubObjective = customSubObjective
                )
                pdfDocument.finishPage(page)

                val fileName = "Rutinas_Semana_$safeUserName.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    shareRoutinePdfFile(context, file, "Rutinas Semanales")
                }
                return@launch
            }

            if (template == "Moderna") {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.isAntiAlias = true

                val gymCountry = dataStoreManager.getGymCountry().first().ifBlank { "" }

                drawModernaPdfTemplate(
                    canvas = canvas,
                    paint = paint,
                    routines = sortedRoutines,
                    gymName = gymName,
                    gymAddress = gymAddress,
                    gymCity = gymCity,
                    gymCountry = gymCountry,
                    gymSlogan = gymSlogan,
                    gymInstagram = gymInstagram,
                    userName = userName,
                    dateStr = currentDateStr,
                    logoBitmap = logoBitmap,
                    customTitle = customTitle,
                    customObjective = customObjective,
                    customSubObjective = customSubObjective
                )
                pdfDocument.finishPage(page)

                val fileName = "Rutinas_Semana_$safeUserName.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    shareRoutinePdfFile(context, file, "Rutinas Semanales")
                }
                return@launch
            }

            if (template == "Clásica" || template == "Clasica") {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.isAntiAlias = true

                val gymCountry = dataStoreManager.getGymCountry().first().ifBlank { "" }

                drawClasicaPdfTemplate(
                    canvas = canvas,
                    paint = paint,
                    routines = sortedRoutines,
                    gymName = gymName,
                    gymAddress = gymAddress,
                    gymCity = gymCity,
                    gymCountry = gymCountry,
                    gymSlogan = gymSlogan,
                    gymInstagram = gymInstagram,
                    userName = userName,
                    dateStr = currentDateStr,
                    logoBitmap = logoBitmap,
                    customTitle = customTitle,
                    customObjective = customObjective,
                    customSubObjective = customSubObjective
                )
                pdfDocument.finishPage(page)

                val fileName = "Rutinas_Semana_$safeUserName.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    shareRoutinePdfFile(context, file, "Rutinas Semanales")
                }
                return@launch
            }

            if (template == "Profesional" || template == "Alfa Balcarce") {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.isAntiAlias = true

                drawProfesionalPdfTemplate(
                    canvas = canvas,
                    paint = paint,
                    routines = sortedRoutines,
                    gymName = gymName,
                    gymAddress = gymAddress,
                    gymCity = gymCity,
                    gymPhone = gymPhone,
                    userName = userName,
                    dateStr = currentDateStr,
                    logoBitmap = logoBitmap,
                    gymSlogan = gymSlogan,
                    gymInstagram = gymInstagram,
                    customTitle = customTitle,
                    customObjective = customObjective,
                    customSubObjective = customSubObjective,
                    anatomyBitmap = anatomyBitmap
                )
                pdfDocument.finishPage(page)

                val fileName = "Rutinas_Semana_$safeUserName.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    shareRoutinePdfFile(context, file, "Rutinas Semanales")
                }
                return@launch
            }

            var pageNumber = 1
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var paint = Paint()
            paint.isAntiAlias = true

            var currentY = 20f

            fun checkPageBreak(neededHeight: Float = 40f) {
                if (currentY + neededHeight > 770f) {
                    paint.color = android.graphics.Color.GRAY
                    paint.textSize = 8f
                    canvas.drawText("Página $pageNumber — $gymName", 480f, 815f, paint)

                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    paint = Paint()
                    paint.isAntiAlias = true
                    currentY = 30f
                }
            }

            when (template) {
                "Minimalista" -> {
                    val blackColor = android.graphics.Color.BLACK
                    val darkGray = android.graphics.Color.parseColor("#374151")
                    val lineGray = android.graphics.Color.parseColor("#D1D5DB")

                    paint.color = blackColor
                    paint.textSize = 15f
                    paint.isFakeBoldText = true
                    canvas.drawText(gymName.uppercase(), 40f, 52f, paint)

                    paint.textSize = 7.5f
                    paint.color = darkGray
                    paint.isFakeBoldText = false
                    canvas.drawText("CENTRO DE ENTRENAMIENTO", 40f, 66f, paint)

                    paint.color = blackColor
                    paint.textSize = 9.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA SEMANAL DE RUTINAS", 310f, 50f, paint)

                    paint.textSize = 7.5f
                    paint.color = darkGray
                    paint.isFakeBoldText = false
                    canvas.drawText("SOCIO: ${userName.uppercase()} ($currentDateStr)", 310f, 64f, paint)

                    // Barra 1: Enfoque
                    paint.color = lineGray
                    paint.strokeWidth = 0.6f
                    canvas.drawLine(40f, 76f, 555f, 76f, paint)
                    canvas.drawLine(40f, 94f, 555f, 94f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = blackColor
                    paint.textSize = 7.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA ENFOCADO EN: FUERZA FUNCIONAL • ESTABILIDAD • MOVILIDAD • SALUD DE LA COLUMNA", 48f, 88f, paint)

                    // Barra 2: Metadatos
                    paint.color = darkGray
                    paint.textSize = 8f
                    paint.isFakeBoldText = true
                    canvas.drawText("Nombre:", 40f, 110f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText(userName, 82f, 110f, paint)
                    paint.color = lineGray
                    canvas.drawLine(80f, 114f, 210f, 114f, paint)

                    paint.color = darkGray
                    paint.isFakeBoldText = true
                    canvas.drawText("Fecha:", 220f, 110f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText(currentDateStr, 255f, 110f, paint)
                    paint.color = lineGray
                    canvas.drawLine(252f, 114f, 320f, 114f, paint)

                    paint.color = darkGray
                    paint.isFakeBoldText = true
                    canvas.drawText("Objetivo:", 330f, 110f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 372f, 110f, paint)
                    paint.color = lineGray
                    canvas.drawLine(370f, 114f, 450f, 114f, paint)

                    paint.color = darkGray
                    paint.isFakeBoldText = true
                    canvas.drawText("Nivel:", 460f, 110f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 490f, 110f, paint)
                    paint.color = lineGray
                    canvas.drawLine(488f, 114f, 555f, 114f, paint)

                    currentY = 126f
                }
                "Moderna" -> {
                    val orangeColor = android.graphics.Color.parseColor("#FF6B00")
                    val darkSlate = android.graphics.Color.parseColor("#0F172A")
                    val borderGray = android.graphics.Color.parseColor("#E2E8F0")

                    paint.color = orangeColor
                    canvas.drawRect(0f, 0f, 14f, 842f, paint)

                    if (logoBitmap != null) {
                        try {
                            val destRect = RectF(32f, 32f, 68f, 68f)
                            canvas.drawBitmap(logoBitmap, null, destRect, paint)
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        paint.color = orangeColor
                        canvas.drawRoundRect(32f, 32f, 66f, 66f, 6f, 6f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 12f
                        paint.isFakeBoldText = true
                        canvas.drawText("TG", 40f, 52f, paint)
                    }

                    val headerX = if (logoBitmap != null) 76f else 74f
                    paint.color = darkSlate
                    paint.textSize = 15f
                    paint.isFakeBoldText = true
                    canvas.drawText(gymName.uppercase(), headerX, 48f, paint)

                    paint.textSize = 7.5f
                    paint.color = android.graphics.Color.parseColor("#64748B")
                    paint.isFakeBoldText = false
                    canvas.drawText("CENTRO DE ENTRENAMIENTO", headerX, 62f, paint)

                    paint.color = darkSlate
                    paint.textSize = 9.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA SEMANAL DE RUTINAS", 310f, 48f, paint)

                    paint.textSize = 7.5f
                    paint.color = orangeColor
                    paint.isFakeBoldText = false
                    canvas.drawText("TRATAMIENTO DE COLUMNA, VALGO Y HERNIA DE DISCO", 310f, 62f, paint)

                    // Barra 1: Enfoque
                    paint.color = android.graphics.Color.parseColor("#FFF7ED")
                    canvas.drawRect(32f, 74f, 565f, 92f, paint)
                    paint.color = android.graphics.Color.parseColor("#FFEDD5")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawRect(32f, 74f, 565f, 92f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.parseColor("#C2410C")
                    paint.textSize = 7.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA ENFOCADO EN: FUERZA FUNCIONAL • ESTABILIDAD • MOVILIDAD • SALUD DE LA COLUMNA", 40f, 86f, paint)

                    // Barra 2: Metadatos
                    paint.color = android.graphics.Color.parseColor("#F8FAFC")
                    canvas.drawRect(32f, 98f, 565f, 116f, paint)
                    paint.color = borderGray
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(32f, 98f, 565f, 116f, paint)

                    paint.style = Paint.Style.FILL
                    paint.textSize = 8f
                    paint.color = orangeColor
                    paint.isFakeBoldText = true
                    canvas.drawText("• NOMBRE:", 40f, 110f, paint)
                    paint.color = darkSlate
                    paint.isFakeBoldText = false
                    canvas.drawText(userName, 92f, 110f, paint)

                    paint.color = orangeColor
                    paint.isFakeBoldText = true
                    canvas.drawText("• FECHA:", 220f, 110f, paint)
                    paint.color = darkSlate
                    paint.isFakeBoldText = false
                    canvas.drawText(currentDateStr, 260f, 110f, paint)

                    paint.color = orangeColor
                    paint.isFakeBoldText = true
                    canvas.drawText("• OBJETIVO:", 330f, 110f, paint)
                    paint.color = darkSlate
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 385f, 110f, paint)

                    paint.color = orangeColor
                    paint.isFakeBoldText = true
                    canvas.drawText("• NIVEL:", 465f, 110f, paint)
                    paint.color = darkSlate
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 502f, 110f, paint)

                    currentY = 124f
                }
                "Ejecutiva" -> {
                    val greenColor = android.graphics.Color.parseColor("#1E4D2B")
                    val lightGreenBg = android.graphics.Color.parseColor("#F2F7F4")
                    val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                    if (logoBitmap != null) {
                        try {
                            val destRect = RectF(35f, 32f, 70f, 68f)
                            canvas.drawBitmap(logoBitmap, null, destRect, paint)
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        paint.color = greenColor
                        canvas.drawRect(35f, 32f, 68f, 68f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 12f
                        paint.isFakeBoldText = true
                        canvas.drawText("TG", 43f, 54f, paint)
                    }

                    val headerX = if (logoBitmap != null) 78f else 76f
                    paint.color = greenColor
                    paint.textSize = 15f
                    paint.isFakeBoldText = true
                    canvas.drawText(gymName.uppercase(), headerX, 48f, paint)

                    paint.textSize = 7.5f
                    paint.isFakeBoldText = false
                    canvas.drawText(gymAddress.take(45), headerX, 62f, paint)

                    paint.color = greenColor
                    paint.textSize = 9.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA SEMANAL DE RUTINAS", 310f, 48f, paint)

                    paint.textSize = 7.5f
                    paint.isFakeBoldText = false
                    canvas.drawText("Tratamiento rodillas en valgo y hernia de disco", 310f, 62f, paint)

                    // Barra 1: Enfoque
                    paint.color = lightGreenBg
                    canvas.drawRect(35f, 74f, 560f, 92f, paint)
                    paint.color = android.graphics.Color.parseColor("#E1EFE5")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawRect(35f, 74f, 560f, 92f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = greenColor
                    paint.textSize = 7.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA ENFOCADO EN: FUERZA FUNCIONAL – ESTABILIDAD – MOVILIDAD – PROTECCIÓN ARTICULAR – SALUD DE LA COLUMNA", 42f, 86f, paint)

                    // Barra 2: Metadatos
                    paint.color = android.graphics.Color.parseColor("#F8FAFC")
                    canvas.drawRect(35f, 98f, 560f, 116f, paint)
                    paint.color = borderGray
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(35f, 98f, 560f, 116f, paint)

                    paint.style = Paint.Style.FILL
                    paint.textSize = 8f
                    paint.color = greenColor
                    paint.isFakeBoldText = true
                    canvas.drawText("NOMBRE:", 42f, 110f, paint)
                    paint.color = android.graphics.Color.parseColor("#0F172A")
                    paint.isFakeBoldText = false
                    canvas.drawText(userName, 92f, 110f, paint)

                    paint.color = greenColor
                    paint.isFakeBoldText = true
                    canvas.drawText("FECHA:", 220f, 110f, paint)
                    paint.color = android.graphics.Color.parseColor("#0F172A")
                    paint.isFakeBoldText = false
                    canvas.drawText(currentDateStr, 258f, 110f, paint)

                    paint.color = greenColor
                    paint.isFakeBoldText = true
                    canvas.drawText("OBJETIVO:", 330f, 110f, paint)
                    paint.color = android.graphics.Color.parseColor("#0F172A")
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 382f, 110f, paint)

                    paint.color = greenColor
                    paint.isFakeBoldText = true
                    canvas.drawText("NIVEL:", 460f, 110f, paint)
                    paint.color = android.graphics.Color.parseColor("#0F172A")
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 495f, 110f, paint)

                    currentY = 124f
                }
                "Profesional", "Alfa Balcarce" -> {
                    val blackColor = android.graphics.Color.BLACK
                    val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                    // 1. BANNER CABECERA NEGRA
                    paint.color = blackColor
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(20f, 20f, 575f, 92f, paint)

                    if (logoBitmap != null) {
                        try {
                            val destRect = RectF(28f, 28f, 75f, 75f)
                            canvas.drawBitmap(logoBitmap, null, destRect, paint)
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        paint.color = android.graphics.Color.parseColor("#1F2937")
                        canvas.drawRoundRect(28f, 28f, 75f, 75f, 6f, 6f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 10f
                        paint.isFakeBoldText = true
                        canvas.drawText("TU TG", 36f, 50f, paint)
                        paint.textSize = 6f
                        canvas.drawText("GIMNASIO", 33f, 62f, paint)
                    }

                    val headerX = 85f
                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 16f
                    paint.isFakeBoldText = true
                    canvas.drawText(gymName.uppercase(), headerX, 48f, paint)

                    paint.textSize = 8f
                    paint.color = android.graphics.Color.parseColor("#00CED1")
                    paint.isFakeBoldText = false
                    canvas.drawText("★  B A L C A R C E  ★", headerX, 64f, paint)

                    paint.color = android.graphics.Color.parseColor("#1F2937")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRect(485f, 28f, 565f, 75f, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 7.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("FITNESS", 502f, 44f, paint)
                    canvas.drawText("H", 520f, 58f, paint)
                    paint.textSize = 6.5f
                    canvas.drawText("PRO", 516f, 68f, paint)

                    paint.textSize = 8.5f
                    paint.color = android.graphics.Color.WHITE
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA SEMANAL DE RUTINAS", headerX, 82f, paint)

                    // Barra 1: Enfoque
                    paint.color = blackColor
                    canvas.drawRect(20f, 98f, 575f, 116f, paint)

                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 7.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("PROGRAMA ENFOCADO EN: FUERZA FUNCIONAL – ESTABILIDAD – MOVILIDAD – PROTECCIÓN ARTICULAR – SALUD DE LA COLUMNA", 28f, 110f, paint)

                    // Barra 2: Metadatos
                    paint.color = android.graphics.Color.parseColor("#F0F4F8")
                    canvas.drawRect(20f, 122f, 575f, 140f, paint)
                    paint.color = borderGray
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawRect(20f, 122f, 575f, 140f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = blackColor
                    paint.textSize = 8f
                    paint.isFakeBoldText = true
                    canvas.drawText("NOMBRE:", 28f, 134f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText(userName, 78f, 134f, paint)

                    paint.isFakeBoldText = true
                    canvas.drawText("FECHA:", 210f, 134f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText(currentDateStr, 248f, 134f, paint)

                    paint.isFakeBoldText = true
                    canvas.drawText("OBJETIVO:", 330f, 134f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 382f, 134f, paint)

                    paint.isFakeBoldText = true
                    canvas.drawText("NIVEL:", 470f, 134f, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText("General", 505f, 134f, paint)

                    currentY = 148f
                }
                else -> { // Clásica
                    paint.color = android.graphics.Color.BLACK
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f
                    canvas.drawRect(30f, 30f, 565f, 812f, paint)
                    paint.style = Paint.Style.FILL

                    if (logoBitmap != null) {
                        try {
                            val destRect = RectF(45f, currentY + 10f, 95f, currentY + 60f)
                            canvas.drawBitmap(logoBitmap, null, destRect, paint)
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    val headerX = if (logoBitmap != null) 105f else 45f
                    paint.textSize = 16f
                    paint.isFakeBoldText = true
                    canvas.drawText(gymName.uppercase(), headerX, currentY + 28f, paint)

                    paint.textSize = 10f
                    paint.isFakeBoldText = false
                    canvas.drawText("PROGRAMA SEMANAL — SOCIO: $userName ($currentDateStr)", headerX, currentY + 46f, paint)

                    canvas.drawLine(45f, currentY + 60f, 550f, currentY + 60f, paint)
                    currentY += 75f
                }
            }

            sortedRoutines.forEach { routine ->
                checkPageBreak(90f)

                val dayName = if (routine.days.isNotEmpty()) routine.days.joinToString(", ").uppercase() else routine.name.uppercase()

                when (template) {
                    "Minimalista" -> {
                        val blackColor = android.graphics.Color.BLACK
                        val darkGray = android.graphics.Color.parseColor("#374151")
                        val lineGray = android.graphics.Color.parseColor("#D1D5DB")
                        val lightLine = android.graphics.Color.parseColor("#E5E7EB")

                        paint.color = blackColor
                        paint.textSize = 10f
                        paint.isFakeBoldText = true
                        canvas.drawText("DÍA 01", 40f, currentY + 12f, paint)

                        paint.color = darkGray
                        paint.textSize = 9.5f
                        canvas.drawText(dayName, 410f, currentY + 12f, paint)

                        paint.color = blackColor
                        paint.strokeWidth = 0.8f
                        canvas.drawLine(40f, currentY + 16f, 555f, currentY + 16f, paint)

                        currentY += 22f

                        // Encabezado Tabla
                        paint.color = blackColor
                        paint.textSize = 8f
                        paint.isFakeBoldText = true
                        canvas.drawText("#", 40f, currentY + 12f, paint)
                        canvas.drawText("EJERCICIO", 65f, currentY + 12f, paint)
                        canvas.drawText("SERIES", 290f, currentY + 12f, paint)
                        canvas.drawText("REPETICIONES", 350f, currentY + 12f, paint)
                        canvas.drawText("NOTAS TÉCNICAS", 430f, currentY + 12f, paint)

                        paint.color = lineGray
                        paint.strokeWidth = 0.5f
                        canvas.drawLine(40f, currentY + 16f, 555f, currentY + 16f, paint)

                        currentY += 18f

                        routine.exercises.forEachIndexed { idx, ex ->
                            checkPageBreak(25f)

                            paint.color = blackColor
                            paint.textSize = 8.5f
                            paint.isFakeBoldText = true
                            canvas.drawText("${idx + 1}", 40f, currentY + 14f, paint)

                            paint.isFakeBoldText = false
                            canvas.drawText(ex.name, 65f, currentY + 14f, paint)
                            canvas.drawText(ex.series, 305f, currentY + 14f, paint)
                            canvas.drawText(ex.reps, 370f, currentY + 14f, paint)
                            paint.color = darkGray
                            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 430f, currentY + 14f, paint)

                            paint.color = lightLine
                            canvas.drawLine(40f, currentY + 18f, 555f, currentY + 18f, paint)

                            currentY += 20f
                        }
                    }
                    "Moderna" -> {
                        val orangeColor = android.graphics.Color.parseColor("#FF6B00")
                        val darkSlate = android.graphics.Color.parseColor("#0F172A")
                        val cardSlate = android.graphics.Color.parseColor("#1E293B")
                        val borderGray = android.graphics.Color.parseColor("#E2E8F0")

                        paint.color = darkSlate
                        canvas.drawRoundRect(32f, currentY, 565f, currentY + 20f, 4f, 4f, paint)

                        paint.color = orangeColor
                        canvas.drawRoundRect(36f, currentY + 3f, 75f, currentY + 17f, 3f, 3f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 8f
                        paint.isFakeBoldText = true
                        canvas.drawText("DÍA", 42f, currentY + 13f, paint)

                        paint.textSize = 9.5f
                        canvas.drawText(dayName, 82f, currentY + 14f, paint)

                        paint.textSize = 8f
                        paint.color = orangeColor
                        canvas.drawText("ENFOQUE: ${routine.objective.ifBlank { "TREN INFERIOR Y ESTABILIDAD" }}", 345f, currentY + 14f, paint)

                        currentY += 20f

                        paint.color = cardSlate
                        canvas.drawRect(32f, currentY, 565f, currentY + 18f, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 8f
                        paint.isFakeBoldText = true
                        canvas.drawText("#", 40f, currentY + 13f, paint)
                        canvas.drawText("EJERCICIO", 68f, currentY + 13f, paint)
                        canvas.drawText("SERIES", 290f, currentY + 13f, paint)
                        canvas.drawText("REPETICIONES", 350f, currentY + 13f, paint)
                        canvas.drawText("NOTAS TÉCNICAS", 430f, currentY + 13f, paint)

                        currentY += 18f

                        routine.exercises.forEachIndexed { idx, ex ->
                            checkPageBreak(25f)

                            paint.color = if (idx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F8FAFC")
                            canvas.drawRect(32f, currentY, 565f, currentY + 20f, paint)

                            paint.color = borderGray
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 0.5f
                            canvas.drawRect(32f, currentY, 565f, currentY + 20f, paint)

                            paint.style = Paint.Style.FILL

                            paint.color = orangeColor
                            canvas.drawCircle(45f, currentY + 10f, 6.5f, paint)

                            paint.color = android.graphics.Color.WHITE
                            paint.textSize = 7.5f
                            paint.isFakeBoldText = true
                            canvas.drawText("${idx + 1}", 42f, currentY + 13f, paint)

                            paint.color = darkSlate
                            paint.textSize = 8.5f
                            paint.isFakeBoldText = true
                            canvas.drawText(ex.name, 68f, currentY + 14f, paint)

                            paint.isFakeBoldText = false
                            canvas.drawText(ex.series, 305f, currentY + 14f, paint)
                            canvas.drawText(ex.reps, 370f, currentY + 14f, paint)
                            paint.color = android.graphics.Color.parseColor("#475569")
                            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 430f, currentY + 14f, paint)

                            currentY += 20f
                        }
                    }
                    "Ejecutiva" -> {
                        val greenColor = android.graphics.Color.parseColor("#1E4D2B")
                        val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                        paint.color = greenColor
                        canvas.drawRect(35f, currentY, 560f, currentY + 20f, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 9.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("DÍA 1 · $dayName", 42f, currentY + 14f, paint)

                        paint.textSize = 8.5f
                        canvas.drawText("MÓDULO I", 505f, currentY + 14f, paint)

                        currentY += 20f

                        // Encabezado Tabla
                        paint.color = android.graphics.Color.parseColor("#EBF3ED")
                        canvas.drawRect(35f, currentY, 560f, currentY + 18f, paint)

                        paint.color = greenColor
                        paint.textSize = 8f
                        paint.isFakeBoldText = true
                        canvas.drawText("#", 42f, currentY + 13f, paint)
                        canvas.drawText("EJERCICIO", 70f, currentY + 13f, paint)
                        canvas.drawText("SERIES", 290f, currentY + 13f, paint)
                        canvas.drawText("REPETICIONES", 350f, currentY + 13f, paint)
                        canvas.drawText("NOTAS TÉCNICAS", 430f, currentY + 13f, paint)

                        currentY += 18f

                        routine.exercises.forEachIndexed { idx, ex ->
                            checkPageBreak(25f)

                            paint.color = if (idx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F8FAF8")
                            canvas.drawRect(35f, currentY, 560f, currentY + 20f, paint)

                            paint.color = borderGray
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 0.5f
                            canvas.drawRect(35f, currentY, 560f, currentY + 20f, paint)

                            paint.style = Paint.Style.FILL
                            paint.color = greenColor
                            paint.textSize = 8.5f
                            paint.isFakeBoldText = true
                            canvas.drawText("${idx + 1}", 42f, currentY + 14f, paint)

                            paint.color = android.graphics.Color.parseColor("#0F172A")
                            canvas.drawText(ex.name, 70f, currentY + 14f, paint)

                            paint.isFakeBoldText = false
                            canvas.drawText(ex.series, 305f, currentY + 14f, paint)
                            canvas.drawText(ex.reps, 370f, currentY + 14f, paint)
                            paint.color = android.graphics.Color.parseColor("#374151")
                            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 430f, currentY + 14f, paint)

                            currentY += 20f
                        }
                    }
                    "Clásica" -> {
                        val navyColor = android.graphics.Color.parseColor("#1B365D")
                        val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                        paint.color = navyColor
                        canvas.drawRect(40f, currentY, 555f, currentY + 20f, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 9.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("DÍA / SECCIÓN: $dayName", 48f, currentY + 14f, paint)

                        paint.textSize = 8f
                        canvas.drawText("ENFOQUE: ${routine.objective.ifBlank { "TREN INFERIOR Y ESTABILIDAD" }}", 340f, currentY + 14f, paint)

                        currentY += 20f

                        paint.color = android.graphics.Color.parseColor("#2C4D75")
                        canvas.drawRect(40f, currentY, 555f, currentY + 18f, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 8f
                        paint.isFakeBoldText = true
                        canvas.drawText("#", 48f, currentY + 13f, paint)
                        canvas.drawText("EJERCICIO", 75f, currentY + 13f, paint)
                        canvas.drawText("SERIES", 290f, currentY + 13f, paint)
                        canvas.drawText("REPETICIONES", 350f, currentY + 13f, paint)
                        canvas.drawText("NOTAS TÉCNICAS", 430f, currentY + 13f, paint)

                        currentY += 18f

                        routine.exercises.forEachIndexed { idx, ex ->
                            checkPageBreak(25f)
                            paint.color = if (idx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F4F7FA")
                            canvas.drawRect(40f, currentY, 555f, currentY + 20f, paint)

                            paint.color = borderGray
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 0.5f
                            canvas.drawRect(40f, currentY, 555f, currentY + 20f, paint)

                            paint.style = Paint.Style.FILL
                            paint.color = navyColor
                            paint.textSize = 8.5f
                            paint.isFakeBoldText = true
                            canvas.drawText("${idx + 1}", 48f, currentY + 14f, paint)

                            paint.isFakeBoldText = false
                            canvas.drawText(ex.name, 75f, currentY + 14f, paint)
                            canvas.drawText(ex.series, 305f, currentY + 14f, paint)
                            canvas.drawText(ex.reps, 370f, currentY + 14f, paint)
                            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 430f, currentY + 14f, paint)

                            currentY += 20f
                        }
                    }
                    else -> { // Profesional / Alfa Balcarce
                        val blackColor = android.graphics.Color.BLACK
                        val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                        val tableHeight = (routine.exercises.size * 20f + 20f).coerceAtLeast(80f)

                        // Bloque lateral negro DÍA X
                        paint.color = blackColor
                        canvas.drawRect(20f, currentY, 115f, currentY + tableHeight, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 15f
                        paint.isFakeBoldText = true
                        canvas.drawText("DÍA", 35f, currentY + 35f, paint)

                        paint.textSize = 7.5f
                        paint.isFakeBoldText = false
                        canvas.drawText(dayName.take(18), 28f, currentY + 52f, paint)

                        // Encabezado Tabla
                        paint.color = blackColor
                        canvas.drawRect(118f, currentY, 575f, currentY + 20f, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 8f
                        paint.isFakeBoldText = true
                        canvas.drawText("#", 125f, currentY + 14f, paint)
                        canvas.drawText("EJERCICIO", 150f, currentY + 14f, paint)
                        canvas.drawText("SERIES", 330f, currentY + 14f, paint)
                        canvas.drawText("REPETICIONES", 390f, currentY + 14f, paint)
                        canvas.drawText("NOTAS TÉCNICAS", 460f, currentY + 14f, paint)

                        currentY += 20f

                        routine.exercises.forEachIndexed { idx, ex ->
                            checkPageBreak(25f)

                            paint.color = if (idx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F9FAFB")
                            canvas.drawRect(118f, currentY, 575f, currentY + 20f, paint)

                            paint.color = borderGray
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 0.5f
                            canvas.drawRect(118f, currentY, 575f, currentY + 20f, paint)

                            paint.style = Paint.Style.FILL
                            paint.color = blackColor
                            paint.textSize = 8.5f
                            paint.isFakeBoldText = true
                            canvas.drawText("${idx + 1}", 125f, currentY + 14f, paint)

                            canvas.drawText(ex.name, 150f, currentY + 14f, paint)

                            paint.isFakeBoldText = false
                            canvas.drawText(ex.series, 345f, currentY + 14f, paint)
                            canvas.drawText(ex.reps, 410f, currentY + 14f, paint)
                            paint.color = android.graphics.Color.parseColor("#374151")
                            canvas.drawText(ex.notes.ifBlank { "Controlar ejecución" }, 460f, currentY + 14f, paint)

                            currentY += 20f
                        }
                    }
                }

                if (routine.notes.isNotBlank()) {
                    checkPageBreak(30f)
                    paint.color = android.graphics.Color.parseColor("#FEF3C7")
                    canvas.drawRoundRect(20f, currentY + 4f, 575f, currentY + 26f, 4f, 4f, paint)

                    paint.color = android.graphics.Color.parseColor("#92400E")
                    paint.textSize = 8.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("Anotaciones del día: ${routine.notes}", 28f, currentY + 19f, paint)
                    currentY += 30f
                } else {
                    currentY += 10f
                }
            }

            val footerY = if (currentY <= 680f) 680f else currentY + 10f
            if (footerY <= 700f) {
                when (template) {
                    "Ejecutiva" -> {
                        val greenColor = android.graphics.Color.parseColor("#1E4D2B")
                        val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                        // Bloque 1: Indicaciones Generales
                        paint.color = greenColor
                        canvas.drawRect(35f, footerY, 198f, footerY + 16f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 7.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("INDICACIONES GENERALES", 41f, footerY + 12f, paint)

                        paint.color = android.graphics.Color.parseColor("#F8FAF8")
                        canvas.drawRect(35f, footerY + 16f, 198f, footerY + 98f, paint)
                        paint.color = borderGray
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 0.5f
                        canvas.drawRect(35f, footerY + 16f, 198f, footerY + 98f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = greenColor
                        paint.textSize = 6.8f
                        paint.isFakeBoldText = false
                        canvas.drawText("▪ Realizar entrada en calor de 10 minutos.", 40f, footerY + 28f, paint)
                        canvas.drawText("▪ Priorizar técnica antes que peso.", 40f, footerY + 40f, paint)
                        canvas.drawText("▪ Aumentar cargas progresivamente.", 40f, footerY + 52f, paint)
                        canvas.drawText("▪ Mantener buena hidratación y descanso.", 40f, footerY + 64f, paint)
                        canvas.drawText("▪ Evitar ejercicios que generen dolor.", 40f, footerY + 76f, paint)

                        // Bloque 2: Registro de Progreso
                        paint.color = greenColor
                        canvas.drawRect(206f, footerY, 386f, footerY + 16f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 7.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("REGISTRO DE PROGRESO", 212f, footerY + 12f, paint)

                        paint.color = android.graphics.Color.parseColor("#F8FAF8")
                        canvas.drawRect(206f, footerY + 16f, 386f, footerY + 98f, paint)
                        paint.color = borderGray
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(206f, footerY + 16f, 386f, footerY + 98f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = greenColor
                        paint.textSize = 6.2f
                        paint.isFakeBoldText = true
                        canvas.drawText("FECHA | DOLOR (0-10) | RENDIMIENTO | OBSERVACIONES", 209f, footerY + 28f, paint)

                        paint.color = borderGray
                        canvas.drawLine(209f, footerY + 32f, 383f, footerY + 32f, paint)
                        canvas.drawLine(209f, footerY + 48f, 383f, footerY + 48f, paint)
                        canvas.drawLine(209f, footerY + 64f, 383f, footerY + 64f, paint)
                        canvas.drawLine(209f, footerY + 80f, 383f, footerY + 80f, paint)

                        // Bloque 3: Notas
                        paint.color = greenColor
                        canvas.drawRect(394f, footerY, 560f, footerY + 16f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 7.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("NOTAS", 400f, footerY + 12f, paint)

                        paint.color = android.graphics.Color.parseColor("#F8FAF8")
                        canvas.drawRect(394f, footerY + 16f, 560f, footerY + 98f, paint)
                        paint.color = borderGray
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(394f, footerY + 16f, 560f, footerY + 98f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = greenColor
                        paint.textSize = 7f
                        canvas.drawText("Consultar cualquier duda con tu profesor.", 398f, footerY + 28f, paint)
                        paint.color = borderGray
                        canvas.drawLine(398f, footerY + 48f, 556f, footerY + 48f, paint)
                        canvas.drawLine(398f, footerY + 64f, 556f, footerY + 64f, paint)
                        canvas.drawLine(398f, footerY + 80f, 556f, footerY + 80f, paint)
                    }
                    "Clásica" -> {
                        val navyColor = android.graphics.Color.parseColor("#1B365D")
                        val borderGray = android.graphics.Color.parseColor("#CBD5E1")

                        // Bloque 1: Indicaciones Generales
                        paint.color = navyColor
                        canvas.drawRect(40f, footerY, 200f, footerY + 16f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 7.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("INDICACIONES GENERALES", 46f, footerY + 12f, paint)

                        paint.color = android.graphics.Color.parseColor("#F8FAFC")
                        canvas.drawRect(40f, footerY + 16f, 200f, footerY + 98f, paint)
                        paint.color = borderGray
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 0.5f
                        canvas.drawRect(40f, footerY + 16f, 200f, footerY + 98f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = navyColor
                        paint.textSize = 6.8f
                        paint.isFakeBoldText = false
                        canvas.drawText("✔ Entrada en calor: 10 minutos.", 45f, footerY + 28f, paint)
                        canvas.drawText("✔ Pausa de 1 a 2 min entre series.", 45f, footerY + 40f, paint)
                        canvas.drawText("✔ Hidratación antes y después.", 45f, footerY + 52f, paint)
                        canvas.drawText("✔ Priorizar técnica sobre carga.", 45f, footerY + 64f, paint)
                        canvas.drawText("✔ Consultar dudas al profesor.", 45f, footerY + 76f, paint)

                        // Bloque 2: Registro de Progreso
                        paint.color = navyColor
                        canvas.drawRect(208f, footerY, 388f, footerY + 16f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 7.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("REGISTRO DE PROGRESO", 214f, footerY + 12f, paint)

                        paint.color = android.graphics.Color.parseColor("#F8FAFC")
                        canvas.drawRect(208f, footerY + 16f, 388f, footerY + 98f, paint)
                        paint.color = borderGray
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(208f, footerY + 16f, 388f, footerY + 98f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = navyColor
                        paint.textSize = 6.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("FECHA | CARGA (kg) | REPETICIONES | OBS", 212f, footerY + 28f, paint)

                        paint.color = borderGray
                        canvas.drawLine(212f, footerY + 32f, 384f, footerY + 32f, paint)
                        canvas.drawLine(212f, footerY + 48f, 384f, footerY + 48f, paint)
                        canvas.drawLine(212f, footerY + 64f, 384f, footerY + 64f, paint)
                        canvas.drawLine(212f, footerY + 80f, 384f, footerY + 80f, paint)

                        // Bloque 3: Notas
                        paint.color = navyColor
                        canvas.drawRect(396f, footerY, 555f, footerY + 16f, paint)
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 7.5f
                        paint.isFakeBoldText = true
                        canvas.drawText("NOTAS", 402f, footerY + 12f, paint)

                        paint.color = android.graphics.Color.parseColor("#F8FAFC")
                        canvas.drawRect(396f, footerY + 16f, 555f, footerY + 98f, paint)
                        paint.color = borderGray
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(396f, footerY + 16f, 555f, footerY + 98f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = navyColor
                        paint.textSize = 7f
                        canvas.drawText("Consultar cualquier duda con tu profesor.", 400f, footerY + 30f, paint)
                        paint.color = borderGray
                        canvas.drawLine(400f, footerY + 48f, 551f, footerY + 48f, paint)
                        canvas.drawLine(400f, footerY + 64f, 551f, footerY + 64f, paint)
                        canvas.drawLine(400f, footerY + 80f, 551f, footerY + 80f, paint)
                    }
                }
            }

            checkPageBreak(40f)
            when (template) {
                "Minimalista" -> {
                    paint.color = android.graphics.Color.LTGRAY
                    canvas.drawLine(40f, 790f, 555f, 790f, paint)
                    paint.color = android.graphics.Color.GRAY
                    paint.textSize = 8.5f
                    paint.isFakeBoldText = false
                    canvas.drawText(gymAddress.take(45), 40f, 810f, paint)
                    canvas.drawText("DISCIPLINA HOY, RESULTADOS SIEMPRE", 350f, 810f, paint)
                }
                "Moderna" -> {
                    paint.color = android.graphics.Color.parseColor("#00CED1")
                    canvas.drawLine(35f, 790f, 565f, 790f, paint)
                    paint.color = android.graphics.Color.parseColor("#64748B")
                    paint.textSize = 8.5f
                    canvas.drawText(gymAddress.take(45), 35f, 810f, paint)
                    canvas.drawText("DISCIPLINA HOY, RESULTADOS SIEMPRE", 350f, 810f, paint)
                }
                "Ejecutiva" -> {
                    paint.color = android.graphics.Color.parseColor("#1E293B")
                    paint.strokeWidth = 1f
                    canvas.drawLine(30f, 790f, 565f, 790f, paint)
                    paint.color = android.graphics.Color.parseColor("#475569")
                    paint.textSize = 8.5f
                    canvas.drawText(gymAddress.take(45), 30f, 810f, paint)
                    canvas.drawText("Firma Entrenador: ___________________", 350f, 810f, paint)
                }
                "Clásica" -> {
                    paint.color = android.graphics.Color.parseColor("#1B365D")
                    paint.strokeWidth = 1f
                    canvas.drawLine(40f, 790f, 555f, 790f, paint)
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 8.5f
                    canvas.drawText(gymAddress.take(45), 40f, 810f, paint)
                    canvas.drawText("DISCIPLINA HOY, RESULTADOS SIEMPRE", 350f, 810f, paint)
                }
                else -> { // Profesional / Alfa Balcarce
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawRect(20f, 790f, 575f, 822f, paint)

                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 9f
                    paint.isFakeBoldText = true
                    canvas.drawText(gymAddress.take(45), 30f, 810f, paint)
                    canvas.drawText("DISCIPLINA HOY, RESULTADOS SIEMPRE", 360f, 810f, paint)
                }
            }

            paint.color = android.graphics.Color.GRAY
            paint.textSize = 8f
            canvas.drawText("Página $pageNumber — $gymName", 480f, 832f, paint)

            pdfDocument.finishPage(page)

            val fileName = "Rutinas_Semana_$safeUserName.pdf"
            val file = File(context.cacheDir, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                shareRoutinePdfFile(context, file, "Rutinas Semanales")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun shareRoutinePdfFile(context: Context, file: File, routineName: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Compartir / Ver Rutina: $routineName").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e2: Exception) {
            e2.printStackTrace()
            Toast.makeText(context, "PDF guardado en: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }
}