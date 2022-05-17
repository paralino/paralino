package com.zbapp.locationtrack.utils.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

fun String.bitmapFromBase64(): Bitmap? {
    return try {
        val decodedString = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun quantityStringResource(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String {
    return LocalContext.current.resources.getQuantityString(id, quantity, *formatArgs)
}