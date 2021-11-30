package com.zbapp.core.models.location

import java.util.*

data class MemberModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,

    val name: String,
    val imageBase64: String? = null,

    val data: LocationDataModel? = null
) {

    companion object {
        fun fromSystem(
            userId: String,
            name: String,
            imageBase64: String?,
            model: LocationDataModel
        ): MemberModel {
            return MemberModel(
                id = UUID.randomUUID().toString(),
                groupId = "",
                userId = userId,
                name = name,
                imageBase64 = imageBase64,
                data = model
            )
        }
    }

}

data class LocationDataModel(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val batteryLevel: Int? = null
)
