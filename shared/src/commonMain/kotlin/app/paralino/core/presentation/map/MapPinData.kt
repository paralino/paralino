package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
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
        focused = true,
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
        focused = true,
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

    constructor(member: MemberLocation, image: ImageModel?, focused: Boolean) : this(
        groupId = member.groupId,
        markerId = member.id,
        pinType = MapPinType.MEMBER,
        focused = focused,
        active = member.isActive,
        image = image,
        lat = member.locationData.data!!.lat,
        lng = member.locationData.data!!.lng,
        accuracy = member.locationData.data?.accuracy ?: 0f,
        shouldShowApproximateCircle = member.locationData.data?.shouldShowApproximateCircle ?: false
    )
}

enum class MapPinType {
    MEMBER, PLACE
}