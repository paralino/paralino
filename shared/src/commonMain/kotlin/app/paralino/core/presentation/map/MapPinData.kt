package app.paralino.core.presentation.map

import androidx.compose.runtime.Stable
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.list.PlaceListItemData

@Stable
data class MapPinData constructor(
    val groupId: String,
    val markerId: String,
    val pinType: MapPinType,
    val markerStyle: MapMarkerStyle,
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
        markerStyle = MapMarkerStyle.CIRCLE,
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
        markerStyle = MapMarkerStyle.CIRCLE,
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
        markerStyle = MapMarkerStyle.CIRCLE,
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
        markerStyle = if(location.locationData.data?.isAccurate == true) MapMarkerStyle.PIN else MapMarkerStyle.CIRCLE,
        focused = false,
        active = location.isPinActive,
        image = image,
        lat = location.locationData.data!!.lat,
        lng = location.locationData.data!!.lng,
        accuracy = location.locationData.data?.accuracy ?: 0f,
        shouldShowApproximateCircle = location.locationData.data?.shouldShowApproximateCircle ?: false
    )

    @Stable
    fun getPinSize(): Int {
        return when (pinType) {
            MapPinType.MEMBER -> {
                when {
                    focused && active -> when (markerStyle) {
                        MapMarkerStyle.PIN -> 110
                        MapMarkerStyle.CIRCLE -> 90
                    }

                    active -> when (markerStyle) {
                        MapMarkerStyle.PIN -> 80
                        MapMarkerStyle.CIRCLE -> 60
                    }

                    else -> when (markerStyle) {
                        MapMarkerStyle.PIN -> 40
                        MapMarkerStyle.CIRCLE -> 40
                    }
                }
            }

            MapPinType.PLACE -> {
                when {
                    focused -> 50
                    else -> 30
                }
            }
        }
    }
}

enum class MapPinType {
    MEMBER, PLACE
}

enum class MapMarkerStyle {
    PIN, CIRCLE
}

data class FocusedMarkerData constructor(
    val markerId: String,
    val type: MapPinType
)

fun MapPinData.asFocusedMarker(): FocusedMarkerData {
    return FocusedMarkerData(markerId = this.markerId, type = this.pinType)
}

fun GroupMemberItemState.asFocusedMarker(): FocusedMarkerData {
    return FocusedMarkerData(markerId = this.keyId, type = MapPinType.MEMBER)
}