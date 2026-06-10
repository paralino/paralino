package app.paralino.core.presentation.common

import app.paralino.core.models.PlaceZoneType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.utils.Emojis

data class PlaceListItemData constructor(
    val id: String,
    val groupOrSpaceId: GroupOrSpaceId,
    val type: PlaceZoneType,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
) {
    constructor(place: GroupPlace) : this(
        id = place.id,
        groupOrSpaceId = place.groupOrSpaceId,
        type = PlaceZoneType.PlaceWithAlert,
        name = place.data.data?.name ?: "---",
        image = place.data.data?.image ?: ImageModel.emoji(Emojis.robot),
        private = place.private,
        lat = place.data.data?.lat,
        lng = place.data.data?.lng,
        radiusMeters = place.data.data?.radiusMeters ?: 0
    )

    constructor(zone: GroupPrivacyZone) : this(
        id = zone.id,
        groupOrSpaceId = zone.groupId.asGroupOrSpaceId(),
        type = PlaceZoneType.PrivacyZone,
        name = zone.data.data?.name ?: "---",
        image = zone.data.data?.image ?: ImageModel.emoji(Emojis.robot),
        private = true,
        lat = zone.data.data?.lat,
        lng = zone.data.data?.lng,
        radiusMeters = zone.data.data?.radiusMeters ?: 0
    )

    fun asFocusedMarker(): FocusedMarkerData {
        return FocusedMarkerData(
            markerId = this.id,
            type = when (type) {
                PlaceZoneType.PlaceWithAlert -> MapPinType.PLACE
                PlaceZoneType.PrivacyZone -> MapPinType.PRIVACY_ZONE
            }
        )
    }

    fun makeMapPin(groupOrSpaceId: GroupOrSpaceId): MapPinData {
        val state = this
        return MapPinData(
            groupOrSpaceId = groupOrSpaceId,
            markerId = state.id,
            pinType = when (type) {
                PlaceZoneType.PlaceWithAlert -> MapPinType.PLACE
                PlaceZoneType.PrivacyZone -> MapPinType.PRIVACY_ZONE
            },
            markerStyle = MapMarkerStyle.CIRCLE,
            focused = false,
            active = true,
            obsolete = false,
            hidden = false,
            title = state.name,
            image = state.image,
            lat = state.lat ?: 0.0,
            lng = state.lng ?: 0.0,
            accuracy = state.radiusMeters.toFloat(),
            isApproximate = false,
            shouldShowApproximateCircle = true
        )
    }
}