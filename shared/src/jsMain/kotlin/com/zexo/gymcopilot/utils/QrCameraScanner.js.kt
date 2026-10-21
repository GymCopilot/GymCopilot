package com.zexo.gymcopilot.utils

actual fun launchQrScanner(onQrScanned: (String) -> Unit) {
    try {
        val scannerFunc = js("window.openGymCopilotQrScanner")
        if (scannerFunc != null) {
            scannerFunc(onQrScanned)
        }
    } catch (e: Throwable) {
        // Fallback
    }
}
