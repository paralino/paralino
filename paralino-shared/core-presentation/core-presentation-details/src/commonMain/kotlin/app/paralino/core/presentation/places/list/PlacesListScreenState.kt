package app.paralino.core.presentation.places.list

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis

data class PlacesListScreenState constructor(
    val groupId: GroupOrSpaceId,
    val groupImage: ImageModel,
    val groupName: String,
    val placesLimit: Int?,
    val isGroupOwner: Boolean,
    val canShowPlacesLimit: Boolean,
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
        image = place.data.data?.image ?: ImageModel.emoji(Emojis.robot),
        private = place.private,
        lat = place.data.data?.lat,
        lng = place.data.data?.lng,
        radiusMeters = place.data.data?.radiusMeters ?: 0
    )

    fun asFocusedMarker(): FocusedMarkerData {
        return FocusedMarkerData(markerId = this.id, type = MapPinType.PLACE)
    }
}

sealed class PlacesScreenEvent() {
    object AddPlace : PlacesScreenEvent()
    data object OpenPaywall : PlacesScreenEvent()
    data object MissingNotificationPermission : PlacesScreenEvent()
}

sealed interface PlacesScreenUiEvent {
    data object CreatePlace : PlacesScreenUiEvent
    data class PostSampleNotification(val content: String) : PlacesScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : PlacesScreenUiEvent
}

fun PlaceListItemData.makeMapPin(groupOrSpaceId: GroupOrSpaceId): MapPinData {
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
        shouldShowApproximateCircle = true
    )
}