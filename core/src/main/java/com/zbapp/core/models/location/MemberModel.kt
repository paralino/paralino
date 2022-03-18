package com.zbapp.core.models.location

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class MemberModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,
    val publicKey: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val shouldRotateKeys: Boolean,
    val data: MemberModelData,
    val mutableData: LocationDataModel? = null
) {

    val name: String get() = data.name
    val imageBase64: String? get() = data.imageBase64

    val isActive: Boolean
        get() {
            val now = Clock.System.now()
            val pinTime = LocalDateTime.parse(
                mutableData?.timeUTC?.takeIf { it.isNotBlank() } ?: return false
            ).toInstant(TimeZone.UTC)
            val activeThreshold = 10.toDuration(DurationUnit.SECONDS)
            val active = pinTime.plus(activeThreshold) > now
            return active
        }

    companion object {
        fun fromSystem(
            userId: String,
            name: String,
            imageBase64: String?,
            model: LocationDataModel
        ): MemberModel {
            return MemberModel(
                id = "",
                groupId = "",
                userId = userId,
                publicKey = "",
                groupKeyVersion = 0,
                memberKeyVersion = 0,
                shouldRotateKeys = false,
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
data class LocationDataModel constructor(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val batteryLevel: Int? = null
)
