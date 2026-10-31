package com.zexo.gymcopilot.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.model.SubscriptionPlan

import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

typealias SubscriptionPlan = com.zexo.gymcopilot.model.SubscriptionPlan


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onProfessorsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    attendanceRepository: com.zexo.gymcopilot.repository.AttendanceRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val plans by dataStoreManager.getSubscriptionPlans().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(attendanceRepository) {
        attendanceRepository?.syncSubscriptionPlansFromServer()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Planes de Suscripción", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier.size(34.dp).clip(CircleShape), colorFilter = ColorFilter.tint(accentColor))
                        }
                    }
                )
            },
            bottomBar = {
                AdminBottomNavigation(
                    currentRoute = "",
                    onHomeClick = onHomeClick,
                    onStoreClick = onStoreClick,
                    onMembersClick = onMembersClick,
                    onProfessorsClick = onProfessorsClick,
                    onSettingsClick = onSettingsClick,
                    accentColor = accentColor
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = accentColor,
                    contentColor = Color(0xFF0C1221), // Color de fondo para simular transparencia
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Plan", modifier = Modifier.size(32.dp))
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                if (plans.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CardMembership, null, modifier = Modifier.size(80.dp), tint = TextGray.copy(alpha = 0.3f))
                                Spacer(Modifier.height(16.dp))
                                Text("No hay planes configurados", color = TextGray, fontSize = 16.sp)
                                Text("Toca el botón + para crear uno", color = TextGray.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                items(plans) { plan ->
                    PlanCard(plan, accentColor, containerShape, onDelete = {
                        scope.launch {
                            val newList = plans.filter { it.id != plan.id }
                            dataStoreManager.saveSubscriptionPlans(newList)
                            attendanceRepository?.uploadSubscriptionPlans(newList)
                        }
                    })
                }
            }
        }

        if (showAddDialog) {
            AddPlanDialog(
                accentColor = accentColor,
                shape = containerShape,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, price, days, desc, totalClasses ->
                    scope.launch {
                        val newList = plans + SubscriptionPlan(
                            name = name, 
                            price = price, 
                            durationDays = days, 
                            description = desc,
                            totalClasses = totalClasses
                        )
                        dataStoreManager.saveSubscriptionPlans(newList)
                        attendanceRepository?.uploadSubscriptionPlans(newList)
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun PlanCard(plan: SubscriptionPlan, accentColor: Color, shape: androidx.compose.ui.graphics.Shape, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.2f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.name, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
                val classesText = if (plan.totalClasses != null) " - ${plan.totalClasses} clases" else ""
                Text("${plan.price} - ${plan.durationDays} días$classesText", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (plan.description.isNotBlank()) {
                    Text(plan.description, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanDialog(
    accentColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var totalClasses by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF081C24),
        title = { Text("Nuevo Plan", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre del Plan", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Precio", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = days, onValueChange = { days = it },
                        label = { Text("Días", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalClasses, onValueChange = { totalClasses = it },
                        label = { Text("Clases (opcional)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Descripción (opcional)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        onConfirm(
                            name, 
                            price.toDoubleOrNull() ?: 0.0, 
                            days.toIntOrNull() ?: 30, 
                            description,
                            totalClasses.toIntOrNull()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                modifier = Modifier.border(1.dp, accentColor, RoundedCornerShape(12.dp))
            ) {
                val contentColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                Text("CREAR", color = contentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = TextWhite)
            }
        }
    )
}
