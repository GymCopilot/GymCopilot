package com.zexo.gymcopilot.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.zexo.gymcopilot.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val fontFamilyCache = mutableMapOf<String, FontFamily>()

fun getGoogleFontFamily(fontName: String): FontFamily {
    val font = GoogleFont(fontName)
    return FontFamily(
        Font(googleFont = font, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = font, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = font, fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = font, fontProvider = provider, weight = FontWeight.Bold),
        Font(googleFont = font, fontProvider = provider, weight = FontWeight.ExtraBold),
        Font(googleFont = font, fontProvider = provider, weight = FontWeight.Black)
    )
}

val gymFonts = listOf(
    "Default", "Serif", "Monospace", "Cursive", "SansSerif",
    "Roboto", "Montserrat", "Oswald", "Bebas Neue", "Open Sans", "Lato", "Raleway",
    "Poppins", "Inter", "Ubuntu", "Playfair Display", "Merriweather", "Lora", "PT Serif",
    "Lobster", "Pacifico", "Dancing Script", "Caveat", "Satisfy", "Courgette",
    "Permanent Marker", "Rock Salt", "Bangers", "Luckiest Guy", "Press Start 2P",
    "Anton", "Righteous", "Archivo Black", "Teko", "Fira Sans", "Work Sans",
    "Quicksand", "Josefin Sans", "Abril Fatface", "Cinzel", "Orbitron", "Exo 2",
    "Michroma", "Audiowide", "Syncopate", "Comfortaa", "Fredoka One", "Alfa Slab One",
    "Patua One", "Sigmar One", "Titan One", "Special Elite", "UnifrakturMaguntia",
    "Creepster", "Monoton", "Faster One", "Shadows Into Light", "Indie Flower",
    "Amatic SC", "Sacramento", "Cookie", "Great Vibes", "Kaushan Script", "Russo One",
    "Staatliches", "Kanit", "Tangerine", "Yellowtail", "Bungee", "Black Ops One"
)

fun getGymFontFamily(fontName: String): FontFamily {
    return when (fontName) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        "Default" -> FontFamily.Default
        else -> try {
            getGoogleFontFamily(fontName)
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
}
