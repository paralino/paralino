package com.zbapp.location.models.location

data class BaseLocationModel(
    val id: String,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val sessionId: String,
    val userId: String,

    val time: Long? = null,
    val batteryLevel: Int? = null,
    val imageBase64: String? = null,
)
