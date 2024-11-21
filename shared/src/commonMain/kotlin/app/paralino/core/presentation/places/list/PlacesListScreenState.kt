package app.paralino.core.presentation.places.list

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis

data class PlacesListScreenState constructor(
    val groupId: String,
    val groupImage: ImageModel,
    val groupName: String,
    val placesLimit: Int,
    val isGroupOwner: Boolean,
    val canShowPlacesLimit: Boolean,
    val shouldShowPlanBanner: Boolean,
    val places: List<PlaceListItemData>,
    val error: ConditionErrorType.PermissionNotifications?,
    val alertDialog: MainAlertDialog?
) {
    val markers: List<MapPinData> = places.filter { it.lat != null && it.lng != null }
        .map { MapPinData(groupId, it) }
}

data class PlaceListItemData constructor(
    val id: String,
    val groupId: String,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
) {
    constructor(place: GroupPlace) : this(
        id = place.id,
        groupId = place.groupId,
        name = place.data.data?.name ?: "---",
        image = place.data.data?.image ?: ImageModel.emoji(Emojis.robot),
        private = place.private,
        lat = place.data.data?.lat,
        lng = place.data.data?.lng,
        radiusMeters = place.data.data?.radiusMeters ?: 0
    )
}

sealed class PlacesScreenEvent() {
    data class Error(val error: Throwable) : PlacesScreenEvent()
    object AddPlace : PlacesScreenEvent()
    data class GroupPlacesLimitReached(val limit: Int) : PlacesScreenEvent()
    data object MissingNotificationPermission : PlacesScreenEvent()
}

sealed interface PlacesScreenUiEvent {
    data object CreatePlace : PlacesScreenUiEvent
    data class PostSampleNotification(val content: String) : PlacesScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : PlacesScreenUiEvent
}