package com.zbapp.location.models.location

data class BaseLocationModel constructor(
    val id: String,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val sessionId: String,
    val circleId: String,

    val userId: String,
    val name: String,
    val imageBase64: String? = null,

    val time: Long? = null,
    val batteryLevel: Int? = null,
)
