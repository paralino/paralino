package com.zbapp.location.utils

import kotlinx.coroutines.flow.MutableStateFlow

fun <T> MutableStateFlow<T>.replace(action: T.() -> T) {
    value = action(value)
}