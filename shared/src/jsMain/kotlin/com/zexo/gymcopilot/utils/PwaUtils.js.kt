package com.zexo.gymcopilot.utils

actual fun isStandaloneMode(): Boolean {
    return try {
        val checkFunc = js("window.isGymCopilotStandalone")
        if (checkFunc != null) checkFunc() as? Boolean ?: true else true
    } catch (e: Throwable) {
        true
    }
}

actual fun triggerPwaInstall() {
    try {
        val installFunc = js("window.triggerGymCopilotInstall")
        installFunc?.invoke()
    } catch (e: Throwable) {}
}
