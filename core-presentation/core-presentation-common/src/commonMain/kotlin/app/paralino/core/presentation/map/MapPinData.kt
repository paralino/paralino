package app.paralino.core.presentation.map

import androidx.compose.runtime.Stable
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationHistoryPointLocationModel
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.models.location.MemberLocation

@Stable
data class MapPinData constructor(
    val groupOrSpaceId: GroupOrSpaceId,
    val markerId: String,
    val pinType: MapPinType,
    val markerStyle: MapMarkerStyle,
    val focused: Boolean,
    val active: Boolean,
    val obsolete: Boolean,
    val hidden: Boolean,
    val image: ImageModel?,
    val lat: Double,
    val lng: Double,
    val isApproximate: Boolean,
    val title: String? = null,
    val timeInfo: String? = null,
    val accuracy: Float = 0f,
    val shouldShowApproximateCircle: Boolean
) {
    constructor(
        location: MemberLocation,
        name: String?,
        image: ImageModel?,
        timeInfo: String?
    ) : this(
        groupOrSpaceId = location.groupOrSpaceId,
        markerId = location.id,
        pinType = MapPinType.MEMBER,
        markerStyle = if(location.locationData.data?.isAccurateForPinType == true) MapMarkerStyle.PIN else MapMarkerStyle.CIRCLE,
        focused = false,
        active = location.isPinActive,
        obsolete = location.isLocationAvailableButOld,
        hidden = location.isLocationAvailableButOld,
        title = name,
        image = image,
        timeInfo = timeInfo,
        lat = location.locationData.data!!.lat,
        lng = location.locationData.data!!.lng,
        accuracy = location.locationData.data?.accuracy ?: 0f,
        isApproximate = location.locationData.data?.isApproximate == true,
        shouldShowApproximateCircle = location.locationData.data?.shouldShowApproximateCircle ?: false
    )

    constructor(
        id: String,
        groupId: GroupOrSpaceId,
        location: LocationHistoryPointLocationModel,
        name: String?,
        image: ImageModel?,
        timeInfo: String?
    ) : this(
        groupOrSpaceId = groupId,
        markerId = id,
        pinType = MapPinType.MEMBER,
        markerStyle = if(location.acc < 100) MapMarkerStyle.PIN else MapMarkerStyle.CIRCLE,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = name,
        image = image,
        timeInfo = timeInfo,
        lat = location.lat,
        lng = location.lng,
        accuracy = location.acc,
        isApproximate = false,
        shouldShowApproximateCircle = location.acc > 5
    )

    constructor(
        id: String,
        groupId: GroupOrSpaceId,
        cluster: LocationHistoryPointState.Cluster,
        name: String?,
        image: ImageModel?,
        timeInfo: String?
    ) : this(
        groupOrSpaceId = groupId,
        markerId = id,
        pinType = MapPinType.MEMBER,
        markerStyle = MapMarkerStyle.CIRCLE,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = name,
        image = image,
        timeInfo = timeInfo,
        lat = cluster.lat,
        lng = cluster.lng,
        accuracy = cluster.accuracy,
        isApproximate = false,
        shouldShowApproximateCircle = cluster.accuracy > 5
    )

    @Stable
    fun getPinSize(): Int {
        return when (pinType) {
            MapPinType.MEMBER -> {
                when {
                    focused -> when (markerStyle) {
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

    fun asFocusedMarker(): FocusedMarkerData {
        return FocusedMarkerData(markerId = this.markerId, type = this.pinType)
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