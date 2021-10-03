package com.zbapp.core.models.location

import java.util.*

data class BaseLocationModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val name: String,
    val imageBase64: String? = null,

    val timeUTC: String? = null,
    val batteryLevel: Int? = null
) {

    companion object {
        fun fromSystem(
            id: String,
            userId: String,
            name: String,
            imageBase64: String?,
            model: PostLocationModel
        ): BaseLocationModel {
            return BaseLocationModel(
                id = UUID.randomUUID().toString(),
                groupId = "",
                userId = userId,
                lat = model.lat,
                lng = model.lng,
                accuracy = model.accuracy,
                name = name,
                imageBase64 = imageBase64,
                timeUTC = model.timeUTC,
                batteryLevel = model.batteryLevel
            )
        }
    }

}

data class PostLocationModel(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val batteryLevel: Int? = null
)
