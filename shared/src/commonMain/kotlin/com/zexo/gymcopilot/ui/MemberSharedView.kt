package com.zexo.gymcopilot.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.LockConfig
import com.zexo.gymcopilot.model.Routine
import com.zexo.gymcopilot.model.SubscriptionPlan
import kotlin.collections.*
import com.zexo.gymcopilot.shared.network.CheckInRequest
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.shared.network.MemberSyncRequest
import com.zexo.gymcopilot.utils.LocalMemberData
import com.zexo.gymcopilot.utils.decodeBase64ToBitmap
import com.zexo.gymcopilot.utils.formatEpochDate
import com.zexo.gymcopilot.utils.generateQrBitmap
import com.zexo.gymcopilot.utils.getCurrentTimeMillis
import com.zexo.gymcopilot.utils.launchQrScanner
import com.zexo.gymcopilot.utils.openExternalUrl
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import com.zexo.gymcopilot.utils.readLocalStorageFlag
import com.zexo.gymcopilot.utils.saveLocalMemberProfile
import com.zexo.gymcopilot.utils.saveLocalStorageFlag
import com.zexo.gymcopilot.utils.updateWebPageBranding
import gymcopilot.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource

@Composable
fun MemberDashboardSharedScreen(
    gymName: String = "Nombre del Gimnasio",
    userEmail: String = "socio@gymcopilot.com",
    customLogoUri: String? = null,
    apiClient: GymKtorApiClient = GymKtorApiClient,
    currentTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var gymIsOpen by remember { mutableStateOf(true) }
    var classInSession by remember { mutableStateOf(false) }
    var showLogoBorder by remember { mutableStateOf(true) }
    var showCheckInSuccessBanner by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var showMemberProfileScreen by remember { mutableStateOf(false) }

    // DATOS DINÁMICOS DEL PERFIL DEL SOCIO Y GIMNASIO
    var activeFirstName by remember { mutableStateOf("") }
    var activeLastName by remember { mutableStateOf("") }
    var activeEmail by remember { mutableStateOf(userEmail) }
    var activePhone by remember { mutableStateOf("") }
    var activeAddress by remember { mutableStateOf("") }
    var activeWeight by remember { mutableStateOf("") }
    var activeHeight by remember { mutableStateOf("") }
    var activePhotoUri by remember { mutableStateOf<String?>(null) }
    var activeGymName by remember { mutableStateOf(gymName) }
    var activeGymUrl by remember { mutableStateOf(LockConfig.activeApiUrl) }
    var activeMembershipStatus by remember { mutableStateOf("ACTIVO") }
    var activeNextRenewalDate by remember { mutableLongStateOf(0L) }
    var activePlanName by remember { mutableStateOf("No asignado") }
    var activeAssignedTrainer by remember { mutableStateOf("") }

    var licenseModelPayer by remember { mutableStateOf(readLocalStorageFlag("license_model_payer") ?: "admin") }
    var userBankAccount by remember { mutableStateOf(readLocalStorageFlag("bank_account") ?: "") }
    var isGymLicensePaid by remember { mutableStateOf(readLocalStorageFlag("gym_license_paid") == "true") }
    var gymLicenseVence by remember { mutableLongStateOf(readLocalStorageFlag("gym_license_vence")?.toLongOrNull() ?: 0L) }
    var subscriptionPlans by remember {
        mutableStateOf<List<SubscriptionPlan>>(
            try {
                val cachedJson = readLocalStorageFlag("gym_plans_json")
                if (!cachedJson.isNullOrBlank()) {
                    val jsonElement = Json.parseToJsonElement(cachedJson)
                    val jsonObject = jsonElement as? JsonObject
                    val dataArray = jsonObject?.get("data") as? JsonArray
                    if (dataArray != null) {
                        val plansList = mutableListOf<SubscriptionPlan>()
                        for (i in 1 until dataArray.size) {
                            val row = dataArray[i] as? JsonArray
                            if (row != null && row.size >= 3) {
                                val id = row[0].jsonPrimitive.content.trim()
                                val name = row[1].jsonPrimitive.content.trim()
                                val rawPriceStr = row[2].jsonPrimitive.content.trim()
                                val cleanPriceStr = rawPriceStr.replace(",", ".").replace(Regex("[^0-9.]"), "")
                                val price = cleanPriceStr.toDoubleOrNull() ?: 0.0
                                val days = if (row.size > 3) row[3].jsonPrimitive.content.toIntOrNull() ?: 30 else 30
                                val desc = if (row.size > 4) row[4].jsonPrimitive.content else ""
                                plansList.add(SubscriptionPlan(id = id, name = name, price = price, durationDays = days, description = desc))
                            }
                        }
                        plansList
                    } else emptyList()
                } else emptyList()
            } catch (_: Throwable) { emptyList() }
        )
    }

    var showBankTransferDialog by remember { mutableStateOf(false) }
    var infoBannerTitle by remember { mutableStateOf("Estado de Cuenta") }
    var infoBannerMessage by remember { mutableStateOf("") }
    var showInfoBanner by remember { mutableStateOf(false) }

    // Cargar planes de suscripción desde el servidor y cachear
    LaunchedEffect(activeGymUrl) {
        if (activeGymUrl.isNotBlank()) {
            try {
                val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                val localProfile = readLocalMemberProfile()
                val ssId = localProfile?.spreadsheetId.orEmpty()
                val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                val url = "$activeGymUrl${paramChar}type=plans&gymId=$gymIdVal$ssParam"
                val responseText = apiClient.getData(url)
                if (responseText.isNotBlank() && responseText.contains("success")) {
                    val jsonElement = Json.parseToJsonElement(responseText)
                    val jsonObject = jsonElement as? JsonObject
                    val dataArray = jsonObject?.get("data") as? JsonArray
                    if (dataArray != null) {
                        val plansList = mutableListOf<SubscriptionPlan>()
                        for (i in 1 until dataArray.size) {
                            val row = dataArray[i] as? JsonArray
                            if (row != null && row.size >= 3) {
                                val id = row[0].jsonPrimitive.content.trim()
                                val name = row[1].jsonPrimitive.content.trim()
                                val rawPriceStr = row[2].jsonPrimitive.content.trim()
                                val cleanPriceStr = rawPriceStr.replace(",", ".").replace(Regex("[^0-9.]"), "")
                                val price = cleanPriceStr.toDoubleOrNull() ?: 0.0
                                val days = if (row.size > 3) row[3].jsonPrimitive.content.toIntOrNull() ?: 30 else 30
                                val desc = if (row.size > 4) row[4].jsonPrimitive.content else ""
                                plansList.add(SubscriptionPlan(id = id, name = name, price = price, durationDays = days, description = desc))
                            }
                        }
                        if (plansList.isNotEmpty()) {
                            subscriptionPlans = plansList
                            saveLocalStorageFlag("gym_plans_json", responseText)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    val onLicenciaPayClickAction: () -> Unit = {
        coroutineScope.launch {
            try {
                val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                val localProfile = readLocalMemberProfile()
                val ssId = localProfile?.spreadsheetId.orEmpty()
                val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                val amount = 0.0

                val reqUrl = "$activeGymUrl${paramChar}action=create_mp_preference&gymId=$gymIdVal&gymName=${activeGymName}&memberEmail=$activeEmail&planId=gym_license&amount=$amount&operationType=APP_LICENSE&paymentModality=member_all&spreadsheetId=$ssId&isSubscription=true&title=Licencia%20App%20-%20${activeGymName}"
                val responseText = apiClient.getData(reqUrl)
                val regex = Regex("\"init_point\"\\s*:\\s*\"([^\"]+)\"")
                val initPoint = regex.find(responseText)?.groupValues?.get(1) ?: ""
                if (initPoint.isNotBlank()) {
                    openExternalUrl(initPoint)
                } else {
                    openExternalUrl("https://www.mercadopago.com.ar")
                }
            } catch (_: Throwable) {
                openExternalUrl("https://www.mercadopago.com.ar")
            }
        }
    }

    val onMembresiaPayClickAction: () -> Unit = {
        val normLicense = licenseModelPayer.trim().lowercase()
        if (normLicense == "admin" || normLicense.isBlank()) {
            showBankTransferDialog = true
        } else {
            coroutineScope.launch {
                try {
                    val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    val localProfile = readLocalMemberProfile()
                    val ssId = localProfile?.spreadsheetId.orEmpty()
                    val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                    val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""

                    // 1. Búsqueda en planes cacheados
                    val trimmedPlan = activePlanName.trim()
                    var resolvedPrice = subscriptionPlans.find {
                        it.name.trim().equals(trimmedPlan, ignoreCase = true) ||
                        it.id.trim().equals(trimmedPlan, ignoreCase = true) ||
                        it.name.trim().lowercase().contains(trimmedPlan.lowercase()) ||
                        trimmedPlan.lowercase().contains(it.name.trim().lowercase())
                    }?.price ?: 0.0

                    // 2. Si no se encontró en caché, consultar type=plans en el momento
                    if (resolvedPrice <= 0.0 && activeGymUrl.isNotBlank()) {
                        try {
                            val plansUrl = "$activeGymUrl${paramChar}type=plans&gymId=$gymIdVal$ssParam"
                            val plansResp = apiClient.getData(plansUrl)
                            if (plansResp.isNotBlank() && plansResp.contains("success")) {
                                val jsonElem = Json.parseToJsonElement(plansResp)
                                val jsonObj = jsonElem as? JsonObject
                                val arr = jsonObj?.get("data") as? JsonArray
                                if (arr != null) {
                                    for (i in 1 until arr.size) {
                                        val row = arr[i] as? JsonArray
                                        if (row != null && row.size >= 3) {
                                            val pName = row[1].jsonPrimitive.content.trim()
                                            val rawPPrice = row[2].jsonPrimitive.content.trim()
                                            val cleanPPrice = rawPPrice.replace(",", ".").replace(Regex("[^0-9.]"), "")
                                            val pPrice = cleanPPrice.toDoubleOrNull() ?: 0.0
                                            if (pName.equals(trimmedPlan, ignoreCase = true) || pName.lowercase().contains(trimmedPlan.lowercase()) || trimmedPlan.lowercase().contains(pName.lowercase())) {
                                                resolvedPrice = pPrice
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: Throwable) {}
                    }

                    // 3. Fallback inteligente: si no hubo coincidencia exacta de nombre, usar el precio del primer plan disponible en la lista del gimnasio
                    if (resolvedPrice <= 0.0 && subscriptionPlans.isNotEmpty()) {
                        resolvedPrice = subscriptionPlans.first().price
                    }

                    val amount = if (resolvedPrice > 0.0) resolvedPrice else 0.0

                    if (amount <= 0.0) {
                        infoBannerTitle = "Plan sin precio"
                        infoBannerMessage = "El plan asignado ('$activePlanName') no tiene un valor o precio configurado en la base de datos. Por favor, comunícate con la administración del gimnasio."
                        showInfoBanner = true
                        return@launch
                    }

                    val reqUrl = "$activeGymUrl${paramChar}action=create_mp_preference&gymId=$gymIdVal&gymName=${activeGymName}&memberEmail=$activeEmail&planId=$activePlanName&amount=$amount&operationType=MEMBERSHIP&paymentModality=$normLicense&spreadsheetId=$ssId&isSubscription=true&title=Cuota%20Membresía%20-%20${activeGymName}"
                    val responseText = apiClient.getData(reqUrl)
                    val regex = Regex("\"init_point\"\\s*:\\s*\"([^\"]+)\"")
                    val initPoint = regex.find(responseText)?.groupValues?.get(1) ?: ""
                    if (initPoint.isNotBlank()) {
                        openExternalUrl(initPoint)
                    } else {
                        openExternalUrl("https://www.mercadopago.com.ar")
                    }
                } catch (_: Throwable) {
                    openExternalUrl("https://www.mercadopago.com.ar")
                }
            }
        }
    }

    // CARGA PERSISTENTE DESDE LOCALSTORAGE EN WEB / DISPOSITIVO
    LaunchedEffect(Unit) {
        val localData = readLocalMemberProfile()
        if (localData != null && localData.email.isNotBlank()) {
            activeFirstName = localData.firstName
            activeLastName = localData.lastName
            activeEmail = localData.email
            activePhone = localData.phone
            activeAddress = localData.address
            activeWeight = localData.weight
            activeHeight = localData.height
            if (localData.photoUri != null) activePhotoUri = localData.photoUri
            if (localData.gymName.isNotBlank()) activeGymName = localData.gymName
            if (localData.gymApiUrl.isNotBlank()) activeGymUrl = localData.gymApiUrl
            activeMembershipStatus = localData.membershipStatus
            activeNextRenewalDate = localData.nextRenewalDate
            if (localData.planName.isNotBlank()) activePlanName = localData.planName
            activeAssignedTrainer = localData.assignedTrainer
        }
    }

    LaunchedEffect(activeGymUrl, activeEmail) {
        if (activeGymUrl.isNotBlank() && activeEmail.isNotBlank()) {
            while (true) {
                try {
                    val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    val localProfile = readLocalMemberProfile()
                    val ssId = localProfile?.spreadsheetId.orEmpty()
                    val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                    val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                    val url = "$activeGymUrl${paramChar}type=members&gymId=$gymIdVal$ssParam"
                    val responseText = apiClient.getData(url)
                    if (responseText.isNotBlank() && responseText.contains("success")) {
                        val jsonElement = Json.parseToJsonElement(responseText)
                        val jsonObject = jsonElement as? JsonObject
                        val dataArray = jsonObject?.get("data") as? JsonArray
                        if (dataArray != null) {
                            val myEmailTrim = activeEmail.lowercase().trim()
                            for (i in 1 until dataArray.size) {
                                val row = dataArray[i] as? JsonArray
                                if (row != null && row.size > 3) {
                                    val emailInRow = row[2].jsonPrimitive.content.lowercase().trim()
                                    if (emailInRow == myEmailTrim) {
                                        val status = row[3].jsonPrimitive.content
                                        if (status.isNotBlank()) activeMembershipStatus = status
                                        if (row.size > 10) {
                                            val renewalStr = row[10].jsonPrimitive.content
                                            val renewalTs = renewalStr.toLongOrNull() ?: 0L
                                            if (renewalTs > 0L) activeNextRenewalDate = renewalTs
                                        }
                                        if (row.size > 12) {
                                            val plan = row[12].jsonPrimitive.content
                                            if (plan.isNotBlank()) activePlanName = plan
                                        }
                                        if (row.size > 13) {
                                            val trainerInCol = row[13].jsonPrimitive.content
                                            activeAssignedTrainer = if (trainerInCol.isNotBlank() && trainerInCol != "null") trainerInCol else ""
                                        } else {
                                            activeAssignedTrainer = ""
                                        }
                                        if (row.size > 14) {
                                            val licStr = row[14].jsonPrimitive.content
                                            val licTs = licStr.toLongOrNull() ?: 0L
                                            if (licTs > 0L) {
                                                gymLicenseVence = licTs
                                                isGymLicensePaid = licTs > getCurrentTimeMillis()
                                                saveLocalStorageFlag("gym_license_vence", licTs.toString())
                                                saveLocalStorageFlag("gym_license_paid", if (isGymLicensePaid) "true" else "false")
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {}
                delay(10000)
            }
        }
    }

    var broadcastMessage by remember { mutableStateOf("") }
    var serverGymLogoUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeGymUrl) {
        if (activeGymUrl.isNotBlank()) {
            while (true) {
                try {
                    val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    val localProfile = readLocalMemberProfile()
                    val ssId = localProfile?.spreadsheetId.orEmpty()
                    val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                    val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                    val url = "$activeGymUrl${paramChar}type=config&gymId=$gymIdVal$ssParam"
                    val responseText = apiClient.getData(url)
                    if (responseText.isNotBlank() && responseText.contains("success")) {
                        val jsonElement = Json.parseToJsonElement(responseText)
                        val jsonObject = jsonElement as? JsonObject
                        val dataArray = jsonObject?.get("data") as? JsonArray
                        if (dataArray != null) {
                            var msgFound: String? = null
                            for (i in 0 until dataArray.size) {
                                val row = dataArray[i] as? JsonArray
                                if (row != null && row.size > 0 && row[0].jsonPrimitive.content == "GYM_CONFIG") {
                                    if (row.size > 1) {
                                        val rawGymName = row[1].jsonPrimitive.content
                                        if (rawGymName.isNotBlank() && rawGymName != "null") {
                                            activeGymName = rawGymName
                                        }
                                    }
                                    if (row.size > 2) {
                                        val rawLogo = row[2].jsonPrimitive.content
                                        if (rawLogo.isNotBlank() && rawLogo != "null") {
                                            serverGymLogoUri = rawLogo
                                        }
                                    }
                                    if (row.size > 11) {
                                        val rawBorder = row[11].jsonPrimitive.content
                                        if (rawBorder != "null") {
                                            showLogoBorder = rawBorder == "true" || rawBorder == "1"
                                        }
                                    }
                                    if (row.size > 12) {
                                        val rawClass = row[12].jsonPrimitive.content
                                        if (rawClass != "null") {
                                            classInSession = rawClass == "true" || rawClass == "1"
                                        }
                                    }
                                    if (row.size > 13) {
                                        val rawMsg = row[13].jsonPrimitive.content
                                        val isIsoDate = rawMsg.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.*"""))
                                        if (!isIsoDate && rawMsg != "null") {
                                            msgFound = rawMsg
                                        }
                                    }
                                    if (row.size > 14) {
                                        val rawOpen = row[14].jsonPrimitive.content
                                        if (rawOpen != "null") {
                                            gymIsOpen = rawOpen == "true" || rawOpen == "1"
                                        }
                                    }
                                    if (row.size > 21) {
                                        val rawBank = row[21].jsonPrimitive.content
                                        val isIsoDate = rawBank.matches(Regex("""\d{4}-\d{2}-\d{2}T.*""")) || (rawBank.contains("T") && rawBank.contains("Z"))
                                        if (rawBank.isNotBlank() && rawBank != "null" && !isIsoDate) {
                                            userBankAccount = rawBank
                                            saveLocalStorageFlag("bank_account", rawBank)
                                        }
                                    }
                                    if (row.size > 22) {
                                        val rawLicense = row[22].jsonPrimitive.content
                                        val isIsoDate = rawLicense.matches(Regex("""\d{4}-\d{2}-\d{2}T.*""")) || (rawLicense.contains("T") && rawLicense.contains("Z"))
                                        if (rawLicense.isNotBlank() && rawLicense != "null" && !isIsoDate) {
                                            val cleanLicense = rawLicense.trim().lowercase()
                                            licenseModelPayer = cleanLicense
                                            saveLocalStorageFlag("license_model_payer", cleanLicense)
                                        }
                                    }
                                    if (gymLicenseVence <= 0L) {
                                        if (row.size > 24) {
                                            val rawStatus = row[24].jsonPrimitive.content
                                            if (rawStatus.isNotBlank() && rawStatus != "null") {
                                                val isPaid = rawStatus.trim().uppercase() == "ACTIVO"
                                                isGymLicensePaid = isPaid
                                                saveLocalStorageFlag("gym_license_paid", if (isPaid) "true" else "false")
                                            }
                                        }
                                        if (row.size > 25) {
                                            val rawExpiry = row[25].jsonPrimitive.content
                                            val expiryLong = rawExpiry.trim().toLongOrNull()
                                            if (expiryLong != null && expiryLong > 0L) {
                                                gymLicenseVence = expiryLong
                                                saveLocalStorageFlag("gym_license_vence", expiryLong.toString())
                                            }
                                        }
                                    }
                                    break
                                }
                            }
                            if (msgFound != null) {
                                broadcastMessage = msgFound
                            }
                        }
                    }
                } catch (_: Throwable) {}
                delay(5000)
            }
        }
    }

    val gymLogoBitmap = remember(serverGymLogoUri, customLogoUri) {
        val uri = serverGymLogoUri ?: customLogoUri
        if (uri != null) decodeBase64ToBitmap(uri) else null
    }

    LaunchedEffect(activeGymName, serverGymLogoUri) {
        updateWebPageBranding(activeGymName, serverGymLogoUri)
    }

    var bannerTitle by remember { mutableStateOf("Asistencia Registrada") }
    var bannerMessage by remember { mutableStateOf("Asistencia registrada") }
    var bannerColor by remember { mutableStateOf(Color(0xFF00C853)) }

    var assignedProfPhotoUri by remember { mutableStateOf<String?>(null) }
    var assignedProfColorInt by remember { mutableStateOf<Int?>(null) }
    var assignedProfSpecialty by remember { mutableStateOf("") }

    LaunchedEffect(activeGymUrl, activeAssignedTrainer) {
        if (activeGymUrl.isNotBlank() && activeAssignedTrainer.isNotBlank() && !activeAssignedTrainer.equals("Sin asignar", true) && !activeAssignedTrainer.equals("N/A", true)) {
            try {
                val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                val localProfile = readLocalMemberProfile()
                val ssId = localProfile?.spreadsheetId.orEmpty()
                val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                val url = "$activeGymUrl${paramChar}type=professors&gymId=$gymIdVal$ssParam"
                val responseText = apiClient.getData(url)
                if (responseText.isNotBlank() && responseText.contains("success")) {
                    val jsonElement = Json.parseToJsonElement(responseText)
                    val jsonObject = jsonElement as? JsonObject
                    val dataArray = jsonObject?.get("data") as? JsonArray
                    if (dataArray != null) {
                        val trainerTrim = activeAssignedTrainer.lowercase().trim()
                        for (i in 1 until dataArray.size) {
                            val row = dataArray[i] as? JsonArray
                            if (row != null && row.size > 2) {
                                val fn = row[1].jsonPrimitive.content.trim()
                                val ln = row[2].jsonPrimitive.content.trim()
                                val fullName = "$fn $ln".lowercase().trim()
                                val revName = "$ln $fn".lowercase().trim()
                                val email = if (row.size > 3) row[3].jsonPrimitive.content.lowercase().trim() else ""

                                if (trainerTrim == fullName || trainerTrim == revName || trainerTrim.contains(fn.lowercase()) || trainerTrim.contains(ln.lowercase()) || fullName.contains(trainerTrim) || (email.isNotBlank() && trainerTrim.contains(email))) {
                                    if (row.size > 4) {
                                        val spec = row[4].jsonPrimitive.content
                                        if (spec.isNotBlank() && spec != "null") assignedProfSpecialty = spec
                                    }
                                    if (row.size > 5) {
                                        val colorStr = row[5].jsonPrimitive.content
                                        val colorVal = colorStr.toIntOrNull()
                                        if (colorVal != null && colorVal != 0) assignedProfColorInt = colorVal
                                    }
                                    if (row.size > 6) {
                                        val photo = row[6].jsonPrimitive.content
                                        if (photo.isNotBlank() && photo != "null") assignedProfPhotoUri = photo
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    if (showHelpScreen) {
        HelpSharedScreen(onBack = { showHelpScreen = false })
    } else if (showMemberProfileScreen) {
        MemberProfileSharedScreen(
            initialFirstName = activeFirstName,
            initialLastName = activeLastName,
            initialEmail = activeEmail,
            initialPhone = activePhone,
            initialAddress = activeAddress,
            initialWeight = activeWeight,
            initialHeight = activeHeight,
            initialPhotoUri = activePhotoUri,
            onBack = { showMemberProfileScreen = false },
            onSaveProfile = { f, l, e, p, a, w, h, photo ->
                activeFirstName = f
                activeLastName = l
                activeEmail = e
                activePhone = p
                activeAddress = a
                activeWeight = w
                activeHeight = h
                activePhotoUri = photo

                saveLocalMemberProfile(
                    LocalMemberData(
                        firstName = f,
                        lastName = l,
                        email = e,
                        phone = p,
                        address = a,
                        weight = w,
                        height = h,
                        photoUri = photo,
                        gymName = activeGymName,
                        gymApiUrl = activeGymUrl,
                        assignedTrainer = activeAssignedTrainer
                    )
                )

                coroutineScope.launch {
                    try {
                        val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                        val localProfile = readLocalMemberProfile()
                        val ssId = localProfile?.spreadsheetId.orEmpty()
                        val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                        val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                        val syncUrl = "$activeGymUrl${paramChar}action=sync_member&gymId=$gymIdVal&email=$e$ssParam"
                        val syncReq = MemberSyncRequest(
                            action = "sync_member",
                            firstName = f,
                            lastName = l,
                            email = e,
                            phone = p,
                            address = a,
                            weight = w,
                            height = h,
                            photoUri = photo
                        )
                        GymKtorApiClient.postData(syncUrl, syncReq)
                    } catch (_: Throwable) {}
                }
                showMemberProfileScreen = false
            }
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF00151C)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. CABECERA CON LOGO Y BIENVENIDA
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF001F29))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF002F3D),
                                    shape = CircleShape,
                                    border = if (showLogoBorder) BorderStroke(2.dp, PrimaryTurquoise) else null,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (gymLogoBitmap != null) {
                                            Image(
                                                bitmap = gymLogoBitmap,
                                                contentDescription = "Logo Gimnasio",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = "Logo Gimnasio",
                                                tint = PrimaryTurquoise,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (activeGymName.isBlank() || activeGymName == "Gym 19") "Nombre del Gimnasio" else activeGymName,
                                        color = TextWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val fullNameDisplay = if (activeFirstName.isNotBlank()) "$activeFirstName $activeLastName".trim() else activeEmail
                                    Text(
                                        text = fullNameDisplay,
                                        color = PrimaryTurquoise,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { showMemberProfileScreen = true }) {
                                    Icon(Icons.Default.Person, contentDescription = "Mi Perfil", tint = TextWhite)
                                }
                                IconButton(onClick = { showHelpScreen = true }) {
                                    Icon(Icons.Default.Info, contentDescription = "Ayuda", tint = TextWhite)
                                }
                            }
                        }

                        if (LockConfig.APP_ENVIRONMENT == 0) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF380808),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF3D00)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("⚠️ MODO PRUEBA (TEST) ACTIVO", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Conectado al entorno de desarrollo en Drive.", color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 2. BADGES DE ESTADÍSTICAS E INDICADORES
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusChip(
                            text = if (gymIsOpen) "GIMNASIO ABIERTO" else "GIMNASIO CERRADO",
                            color = if (gymIsOpen) Color(0xFF00C853) else Color.Red,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(
                            text = if (classInSession) "CLASES EN VIVO" else "SIN CLASES AHORA",
                            color = if (classInSession) Color(0xFF00C853) else Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. TARJETA ESTADO DE MEMBRESÍA
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SharedMembershipStatusCard(
                            membershipStatus = activeMembershipStatus,
                            nextRenewalDate = activeNextRenewalDate,
                            planName = activePlanName,
                            licenseModelPayer = licenseModelPayer,
                            isGymLicensePaid = isGymLicensePaid,
                            gymLicenseVence = gymLicenseVence,
                            onLicenciaPayClick = {
                                if (isGymLicensePaid) {
                                    infoBannerTitle = "Licencia Activa"
                                    infoBannerMessage = "La licencia de la app ya se encuentra al día y activa."
                                    showInfoBanner = true
                                } else {
                                    onLicenciaPayClickAction()
                                }
                            },
                            onMembresiaPayClick = {
                                val isActivo = activeMembershipStatus.uppercase() == "ACTIVO" || activeMembershipStatus.uppercase() == "ACTIVE" || activeMembershipStatus.uppercase() == "VIGENTE"
                                if (isActivo) {
                                    infoBannerTitle = "Membresía Activa"
                                    infoBannerMessage = "Tu membresía ya se encuentra al día y activa."
                                    showInfoBanner = true
                                } else {
                                    onMembresiaPayClickAction()
                                }
                            }
                        )
                    }
                }

                if (broadcastMessage.isNotBlank() && broadcastMessage != "null") {
                    item {
                        BroadcastMessageSharedDisplay(broadcastMessage)
                    }
                }

                // 4. GRILLA DE ACCIONES RÁPIDAS
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MemberActionSquareCard(
                                title = "QR Login",
                                iconPainter = painterResource(Res.drawable.qr_code),
                                iconBg = Color(0xFF00363A),
                                iconTint = PrimaryTurquoise,
                                modifier = Modifier.weight(1f)
                            ) {
                                showQrDialog = true
                            }

                            MemberActionSquareCard(
                                title = "Check-in",
                                iconVector = Icons.Default.QrCodeScanner,
                                iconBg = Color(0xFF0D2E21),
                                iconTint = Color(0xFF00C853),
                                modifier = Modifier.weight(1f)
                            ) {
                                launchQrScanner { cleanCode ->
                                    if (cleanCode.isNotBlank()) {
                                        val isLinking = cleanCode.startsWith("MEMBERLINK|") || cleanCode.startsWith("MEMBER_LINK:")
                                        if (isLinking) {
                                            if (cleanCode.startsWith("MEMBERLINK|")) {
                                                val parts = cleanCode.split("|")
                                                if (parts.size >= 4) {
                                                    activeFirstName = parts[1]
                                                    activeLastName = parts[2]
                                                    activeEmail = parts[3]
                                                    if (parts.size >= 5 && parts[4].isNotBlank()) activeGymUrl = parts[4]
                                                    if (parts.size >= 6 && parts[5].isNotBlank()) activeGymName = parts[5]
                                                }
                                            } else if (cleanCode.startsWith("MEMBER_LINK:")) {
                                                val parts = cleanCode.split(":")
                                                if (parts.size >= 5) {
                                                    activeEmail = parts[2]
                                                    activeFirstName = parts[3]
                                                    activeLastName = parts[4]
                                                }
                                            }

                                            saveLocalMemberProfile(
                                                LocalMemberData(
                                                    firstName = activeFirstName,
                                                    lastName = activeLastName,
                                                    email = activeEmail,
                                                    phone = activePhone,
                                                    address = activeAddress,
                                                    weight = activeWeight,
                                                    height = activeHeight,
                                                    photoUri = activePhotoUri,
                                                    gymName = activeGymName,
                                                    gymApiUrl = activeGymUrl,
                                                    assignedTrainer = activeAssignedTrainer
                                                )
                                            )

                                            bannerTitle = "Cuenta vinculada"
                                            bannerMessage = "Cuenta vinculada"
                                            bannerColor = PrimaryTurquoise
                                            showCheckInSuccessBanner = true
                                        } else {
                                            if (cleanCode.contains("@")) {
                                                activeEmail = cleanCode.trim()
                                            }

                                            saveLocalMemberProfile(
                                                LocalMemberData(
                                                    firstName = activeFirstName,
                                                    lastName = activeLastName,
                                                    email = activeEmail,
                                                    phone = activePhone,
                                                    address = activeAddress,
                                                    weight = activeWeight,
                                                    height = activeHeight,
                                                    photoUri = activePhotoUri,
                                                    gymName = activeGymName,
                                                    gymApiUrl = activeGymUrl,
                                                    assignedTrainer = activeAssignedTrainer
                                                )
                                            )

                                            coroutineScope.launch {
                                                try {
                                                    val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
                                                    val localProfile = readLocalMemberProfile()
                                                    val ssId = localProfile?.spreadsheetId.orEmpty()
                                                    val paramChar = if (activeGymUrl.contains("?")) "&" else "?"
                                                    val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
                                                    val checkInUrl = "$activeGymUrl${paramChar}action=check_in&gymId=$gymIdVal&email=${activeEmail}&memberEmail=${activeEmail}$ssParam"
                                                    val checkInReq = CheckInRequest(
                                                        action = "check_in",
                                                        gymId = gymIdVal,
                                                        memberEmail = activeEmail,
                                                        timestamp = getCurrentTimeMillis(),
                                                        spreadsheetId = ssId
                                                    )
                                                    apiClient.postData(checkInUrl, checkInReq)
                                                } catch (_: Throwable) {}
                                            }

                                            bannerTitle = "Asistencia registrada"
                                            bannerMessage = "Asistencia registrada"
                                            bannerColor = Color(0xFF00C853)
                                            showCheckInSuccessBanner = true
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MemberActionSquareCard(
                                title = "Mis Rutinas",
                                iconVector = Icons.Default.FitnessCenter,
                                iconBg = Color(0xFF0D233A),
                                iconTint = Color(0xFF42A5F5),
                                modifier = Modifier.weight(1f)
                            ) {
                                onTabSelected(3)
                            }

                            MemberActionSquareCard(
                                title = "Timer",
                                iconVector = Icons.Default.Timer,
                                iconBg = Color(0xFF3B331A),
                                iconTint = Color(0xFFFFD54F),
                                modifier = Modifier.weight(1f)
                            ) {
                                showTimerDialog = true
                            }
                        }
                    }
                }

                // 5. TARJETA "RUTINA DE HOY"
                item {
                    val todayRoutine = remember(activeEmail) {
                        val now = getCurrentTimeMillis()
                        val days = now / (24 * 60 * 60 * 1000L)
                        val dayIndex = ((days + 3) % 7).toInt()
                        val todayTag = when (dayIndex) {
                            0 -> "LU"
                            1 -> "MA"
                            2 -> "MI"
                            3 -> "JU"
                            4 -> "VI"
                            5 -> "SÁ"
                            6 -> "DO"
                            else -> ""
                        }
                        val savedJson = readLocalStorageFlag("gymcopilot_user_routines")
                        val userRoutines = if (!savedJson.isNullOrBlank()) {
                            try {
                                Json.decodeFromString<List<Routine>>(savedJson)
                            } catch (_: Throwable) { emptyList() }
                        } else { emptyList() }

                        val normalizedEmail = activeEmail.lowercase().trim()
                        val myRoutines = userRoutines.filter { 
                            it.creatorEmail.lowercase().trim() == normalizedEmail || 
                            it.assignedMemberEmails.any { e -> e.lowercase().trim() == normalizedEmail } 
                        }
                        myRoutines.find { it.days.contains(todayTag) }
                    }

                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "RUTINA DE HOY",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                if (todayRoutine != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrimaryTurquoise, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = todayRoutine.name,
                                            color = TextWhite,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { onTabSelected(3) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("VER DETALLE DE RUTINA", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🏆", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "¡Día de Descanso!",
                                            color = TextWhite,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { onTabSelected(3) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("VER TODAS MIS RUTINAS", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. TARJETA REAL "MI PROFESOR ASIGNADO"
                if (activeAssignedTrainer.isNotBlank() && !activeAssignedTrainer.equals("Sin asignar", true) && !activeAssignedTrainer.equals("N/A", true) && activeAssignedTrainer != "null") {
                    item {
                        val profColor = if (assignedProfColorInt != null && assignedProfColorInt != 0) Color(assignedProfColorInt!!) else PrimaryTurquoise
                        val profPhotoBitmap = remember(assignedProfPhotoUri) {
                            if (!assignedProfPhotoUri.isNullOrBlank()) decodeBase64ToBitmap(assignedProfPhotoUri!!) else null
                        }
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, profColor.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            color = profColor.copy(alpha = 0.15f),
                                            shape = CircleShape,
                                            border = BorderStroke(2.dp, profColor),
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (profPhotoBitmap != null) {
                                                    Image(
                                                        bitmap = profPhotoBitmap,
                                                        contentDescription = "Foto Profesor",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = "Profesor Asignado",
                                                        tint = profColor,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "MI PROFESOR ASIGNADO",
                                                color = TextGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = activeAssignedTrainer,
                                                color = TextWhite,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (assignedProfSpecialty.isNotBlank()) assignedProfSpecialty else "Entrenador Personal",
                                                color = profColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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

    // TEMPORIZADOR ADAPTATIVO MODAL INTERACTIVO
    if (showTimerDialog) {
        AdaptiveTimerSharedDialog(
            onDismiss = { showTimerDialog = false }
        )
    }

    if (showQrDialog) {
        val gymIdVal = activeGymName.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "gym_default" }
        val qrContent = "ATTENDANCE:$gymIdVal:$activeEmail"
        val qrBitmap: ImageBitmap? = remember(qrContent) { generateQrBitmap(qrContent, 512) }
        val displayName = if (activeFirstName.isNotBlank()) "$activeFirstName $activeLastName".trim() else "Socio del Gimnasio"
        val displayGym = if (activeGymName.isNotBlank() && activeGymName != "Gym 19") activeGymName else "Gimnasio"

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Mi Código QR - Asistencia", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(200.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "QR Asistencia",
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Default.QrCode2, contentDescription = "QR Asistencia", tint = Color.Black, modifier = Modifier.size(150.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = displayName,
                        color = PrimaryTurquoise,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeEmail,
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                    Text(
                        text = displayGym,
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Muestra este código en la recepción para que el administrador escanee tu asistencia.",
                        color = TextGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            containerColor = Color(0xFF081C24),
            confirmButton = {
                Button(onClick = { showQrDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCheckInSuccessBanner) {
        AlertDialog(
            onDismissRequest = { showCheckInSuccessBanner = false },
            title = { Text(bannerTitle, color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Surface(
                    color = bannerColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, bannerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = bannerColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(bannerMessage, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCheckInSuccessBanner = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)) {
                    Text("ACEPTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF081C24)
        )
    }

    if (showBankTransferDialog) {
        AlertDialog(
            onDismissRequest = { showBankTransferDialog = false },
            containerColor = Color(0xFF081C24),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = PrimaryTurquoise,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Pago de Cuota - $activeGymName", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Realiza el pago de tu cuota mensual mediante transferencia bancaria al establecimiento:",
                        color = TextWhite.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF111926))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Plan:", color = PrimaryTurquoise, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = activePlanName.ifBlank { "Membresía" },
                                    color = TextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF111926))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Alias / CBU:", color = PrimaryTurquoise, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = userBankAccount.ifBlank { "Sin configurar" },
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            openExternalUrl("https://www.mercadopago.com.ar")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir Mercado Pago", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBankTransferDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)
                ) {
                    Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }



    if (showInfoBanner) {
        AlertDialog(
            onDismissRequest = { showInfoBanner = false },
            title = { Text(infoBannerTitle, color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(infoBannerMessage, color = TextWhite, fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoBanner = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTurquoise)
                ) {
                    Text("ACEPTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF081C24)
        )
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SharedMembershipStatusCard(
    membershipStatus: String,
    nextRenewalDate: Long,
    planName: String,
    licenseModelPayer: String = "admin",
    isGymLicensePaid: Boolean = false,
    gymLicenseVence: Long = 0L,
    onLicenciaPayClick: () -> Unit = {},
    onMembresiaPayClick: () -> Unit = {}
) {
    val isActivo = membershipStatus.uppercase() == "ACTIVO" || membershipStatus.uppercase() == "ACTIVE" || membershipStatus.uppercase() == "VIGENTE"
    val statusText = if (isActivo) "Activo" else "Pendiente de pago"
    val statusColor = if (isActivo) Color(0xFF00C853) else Color(0xFFFF3D00)

    val now = getCurrentTimeMillis()
    val trialTimeLeft = if (gymLicenseVence > 0L) gymLicenseVence - now else 0L
    val trialDays = if (trialTimeLeft > 0L) trialTimeLeft / (24 * 60 * 60 * 1000L) else 0L
    val trialHours = if (trialTimeLeft > 0L) (trialTimeLeft % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L) else 0L
    val trialSubtitle = if (!isGymLicensePaid && gymLicenseVence > 0L) {
        if (trialTimeLeft > 0L) "Demo: ${trialDays}d ${trialHours}h" else "Demo Expirada"
    } else {
        "App"
    }

    val daysLeft = if (nextRenewalDate > 0) ((nextRenewalDate - now) / (24 * 60 * 60 * 1000L)).toInt() else -1
    
    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val (venceColor, venceAlpha) = when {
        daysLeft in 0..3 || daysLeft < 0 -> Color(0xFFFF3D00) to blinkAlpha
        daysLeft in 4..5 -> Color(0xFFFFD54F) to blinkAlpha
        else -> Color(0xFF00C853) to 1f
    }

    val formattedRenewal = if (nextRenewalDate > 0) formatEpochDate(nextRenewalDate) else "N/A"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, statusColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ESTADO DE MEMBRESÍA",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Plan: $planName",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Vence: $formattedRenewal",
                    color = venceColor.copy(alpha = venceAlpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = if (isActivo) Color(0xFF0B2914) else Color(0xFF380808),
                shape = CircleShape,
                border = BorderStroke(1.5.dp, statusColor),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Badge",
                        tint = statusColor,
                        modifier = Modifier.size(42.dp)
                    )
                    Icon(
                        imageVector = if (isActivo) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isActivo) "Check" else "Cross",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
        val normLicense = licenseModelPayer.trim().lowercase()
        val isMemberAll = normLicense == "member_all" || normLicense == "member" || normLicense.contains("all")
        val isMemberGymOnly = normLicense == "member_gym_only" || normLicense.contains("gym_only")

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMemberAll) {
                    val licenseBorder = if (isGymLicensePaid) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFFF3D00)
                    val licenseBgColor = if (isGymLicensePaid) Color(0xFF00222E) else Color(0xFF380808)

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.5.dp, licenseBorder, RoundedCornerShape(12.dp))
                            .clickable { onLicenciaPayClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = licenseBgColor)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhoneAndroid, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("LICENCIA", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(trialSubtitle, color = TextGray, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    val membershipBorder = if (isActivo) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFFF3D00)
                    val membershipBgColor = if (isActivo) Color(0xFF00222E) else Color(0xFF380808)

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.5.dp, membershipBorder, RoundedCornerShape(12.dp))
                            .clickable { onMembresiaPayClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = membershipBgColor)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color(0xFF009EE3).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Payment, null, tint = Color(0xFF009EE3), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("PAGOS", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Membresía", color = TextGray, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else if (isMemberGymOnly) {
                    val licenseBorder = if (isGymLicensePaid) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFFF3D00)
                    val licenseBgColor = if (isGymLicensePaid) Color(0xFF00222E) else Color(0xFF380808)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, licenseBorder, RoundedCornerShape(12.dp))
                            .clickable { onLicenciaPayClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = licenseBgColor)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhoneAndroid, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("LICENCIA", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(trialSubtitle, color = TextGray, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else {
                    val membershipBorder = if (isActivo) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFFF3D00)
                    val membershipBgColor = if (isActivo) Color(0xFF00222E) else Color(0xFF380808)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, membershipBorder, RoundedCornerShape(12.dp))
                            .clickable { onMembresiaPayClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = membershipBgColor)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color(0xFF009EE3).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Payment, null, tint = Color(0xFF009EE3), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("PAGOS", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Membresía", color = TextGray, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BroadcastMessageSharedDisplay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    0.0f to Color(0xFF6A1B9A), // Violeta lado izquierdo
                    0.25f to Color(0xFF6A1B9A).copy(alpha = 0.5f),
                    0.5f to Color(0xFF2E0814),
                    0.75f to Color.Red.copy(alpha = 0.5f),
                    1.0f to Color(0xFFB71C1C) // Rojo lado derecho
                )
            )
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
fun MemberActionSquareCard(
    title: String,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001F29)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, PrimaryTurquoise),
        modifier = modifier
            .height(125.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = iconBg,
                shape = CircleShape,
                modifier = Modifier.size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
                    } else if (iconVector != null) {
                        Icon(imageVector = iconVector, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
