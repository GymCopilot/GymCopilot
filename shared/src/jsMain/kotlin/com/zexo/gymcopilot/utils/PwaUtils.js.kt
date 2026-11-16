package com.zexo.gymcopilot.utils

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

actual fun printRoutinesWeb(routinesJson: String, title: String, objective: String, subObjective: String) {
    try {
        val printFunc = js("window.printGymCopilotRoutines")
        if (printFunc != null) {
            printFunc(routinesJson, title, objective, subObjective)
        }
    } catch (e: Throwable) {
        println("Error printing routines on web: ${e.message}")
    }
}

