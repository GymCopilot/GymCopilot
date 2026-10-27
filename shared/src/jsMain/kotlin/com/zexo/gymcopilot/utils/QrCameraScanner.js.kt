package com.zexo.gymcopilot.utils

import kotlinx.browser.window

actual fun launchQrScanner(onQrScanned: (String) -> Unit) {
    try {
        val launcher = js("window.launchGymCopilotScannerKotlin")
        if (launcher != null) {
            launcher(onQrScanned)
        } else {
            val code = window.prompt("Ingresa el código QR o Email del socio para asistencia:")
            if (!code.isNullOrBlank()) {
                onQrScanned(code)
            }
        }
    } catch (e: Throwable) {
        try {
            val code = window.prompt("Ingresa el código QR o Email del socio para asistencia:")
            if (!code.isNullOrBlank()) {
                onQrScanned(code)
            }
        } catch (_: Throwable) {}
    }
}
