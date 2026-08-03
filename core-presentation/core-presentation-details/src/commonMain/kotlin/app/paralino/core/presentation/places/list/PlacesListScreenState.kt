package app.paralino.core.presentation.places.list

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.utils.ConditionErrorType

data class PlacesListScreenState constructor(
    val groupId: GroupOrSpaceId,
    val groupImage: ImageModel,
    val groupName: String,
    val placesLimit: Int?,
    val isGroupOwner: Boolean,
    val canShowPlacesLimit: Boolean,
    val canShowPlaceAddButton: Boolean,
    val shouldShowPlanBanner: Boolean,
    val places: List<PlaceListItemData>,
    val error: ConditionErrorType.PermissionNotifications?,
    val androidMapSDKProvider: MapsSDKProvider?,
    val alertDialog: MainAlertDialog?
) {
    val markers: List<MapPinData> = places.filter { it.lat != null && it.lng != null }
        .map { it.makeMapPin(groupOrSpaceId = groupId) }

    val placesLimitReached: Boolean = placesLimit != null && placesLimit <= places.size
    val isOverLimit: Boolean = placesLimit != null && places.size > placesLimit
    val isPersonalSpace = groupId.isPersonalSpace
}

sealed class PlacesScreenEvent() {
    object AddPlace : PlacesScreenEvent()
    data object OpenPaywall : PlacesScreenEvent()
    data object SkipOnboarding : PlacesScreenEvent()
    data object MissingNotificationPermission : PlacesScreenEvent()
}

sealed interface PlacesScreenUiEvent {
    data object CreatePlace : PlacesScreenUiEvent
    data object OnNextClick : PlacesScreenUiEvent
    data class PostSampleNotification(val content: String) : PlacesScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : PlacesScreenUiEvent
}