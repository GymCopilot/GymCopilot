package com.zexo.gymcopilot.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.model.*
import com.zexo.gymcopilot.network.*
import com.zexo.gymcopilot.shared.network.GymKtorApiClient


import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository(
    private val apiService: GymApiService,
    private val dataStoreManager: DataStoreManager
) {
    private val sheetDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    private fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    suspend fun syncProfessorProfile(professor: Professor): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = dataStoreManager.getGymSpreadsheetId().first()
            val request = ProfileSyncRequest(
                action = "update_profile",
                professorId = professor.id,
                firstName = professor.firstName,
                lastName = professor.lastName,
                email = professor.email,
                specialty = professor.specialty,
                profileColor = professor.profileColor,
                photoUri = professor.photoUri,
                gymId = gymId,
                timestamp = System.currentTimeMillis(),
                spreadsheetId = ssId
            )
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val requestUrl = "$gymUrl${paramChar}action=update_profile&gymId=$gymId&professorId=${professor.id}&email=${java.net.URLEncoder.encode(professor.email, "UTF-8")}&firstName=${java.net.URLEncoder.encode(professor.firstName, "UTF-8")}&lastName=${java.net.URLEncoder.encode(professor.lastName, "UTF-8")}$ssParam"
            try {
                GymKtorApiClient.postData(requestUrl, request)
                Result.success(Unit)
            } catch (e: Exception) {
                val dynamicService = NetworkModule.getApiService(gymUrl)
                val response = dynamicService.postProfileSync(requestUrl, null, request)
                if (response.isSuccessful || response.code() == 302) Result.success(Unit)
                else Result.failure(Exception("Error syncing profile: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncMemberProfile(
        firstName: String, 
        lastName: String, 
        email: String, 
        phone: String, 
        address: String, 
        weight: String, 
        height: String,
        photoUri: String? = null,
        membershipStatus: String? = null,
        nextRenewalDate: Long? = null,
        planId: String? = null,
        planType: String? = null,
        assignedTrainer: String? = null
    ): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = dataStoreManager.getGymSpreadsheetId().first()
            val request = MemberSyncRequest(
                action = "update_member",
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                address = address,
                weight = weight,
                height = height,
                photoUri = photoUri,
                membershipStatus = membershipStatus,
                nextRenewalDate = nextRenewalDate,
                planId = planId,
                planType = planType,
                assignedTrainer = assignedTrainer,
                gymId = gymId,
                timestamp = System.currentTimeMillis(),
                spreadsheetId = ssId
            )
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val requestUrl = "$gymUrl${paramChar}action=update_member&gymId=$gymId&email=${java.net.URLEncoder.encode(email, "UTF-8")}&firstName=${java.net.URLEncoder.encode(firstName, "UTF-8")}&lastName=${java.net.URLEncoder.encode(lastName, "UTF-8")}&membershipStatus=${java.net.URLEncoder.encode(membershipStatus ?: "DEUDOR", "UTF-8")}$ssParam"
            try {
                GymKtorApiClient.postData(requestUrl, request)
                Result.success(Unit)
            } catch (e: Exception) {
                val dynamicService = NetworkModule.getApiService(gymUrl)
                val response = dynamicService.postMemberSync(requestUrl, null, request)
                if (response.isSuccessful || response.code() == 302) Result.success(Unit)
                else Result.failure(Exception("Error syncing member: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncMemberRoutine(
        professorId: String,
        memberEmail: String,
        routineJson: String,
        routineId: String = "",
        action: String = "update_routine",
        status: String? = null
    ): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = dataStoreManager.getGymSpreadsheetId().first()
            
            val request = RoutineSyncRequest(
                action = action,
                professorId = professorId,
                memberEmail = memberEmail.lowercase().trim(),
                routine = routineJson,
                routineId = routineId,
                status = status,
                gymId = gymId,
                timestamp = System.currentTimeMillis(),
                spreadsheetId = ssId
            )
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val requestUrl = "$gymUrl${paramChar}action=$action&gymId=$gymId&memberEmail=${java.net.URLEncoder.encode(memberEmail, "UTF-8")}$ssParam"
            try {
                GymKtorApiClient.postData(requestUrl, request)
                Result.success(Unit)
            } catch (e: Exception) {
                val dynamicService = NetworkModule.getApiService(gymUrl)
                val response = dynamicService.postRoutineSync(requestUrl, null, request)
                if (response.isSuccessful || response.code() == 302) Result.success(Unit)
                else Result.failure(Exception("Error syncing routine: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun performCheckIn(gymId: String, emailOverride: String? = null, baseUrlOverride: String? = null): Result<Unit> {
        return try {
            val gymUrl = baseUrlOverride ?: dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("Red no configurada"))

            val userRole = dataStoreManager.getUserRole().first()
            val email: String? = when {
                emailOverride != null -> emailOverride
                userRole == "professor" -> {
                    val profId = dataStoreManager.getLinkedProfessorId().first()
                    val profs = dataStoreManager.getProfessors().first()
                    val currentProf = profs.find { it.id == profId }
                    currentProf?.email ?: dataStoreManager.getUserEmail().first()
                }
                else -> {
                    val userEmail = dataStoreManager.getUserEmail().first()
                    if (userEmail.isBlank()) dataStoreManager.getLinkedMemberEmail().first() else userEmail
                }
            }

            val finalEmail = email?.lowercase(Locale.getDefault())?.trim() ?: ""
            if (finalEmail.isBlank()) return Result.failure(Exception("Email no encontrado"))
            
            val ssId = dataStoreManager.getGymSpreadsheetId().first()
            val timestamp = System.currentTimeMillis()
            val request = CheckInRequest(
                gymId = gymId,
                memberEmail = finalEmail,
                timestamp = timestamp,
                spreadsheetId = ssId
            )

            val normalizedGymId = gymId.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]"), "_")
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val requestUrl = "$gymUrl${paramChar}action=check_in&gymId=$normalizedGymId&email=${java.net.URLEncoder.encode(finalEmail, "UTF-8")}&memberEmail=${java.net.URLEncoder.encode(finalEmail, "UTF-8")}&timestamp=$timestamp$ssParam"

            try {
                GymKtorApiClient.getData(requestUrl)
                updateLocalUserAttendance(finalEmail, timestamp)
                Result.success(Unit)
            } catch (eKtor: Exception) {
                val dynamicService = NetworkModule.getApiService(gymUrl)
                val response = dynamicService.postCheckIn(requestUrl, null, request)

                if (response.isSuccessful || response.code() == 302) {
                    updateLocalUserAttendance(finalEmail, timestamp)
                    Result.success(Unit)
                }
                else Result.failure(Exception("Error servidor: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun updateLocalUserAttendance(email: String, timestamp: Long) {
        val normalizedEmail = email.lowercase(Locale.getDefault()).trim()
        val members = dataStoreManager.getMembers().first().toMutableList()
        val professors = dataStoreManager.getProfessors().first()
        val userRole = dataStoreManager.getUserRole().first()?.lowercase()
        val userEmail = dataStoreManager.getUserEmail().first().lowercase().trim()
        
        val index = members.indexOfFirst { it.email.equals(normalizedEmail, ignoreCase = true) }
        
        val professor = professors.find { it.email.equals(normalizedEmail, ignoreCase = true) }
        val fName = professor?.firstName ?: normalizedEmail.substringBefore("@").replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
        val lName = professor?.lastName ?: ""

        if (index != -1) {
            val m = members[index]
            val updatedName = if (m.lastName == "Remote" || m.lastName == "Staff" || m.lastName.isBlank()) {
                m.copy(firstName = fName, lastName = lName)
            } else m

            // Calcular si esta asistencia pertenece al periodo actual
            val plans = dataStoreManager.getSubscriptionPlans().first()
            val plan = plans.find { it.id == m.planId }
            val durationDays = plan?.durationDays ?: 30
            val startOfPeriod = m.nextRenewalDate - (durationDays.toLong() * 24 * 60 * 60 * 1000)
            
            val isCurrentPeriod = timestamp in startOfPeriod..m.nextRenewalDate
            
            val nowCal = Calendar.getInstance()
            val tsCal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val isSameMonth = nowCal.get(Calendar.YEAR) == tsCal.get(Calendar.YEAR) && 
                              nowCal.get(Calendar.MONTH) == tsCal.get(Calendar.MONTH)

            members[index] = updatedName.copy(
                lastVisit = if (timestamp > (m.lastVisit ?: 0)) timestamp else m.lastVisit,
                attendanceCount = m.attendanceCount + 1,
                currentAttendanceCount = if (isCurrentPeriod) m.currentAttendanceCount + 1 else m.currentAttendanceCount,
                monthAttendanceCount = if (isSameMonth) m.monthAttendanceCount + 1 else m.monthAttendanceCount
            )
            
            // Si la asistencia es de hoy, marcamos el flag de Wi-Fi para evitar duplicados
            if (isToday(timestamp)) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                dataStoreManager.setLastWifiCheckinDate(today)
            }
        } else {
            // Evitar crear un nuevo socio si es un profesor o el administrador actual
            val isProfessor = professor != null
            val isAdmin = userRole == "admin" && normalizedEmail == userEmail
            
            if (!isProfessor && !isAdmin) {
                members.add(Member(
                    firstName = fName, lastName = lName, email = normalizedEmail,
                    lastVisit = timestamp, attendanceCount = 1, currentAttendanceCount = 1, membershipStatus = "DEUDOR"
                ))
            }
        }
        dataStoreManager.saveMembers(members)
    }

    suspend fun syncWithRemote(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.success(Unit)

            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val syncUrl = "$gymUrl${paramChar}type=asistencia&gymId=$gymId&email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}$ssParam"

            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getAttendanceRaw(syncUrl, null)

            if (response.isSuccessful) {
                val jsonElement = response.body() ?: return Result.success(Unit)
                val localMembers = dataStoreManager.getMembers().first().toMutableList()
                val professors = dataStoreManager.getProfessors().first()
                val userRole = dataStoreManager.getUserRole().first()?.lowercase()
                val userEmail = dataStoreManager.getUserEmail().first().lowercase().trim()
                var anyUpdated = false

                // Limpieza preventiva: eliminar cualquier socio que sea Admin o Profesor (apellido "Staff" o "Remote")
                val filteredLocal = localMembers.filter { m ->
                    val isProf = professors.any { it.email.equals(m.email, ignoreCase = true) }
                    val isAdmin = userRole == "admin" && m.email.equals(userEmail, ignoreCase = true) && (m.lastName == "Staff" || m.lastName == "Remote")
                    !(isProf || isAdmin)
                }
                if (filteredLocal.size != localMembers.size) {
                    localMembers.clear()
                    localMembers.addAll(filteredLocal)
                    anyUpdated = true
                }
                
                val rows = when {
                    jsonElement.isJsonArray -> jsonElement.asJsonArray
                    jsonElement.isJsonObject && jsonElement.asJsonObject.has("data") -> jsonElement.asJsonObject.getAsJsonArray("data")
                    else -> null
                } ?: return Result.success(Unit)

                val records: List<AttendanceRecord> = rows.mapNotNull { element: JsonElement -> 
                    parseItem(element) 
                }.filter { record: AttendanceRecord -> 
                    val emailVal = record.email
                    !emailVal.isNullOrBlank() && !emailVal.contains("Email", ignoreCase = true) 
                }

                val recordsByEmail: Map<String, List<AttendanceRecord>> = records.groupBy { record: AttendanceRecord -> 
                    record.email!!.lowercase(Locale.getDefault()).trim() 
                }

                val allPlans = dataStoreManager.getSubscriptionPlans().first()

                recordsByEmail.forEach { (email: String, userRecords: List<AttendanceRecord>) ->
                    val latestRecord = userRecords.maxByOrNull { it.timestamp } ?: return@forEach
                    val remoteCount = userRecords.size
                    val index = localMembers.indexOfFirst { it.email.equals(email, ignoreCase = true) }
                    
                    val professor = professors.find { it.email.equals(email, ignoreCase = true) }
                    val fName = professor?.firstName ?: email.substringBefore("@").replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                    val lName = professor?.lastName ?: "Staff"

                    if (index != -1) {
                        val m = localMembers[index]
                        var updated = m
                        if (m.lastName == "Remote" || m.lastName == "Staff") {
                            updated = updated.copy(firstName = fName, lastName = lName)
                            anyUpdated = true
                        }

                        // Calcular asistencia del periodo actual
                        val plan = allPlans.find { it.id == m.planId }
                        val durationDays = plan?.durationDays ?: 30
                        val startOfPeriod = m.nextRenewalDate - (durationDays.toLong() * 24 * 60 * 60 * 1000)
                        val currentPeriodCount = userRecords.count { it.timestamp in startOfPeriod..m.nextRenewalDate }
                        
                        val nowCal = Calendar.getInstance()
                        val currentMonthCount = userRecords.count { 
                            val tsCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                            nowCal.get(Calendar.YEAR) == tsCal.get(Calendar.YEAR) && 
                            nowCal.get(Calendar.MONTH) == tsCal.get(Calendar.MONTH)
                        }

                        if ((latestRecord.timestamp > (m.lastVisit ?: 0)) || m.attendanceCount != remoteCount || m.currentAttendanceCount != currentPeriodCount || m.monthAttendanceCount != currentMonthCount) {
                            localMembers[index] = updated.copy(
                                lastVisit = latestRecord.timestamp, 
                                attendanceCount = remoteCount,
                                currentAttendanceCount = currentPeriodCount,
                                monthAttendanceCount = currentMonthCount
                            )
                            anyUpdated = true
                            
                            // Si el registro más reciente es de hoy, marcamos el flag de Wi-Fi
                            if (isToday(latestRecord.timestamp)) {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(latestRecord.timestamp))
                                dataStoreManager.setLastWifiCheckinDate(today)
                            }
                        } else if (updated != m) {
                            localMembers[index] = updated
                            anyUpdated = true
                        }
                    } else {
                        // Evitar crear nuevo socio para Admin o Profesores desde registros remotos
                        val isProfessor = professor != null
                        val isAdmin = userRole == "admin" && email.equals(userEmail, ignoreCase = true)
                        
                        if (!isProfessor && !isAdmin) {
                            localMembers.add(Member(
                                firstName = fName, lastName = lName, email = email,
                                lastVisit = latestRecord.timestamp, attendanceCount = remoteCount, 
                                currentAttendanceCount = remoteCount, // Default al total si es nuevo
                                membershipStatus = "DEUDOR"
                            ))
                            anyUpdated = true
                        }
                    }
                }
                if (anyUpdated) dataStoreManager.saveMembers(localMembers)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error servidor: ${response.code()}"))
            }
        } catch (e: Exception) { 
            Log.e("AttendanceSync", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseItem(item: JsonElement): AttendanceRecord? {
        return try {
            if (item.isJsonArray) {
                val arr = item.asJsonArray
                if (arr.size() < 3) return null
                val dateStr = arr[0].asString
                val gymId = arr[1].asString
                val email = arr[2].asString
                var ts = if (arr.size() > 3) {
                    val rawTs = arr[3].asString
                    rawTs.toLongOrNull() ?: rawTs.toDoubleOrNull()?.toLong() ?: 0L
                } else 0L
                if (ts <= 0L && dateStr.isNotBlank()) {
                    ts = try { sheetDateFormat.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                }
                AttendanceRecord(dateStr, gymId, email, if (ts > 0) ts else System.currentTimeMillis())
            } else if (item.isJsonObject) {
                val obj = item.asJsonObject
                val email = (obj.get("Email_Usuario") ?: obj.get("memberEmail") ?: obj.get("email") ?: obj.get("Email"))?.asString
                val dateStr = (obj.get("Fecha_Servidor") ?: obj.get("date") ?: obj.get("Fecha"))?.asString
                val rawTs = obj.get("Timestamp_Original") ?: obj.get("timestamp") ?: obj.get("Timestamp")
                var ts = if (rawTs != null && !rawTs.isJsonNull) {
                    try { rawTs.asLong } catch(e: Exception) { rawTs.asLong }
                } else 0L
                if (ts <= 0L && !dateStr.isNullOrBlank()) {
                    ts = try { sheetDateFormat.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                }
                AttendanceRecord(dateStr, null, email, if (ts > 0) ts else System.currentTimeMillis())
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun syncProduct(product: Product, isDelete: Boolean = false): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val userEmail = dataStoreManager.getUserEmail().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = dataStoreManager.getGymSpreadsheetId().first()
            val request = ProductSyncRequest(
                action = if (isDelete) "delete_product" else "update_product",
                id = product.id,
                name = product.name,
                price = product.price,
                categoryId = product.categoryId,
                imageUri = product.imageUri,
                description = product.description,
                contactMethod = product.contactMethod,
                contactPhone = product.contactPhone,
                bannerType = product.bannerType,
                bannerText = product.bannerText,
                gymId = gymId,
                email = userEmail,
                spreadsheetId = ssId
            )
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val actionStr = if (isDelete) "delete_product" else "update_product"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val requestUrl = "$gymUrl${paramChar}action=$actionStr&gymId=$gymId&email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}&id=${product.id}&name=${java.net.URLEncoder.encode(product.name ?: "", "UTF-8")}&price=${product.price}$ssParam"
            
            try {
                GymKtorApiClient.postData(requestUrl, request)
                Result.success(Unit)
            } catch (eKtor: Exception) {
                val dynamicService = NetworkModule.getApiService(gymUrl)
                val response = dynamicService.postProductSync(requestUrl, null, request)
                if (response.isSuccessful || response.code() == 302) Result.success(Unit)
                else Result.failure(Exception("Error syncing product: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncGymInfo(
        name: String? = null,
        logoUri: String? = null,
        phone: String? = null,
        address: String? = null,
        city: String? = null,
        zip: String? = null,
        country: String? = null,
        accentColor: Int? = null,
        backgroundColor: Int? = null,
        buttonStyle: Int? = null,
        broadcastMessage: String? = null,
        classInSession: Boolean? = null,
        gymIsOpen: Boolean? = null
    ): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            
            val finalName = name ?: dataStoreManager.getGymName().first()
            val finalLogo = logoUri ?: dataStoreManager.getGymLogoUri().first()
            val finalPhone = phone ?: dataStoreManager.getGymPhone().first()
            val finalAddress = address ?: dataStoreManager.getGymAddress().first()
            val finalCity = city ?: dataStoreManager.getGymCity().first()
            val finalZip = zip ?: dataStoreManager.getGymPostalCode().first()
            val finalCountry = country ?: dataStoreManager.getGymCountry().first()
            val finalColor = accentColor ?: dataStoreManager.getButtonColor().first()
        val finalBgColor = backgroundColor ?: dataStoreManager.getBackgroundColor().first()
        val finalStyle = buttonStyle ?: dataStoreManager.getButtonStyle().first()
        val finalShowBorder = dataStoreManager.getShowLogoBorder().first()
        val finalMessage = broadcastMessage ?: dataStoreManager.getBroadcastMessage().first()
        val finalClass = classInSession ?: dataStoreManager.getClassInSessionManual().first()
        val finalGymOpen = gymIsOpen ?: dataStoreManager.getGymIsOpen().first()
        
        val ssid1 = dataStoreManager.getGymWifiSsid().first()
        val ssid2 = dataStoreManager.getGymWifiSsid2().first()
        val ssid3 = dataStoreManager.getGymWifiSsid3().first()

        val gymId = finalName.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val userEmail = dataStoreManager.getUserEmail().first()

        val request = GymSyncRequest(
            action = "update_gym_info",
            name = finalName,
            logoUri = finalLogo,
            phone = finalPhone,
            address = finalAddress,
            city = finalCity,
            zip = finalZip,
            country = finalCountry,
            accentColor = finalColor,
            backgroundColor = finalBgColor,
            buttonStyle = finalStyle,
            showLogoBorder = finalShowBorder,
            broadcastMessage = finalMessage,
            classInSession = finalClass,
            gymIsOpen = finalGymOpen,
            wifiSsid = ssid1,
            wifiSsid2 = ssid2,
            wifiSsid3 = ssid3,
            gymId = gymId,
            email = userEmail
        )

        val encName = java.net.URLEncoder.encode(finalName, "UTF-8")
        val encPhone = java.net.URLEncoder.encode(finalPhone, "UTF-8")
        val encAddress = java.net.URLEncoder.encode(finalAddress, "UTF-8")
        val encCity = java.net.URLEncoder.encode(finalCity, "UTF-8")
        val encZip = java.net.URLEncoder.encode(finalZip, "UTF-8")
        val encCountry = java.net.URLEncoder.encode(finalCountry, "UTF-8")
        val encMsg = java.net.URLEncoder.encode(finalMessage, "UTF-8")
        val encEmail = java.net.URLEncoder.encode(userEmail, "UTF-8")

        val ssId = dataStoreManager.getGymSpreadsheetId().first()
        val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
        val paramChar = if (gymUrl.contains("?")) "&" else "?"
        val requestUrl = "$gymUrl${paramChar}action=update_gym_info&gymId=$gymId&email=$encEmail&name=$encName&phone=$encPhone&address=$encAddress&city=$encCity&zip=$encZip&country=$encCountry&accentColor=$finalColor&backgroundColor=$finalBgColor&buttonStyle=$finalStyle&showLogoBorder=$finalShowBorder&classInSession=$finalClass&broadcastMessage=$encMsg&gymIsOpen=$finalGymOpen$ssParam"

        try {
            GymKtorApiClient.postData(requestUrl, request)
            Result.success(Unit)
        } catch (eKtor: Exception) {
            val dynamicService = NetworkModule.getApiService(gymUrl)
            val response = dynamicService.postGymSync(requestUrl, null, request)
            if (response.isSuccessful || response.code() == 302) Result.success(Unit)
            else Result.failure(Exception("Error syncing gym info: ${response.code()}"))
        }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncGymConfigFromServer(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.success(Unit)
            
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val baseParam = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val syncUrl = "$gymUrl${baseParam}type=config&gymId=$gymId&email=$userEmail$ssParam"
            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getConfigRaw(syncUrl, null)

            if (response.isSuccessful) {
                val jsonElement = response.body() ?: return Result.success(Unit)
                val rows = when {
                    jsonElement.isJsonArray -> jsonElement.asJsonArray
                    jsonElement.isJsonObject && jsonElement.asJsonObject.has("data") -> jsonElement.asJsonObject.getAsJsonArray("data")
                    else -> null
                } ?: return Result.success(Unit)

                processConfigRows(rows)
                Result.success(Unit)
            } else Result.failure(Exception("Error servidor: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun processConfigRows(rows: JsonArray) {
        var configRow: JsonArray? = null
        for (i in 0 until rows.size()) {
            val row = rows[i].asJsonArray
            if (row.size() > 0 && row[0].asString == "GYM_CONFIG") {
                configRow = row
                break
            }
        }

        if (configRow != null && configRow!!.size() >= 9) {
            val name = configRow!![1].asString
            val logo = configRow!![2].asString
            val phone = configRow!![3].asString
            val address = configRow!![4].asString
            val city = configRow!![5].asString
            val zip = configRow!![6].asString
            val country = configRow!![7].asString
            
            // Índices corregidos según appsscript_backup.js v5.9.1+
            val colorStr = configRow!![8].asString
            val bgColorStr = if (configRow!!.size() > 9) configRow!![9].asString else ""
            val styleStr = if (configRow!!.size() > 10) configRow!![10].asString else ""
            val showBorderStr = if (configRow!!.size() > 11) configRow!![11].asString else "true"
            val classInSessionStr = if (configRow!!.size() > 12) configRow!![12].asString else "false"
            val rawMessage = if (configRow!!.size() > 13) configRow!![13].asString else ""
            val gymIsOpenStr = if (configRow!!.size() > 14) configRow!![14].asString else "true"
            
            val remoteSsid1 = if (configRow!!.size() > 15) configRow!![15].asString else ""
            val remoteSsid2 = if (configRow!!.size() > 16) configRow!![16].asString else ""
            val remoteSsid3 = if (configRow!!.size() > 17) configRow!![17].asString else ""

            Log.d("ConfigSync", "Procesando config: color=$colorStr, bgColor=$bgColorStr, style=$styleStr, border=$showBorderStr, ssids=[$remoteSsid1, $remoteSsid2, $remoteSsid3]")

            val isIsoDate = rawMessage.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.*"""))
            val message = if (isIsoDate) "" else rawMessage
            
            // Log de seguridad para depurar desplazamientos
            if (rawMessage.length > 50) Log.w("ConfigSync", "Mensaje sospechosamente largo, posible desplazamiento de columnas")

            val currentLocalName = dataStoreManager.getGymName().first()
            if (currentLocalName.isNullOrBlank() && !name.isNullOrBlank() && name != "null") {
                dataStoreManager.setGymName(name)
            }

            val currentLocalLogo = dataStoreManager.getGymLogoUri().first()
            if (currentLocalLogo.isNullOrBlank() && !logo.isNullOrBlank() && logo != "null") {
                dataStoreManager.setGymLogoUri(logo)
            }

            val currentLocalPhone = dataStoreManager.getGymPhone().first()
            if (currentLocalPhone.isNullOrBlank() && !phone.isNullOrBlank() && phone != "null") {
                dataStoreManager.setGymPhone(phone)
            }

            val currentLocalAddress = dataStoreManager.getGymAddress().first()
            if (currentLocalAddress.isNullOrBlank() && !address.isNullOrBlank() && address != "null") {
                dataStoreManager.setGymAddress(address)
            }

            val currentLocalCity = dataStoreManager.getGymCity().first()
            if (currentLocalCity.isNullOrBlank() && !city.isNullOrBlank() && city != "null") {
                dataStoreManager.setGymCity(city)
            }

            val currentLocalZip = dataStoreManager.getGymPostalCode().first()
            if (currentLocalZip.isNullOrBlank() && !zip.isNullOrBlank() && zip != "null") {
                dataStoreManager.setGymPostalCode(zip)
            }

            val currentLocalCountry = dataStoreManager.getGymCountry().first()
            if (currentLocalCountry.isNullOrBlank() && !country.isNullOrBlank() && country != "null") {
                dataStoreManager.setGymCountry(country)
            }
            dataStoreManager.setClassInSessionManual(classInSessionStr.lowercase() == "true")
            dataStoreManager.setGymIsOpen(gymIsOpenStr.lowercase() != "false")
            dataStoreManager.setShowLogoBorder(showBorderStr.lowercase() != "false")
            if (message.isNotBlank() && message != "null") dataStoreManager.setBroadcastMessage(message)
            
            if (remoteSsid1.isNotBlank() && remoteSsid1 != "null") dataStoreManager.setGymWifiSsid(remoteSsid1)
            if (remoteSsid2.isNotBlank() && remoteSsid2 != "null") dataStoreManager.setGymWifiSsid2(remoteSsid2)
            if (remoteSsid3.isNotBlank() && remoteSsid3 != "null") dataStoreManager.setGymWifiSsid3(remoteSsid3)
            
            val colorInt = colorStr.toIntOrNull()
            if (colorInt != null) {
                dataStoreManager.setButtonColor(colorInt)
            }

            val bgColorInt = bgColorStr.toIntOrNull()
            if (bgColorInt != null) {
                dataStoreManager.setBackgroundColor(bgColorInt)
            }

            val styleInt = styleStr.toIntOrNull()
            if (styleInt != null) {
                dataStoreManager.setButtonStyle(styleInt)
            }
        }
    }

    suspend fun uploadSubscriptionPlans(plans: List<SubscriptionPlan>): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()
            val request = PlansSyncRequest(plans = plans, gymId = gymId, email = userEmail, spreadsheetId = ssId)
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val requestUrl = "$gymUrl${paramChar}action=update_plans&gymId=$gymId&email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}$ssParam"
            
            try {
                GymKtorApiClient.postData(requestUrl, request)
                Result.success(Unit)
            } catch (eKtor: Exception) {
                val dynamicService = NetworkModule.getApiService(gymUrl)
                val response = dynamicService.postPlansSync(requestUrl, null, request)
                if (response.isSuccessful || response.code() == 302) Result.success(Unit)
                else Result.failure(Exception("Error syncing plans: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncSubscriptionPlansFromServer(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.success(Unit)
            
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val baseParam = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val syncUrl = "$gymUrl${baseParam}type=plans&gymId=$gymId&email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}$ssParam"
            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getConfigRaw(syncUrl, null)

            if (response.isSuccessful) {
                val jsonElement = response.body() ?: return Result.success(Unit)
                val rows = when {
                    jsonElement.isJsonArray -> jsonElement.asJsonArray
                    jsonElement.isJsonObject && jsonElement.asJsonObject.has("data") -> jsonElement.asJsonObject.getAsJsonArray("data")
                    else -> null
                } ?: return Result.success(Unit)

                processPlanRows(rows)
                Result.success(Unit)
            } else Result.failure(Exception("Error fetching plans: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun processPlanRows(rows: JsonArray) {
        val remotePlans = mutableListOf<SubscriptionPlan>()
        for (i in 1 until rows.size()) {
            val row = rows[i].asJsonArray
            if (row.size() >= 4) {
                val id = row[0].asString
                val name = row[1].asString
                val price = row[2].asString.toDoubleOrNull() ?: 0.0
                val days = row[3].asString.toIntOrNull() ?: 30
                val desc = if (row.size() > 4) row[4].asString else ""

                remotePlans.add(SubscriptionPlan(
                    id = id,
                    name = name,
                    price = price,
                    durationDays = days,
                    description = desc
                ))
            }
        }
        if (remotePlans.isNotEmpty()) {
            dataStoreManager.saveSubscriptionPlans(remotePlans)
        }
    }

    suspend fun syncMembersFromServer(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.success(Unit)

            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val baseParam = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val syncUrl = "$gymUrl${baseParam}type=members&gymId=$gymId&email=$userEmail$ssParam"
            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getAttendanceRaw(syncUrl, null)

            if (response.isSuccessful) {
                val jsonElement = response.body() ?: return Result.failure(Exception("Empty body"))
                val rows = when {
                    jsonElement.isJsonArray -> jsonElement.asJsonArray
                    jsonElement.isJsonObject && jsonElement.asJsonObject.has("data") -> jsonElement.asJsonObject.getAsJsonArray("data")
                    else -> null
                } ?: return Result.failure(Exception("Invalid format"))

                processMemberRows(rows)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error servidor: ${response.code()}"))
            }
        } catch (e: Exception) { 
            Log.e("AttendanceSync", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun processMemberRows(rows: JsonArray) {
        val remoteMembers = mutableListOf<Member>()
        for (i in 1 until rows.size()) {
            val row = rows[i].asJsonArray
            if (row.size() >= 3) {
                val fName = row[0].asString
                val lName = row[1].asString
                val email = row[2].asString.trim().lowercase()

                if (email.isNotBlank() && !email.contains("email", ignoreCase = true)) {
                    val status = if (row.size() > 3) row[3].asString else "DEUDOR"
                    val phone = if (row.size() > 4) row[4].asString else ""
                    val address = if (row.size() > 5) row[5].asString else ""
                    val weight = if (row.size() > 6) row[6].asString else ""
                    val height = if (row.size() > 7) row[7].asString else ""
                    val photo = if (row.size() > 8) row[8].asString else ""

                    val renewalTs = if (row.size() > 10) {
                        val rStr = row[10].asString
                        rStr.toLongOrNull() ?: rStr.toDoubleOrNull()?.toLong() ?: 0L
                    } else 0L

                    val pId = if (row.size() > 11) row[11].asString else null
                    val pType = if (row.size() > 12) row[12].asString else "Mensual"
                    val profAssigned = if (row.size() > 13) row[13].asString else ""

                    remoteMembers.add(Member(
                        firstName = fName,
                        lastName = lName,
                        email = email,
                        phone = phone,
                        address = address,
                        weight = weight,
                        height = height,
                        photoUri = if (photo.isBlank() || photo == "null") null else photo,
                        membershipStatus = if (status.isBlank() || status == "null") "DEUDOR" else status,
                        nextRenewalDate = if (renewalTs == 0L) System.currentTimeMillis() else renewalTs,
                        planId = if (pId.isNullOrBlank() || pId == "null") null else pId,
                        planType = pType,
                        assignedTrainer = profAssigned,
                        attendanceCount = 0,
                        currentAttendanceCount = 0,
                        monthAttendanceCount = 0
                    ))
                }
            }
        }

        if (remoteMembers.isNotEmpty()) {
            val currentLocal = dataStoreManager.getMembers().first().toMutableList()
            remoteMembers.forEach { remote ->
                val idx = currentLocal.indexOfFirst { it.email.equals(remote.email, ignoreCase = true) }
                if (idx != -1) {
                    val existing = currentLocal[idx]
                    currentLocal[idx] = remote.copy(
                        attendanceCount = maxOf(existing.attendanceCount, remote.attendanceCount),
                        currentAttendanceCount = maxOf(existing.currentAttendanceCount, remote.currentAttendanceCount),
                        monthAttendanceCount = maxOf(existing.monthAttendanceCount, remote.monthAttendanceCount),
                        lastVisit = if ((existing.lastVisit ?: 0L) > (remote.lastVisit ?: 0L)) existing.lastVisit else remote.lastVisit
                    )
                } else {
                    currentLocal.add(remote)
                }
            }
            dataStoreManager.saveMembers(currentLocal)
        }
    }

    suspend fun syncProfessorsFromServer(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))

            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val baseParam = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val syncUrl = "$gymUrl${baseParam}type=professors&gymId=$gymId&email=$userEmail$ssParam"
            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getRoutinesRaw(syncUrl, null)

            if (response.isSuccessful) {
                val jsonElement = response.body() ?: return Result.failure(Exception("Empty body"))
                val rows = when {
                    jsonElement.isJsonArray -> jsonElement.asJsonArray
                    jsonElement.isJsonObject && jsonElement.asJsonObject.has("data") -> jsonElement.asJsonObject.getAsJsonArray("data")
                    else -> null
                } ?: return Result.failure(Exception("Invalid format"))

                processProfessorRows(rows)
                Result.success(Unit)
            } else Result.failure(Exception("Error servidor: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteProfessor(professorId: String, email: String = "", firstName: String = "", lastName: String = ""): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val encProfId = java.net.URLEncoder.encode(professorId, "UTF-8")
            val encProfEmail = java.net.URLEncoder.encode(email, "UTF-8")
            val encFName = java.net.URLEncoder.encode(firstName, "UTF-8")
            val encLName = java.net.URLEncoder.encode(lastName, "UTF-8")
            val encUserEmail = java.net.URLEncoder.encode(userEmail, "UTF-8")
            val requestUrl = "$gymUrl${paramChar}action=delete_professor&gymId=$gymId&email=$encUserEmail&professorId=$encProfId&profEmail=$encProfEmail&firstName=$encFName&lastName=$encLName$ssParam"

            val body = mapOf(
                "action" to "delete_professor",
                "gymId" to gymId,
                "spreadsheetId" to ssId,
                "email" to userEmail,
                "professorId" to professorId,
                "profEmail" to email,
                "firstName" to firstName,
                "lastName" to lastName
            )

            try {
                GymKtorApiClient.postData(requestUrl, body)
                Result.success(Unit)
            } catch (e: Exception) {
                try {
                    GymKtorApiClient.getData(requestUrl)
                    Result.success(Unit)
                } catch (eGet: Exception) {
                    Result.failure(eGet)
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteMember(email: String, firstName: String = "", lastName: String = ""): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))
            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val encMemberEmail = java.net.URLEncoder.encode(email, "UTF-8")
            val encFName = java.net.URLEncoder.encode(firstName, "UTF-8")
            val encLName = java.net.URLEncoder.encode(lastName, "UTF-8")
            val encUserEmail = java.net.URLEncoder.encode(userEmail, "UTF-8")
            val requestUrl = "$gymUrl${paramChar}action=delete_member&gymId=$gymId&email=$encUserEmail&memberEmail=$encMemberEmail&targetEmail=$encMemberEmail&firstName=$encFName&lastName=$encLName$ssParam"

            val body = mapOf(
                "action" to "delete_member",
                "gymId" to gymId,
                "spreadsheetId" to ssId,
                "email" to userEmail,
                "memberEmail" to email,
                "targetEmail" to email,
                "firstName" to firstName,
                "lastName" to lastName
            )

            try {
                GymKtorApiClient.postData(requestUrl, body)
                Result.success(Unit)
            } catch (e: Exception) {
                try {
                    GymKtorApiClient.getData(requestUrl)
                    Result.success(Unit)
                } catch (eGet: Exception) {
                    Result.failure(eGet)
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun processProfessorRows(rows: JsonArray) {
        val remoteProfessors = mutableListOf<Professor>()
        for (i in 1 until rows.size()) {
            val row = rows[i].asJsonArray
            if (row.size() >= 4) {
                val profId = row[0].asString
                val fName = row[1].asString
                val lName = row[2].asString
                val email = row[3].asString
                val specialty = if (row.size() > 4) row[4].asString else ""

                var colorVal: Int? = null
                var photo: String? = null

                for (j in 4 until row.size()) {
                    val cell = row[j].asString.trim()
                    if (cell.contains("http") || cell.contains("drive.google.com") ||
                        cell.startsWith("content://") || cell.startsWith("data:image") ||
                        cell.contains("/files/") || cell.contains("/cache/")) { 
                        photo = cell
                    } else if (colorVal == null) {
                        val c = cell.toIntOrNull()
                        if (c != null && (c < -1000 || c > 1000)) {
                            colorVal = c
                        }
                    }
                }

                remoteProfessors.add(Professor(
                    id = profId,
                    firstName = fName,
                    lastName = lName,
                    email = email,
                    specialty = specialty,
                    profileColor = colorVal,
                    photoUri = photo
                ))
            }
        }

        if (remoteProfessors.isNotEmpty()) {
            val currentLocalProfessors = dataStoreManager.getProfessors().first()
            val remoteIds = remoteProfessors.map { it.id }.toSet()
            val localOnly = currentLocalProfessors.filter { it.id !in remoteIds }

            val mergedProfessors = remoteProfessors.map { remote ->
                val local = currentLocalProfessors.find { it.id == remote.id }
                if (local != null) {
                    remote.copy(
                        photoUri = if (remote.photoUri.isNullOrBlank()) local.photoUri else remote.photoUri,
                        address = if (remote.address.isBlank()) local.address else remote.address,
                        phone = if (remote.phone.isBlank()) local.phone else remote.phone,
                        profileColor = remote.profileColor ?: local.profileColor
                    )
                } else {
                    remote
                }
            } + localOnly
            dataStoreManager.saveProfessors(mergedProfessors)
        }
    }

    suspend fun syncRoutinesFromServer(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.failure(Exception("URL no configurada"))

            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val baseParam = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val syncUrl = "$gymUrl${baseParam}type=routines&gymId=$gymId&email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}$ssParam"
            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getRoutinesRaw(syncUrl, null)

            if (response.isSuccessful) {
                val jsonElement = response.body() ?: return Result.failure(Exception("Empty body"))
                val rows = when {
                    jsonElement.isJsonArray -> jsonElement.asJsonArray
                    jsonElement.isJsonObject && jsonElement.asJsonObject.has("data") -> jsonElement.asJsonObject.getAsJsonArray("data")
                    else -> null
                } ?: return Result.failure(Exception("Invalid format"))

                processRoutineRows(rows)
                Result.success(Unit)
            } else Result.failure(Exception("Error servidor: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun processRoutineRows(rows: JsonArray) {
        val userEmail = dataStoreManager.getUserEmail().first().lowercase().trim()
        val userRole = dataStoreManager.getUserRole().first()?.lowercase()
        val rawProfId = dataStoreManager.getLinkedProfessorId().first()
        val currentProfId = if (!rawProfId.isNullOrBlank()) rawProfId.trim() else userEmail

        val localRoutines = dataStoreManager.getRoutines().first().toMutableList()
        var anyUpdated = false
        val remoteRoutineIdsForUser = mutableSetOf<String>()

        for (i in 1 until rows.size()) {
            val row = rows[i].asJsonArray
            if (row.size() < 4) continue
            
            val memberEmail = row[1].asString.lowercase().trim()
            val professorIdInRow = row[2].asString.trim()
            val routineJson = if (row.size() >= 5) row[4].asString else row[3].asString

            val isForThisUser = if (userRole == "professor" || userRole == "admin") {
                (professorIdInRow.isNotBlank() && (professorIdInRow == currentProfId || professorIdInRow == userEmail)) || 
                memberEmail == userEmail
            } else {
                memberEmail == userEmail
            }

            if (isForThisUser && routineJson.isNotBlank()) {
                try {
                    var remoteRoutine = Json.decodeFromString<Routine>(routineJson)
                    remoteRoutine = remoteRoutine.copy(
                        creatorEmail = remoteRoutine.creatorEmail.lowercase().trim(),
                        assignedMemberEmails = remoteRoutine.assignedMemberEmails.map { it.lowercase().trim() }
                    )

                    if ((userRole == "professor" || userRole == "admin") && memberEmail != userEmail) {
                        remoteRoutine = remoteRoutine.copy(id = "${remoteRoutine.id}::$memberEmail")
                    }

                    remoteRoutineIdsForUser.add(remoteRoutine.id)

                    if (remoteRoutine.status == "Completada" && !isToday(remoteRoutine.updatedAt)) {
                        remoteRoutine = remoteRoutine.copy(status = "Activa")
                        anyUpdated = true
                    }
                    
                    val index = localRoutines.indexOfFirst { it.id == remoteRoutine.id }
                    if (index != -1) {
                        if (localRoutines[index] != remoteRoutine) {
                            localRoutines[index] = remoteRoutine
                            anyUpdated = true
                        }
                    } else {
                        localRoutines.add(remoteRoutine)
                        anyUpdated = true
                    }
                } catch (e: Exception) {
                    Log.e("RoutineSync", "Error decoding: ${e.message}")
                }
            }
        }

        val routinesToRemove = localRoutines.filter { routine ->
            routine.id !in remoteRoutineIdsForUser && (
                (userRole == "member" && (routine.type == "Asignada" || routine.creatorEmail != userEmail)) ||
                ((userRole == "professor" || userRole == "admin") && (routine.id.contains("::") || (routine.type == "Asignada" && routine.creatorEmail != userEmail)))
            )
        }

        if (routinesToRemove.isNotEmpty()) {
            localRoutines.removeAll(routinesToRemove)
            anyUpdated = true
        }

        if (anyUpdated) dataStoreManager.saveRoutines(localRoutines)
    }

    suspend fun syncEverythingRemote(): Result<Unit> {
        return try {
            val gymUrl = dataStoreManager.getGymApiUrl().first()
            if (gymUrl.isBlank()) return Result.success(Unit)

            val gymName = dataStoreManager.getGymName().first()
            val gymId = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val userEmail = dataStoreManager.getUserEmail().first()
            val ssId = dataStoreManager.getGymSpreadsheetId().first()

            val baseParam = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val megaUrl = "$gymUrl${baseParam}type=all&gymId=$gymId&email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}$ssParam"
            val apiServiceGet = NetworkModule.getApiServiceForGet(gymUrl)
            val response = apiServiceGet.getMegaSyncRaw(megaUrl, null)

            if (response.isSuccessful) {
                val megaResponse = response.body() ?: return Result.failure(Exception("Respuesta vacía"))
                val data = megaResponse.data ?: return Result.success(Unit)

                // 1. Configuración
                data.config?.let { processConfigRows(it) }
                // 2. Planes
                data.plans?.let { processPlanRows(it) }
                // 3. Miembros
                data.members?.let { processMemberRows(it) }
                // 4. Profesores
                data.professors?.let { processProfessorRows(it) }
                // 5. Rutinas
                data.routines?.let { processRoutineRows(it) }
                // 6. Horarios
                data.schedules?.let { processScheduleRows(it) }
                
                Log.d("MegaSync", "Sincronización total completada con éxito")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("MegaSync", "Error en sincronización total: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun processScheduleRows(rows: JsonArray) {
        val result = mutableMapOf<String, MutableList<ScheduleEntry>>()
        try {
            val startIdx = if (rows.size() > 0 && rows[0].isJsonArray && rows[0].asJsonArray[0].asString == "ID_Profesor") 1 else 0

            for (i in startIdx until rows.size()) {
                val row = rows[i].asJsonArray
                if (row.size() < 4) continue

                val rawProfId = row[0].asString.trim()
                if (rawProfId.isEmpty()) continue
                
                // Normalizar el ID del gimnasio para que coincida con lo que espera la UI (GENERAL_GYM)
                val profId = if (rawProfId.equals("general_gym", ignoreCase = true)) "GENERAL_GYM" else rawProfId

                val dayOrDate = row[1].asString.trim()
                val start = formatTime(row[2].asString.trim())
                val end = formatTime(row[3].asString.trim())
                val name = if (row.size() > 4) row[4].asString.trim() else ""
                val emailsRaw = if (row.size() > 5) row[5].asString.trim() else ""
                val emails = if (emailsRaw.isNotEmpty()) emailsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()

                val entry = if (dayOrDate.contains("-")) {
                    ScheduleEntry(date = dayOrDate, startTime = start, endTime = end, eventName = name, assignedMemberEmails = emails)
                } else {
                    val dayInt = dayOrDate.toDoubleOrNull()?.toInt() ?: dayOrDate.toIntOrNull()
                    if (dayInt != null) ScheduleEntry(dayOfWeek = dayInt, startTime = start, endTime = end, eventName = name, assignedMemberEmails = emails) else null
                }

                if (entry != null) {
                    result.getOrPut(profId) { mutableListOf() }.add(entry)
                }
            }
            if (result.isNotEmpty()) dataStoreManager.saveAllSchedules(result)
        } catch (e: Exception) {
            Log.e("MegaSync", "Error horarios: ${e.message}")
        }
    }

    private fun formatTime(raw: String): String {
        return if (raw.contains("T") && raw.contains(":")) {
            try { raw.substringAfter("T").substring(0, 5) } catch (e: Exception) { raw }
        } else raw
    }

    suspend fun linkProfessor(id: String, firstName: String, lastName: String, email: String, url: String, gymName: String = "", color: Int? = null) {
        dataStoreManager.setLinkedProfessorId(id)
        dataStoreManager.setUserRole("professor")
        dataStoreManager.setUserName("$firstName $lastName")
        dataStoreManager.setUserEmail(email.lowercase().trim())
        dataStoreManager.setGymApiUrl(url)
        dataStoreManager.setAuthToken("prof_session")
        dataStoreManager.setShortcutCreated(false)
        if (gymName.isNotBlank()) dataStoreManager.setGymName(gymName)
    }

    suspend fun linkMember(firstName: String, lastName: String, email: String, url: String, gymName: String = "") {
        dataStoreManager.setLinkedMemberEmail(email.lowercase().trim())
        dataStoreManager.setUserRole("member")
        dataStoreManager.setUserName("$firstName $lastName")
        dataStoreManager.setUserEmail(email.lowercase().trim())
        dataStoreManager.setGymApiUrl(url)
        dataStoreManager.setAuthToken("member_session")
        dataStoreManager.setShortcutCreated(false)
        if (gymName.isNotBlank()) dataStoreManager.setGymName(gymName)
    }

    suspend fun setupGymFromAdminQr(gymId: String, gymName: String, url: String) {
        if (url.isNotBlank()) dataStoreManager.setGymApiUrl(url)
        if (gymName.isNotBlank()) dataStoreManager.setGymName(gymName)
        dataStoreManager.setShortcutCreated(false)
    }
}
