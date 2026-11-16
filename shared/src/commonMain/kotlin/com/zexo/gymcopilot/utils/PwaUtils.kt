package com.zexo.gymcopilot.utils

expect fun isStandaloneMode(): Boolean
expect fun triggerPwaInstall(): Boolean
expect fun isIOSDevice(): Boolean
expect fun isWebTarget(): Boolean
expect fun printRoutinesWeb(routinesJson: String, title: String, objective: String, subObjective: String)

