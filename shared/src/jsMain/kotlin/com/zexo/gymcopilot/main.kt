package com.zexo.gymcopilot

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.zexo.gymcopilot.ui.SharedGymCopilotApp
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow("GymCopilot", canvasElementId = "ComposeTarget") {
            SharedGymCopilotApp()
        }
    }
}
