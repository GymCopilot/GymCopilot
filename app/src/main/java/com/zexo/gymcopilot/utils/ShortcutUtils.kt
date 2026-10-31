package com.zexo.gymcopilot.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zexo.gymcopilot.MainActivity
import com.zexo.gymcopilot.R

object ShortcutUtils {
    private const val SHORTCUT_ID = "gym_shortcut_pin"

    fun createGymShortcut(context: Context, gymName: String, logoSource: String?) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return

        val shortcutInfo = buildShortcutInfo(context, gymName, logoSource)
        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }

    fun updateGymShortcut(context: Context, gymName: String, logoSource: String?) {
        val shortcutInfo = buildShortcutInfo(context, gymName, logoSource)
        ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo))
    }

    private fun buildShortcutInfo(context: Context, gymName: String, logoSource: String?): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }

        val label = gymName.ifBlank { "Gym Copilot" }
        
        val bitmap = decodeLogo(context, logoSource)
        val icon = if (bitmap != null) {
            // Usar AdaptiveBitmap para que la imagen ocupe todo el espacio y casi toque los bordes.
            // Android recortará las esquinas según la forma del sistema, pero permitirá una imagen mucho más grande.
            val scaled = scaleForAdaptiveIcon(bitmap)
            IconCompat.createWithAdaptiveBitmap(scaled)
        } else {
            IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }

        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(icon)
            .setIntent(intent)
            .build()
    }

    private fun decodeLogo(context: Context, source: String?): Bitmap? {
        if (source.isNullOrBlank()) return null
        return try {
            if (source.startsWith("data:image")) {
                val base64String = source.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } else if (source.startsWith("file://")) {
                val path = Uri.parse(source).path ?: source.removePrefix("file://")
                val file = java.io.File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } else {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(source))
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleForAdaptiveIcon(bitmap: Bitmap): Bitmap {
        val outerSize = 512
        val squareLogo = cropToSquare(bitmap)

        // Calculamos el tamaño para que quepa dentro de la "Zona Segura" de Android (aprox 66%).
        // Usamos 0.65 para asegurar que el logo completo sea visible independientemente de la máscara del sistema.
        val logoSize = (outerSize * 0.65).toInt()

        val scaledLogo = Bitmap.createScaledBitmap(squareLogo, logoSize, logoSize, true)
        val result = Bitmap.createBitmap(outerSize, outerSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        val offset = (outerSize - logoSize) / 2f
        canvas.drawBitmap(scaledLogo, offset, offset, null)

        return result
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newSize = if (width > height) height else width
        val left = (width - newSize) / 2
        val top = (height - newSize) / 2
        return Bitmap.createBitmap(bitmap, left, top, newSize, newSize)
    }

    fun getBase64FromSource(context: Context, source: String?): String? {
        if (source.isNullOrBlank()) return null
        if (source.startsWith("data:image")) return source
        return try {
            val bitmap = decodeLogo(context, source) ?: return null
            val maxDim = 200
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val width = if (aspectRatio > 1) maxDim else (maxDim * aspectRatio).toInt()
                val height = if (aspectRatio > 1) (maxDim / aspectRatio).toInt() else maxDim
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            null
        }
    }
}
