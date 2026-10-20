package com.zexo.gymcopilot.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import org.jetbrains.skia.Image as SkiaImage
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

actual fun launchImagePicker(onImagePicked: (String) -> Unit) {
    try {
        val fileInput = document.createElement("input") as HTMLInputElement
        fileInput.type = "file"
        fileInput.accept = "image/*"
        fileInput.onchange = {
            val files = fileInput.files
            if (files != null && files.length > 0) {
                val file = files.item(0)
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = {
                        val result = reader.result as? String
                        if (result != null) {
                            onImagePicked(result)
                        }
                    }
                    reader.readAsDataURL(file)
                }
            }
        }
        fileInput.click()
    } catch (e: Throwable) {
        // Fallback
    }
}

actual fun decodeBase64ToBitmap(base64Str: String): ImageBitmap? {
    return try {
        val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val bytes = decodeBase64StringToByteArray(cleanBase64)
        if (bytes != null && bytes.isNotEmpty()) {
            val skiaImage = SkiaImage.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } else {
            null
        }
    } catch (e: Throwable) {
        null
    }
}

private fun decodeBase64StringToByteArray(base64Str: String): ByteArray? {
    return try {
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val clean = base64Str.replace("=", "").replace("\n", "").replace("\r", "").trim()
        val len = clean.length
        val outputLen = (len * 3) / 4
        val out = ByteArray(outputLen)
        var outPos = 0
        var i = 0

        while (i < len) {
            val b1 = table.indexOf(clean[i++])
            val b2 = if (i < len) table.indexOf(clean[i++]) else 0
            val b3 = if (i < len) table.indexOf(clean[i++]) else 0
            val b4 = if (i < len) table.indexOf(clean[i++]) else 0

            val triple = (b1 shl 18) or (b2 shl 12) or (b3 shl 6) or b4

            if (outPos < outputLen) out[outPos++] = ((triple shl 8) ushr 24).toByte()
            if (outPos < outputLen && b3 != -1) out[outPos++] = ((triple shl 16) ushr 24).toByte()
            if (outPos < outputLen && b4 != -1) out[outPos++] = ((triple shl 24) ushr 24).toByte()
        }
        out
    } catch (e: Throwable) {
        null
    }
}
