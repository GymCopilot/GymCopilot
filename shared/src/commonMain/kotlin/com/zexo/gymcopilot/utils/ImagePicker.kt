package com.zexo.gymcopilot.utils

import androidx.compose.ui.graphics.ImageBitmap

expect fun launchImagePicker(onImagePicked: (String) -> Unit)

expect fun decodeBase64ToBitmap(base64Str: String): ImageBitmap?
