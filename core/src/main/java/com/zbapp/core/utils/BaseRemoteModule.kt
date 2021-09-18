package com.zbapp.core.utils

import kotlinx.serialization.json.Json

object BaseRemoteModule {
    val formatter = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}
