package com.zexo.gymcopilot

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.zexo.gymcopilot.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_settings")

typealias Member = com.zexo.gymcopilot.model.Member
typealias Professor = com.zexo.gymcopilot.model.Professor
typealias Exercise = com.zexo.gymcopilot.model.Exercise
typealias Routine = com.zexo.gymcopilot.model.Routine
typealias ScheduleEntry = com.zexo.gymcopilot.model.ScheduleEntry
typealias ChatMessage = com.zexo.gymcopilot.model.ChatMessage
typealias PaymentRecord = com.zexo.gymcopilot.model.PaymentRecord
typealias Product = com.zexo.gymcopilot.model.Product
typealias SubscriptionPlan = com.zexo.gymcopilot.model.SubscriptionPlan



class DataStoreManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val APP_LANGUAGE = stringPreferencesKey("APP_LANGUAGE")
        val APP_CURRENCY = stringPreferencesKey("APP_CURRENCY")
        val MEMBER_DESIGNATION = stringPreferencesKey("MEMBER_DESIGNATION")
        val USER_ROLE = stringPreferencesKey("USER_ROLE")
        val GYM_LOGO_URI = stringPreferencesKey("GYM_LOGO_URI")
        val GYM_NAME = stringPreferencesKey("GYM_NAME")
        val GYM_NAME_FONT = stringPreferencesKey("GYM_NAME_FONT")
        val GYM_SPREADSHEET_ID = stringPreferencesKey("GYM_SPREADSHEET_ID")
        val GYM_PHONE = stringPreferencesKey("GYM_PHONE")
        val GYM_BACKGROUND_URI = stringPreferencesKey("GYM_BACKGROUND_URI")
        val GYM_ADDRESS = stringPreferencesKey("GYM_ADDRESS")
        val GYM_CITY = stringPreferencesKey("GYM_CITY")
        val GYM_POSTAL_CODE = stringPreferencesKey("GYM_POSTAL_CODE")
        val GYM_COUNTRY = stringPreferencesKey("GYM_COUNTRY")
        val SHOW_LOGO_BORDER = booleanPreferencesKey("SHOW_LOGO_BORDER")
        val BUTTON_COLOR = intPreferencesKey("BUTTON_COLOR")
        val BUTTON_STYLE = intPreferencesKey("BUTTON_STYLE") 
        val BACKGROUND_COLOR = intPreferencesKey("BACKGROUND_COLOR")
        val STORE_CATEGORIES = stringSetPreferencesKey("STORE_CATEGORIES")
        val CUSTOM_CATEGORY_IDS = stringSetPreferencesKey("CUSTOM_CATEGORY_IDS")
        
        val LAST_SELECTED_CATEGORY = stringPreferencesKey("LAST_SELECTED_CATEGORY")
        val STORE_PRODUCTS = stringPreferencesKey("STORE_PRODUCTS")
        val CART_ITEMS = stringPreferencesKey("CART_ITEMS")
        val PREFERRED_CONTACT_METHOD = stringPreferencesKey("PREFERRED_CONTACT_METHOD")
        val CONTACT_PHONE = stringPreferencesKey("CONTACT_PHONE")
        val STORE_ANNOUNCEMENT = stringPreferencesKey("STORE_ANNOUNCEMENT")

        val LINKED_PROFESSOR_ID = stringPreferencesKey("LINKED_PROFESSOR_ID")
        val LINKED_MEMBER_EMAIL = stringPreferencesKey("LINKED_MEMBER_EMAIL")

        val USER_NAME = stringPreferencesKey("USER_NAME")
        val USER_EMAIL = stringPreferencesKey("USER_EMAIL")
        val USER_PHONE = stringPreferencesKey("USER_PHONE")
        val USER_WEIGHT = stringPreferencesKey("USER_WEIGHT")
        val USER_HEIGHT = stringPreferencesKey("USER_HEIGHT")
        val USER_ADDRESS = stringPreferencesKey("USER_ADDRESS")
        val USER_ID_NUMBER = stringPreferencesKey("USER_ID_NUMBER")
        val USER_TAX_ID = stringPreferencesKey("USER_TAX_ID")
        val USER_SPECIALTY = stringPreferencesKey("USER_SPECIALTY")
        val USER_EMERGENCY_CONTACT = stringPreferencesKey("USER_EMERGENCY_CONTACT")
        val USER_BANK_ACCOUNT = stringPreferencesKey("USER_BANK_ACCOUNT")
        val USER_PHOTO_URI = stringPreferencesKey("USER_PHOTO_URI")

        val GYM_MEMBERS = stringPreferencesKey("GYM_MEMBERS")
        val GYM_PROFESSORS = stringPreferencesKey("GYM_PROFESSORS")
        val GYM_ROUTINES = stringPreferencesKey("GYM_ROUTINES")
        val SUBSCRIPTION_PLANS = stringPreferencesKey("SUBSCRIPTION_PLANS")
        val PAYMENT_RECORDS = stringPreferencesKey("PAYMENT_RECORDS")
        
        val AUTH_TOKEN = stringPreferencesKey("AUTH_TOKEN")
        val GOOGLE_ACCESS_TOKEN = stringPreferencesKey("GOOGLE_ACCESS_TOKEN")
        val GYM_API_URL = stringPreferencesKey("GYM_API_URL")
        val GYM_SCRIPT_ID = stringPreferencesKey("GYM_SCRIPT_ID")

        val GYM_IS_OPEN = booleanPreferencesKey("GYM_IS_OPEN")
        val CLASS_IN_SESSION_MANUAL = booleanPreferencesKey("CLASS_IN_SESSION_MANUAL")
        val CLASS_OVERRIDE_UNTIL = longPreferencesKey("CLASS_OVERRIDE_UNTIL")
        val ASSISTANT_ENABLED = booleanPreferencesKey("ASSISTANT_ENABLED")
        val WELCOME_DISMISSED = booleanPreferencesKey("WELCOME_DISMISSED")
        val CLOUD_HELP_DISMISSED = booleanPreferencesKey("CLOUD_HELP_DISMISSED")
        val STEP2_HELP_DISMISSED = booleanPreferencesKey("STEP2_HELP_DISMISSED")
        val DELAY_HELP_DISMISSED = booleanPreferencesKey("DELAY_HELP_DISMISSED")
        val READY_DISMISSED = booleanPreferencesKey("READY_DISMISSED")
        val DASHBOARD_TOUR_STEP1_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_STEP1_DISMISSED")
        val DASHBOARD_TOUR_STEP1_CENTRAL_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_STEP1_CENTRAL_DISMISSED")
        val SETTINGS_TOUR_SKIN_DISMISSED = booleanPreferencesKey("SETTINGS_TOUR_SKIN_DISMISSED")
        val APPEARANCE_TOUR_DISMISSED = booleanPreferencesKey("APPEARANCE_TOUR_DISMISSED")
        val DASHBOARD_TOUR_BROADCAST_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_BROADCAST_DISMISSED")
        val DASHBOARD_TOUR_PLANS_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_PLANS_DISMISSED")
        val DASHBOARD_TOUR_INCOME_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_INCOME_DISMISSED")
        val DASHBOARD_TOUR_METRICS_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_METRICS_DISMISSED")
        val DASHBOARD_TOUR_SCHEDULE_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_SCHEDULE_DISMISSED")
        val DASHBOARD_TOUR_QR_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_QR_DISMISSED")
        val DASHBOARD_TOUR_PROFESSORS_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_PROFESSORS_DISMISSED")
        val DASHBOARD_TOUR_MEMBERS_NAV_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_MEMBERS_NAV_DISMISSED")
        val DASHBOARD_TOUR_STORE_NAV_DISMISSED = booleanPreferencesKey("DASHBOARD_TOUR_STORE_NAV_DISMISSED")
        val GYM_BROADCAST_MESSAGE = stringPreferencesKey("GYM_BROADCAST_MESSAGE")
        val SHORTCUT_CREATED = booleanPreferencesKey("SHORTCUT_CREATED")
        val LAST_SHORTCUT_NAME = stringPreferencesKey("LAST_SHORTCUT_NAME")
        val LAST_SHORTCUT_LOGO_URI = stringPreferencesKey("LAST_SHORTCUT_LOGO_URI")
        
        val PROFESSOR_SCHEDULES = stringPreferencesKey("PROFESSOR_SCHEDULES")

        val APP_LOCK_EXPIRATION_TIME = longPreferencesKey("APP_LOCK_EXPIRATION_TIME")
        val GYM_WIFI_SSID = stringPreferencesKey("GYM_WIFI_SSID")
        val GYM_WIFI_SSID2 = stringPreferencesKey("GYM_WIFI_SSID2")
        val GYM_WIFI_SSID3 = stringPreferencesKey("GYM_WIFI_SSID3")
        val LAST_WIFI_CHECKIN_DATE = stringPreferencesKey("LAST_WIFI_CHECKIN_DATE")
    }

    suspend fun setAuthToken(token: String) { dataStore.edit { it[AUTH_TOKEN] = token } }
    fun getAuthToken(): Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }

    suspend fun setGoogleAccessToken(token: String) { dataStore.edit { it[GOOGLE_ACCESS_TOKEN] = token } }
    fun getGoogleAccessToken(): Flow<String> = dataStore.data.map { it[GOOGLE_ACCESS_TOKEN] ?: "" }

    suspend fun setGymApiUrl(url: String) { dataStore.edit { it[GYM_API_URL] = url } }
    fun getGymApiUrl(): Flow<String> = dataStore.data.map { it[GYM_API_URL] ?: "" }

    suspend fun setGymScriptId(id: String) { dataStore.edit { it[GYM_SCRIPT_ID] = id } }
    fun getGymScriptId(): Flow<String> = dataStore.data.map { it[GYM_SCRIPT_ID] ?: "" }

    suspend fun setAppLanguage(language: String) { dataStore.edit { it[APP_LANGUAGE] = language } }
    fun getAppLanguage(): Flow<String> = dataStore.data.map { it[APP_LANGUAGE] ?: "Español" }

    suspend fun setAppCurrency(currency: String) { dataStore.edit { it[APP_CURRENCY] = currency } }
    fun getAppCurrency(): Flow<String> = dataStore.data.map { it[APP_CURRENCY] ?: "Peso Argentino" }

    suspend fun setMemberDesignation(designation: String) { dataStore.edit { it[MEMBER_DESIGNATION] = designation } }
    fun getMemberDesignation(): Flow<String> = dataStore.data.map { it[MEMBER_DESIGNATION] ?: "Alumno" }

    suspend fun setUserRole(role: String) { dataStore.edit { it[USER_ROLE] = role } }
    fun getUserRole(): Flow<String?> = dataStore.data.map { it[USER_ROLE] }

    suspend fun setLinkedProfessorId(id: String) { dataStore.edit { it[LINKED_PROFESSOR_ID] = id } }
    fun getLinkedProfessorId(): Flow<String?> = dataStore.data.map { it[LINKED_PROFESSOR_ID] }

    suspend fun setLinkedMemberEmail(email: String) { dataStore.edit { it[LINKED_MEMBER_EMAIL] = email } }
    fun getLinkedMemberEmail(): Flow<String?> = dataStore.data.map { it[LINKED_MEMBER_EMAIL] }

    suspend fun setGymLogoUri(uri: String) { dataStore.edit { it[GYM_LOGO_URI] = uri } }
    fun getGymLogoUri(): Flow<String?> = dataStore.data.map { it[GYM_LOGO_URI] }

    suspend fun setGymName(name: String) { dataStore.edit { it[GYM_NAME] = name } }
    fun getGymName(): Flow<String> = dataStore.data.map { it[GYM_NAME] ?: "" }

    suspend fun setGymSpreadsheetId(id: String) { dataStore.edit { it[GYM_SPREADSHEET_ID] = id } }
    fun getGymSpreadsheetId(): Flow<String> = dataStore.data.map { it[GYM_SPREADSHEET_ID] ?: "" }

    suspend fun setGymNameFont(font: String) { dataStore.edit { it[GYM_NAME_FONT] = font } }
    fun getGymNameFont(): Flow<String> = dataStore.data.map { it[GYM_NAME_FONT] ?: "Default" }

    suspend fun setGymPhone(phone: String) { dataStore.edit { it[GYM_PHONE] = phone } }
    fun getGymPhone(): Flow<String> = dataStore.data.map { it[GYM_PHONE] ?: "" }

    suspend fun setGymBackgroundUri(uri: String) { dataStore.edit { it[GYM_BACKGROUND_URI] = uri } } 
    fun getGymBackgroundUri(): Flow<String?> = dataStore.data.map { it[GYM_BACKGROUND_URI] }
    suspend fun clearGymBackgroundUri() { dataStore.edit { it.remove(GYM_BACKGROUND_URI) } }

    suspend fun setGymAddress(address: String) { dataStore.edit { it[GYM_ADDRESS] = address } }
    fun getGymAddress(): Flow<String> = dataStore.data.map { it[GYM_ADDRESS] ?: "" }

    suspend fun setGymCity(city: String) { dataStore.edit { it[GYM_CITY] = city } }
    fun getGymCity(): Flow<String> = dataStore.data.map { it[GYM_CITY] ?: "" }

    suspend fun setGymPostalCode(code: String) { dataStore.edit { it[GYM_POSTAL_CODE] = code } }
    fun getGymPostalCode(): Flow<String> = dataStore.data.map { it[GYM_POSTAL_CODE] ?: "" }

    suspend fun setGymCountry(country: String) { dataStore.edit { it[GYM_COUNTRY] = country } }
    fun getGymCountry(): Flow<String> = dataStore.data.map { it[GYM_COUNTRY] ?: "" }

    suspend fun setShowLogoBorder(show: Boolean) { dataStore.edit { it[SHOW_LOGO_BORDER] = show } }
    fun getShowLogoBorder(): Flow<Boolean> = dataStore.data.map { it[SHOW_LOGO_BORDER] ?: true }

    suspend fun setButtonColor(color: Int) { dataStore.edit { it[BUTTON_COLOR] = color } }
    suspend fun resetButtonColor() { dataStore.edit { it.remove(BUTTON_COLOR) } }
    fun getButtonColor(): Flow<Int?> = dataStore.data.map { it[BUTTON_COLOR] }

    suspend fun setButtonStyle(style: Int) { dataStore.edit { it[BUTTON_STYLE] = style } }
    fun getButtonStyle(): Flow<Int> = dataStore.data.map { it[BUTTON_STYLE] ?: 1 }

    suspend fun setBackgroundColor(color: Int) { dataStore.edit { it[BACKGROUND_COLOR] = color } }
    suspend fun resetBackgroundColor() { dataStore.edit { it.remove(BACKGROUND_COLOR) } }
    fun getBackgroundColor(): Flow<Int?> = dataStore.data.map { it[BACKGROUND_COLOR] }

    suspend fun setStoreCategories(categories: Set<String>) { dataStore.edit { it[STORE_CATEGORIES] = categories } }
    fun getStoreCategories(): Flow<Set<String>> = dataStore.data.map { it[STORE_CATEGORIES] ?: emptySet() }

    suspend fun setCustomCategoryIds(ids: Set<String>) { dataStore.edit { it[CUSTOM_CATEGORY_IDS] = ids } }
    fun getCustomCategoryIds(): Flow<Set<String>> = dataStore.data.map { it[CUSTOM_CATEGORY_IDS] ?: emptySet() }

    suspend fun setLastSelectedCategory(categoryId: String) { dataStore.edit { it[LAST_SELECTED_CATEGORY] = categoryId } }
    fun getLastSelectedCategory(): Flow<String?> = dataStore.data.map { it[LAST_SELECTED_CATEGORY] }

    fun getCategoryColor(categoryId: String): Flow<Int?> = dataStore.data.map { it[intPreferencesKey("CAT_COLOR_$categoryId")] }
    suspend fun setCategoryColor(categoryId: String, color: Int) { dataStore.edit { it[intPreferencesKey("CAT_COLOR_$categoryId")] = color } }

    fun getCategoryUri(categoryId: String): Flow<String?> = dataStore.data.map { it[stringPreferencesKey("CAT_URI_$categoryId")] }
    suspend fun setCategoryUri(categoryId: String, uri: String) { dataStore.edit { it[stringPreferencesKey("CAT_URI_$categoryId")] = uri } }

    fun getCategoryName(categoryId: String): Flow<String?> = dataStore.data.map { it[stringPreferencesKey("CAT_NAME_$categoryId")] }
    suspend fun setCategoryName(categoryId: String, name: String) { dataStore.edit { it[stringPreferencesKey("CAT_NAME_$categoryId")] = name } }

    suspend fun resetCategory(categoryId: String) {
        dataStore.edit { 
            it.remove(intPreferencesKey("CAT_COLOR_$categoryId"))
            it.remove(stringPreferencesKey("CAT_URI_$categoryId"))
            it.remove(stringPreferencesKey("CAT_NAME_$categoryId"))
        }
    }
    
    fun getProducts(): Flow<List<Product>> = dataStore.data.map { preferences ->
        val productsJson = preferences[STORE_PRODUCTS] ?: "[]"
        try { Json.decodeFromString<List<Product>>(productsJson) } catch (e: Exception) { emptyList() }
    }

    suspend fun addProduct(product: Product) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Product>>(preferences[STORE_PRODUCTS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.add(product)
            preferences[STORE_PRODUCTS] = Json.encodeToString(current)
        }
    }

    suspend fun updateProduct(updatedProduct: Product) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Product>>(preferences[STORE_PRODUCTS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            val index = current.indexOfFirst { it.id == updatedProduct.id }
            if (index != -1) {
                current[index] = updatedProduct
                preferences[STORE_PRODUCTS] = Json.encodeToString(current)
            }
        }
    }

    suspend fun deleteProduct(productId: String) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Product>>(preferences[STORE_PRODUCTS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.removeAll { it.id == productId }
            preferences[STORE_PRODUCTS] = Json.encodeToString(current)
        }
    }

    suspend fun saveAllProducts(products: List<Product>) {
        dataStore.edit { preferences ->
            preferences[STORE_PRODUCTS] = Json.encodeToString(products)
        }
    }
    
    fun getCartItems(): Flow<List<Product>> = dataStore.data.map { preferences ->
        val cartJson = preferences[CART_ITEMS] ?: "[]"
        try { Json.decodeFromString<List<Product>>(cartJson) } catch (e: Exception) { emptyList() }
    }

    suspend fun addToCart(product: Product) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Product>>(preferences[CART_ITEMS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.add(product)
            preferences[CART_ITEMS] = Json.encodeToString(current)
        }
    }

    suspend fun removeFromCart(productId: String) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Product>>(preferences[CART_ITEMS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            val index = current.indexOfFirst { it.id == productId }
            if (index != -1) {
                current.removeAt(index)
                preferences[CART_ITEMS] = Json.encodeToString(current)
            }
        }
    }

    suspend fun clearCart() { dataStore.edit { it.remove(CART_ITEMS) } }
    
    suspend fun setPreferredContactMethod(method: String) { dataStore.edit { it[PREFERRED_CONTACT_METHOD] = method } }
    fun getPreferredContactMethod(): Flow<String> = dataStore.data.map { it[PREFERRED_CONTACT_METHOD] ?: "whatsapp" }

    suspend fun setContactPhone(phone: String) { dataStore.edit { it[CONTACT_PHONE] = phone } }
    fun getContactPhone(): Flow<String> = dataStore.data.map { it[CONTACT_PHONE] ?: "" }

    suspend fun setStoreAnnouncement(announcement: String) { dataStore.edit { it[STORE_ANNOUNCEMENT] = announcement } }
    fun getStoreAnnouncement(): Flow<String> = dataStore.data.map { it[STORE_ANNOUNCEMENT] ?: "" }

    suspend fun setUserName(name: String) { dataStore.edit { it[USER_NAME] = name } }
    fun getUserName(): Flow<String> = dataStore.data.map { it[USER_NAME] ?: "" }

    suspend fun setUserEmail(email: String) { dataStore.edit { it[USER_EMAIL] = email } }
    fun getUserEmail(): Flow<String> = dataStore.data.map { it[USER_EMAIL] ?: "" }

    suspend fun setUserPhone(phone: String) { dataStore.edit { it[USER_PHONE] = phone } }
    fun getUserPhone(): Flow<String> = dataStore.data.map { it[USER_PHONE] ?: "" }

    suspend fun setUserWeight(weight: String) { dataStore.edit { it[USER_WEIGHT] = weight } }
    fun getUserWeight(): Flow<String> = dataStore.data.map { it[USER_WEIGHT] ?: "" }

    suspend fun setUserHeight(height: String) { dataStore.edit { it[USER_HEIGHT] = height } }
    fun getUserHeight(): Flow<String> = dataStore.data.map { it[USER_HEIGHT] ?: "" }

    suspend fun setUserAddress(address: String) { dataStore.edit { it[USER_ADDRESS] = address } }
    fun getUserAddress(): Flow<String> = dataStore.data.map { it[USER_ADDRESS] ?: "" }

    suspend fun setUserIdNumber(idNumber: String) { dataStore.edit { it[USER_ID_NUMBER] = idNumber } }
    fun getUserIdNumber(): Flow<String> = dataStore.data.map { it[USER_ID_NUMBER] ?: "" }

    suspend fun setUserTaxId(taxId: String) { dataStore.edit { it[USER_TAX_ID] = taxId } }
    fun getUserTaxId(): Flow<String> = dataStore.data.map { it[USER_TAX_ID] ?: "" }

    suspend fun setUserSpecialty(specialty: String) { dataStore.edit { it[USER_SPECIALTY] = specialty } }
    fun getUserSpecialty(): Flow<String> = dataStore.data.map { it[USER_SPECIALTY] ?: "" }

    suspend fun setUserEmergencyContact(contact: String) { dataStore.edit { it[USER_EMERGENCY_CONTACT] = contact } }
    fun getUserEmergencyContact(): Flow<String> = dataStore.data.map { it[USER_EMERGENCY_CONTACT] ?: "" }

    suspend fun setUserBankAccount(account: String) { dataStore.edit { it[USER_BANK_ACCOUNT] = account } }
    fun getUserBankAccount(): Flow<String> = dataStore.data.map { it[USER_BANK_ACCOUNT] ?: "" }

    suspend fun setUserPhotoUri(uri: String) { dataStore.edit { it[USER_PHOTO_URI] = uri } }
    fun getUserPhotoUri(): Flow<String> = dataStore.data.map { it[USER_PHOTO_URI] ?: "" }

    fun getMembers(): Flow<List<Member>> = dataStore.data.map { preferences ->
        val membersJson = preferences[GYM_MEMBERS] ?: "[]"
        try { Json.decodeFromString<List<Member>>(membersJson) } catch (e: Exception) { emptyList() }
    }

    suspend fun addMember(member: Member) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Member>>(preferences[GYM_MEMBERS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            if (current.none { it.email == member.email }) {
                current.add(member)
                preferences[GYM_MEMBERS] = Json.encodeToString(current)
            }
        }
    }

    suspend fun updateMember(updatedMember: Member) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Member>>(preferences[GYM_MEMBERS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            val index = current.indexOfFirst { it.email == updatedMember.email }
            if (index != -1) {
                current[index] = updatedMember
                preferences[GYM_MEMBERS] = Json.encodeToString(current)
            }
        }
    }

    suspend fun removeMember(email: String, fullName: String = "") {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Member>>(preferences[GYM_MEMBERS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.removeAll { 
                (email.isNotBlank() && it.email.trim().equals(email.trim(), ignoreCase = true)) ||
                (email.isBlank() && fullName.isNotBlank() && it.fullName.trim().equals(fullName.trim(), ignoreCase = true)) ||
                (email.isBlank() && fullName.isBlank() && it.email.isBlank() && it.fullName.isBlank()) ||
                it.email == email
            }
            preferences[GYM_MEMBERS] = Json.encodeToString(current)
        }
    }

    suspend fun saveMembers(members: List<Member>) { dataStore.edit { it[GYM_MEMBERS] = Json.encodeToString(members) } }

    fun getProfessors(): Flow<List<Professor>> = dataStore.data.map { preferences ->
        val profsJson = preferences[GYM_PROFESSORS] ?: "[]"
        try { Json.decodeFromString<List<Professor>>(profsJson) } catch (e: Exception) { emptyList() }
    }

    suspend fun addProfessor(professor: Professor) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Professor>>(preferences[GYM_PROFESSORS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.add(professor)
            preferences[GYM_PROFESSORS] = Json.encodeToString(current)
        }
    }

    suspend fun updateProfessor(updatedProfessor: Professor) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Professor>>(preferences[GYM_PROFESSORS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            val index = current.indexOfFirst { it.id == updatedProfessor.id }
            if (index != -1) {
                current[index] = updatedProfessor
                preferences[GYM_PROFESSORS] = Json.encodeToString(current)
            } else {
                current.add(updatedProfessor)
                preferences[GYM_PROFESSORS] = Json.encodeToString(current)
            }
        }
    }

    suspend fun removeProfessor(id: String) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Professor>>(preferences[GYM_PROFESSORS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.removeAll { it.id == id }
            preferences[GYM_PROFESSORS] = Json.encodeToString(current)
        }
    }

    suspend fun saveProfessors(professors: List<Professor>) { dataStore.edit { it[GYM_PROFESSORS] = Json.encodeToString(professors) } }

    fun getRoutines(): Flow<List<Routine>> = dataStore.data.map { preferences ->
        val routinesJson = preferences[GYM_ROUTINES] ?: "[]"
        try { Json.decodeFromString<List<Routine>>(routinesJson) } catch (e: Exception) { emptyList() }
    }

    suspend fun addRoutine(routine: Routine) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Routine>>(preferences[GYM_ROUTINES] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.add(routine)
            preferences[GYM_ROUTINES] = Json.encodeToString(current)
        }
    }

    suspend fun updateRoutine(updatedRoutine: Routine) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Routine>>(preferences[GYM_ROUTINES] ?: "[]") } catch (e: Exception) { mutableListOf() }
            val index = current.indexOfFirst { it.id == updatedRoutine.id }
            if (index != -1) {
                current[index] = updatedRoutine
                preferences[GYM_ROUTINES] = Json.encodeToString(current)
            }
        }
    }

    suspend fun deleteRoutine(routineId: String) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<Routine>>(preferences[GYM_ROUTINES] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.removeAll { it.id == routineId }
            preferences[GYM_ROUTINES] = Json.encodeToString(current)
        }
    }

    suspend fun saveRoutines(routines: List<Routine>) {
        dataStore.edit { preferences ->
            preferences[GYM_ROUTINES] = Json.encodeToString(routines)
        }
    }

    fun getGymIsOpen(): Flow<Boolean> = dataStore.data.map { it[GYM_IS_OPEN] ?: true }
    suspend fun setGymIsOpen(isOpen: Boolean) { dataStore.edit { it[GYM_IS_OPEN] = isOpen } }

    fun getClassInSessionManual(): Flow<Boolean> = dataStore.data.map { it[CLASS_IN_SESSION_MANUAL] ?: false }
    suspend fun setClassInSessionManual(inSession: Boolean) { dataStore.edit { it[CLASS_IN_SESSION_MANUAL] = inSession } }

    fun getClassOverrideUntil(): Flow<Long> = dataStore.data.map { it[CLASS_OVERRIDE_UNTIL] ?: 0L }
    suspend fun setClassOverrideUntil(timestamp: Long) { dataStore.edit { it[CLASS_OVERRIDE_UNTIL] = timestamp } }

    fun getAssistantEnabled(): Flow<Boolean> = dataStore.data.map { it[ASSISTANT_ENABLED] ?: false }
    suspend fun setAssistantEnabled(enabled: Boolean) { dataStore.edit { it[ASSISTANT_ENABLED] = enabled } }

    fun getWelcomeDismissed(): Flow<Boolean> = dataStore.data.map { it[WELCOME_DISMISSED] ?: false }
    suspend fun setWelcomeDismissed(dismissed: Boolean) { dataStore.edit { it[WELCOME_DISMISSED] = dismissed } }

    fun getCloudHelpDismissed(): Flow<Boolean> = dataStore.data.map { it[CLOUD_HELP_DISMISSED] ?: false }
    suspend fun setCloudHelpDismissed(dismissed: Boolean) { dataStore.edit { it[CLOUD_HELP_DISMISSED] = dismissed } }

    fun getStep2HelpDismissed(): Flow<Boolean> = dataStore.data.map { it[STEP2_HELP_DISMISSED] ?: false }
    suspend fun setStep2HelpDismissed(dismissed: Boolean) { dataStore.edit { it[STEP2_HELP_DISMISSED] = dismissed } }

    fun getDelayHelpDismissed(): Flow<Boolean> = dataStore.data.map { it[DELAY_HELP_DISMISSED] ?: false }
    suspend fun setDelayHelpDismissed(dismissed: Boolean) { dataStore.edit { it[DELAY_HELP_DISMISSED] = dismissed } }

    fun getReadyDismissed(): Flow<Boolean> = dataStore.data.map { it[READY_DISMISSED] ?: false }
    suspend fun setReadyDismissed(dismissed: Boolean) { dataStore.edit { it[READY_DISMISSED] = dismissed } }

    fun getDashboardTourStep1Dismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_STEP1_DISMISSED] ?: false }
    suspend fun setDashboardTourStep1Dismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_STEP1_DISMISSED] = dismissed } }

    fun getDashboardTourStep1CentralDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_STEP1_CENTRAL_DISMISSED] ?: false }
    suspend fun setDashboardTourStep1CentralDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_STEP1_CENTRAL_DISMISSED] = dismissed } }

    fun getSettingsTourSkinDismissed(): Flow<Boolean> = dataStore.data.map { it[SETTINGS_TOUR_SKIN_DISMISSED] ?: false }
    suspend fun setSettingsTourSkinDismissed(dismissed: Boolean) { dataStore.edit { it[SETTINGS_TOUR_SKIN_DISMISSED] = dismissed } }

    fun getAppearanceTourDismissed(): Flow<Boolean> = dataStore.data.map { it[APPEARANCE_TOUR_DISMISSED] ?: false }
    suspend fun setAppearanceTourDismissed(dismissed: Boolean) { dataStore.edit { it[APPEARANCE_TOUR_DISMISSED] = dismissed } }

    fun getDashboardTourBroadcastDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_BROADCAST_DISMISSED] ?: false }
    suspend fun setDashboardTourBroadcastDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_BROADCAST_DISMISSED] = dismissed } }

    fun getDashboardTourPlansDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_PLANS_DISMISSED] ?: false }
    suspend fun setDashboardTourPlansDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_PLANS_DISMISSED] = dismissed } }

    fun getDashboardTourIncomeDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_INCOME_DISMISSED] ?: false }
    suspend fun setDashboardTourIncomeDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_INCOME_DISMISSED] = dismissed } }

    fun getDashboardTourMetricsDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_METRICS_DISMISSED] ?: false }
    suspend fun setDashboardTourMetricsDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_METRICS_DISMISSED] = dismissed } }

    fun getDashboardTourScheduleDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_SCHEDULE_DISMISSED] ?: false }
    suspend fun setDashboardTourScheduleDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_SCHEDULE_DISMISSED] = dismissed } }

    fun getDashboardTourQrDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_QR_DISMISSED] ?: false }
    suspend fun setDashboardTourQrDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_QR_DISMISSED] = dismissed } }

    fun getDashboardTourProfessorsDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_PROFESSORS_DISMISSED] ?: false }
    suspend fun setDashboardTourProfessorsDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_PROFESSORS_DISMISSED] = dismissed } }

    fun getDashboardTourMembersNavDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_MEMBERS_NAV_DISMISSED] ?: false }
    suspend fun setDashboardTourMembersNavDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_MEMBERS_NAV_DISMISSED] = dismissed } }

    fun getDashboardTourStoreNavDismissed(): Flow<Boolean> = dataStore.data.map { it[DASHBOARD_TOUR_STORE_NAV_DISMISSED] ?: false }
    suspend fun setDashboardTourStoreNavDismissed(dismissed: Boolean) { dataStore.edit { it[DASHBOARD_TOUR_STORE_NAV_DISMISSED] = dismissed } }


    fun getBroadcastMessage(): Flow<String> = dataStore.data.map { it[GYM_BROADCAST_MESSAGE] ?: "" }
    suspend fun setBroadcastMessage(message: String) { dataStore.edit { it[GYM_BROADCAST_MESSAGE] = message } }

    fun isShortcutCreated(): Flow<Boolean> = dataStore.data.map { it[SHORTCUT_CREATED] ?: false }
    suspend fun setShortcutCreated(created: Boolean) { dataStore.edit { it[SHORTCUT_CREATED] = created } }

    fun getLastShortcutName(): Flow<String> = dataStore.data.map { it[LAST_SHORTCUT_NAME] ?: "" }
    suspend fun setLastShortcutName(name: String) { dataStore.edit { it[LAST_SHORTCUT_NAME] = name } }

    fun getLastShortcutLogoUri(): Flow<String> = dataStore.data.map { it[LAST_SHORTCUT_LOGO_URI] ?: "" }
    suspend fun setLastShortcutLogoUri(uri: String) { dataStore.edit { it[LAST_SHORTCUT_LOGO_URI] = uri } }

    fun getProfessorSchedules(): Flow<Map<String, List<ScheduleEntry>>> = dataStore.data.map { preferences ->
        val json = preferences[PROFESSOR_SCHEDULES] ?: "{}"
        try { Json.decodeFromString<Map<String, List<ScheduleEntry>>>(json) } catch (e: Exception) { emptyMap() }
    }

    suspend fun saveProfessorSchedule(professorId: String, schedules: List<ScheduleEntry>) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableMap<String, List<ScheduleEntry>>>(preferences[PROFESSOR_SCHEDULES] ?: "{}") } catch (e: Exception) { mutableMapOf() }
            current[professorId] = schedules
            preferences[PROFESSOR_SCHEDULES] = Json.encodeToString(current)
        }
    }

    suspend fun saveAllSchedules(allSchedules: Map<String, List<ScheduleEntry>>) {
        dataStore.edit { preferences ->
            preferences[PROFESSOR_SCHEDULES] = Json.encodeToString(allSchedules)
        }
    }

    fun getChatCache(chatKey: String): Flow<List<ChatMessage>> = dataStore.data.map { preferences ->
        val json = preferences[stringPreferencesKey("CHAT_CACHE_$chatKey")] ?: "[]"
        try { Json.decodeFromString<List<ChatMessage>>(json) } catch (e: Exception) { emptyList() }
    }

    suspend fun saveChatCache(chatKey: String, messages: List<ChatMessage>) {
        dataStore.edit { preferences ->
            // No cacheamos mensajes pendientes para evitar duplicados al recargar
            val persistentOnly = messages.filter { !it.isPending }
            preferences[stringPreferencesKey("CHAT_CACHE_$chatKey")] = Json.encodeToString(persistentOnly)
        }
    }

    suspend fun clearChatCache(chatKey: String) {
        dataStore.edit { it.remove(stringPreferencesKey("CHAT_CACHE_$chatKey")) }
    }

    fun getLastReadTimestamp(chatKey: String): Flow<Long> = dataStore.data.map { 
        it[longPreferencesKey("LAST_READ_$chatKey")] ?: 0L 
    }

    suspend fun setLastReadTimestamp(chatKey: String, timestamp: Long) {
        dataStore.edit { it[longPreferencesKey("LAST_READ_$chatKey")] = timestamp }
    }

    fun getSubscriptionPlans(): Flow<List<SubscriptionPlan>> = dataStore.data.map { preferences ->
        val plansJson = preferences[SUBSCRIPTION_PLANS] ?: "[]"
        try { Json.decodeFromString<List<SubscriptionPlan>>(plansJson) } catch (e: Exception) { emptyList() }
    }

    suspend fun saveSubscriptionPlans(plans: List<SubscriptionPlan>) {
        dataStore.edit { it[SUBSCRIPTION_PLANS] = Json.encodeToString(plans) }
    }

    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun getPaymentRecords(): Flow<List<PaymentRecord>> = dataStore.data.map { preferences ->
        val json = preferences[PAYMENT_RECORDS] ?: "[]"
        try { Json.decodeFromString<List<PaymentRecord>>(json) } catch (e: Exception) { emptyList() }
    }

    suspend fun addPaymentRecord(record: PaymentRecord) {
        dataStore.edit { preferences ->
            val current = try { Json.decodeFromString<MutableList<PaymentRecord>>(preferences[PAYMENT_RECORDS] ?: "[]") } catch (e: Exception) { mutableListOf() }
            current.add(record)
            preferences[PAYMENT_RECORDS] = Json.encodeToString(current)
        }
    }

    fun getAppLockExpirationTime(): Flow<Long> = dataStore.data.map { it[APP_LOCK_EXPIRATION_TIME] ?: 0L }
    suspend fun setAppLockExpirationTime(timestamp: Long) { dataStore.edit { it[APP_LOCK_EXPIRATION_TIME] = timestamp } }

    fun getGymWifiSsid(): Flow<String> = dataStore.data.map { it[GYM_WIFI_SSID] ?: "" }
    suspend fun setGymWifiSsid(ssid: String) { dataStore.edit { it[GYM_WIFI_SSID] = ssid } }

    fun getGymWifiSsid2(): Flow<String> = dataStore.data.map { it[GYM_WIFI_SSID2] ?: "" }
    suspend fun setGymWifiSsid2(ssid: String) { dataStore.edit { it[GYM_WIFI_SSID2] = ssid } }

    fun getGymWifiSsid3(): Flow<String> = dataStore.data.map { it[GYM_WIFI_SSID3] ?: "" }
    suspend fun setGymWifiSsid3(ssid: String) { dataStore.edit { it[GYM_WIFI_SSID3] = ssid } }

    fun getLastWifiCheckinDate(): Flow<String> = dataStore.data.map { it[LAST_WIFI_CHECKIN_DATE] ?: "" }
    suspend fun setLastWifiCheckinDate(date: String) { dataStore.edit { it[LAST_WIFI_CHECKIN_DATE] = date } }
}
