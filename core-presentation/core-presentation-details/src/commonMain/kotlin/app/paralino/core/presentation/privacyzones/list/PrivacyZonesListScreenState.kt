package app.paralino.core.presentation.privacyzones.list

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.map.MapPinData

data class PrivacyZonesListScreenState constructor(
    val groupId: GroupOrSpaceId,
    val groupImage: ImageModel,
    val groupName: String,
    val privacyZonesLimit: Int?,
    val isGroupOwner: Boolean,
    val shouldShowPlanBanner: Boolean,
    val zones: List<PlaceListItemData>,
    val androidMapSDKProvider: MapsSDKProvider?,
    val alertDialog: MainAlertDialog?
) {
    val markers: List<MapPinData> = zones.filter { it.lat != null && it.lng != null }
        .map { it.makeMapPin(groupOrSpaceId = groupId) }

    val privacyZonesLimitReached: Boolean = privacyZonesLimit != null && privacyZonesLimit <= zones.size
    val isOverLimit: Boolean = privacyZonesLimit != null && zones.size > privacyZonesLimit
}

sealed class PrivacyZonesScreenEvent() {
    object AddPrivacyZone : PrivacyZonesScreenEvent()
    data object OpenPaywall : PrivacyZonesScreenEvent()
}

sealed interface PrivacyZonesScreenUiEvent {
    data object CreatePrivacyZone : PrivacyZonesScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : PrivacyZonesScreenUiEvent
}
