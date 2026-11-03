package com.zexo.gymcopilot.utils

import android.media.ToneGenerator
import android.media.AudioManager

actual fun playTickSound() {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 30)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 15)
    } catch (e: Exception) {}
}
