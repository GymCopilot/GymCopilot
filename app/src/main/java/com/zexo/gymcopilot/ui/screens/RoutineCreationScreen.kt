package com.zexo.gymcopilot.ui.screens

import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.gymcopilot.DataStoreManager
import com.zexo.gymcopilot.Exercise
import com.zexo.gymcopilot.Professor
import com.zexo.gymcopilot.R
import com.zexo.gymcopilot.Routine
import com.zexo.gymcopilot.network.NetworkModule
import com.zexo.gymcopilot.repository.AttendanceRepository
import com.zexo.gymcopilot.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs


data class ExerciseEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var series: String,
    var reps: String,
    var weight: String,
    var rest: String,
    var category: String = "PECHO",
    var time: String = "0",
    var notes: String = "",
    var isLocked: Boolean = false // Nuevo campo para el estado de bloqueo
)

val PREDEFINED_EXERCISES = mapOf(
    "PECHO" to listOf(
        "Press banca horizontal", "Press banco inclinado barra", "Press declinado barra",
        "Press plano mancuerna", "Press inclinado mancuerna", "Aperturas mancuerna",
        "Press de pecho máquina", "Press inclinado máquina", "Press convergente", "Peck Deck máquina", "Chest Fly Machine",
        "Cruce de poleas", "Cruce de poleas alto", "Cruce de poleas bajo",
        "Flexiones clásicas", "Flexiones inclinadas", "Flexiones diamante", "Floor Press", "Pullover",
        "Pase de pecho con balón", "Wall Balls", "Lanzamiento contra pared"
    ),
    "ESPALDA" to listOf(
        "Remo con barra", "Remo Pendlay barra", "Peso muerto barra", "Jalón con brazos rectos",
        "Remo unilateral mancuerna", "Remo pecho apoyado mancuerna",
        "Jalón frontal polea", "Jalón tras nuca", "Jalón unilateral", "Jalón agarre estrecho", "Jalón supino", "Pullover en polea", "Face Pull polea",
        "Pullover machine", "Remo alto máquina", "Encogimientos barra", "Remo al mentón",
        "Dominadas", "Band Pull-apart", "Trepa de cuerda", "Farmer Walk", "Rodillas al pecho",
        "Swing Ruso", "Swing Americano", "Lanzamiento al piso", "Movilidad torácica", "Estiramientos", "Estiramiento de cuello"
    ),
    "HOMBROS" to listOf(
        "Press militar barra", "Push Press barra",
        "Press militar mancuerna", "Press sentado mancuerna", "Elevaciones frontales mancuerna", "Elevaciones laterales mancuerna", "Pájaros mancuerna",
        "Press de hombros máquina", "Elevación lateral máquina", "Reverse Peck Deck",
        "Elevaciones laterales polea", "Band Pull-apart",
        "Kettlebell Halo", "Kettlebell Windmill", "Wall Balls", "Handstand (Pino)", "Lanzamiento contra pared", "Rotación de hombros"
    ),
    "BÍCEPS" to listOf(
        "Curl barra recta", "Curl EZ barra",
        "Curl alternado mancuerna", "Curl sentado mancuerna", "Curl martillo mancuerna", "Curl concentrado", "Curl inclinado mancuerna",
        "Curl de bícéps máquina", "Curl biceps máquina",
        "Curl unilateral polea", "Curl en polea",
        "Trepa de cuerda"
    ),
    "TRÍCEPS" to listOf(
        "Press francés barra", "Press cerrado barra",
        "Extensión sobre cabeza mancuerna", "Extensión sentado mancuerna", "Patada de tríceps mancuerna",
        "Extensión de tríceps máquina", "Triceps de polea", "Pushdown barra polea", "Pushdown cuerda polea", "Tríceps cuerda", "Extensión unilateral polea",
        "Fondos", "Handstand (Pino)", "Lanzamiento sobre cabeza", "Pase de pecho con balón", "Wall Balls", "Lanzamiento contra pared"
    ),
    "ANTEBRAZOS" to listOf(
        "Curl de muñeca barra", "Curl inverso barra",
        "Wrist Curl mancuerna", "Reverse Wrist Curl",
        "Farmer Walk", "Hand Grippers", "Trepa de cuerda", "Movilidad de muñeca"
    ),
    "ABDOMINALES" to listOf(
        "Crunch en máquina", "Abdominal asistido máquina",
        "Crunch suelo", "Abdominal corto en el suelo", "Elevación de piernas", "Plancha", "Hollow Hold", "Estabilidad de Core",
        "Toes to Bar", "Crunch en polea", "Woodchopper polea", "Pallof Press polea", "Russian Twist",
        "Hundred", "Teaser", "Roll Up", "Swimming", "Side Kick", "Single Leg Stretch",
        "Lanzamiento al piso", "Lanzamiento sobre cabeza"
    ),
    "OBLICUOS" to listOf(
        "Woodchopper polea", "Rotaciones con polea", "Plancha lateral",
        "Oblicuos mancuerna", "Russian Twist", "Kettlebell Windmill", "Side Kick"
    ),
    "GLÚTEOS" to listOf(
        "Hip Thrust barra", "Sentadilla barra", "Peso muerto rumano barra",
        "Zancadas mancuerna", "Peso muerto rumano mancuerna",
        "Hip Thrust Machine", "Abductores máquina",
        "Patada de glúteo polea", "Side Kick",
        "Sentadilla con balón", "Sentadilla con banda", "Elevación del glúteo", "Swing Ruso", "Swing Americano", "Saltos al cajón", "Sprint", "Estiramiento de flexores"
    ),
    "CUÁDRICEPS" to listOf(
        "Sentadilla barra", "Sentadilla frontal barra",
        "Prensa de piernas", "Hack Squat", "Extensión de cuádriceps máquina", "Belt Squat",
        "Goblet Squat mancuerna", "Step Up mancuerna",
        "Pistol Squat", "Saltos al cajón", "Sprint", "Carrera en cuestas", "Wall Balls", "Sentadilla con balón", "Sentadilla con banda", "Lanzamiento contra pared", "Estiramiento de flexores"
    ),
    "ISQUIOTIBIALES" to listOf(
        "Peso muerto rumano barra", "Peso muerto piernas rígidas", "Buenos días barra",
        "Peso muerto rumano mancuerna",
        "Curl femoral acostado máquina", "Nordic Curl",
        "Swing Ruso", "Swing Americano", "Sprint", "Carrera en cuestas"
    ),
    "ADUCTORES" to listOf(
        "Aductores en máquina", "Aducción de cadera polea", "Copenhagen Plank"
    ),
    "ABDUCTORES" to listOf(
        "Abductores en máquina", "Abducción de cadera polea", "Monster Walk con banda", "Caminata lateral banda", "Sentadilla con banda"
    ),
    "GEMELOS" to listOf(
        "Elevación de talones barra", "Elevación unilateral mancuerna",
        "Gemelos de pie máquina", "Gemelos sentado máquina", "Gemelos en prensa",
        "Calf Raises peso corporal", "Saltos al cajón", "Trote", "Sprint", "Carrera en cuestas", "Skipping", "Saltos dobles (Soga)", "Movilidad de tobillo"
    ),
    "EJERCICIOS COMBINADOS" to listOf(
        "Clean barra", "Snatch barra", "Thruster barra", "Peso muerto barra",
        "Correr en cinta", "Caminar en cinta", "Remo ergómetro", "Bicicleta fija", "Elíptico",
        "Clean Kettlebell", "Snatch Kettlebell", "Swing Kettlebell", "Turkish Get Up Kettlebell", "Kettlebell Halo", "Kettlebell Windmill",
        "Devil Press mancuerna", "Caminata con carga",
        "Mountain Climbers", "Gateo de oso", "Saltos al cajón", "Trote", "Sprint", "Carrera en cuestas", "Skipping", "Wall Balls", "Trepa de cuerda", "Saltos dobles (Soga)", "Lanzamiento sobre cabeza", "Lanzamiento al piso", "Lanzamiento contra pared", "Handstand (Pino)", "Liberación miofascial"
    )
)

fun getExerciseIcon(exerciseName: String): Int {
    var name = exerciseName.lowercase().trim()

    // Derivación de Icono Inteligente
    if (name.contains("(") && name.endsWith(")")) {
        val extracted = name.substringAfterLast("(").substringBeforeLast(")")
        if (extracted.isNotBlank()) {
            name = extracted.trim()
        }
    }

    return when {
        // Categorías
        name == "pecho" -> R.drawable.g_categoria_pecho
        name == "espalda" || name == "dorsales" -> R.drawable.g_categoria_espalda
        name == "hombros" -> R.drawable.g_categoria_hombros
        name == "bíceps" -> R.drawable.g_categoria_biceps
        name == "tríceps" -> R.drawable.g_categoria_triceps
        name == "antebrazos" -> R.drawable.g_categoria_antebrazo
        name == "abdominales" -> R.drawable.g_categoria_abdominales
        name == "oblicuos" -> R.drawable.g_categoria_oblicuos
        name == "glúteos" -> R.drawable.g_categoria_gluteos
        name == "cuádriceps" -> R.drawable.g_categoria_cuadriceps
        name == "isquiotibiales" -> R.drawable.g_categoria_isquiotibiales
        name == "aductores" -> R.drawable.g_categoria_aductores
        name == "abductores" -> R.drawable.g_categoria_abductores
        name == "gemelos" -> R.drawable.g_categoria_gemelos
        name == "ejercicios combinados" || name == "cuerpo completo" || name == "fullbody" -> R.drawable.g_categoria_cuerpocompleto

        // PECHO
        name.contains("chest fly") || name.contains("machine") -> R.drawable.g_peckdeck
        name.contains("peck deck") || name.contains("machine") -> R.drawable.g_peckdeckmaquina
        name.contains("horizontal") && name.contains("press") -> R.drawable.g_pressbancapechohorizontal
        name.contains("press plano mancuerna") -> R.drawable.g_pressbancohorizontalmancuerna
        name.contains("press inclinado mancuerna") -> R.drawable.g_pressinclinado
        name.contains("press banco inclinado barra") -> R.drawable.g_pressbancapecho
        name.contains("press banca plano") || name.contains("press plano barra") -> R.drawable.g_pressbancapecho
        name.contains("press inclinado máquina") -> R.drawable.g_presspechoinclinado
        name.contains("press inclinado") -> R.drawable.g_presspechoinclinado
        name.contains("press declinado") -> R.drawable.g_pressdeclinadobarra
        name.contains("press plano") -> R.drawable.g_pressplano
        name.contains("aperturas") -> R.drawable.g_aperturas
        name.contains("press convergente") -> R.drawable.g_pressconvergente
        name.contains("press") && name.contains("máquina") && name.contains("pecho") -> R.drawable.g_presspechomaquina
        name.contains("press") && name.contains("pecho") && name.contains("polea") -> R.drawable.g_crucedepoleas
        name.contains("cruce ") && name.contains("poleas") && name.contains("bajo") -> R.drawable.g_presspechoconpolea
        name.contains("cruce") && name.contains("poleas") && name.contains("alto")-> R.drawable.g_crucedepoleasalto
        name.contains("cruce") && name.contains("poleas") -> R.drawable.g_crucedepoleas
        name.contains("floor press") -> R.drawable.g_floorpress
        name.contains("flexiones") && name.contains("inclinadas") -> R.drawable.g_flexionesinclinadas
        name.contains("flexiones") && name.contains("diamante") -> R.drawable.g_flexionesdiamente
        name.contains("flexiones") -> R.drawable.g_flexionesclasicas
        name.contains("pullover") && name.contains("máquina") -> R.drawable.g_pullovermachine
        name.contains("pullover") && name.contains("polea") -> R.drawable.g_pulloverpolea
        name.contains("pullover") -> R.drawable.g_pulloverbanco
        name.contains("pase") && name.contains("pecho") && name.contains("balón") -> R.drawable.g_pasedepechobola

        // ESPALDA
        name.contains("jalón frontal") -> R.drawable.g_jalonfrontal
        name.contains("rodillas al pecho") -> R.drawable.g_leggstretch
        name.contains("jalón al pecho") -> R.drawable.g_jalonpecho
        name.contains("jalón tras nuca") -> R.drawable.g_jalontrasnuca
        name.contains("jalón unilateral") -> R.drawable.g_jalonunilateral
        name.contains("jalón agarre estrecho") -> R.drawable.g_jalonagarrestrecho
        name.contains("jalón supino") -> R.drawable.g_jalonsupino
        name.contains("dominada") || name.contains("chin ups") || name.contains("muscle ups") -> R.drawable.g_dominada
        name.contains("remo con barra") -> R.drawable.g_remoconbarra
        name.contains("remo pendlay") -> R.drawable.g_remopendlay
        name.contains("remo unilateral") -> R.drawable.g_remounilateral
        name.contains("remo") && name.contains("alto") && name.contains("máquina") -> R.drawable.g_maquinaremo
        name.contains("remo") -> R.drawable.g_remo
        name.contains("encogimiento") -> R.drawable.g_encogimientohombros
        name.contains("face pull") -> R.drawable.g_facepulls
        name.contains("muerto") && name.contains("barra") -> R.drawable.g_pasomuertobarra
        name.contains("jalón brazos rectos") -> R.drawable.g_pasomuertobarra
        name.contains("trepa") && name.contains("cuerda") -> R.drawable.g_ropeclimbs
        name.contains("pull-apart") -> R.drawable.g_pullaparts
        name.contains("movilidad torácica") -> R.drawable.g_movilidadtoraxica
        name.contains("cuerdas") -> R.drawable.g_movimientodecuerdas
        name.contains("estiramiento de cuello") -> R.drawable.g_neckstretching


        // HOMBROS
        name.contains("militar") && name.contains("barra") -> R.drawable.g_pressmilitarbarra
        name.contains("militar") && name.contains("mancuerna") -> R.drawable.g_pressmilitarmancuerna
        name.contains("militar") -> R.drawable.g_pressmilitar
        name.contains("press sentado mancuerna") -> R.drawable.g_presssentadomancuerna
        name.contains("press") && name.contains("hombros") && name.contains("máquina") -> R.drawable.g_presshombrosmaquina
        name.contains("elevaciones") && (name.contains("laterales") || name.contains("polea")) -> R.drawable.g_elevacionlateraldepoleas
        name.contains("elevaciones laterales") -> R.drawable.g_elevacioneslaterales
        name.contains("elevaciones frontales") -> R.drawable.g_elevacionesfrontales
        name.contains("push press") -> R.drawable.g_pushpress
        name.contains("pájaro") || name.contains("reverse fly") || name.contains("reverse peck deck") -> R.drawable.g_pajaro
        name.contains("halo") -> R.drawable.g_halo
        name.contains("handstand") || name.contains("pino") -> R.drawable.g_handstands
        name.contains("windmill") -> R.drawable.g_windmill
        name.contains("rotación de hombros") -> R.drawable.g_rotaciondehombros

        // BÍCEPS
        name.contains("barra recta") && name.contains("curl") -> R.drawable.g_curlbicepsbarrarecta
        name.contains("ez") && name.contains("curl") -> R.drawable.g_ezcurlbarra
        name.contains("curl") && name.contains("sentado") && name.contains("mancuerna") -> R.drawable.g_curlmancuernasentado
        name.contains("curl") && name.contains("sentado") -> R.drawable.g_curlsentado
        name.contains("curl alternado") -> R.drawable.g_curlalternado
        name.contains("martillo") -> R.drawable.g_curlmartillomancuerna
        name.contains("concentrado") -> R.drawable.g_curlconcentrado
        name.contains("inclinado") && name.contains("curl") -> R.drawable.g_curlmancuernainclinado
        name.contains("curl biceps máquina") -> R.drawable.g_curlbicepsmaquina
        name.contains("curl") && name.contains("unilateral") && name.contains("polea") -> R.drawable.g_curlunilateralpoleas
        name.contains("curl") && (name.contains("poleas") || name.contains("polea")) -> R.drawable.g_curldepoleas
        name.contains("inverso") && name.contains("barra") -> R.drawable.g_curlinversobarra
        name.contains("curl") && (name.contains("biceps") || name.contains("bíceps")) -> R.drawable.g_curlbiceps

        // TRÍCEPS
        name.contains("extensión") && name.contains("tríceps") && name.contains("máquina") -> R.drawable.g_extensiontricepsmaquina
        name.contains("extensión") && name.contains("tríceps") && name.contains("sentado") -> R.drawable.g_extensiontricepssentado
        name.contains("pushdown") && name.contains("barra") -> R.drawable.g_pushdownbarrapolea
        name.contains("pushdown") && name.contains("cuerda") -> R.drawable.g_pushdowncuerda
        name.contains("tríceps") && name.contains("cuerda") -> R.drawable.g_tricepsdecuerdapolea
        name.contains("tríceps") && name.contains("polea") -> R.drawable.g_tricepsdepolea
        name.contains("sobre cabeza") && name.contains("mancuerna") -> R.drawable.g_extensiontriceps
        name.contains("press francés") -> R.drawable.g_pressfrancesbarra
        name.contains("extensión") && name.contains("unilateral") && name.contains("polea") -> R.drawable.g_extensionunilateraldepoleatriceps
        name.contains("extensión") && name.contains("sentado") && name.contains("mancuerna") -> R.drawable.g_extensionsentadomancuerna
        name.contains("press cerrado") -> R.drawable.g_presscerradobarra
        name.contains("Triceps de polea") -> R.drawable.g_tricedepoleas
        name.contains("patada") && name.contains("tríceps") -> R.drawable.g_patadatriceps
        name.contains("fondos") -> R.drawable.g_fondo

        // ANTEBRAZOS
        name.contains("muñeca") && name.contains("barra") -> R.drawable.g_curldemunecabarra
        name.contains("wrist curl") && name.contains("mancuerna") -> R.drawable.g_wirstcurlmncuerna
        name.contains("reverse wrist curl") -> R.drawable.g_reversewirstcurl
        name.contains("hand grippers") -> R.drawable.g_handgripper

        // ABDOMINALES
        name.contains("crunch") && name.contains("máquina") -> R.drawable.g_crunchenmaquina
        name.contains("abdominal corto") && name.contains("suelo") -> R.drawable.g_hundred
        name.contains("abdominal") && name.contains("asistido") -> R.drawable.g_abdominalasistidomaquina
        name.contains("crunch") && name.contains("suelo") -> R.drawable.g_crunchsuelo
        name.contains("elevación de piernas") || name.contains("abdominales bajos") -> R.drawable.g_abdominalesbajos
        name.contains("pallof press") && name.contains("arriba") -> R.drawable.g_pallofpressarriba
        name.contains("pallof press") -> R.drawable.g_pallofpress
        name.contains("woodchopper") -> R.drawable.g_woodchopper
        name.contains("russian twist") -> R.drawable.g_russiantwist
        name.contains("toes to bar") -> R.drawable.g_toestobar
        name.contains("hundred") -> R.drawable.g_hundredpilates
        name.contains("teaser") -> R.drawable.g_teaser
        name.contains("roll up") -> R.drawable.g_rolluppilates
        name.contains("swimming") -> R.drawable.g_swimming
        name.contains("crunch") && name.contains("polea") -> R.drawable.g_crunchdepolea
        name.contains("hollow hold") -> R.drawable.g_hollowhold
        name.contains("plancha") && name.contains("lateral") -> R.drawable.g_planchalateral
        name.contains("plancha") && name.contains("extendidos") -> R.drawable.g_planchabrazosextendidos
        name.contains("plancha") && name.contains("unilateral") -> R.drawable.g_planchaunilateralmancuerna
        name.contains("plancha") -> R.drawable.g_planchabrazosextendidos
        name.contains("core") && name.contains("estabilidad") -> R.drawable.g_coreestability
        name.contains("single leg stretch") -> R.drawable.g_singleleggstretch
        name.contains("oblicuos") && name.contains("mancuerna") -> R.drawable.g_oblicuosmancuerna
        name.contains("rotaciones") && name.contains("polea") -> R.drawable.g_rotacionesconpolea
        name.contains("flexiones laterales") -> R.drawable.g_oblicuosmancuerna

        // PIERNAS
        name.contains("prensa") -> R.drawable.g_prensadepiernas
        name.contains("hack squat") -> R.drawable.g_hacksquart
        name.contains("extensión") && name.contains("cuádriceps") -> R.drawable.g_extensioncuadriceps
        name.contains("sentadilla frontal") -> R.drawable.g_sentadillafrontal
        name.contains("goblet squat") -> R.drawable.g_goblesquat
        name.contains("pistol squat") -> R.drawable.g_pistolsquat
        name.contains("sentadilla") && name.contains("balón") -> R.drawable.g_sentadillasconbalon
        name.contains("sentadilla") && name.contains("banda") -> R.drawable.g_sentadillasconbanda
        name.contains("sentadilla") && name.contains("barra") -> R.drawable.g_sentadillabarra
        name.contains("sentadilla") -> R.drawable.g_sentadillas
        name.contains("belt squat") -> R.drawable.g_beltsquat
        name.contains("hip thrust") && name.contains("barra")-> R.drawable.g_hipthrust
        name.contains("hip thrust") && name.contains("machine")-> R.drawable.g_hipthrustmachine
        name.contains("muerto") && name.contains("piernas rígidas") -> R.drawable.g_pesomuertopiernasrigidas
        name.contains("muerto rumano") && name.contains("mancuerna") -> R.drawable.g_pasomuertorumanomancuerna
        name.contains("femorales") || name.contains("curl femoral") -> R.drawable.g_femorales
        name.contains("nordic curl") -> R.drawable.g_nordiccurl
        (name.contains("kickback") || name.contains("patada")) && name.contains("glúteo") -> R.drawable.g_patadagluteopolea
        name.contains("aductores") && name.contains("máquina") -> R.drawable.g_aductoresmaquina
        name.contains("abductores") && name.contains("máquina") -> R.drawable.g_abductoresmaquina
        name.contains("aducción") && name.contains("cadera") -> R.drawable.g_aduccionabduccioncadera
        name.contains("abducción") && name.contains("cadera") -> R.drawable.g_aduccionabduccioncadera
        name.contains("copenhagen plank") -> R.drawable.g_copenaghenplank
        name.contains("monster walk") -> R.drawable.g_monsterwalk
        name.contains("caminata lateral") && name.contains("banda") -> R.drawable.g_caminatalateralconbanda
        name.contains("caminata lateral") -> R.drawable.g_caminatalateral
        name.contains("zancada") -> R.drawable.g_zancada
        name.contains("buenos días") -> R.drawable.g_buenosdias
        name.contains("gemelos") && name.contains("sentado") && name.contains("máquina") -> R.drawable.g_gemelossentadomaquina
        name.contains("gemelos") && name.contains("pie") -> R.drawable.g_gemelosdepie
        name.contains("gemelos") && name.contains("sentado") -> R.drawable.g_gemelossentado
        name.contains("elevación unilateral mancuerna") -> R.drawable.g_calfraisespesocorporalunilateral
        name.contains("elevación de talones") -> R.drawable.g_elevaciontalones
        name.contains("calf raises") -> R.drawable.g_calfraisespesocorporal
        name.contains("step up") -> R.drawable.g_stepup
        name.contains("elevación del glúteo") -> R.drawable.g_elevaciondelgluteo
        name.contains("side kick") -> R.drawable.g_sidekick
        name.contains("patada de glúteo") && name.contains("polea") -> R.drawable.g_patadagluteopolea


        // COMBINADOS / OTROS
        name.contains("swing") && name.contains("ruso") -> R.drawable.g_swingruso
        name.contains("bicicleta fija")  -> R.drawable.g_bicicletafija
        name.contains("swing") && name.contains("americano") -> R.drawable.g_swingamericano
        name.contains("swing") -> R.drawable.g_swing
        name.contains("wall ball") -> R.drawable.g_wallballs
        name.contains("correr en cinta") -> R.drawable.g_cintacorrer
        name.contains("caminar en cinta") -> R.drawable.g_cintacaminar

        name.contains("clean") -> R.drawable.g_clean
        name.contains("clean") && name.contains("barra") -> R.drawable.g_pushpress
        name.contains("snatch") && name.contains("barra")-> R.drawable.g_sentadillafrontal
        name.contains("thruster") -> R.drawable.g_thrusters
        name.contains("devil press") -> R.drawable.g_devilpressmancuerna

        name.contains("turkish") -> R.drawable.g_turkishgetup
        name.contains("farmer") -> R.drawable.g_farmerwalk
        name.contains("double unders") -> R.drawable.g_doubleunders
        name.contains("soga") -> R.drawable.g_soga
        name.contains("lanzamiento") && name.contains("piso") -> R.drawable.g_lanzamientoalpiso
        name.contains("lanzamiento") && name.contains("pared") -> R.drawable.g_lanzamientocontralapered
        name.contains("overhead throw") || (name.contains("lanzamiento") && name.contains("cabeza")) -> R.drawable.g_overheadthrow
        name.contains("salto") && name.contains("cajón") -> R.drawable.g_boxjump
        name.contains("salto") -> R.drawable.g_salto

        // Cardio / Carrera
        name.contains("sprint") -> R.drawable.g_sprint
        name.contains("trote") -> R.drawable.g_trote
        name.contains("cinta") && name.contains("correr") -> R.drawable.g_cintadecorrer
        name.contains("cinta") && name.contains("caminar") -> R.drawable.g_cintacaminar
        name.contains("cinta") -> R.drawable.g_cintadecorrer
        name.contains("correr") -> R.drawable.g_sprint

        name.contains("cuestas") -> R.drawable.g_cuestas
        name.contains("skipping") -> R.drawable.g_skipping
        name.contains("mountain climbers") -> R.drawable.g_escaladorsuelo
        name.contains("escaladora") || name.contains("escalador") -> R.drawable.g_escalador
        name.contains("remo ergómetro") -> R.drawable.g_maquinaremo
        name.contains("bicicleta") -> R.drawable.g_bicicletafija
        name.contains("elíptico") -> R.drawable.g_eliptico
        name.contains("caminata con carga") -> R.drawable.g_caminataconcarga
        name.contains("gateo de oso") -> R.drawable.g_gateodeoso

        // Movilidad
        name.contains("tobillo") -> R.drawable.g_anclemovility
        name.contains("muñeca") && name.contains("movilidad") -> R.drawable.g_wirstmovility
        name.contains("estiramiento") -> R.drawable.g_estiramientos
        name.contains("estiramiento de cuello") -> R.drawable.g_neckstretching
        name.contains("miofacial") || name.contains("miofascial") -> R.drawable.g_liberacionmiofacial
        name.contains("flexor") -> R.drawable.g_flipflexorstretch

        else -> R.drawable.g_swing
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCreationScreen(
    routineId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    val soundPool = remember {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
    }
    val tickSoundId = remember { soundPool.load(context, R.raw.tick, 1) }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    val userRole by dataStoreManager.getUserRole().collectAsState(initial = "")
    val userEmail by dataStoreManager.getUserEmail().collectAsState(initial = "")
    val userName by dataStoreManager.getUserName().collectAsState(initial = "")
    val gymApiUrl by dataStoreManager.getGymApiUrl().collectAsState(initial = "")

    val savedBtnColor by dataStoreManager.getButtonColor().collectAsState(initial = null)
    val accentColor = if (savedBtnColor != null) Color(savedBtnColor!!) else PrimaryTurquoise
    val buttonStyle by dataStoreManager.getButtonStyle().collectAsState(initial = 1)
    val containerShape = getButtonStyleShape(buttonStyle)

    val repository = remember(gymApiUrl) {
        AttendanceRepository(NetworkModule.getApiService(gymApiUrl), dataStoreManager)
    }

    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }
    var selectedObjective by remember { mutableStateOf("Hipertrofia") }
    var selectedLevel by remember { mutableStateOf("Intermedio") }
    var selectedDuration by remember { mutableStateOf("60 min") }
    var selectedCategory by remember { mutableStateOf("FullBody") }

    val selectedDays = remember { mutableStateListOf<String>() }
    var routineType by remember { mutableStateOf("Personal") }
    var routineStatus by remember { mutableStateOf("Activa") }

    val allProfessors by dataStoreManager.getProfessors().collectAsState(initial = emptyList())
    val selectedProfessorIds = remember { mutableStateListOf<String>() }

    val exercises = remember {
        mutableStateListOf(
            ExerciseEntry(name = "PECHO", series = "4", reps = "12", weight = "0", rest = "60", category = "PECHO"),
        )
    }

    val dynamicExercises = remember {
        mutableStateMapOf<String, List<String>>().apply { putAll(PREDEFINED_EXERCISES) }
    }

    val listState = rememberLazyListState()
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) {
        if (routineId != null) {
            val routine = dataStoreManager.getRoutines().first().find { it.id == routineId }
            if (routine != null) {
                routineName = routine.name
                routineDescription = routine.description
                selectedObjective = routine.objective
                selectedLevel = routine.level
                selectedDuration = routine.duration
                selectedCategory = routine.category
                selectedDays.clear()
                selectedDays.addAll(routine.days)
                routineType = routine.type
                routineStatus = routine.status
                selectedProfessorIds.clear()
                selectedProfessorIds.addAll(routine.professorIds)
                exercises.clear()
                routine.exercises.forEach { ex ->
                    exercises.add(ExerciseEntry(
                        id = ex.id,
                        name = ex.name,
                        series = ex.series,
                        reps = ex.reps,
                        weight = ex.weight,
                        rest = ex.rest,
                        time = ex.time,
                        notes = ex.notes,
                        category = ex.name.substringAfterLast("(", "").substringBeforeLast(")").trim().ifBlank { ex.name },
                        isLocked = true // Las rutinas cargadas vienen bloqueadas por defecto
                    ))
                }
            }
        }
    }

    fun handleBack() {
        if (routineName.isNotBlank() || exercises.size > 1 || (exercises.size == 1 && (exercises[0].name != "PECHO"))) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color(0xFF081C24),
            title = { Text("¿Salir sin guardar?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Tienes cambios sin guardar o información incompleta. Si sales ahora, perderás el progreso de esta rutina.", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { showExitDialog = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("SALIR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CONTINUAR EDITANDO", color = accentColor)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (routineId == null) "Constructor de Rutinas" else "Editar Rutina", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = { handleBack() }) {
                            Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back", modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape), colorFilter = ColorFilter.tint(accentColor))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (routineName.isNotBlank()) {
                                scope.launch {
                                    val totalSecs = exercises.sumOf { it.time.toIntOrNull() ?: 0 }
                                    val autoDur = if (totalSecs >= 60) "${totalSecs / 60} min" else "$totalSecs seg"

                                    val newRoutine = Routine(
                                        id = routineId ?: java.util.UUID.randomUUID().toString(),
                                        name = routineName,
                                        description = routineDescription,
                                        objective = selectedObjective,
                                        level = selectedLevel,
                                        duration = autoDur,
                                        category = selectedCategory,
                                        days = selectedDays.toList(),
                                        type = routineType,
                                        status = routineStatus,
                                        professorIds = selectedProfessorIds.toList(),
                                        exercises = exercises.map { entry ->
                                            Exercise(entry.id, entry.name, entry.series, entry.reps, entry.weight, entry.rest, entry.time, entry.notes)
                                        },
                                        creatorName = userName ?: "Tú",
                                        creatorEmail = userEmail ?: "",
                                        updatedAt = System.currentTimeMillis()
                                    )

                                    if (routineId == null) dataStoreManager.addRoutine(newRoutine)
                                    else dataStoreManager.updateRoutine(newRoutine)

                                    if (userRole?.lowercase() == "member" && routineType == "Personal") {
                                        repository.syncMemberRoutine("", userEmail ?: "", Json.encodeToString(newRoutine))
                                    }
                                    onBack()
                                }
                            } else {
                                Toast.makeText(context, "El nombre de la rutina es obligatorio", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.checked),
                                contentDescription = "Guardar",
                                modifier = Modifier.size(28.dp),
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        exercises.add(0, ExerciseEntry(name = "PECHO", series = "4", reps = "10", weight = "0", rest = "60", category = "PECHO"))
                        scope.launch {
                            // Scroll inmediato al inicio de la lista de ejercicios para que coincida con el banner superior
                            listState.animateScrollToItem(index = 4, scrollOffset = 0)
                        }
                    },
                    containerColor = accentColor,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Ejercicio", modifier = Modifier.size(30.dp))
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), 
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    SectionHeader("1. Información General", accentColor)
                    GeneralInfoSection(routineName, { routineName = it }, routineDescription, { routineDescription = it }, selectedObjective, { selectedObjective = it }, selectedLevel, { selectedLevel = it }, selectedDuration, { selectedDuration = it }, selectedCategory, { selectedCategory = it }, accentColor, containerShape)
                }
                item {
                    SectionHeader("2. Configuración", accentColor)
                    ConfigurationSection(selectedDays, routineType, { routineType = it }, routineStatus, { routineStatus = it }, accentColor, containerShape)
                }
                item {
                    SectionHeader("3. Profesores responsables", accentColor)
                    ProfessorsSection(allProfessors, selectedProfessorIds, accentColor, containerShape)
                }
                item { SectionHeader("4. Constructor de ejercicios", accentColor) }

                itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        accentColor = accentColor,
                        shape = containerShape,
                        soundPool = soundPool,
                        tickSoundId = tickSoundId,
                        dynamicExercises = dynamicExercises,
                        onDelete = {
                            val currentList = dynamicExercises[exercise.category] ?: emptyList()
                            if (exercise.name in currentList && exercise.name !in (PREDEFINED_EXERCISES[exercise.category] ?: emptyList())) {
                                dynamicExercises[exercise.category] = currentList.filter { it != exercise.name }
                            }
                            exercises.removeAt(index)
                        },
                        onMoveUp = { if (index > 0) { val e = exercises.removeAt(index); exercises.add(index - 1, e) } },
                        onMoveDown = { if (index < exercises.size - 1) { val e = exercises.removeAt(index); exercises.add(index + 1, e) } },
                        onUpdate = { updated -> exercises[index] = updated }
                    )
                }

                item {
                    SectionHeader("5. Resumen final", accentColor)
                    SummarySection(routineId, exercises, selectedDuration, selectedProfessorIds.size, routineName, routineDescription, selectedObjective, selectedLevel, selectedDays.toList(), routineType, routineStatus, selectedCategory, selectedProfessorIds.toList(), userEmail, userRole, userName, dataStoreManager, allProfessors, repository, accentColor, containerShape, onBack, scope)
                }
            }
        }
    }
}

@Composable
fun LazyItemScope.ExerciseCard(
    exercise: ExerciseEntry,
    accentColor: Color,
    shape: Shape,
    soundPool: SoundPool,
    tickSoundId: Int,
    dynamicExercises: MutableMap<String, List<String>>,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdate: (ExerciseEntry) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(!exercise.isLocked) }
    var activeField by remember { mutableStateOf<String?>(null) }
    var isAddingPersonal by remember { mutableStateOf(false) }
    var personalName by remember { mutableStateOf("") }
    var selectedIconForPersonal by remember { mutableStateOf(exercise.category) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Estado para la animación de entrada suave
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val visualOptions = remember(exercise.category, dynamicExercises[exercise.category]) {
        listOf(exercise.category) + (dynamicExercises[exercise.category] ?: emptyList())
    }

    LaunchedEffect(exercise.category) {
        if (!isAddingPersonal) {
            if (!visualOptions.contains(exercise.name)) {
                onUpdate(exercise.copy(name = exercise.category))
            }
        } else {
            selectedIconForPersonal = exercise.category
        }
    }

    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            containerColor = Color(0xFF081C24),
            title = { Text("Ejercicio Personal", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "MODO PERSONAL:\n1. Elige una categoría e icono.\n2. Escribe el nombre del nuevo ejercicio.\n3. Toca \u0027Listo\u0027 para incorporarlo permanentemente a la lista.",
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text("ENTENDIDO", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF081C24),
            title = { Text("Eliminar Ejercicio", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Estás seguro de que deseas eliminar este ejercicio de tu lista personal?", color = TextGray)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        isVisible = false
                        scope.launch {
                            delay(600)
                            onDelete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("CANCELAR", color = TextWhite)
                }
            }
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(600)),
        exit = shrinkVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(600)),
        modifier = Modifier.animateItem(
            placementSpec = tween(600, easing = FastOutSlowInEasing),
            fadeInSpec = null, // Desactivamos el fade predeterminado de animateItem para usar el de AnimatedVisibility
            fadeOutSpec = null
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = if (isEditing) 2.dp else 1.dp,
                color = if (isEditing) accentColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.05f)
            )
        ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .animateContentSize()) {
            // Contenedor Visual y Categoría (Bloqueado por alpha si no edita)
            Row(modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEditing) 1f else 0.6f), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F151B))
                        .border(1.2.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    WheelPickerScroll(
                        options = visualOptions,
                        currentValue = if (isAddingPersonal) selectedIconForPersonal else exercise.name,
                        accentColor = accentColor,
                        soundPool = soundPool, tickSoundId = tickSoundId,
                        onValueChange = {
                            if (isEditing) {
                                if (isAddingPersonal) selectedIconForPersonal = it
                                else onUpdate(exercise.copy(name = it))
                            }
                        },
                        unit = "", isImage = true, containerHeight = 200.dp,
                        enabled = isEditing
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier
                    .weight(1f)
                    .height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F151B))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelPickerScroll(
                            options = dynamicExercises.keys.toList(), currentValue = exercise.category,
                            accentColor = accentColor, soundPool = soundPool, tickSoundId = tickSoundId,
                            onValueChange = { newCat ->
                                if (isEditing) onUpdate(exercise.copy(category = newCat, name = if(isAddingPersonal) exercise.name else newCat))
                            },
                            unit = "", isImage = false, containerHeight = 150.dp,
                            enabled = isEditing
                        )
                    }

                    Button(
                        onClick = {
                            isAddingPersonal = !isAddingPersonal
                            if(isAddingPersonal) {
                                onUpdate(exercise.copy(name = ""))
                                showInstructionsDialog = true
                                isEditing = true
                            }
                        },
                        enabled = isEditing,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAddingPersonal) accentColor else Color.White.copy(alpha = 0.05f),
                            contentColor = if (isAddingPersonal) Color.Black else accentColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(if(isAddingPersonal) Icons.Default.Close else Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PERSONAL", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isAddingPersonal) {
                OutlinedTextField(
                    value = personalName,
                    onValueChange = { personalName = it },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nombre del ejercicio...", color = TextGray) },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF252D36),
                        unfocusedContainerColor = Color(0xFF252D36),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White
                    )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF252D36))
                        .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                        .alpha(if (isEditing) 1f else 0.7f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (exercise.name == exercise.category) "Seleccione un ejercicio de ${exercise.category}" else exercise.name,
                        color = if (exercise.name == exercise.category) TextGray else Color.White,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Ruedas de parámetros
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                val itemHeight = if (activeField != null && isEditing) 110.dp else 45.dp
                ExerciseWheelItem("Series", exercise.series, (1..20).map { it.toString() }, activeField == "series", itemHeight, accentColor, soundPool, tickSoundId, isEditing, { s -> onUpdate(exercise.copy(series = s)) }, { activeField = "series" }, Modifier.weight(1f))
                ExerciseWheelItem("Reps", exercise.reps, (1..50).map { it.toString() }, activeField == "reps", itemHeight, accentColor, soundPool, tickSoundId, isEditing, { r -> onUpdate(exercise.copy(reps = r)) }, { activeField = "reps" }, Modifier.weight(1f))
                ExerciseWheelItem("Peso", exercise.weight, (0..300).map { it.toString() }, activeField == "weight", itemHeight, accentColor, soundPool, tickSoundId, isEditing, { w -> onUpdate(exercise.copy(weight = w)) }, { activeField = "weight" }, Modifier.weight(1.1f), " kg")
                ExerciseWheelItem("Descanso", exercise.rest, (0..300 step 15).map { it.toString() }, activeField == "rest", itemHeight, accentColor, soundPool, tickSoundId, isEditing, { d -> onUpdate(exercise.copy(rest = d)) }, { activeField = "rest" }, Modifier.weight(1.2f), " seg")
                ExerciseWheelItem("Tiempo", exercise.time, (0..600 step 5).map { it.toString() }, activeField == "time", itemHeight, accentColor, soundPool, tickSoundId, isEditing, { t -> onUpdate(exercise.copy(time = t)) }, { activeField = "time" }, Modifier.weight(1.1f), " s")
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))) { Icon(Icons.Default.KeyboardArrowUp, null, tint = accentColor) }
                IconButton(onClick = onMoveDown, modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))) { Icon(Icons.Default.KeyboardArrowDown, null, tint = accentColor) }

                val canConfirm = if (isAddingPersonal) personalName.isNotBlank() else exercise.name != exercise.category

                // Dentro de Row final de ExerciseCard...
                Box(modifier = Modifier.weight(1f).height(48.dp)) {
                    if (isEditing) {
                        Button(
                            onClick = {
                                if (isAddingPersonal && personalName.isNotBlank()) {
                                    val finalName = "$personalName ($selectedIconForPersonal)"
                                    val currentList = dynamicExercises[exercise.category]?.toMutableList() ?: mutableListOf()
                                    if (finalName !in currentList) {
                                        currentList.add(finalName)
                                        dynamicExercises[exercise.category] = currentList
                                    }
                                    onUpdate(exercise.copy(name = finalName))
                                    isAddingPersonal = false
                                    personalName = ""
                                }
                                scope.launch {
                                    delay(1000) // 1 segundo de espera
                                    isEditing = false
                                    activeField = null
                                    onUpdate(exercise.copy(isLocked = true))
                                }
                            },
                            enabled = canConfirm,
                            modifier = Modifier.fillMaxSize().border(1.dp, accentColor, RoundedCornerShape(10.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalButtonBackgroundColor.current),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val iconColor = if (LocalButtonBackgroundColor.current.luminance() > 0.5f) Color.Black else Color.White
                            Image(
                                painter = painterResource(id = R.drawable.checked),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(iconColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Listo", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = iconColor)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Parte 3/4: Estado
                            Surface(
                                modifier = Modifier.weight(0.75f).fillMaxHeight(),
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.drawable.checked),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            colorFilter = ColorFilter.tint(accentColor)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Agregado", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Parte 1/4: Editar
                            Button(
                                onClick = {
                                    isEditing = true
                                    onUpdate(exercise.copy(isLocked = false))
                                },
                                modifier = Modifier.weight(0.25f).fillMaxHeight(),
                                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val currentList = dynamicExercises[exercise.category] ?: emptyList()
                        if (exercise.name in currentList && exercise.name !in (PREDEFINED_EXERCISES[exercise.category] ?: emptyList())) {
                            showDeleteConfirmDialog = true
                        } else {
                            isVisible = false
                            scope.launch {
                                delay(600)
                                onDelete()
                            }
                        }
                    }
                ) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(24.dp)) }
            }
        }
    }
}
}

@Composable
fun ExerciseWheelItem(label: String, value: String, options: List<String>, isActive: Boolean, height: androidx.compose.ui.unit.Dp, accentColor: Color, soundPool: SoundPool, tickSoundId: Int, isEditing: Boolean, onValueChange: (String) -> Unit, onActivate: () -> Unit, modifier: Modifier, unit: String = "") {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextWhite.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F151B))
            .clickable(enabled = isEditing) { onActivate() }, contentAlignment = Alignment.Center) {
            if (isActive && isEditing) {
                WheelPickerScroll(options, value, accentColor, soundPool, tickSoundId, onValueChange, unit, containerHeight = height)
                Box(modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(8.dp)))
            } else {
                Text(text = "$value$unit", color = if (isEditing) Color.White else Color.White.copy(0.4f), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, accentColor: Color) {
    Text(text = title, color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
}

@Composable
fun LevelStars(level: String, isSelected: Boolean, accentColor: Color) {
    val starCount = when (level) {
        "Principiante" -> 1
        "Intermedio" -> 3
        "Avanzado" -> 5
        else -> 1
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(starCount) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isSelected) Color.Black else accentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneralInfoSection(
    routineName: String, onNameChange: (String) -> Unit,
    routineDescription: String, onDescChange: (String) -> Unit,
    selectedObjective: String, onObjectiveChange: (String) -> Unit,
    selectedLevel: String, onLevelChange: (String) -> Unit,
    selectedDuration: String, onDurationChange: (String) -> Unit,
    selectedCategory: String, onCategoryChange: (String) -> Unit,
    accentColor: Color, containerShape: Shape
) {
    Card(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = containerShape
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RoutineTextField(value = routineName, onValueChange = onNameChange, label = "Nombre de la rutina *", accentColor = accentColor, shape = containerShape)
            RoutineTextField(value = routineDescription, onValueChange = onDescChange, label = "Descripción", accentColor = accentColor, shape = containerShape, singleLine = false, minLines = 2)

            Text("Grupo Muscular", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val categories = listOf(
                "Pecho" to R.drawable.g_categoria_pecho,
                "Espalda" to R.drawable.g_categoria_espalda,
                "FullBody" to R.drawable.g_categoria_cuerpocompleto,
                "Pierna" to R.drawable.g_categoria_piernas,
                "Brazo" to R.drawable.g_categoria_brazos,
                "Abdomen" to R.drawable.g_categoria_abdominales
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { (cat, icon) ->
                            ChoiceChip(
                                text = cat, iconRes = icon,
                                selected = selectedCategory == cat,
                                onClick = { onCategoryChange(cat) },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Text("Objetivo", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val objectives = listOf(
                "Hipertrofia" to R.drawable.hypertrophy,
                "Fuerza" to R.drawable.force,
                "Resistencia" to R.drawable.g_sprint,
                "Definición" to R.drawable.definition,
                "Cardio" to R.drawable.cardio,
                "Rehabilitación" to R.drawable.rehabilitation
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                objectives.chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { (obj, icon) ->
                            ChoiceChip(
                                text = obj, iconRes = icon,
                                selected = selectedObjective == obj,
                                onClick = { onObjectiveChange(obj) },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Text("Nivel", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Principiante", "Intermedio", "Avanzado").forEach { level ->
                    val isSelected = selectedLevel == level
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLevelChange(level) },
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LevelStars(level, isSelected, accentColor)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = level, color = if (isSelected) Color.Black else TextWhite,
                                fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    iconRes: Int? = null
) {
    val buttonBgColor = LocalButtonBackgroundColor.current
    val contentColor = if (selected) (if (accentColor.luminance() > 0.5f) Color.Black else Color.White)
                       else (if (buttonBgColor.luminance() > 0.5f) Color.Black else TextWhite)

    Surface(
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() }
            .border(1.5.dp, if (selected) accentColor else accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        color = if (selected) accentColor else buttonBgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConfigurationSection(selectedDays: MutableList<String>, routineType: String, onTypeChange: (String) -> Unit, routineStatus: String, onStatusChange: (String) -> Unit, accentColor: Color, containerShape: Shape) {
    Card(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = containerShape) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Días recomendados", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val days = listOf("LU", "MA", "MI", "JU", "VI", "SÁ", "DO")
                days.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    val color = accentColor
                    Box(modifier = Modifier
                        .weight(1f)
                        .clip(containerShape)
                        .background(
                            if (isSelected) color.copy(alpha = 0.2f) else Color.White.copy(
                                alpha = 0.03f
                            )
                        )
                        .border(
                            1.dp,
                            if (isSelected) color else Color.White.copy(alpha = 0.05f),
                            containerShape
                        )
                        .clickable {
                            if (isSelected) selectedDays.remove(day) else selectedDays.add(
                                day
                            )
                        }
                        .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(text = day, color = if (isSelected) color else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text("Tipo de rutina", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Personal", "Asignable").forEach { type -> ChoiceChip(text = type, selected = routineType == type, onClick = { onTypeChange(type) }, accentColor = accentColor, modifier = Modifier.weight(1f)) }
            }
            Text("Estado", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Borrador", "Activa", "Archivada").forEach { status -> ChoiceChip(text = status, selected = routineStatus == status, onClick = { onStatusChange(status) }, accentColor = accentColor, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessorsSection(allProfessors: List<Professor>, selectedProfessorIds: MutableList<String>, accentColor: Color, containerShape: Shape) {
    Card(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = containerShape) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (allProfessors.isEmpty()) Text("No hay profesores registrados", color = TextGray, fontSize = 14.sp)
            else FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                allProfessors.forEach { professor -> ProfessorSelectionItem(professor, selectedProfessorIds.contains(professor.id), { if (selectedProfessorIds.contains(professor.id)) selectedProfessorIds.remove(professor.id) else selectedProfessorIds.add(professor.id) }, containerShape) }
            }
        }
    }
}

@Composable
fun SummarySection(
    routineId: String?,
    exercises: List<ExerciseEntry>,
    selectedDuration: String,
    professorsCount: Int,
    routineName: String,
    routineDescription: String,
    selectedObjective: String,
    selectedLevel: String,
    selectedDays: List<String>,
    routineType: String,
    routineStatus: String,
    selectedCategory: String,
    selectedProfessorIds: List<String>,
    userEmail: String?,
    userRole: String?,
    userName: String?,
    dataStoreManager: DataStoreManager,
    allProfessors: List<Professor>,
    repository: AttendanceRepository,
    accentColor: Color,
    containerShape: Shape,
    onBack: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val totalSeconds = exercises.sumOf { it.time.toIntOrNull() ?: 0 }
    val autoDuration = if (totalSeconds >= 60) "${totalSeconds / 60} min" else "$totalSeconds seg"

    Card(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = containerShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen automático", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            SummaryRow("Ejercicios", exercises.size.toString())
            SummaryRow("Series totales", exercises.sumOf { it.series.toIntOrNull() ?: 0 }.toString())
            SummaryRow("Duración total", autoDuration)
            SummaryRow("Profesores asignados", professorsCount.toString())
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextWhite.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfessorSelectionItem(professor: Professor, isSelected: Boolean, onToggle: () -> Unit, containerShape: Shape) {
    val profColor = professor.profileColor?.let { Color(it) } ?: Color.Gray
    val hasPhoto = !professor.photoUri.isNullOrBlank()

    // Usamos CircleShape para perfiles por consistencia visual de UI de usuario
    val profileShape = CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onToggle() }
            .padding(4.dp)
            .width(75.dp) // Ancho fijo para evitar saltos de layout
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(profileShape)
                .background(if (isSelected && !hasPhoto) profColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) profColor else Color.White.copy(alpha = 0.1f),
                    shape = profileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val photo = professor.photoUri.orEmpty()
            if (photo.isNotBlank()) {
                val imgModel = remember(photo) {
                    if (photo.startsWith("data:image")) {
                        try {
                            val base64String = photo.substringAfter("base64,")
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) { photo }
                    } else photo
                }
                AsyncImage(
                    model = imgModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(profileShape),
                    contentScale = ContentScale.Crop,
                    // Añadimos un placeholder por si la carga falla
                    error = painterResource(id = R.drawable.g_categoria_cuerpocompleto),
                    alpha = if (isSelected) 1f else 0.7f
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected) profColor else Color.Gray,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = professor.firstName,
            color = if (isSelected) Color.White else TextGray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RoutineTextField(value: String, onValueChange: (String) -> Unit, label: String, accentColor: Color, shape: Shape, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label, color = TextGray) }, shape = shape, singleLine = singleLine, minLines = minLines, colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF00222E), unfocusedContainerColor = Color(0xFF00222E), focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedLabelColor = accentColor))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPickerScroll(options: List<String>,
                      currentValue: String,
                      accentColor: Color,
                      soundPool: SoundPool,
                      tickSoundId: Int,
                      onValueChange: (String) -> Unit,
                      unit: String,
                      isImage: Boolean = false,
                      containerHeight: androidx.compose.ui.unit.Dp = 105.dp,
                      enabled: Boolean = true
) {
    val itemHeight = if (isImage) 180.dp else 40.dp
    val verticalPadding = (containerHeight - itemHeight) / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = options.indexOf(currentValue).coerceAtLeast(0))

    // CORRECCIÓN: Manejo de FlingBehavior nulable
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val center = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - center)
            }?.index ?: -1
        }
    }

    LaunchedEffect(centerIndex) {
        if (centerIndex != -1 && tickSoundId != 0 && enabled) {
            soundPool.play(tickSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    LaunchedEffect(currentValue, options) {
        val targetIndex = options.indexOf(currentValue).coerceAtLeast(0)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && enabled) {
            if (centerIndex in options.indices) onValueChange(options[centerIndex])
        }
    }

    LazyColumn(
        state = listState,
        // CORRECCIÓN: Si snapFlingBehavior es null, no se asigna. Si enabled es false, se usa un comportamiento por defecto
        flingBehavior = snapFlingBehavior ?: ScrollableDefaults.flingBehavior(),
        userScrollEnabled = enabled,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = verticalPadding.coerceAtLeast(0.dp))
    ) {
        itemsIndexed(options) { index, item ->
            val isCentered = centerIndex == index
            Column(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isImage) {
                    Image(
                        painter = painterResource(id = getExerciseIcon(item)),
                        contentDescription = null,
                        modifier = Modifier.size(if (isCentered) 180.dp else 80.dp),
                        colorFilter = ColorFilter.tint(if (isCentered) accentColor else accentColor.copy(alpha = 0.2f))
                    )
                } else {
                    Text(
                        text = if (isCentered) "$item$unit" else item,
                        color = if (isCentered) Color.White else Color.White.copy(0.2f),
                        fontSize = if (isCentered) 15.sp else 12.sp,
                        fontWeight = if (isCentered) FontWeight.Black else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}