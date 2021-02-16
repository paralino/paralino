package com.zbapp.location.models.location

data class BaseLocationModel(
    val id: Int = 0,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val sessionId: String,
    val userId: String,
    val circleId: String,

    val time: Long? = null,
    val batteryLevel: Int? = null,
    val imageBase64: String? = null,
)
