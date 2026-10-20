package com.zexo.gymcopilot.utils

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun launchImagePicker(onImagePicked: (String) -> Unit) {
    // Android activity picker
}

actual fun decodeBase64ToBitmap(base64Str: String): ImageBitmap? {
    return try {
        val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Throwable) {
        null
    }
}
