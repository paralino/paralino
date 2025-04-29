package app.paralino.core.presentation.places.detail

import app.paralino.core.models.base.ImageModel

data class PlaceDetailScreenState constructor(
    val groupId: String,
    val placeId: String,
    val groupName: String,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
    val canEdit: Boolean,
    val canHaveAlerts: Boolean,
    val fullLoading: Boolean,
    val isPlaceAlertsSwitchLoading: Boolean,
    val arePlaceAlertsActive: Boolean,
    val settings: List<PlaceSettingsState>
)

data class PlaceSettingsState constructor(
    val groupId: String,
    val placeId: String,
    val memberUserId: String,
    val isCurrentUser: Boolean,
    val name: String,
    val image: ImageModel,
    val notifyEnter: Boolean,
    val notifyExit: Boolean,
    val loadingEnter: Boolean,
    val loadingExit: Boolean
) {
    val loading get() = loadingEnter || loadingExit
}

sealed class PlaceDetailScreenEvent() {
    data class Error(val error: Throwable): PlaceDetailScreenEvent()
    object PlaceDeleted: PlaceDetailScreenEvent()
}