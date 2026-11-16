package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.model.Routine
import kotlinx.browser.window
import kotlinx.browser.document

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
        val titleText = customTitle.ifBlank { routines.firstOrNull()?.name ?: "Rutina de Entrenamiento" }
        val objectiveText = customObjective.ifBlank { routines.firstOrNull()?.description ?: "Mejora de condición física y rendimiento" }
        val subObjText = customSubObjective.ifBlank { "Programa Enfocado - $template" }

        val primaryColor = when (template) {
            "Ejecutiva" -> "#0F172A"
            "Moderna" -> "#00838F"
            "Minimalista" -> "#222222"
            "Profesional", "Alfa Balcarce" -> "#2E7D32"
            else -> "#1A365D"
        }

        val htmlContent = buildString {
            append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$titleText - $gymName</title>")
            append("<style>")
            append("body { font-family: 'Segoe UI', Arial, sans-serif; color: #222; margin: 20px; background: #f8fafc; }")
            append(".page { background: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin-bottom: 20px; max-width: 800px; margin-left: auto; margin-right: auto; }")
            append(".header { text-align: center; border-bottom: 3px solid $primaryColor; padding-bottom: 15px; margin-bottom: 20px; }")
            append(".gym-title { font-size: 26px; font-weight: 800; color: $primaryColor; letter-spacing: 1px; }")
            append(".routine-title { font-size: 22px; font-weight: 700; margin-top: 6px; color: #00838F; }")
            append(".meta { font-size: 13px; color: #555; margin-top: 8px; background: #f1f5f9; padding: 8px 12px; border-radius: 6px; display: inline-block; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 15px; margin-bottom: 20px; }")
            append("th, td { border: 1px solid #cbd5e1; padding: 10px 12px; font-size: 13px; text-align: left; }")
            append("th { background-color: $primaryColor; color: white; font-weight: 600; }")
            append("tr:nth-child(even) { background-color: #f8fafc; }")
            append(".notes-box { margin-top: 15px; padding: 10px; background: #fdf6b2; border-left: 4px solid #eab308; font-size: 12px; border-radius: 4px; }")
            append(".footer { margin-top: 30px; text-align: center; font-size: 11px; color: #64748b; border-top: 1px solid #e2e8f0; padding-top: 12px; }")
            append("@media print { body { background: #fff; } .page { box-shadow: none; padding: 0; margin: 0; max-width: none; } body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }")
            append("</style></head><body>")

            routines.forEach { routine ->
                append("<div class='page'>")
                append("<div class='header'>")
                append("<div class='gym-title'>${gymName.uppercase()}</div>")
                append("<div class='routine-title'>${routine.name.ifBlank { titleText }}</div>")
                append("<div class='meta'><strong>Objetivo:</strong> $objectiveText &nbsp;|&nbsp; <strong>Programa:</strong> $subObjText &nbsp;|&nbsp; <strong>Duración:</strong> ${routine.duration}</div>")
                append("</div>")

                append("<table><thead><tr><th>#</th><th>Ejercicio</th><th>Series</th><th>Reps</th><th>Peso</th><th>Descanso</th><th>Notas Técnicas</th></tr></thead><tbody>")
                routine.exercises.forEachIndexed { idx, ex ->
                    append("<tr>")
                    append("<td><strong>${idx + 1}</strong></td>")
                    append("<td><strong>${ex.name}</strong></td>")
                    append("<td>${ex.series}</td>")
                    append("<td>${ex.reps}</td>")
                    append("<td>${ex.weight}</td>")
                    append("<td>${ex.rest}</td>")
                    append("<td>${ex.notes}</td>")
                    append("</tr>")
                }
                append("</tbody></table>")

                if (routine.notes.isNotBlank()) {
                    append("<div class='notes-box'><strong>Observaciones Generales:</strong> ${routine.notes}</div>")
                }

                append("<div class='footer'>Generado por GymCopilot • Plantilla: $template • www.gymcopilot.com</div>")
                append("</div>")
            }

            append("</body></html>")
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
