package com.zbapp.core.models.location

data class BaseLocationModel constructor(
    val id: String,
    val circleId: String,
    val userId: String,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val name: String,
    val imageBase64: String? = null,

    val timeUTC: String? = null,
    val batteryLevel: Int? = null
)
