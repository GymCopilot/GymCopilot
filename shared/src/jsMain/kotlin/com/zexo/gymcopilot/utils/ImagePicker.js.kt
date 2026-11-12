package com.zexo.gymcopilot.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skia.Image as SkiaImage
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

actual fun launchImagePicker(onImagePicked: (String) -> Unit) {
    try {
        val fileInput = document.createElement("input") as HTMLInputElement
        fileInput.type = "file"
        fileInput.accept = "image/*"
        fileInput.style.display = "none"

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
                        try { document.body?.removeChild(fileInput) } catch (t: Throwable) {}
                    }
                    reader.readAsDataURL(file)
                } else {
                    try { document.body?.removeChild(fileInput) } catch (t: Throwable) {}
                }
            } else {
                try { document.body?.removeChild(fileInput) } catch (t: Throwable) {}
            }
        }

        document.body?.appendChild(fileInput)
        fileInput.click()
    } catch (e: Throwable) {
        // Fallback
    }
}

actual fun decodeBase64ToBitmap(base64Str: String): ImageBitmap? {
    return try {
        val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val clean = cleanBase64.replace("-", "+").replace("_", "/").replace("\n", "").replace("\r", "").trim()
        val binaryStr = window.atob(clean)
        val bytes = ByteArray(binaryStr.length)
        for (i in 0 until binaryStr.length) {
            bytes[i] = binaryStr[i].code.toByte()
        }
        if (bytes.isNotEmpty()) {
            val skiaImage = SkiaImage.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } else {
            null
        }
    } catch (e: Throwable) {
        null
    }
}

actual fun generateQrBitmap(content: String, size: Int): ImageBitmap? {
    return try {
        val qrFn = js("QRCode")
        if (qrFn != null) {
            val canvas = document.createElement("canvas") as org.w3c.dom.HTMLCanvasElement
            canvas.width = size
            canvas.height = size
            qrFn.toCanvas(canvas, content, js("({ width: size, margin: 2 })"))
            val dataUrl = canvas.toDataURL("image/png")
            decodeBase64ToBitmap(dataUrl)
        } else null
    } catch (e: Throwable) {
        null
    }
}
