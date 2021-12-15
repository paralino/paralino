package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class LocationHistoryRaw(
    val id: String,
    val date: String,
    val data: String
)