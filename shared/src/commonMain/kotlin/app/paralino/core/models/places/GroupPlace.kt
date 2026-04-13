package app.paralino.core.models.places

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.places.detail.PlaceSensitivityDelayType
import app.paralino.core.presentation.places.detail.PlaceSensitivityLocationRefreshType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class GroupPlace constructor(
    val id: String,
    val groupOrOwnerUserId: String,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    val private: Boolean,
    val isInPersonalSpace: Boolean,
    @Contextual
    val data: CryptoResource<GroupPlaceData>,
    val eventKeys: List<String>
) {
    val nameOrNull: String? get() = data.data?.name
    val groupOrSpaceId: GroupOrSpaceId get() = if (isInPersonalSpace) {
        UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
    } else {
        groupOrOwnerUserId.asGroupOrSpaceId()
    }

    val eventSeed: String? = data.data?.eventSeed
    val canSendAlert: Boolean = (data.data?.radiusMeters ?: 0) > 0
    val alertSensitivity: GroupPlaceSensitivityData = data.data?.sensitivity ?: GroupPlaceSensitivityData.default
}

@Serializable
data class GroupPlaceData constructor(
    val name: String,
    val image: ImageModel,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val expiresAt: String,
    val type: String,
    val eventSeed: String? = null,
    val sensitivity: GroupPlaceSensitivityData? = null
)

@Serializable
data class GroupPlaceSensitivityData constructor(
    val delay: String,
    val refresh: String,
    val refreshOnlyInaccurate: Boolean = true,
    val ignoreInaccurate: Boolean = false,
) {
    @Transient val sensitivityDelay = PlaceSensitivityDelayType.fromKey(key = delay) ?: PlaceSensitivityDelayType.default
    @Transient val sensitivityRefresh = PlaceSensitivityLocationRefreshType.fromKey(key = refresh) ?: PlaceSensitivityLocationRefreshType.default

    companion object {
        val default = GroupPlaceSensitivityData(
            delay = PlaceSensitivityDelayType.default.key,
            refresh = PlaceSensitivityLocationRefreshType.default.key,
            refreshOnlyInaccurate = true,
            ignoreInaccurate = false
        )
    }
}

@Serializable
data class GroupPlaceWithSettings constructor(
    val place: GroupPlace,
    val settings: GroupPlaceSettings?
)

data class GroupPlaceGeofenceData constructor(
    val groupId: GroupOrSpaceId,
    val placeId: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int
)
