package com.zexo.gymcopilot.utils

actual fun isStandaloneMode(): Boolean = true
actual fun triggerPwaInstall(): Boolean = true
actual fun isIOSDevice(): Boolean = false
actual fun isWebTarget(): Boolean = false
actual fun printRoutinesWeb(routinesJson: String, title: String, objective: String, subObjective: String) {}

