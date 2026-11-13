package com.zexo.gymcopilot.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zexo.gymcopilot.model.Exercise
import com.zexo.gymcopilot.model.Routine
import com.zexo.gymcopilot.shared.network.GymKtorApiClient
import com.zexo.gymcopilot.utils.playTickSound
import com.zexo.gymcopilot.utils.readLocalMemberProfile
import gymcopilot.shared.generated.resources.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource

data class ExerciseEntryShared(
    val id: String = "ex_${kotlin.random.Random.nextLong()}",
    var name: String,
    var series: String,
    var reps: String,
    var weight: String,
    var rest: String,
    var category: String = "PECHO",
    var time: String = "0",
    var notes: String = "",
    var isLocked: Boolean = false
)

data class LevelItemShared(val name: String, val stars: Int)

val PREDEFINED_EXERCISES_SHARED = mapOf(
    "PECHO" to listOf(
        "Press banca horizontal",
        "Press banco inclinado barra",
        "Press declinado barra",
        "Press plano mancuerna",
        "Press inclinado mancuerna",
        "Aperturas mancuerna",
        "Press de pecho máquina",
        "Press inclinado máquina",
        "Press convergente",
        "Peck Deck máquina",
        "Chest Fly Machine",
        "Cruce de poleas",
        "Cruce de poleas alto",
        "Cruce de poleas bajo",
        "Flexiones clásicas",
        "Flexiones inclinadas",
        "Flexiones diamante",
        "Floor Press",
        "Pullover",
        "Pase de pecho con balón",
        "Wall Balls",
        "Lanzamiento contra pared"
    ),
    "ESPALDA" to listOf(
        "Remo con barra",
        "Remo Pendlay barra",
        "Peso muerto barra",
        "Jalón con brazos rectos",
        "Remo unilateral mancuerna",
        "Remo pecho apoyado mancuerna",
        "Jalón frontal polea",
        "Jalón tras nuca",
        "Jalón unilateral",
        "Jalón agarre estrecho",
        "Jalón supino",
        "Pullover en polea",
        "Face Pull polea",
        "Pullover machine",
        "Remo alto máquina",
        "Encogimientos barra",
        "Remo al mentón",
        "Dominadas",
        "Band Pull-apart",
        "Trepa de cuerda",
        "Farmer Walk",
        "Rodillas al pecho",
        "Swing Ruso",
        "Swing Americano",
        "Lanzamiento al piso",
        "Movilidad torácica",
        "Estiramientos",
        "Estiramiento de cuello"
    ),
    "HOMBROS" to listOf(
        "Press militar barra",
        "Press militar mancuerna",
        "Push Press barra",
        "Press sentado mancuerna",
        "Elevaciones frontales mancuerna",
        "Elevaciones laterales mancuerna",
        "Elevaciones laterales polea",
        "Pájaros mancuerna",
        "Elevación lateral máquina",
        "Reverse Peck Deck",
        "Band Pull-apart",
        "Kettlebell Halo",
        "Kettlebell Windmill",
        "Handstand (Pino)",
        "Lanzamiento contra pared",
        "Rotación de hombros"
    ),
    "BÍCEPS" to listOf(
        "Curl concentrado", "Curl martillo mancuerna", "Curl sentado mancuerna",
        "Curl alternado mancuerna", "Curl EZ barra", "Curl barra recta",
        "Trepa de cuerda", "Curl en polea", "Curl unilateral polea",
        "Curl biceps máquina", "Curl inclinado mancuerna"
    ),
    "TRÍCEPS" to listOf(
        "Press francés barra",
        "Press cerrado barra",
        "Extensión sobre cabeza mancuerna",
        "Extensión sentado mancuerna",
        "Patada de tríceps mancuerna",
        "Extensión de tríceps máquina",
        "Triceps de polea",
        "Pushdown barra polea",
        "Pushdown cuerda polea",
        "Extensión unilateral polea",
        "Fondos",
        "Handstand (Pino)",
        "Pase de pecho con balón",
        "Lanzamiento contra pared"
    ),
    "ANTEBRAZOS" to listOf(
        "Curl de muñeca barra",
        "Curl inverso barra",
        "Farmer Walk",
        "Wrist Curl mancuerna",
        "Reverse Wrist Curl",
        "Trepa de cuerda",
        "Movilidad de muñeca"
    ),
    "ABDOMINALES" to listOf(
        "Crunch en máquina",
        "Abdominal asistido máquina",
        "Crunch suelo",
        "Abdominal corto en el suelo",
        "Elevación de piernas",
        "Plancha",
        "Hollow Hold",
        "Toes to Bar",
        "Crunch en polea",
        "Woodchopper polea",
        "Pallof Press polea",
        "Russian Twist",
        "Hundred",
        "Teaser",
        "Roll Up",
        "Swimming",
        "Side Kick",
        "Single Leg Stretch",
        "Lanzamiento al piso",
        "Lanzamiento sobre cabeza"
    ),
    "OBLICUOS" to listOf(
        "Woodchopper polea", "Rotaciones con polea", "Plancha lateral", "Oblicuos mancuerna", "Russian Twist"
    ),
    "GLÚTEOS" to listOf(
        "Hip Thrust barra",
        "Sentadilla barra",
        "Peso muerto rumano barra",
        "Zancadas mancuerna",
        "Peso muerto rumano mancuerna",
        "Hip Thrust Machine",
        "Abductores máquina",
        "Patada de glúteo polea",
        "Side Kick",
        "Sentadilla con balón",
        "Sentadilla con banda",
        "Elevación del glúteo",
        "Swing Ruso",
        "Swing Americano",
        "Saltos al cajón",
        "Sprint",
        "Estiramiento de flexores"
    ),
    "CUÁDRICEPS" to listOf(
        "Sentadilla barra",
        "Sentadilla frontal barra",
        "Prensa de piernas",
        "Hack Squat",
        "Extensión de cuádriceps máquina",
        "Belt Squat",
        "Goblet Squat mancuerna",
        "Step Up mancuerna",
        "Pistol Squat",
        "Saltos al cajón",
        "Sprint",
        "Carrera en cuestas",
        "Wall Balls",
        "Sentadilla con balón",
        "Sentadilla con banda",
        "Lanzamiento contra pared",
        "Estiramiento de flexores"
    ),
    "ISQUIOTIBIALES" to listOf(
        "Peso muerto rumano barra", "Peso muerto piernas rígidas", "Buenos días barra",
        "Curl femoral acostado máquina", "Nordic Curl"
    ),
    "ADUCTORES" to listOf(
        "Aductores en máquina", "Aducción de cadera polea", "Copenhagen Plank"
    ),
    "ABDUCTORES" to listOf(
        "Abductores en máquina", "Abducción de cadera polea", "Monster Walk con banda", "Sentadilla con banda", "Caminata lateral banda"
    ),
    "GEMELOS" to listOf(
        "Elevación de talones barra",
        "Gemelos de pie máquina",
        "Gemelos sentado máquina",
        "Gemelos en prensa",
        "Calf Raises peso corporal",
        "Saltos al cajón",
        "Trote",
        "Sprint",
        "Carrera en cuestas",
        "Skipping",
        "Saltos dobles (Soga)",
        "Movilidad de tobillo",
        "Elevación unilateral mancuerna"
    ),
    "EJERCICIOS COMBINADOS" to listOf(
        "Clean barra",
        "Snatch barra",
        "Thruster barra",
        "Correr en cinta",
        "Caminar en cinta",
        "Trote",
        "Sprint",
        "Carrera en cuestas",
        "Salto al cajón",
        "Skipping",
        "Trepa de cuerda",
        "Wall Balls",
        "Lanzamiento al piso",
        "Lanzamiento sobre cabeza",
        "Lanzamiento contra pared",
        "Elíptico",
        "Bicicleta fija",
        "Remo ergómetro",
        "Handstand (Pino)",
        "Liberación miofascial",
        "Devil Press mancuerna",
        "Caminata con carga",
        "Mountain Climbers",
        "Gateo de oso",
        "Turkish Get Up Kettlebell",
        "Swing Kettlebell",
        "Clean Kettlebell",
        "Kettlebell Windmill",
        "Kettlebell Halo"
    )
)

fun getExerciseIconShared(exerciseName: String): org.jetbrains.compose.resources.DrawableResource {
    val name = exerciseName.lowercase().trim()
    return when {
        // Categorías
        name == "pecho" -> Res.drawable.g_categoria_pecho
        name == "espalda" || name == "dorsales" -> Res.drawable.g_categoria_espalda
        name == "hombros" -> Res.drawable.g_categoria_hombros
        name == "bíceps" || name == "biceps" -> Res.drawable.g_categoria_biceps
        name == "tríceps" || name == "triceps" -> Res.drawable.g_categoria_triceps
        name == "antebrazos" -> Res.drawable.g_categoria_antebrazo
        name == "abdominales" -> Res.drawable.g_categoria_abdominales
        name == "oblicuos" -> Res.drawable.g_categoria_oblicuos
        name == "glúteos" || name == "gluteos" -> Res.drawable.g_categoria_gluteos
        name == "cuádriceps" || name == "cuadriceps" -> Res.drawable.g_categoria_cuadriceps
        name == "isquiotibiales" -> Res.drawable.g_categoria_isquiotibiales
        name == "aductores" -> Res.drawable.g_aductoresmaquina
        name == "abductores" -> Res.drawable.g_abductoresmaquina
        name == "gemelos" -> Res.drawable.g_gemelosdepie
        name == "ejercicios combinados" || name == "cuerpo completo" || name == "fullbody" -> Res.drawable.g_categoria_cuerpocompleto

        // PECHO
        name.contains("chest fly") || name.contains("machine") -> Res.drawable.g_peckdeck
        name.contains("peck deck") -> Res.drawable.g_peckdeckmaquina
        name.contains("horizontal") && name.contains("press") -> Res.drawable.g_pressbancapechohorizontal
        name.contains("press plano mancuerna") -> Res.drawable.g_pressbancohorizontalmancuerna
        name.contains("press inclinado mancuerna") -> Res.drawable.g_pressinclinado
        name.contains("press banco inclinado barra") -> Res.drawable.g_pressbancapecho
        name.contains("press banca plano") || name.contains("press plano barra") -> Res.drawable.g_pressbancapecho
        name.contains("press inclinado máquina") -> Res.drawable.g_presspechoinclinado
        name.contains("press inclinado") -> Res.drawable.g_presspechoinclinado
        name.contains("press declinado") -> Res.drawable.g_pressdeclinadobarra
        name.contains("press plano") -> Res.drawable.g_pressplano
        name.contains("aperturas") -> Res.drawable.g_aperturas
        name.contains("press convergente") -> Res.drawable.g_pressconvergente
        name.contains("press") && name.contains("máquina") && name.contains("pecho") -> Res.drawable.g_presspechomaquina
        name.contains("cruce ") && name.contains("poleas") && name.contains("bajo") -> Res.drawable.g_presspechoconpolea
        name.contains("cruce") && name.contains("poleas") && name.contains("alto")-> Res.drawable.g_crucedepoleasalto
        name.contains("cruce") && name.contains("poleas") -> Res.drawable.g_crucedepoleas
        name.contains("floor press") -> Res.drawable.g_floorpress
        name.contains("flexiones") && name.contains("inclinadas") -> Res.drawable.g_flexionesinclinadas
        name.contains("flexiones") && name.contains("diamante") -> Res.drawable.g_flexionesdiamente
        name.contains("flexiones") -> Res.drawable.g_flexionesclasicas
        name.contains("pullover machine") -> Res.drawable.g_pullovermachine
        name.contains("pullover en polea") -> Res.drawable.g_pulloverpolea
        name.contains("pullover") -> Res.drawable.g_pulloverbanco
        name.contains("pase") && name.contains("pecho") && name.contains("balón") -> Res.drawable.g_pasedepechobola
        name.contains("lanzamiento") && name.contains("pared") -> Res.drawable.g_lanzamientocontralapered

        // ESPALDA
        name.contains("remo con barra") -> Res.drawable.g_remoconbarra
        name.contains("remo pendlay") -> Res.drawable.g_remopendlay
        name.contains("remo unilateral") -> Res.drawable.g_remounilateral
        name.contains("remo") && name.contains("alto") -> Res.drawable.g_maquinaremo
        name.contains("remo") -> Res.drawable.g_remo
        name.contains("peso muerto barra") -> Res.drawable.g_pasomuertobarra
        name.contains("jalón con brazos rectos") -> Res.drawable.g_pasomuertobarra
        name.contains("jalón frontal") -> Res.drawable.g_jalonfrontal
        name.contains("jalón tras nuca") -> Res.drawable.g_jalontrasnuca
        name.contains("jalón unilateral") -> Res.drawable.g_jalonunilateral
        name.contains("jalón agarre estrecho") -> Res.drawable.g_jalonagarrestrecho
        name.contains("jalón supino") -> Res.drawable.g_jalonsupino
        name.contains("face pull") -> Res.drawable.g_facepulls
        name.contains("encogimientos") -> Res.drawable.g_encogimientohombros
        name.contains("remo al mentón") -> Res.drawable.g_encogimientohombros
        name.contains("dominada") -> Res.drawable.g_dominada
        name.contains("band pull-apart") || name.contains("pull-apart") -> Res.drawable.g_pullaparts
        name.contains("rodillas al pecho") -> Res.drawable.g_leggstretch
        name.contains("movilidad torácica") -> Res.drawable.g_movilidadtoraxica
        name.contains("estiramiento de cuello") -> Res.drawable.g_neckstretching
        name.contains("estiramientos") -> Res.drawable.g_estiramientos

        // HOMBROS
        name.contains("militar") && name.contains("barra") -> Res.drawable.g_pressmilitarbarra
        name.contains("militar") && name.contains("mancuerna") -> Res.drawable.g_pressmilitarmancuerna
        name.contains("militar") -> Res.drawable.g_pressmilitar
        name.contains("press sentado mancuerna") -> Res.drawable.g_presssentadomancuerna
        name.contains("push press") -> Res.drawable.g_pushpress
        name.contains("elevaciones frontales") -> Res.drawable.g_elevacionesfrontales
        name.contains("elevaciones laterales polea") -> Res.drawable.g_elevacionlateraldepoleas
        name.contains("elevaciones laterales") -> Res.drawable.g_elevacioneslaterales
        name.contains("pájaro") || name.contains("reverse fly") -> Res.drawable.g_pajaro
        name.contains("reverse peck deck") -> Res.drawable.g_peckdeckmaquina
        name.contains("kettlebell halo") || name.contains("halo") -> Res.drawable.g_halo
        name.contains("kettlebell windmill") || name.contains("windmill") -> Res.drawable.g_windmill
        name.contains("handstand") || name.contains("pino") -> Res.drawable.g_handstands
        name.contains("rotación de hombros") -> Res.drawable.g_rotaciondehombros

        // TRÍCEPS
        name.contains("press francés") -> Res.drawable.g_pressfrancesbarra
        name.contains("press cerrado") -> Res.drawable.g_presscerradobarra
        name.contains("sobre cabeza") -> Res.drawable.g_extensiontriceps
        name.contains("sentado") && name.contains("mancuerna") -> Res.drawable.g_extensionsentadomancuerna
        name.contains("patada") -> Res.drawable.g_patadatriceps
        name.contains("extensión") && name.contains("tríceps") && name.contains("máquina") -> Res.drawable.g_extensiontricepsmaquina
        name.contains("triceps de polea") || name.contains("tríceps de polea") -> Res.drawable.g_tricepsdecuerdapolea
        name.contains("pushdown") && name.contains("barra") -> Res.drawable.g_pushdownbarrapolea
        name.contains("pushdown") && name.contains("cuerda") -> Res.drawable.g_pushdowncuerda
        name.contains("extensión unilateral") -> Res.drawable.g_extensionunilateraldepoleatriceps
        name.contains("fondos") -> Res.drawable.g_fondo

        // ANTEBRAZOS
        name.contains("curl de muñeca") || name.contains("wrist curl bar") -> Res.drawable.g_curldemunecabarra
        name.contains("wrist curl mancuerna") -> Res.drawable.g_wirstcurlmncuerna
        name.contains("reverse wrist curl") -> Res.drawable.g_reversewirstcurl
        name.contains("movilidad de muñeca") -> Res.drawable.g_anclemovility

        // ABDOMINALES
        name.contains("crunch") && name.contains("máquina") -> Res.drawable.g_crunchenmaquina
        name.contains("abdominal corto") && name.contains("suelo") -> Res.drawable.g_hundred
        name.contains("abdominal") && name.contains("asistido") -> Res.drawable.g_abdominalasistidomaquina
        name.contains("crunch") && name.contains("suelo") -> Res.drawable.g_crunchsuelo
        name.contains("elevación de piernas") || name.contains("abdominales bajos") -> Res.drawable.g_abdominalesbajos
        name.contains("pallof press") -> Res.drawable.g_pallofpress
        name.contains("woodchopper") -> Res.drawable.g_woodchopper
        name.contains("russian twist") -> Res.drawable.g_russiantwist
        name.contains("toes to bar") -> Res.drawable.g_toestobar
        name.contains("hundred") -> Res.drawable.g_hundredpilates
        name.contains("teaser") -> Res.drawable.g_teaser
        name.contains("roll up") -> Res.drawable.g_rolluppilates
        name.contains("swimming") -> Res.drawable.g_swimming
        name.contains("crunch") && name.contains("polea") -> Res.drawable.g_crunchdepolea
        name.contains("hollow hold") -> Res.drawable.g_hollowhold
        name.contains("plancha") -> Res.drawable.g_planchabrazosextendidos
        name.contains("single leg stretch") -> Res.drawable.g_singleleggstretch
        name.contains("lanzamiento") && name.contains("piso") -> Res.drawable.g_lanzamientoalpiso
        name.contains("overhead throw") || (name.contains("lanzamiento") && name.contains("cabeza")) -> Res.drawable.g_overheadthrow
        name.contains("side kick") -> Res.drawable.g_sidekick

        // GLÚTEOS / PIERNAS / CUÁDRICEPS / ABDUCTORES / ADUCTORES / GEMELOS
        name.contains("hip thrust") && name.contains("machine") -> Res.drawable.g_hipthrustmachine
        name.contains("hip thrust") -> Res.drawable.g_hipthrust
        name.contains("peso muerto rumano") -> Res.drawable.g_pasomuertorumanomancuerna
        name.contains("zancada") -> Res.drawable.g_zancada
        name.contains("elevación del glúteo") -> Res.drawable.g_elevaciondelgluteo
        name.contains("patada de glúteo") -> Res.drawable.g_patadagluteopolea
        name.contains("prensa") -> Res.drawable.g_prensadepiernas
        name.contains("hack squat") -> Res.drawable.g_hacksquart
        name.contains("extensión") && name.contains("cuádriceps") -> Res.drawable.g_extensioncuadriceps
        name.contains("sentadilla frontal") -> Res.drawable.g_sentadillafrontal
        name.contains("goblet squat") -> Res.drawable.g_goblesquat
        name.contains("pistol squat") -> Res.drawable.g_pistolsquat
        name.contains("sentadilla") && name.contains("balón") -> Res.drawable.g_sentadillasconbalon
        name.contains("sentadilla") && name.contains("banda") -> Res.drawable.g_sentadillasconbanda
        name.contains("sentadilla") && name.contains("barra") -> Res.drawable.g_sentadillabarra
        name.contains("sentadilla") -> Res.drawable.g_sentadillas
        name.contains("belt squat") -> Res.drawable.g_beltsquat
        name.contains("step up") -> Res.drawable.g_stepup
        name.contains("sprint") -> Res.drawable.g_sprint
        name.contains("carrera en cuestas") || name.contains("cuestas") -> Res.drawable.g_sprint
        name.contains("trote") -> Res.drawable.g_trote
        name.contains("skipping") -> Res.drawable.g_escalador
        name.contains("saltos dobles") || name.contains("soga") -> Res.drawable.g_soga
        name.contains("movilidad de tobillo") -> Res.drawable.g_anclemovility
        name.contains("calf raises") || name.contains("peso corporal") -> Res.drawable.g_calfraisespesocorporal
        name.contains("gemelos en prensa") -> Res.drawable.g_prensadepiernas
        name.contains("wall ball") -> Res.drawable.g_wallballs
        name.contains("salto") && name.contains("cajón") -> Res.drawable.g_boxjump
        name.contains("estiramiento") && name.contains("flexores") -> Res.drawable.g_estiramientos
        name.contains("aducción") && name.contains("cadera") -> Res.drawable.g_aduccionabduccioncadera
        name.contains("abducción") && name.contains("cadera") -> Res.drawable.g_aduccionabduccioncadera
        name.contains("aductores en máquina") -> Res.drawable.g_aductoresmaquina
        name.contains("abductores en máquina") -> Res.drawable.g_abductoresmaquina
        name.contains("copenhagen plank") -> Res.drawable.g_copenaghenplank
        name.contains("caminata lateral banda") || name.contains("caminata lateral") -> Res.drawable.g_caminatalateralconbanda
        name.contains("monster walk") -> Res.drawable.g_monsterwalk

        // COMBINADOS / CARDIO / OTROS
        name.contains("correr en cinta") || (name.contains("cinta") && name.contains("correr")) -> Res.drawable.g_cintacorrer
        name.contains("caminar en cinta") || (name.contains("cinta") && name.contains("caminar")) -> Res.drawable.g_cintacaminar
        name.contains("elíptico") || name.contains("eliptico") -> Res.drawable.g_eliptico
        name.contains("bicicleta fija") -> Res.drawable.g_bicicletafija
        name.contains("remo ergómetro") -> Res.drawable.g_remo
        name.contains("liberación miofascial") -> Res.drawable.g_estiramientos
        name.contains("devil press") -> Res.drawable.g_devilpressmancuerna
        name.contains("caminata con carga") -> Res.drawable.g_caminataconcarga
        name.contains("mountain climbers") -> Res.drawable.g_escaladorsuelo
        name.contains("gateo de oso") -> Res.drawable.g_gateodeoso
        name.contains("turkish get up") -> Res.drawable.g_turkishgetup
        name.contains("swing") -> Res.drawable.g_swing
        name.contains("clean") -> Res.drawable.g_clean
        name.contains("snatch") -> Res.drawable.g_sentadillafrontal
        name.contains("thruster") -> Res.drawable.g_thrusters

        // BÍCEPS
        name.contains("concentrado") -> Res.drawable.g_curlconcentrado
        name.contains("martillo") -> Res.drawable.g_curlmartillomancuerna
        name.contains("sentado mancuerna") -> Res.drawable.g_curlmancuernasentado
        name.contains("alternado") -> Res.drawable.g_curlalternado
        name.contains("ez barra") -> Res.drawable.g_ezcurlbarra
        name.contains("barra recta") -> Res.drawable.g_curlbicepsbarrarecta
        name.contains("trepa de cuerda") -> Res.drawable.g_ropeclimbs
        name.contains("en polea") -> Res.drawable.g_curldepoleas
        name.contains("unilateral polea") -> Res.drawable.g_curlunilateralpoleas
        name.contains("biceps máquina") || name.contains("bíceps máquina") -> Res.drawable.g_curlbicepsmaquina
        name.contains("inclinado mancuerna") -> Res.drawable.g_curlmancuernainclinado
        else -> Res.drawable.fitness_center
    }
}

fun getCategoryIconShared(categoryName: String): org.jetbrains.compose.resources.DrawableResource {
    return when (categoryName.uppercase().trim()) {
        "PECHO" -> Res.drawable.g_categoria_pecho
        "ESPALDA" -> Res.drawable.g_categoria_espalda
        "HOMBROS" -> Res.drawable.g_categoria_hombros
        "BÍCEPS", "BICEPS" -> Res.drawable.g_categoria_biceps
        "TRÍCEPS", "TRICEPS" -> Res.drawable.g_categoria_triceps
        "ANTEBRAZOS" -> Res.drawable.g_categoria_antebrazo
        "ABDOMINALES" -> Res.drawable.g_categoria_abdominales
        "OBLICUOS" -> Res.drawable.g_categoria_oblicuos
        "GLÚTEOS", "GLUTEOS" -> Res.drawable.g_categoria_gluteos
        "CUÁDRICEPS", "CUADRICEPS" -> Res.drawable.g_categoria_cuadriceps
        "ISQUIOTIBIALES" -> Res.drawable.g_categoria_isquiotibiales
        "ADUCTORES" -> Res.drawable.g_aductoresmaquina
        "ABDUCTORES" -> Res.drawable.g_abductoresmaquina
        "GEMELOS" -> Res.drawable.g_gemelosdepie
        "EJERCICIOS COMBINADOS" -> Res.drawable.g_categoria_cuerpocompleto
        else -> Res.drawable.g_categoria_cuerpocompleto
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCreationSharedScreen(
    routineId: String? = null,
    routineToEdit: Routine? = null,
    onBack: () -> Unit,
    onSaveRoutine: (Routine) -> Unit = {},
    apiClient: GymKtorApiClient? = null
) {
    val _unusedClient = apiClient

    var routineName by remember(routineToEdit) { mutableStateOf(routineToEdit?.name ?: "") }
    var routineDescription by remember(routineToEdit) { mutableStateOf(routineToEdit?.description ?: "") }
    var selectedObjective by remember(routineToEdit) { mutableStateOf(routineToEdit?.objective?.ifBlank { "Hipertrofia" } ?: "Hipertrofia") }
    var selectedLevel by remember(routineToEdit) { mutableStateOf(routineToEdit?.level?.ifBlank { "Intermedio" } ?: "Intermedio") }
    val selectedDuration by remember(routineToEdit) { mutableStateOf(routineToEdit?.duration?.ifBlank { "60 min" } ?: "60 min") }

    // Al abrir para crear una rutina nueva, los días NO deben estar seleccionados (lista vacía)
    val selectedDays = remember(routineToEdit) {
        mutableStateListOf<String>().apply {
            if (routineToEdit != null && routineToEdit.days.isNotEmpty()) {
                addAll(routineToEdit.days)
            }
        }
    }
    var routineType by remember(routineToEdit) { mutableStateOf(routineToEdit?.type?.ifBlank { "Personal" } ?: "Personal") }
    var routineStatus by remember(routineToEdit) { mutableStateOf(routineToEdit?.status?.ifBlank { "Activa" } ?: "Activa") }

    // Obtener profesor asignado y profesores reales desde la base de datos (planilla / DB)
    val localMember = remember { readLocalMemberProfile() }
    var remoteProfessorsNames by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val gymUrl = localMember?.gymApiUrl.orEmpty().ifBlank { "https://script.google.com/macros/s/AKfycbzXYYMu43-ATeyxQmwdF3CuRYXi7KbpChyTQEBFIsWh7SCHv2XugxsWen61oBeDdzw/exec" }
            val gymName = localMember?.gymName.orEmpty().ifBlank { "Gym 47" }
            val gymIdVal = gymName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val ssId = localMember?.spreadsheetId.orEmpty()
            val paramChar = if (gymUrl.contains("?")) "&" else "?"
            val ssParam = if (ssId.isNotBlank()) "&spreadsheetId=$ssId" else ""
            val url = "$gymUrl${paramChar}type=professors&gymId=$gymIdVal$ssParam"
            val client = apiClient ?: GymKtorApiClient
            val responseText = client.getData(url)
            if (responseText.isNotBlank() && responseText.contains("success")) {
                val jsonElement = client.jsonInstance.parseToJsonElement(responseText)
                val jsonObject = jsonElement as? JsonObject
                val dataArray = jsonObject?.get("data") as? JsonArray
                if (dataArray != null && dataArray.size > 1) {
                    val list = mutableListOf<String>()
                    for (i in 1 until dataArray.size) {
                        val row = dataArray[i] as? JsonArray
                        if (row != null && row.size > 2) {
                            val fn = row[1].jsonPrimitive.content.trim()
                            val ln = row[2].jsonPrimitive.content.trim()
                            val fullName = "$fn $ln".trim()
                            if (fullName.isNotBlank() && fullName != "null") {
                                list.add(fullName)
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        remoteProfessorsNames = list
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    val professorsList: List<String> = remember(localMember, remoteProfessorsNames) {
        val list = mutableListOf<String>()
        val assigned = localMember?.assignedTrainer?.trim().orEmpty()
        if (assigned.isNotBlank() && !assigned.equals("Sin asignar", true) && !assigned.equals("N/A", true) && assigned != "null") {
            list.add(assigned)
        }
        remoteProfessorsNames.forEach { if (!list.contains(it)) list.add(it) }
        if (list.isEmpty()) {
            list.add(localMember?.firstName?.let { "Prof. $it" } ?: "Profesor Asignado")
        }
        list.distinct()
    }

    val selectedProfessorIds = remember(routineToEdit, professorsList) {
        mutableStateListOf<String>().apply {
            if (routineToEdit != null && routineToEdit.professorIds.isNotEmpty()) {
                addAll(routineToEdit.professorIds)
            } else {
                if (professorsList.isNotEmpty()) add(professorsList.first())
            }
        }
    }

    val exercises = remember(routineToEdit) {
        mutableStateListOf<ExerciseEntryShared>().apply {
            if (routineToEdit != null && routineToEdit.exercises.isNotEmpty()) {
                addAll(
                    routineToEdit.exercises.map { ex ->
                        val cleanWeight = ex.weight.replace("kg", "").trim()
                        val cleanRest = ex.rest.replace("seg", "").replace("s", "").trim()
                        val cleanTime = ex.time.replace("s", "").replace("seg", "").trim()
                        ExerciseEntryShared(
                            id = ex.id,
                            name = ex.name,
                            series = ex.series,
                            reps = ex.reps,
                            weight = if (cleanWeight.isBlank()) "0" else cleanWeight,
                            rest = if (cleanRest.isBlank()) "60" else cleanRest,
                            category = "PECHO",
                            time = if (cleanTime.isBlank()) "0" else cleanTime,
                            notes = ex.notes,
                            isLocked = true
                        )
                    }
                )
            } else {
                add(
                    ExerciseEntryShared(
                        name = "Curl concentrado",
                        series = "4",
                        reps = "12",
                        weight = "0",
                        rest = "60",
                        category = "BÍCEPS"
                    )
                )
            }
        }
    }

    var showErrorDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceColor, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextWhite)
                }
                Text(
                    text = if (routineId == null && routineToEdit == null) "Constructor de Rutinas" else "Editar Rutina",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        if (routineName.isBlank()) {
                            showErrorDialog = true
                        } else {
                            val savedRoutine = Routine(
                                id = routineToEdit?.id ?: "rut_${kotlin.random.Random.nextLong()}",
                                name = routineName,
                                description = routineDescription.ifBlank { "Rutina personal de $selectedObjective" },
                                objective = selectedObjective,
                                level = selectedLevel,
                                duration = selectedDuration,
                                days = selectedDays.toList(),
                                type = routineType,
                                status = routineStatus,
                                category = selectedObjective,
                                professorIds = selectedProfessorIds.toList(),
                                isFavorite = routineToEdit?.isFavorite ?: false,
                                exercises = exercises.map { ex ->
                                    val wVal = ex.weight.replace("kg", "").trim()
                                    val rVal = ex.rest.replace("seg", "").replace("s", "").trim()
                                    Exercise(
                                        id = ex.id,
                                        name = ex.name,
                                        series = ex.series,
                                        reps = ex.reps,
                                        weight = if (wVal.isNotBlank() && !wVal.endsWith("kg")) "$wVal kg" else ex.weight,
                                        rest = if (rVal.isNotBlank() && !rVal.endsWith("seg")) "$rVal seg" else ex.rest,
                                        time = ex.time,
                                        notes = ex.notes
                                    )
                                }
                            )
                            onSaveRoutine(savedRoutine)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryTurquoise.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar", tint = PrimaryTurquoise)
                }
            }

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text("Atención", color = TextWhite, fontWeight = FontWeight.Bold) },
                    text = { Text("No se puede guardar una rutina si no tiene nombre.", color = TextWhite) },
                    confirmButton = {
                        TextButton(onClick = { showErrorDialog = false }) {
                            Text("Aceptar", color = PrimaryTurquoise, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF001F29),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Información General
                item {
                    RoutineSectionHeader("1. Información General")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value = routineName,
                                onValueChange = { routineName = it },
                                placeholder = { Text("Nombre de la rutina *", color = TextGray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F151B),
                                    unfocusedContainerColor = Color(0xFF0F151B),
                                    focusedBorderColor = PrimaryTurquoise,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = routineDescription,
                                onValueChange = { routineDescription = it },
                                placeholder = { Text("Descripción", color = TextGray) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F151B),
                                    unfocusedContainerColor = Color(0xFF0F151B),
                                    focusedBorderColor = PrimaryTurquoise,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Objetivo", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val objectives = listOf(
                                Triple("Hipertrofia", "Hipertrofia", Res.drawable.hypertrophy),
                                Triple("Fuerza", "Fuerza", Res.drawable.force),
                                Triple("Resistencia", "Resistencia", Res.drawable.g_sprint),
                                Triple("Definición", "Definición", Res.drawable.definition),
                                Triple("Cardio", "Cardio", Res.drawable.cardio),
                                Triple("Rehabilitación", "Rehabilitación", Res.drawable.rehabilitation)
                            )
                            objectives.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (obj, label, iconRes) ->
                                        val isSelected = selectedObjective == obj
                                        Card(
                                            onClick = { selectedObjective = obj },
                                            colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                            border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).height(75.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(iconRes),
                                                    contentDescription = label,
                                                    modifier = Modifier.size(28.dp),
                                                    colorFilter = ColorFilter.tint(if (isSelected) Color.Black else TextWhite)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else TextWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text("Nivel", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val levels = listOf(
                                LevelItemShared("Principiante", 1),
                                LevelItemShared("Intermedio", 3),
                                LevelItemShared("Avanzado", 5)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                levels.forEach { lvlItem ->
                                    val isSelected = selectedLevel == lvlItem.name
                                    Card(
                                        onClick = { selectedLevel = lvlItem.name },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(65.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                repeat(lvlItem.stars) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.Black else PrimaryTurquoise,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = lvlItem.name,
                                                color = if (isSelected) Color.Black else TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Configuración (con días deseleccionados al crear una rutina nueva)
                item {
                    RoutineSectionHeader("2. Configuración")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Días recomendados", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val daysMap = listOf("LU" to "L", "MA" to "M", "MI" to "M", "JU" to "J", "VI" to "V", "SÁ" to "S", "DO" to "D")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysMap.forEach { (fullKey, shortLetter) ->
                                    val isSelected = selectedDays.contains(fullKey)
                                    Card(
                                        onClick = {
                                            if (isSelected) selectedDays.remove(fullKey) else selectedDays.add(fullKey)
                                        },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = shortLetter,
                                                color = if (isSelected) Color.Black else TextWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Text("Tipo de rutina", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Personal", "Asignable").forEach { type ->
                                    val isSelected = routineType == type
                                    Card(
                                        onClick = { routineType = type },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(50.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = type,
                                                color = if (isSelected) Color.Black else TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Text("Estado", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Borrador", "Activa", "Archivada").forEach { st ->
                                    val isSelected = routineStatus == st
                                    Card(
                                        onClick = { routineStatus = st },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryTurquoise else Color(0xFF0F151B)),
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryTurquoise else PrimaryTurquoise.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(50.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = st,
                                                color = if (isSelected) Color.Black else TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Profesores responsables (extraído de la base de datos local / Profesor_Asignado)
                item {
                    RoutineSectionHeader("3. Profesores responsables")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Profesor asignado principal", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Obtenido de la base de datos", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                professorsList.forEach { profName ->
                                    val isProfSelected = selectedProfessorIds.contains(profName)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                if (isProfSelected) selectedProfessorIds.remove(profName)
                                                else selectedProfessorIds.add(profName)
                                            }
                                            .width(85.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(if (isProfSelected) PrimaryTurquoise.copy(alpha = 0.2f) else Color(0xFF0F151B))
                                                .border(
                                                    width = if (isProfSelected) 2.dp else 1.dp,
                                                    color = if (isProfSelected) PrimaryTurquoise else Color.White.copy(alpha = 0.1f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = profName,
                                                tint = if (isProfSelected) PrimaryTurquoise else TextGray,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = profName,
                                            color = if (isProfSelected) PrimaryTurquoise else TextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = if (isProfSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Constructor de ejercicios
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoutineSectionHeader("4. Constructor de ejercicios")
                    }
                }

                itemsIndexed(exercises) { index, exercise ->
                    RoutineExerciseItemCard(
                        exercise = exercise,
                        onUpdate = { updated -> exercises[index] = updated },
                        onDelete = { exercises.removeAt(index) },
                        onMoveUp = { if (index > 0) { val e = exercises.removeAt(index); exercises.add(index - 1, e) } },
                        onMoveDown = { if (index < exercises.size - 1) { val e = exercises.removeAt(index); exercises.add(index + 1, e) } }
                    )
                }

                // 5. Resumen final
                item {
                    RoutineSectionHeader("5. Resumen final")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Resumen automático", color = PrimaryTurquoise, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ejercicios", color = TextGray)
                                Text("${exercises.size}", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Series totales", color = TextGray)
                                Text("${exercises.sumOf { it.series.toIntOrNull() ?: 4 }}", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Duración estimada", color = TextGray)
                                Text(selectedDuration, color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estado", color = TextGray)
                                Text("Activa", color = PrimaryTurquoise, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Botón flotante circular + para agregar ejercicios abajo a la derecha
        FloatingActionButton(
            onClick = {
                exercises.add(ExerciseEntryShared(name = "Curl concentrado", series = "4", reps = "12", weight = "0", rest = "60", category = "BÍCEPS"))
            },
            containerColor = PrimaryTurquoise,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir Ejercicio", modifier = Modifier.size(28.dp), tint = Color.Black)
        }
    }
}

@Composable
fun RoutineSectionHeader(title: String) {
    Text(
        text = title,
        color = PrimaryTurquoise,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun RoutineExerciseItemCard(
    exercise: ExerciseEntryShared,
    onUpdate: (ExerciseEntryShared) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var activeField by remember { mutableStateOf<String?>(null) }
    var isPersonalMode by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var customNameText by remember(exercise.name) { mutableStateOf(exercise.name) }

    val categories = PREDEFINED_EXERCISES_SHARED.keys.toList()
    val visualOptions = remember(exercise.category) {
        listOf(exercise.category) + (PREDEFINED_EXERCISES_SHARED[exercise.category] ?: emptyList())
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Ejercicio Personal", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODO PERSONAL:", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("1. Elige una categoría e icono.", color = TextWhite, fontSize = 12.sp)
                    Text("2. Escribe el nombre del nuevo ejercicio.", color = TextWhite, fontSize = 12.sp)
                    Text("3. Toca 'Listo' para incorporarlo permanentemente a la lista.", color = TextWhite, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("ENTENDIDO", color = PrimaryTurquoise, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF001F29),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F151B))
                        .border(1.dp, PrimaryTurquoise.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    WheelPickerScrollSharedImages(
                        options = visualOptions,
                        currentValue = exercise.name,
                        categoryName = exercise.category,
                        accentColor = PrimaryTurquoise,
                        onValueChange = { selectedItem ->
                            if (!isPersonalMode) {
                                onUpdate(exercise.copy(name = selectedItem))
                            }
                        },
                        containerHeight = 200.dp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F151B))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelPickerScrollShared(
                            options = categories,
                            currentValue = exercise.category,
                            onValueChange = { newCat ->
                                val defaultEx = newCat
                                if (!isPersonalMode) {
                                    onUpdate(exercise.copy(category = newCat, name = defaultEx))
                                } else {
                                    onUpdate(exercise.copy(category = newCat))
                                }
                            },
                            unit = "",
                            containerHeight = 150.dp
                        )
                    }

                    Button(
                        onClick = {
                            if (!isPersonalMode) {
                                isPersonalMode = true
                                showInfoDialog = true
                                if (exercise.name == exercise.category) {
                                    onUpdate(exercise.copy(name = ""))
                                    customNameText = ""
                                }
                            } else {
                                isPersonalMode = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPersonalMode) PrimaryTurquoise else Color(0xFF0F151B)
                        ),
                        border = if (isPersonalMode) null else BorderStroke(1.dp, PrimaryTurquoise.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isPersonalMode) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isPersonalMode) Color.Black else PrimaryTurquoise,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "PERSONAL",
                            color = if (isPersonalMode) Color.Black else PrimaryTurquoise,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (isPersonalMode) {
                OutlinedTextField(
                    value = customNameText,
                    onValueChange = {
                        customNameText = it
                        onUpdate(exercise.copy(name = it))
                    },
                    placeholder = { Text("Nombre del ejercicio...", color = TextGray, fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF252D36),
                        unfocusedContainerColor = Color(0xFF252D36),
                        focusedBorderColor = PrimaryTurquoise,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF252D36))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = exercise.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                val itemHeight = if (activeField != null) 100.dp else 42.dp
                val cleanWeight = exercise.weight.replace("kg", "").trim()
                val cleanRest = exercise.rest.replace("seg", "").replace("s", "").trim()
                val cleanTime = exercise.time.replace("s", "").replace("seg", "").trim()

                ParameterWheelBox("Series", exercise.series, (1..20).map { it.toString() }, activeField == "series", itemHeight, PrimaryTurquoise, { s -> onUpdate(exercise.copy(series = s)) }, { activeField = "series" }, Modifier.weight(1f))
                ParameterWheelBox("Reps", exercise.reps, (1..50).map { it.toString() }, activeField == "reps", itemHeight, PrimaryTurquoise, { r -> onUpdate(exercise.copy(reps = r)) }, { activeField = "reps" }, Modifier.weight(1f))
                ParameterWheelBox("Peso", cleanWeight, (0..300).map { it.toString() }, activeField == "weight", itemHeight, PrimaryTurquoise, { w -> onUpdate(exercise.copy(weight = w)) }, { activeField = "weight" }, Modifier.weight(1.1f), " kg")
                ParameterWheelBox("Descanso", cleanRest, (0..300 step 15).map { it.toString() }, activeField == "rest", itemHeight, PrimaryTurquoise, { d -> onUpdate(exercise.copy(rest = d)) }, { activeField = "rest" }, Modifier.weight(1.2f), " seg")
                ParameterWheelBox("Tiempo", cleanTime, (0..600 step 5).map { it.toString() }, activeField == "time", itemHeight, PrimaryTurquoise, { t -> onUpdate(exercise.copy(time = t)) }, { activeField = "time" }, Modifier.weight(1f), " s")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = PrimaryTurquoise)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = PrimaryTurquoise)
                }

                if (exercise.isLocked) {
                    Row(
                        modifier = Modifier.weight(1f).height(44.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.weight(0.75f).fillMaxHeight(),
                            color = Color(0xFF252D36),
                            shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                            border = BorderStroke(1.dp, PrimaryTurquoise)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(Res.drawable.checked),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        colorFilter = ColorFilter.tint(PrimaryTurquoise)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Agregado", color = PrimaryTurquoise, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Button(
                            onClick = { onUpdate(exercise.copy(isLocked = false)) },
                            modifier = Modifier.weight(0.25f).fillMaxHeight(),
                            shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            activeField = null
                            onUpdate(exercise.copy(isLocked = true))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252D36)),
                        border = BorderStroke(1.dp, PrimaryTurquoise),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.checked),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Listo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun WheelPickerScrollShared(
    options: List<String>,
    currentValue: String,
    onValueChange: (String) -> Unit,
    unit: String,
    containerHeight: androidx.compose.ui.unit.Dp = 100.dp
) {
    val itemHeight = 36.dp
    val verticalPadding = (containerHeight - itemHeight) / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = options.indexOf(currentValue).coerceAtLeast(0))
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val center = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - center)
            }?.index ?: -1
        }
    }

    var lastPlayedIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(centerIndex) {
        if (centerIndex != -1 && centerIndex != lastPlayedIndex) {
            lastPlayedIndex = centerIndex
            playTickSound()
        }
    }

    LaunchedEffect(currentValue, options) {
        val targetIndex = options.indexOf(currentValue).coerceAtLeast(0)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            if (centerIndex in options.indices) {
                onValueChange(options[centerIndex])
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = verticalPadding.coerceAtLeast(0.dp))
    ) {
        itemsIndexed(options) { index, item ->
            val isCentered = centerIndex == index
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCentered) "$item$unit" else item,
                    color = if (isCentered) Color.White else Color.White.copy(alpha = 0.25f),
                    fontSize = if (isCentered) 14.sp else 11.sp,
                    fontWeight = if (isCentered) FontWeight.Black else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WheelPickerScrollSharedImages(
    options: List<String>,
    currentValue: String,
    categoryName: String,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    containerHeight: androidx.compose.ui.unit.Dp = 200.dp
) {
    val itemHeight = 200.dp
    val verticalPadding = (containerHeight - itemHeight) / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = options.indexOf(currentValue).coerceAtLeast(0))
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val center = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - center)
            }?.index ?: -1
        }
    }

    var lastPlayedIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(centerIndex) {
        if (centerIndex != -1 && centerIndex != lastPlayedIndex) {
            lastPlayedIndex = centerIndex
            playTickSound()
        }
    }

    LaunchedEffect(currentValue, options) {
        val targetIndex = options.indexOf(currentValue).coerceAtLeast(0)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            if (centerIndex in options.indices) {
                onValueChange(options[centerIndex])
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = verticalPadding.coerceAtLeast(0.dp))
    ) {
        itemsIndexed(options) { index, item ->
            val isCentered = centerIndex == index
            val iconRes = if (index == 0 || item.uppercase().trim() == categoryName.uppercase().trim()) {
                getCategoryIconShared(categoryName)
            } else {
                getExerciseIconShared(item)
            }
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = item,
                    modifier = Modifier.size(if (isCentered) 170.dp else 75.dp),
                    colorFilter = ColorFilter.tint(if (isCentered) accentColor else accentColor.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
fun ParameterWheelBox(
    label: String,
    value: String,
    options: List<String>,
    isActive: Boolean,
    height: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier,
    unit: String = ""
) {
    val cleanVal = value.replace("kg", "").replace("seg", "").replace("s", "").trim()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F151B))
                .border(1.dp, if (isActive) accentColor else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .clickable { onActivate() },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isActive) {
                    WheelPickerScrollShared(
                        options = options,
                        currentValue = cleanVal,
                        onValueChange = onValueChange,
                        unit = unit,
                        containerHeight = height
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                } else {
                    Text(
                        text = "$cleanVal$unit",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
