package com.zexo.gymcopilot.utils

actual fun launchQrScanner(onQrScanned: (String) -> Unit) {
    try {
        val launcher = js("window.launchGymCopilotScannerKotlin")
        if (launcher != null) {
            launcher(onQrScanned)
        }
    } catch (_: Throwable) {}
}
