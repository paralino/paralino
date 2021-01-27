package com.zbapp.locationtrack.utils.extensions

fun <T> Any?.safeCast(): T? {
    return this as? T?
}

fun <T> Any.cast(): T {
    return this as T
}