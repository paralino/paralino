package com.zbapp.locationtrack.utils.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

fun String.bitmapFromBase64(): Bitmap? {
    return try {
        val decodedString = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    } catch (e: Exception) {
        null
    }
}