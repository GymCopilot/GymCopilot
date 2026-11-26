package com.zexo.gymcopilot.ui.screens

import com.zexo.gymcopilot.model.Product


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.R
import kotlinx.serialization.Serializable

enum class GymCategory(val id: String, val bgColor: Color, val defaultIconRes: Int) {
    EQUIPMENT("EQUIPMENT", Color(0xFFFF5252), R.drawable.equipment_accesory),
    CLOTHING("CLOTHING", Color(0xFF7C4DFF), R.drawable.clothing),
    NUTRITION("NUTRITION", Color(0xFFFFD700), R.drawable.suplements),
    WELLNESS("WELLNESS", Color(0xFF69F0AE), R.drawable.servicies),
    DIGITAL("DIGITAL", Color(0xFF2196F3), R.drawable.membership),
    TECH("TECH", Color(0xFF40E0D0), R.drawable.technology),
    HEALTHY_FOOD("HEALTHY_FOOD", Color(0xFFFF8A65), R.drawable.food),
    EXTRAS("EXTRAS", Color(0xFF9575CD), R.drawable.gymcopilot)
}

fun getCategoryTranslation(cat: GymCategory, lang: String): String {
    return when (lang) {
        "Español" -> when (cat) {
            GymCategory.EQUIPMENT -> "Equipamiento y accesorios"
            GymCategory.CLOTHING -> "Indumentaria deportiva"
            GymCategory.NUTRITION -> "Nutrición y suplementos"
            GymCategory.WELLNESS -> "Servicios de bienestar"
            GymCategory.DIGITAL -> "Membresías"
            GymCategory.TECH -> "Tecnología"
            GymCategory.HEALTHY_FOOD -> "Alimentación saludable"
            GymCategory.EXTRAS -> "Experiencia e extras"
        }
        else -> when (cat) {
            GymCategory.EQUIPMENT -> "Equipment & Accessories"
            GymCategory.CLOTHING -> "Sports Clothing"
            GymCategory.NUTRITION -> "Nutrition & Supplements"
            GymCategory.WELLNESS -> "Wellness Services"
            GymCategory.DIGITAL -> "Memberships"
            GymCategory.TECH -> "Technology"
            GymCategory.HEALTHY_FOOD -> "Healthy Food"
            GymCategory.EXTRAS -> "Experience & Extras"
        }
    }
}

typealias Product = com.zexo.gymcopilot.model.Product


data class CurrencyInfo(val name: String, val symbol: String, val flag: String)

object CurrencyData {
    val currencies = listOf(
        CurrencyInfo("Peso Argentino", "$", "🇦🇷"),
        CurrencyInfo("Dólar Estadounidense", "USD $", "🇺🇸"),
        CurrencyInfo("Euro", "€", "🇪🇺"),
        CurrencyInfo("Real Brasileño", "R$", "🇧🇷"),
        CurrencyInfo("Peso Mexicano", "$", "🇲🇽"),
        CurrencyInfo("Peso Chileno", "$", "🇨🇱"),
        CurrencyInfo("Peso Colombiano", "$", "🇨🇴"),
        CurrencyInfo("Sol Peruano", "S/", "🇵🇪"),
        CurrencyInfo("Peso Uruguayo", "$", "🇺🇾"),
        CurrencyInfo("Guaraní Paraguayo", "₲", "🇵🇾"),
        CurrencyInfo("Boliviano", "Bs", "🇧🇴"),
        CurrencyInfo("Peso Dominicano", "RD$", "🇩🇴"),
        CurrencyInfo("Quetzal Guatemalteco", "Q", "🇬🇹"),
        CurrencyInfo("Lempira Hondureña", "L", "🇭🇳"),
        CurrencyInfo("Córdoba Nicaragüense", "C$", "🇳🇮"),
        CurrencyInfo("Colón Costarricense", "₡", "🇨🇷"),
        CurrencyInfo("Balboa Panameño", "B/.", "🇵🇦"),
        CurrencyInfo("Bolívar Venezolano", "Bs.S", "🇻🇪"),
        CurrencyInfo("Dólar Canadiense", "CA$", "🇨🇦"),
        CurrencyInfo("Libra Esterlina", "£", "🇬🇧"),
        CurrencyInfo("Yen Japonés", "¥", "🇯🇵"),
        CurrencyInfo("Franco Suizo", "CHF", "🇨🇭"),
        CurrencyInfo("Dólar Australiano", "A$", "🇦🇺"),
        CurrencyInfo("Dólar Neozelandés", "NZ$", "🇳🇿"),
        CurrencyInfo("Yuan Chino", "¥", "🇨🇳"),
        CurrencyInfo("Won Surcoreano", "₩", "🇰🇷"),
        CurrencyInfo("Rupia India", "₹", "🇮🇳"),
        CurrencyInfo("Rublo Ruso", "₽", "🇷🇺"),
        CurrencyInfo("Rand Sudafricano", "R", "🇿🇦"),
        CurrencyInfo("Lira Turca", "₺", "🇹🇷"),
        CurrencyInfo("Dirham de Emiratos", "AED", "🇦🇪"),
        CurrencyInfo("Riyal Saudí", "SR", "🇸🇦"),
        CurrencyInfo("Shekel Israelí", "₪", "🇮🇱"),
        CurrencyInfo("Baht Tailandés", "฿", "🇹🇭"),
        CurrencyInfo("Dólar de Singapur", "S$", "🇸🇬"),
        CurrencyInfo("Ringgit Malayo", "RM", "🇲🇾"),
        CurrencyInfo("Peso Filipino", "₱", "🇵🇭"),
        CurrencyInfo("Rupia Indonesa", "Rp", "🇮🇩"),
        CurrencyInfo("Corona Sueca", "kr", "🇸🇪"),
        CurrencyInfo("Corona Noruega", "kr", "🇳🇴"),
        CurrencyInfo("Corona Danesa", "kr", "🇩🇰"),
        CurrencyInfo("Corona Islandesa", "kr", "🇮🇸"),
        CurrencyInfo("Złoty Polaco", "zł", "🇵🇱"),
        CurrencyInfo("Corona Checa", "Kč", "🇨🇿"),
        CurrencyInfo("Forinto Húngaro", "Ft", "🇭🇺"),
        CurrencyInfo("Leu Rumano", "lei", "🇷🇴"),
        CurrencyInfo("Lev Búlgaro", "лв", "🇧🇬"),
        CurrencyInfo("Grivna Ucraniana", "₴", "🇺🇦"),
        CurrencyInfo("Tenge Kazajo", "₸", "🇰🇿"),
        CurrencyInfo("Lari Georgiano", "₾", "🇬🇪"),
        CurrencyInfo("Manat Azerbaiyano", "₼", "🇦🇿"),
        CurrencyInfo("Dram Armenio", "֏", "🇦🇲"),
        CurrencyInfo("Lek Albanés", "Lek", "🇦🇱"),
        CurrencyInfo("Dinar Serbio", "дин.", "🇷🇸"),
        CurrencyInfo("Marco Convertible", "KM", "🇧🇦"),
        CurrencyInfo("Denar Macedonio", "ден", "🇲🇰"),
        CurrencyInfo("Euro (Montenegro)", "€", "🇲🇪"),
        CurrencyInfo("Dólar de Hong Kong", "HK$", "🇭🇰"),
        CurrencyInfo("Nuevo Dólar Taiwanés", "NT$", "🇹🇼"),
        CurrencyInfo("Dong Vietnamita", "₫", "🇻🇳"),
        CurrencyInfo("Rupia Pakistaní", "₨", "🇵🇰"),
        CurrencyInfo("Taka Bangladesí", "৳", "🇧🇩"),
        CurrencyInfo("Rupia de Sri Lanka", "Rs", "🇱🇰"),
        CurrencyInfo("Rupia Nepalesa", "₨", "🇳🇵"),
        CurrencyInfo("Afgani Afgano", "؋", "🇦🇫"),
        CurrencyInfo("Rial Iraní", "﷼", "🇮🇷"),
        CurrencyInfo("Dinar Iraquí", "ع.д", "🇮🇶"),
        CurrencyInfo("Dinar Jordano", "د.ا", "🇯🇴"),
        CurrencyInfo("Dinar Kuwaití", "د.ك", "🇰🇼"),
        CurrencyInfo("Dinar Bahreiní", "ب.д", "🇧🇭"),
        CurrencyInfo("Rial Qatarí", "ر.ق", "🇶🇦"),
        CurrencyInfo("Rial Omaní", "ر.ع.", "🇴🇲"),
        CurrencyInfo("Libra Libanesa", "ل.ل", "🇱🇧"),
        CurrencyInfo("Libra Siria", "£S", "🇸🇾"),
        CurrencyInfo("EGP (Libra Egipcia)", "E£", "🇪🇬"),
        CurrencyInfo("Dirham Marroquí", "د.م.", "🇲🇦"),
        CurrencyInfo("Dinar Tunecino", "د.ت", "🇹🇳"),
        CurrencyInfo("Dinar Argelino", "د.ج", "🇩🇿"),
        CurrencyInfo("Dinar Libio", "ل.д", "🇱🇾"),
        CurrencyInfo("Chelín Keniano", "KSh", "🇰🇪"),
        CurrencyInfo("NGN (Naira Nigeriana)", "₦", "🇳🇬"),
        CurrencyInfo("Cedi Ganés", "GH₵", "🇬🇭"),
        CurrencyInfo("Franco CFA (BCEAO)", "CFA", "🇸🇳"),
        CurrencyInfo("Franco CFA (BEAC)", "FCFA", "🇨🇲"),
        CurrencyInfo("Birr Etíope", "Br", "🇪🇹"),
        CurrencyInfo("Chelín Tanzano", "TSh", "🇹🇿"),
        CurrencyInfo("Chelín Ugandés", "USh", "🇺🇬"),
        CurrencyInfo("Kwacha Zambiano", "ZK", "🇿🇲"),
        CurrencyInfo("Kwanza Angoleño", "Kz", "🇦🇴"),
        CurrencyInfo("Pula de Botsuana", "P", "🇧🇼"),
        CurrencyInfo("Dólar de Namibia", "N$", "🇳🇦"),
        CurrencyInfo("Dólar Jamaiquino", "J$", "🇯🇲"),
        CurrencyInfo("Dólar de Trinidad y Tobago", "TT$", "🇹🇹"),
        CurrencyInfo("Dólar de Barbados", "Bds$", "🇧🇧"),
        CurrencyInfo("Dólar de las Bahamas", "B$", "🇧🇸"),
        CurrencyInfo("Dólar del Caribe Oriental", "EC$", "🇱🇨"),
        CurrencyInfo("Dólar Beliceño", "BZ$", "🇧🇿"),
        CurrencyInfo("Dólar Guyanés", "G$", "🇬🇾"),
        CurrencyInfo("Dólar Surinamés", "SRD", "🇸🇷"),
        CurrencyInfo("Franco Polinesio", "₣", "🇵🇫")
    )

    fun getSymbol(nameOrSymbol: String): String {
        return currencies.find { it.name == nameOrSymbol }?.symbol ?: nameOrSymbol
    }
}

@Composable
fun InfiniteCategoryBanner(
    allActiveCategoryIds: List<String>,
    appLanguage: String,
    dataStoreManager: DataStoreManager,
    selectedCategoryId: String? = null,
    onCategoryClick: (String) -> Unit
) {
    if (allActiveCategoryIds.isEmpty()) return

    val itemCount = allActiveCategoryIds.size

    if (itemCount in 1..4) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            allActiveCategoryIds.forEach { categoryId ->
                CategoryBannerItem(
                    categoryId = categoryId,
                    appLanguage = appLanguage,
                    dataStoreManager = dataStoreManager,
                    isSelected = categoryId == selectedCategoryId,
                    modifier = Modifier.weight(1f),
                    onCategoryClick = onCategoryClick
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            items(allActiveCategoryIds) { categoryId ->
                CategoryBannerItem(
                    categoryId = categoryId,
                    appLanguage = appLanguage,
                    dataStoreManager = dataStoreManager,
                    isSelected = categoryId == selectedCategoryId,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
fun CategoryBannerItem(
    categoryId: String,
    appLanguage: String,
    dataStoreManager: DataStoreManager,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onCategoryClick: (String) -> Unit
) {
    val customName by dataStoreManager.getCategoryName(categoryId).collectAsState(initial = "")
    val fixedCat = GymCategory.entries.find { it.id == categoryId }

    val displayName = customName?.takeIf { it.isNotEmpty() }
        ?: fixedCat?.let { getCategoryTranslation(it, appLanguage) }
        ?: "Extra"

    val categorySavedColor by dataStoreManager.getCategoryColor(categoryId).collectAsState(initial = null)
    val bgColor = if (categorySavedColor != null) Color(categorySavedColor!!)
    else fixedCat?.bgColor ?: Color(0xFF9575CD)

    val contentColor = Color.Black

    Surface(
        color = bgColor,
        shape = RectangleShape,
        modifier = modifier
            .fillMaxHeight()
            .clickable { onCategoryClick(categoryId) }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.uppercase(),
                color = if (isSelected) contentColor else contentColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ScrollingAnnouncementBanner(
    text: String,
    backgroundColor: Color = Color.Black,
    textColor: Color = Color.White
) {
    if (text.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val density = LocalDensity.current
    var containerWidth by remember { mutableStateOf(0) }
    var textWidth by remember { mutableStateOf(0) }

    val duration = (text.length * 300).coerceAtLeast(12000)

    val offset by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(backgroundColor)
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        val xPos = (offset * (textWidth + containerWidth)) / 2 + (containerWidth / 2) - (textWidth / 2)

        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(x = with(density) { xPos.toDp() })
                .onSizeChanged { textWidth = it.width },
            maxLines = 1,
            softWrap = false
        )
    }
}

object BannerTranslations {
    private val phrasesMap = mapOf(
        "FEW_AVAILABLE" to mapOf("English" to "Few available!", "Español" to "¡Pocos disponibles!", "Português" to "Poucos!", "Italiano" to "Pochi!", "Français" to "Peu!", "Deutsch" to "Nur noch wenige!", "Русский" to "Мало!", "日本語" to "残りわずか！", "中文" to "库存紧张！", "العربية" to "قليل جداً!", "हिन्दी" to "کم بچے हैं!", "বাংলা" to "অল্প আছে!", "اردو" to "کم دستیاب!", "Bahasa Indonesia" to "Sedikit!"),
        "LIMITED_EDITION" to mapOf("English" to "Limited edition", "Español" to "Edición limitada", "Português" to "Edição limitada", "Italiano" to "Edizione limitata", "Français" to "Édition limitada", "Deutsch" to "Limitierte Auflage", "Русский" to "Лимиtiрованная serie", "日本語" to "限定版", "中文" to "限量版", "العربية" to "إصدار limitado", "हिन्दी" to "सीमित संस्करण", "বাংলা" to "সীমিত সংস্করণ", "اردو" to "محدود ایڈیشن", "Bahasa Indonesia" to "Edisi terbatas"),
        "EXCLUSIVE_TODAY" to mapOf("English" to "Exclusive today", "Español" to "Exclusivo hoy", "Português" to "Exclusivo hoje", "Italiano" to "Esclusivo oggi", "Français" to "Exclusif aujourd'hui", "Deutsch" to "Heute exklusiv", "Русский" to "Эксклюзивно hoy", "日本語" to "本日限定", "中文" to "今日独家", "العربية" to "حصري اليوم", "हिन्दी" to "आज खास", "বাংলা" to "আজকের স্পেশal", "اردو" to "آج خصوصی", "Bahasa Indonesia" to "Eksclusif hari ini"),
        "LIMITED_UNITS" to mapOf("English" to "Limited units", "Español" to "Unidades contadas", "Português" to "Unidades limitadas", "Italiano" to "Unità limitate", "Français" to "Unités limitées", "Deutsch" to "Begrenzte Stückzahl", "Русский" to "Ограничено", "日本語" to "数量限定", "中文" to "限量供应", "العربية" to "وحدات محدودة", "हिन्दी" to "सीमित इकाइयाँ", "বাংলা" to "সীमित UNIT", "اردu" to "محدود یونٹ", "Bahasa Indonesia" to "Unit terbatas"),
        "ENDS_SOON" to mapOf("English" to "Ends soon", "Español" to "Finaliza pronto", "Português" to "Termina em breve", "Italiano" to "Finisce presto", "Français" to "Finit bientôt", "Deutsch" to "Endet bald", "Русский" to "Скоoro закончится", "日本語" to "まもなく終了", "中文" to "即将结束", "العربية" to "ينتهي قريباً", "हिन्दी" to "जल्द समाप्त", "বাংলা" to "শীঘ্রই শেষ হবে", "اردو" to "جلد ختم", "Bahasa Indonesia" to "Segera berakhir"),
        "FLASH_OFFER" to mapOf("English" to "Flash offer", "Español" to "Oferta relámpago", "Português" to "Oferta relâmpago", "Italiano" to "Offerta lampo", "Français" to "Offre éclair", "Deutsch" to "Blitzangebot", "Русский" to "Молниеносно", "日本語" to "タイムセール", "中文" to "闪电特惠", "العربية" to "عرض خاطf", "हिन्दी" to "फ्लैश सेल", "বাংলা" to "ফ্ল্যাশ অফার", "اردu" to "فلیش آفر", "Bahasa Indonesia" to "Penawaran kilat"),
        "LIMITED_TIME" to mapOf("English" to "Limited time", "Español" to "Tiempo limitado", "Português" to "Tiempo limitado", "Italiano" to "Tiempo limitato", "Français" to "Temps limitado", "Deutsch" to "Begrenzte Zeit", "Русский" to "Ограничено", "日本語" to "期間限定", "中文" to "限时优惠", "العربية" to "وقت limitado", "हिन्दी" to "सीमित समय", "বাংলা" to "সীমিত সময়", "اردو" to "محدود وقت", "Bahasa Indonesia" to "Waktu terbatas"),
        "LIMITED_STOCK" to mapOf("English" to "Limited stock", "Español" to "Stock limitado", "Português" to "Stock limitado", "Italiano" to "Stock limitato", "Français" to "Stock limité", "Deutsch" to "Vorrat limitado", "Русский" to "Запас limitado", "日本語" to "在庫限り", "中文" to "库存有限", "العربية" to "مخزون limitado", "हिन्दी" to "सीमित स्टॉक", "বাংলা" to "সীমিত STOCK", "اردو" to "محدود اسٹاک", "Bahasa Indonesia" to "Stok terbatas"),
        "BUY_1_GET_1" to mapOf("English" to "Buy 1 Get 1", "Español" to "2x1", "Português" to "2x1", "Italiano" to "2x1", "Français" to "2x1", "Deutsch" to "2 für 1", "Русский" to "2 por цене 1", "日本語" to "1つ gratuito", "中文" to "买一送一", "العربية" to "2 بسعر 1", "हिन्दी" to "1+1 फ्री", "বাংলা" to "১+১ ফ্রি", "اردو" to "1+1 मुफ्त", "Bahasa Indonesia" to "Beli 1 gratis 1"),
        "BUY_3_PAY_2" to mapOf("English" to "3 for 2", "Español" to "2x3", "Português" to "2x3", "Italiano" to "3x2", "Français" to "3x2", "Deutsch" to "3 für 2", "Русский" to "3 por цене 2", "日本語" to "3個で2個分", "中文" to "买三付二", "العربية" to "3 بسعر 2", "हिन्दी" to "3 के दाम 2", "বাংলা" to "৩টির দামে ২", "اردo" to "3 کی قیمت 2", "Bahasa Indonesia" to "Beli 3 bayar 2"),
        "DISCOUNT" to mapOf("English" to "Discount", "Español" to "Descuento", "Português" to "Desconto", "Italiano" to "Sconto", "Français" to "Remise", "Deutsch" to "Rabatt", "Русский" to "Скидка", "日本語" to "割引", "中文" to "折扣", "العربية" to "خصم", "हिन्दी" to "छूट", "বাংলা" to "ছাড়", "اردu" to "رعایت", "Bahasa Indonesia" to "Diskon"),
        "OFFER" to mapOf("English" to "Offer", "Español" to "Oferta", "Português" to "Oferta", "Italiano" to "Offerta", "Français" to "Offre", "Deutsch" to "Angebot", "Русский" to "Предложение", "日本語" to "オファー", "中文" to "优惠", "العربية" to "عرض", "हिन्दी" to "ऑफर", "বাংলা" to "অফার", "اردo" to "آفر", "Bahasa Indonesia" to "Penawaran")
    )

    fun getPhrasesForLanguage(lang: String): List<String> {
        return phrasesMap.values.map { it[lang] ?: it["English"] ?: "" }
    }

    fun translate(text: String?, targetLang: String): String? {
        if (text == null) return null
        val key = phrasesMap.entries.find { entry ->
            entry.value.values.any { it.equals(text, ignoreCase = true) }
        }?.key ?: return text
        return phrasesMap[key]?.get(targetLang) ?: phrasesMap[key]?.get("English") ?: text
    }
}
