package com.zbapp.core.utils

fun randomString(length: Int = 32): String {
    val source = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { source.random() }
        .joinToString("")
}