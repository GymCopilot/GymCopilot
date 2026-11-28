package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.model.Routine

actual fun isStandaloneMode(): Boolean = true
actual fun triggerPwaInstall(): Boolean = true
actual fun isIOSDevice(): Boolean = true
actual fun isWebTarget(): Boolean = false

actual fun triggerRoutinePdfExport(
    routines: List<Routine>,
    template: String,
    customTitle: String,
    customObjective: String,
    customSubObjective: String,
    gymName: String
) {}

actual fun updateWebPageBranding(gymName: String, gymLogoUri: String?) {}
actual fun openExternalUrl(url: String) {}
actual fun reloadWebApp() {}
