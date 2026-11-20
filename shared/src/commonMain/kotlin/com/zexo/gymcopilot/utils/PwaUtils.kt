package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.model.Routine

expect fun isStandaloneMode(): Boolean
expect fun triggerPwaInstall(): Boolean
expect fun isIOSDevice(): Boolean
expect fun isWebTarget(): Boolean
expect fun triggerRoutinePdfExport(
    routines: List<Routine>,
    template: String,
    customTitle: String,
    customObjective: String,
    customSubObjective: String,
    gymName: String
)
expect fun updateWebPageBranding(gymName: String, gymLogoUri: String?)
expect fun openExternalUrl(url: String)
