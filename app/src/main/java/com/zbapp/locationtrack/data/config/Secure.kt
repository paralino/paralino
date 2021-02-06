package com.zbapp.locationtrack.data.config

import java.util.*


fun randomString(length: Int): String{
    if(length < 1) return ""

    val symbols = CharArray(36)
    for (idx in 0..9)
        symbols[idx] = ('0'.toInt() + idx).toChar()
    for (idx in 10..35)
        symbols[idx] = ('a'.toInt() + idx - 10).toChar()

    val random = Random()
    val buf = CharArray(length)
    for (idx in buf.indices)
        buf[idx] = symbols[random.nextInt(symbols.size)]
    return String(buf)
}