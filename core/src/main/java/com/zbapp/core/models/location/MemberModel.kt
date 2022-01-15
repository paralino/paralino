package com.zbapp.core.models.location

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MemberModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,
    val publicKey: String,
    val data: MemberModelData,
    val mutableData: LocationDataModel? = null
) {

    val name: String get() = data.name
    val imageBase64: String? get() = data.imageBase64

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
                publicKey = "",
                data = MemberModelData(name, imageBase64),
                mutableData = model
            )
        }
    }

}

@Serializable
data class MemberModelData constructor(
    val name: String,
    val imageBase64: String? = null
)

@Serializable
data class LocationDataModel(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val batteryLevel: Int? = null
)
