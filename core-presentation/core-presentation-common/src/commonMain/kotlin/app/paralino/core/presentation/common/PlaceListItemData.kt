package app.paralino.core.presentation.common

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.utils.Emojis

data class PlaceListItemData constructor(
    val id: String,
    val groupOrSpaceId: GroupOrSpaceId,
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
        name = place.data.data?.name ?: "---",
        image = place.data.data?.image ?: ImageModel.Companion.emoji(Emojis.robot),
        private = place.private,
        lat = place.data.data?.lat,
        lng = place.data.data?.lng,
        radiusMeters = place.data.data?.radiusMeters ?: 0
    )

    fun asFocusedMarker(): FocusedMarkerData {
        return FocusedMarkerData(markerId = this.id, type = MapPinType.PLACE)
    }

    fun makeMapPin(groupOrSpaceId: GroupOrSpaceId): MapPinData {
        val state = this
        return MapPinData(
            groupOrSpaceId = groupOrSpaceId,
            markerId = state.id,
            pinType = MapPinType.PLACE,
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