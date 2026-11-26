package com.zexo.gymcopilot.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.ui.theme.PrimaryTurquoise
import com.zexo.gymcopilot.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    
    val cartItems by dataStoreManager.getCartItems().collectAsState(initial = emptyList())
    val contactMethod by dataStoreManager.getPreferredContactMethod().collectAsState(initial = "whatsapp")
    val contactPhone by dataStoreManager.getContactPhone().collectAsState(initial = "")
    val appLanguage by dataStoreManager.getAppLanguage().collectAsState(initial = "English")
    val appCurrencySelection by dataStoreManager.getAppCurrency().collectAsState(initial = "Peso Argentino")
    val currencySymbol = remember(appCurrencySelection) { CurrencyData.getSymbol(appCurrencySelection) }
    
    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val appAccentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise

    val t = remember(appLanguage) {
        when (appLanguage) {
            "Español" -> mapOf(
                "title" to "Mi Carrito",
                "empty" to "Tu carrito está vacío",
                "total" to "Total Estimado",
                "contact" to "PEDIR POR",
                "remove" to "Eliminar"
            )
            else -> mapOf(
                "title" to "My Cart",
                "empty" to "Your cart is empty",
                "total" to "Estimated Total",
                "contact" to "ORDER VIA",
                "remove" to "Remove"
            )
        }
    }

    val totalPrice = cartItems.sumOf { it.price }

    val contactIcon = when (contactMethod) {
        "whatsapp" -> R.drawable.whatsapp
        "telegram" -> R.drawable.telegram
        "viber" -> R.drawable.viber
        "wechat" -> R.drawable.wechat
        "line" -> R.drawable.line
        else -> R.drawable.whatsapp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t["title"] ?: "", color = Color.Black, fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(t["empty"] ?: "", color = Color.Gray)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems) { product ->
                        CartItemRow(
                            product = product,
                            currencySymbol = currencySymbol,
                            onRemove = { scope.launch { dataStoreManager.removeFromCart(product.id) } }
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t["total"] ?: "", fontSize = 16.sp, color = Color.Gray)
                            Text("$currencySymbol${String.format("%.2f", totalPrice)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                val message = cartItems.joinToString("\n") { "- ${it.name}: $currencySymbol${it.price}" }
                                val fullMessage = "Hola! Me interesa comprar:\n$message\nTotal: $currencySymbol${String.format("%.2f", totalPrice)}"
                                
                                val intent = when (contactMethod) {
                                    "whatsapp" -> {
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$contactPhone&text=${Uri.encode(fullMessage)}")
                                        Intent(Intent.ACTION_VIEW, uri)
                                    }
                                    "telegram" -> {
                                        val uri = Uri.parse("https://t.me/share/url?url=${Uri.encode(fullMessage)}&text=${Uri.encode(fullMessage)}")
                                        Intent(Intent.ACTION_VIEW, uri)
                                    }
                                    else -> {
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, fullMessage)
                                        }
                                    }
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), 
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = contactIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${t["contact"]} ${contactMethod.uppercase()}", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(product: Product, currencySymbol: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUri != null) {
                    AsyncImage(model = product.imageUri, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color.LightGray)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text("$currencySymbol${product.price}", color = PrimaryTurquoise, fontWeight = FontWeight.Bold)
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}
