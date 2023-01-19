package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.list.PlaceListItemData

data class MapPinData constructor(
    val groupId: String,
    val markerId: String,
    val pinType: MapPinType,
    val focused: Boolean,
    val active: Boolean,
    val image: ImageModel?,
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val shouldShowApproximateCircle: Boolean
) {

    constructor(state: CreatePlaceScreenState) : this(
        groupId = state.groupId,
        markerId = "place_marker",
        pinType = MapPinType.PLACE,
        focused = false,
        active = true,
        image = state.image,
        lat = state.lat ?: 0.0,
        lng = state.lng ?: 0.0,
        accuracy = state.radiusMeters.toFloat(),
        shouldShowApproximateCircle = true
    )

    constructor(state: PlaceDetailScreenState) : this(
        groupId = state.groupId,
        markerId = state.placeId,
        pinType = MapPinType.PLACE,
        focused = false,
        active = true,
        image = state.image,
        lat = state.lat ?: 0.0,
        lng = state.lng ?: 0.0,
        accuracy = state.radiusMeters.toFloat(),
        shouldShowApproximateCircle = true
    )

    constructor(groupId: String, state: PlaceListItemData) : this(
        groupId = groupId,
        markerId = state.id,
        pinType = MapPinType.PLACE,
        focused = false,
        active = true,
        image = state.image,
        lat = state.lat ?: 0.0,
        lng = state.lng ?: 0.0,
        accuracy = state.radiusMeters.toFloat(),
        shouldShowApproximateCircle = true
    )

    constructor(location: MemberLocation, image: ImageModel?) : this(
        groupId = location.groupId,
        markerId = location.id,
        pinType = MapPinType.MEMBER,
        focused = false,
        active = location.isActive,
        image = image,
        lat = location.locationData.data!!.lat,
        lng = location.locationData.data!!.lng,
        accuracy = location.locationData.data?.accuracy ?: 0f,
        shouldShowApproximateCircle = location.locationData.data?.shouldShowApproximateCircle ?: false
    )
}

enum class MapPinType {
    MEMBER, PLACE
}

data class FocusedMarkerData constructor(
    val markerId: String,
    val type: MapPinType
)

fun MapPinData.asFocusedMarker(): FocusedMarkerData {
    return FocusedMarkerData(markerId = this.markerId, type = this.pinType)
}

fun GroupMemberItemStateWithOneLocation.asFocusedMarker(): FocusedMarkerData {
    return FocusedMarkerData(markerId = this.keyId, type = MapPinType.MEMBER)
}