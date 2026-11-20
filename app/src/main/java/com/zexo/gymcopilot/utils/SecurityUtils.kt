package com.zexo.gymcopilot.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar

object SecurityUtils {
    /**
     * Genera el código dinámico basado en la fecha actual.
     * Algoritmo: (Día + Mes + Año) + "gym"
     */
    fun generateDynamicCode(): String {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "${day + month + year}gym"
    }

    /**
     * Genera el código dinámico para el desbloqueo del modelo de licenciamiento.
     * Algoritmo: (Día + Mes + Año) + "licencia"
     */
    fun generateLicenseDynamicCode(): String {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "${day + month + year}licencia"
    }

    /**
     * Abre la pasarela Checkout Pro de Mercado Pago en el navegador/Chrome.
     * Garantiza que Mercado Pago renderice siempre la pantalla de cobro oficial
     * mostrando la descripción del plan y el importe exacto.
     */
    fun openMercadoPagoUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val cleanUrl = url.trim()

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            } catch (e2: Exception) {}
        }
    }
}
