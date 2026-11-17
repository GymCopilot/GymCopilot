package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.model.Routine
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element

actual fun isStandaloneMode(): Boolean {
    return try {
        val checkFunc = js("window.isGymCopilotStandalone")
        if (checkFunc != null) checkFunc() as? Boolean ?: false else false
    } catch (e: Throwable) {
        false
    }
}

actual fun triggerPwaInstall(): Boolean {
    return try {
        val installFunc = js("window.triggerGymCopilotInstall")
        if (installFunc != null) installFunc() as? Boolean ?: false else false
    } catch (e: Throwable) {
        false
    }
}

actual fun isIOSDevice(): Boolean {
    return try {
        val iosFunc = js("window.isIOSDevice")
        if (iosFunc != null) iosFunc() as? Boolean ?: false else false
    } catch (e: Throwable) {
        false
    }
}

actual fun isWebTarget(): Boolean = true

actual fun updateWebPageBranding(gymName: String, gymLogoUri: String?) {
    try {
        if (gymName.isNotBlank() && gymName != "Gym 19" && gymName != "Nombre del Gimnasio") {
            document.title = gymName
            val metaAppTitle = document.querySelector("meta[name='apple-mobile-web-app-title']")
            metaAppTitle?.setAttribute("content", gymName)
        }
        if (!gymLogoUri.isNullOrBlank()) {
            val links = document.querySelectorAll("link[rel*='icon']")
            for (i in 0 until links.length) {
                val link = links.item(i) as? Element
                link?.setAttribute("href", gymLogoUri)
            }
        }
    } catch (e: Throwable) {}
}

actual fun triggerRoutinePdfExport(
    routines: List<Routine>,
    template: String,
    customTitle: String,
    customObjective: String,
    customSubObjective: String,
    gymName: String
) {
    try {
        val firstRoutine = routines.firstOrNull() ?: Routine(id = "", name = "", description = "", level = "", objective = "", exercises = emptyList(), days = emptyList())
        val titleText = customTitle.ifBlank { firstRoutine.name.ifBlank { "RUTINA DE ENTRENAMIENTO" } }
        val objectiveText = customObjective.ifBlank { firstRoutine.objective.ifBlank { "TRATAMIENTO RODILLAS EN VALGO Y HERNIA DE DISCO" } }
        val subObjText = customSubObjective.ifBlank { firstRoutine.description.ifBlank { "FUERZA FUNCIONAL - ESTABILIDAD - MOVILIDAD - PROTECCIÓN ARTICULAR - SALUD DE LA COLUMNA" } }

        val localProfile = readLocalMemberProfile()
        val userName = listOf(localProfile?.firstName, localProfile?.lastName).filter { !it.isNullOrBlank() }.joinToString(" ").ifBlank { "Alumno / Socio" }
        val currentDate = "Hoy"
        val gymAddress = localProfile?.address.orEmpty().ifBlank { "Calle 13 entre 16 y 14, Chaves N° 564" }
        val gymCity = "Balcarce, Buenos Aires"
        val gymInstagram = "@tugimnasio"
        val gymSlogan = "DISCIPLINA HOY, RESULTADOS SIEMPRE."

        val htmlContent = when (template) {
            "Moderna" -> buildModernHtml(routines, template, titleText, objectiveText, subObjText, userName, currentDate, gymAddress, gymCity, gymInstagram, gymSlogan, gymName)
            "Ejecutiva" -> buildExecutiveHtml(routines, template, titleText, objectiveText, subObjText, userName, currentDate, gymAddress, gymCity, gymInstagram, gymSlogan, gymName)
            "Minimalista" -> buildMinimalistHtml(routines, template, titleText, objectiveText, subObjText, userName, currentDate, gymAddress, gymCity, gymInstagram, gymSlogan, gymName)
            "Profesional", "Alfa Balcarce" -> buildProHtml(routines, template, titleText, objectiveText, subObjText, userName, currentDate, gymAddress, gymCity, gymInstagram, gymSlogan, gymName)
            else -> buildClassicHtml(routines, template, titleText, objectiveText, subObjText, userName, currentDate, gymAddress, gymCity, gymInstagram, gymSlogan, gymName)
        }

        val win = window.open("", "_blank")
        if (win != null) {
            win.document.open()
            win.document.write(htmlContent)
            win.document.close()
            win.setTimeout({
                win.print()
            }, 600)
        }
    } catch (e: Throwable) {
        println("Error triggering routine PDF export: ${e.message}")
    }
}

private fun commonA4Style(primaryColor: String): String = buildString {
    append("@page { size: A4 portrait; margin: 0; }")
    append("body { font-family: Arial, sans-serif; color: #111; margin: 0; padding: 0; background: #555; display: flex; justify-content: center; }")
    append(".sheet { width: 210mm; min-height: 297mm; max-width: 210mm; margin: 0 auto; padding: 12mm; box-sizing: border-box; background: #fff; display: flex; flex-direction: column; justify-content: space-between; box-shadow: 0 0 10px rgba(0,0,0,0.5); }")
    append(".top-content { flex: 1; }")
    append(".bottom-content { margin-top: auto; padding-top: 15px; }")
    append(".footer-section { display: flex; gap: 8px; margin-top: 15px; }")
    append(".footer-col { flex: 1; border: 0.8px solid $primaryColor; font-size: 8px; }")
    append(".footer-col-title { background: $primaryColor; color: white; padding: 4px 6px; font-weight: bold; font-size: 8px; text-align: center; }")
    append(".footer-col-body { padding: 6px; color: #333; }")
    append("table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }")
    append("th, td { border: 0.5px solid #CBD5E1; padding: 6px 8px; font-size: 9px; text-align: left; }")
    append("tr:nth-child(even) { background: #F9F9F9; }")
    append("@media print { body { background: #fff; padding: 0; } .sheet { border: none; box-shadow: none; width: 210mm; min-height: 297mm; max-width: none; padding: 12mm; } body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }")
}

private fun buildClassicHtml(
    routines: List<Routine>, template: String, titleText: String, objectiveText: String, subObjText: String,
    userName: String, currentDate: String, gymAddress: String, gymCity: String, gymInstagram: String, gymSlogan: String, gymName: String
): String = buildString {
    val primaryColor = "#1A365D"
    append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
    append("<style>")
    append(commonA4Style(primaryColor))
    append(".header-box { border: 1.5px solid #4A5568; padding: 10px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }")
    append(".logo-area { width: 90px; height: 80px; background: $primaryColor; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: bold; }")
    append(".header-info { flex: 1; margin-left: 15px; }")
    append(".gym-name { font-size: 26px; font-weight: bold; color: $primaryColor; text-transform: uppercase; }")
    append(".gym-loc { font-size: 10px; color: #555; text-transform: uppercase; margin-top: 3px; }")
    append(".header-title-area { text-align: right; }")
    append(".rutina-title { font-size: 13px; font-weight: bold; color: #000; }")
    append(".rutina-sub { font-size: 10px; color: #555; margin-top: 2px; }")
    append(".focus-box { border: 0.5px solid #4A5568; background: #f8fafc; text-align: center; padding: 6px; font-size: 8px; font-weight: bold; color: #333; margin-bottom: 8px; text-transform: uppercase; }")
    append(".member-box { border: 0.5px solid #CBD5E1; padding: 6px 10px; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; margin-bottom: 12px; background: #fff; }")
    append(".day-header { background: $primaryColor; color: white; padding: 6px 10px; font-size: 10px; font-weight: bold; display: flex; justify-content: space-between; margin-top: 10px; }")
    append("th { background: #F7FAFC; color: #000; font-weight: bold; }")
    append(".bottom-footer { border: 0.5px solid #CBD5E1; padding: 8px 10px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; color: #555; }")
    append("</style></head><body>")

    append("<div class='sheet'>")
    append("<div class='top-content'>")
    append("<div class='header-box'><div class='logo-area'>TG</div><div class='header-info'><div class='gym-name'>$gymName</div><div class='gym-loc'>$gymAddress · $gymCity</div></div><div class='header-title-area'><div class='rutina-title'>${titleText.take(45)}</div><div class='rutina-sub'>${objectiveText.take(55)}</div></div></div>")
    append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")
    append("<div class='member-box'><span>NOMBRE: $userName</span><span>FECHA: $currentDate</span><span>OBJETIVO: $objectiveText</span><span>NIVEL: General</span></div>")
    routines.forEachIndexed { rIdx, r ->
        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        append("<div class='day-header'><span>DÍA ${rIdx + 1} — $dayName</span><span>${r.description.ifBlank { "ENFOQUE: TREN INFERIOR" }.uppercase()}</span></div>")
        append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
        r.exercises.forEachIndexed { idx, ex ->
            append("<tr><td><strong>${idx + 1}</strong></td><td><strong>${ex.name}</strong></td><td>${ex.series}</td><td>${ex.reps}</td><td>${ex.notes.ifBlank { "Controla la postura y técnica." }}</td></tr>")
        }
        append("</tbody></table>")
    }
    append("</div>") // End top-content

    append("<div class='bottom-content'>")
    append("<div class='footer-section'>")
    append("<div class='footer-col'><div class='footer-col-title'>INDICACIONES GENERALES</div><div class='footer-col-body'>✓ Realizar entrada en calor de 10 min.<br>✓ Priorizar técnica antes que peso.<br>✓ Aumentar cargas progresivamente.<br>✓ Mantener buena hidratación.<br>✓ Consultar con profesional ante dudas.</div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>REGISTRO DE PROGRESO</div><div class='footer-col-body' style='padding: 2px;'><table style='margin:0; font-size:7px;'><tr><th>FECHA</th><th>DOLOR (0-10)</th><th>REND.</th></tr>")
    repeat(3) { append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>") }
    append("</table></div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>NOTAS</div><div class='footer-col-body'>${routines.firstOrNull()?.notes.orEmpty().ifBlank { "Consultar cualquier duda con tu profesor." }}</div></div>")
    append("</div>")
    append("<div class='bottom-footer'><span>$gymAddress · $gymCity</span><strong style='color: $primaryColor;'>$gymInstagram</strong><span>$gymSlogan</span></div>")
    append("</div>") // End bottom-content

    append("</div></body></html>")
}

private fun buildModernHtml(
    routines: List<Routine>, template: String, titleText: String, objectiveText: String, subObjText: String,
    userName: String, currentDate: String, gymAddress: String, gymCity: String, gymInstagram: String, gymSlogan: String, gymName: String
): String = buildString {
    val primaryColor = "#333333"
    append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
    append("<style>")
    append(commonA4Style(primaryColor))
    append(".header-box { background: #333333; color: white; padding: 12px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }")
    append(".logo-area { width: 95px; height: 75px; background: #E65100; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: bold; }")
    append(".header-info { flex: 1; margin-left: 15px; }")
    append(".gym-name { font-size: 26px; font-weight: bold; color: white; text-transform: uppercase; }")
    append(".gym-loc { font-size: 10px; color: #B0BEC5; text-transform: uppercase; margin-top: 3px; }")
    append(".header-title-area { text-align: right; }")
    append(".rutina-title { font-size: 12px; font-weight: bold; color: white; }")
    append(".rutina-sub { font-size: 9px; color: #B0BEC5; margin-top: 2px; }")
    append(".focus-box { border: 1px solid #E65100; background: #FFF3E0; text-align: center; padding: 6px; font-size: 8px; font-weight: bold; color: #E65100; margin-bottom: 8px; text-transform: uppercase; }")
    append(".member-box { border: 0.8px solid #CBD5E1; padding: 6px 10px; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; margin-bottom: 12px; background: #fff; }")
    append(".day-header { background: #333333; color: white; padding: 6px 10px; font-size: 10px; font-weight: bold; display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }")
    append(".day-badge { background: #E65100; color: white; padding: 2px 8px; font-size: 8px; font-weight: bold; margin-right: 8px; }")
    append("th { background: #ECEFF1; color: #000; font-weight: bold; }")
    append(".footer-col-title { background: #333333; }")
    append(".bottom-footer { background: #333333; color: white; padding: 8px 10px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; }")
    append("</style></head><body>")

    append("<div class='sheet'>")
    append("<div class='top-content'>")
    append("<div class='header-box'><div class='logo-area'>TG</div><div class='header-info'><div class='gym-name'>$gymName</div><div class='gym-loc'>CENTRO DE ENTRENAMIENTO · $gymCity</div></div><div class='header-title-area'><div class='rutina-title'>${titleText.take(45)}</div><div class='rutina-sub'>${objectiveText.take(55)}</div></div></div>")
    append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")
    append("<div class='member-box'><span>• NOMBRE: $userName</span><span>• FECHA: $currentDate</span><span>• OBJETIVO: $objectiveText</span><span>• NIVEL: General</span></div>")
    routines.forEachIndexed { rIdx, r ->
        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        append("<div class='day-header'><div><span class='day-badge'>DÍA ${rIdx + 1}</span> <span>$dayName</span></div><span style='color: #B0BEC5; font-size: 8px;'>${r.exercises.size} EJERCICIOS</span></div>")
        append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
        r.exercises.forEachIndexed { idx, ex ->
            val numStr = (idx + 1).toString().padStart(2, '0')
            append("<tr><td><strong style='color: #E65100;'>$numStr</strong></td><td><strong>${ex.name}</strong></td><td>${ex.series}</td><td>${ex.reps}</td><td>${ex.notes.ifBlank { "Controlar postura y técnica adecuada." }}</td></tr>")
        }
        append("</tbody></table>")
    }
    append("</div>") // End top-content

    append("<div class='bottom-content'>")
    append("<div class='footer-section'>")
    append("<div class='footer-col'><div class='footer-col-title'>INDICACIONES GENERALES</div><div class='footer-col-body'>• Realizar entrada en calor de 10 min.<br>• Priorizar técnica antes que peso.<br>• Aumentar cargas progresivamente.<br>• Mantener buena hidratación.<br>• Consultar con profesional ante dudas.</div></div>")
    append("<div class='footer-col'><div class='footer-col-title' style='background: #333333;'>REGISTRO DE PROGRESO</div><div class='footer-col-body' style='padding: 2px;'><table style='margin:0; font-size:7px;'><tr><th style='background:#333333; color:white;'>FECHA</th><th style='background:#333333; color:white;'>DOLOR</th><th style='background:#333333; color:white;'>REND.</th></tr>")
    repeat(3) { append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>") }
    append("</table></div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>NOTAS</div><div class='footer-col-body'>${routines.firstOrNull()?.notes.orEmpty().ifBlank { "Consultar cualquier duda con tu profesor." }}</div></div>")
    append("</div>")
    append("<div class='bottom-footer'><span>$gymAddress · $gymCity</span><strong style='color: #80CBC4;'>$gymInstagram</strong><span>$gymSlogan</span></div>")
    append("</div>") // End bottom-content

    append("</div></body></html>")
}

private fun buildExecutiveHtml(
    routines: List<Routine>, template: String, titleText: String, objectiveText: String, subObjText: String,
    userName: String, currentDate: String, gymAddress: String, gymCity: String, gymInstagram: String, gymSlogan: String, gymName: String
): String = buildString {
    val primaryColor = "#1B4D3E"
    append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
    append("<style>")
    append(commonA4Style(primaryColor))
    append(".header-box { border: 1.5px solid #4B5563; padding: 12px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }")
    append(".logo-area { width: 100px; height: 75px; background: $primaryColor; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: bold; }")
    append(".header-info { flex: 1; margin-left: 15px; }")
    append(".gym-name { font-size: 32px; font-weight: bold; color: $primaryColor; text-transform: uppercase; }")
    append(".gym-loc { font-size: 10px; color: #4B5563; text-transform: uppercase; margin-top: 3px; }")
    append(".header-title-area { text-align: right; }")
    append(".rutina-title { font-size: 12px; font-weight: bold; color: #000; }")
    append(".rutina-sub { font-size: 9px; color: #4B5563; margin-top: 2px; }")
    append(".focus-box { border: 1px solid #2E7D32; background: #E8F5E9; text-align: center; padding: 6px; font-size: 8px; font-weight: bold; color: #1B5E20; margin-bottom: 8px; text-transform: uppercase; }")
    append(".member-box { border: 1px solid #E0E0E0; padding: 6px 10px; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; margin-bottom: 12px; background: #fff; }")
    append(".day-header { background: $primaryColor; color: white; padding: 6px 10px; font-size: 10px; font-weight: bold; display: flex; justify-content: space-between; margin-top: 10px; }")
    append("th { background: #E2E8F0; color: #000; font-weight: bold; }")
    append(".bottom-footer { border: 0.5px solid #E0E0E0; padding: 8px 10px; margin-top: 12px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; color: #555; }")
    append("</style></head><body>")

    append("<div class='sheet'>")
    append("<div class='top-content'>")
    append("<div class='header-box'><div class='logo-area'>TG</div><div class='header-info'><div class='gym-name'>$gymName</div><div class='gym-loc'>$gymAddress · $gymCity</div></div><div class='header-title-area'><div class='rutina-title'>${titleText.take(45)}</div><div class='rutina-sub'>${objectiveText.take(55)}</div></div></div>")
    append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")
    append("<div class='member-box'><span>NOMBRE: $userName</span><span>FECHA: $currentDate</span><span>OBJETIVO: $objectiveText</span><span>NIVEL: General</span></div>")
    routines.forEachIndexed { rIdx, r ->
        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        append("<div class='day-header'><span>DÍA ${rIdx + 1} · $dayName</span><span>MÓDULO ${rIdx + 1}</span></div>")
        append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
        r.exercises.forEachIndexed { idx, ex ->
            append("<tr><td><strong>${idx + 1}</strong></td><td><strong>${ex.name}</strong></td><td>${ex.series}</td><td>${ex.reps}</td><td>${ex.notes.ifBlank { "Controla la postura y técnica." }}</td></tr>")
        }
        append("</tbody></table>")
    }
    append("</div>") // End top-content

    append("<div class='bottom-content'>")
    append("<div class='footer-section'>")
    append("<div class='footer-col'><div class='footer-col-title'>INDICACIONES GENERALES</div><div class='footer-col-body'>• Realizar entrada en calor de 10 min.<br>• Priorizar técnica antes que peso.<br>• Aumentar cargas progresivamente.<br>• Mantener buena hidratación.<br>• Consultar con profesional ante dudas.</div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>REGISTRO DE PROGRESO</div><div class='footer-col-body' style='padding: 2px;'><table style='margin:0; font-size:7px;'><tr><th>FECHA</th><th>DOLOR</th><th>REND.</th></tr>")
    repeat(3) { append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>") }
    append("</table></div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>NOTAS</div><div class='footer-col-body'>${routines.firstOrNull()?.notes.orEmpty().ifBlank { "Consultar cualquier duda con tu profesor." }}</div></div>")
    append("</div>")
    append("<div class='bottom-footer'><span>$gymAddress · $gymCity</span><strong style='color: $primaryColor;'>$gymInstagram</strong><span>$gymSlogan</span></div>")
    append("</div>") // End bottom-content

    append("</div></body></html>")
}

private fun buildMinimalistHtml(
    routines: List<Routine>, template: String, titleText: String, objectiveText: String, subObjText: String,
    userName: String, currentDate: String, gymAddress: String, gymCity: String, gymInstagram: String, gymSlogan: String, gymName: String
): String = buildString {
    val primaryColor = "#222222"
    append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
    append("<style>")
    append(commonA4Style(primaryColor))
    append(".header-box { border: 1px solid #CBD5E1; padding: 10px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }")
    append(".logo-area { width: 80px; height: 70px; border: 1px solid #222; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: bold; }")
    append(".header-info { flex: 1; margin-left: 15px; }")
    append(".gym-name { font-size: 22px; font-weight: bold; color: #222; text-transform: uppercase; }")
    append(".gym-loc { font-size: 9px; color: #555; text-transform: uppercase; margin-top: 3px; }")
    append(".header-title-area { text-align: right; }")
    append(".rutina-title { font-size: 11px; font-weight: bold; color: #000; }")
    append(".rutina-sub { font-size: 8.5px; color: #555; margin-top: 2px; }")
    append(".focus-box { border-top: 1px solid #222; border-bottom: 1px solid #222; text-align: center; padding: 6px; font-size: 8px; font-weight: bold; color: #222; margin-bottom: 8px; text-transform: uppercase; }")
    append(".member-box { padding: 4px 0; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; border-bottom: 1px solid #CBD5E1; margin-bottom: 10px; }")
    append(".day-header { background: #222; color: white; padding: 5px 10px; font-size: 9px; font-weight: bold; display: flex; justify-content: space-between; margin-top: 10px; }")
    append("th { background: #F1F5F9; color: #000; font-weight: bold; }")
    append(".footer-col-title { background: #222; }")
    append(".bottom-footer { border-top: 1px solid #CBD5E1; padding: 8px 10px; margin-top: 12px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; color: #555; }")
    append("</style></head><body>")

    append("<div class='sheet'>")
    append("<div class='top-content'>")
    append("<div class='header-box'><div class='header-info'><div class='gym-name'>$gymName</div><div class='gym-loc'>$gymAddress · $gymCity</div></div><div class='logo-area'>TG</div><div class='header-title-area'><div class='rutina-title'>${titleText.take(45)}</div><div class='rutina-sub'>${objectiveText.take(55)}</div></div></div>")
    append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")
    append("<div class='member-box'><span>Nombre: $userName</span><span>Fecha: $currentDate</span><span>Objetivo: $objectiveText</span><span>Nivel: General</span></div>")
    routines.forEachIndexed { rIdx, r ->
        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        append("<div class='day-header'><span>DÍA ${rIdx + 1} — $dayName</span><span>${r.description.ifBlank { "ENFOQUE" }.uppercase()}</span></div>")
        append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
        r.exercises.forEachIndexed { idx, ex ->
            val numStr = (idx + 1).toString().padStart(2, '0')
            append("<tr><td><strong>$numStr</strong></td><td><strong>${ex.name}</strong></td><td>${ex.series}</td><td>${ex.reps}</td><td>${ex.notes.ifBlank { "Controla la postura." }}</td></tr>")
        }
        append("</tbody></table>")
    }
    append("</div>") // End top-content

    append("<div class='bottom-content'>")
    append("<div class='footer-section'>")
    append("<div class='footer-col'><div class='footer-col-title'>INDICACIONES GENERALES</div><div class='footer-col-body'>• Realizar entrada en calor de 10 min.<br>• Priorizar técnica antes que peso.<br>• Aumentar cargas progresivamente.<br>• Mantener buena hidratación.<br>• Consultar con profesional ante dudas.</div></div>")
    append("<div class='footer-col'><div class='footer-col-title' style='background:#222;'>REGISTRO DE PROGRESO</div><div class='footer-col-body' style='padding: 2px;'><table style='margin:0; font-size:7px;'><tr><th style='background:#222; color:white;'>FECHA</th><th style='background:#222; color:white;'>DOLOR</th><th style='background:#222; color:white;'>REND.</th></tr>")
    repeat(3) { append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>") }
    append("</table></div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>NOTAS</div><div class='footer-col-body'>${routines.firstOrNull()?.notes.orEmpty().ifBlank { "Consultar cualquier duda con tu profesor." }}</div></div>")
    append("</div>")
    append("<div class='bottom-footer'><span>$gymAddress · $gymCity</span><strong style='color: #222;'>$gymInstagram</strong><span>$gymSlogan</span></div>")
    append("</div>") // End bottom-content

    append("</div></body></html>")
}

private fun buildProHtml(
    routines: List<Routine>, template: String, titleText: String, objectiveText: String, subObjText: String,
    userName: String, currentDate: String, gymAddress: String, gymCity: String, gymInstagram: String, gymSlogan: String, gymName: String
): String = buildString {
    val primaryColor = "#1E1E1E"
    append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
    append("<style>")
    append(commonA4Style(primaryColor))
    append(".header-box { background: #1E1E1E; color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; border-radius: 4px; }")
    append(".logo-area { width: 90px; height: 85px; background: #333; color: white; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 26px; font-weight: bold; border: 1.5px solid #E2E8F0; }")
    append(".header-info { flex: 1; text-align: center; margin: 0 15px; }")
    append(".gym-name { font-size: 26px; font-weight: bold; color: white; text-transform: uppercase; }")
    append(".gym-city { font-size: 11px; color: #E2E8F0; font-weight: bold; margin-top: 4px; }")
    append(".rutina-title { font-size: 13px; font-weight: bold; color: white; margin-top: 6px; }")
    append(".rutina-sub { font-size: 10px; color: #E2E8F0; margin-top: 2px; }")
    append(".right-box { width: 90px; height: 85px; border: 1.5px dashed #E2E8F0; display: flex; align-items: center; justify-content: center; font-size: 8px; color: #E2E8F0; text-align: center; }")
    append(".focus-box { background: #333333; color: white; text-align: center; padding: 6px; font-size: 8.5px; font-weight: bold; margin-bottom: 8px; text-transform: uppercase; }")
    append(".member-box { border: 1px solid #1E293B; padding: 6px 10px; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; margin-bottom: 12px; background: #fff; color: #1E1E1E; }")
    append(".day-header { background: #2E7D32; color: white; padding: 6px 10px; font-size: 10px; font-weight: bold; display: flex; justify-content: space-between; margin-top: 10px; }")
    append("th { background: #E6F4EA; color: #000; font-weight: bold; }")
    append(".footer-col-title { background: #2E7D32; }")
    append(".bottom-footer { border: 0.5px solid #CBD5E1; padding: 8px 10px; margin-top: 12px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; color: #555; }")
    append("</style></head><body>")

    append("<div class='sheet'>")
    append("<div class='top-content'>")
    append("<div class='header-box'><div class='logo-area'>TG</div><div class='header-info'><div class='gym-name'>$gymName</div><div class='gym-city'>★ ${gymCity.uppercase()} ★</div><div class='rutina-title'>${titleText.take(45)}</div><div class='rutina-sub'>${objectiveText.take(55)}</div></div><div class='right-box'>ANATOMÍA H-PRO</div></div>")
    append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")
    append("<div class='member-box'><span>NOMBRE: $userName</span><span>FECHA: $currentDate</span><span>OBJETIVO: $objectiveText</span><span>NIVEL: General</span></div>")
    routines.forEachIndexed { rIdx, r ->
        val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
        append("<div class='day-header'><span>DÍA ${rIdx + 1} — $dayName</span><span>${r.description.ifBlank { "ENFOQUE PROFESIONAL" }.uppercase()}</span></div>")
        append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
        r.exercises.forEachIndexed { idx, ex ->
            append("<tr><td><strong>${idx + 1}</strong></td><td><strong>${ex.name}</strong></td><td>${ex.series}</td><td>${ex.reps}</td><td>${ex.notes.ifBlank { "Controla la postura y técnica." }}</td></tr>")
        }
        append("</tbody></table>")
    }
    append("</div>") // End top-content

    append("<div class='bottom-content'>")
    append("<div class='footer-section'>")
    append("<div class='footer-col'><div class='footer-col-title'>INDICACIONES GENERALES</div><div class='footer-col-body'>✓ Realizar entrada en calor de 10 min.<br>✓ Priorizar técnica antes que peso.<br>✓ Aumentar cargas progresivamente.<br>✓ Mantener buena hidratación.<br>✓ Consultar con profesional ante dudas.</div></div>")
    append("<div class='footer-col'><div class='footer-col-title' style='background: #2E7D32;'>REGISTRO DE PROGRESO</div><div class='footer-col-body' style='padding: 2px;'><table style='margin:0; font-size:7px;'><tr><th style='background:#E6F4EA;'>FECHA</th><th style='background:#E6F4EA;'>DOLOR</th><th style='background:#E6F4EA;'>REND.</th></tr>")
    repeat(3) { append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>") }
    append("</table></div></div>")
    append("<div class='footer-col'><div class='footer-col-title'>NOTAS</div><div class='footer-col-body'>${routines.firstOrNull()?.notes.orEmpty().ifBlank { "Consultar cualquier duda con tu profesor." }}</div></div>")
    append("</div>")
    append("<div class='bottom-footer'><span>$gymAddress · $gymCity</span><strong style='color: #2E7D32;'>$gymInstagram</strong><span>$gymSlogan</span></div>")
    append("</div>") // End bottom-content

    append("</div></body></html>")
}
