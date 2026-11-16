package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.model.Routine
import kotlinx.browser.window

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

        val htmlContent = if (template == "Moderna") {
            buildString {
                append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
                append("<style>")
                append("body { font-family: Arial, sans-serif; color: #111; margin: 0; padding: 15px; background: #fff; }")
                append(".modern-sheet { width: 100%; max-width: 750px; margin: 0 auto; border: 1.5px solid #333333; padding: 15px; box-sizing: border-box; background: #fff; }")

                // Header
                append(".header-box { background: #333333; color: white; padding: 12px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }")
                append(".logo-area { width: 95px; height: 75px; background: #E65100; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: bold; }")
                append(".header-info { flex: 1; margin-left: 15px; }")
                append(".gym-name { font-size: 26px; font-weight: bold; color: white; text-transform: uppercase; }")
                append(".gym-loc { font-size: 10px; color: #B0BEC5; text-transform: uppercase; margin-top: 3px; }")
                append(".header-title-area { text-align: right; }")
                append(".rutina-title { font-size: 12px; font-weight: bold; color: white; }")
                append(".rutina-sub { font-size: 9px; color: #B0BEC5; margin-top: 2px; }")

                // Focus box
                append(".focus-box { border: 1px solid #E65100; background: #FFF3E0; text-align: center; padding: 6px; font-size: 8px; font-weight: bold; color: #E65100; margin-bottom: 8px; text-transform: uppercase; }")

                // Member info box
                append(".member-box { border: 0.8px solid #CBD5E1; padding: 6px 10px; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; margin-bottom: 12px; background: #fff; }")

                // Day table
                append(".day-header { background: #333333; color: white; padding: 6px 10px; font-size: 10px; font-weight: bold; display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }")
                append(".day-badge { background: #E65100; color: white; padding: 2px 8px; font-size: 8px; font-weight: bold; margin-right: 8px; }")
                append("table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }")
                append("th, td { border: 0.5px solid #CBD5E1; padding: 6px 8px; font-size: 9px; text-align: left; }")
                append("th { background: #ECEFF1; color: #000; font-weight: bold; }")
                append("tr:nth-child(even) { background: #F9F9F9; }")

                // Footer section (3 columns)
                append(".footer-section { display: flex; gap: 8px; margin-top: 15px; }")
                append(".footer-col { flex: 1; border: 0.8px solid #333333; font-size: 8px; }")
                append(".footer-col-title { background: #333333; color: white; padding: 4px 6px; font-weight: bold; font-size: 8px; text-align: center; }")
                append(".footer-col-body { padding: 6px; color: #333; }")

                // Bottom institutional footer
                append(".bottom-footer { background: #333333; color: white; padding: 8px 10px; margin-top: 12px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; }")
                append("@media print { body { padding: 0; background: #fff; } .modern-sheet { border: none; box-shadow: none; max-width: none; } body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }")
                append("</style></head><body>")

                append("<div class='modern-sheet'>")

                // Header
                append("<div class='header-box'>")
                append("<div class='logo-area'>TG</div>")
                append("<div class='header-info'>")
                append("<div class='gym-name'>$gymName</div>")
                append("<div class='gym-loc'>CENTRO DE ENTRENAMIENTO · $gymCity</div>")
                append("</div>")
                append("<div class='header-title-area'>")
                append("<div class='rutina-title'>${titleText.take(45)}</div>")
                append("<div class='rutina-sub'>${objectiveText.take(55)}</div>")
                append("</div>")
                append("</div>")

                // Focus
                append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")

                // Member Info
                append("<div class='member-box'>")
                append("<span>• NOMBRE: $userName</span>")
                append("<span>• FECHA: $currentDate</span>")
                append("<span>• OBJETIVO: $objectiveText</span>")
                append("<span>• NIVEL: ${firstRoutine.level.ifBlank { "General" }}</span>")
                append("</div>")

                // Routines / Days
                routines.forEachIndexed { rIdx, r ->
                    val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
                    append("<div class='day-header'><div><span class='day-badge'>DÍA ${rIdx + 1}</span> <span>$dayName</span></div><span style='color: #B0BEC5; font-size: 8px;'>${r.exercises.size} EJERCICIOS</span></div>")
                    append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
                    r.exercises.forEachIndexed { idx, ex ->
                        val exNum = (idx + 1).toString().padStart(2, '0')
                        append("<tr>")
                        append("<td><strong style='color: #E65100;'>$exNum</strong></td>")
                        append("<td><strong>${ex.name}</strong></td>")
                        append("<td>${ex.series}</td>")
                        append("<td>${ex.reps}</td>")
                        append("<td>${ex.notes.ifBlank { "Controlar postura y técnica adecuada." }}</td>")
                        append("</tr>")
                    }
                    append("</tbody></table>")
                }

                // Footer 3 columns
                append("<div class='footer-section'>")

                // Col 1: Indicaciones
                append("<div class='footer-col'>")
                append("<div class='footer-col-title'>INDICACIONES GENERALES</div>")
                append("<div class='footer-col-body'>")
                append("• Realizar entrada en calor de 10 min.<br>")
                append("• Priorizar técnica antes que peso.<br>")
                append("• Aumentar cargas progresivamente.<br>")
                append("• Mantener buena hidratación.<br>")
                append("• Consultar con profesional ante dudas.")
                append("</div></div>")

                // Col 2: Registro de Progreso
                append("<div class='footer-col'>")
                append("<div class='footer-col-title' style='background: #333333;'>REGISTRO DE PROGRESO</div>")
                append("<div class='footer-col-body' style='padding: 2px;'>")
                append("<table style='margin:0; font-size:7px;'><tr><th style='background:#333333; color:white;'>FECHA</th><th style='background:#333333; color:white;'>DOLOR</th><th style='background:#333333; color:white;'>REND.</th></tr>")
                repeat(3) {
                    append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>")
                }
                append("</table></div></div>")

                // Col 3: Notas
                append("<div class='footer-col'>")
                append("<div class='footer-col-title'>NOTAS</div>")
                append("<div class='footer-col-body'>${firstRoutine.notes.ifBlank { "Consultar cualquier duda con tu profesor." }}</div>")
                append("</div>")

                append("</div>") // End footer-section

                // Bottom institutional footer
                append("<div class='bottom-footer'>")
                append("<span>$gymAddress · $gymCity</span>")
                append("<strong style='color: #80CBC4;'>$gymInstagram</strong>")
                append("<span>$gymSlogan</span>")
                append("</div>")

                append("</div>") // End modern-sheet
                append("</body></html>")
            }
        } else {
            // Classic Template
            buildString {
                append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
                append("<style>")
                append("body { font-family: Arial, sans-serif; color: #111; margin: 0; padding: 15px; background: #fff; }")
                append(".classic-sheet { width: 100%; max-width: 750px; margin: 0 auto; border: 1.5px solid #4A5568; padding: 15px; box-sizing: border-box; background: #fff; }")

                // Header
                append(".header-box { border: 1.5px solid #4A5568; padding: 10px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }")
                append(".logo-area { width: 90px; height: 80px; background: #1A365D; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: bold; }")
                append(".header-info { flex: 1; margin-left: 15px; }")
                append(".gym-name { font-size: 26px; font-weight: bold; color: #1A365D; text-transform: uppercase; }")
                append(".gym-loc { font-size: 10px; color: #555; text-transform: uppercase; margin-top: 3px; }")
                append(".header-title-area { text-align: right; }")
                append(".rutina-title { font-size: 13px; font-weight: bold; color: #000; }")
                append(".rutina-sub { font-size: 10px; color: #555; margin-top: 2px; }")

                // Focus box
                append(".focus-box { border: 0.5px solid #4A5568; background: #f8fafc; text-align: center; padding: 6px; font-size: 8px; font-weight: bold; color: #333; margin-bottom: 8px; text-transform: uppercase; }")

                // Member info box
                append(".member-box { border: 0.5px solid #CBD5E1; padding: 6px 10px; display: flex; justify-content: space-between; font-size: 9px; font-weight: bold; margin-bottom: 12px; background: #fff; }")

                // Day table
                append(".day-header { background: #1A365D; color: white; padding: 6px 10px; font-size: 10px; font-weight: bold; display: flex; justify-content: space-between; margin-top: 10px; }")
                append("table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }")
                append("th, td { border: 0.5px solid #CBD5E1; padding: 6px 8px; font-size: 9px; text-align: left; }")
                append("th { background: #F7FAFC; color: #000; font-weight: bold; }")
                append("tr:nth-child(even) { background: #F8FAFC; }")

                // Footer section (3 columns)
                append(".footer-section { display: flex; gap: 8px; margin-top: 15px; }")
                append(".footer-col { flex: 1; border: 0.5px solid #4A5568; font-size: 8px; }")
                append(".footer-col-title { background: #1A365D; color: white; padding: 4px 6px; font-weight: bold; font-size: 8px; text-align: center; }")
                append(".footer-col-body { padding: 6px; color: #333; }")

                // Bottom institutional footer
                append(".bottom-footer { border: 0.5px solid #CBD5E1; padding: 8px 10px; margin-top: 12px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; color: #555; }")
                append("@media print { body { padding: 0; background: #fff; } .classic-sheet { border: none; box-shadow: none; max-width: none; } body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }")
                append("</style></head><body>")

                append("<div class='classic-sheet'>")

                // Header
                append("<div class='header-box'>")
                append("<div class='logo-area'>TG</div>")
                append("<div class='header-info'>")
                append("<div class='gym-name'>$gymName</div>")
                append("<div class='gym-loc'>$gymAddress · $gymCity</div>")
                append("</div>")
                append("<div class='header-title-area'>")
                append("<div class='rutina-title'>${titleText.take(45)}</div>")
                append("<div class='rutina-sub'>${objectiveText.take(55)}</div>")
                append("</div>")
                append("</div>")

                // Focus
                append("<div class='focus-box'>PROGRAMA ENFOCADO EN: $subObjText</div>")

                // Member Info
                append("<div class='member-box'>")
                append("<span>NOMBRE: $userName</span>")
                append("<span>FECHA: $currentDate</span>")
                append("<span>OBJETIVO: $objectiveText</span>")
                append("<span>NIVEL: ${firstRoutine.level.ifBlank { "General" }}</span>")
                append("</div>")

                // Routines / Days
                routines.forEachIndexed { rIdx, r ->
                    val dayName = if (r.days.isNotEmpty()) r.days.joinToString(", ").uppercase() else r.name.uppercase()
                    append("<div class='day-header'><span>DÍA ${rIdx + 1} — $dayName</span><span>${r.description.ifBlank { "ENFOQUE: TREN INFERIOR" }.uppercase()}</span></div>")
                    append("<table><thead><tr><th style='width: 30px;'>#</th><th>EJERCICIO</th><th style='width: 50px;'>SERIES</th><th style='width: 60px;'>REPS</th><th>NOTAS TÉCNICAS</th></tr></thead><tbody>")
                    r.exercises.forEachIndexed { idx, ex ->
                        append("<tr>")
                        append("<td><strong>${idx + 1}</strong></td>")
                        append("<td><strong>${ex.name}</strong></td>")
                        append("<td>${ex.series}</td>")
                        append("<td>${ex.reps}</td>")
                        append("<td>${ex.notes.ifBlank { "Controla la postura y técnica." }}</td>")
                        append("</tr>")
                    }
                    append("</tbody></table>")
                }

                // Footer 3 columns
                append("<div class='footer-section'>")

                // Col 1: Indicaciones
                append("<div class='footer-col'>")
                append("<div class='footer-col-title'>INDICACIONES GENERALES</div>")
                append("<div class='footer-col-body'>")
                append("✓ Realizar entrada en calor de 10 min.<br>")
                append("✓ Priorizar técnica antes que peso.<br>")
                append("✓ Aumentar cargas progresivamente.<br>")
                append("✓ Mantener buena hidratación.<br>")
                append("✓ Consultar con profesional ante dudas.")
                append("</div></div>")

                // Col 2: Registro de Progreso
                append("<div class='footer-col'>")
                append("<div class='footer-col-title'>REGISTRO DE PROGRESO</div>")
                append("<div class='footer-col-body' style='padding: 2px;'>")
                append("<table style='margin:0; font-size:7px;'><tr><th>FECHA</th><th>DOLOR (0-10)</th><th>REND.</th></tr>")
                repeat(3) {
                    append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>")
                }
                append("</table></div></div>")

                // Col 3: Notas
                append("<div class='footer-col'>")
                append("<div class='footer-col-title'>NOTAS</div>")
                append("<div class='footer-col-body'>${firstRoutine.notes.ifBlank { "Consultar cualquier duda con tu profesor." }}</div>")
                append("</div>")

                append("</div>") // End footer-section

                // Bottom institutional footer
                append("<div class='bottom-footer'>")
                append("<span>$gymAddress · $gymCity</span>")
                append("<strong style='color: #1A365D;'>$gymInstagram</strong>")
                append("<span>$gymSlogan</span>")
                append("</div>")

                append("</div>") // End classic-sheet
                append("</body></html>")
            }
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
