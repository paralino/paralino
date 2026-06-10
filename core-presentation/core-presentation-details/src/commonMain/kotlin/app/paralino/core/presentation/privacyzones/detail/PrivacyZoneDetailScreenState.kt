package app.paralino.core.presentation.privacyzones.detail

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType

data class PrivacyZoneDetailScreenState constructor(
    val groupOrSpaceId: GroupOrSpaceId,
    val zoneId: String,
    val groupName: String,
    val name: String,
    val image: ImageModel,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
    val expiresAt: String,
    val canEdit: Boolean,
    val fullLoading: Boolean,
    val androidMapSDKProvider: MapsSDKProvider?,
    val alertDialog: MainAlertDialog? = null,
)

sealed class PrivacyZoneDetailScreenEvent() {
    object ZoneDeleted : PrivacyZoneDetailScreenEvent()
}

sealed interface PrivacyZoneDetailScreenUiEvent {
    data object DeleteZone : PrivacyZoneDetailScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : PrivacyZoneDetailScreenUiEvent
}

fun PrivacyZoneDetailScreenState.makeMapPin(): MapPinData {
    val state = this
    return MapPinData(
        groupOrSpaceId = state.groupOrSpaceId,
        markerId = state.zoneId,
        pinType = MapPinType.PRIVACY_ZONE,
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
