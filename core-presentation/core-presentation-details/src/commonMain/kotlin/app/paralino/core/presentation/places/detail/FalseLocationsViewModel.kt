package app.paralino.core.presentation.places.detail

import androidx.compose.ui.unit.dp
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.places.FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation
import app.paralino.core.repo.places.FalsePlaceAlertMemory
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.moko.MR
import app.paralino.core.resources.moko.place_improvements_away
import app.paralino.core.resources.moko.place_improvements_falseEnter
import app.paralino.core.resources.moko.place_improvements_falseExit
import app.paralino.core.resources.moko.place_improvements_falseLocations_empty
import app.paralino.core.resources.moko.place_improvements_falseLocations_info
import app.paralino.core.resources.moko.place_improvements_falseLocations_title
import app.paralino.core.resources.moko.place_improvements_forgetFalseLocation_message
import app.paralino.core.resources.moko.place_improvements_forgetFalseLocation_title
import app.paralino.core.resources.moko.place_improvements_info
import app.paralino.core.resources.moko.place_improvements_seenTimes
import app.paralino.core.resources.moko.place_improvements_tag_filtering
import app.paralino.core.resources.moko.place_improvements_tag_watching
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FalseLocationsViewModel(
    val groupId: String,
    val placeId: String,
    private val placeLocal: PlacesLocalDataSource,
    private val falseAlertMemory: FalsePlaceAlertMemory,
    private val premiumStatusManager: PremiumStatusManager,
    private val geofenceManager: IGeofenceManager,
    private val valueFormatter: IValueFormatter,
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.place_improvements_falseLocations_title.localizedString()
) {

    private data class FalseLocationScreeData(
        val place: GroupPlace,
        val falseLocations: List<FalsePlaceAlertLocation>
    )

    private val internalState = MutableStateFlow<FalseLocationScreeData?>(null)

    init {
        reload()
    }

    private fun reload() {
        coroutineScope.launch(Dispatchers.IO) {
            val place = placeLocal.getPlaceWithoutSettingsOnce(placeId = placeId) ?: return@launch
            internalState.update {
                FalseLocationScreeData(
                    place = place,
                    falseLocations = falseAlertMemory.falseLocations(placeId = placeId),
                )
            }
            _uiState.update { createInitialState() }
        }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val data = internalState.value
        val placeData = data?.place?.data?.data
        if (data == null || placeData == null) {
            return GenericSettingsScreenState(
                screenTitle = initialScreenTitle,
                alertDialog = null,
                content = GenericSettingsScreenStateContent.Loading
            )
        }

        val placeCenter = LatLng(lat = placeData.lat, lng = placeData.lng)

        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = _uiState.value.alertDialog,
            content = GenericSettingsScreenStateContent.ListItems(
                items = buildList {
                    add(GenericSettingListItem.Spacer(20.dp, keyId = "top_space"))
                    add(
                        GenericSettingListItem.Subtitle(
                            content = MR.strings.place_improvements_info.localizedString(),
                            bottomPadding = 0.dp
                        )
                    )

                    add(GenericSettingListItem.Spacer(12.dp, keyId = "info_space"))
                    add(GenericSettingListItem.Subtitle(MR.strings.place_improvements_falseLocations_info.localizedString()))
                    if (data.falseLocations.isEmpty()) {
                        add(
                            GenericSettingListItem.Notice(
                                content = MR.strings.place_improvements_falseLocations_empty.localizedString(),
                                icon = CoreAppIcons.InfoCircle
                            )
                        )
                    } else {
                        val placeMarker = listOf(placePin(data.place))
                        val falseLocationMarkers = data.falseLocations.mapIndexed { index, it ->
                            falseLocationPin(data.place, index, it)
                        }

                        add(
                            GenericSettingListItem.Map(
                                keyId = "false_locations_map",
                                mapSDKProvider = premiumStatusManager.defaultMapProvider(),
                                height = 240.dp,
                                isDarkMode = null,
                                centerLocation = placeCenter,
                                zoomLevel = 13f,
                                isVisible = true,
                                markers = placeMarker + falseLocationMarkers
                            )
                        )
                        add(GenericSettingListItem.Spacer(12.dp, keyId = "false_locations_space"))
                        data.falseLocations.forEachIndexed { index, location ->
                            add(falseLocationItem(location = location, placeCenter = placeCenter))
                        }
                    }

                    add(GenericSettingListItem.Spacer(20.dp, keyId = "bottom_space"))
                }
            )
        )
    }

    private fun falseLocationItem(
        location: FalsePlaceAlertLocation,
        placeCenter: LatLng
    ): GenericSettingListItem.DeletableItem {
        val title = falseLocationTitle(location)
        val distance = geofenceManager.distanceBetweenPoints(
            lat1 = location.lat,
            lng1 = location.lng,
            lat2 = placeCenter.lat,
            lng2 = placeCenter.lng
        )

        return GenericSettingListItem.DeletableItem(
            title = title,
            icon = CoreAppIcons.NotListedLocation,
            tag = if (location.hits >= 2) {
                MR.strings.place_improvements_tag_filtering.localizedString()
            } else {
                MR.strings.place_improvements_tag_watching.localizedString()
            },
            isTagActive = location.hits >= 2,
            description = listOfNotNull(
                MR.strings.place_improvements_away.localizedString(
                    valueFormatter.getDistanceSmall(
                        meters = distance.toDouble(),
                        distanceType = DistanceType.DISTANCE
                    )
                ),
                MR.strings.place_improvements_seenTimes.localizedString(location.hits),
                valueFormatter.lastSeenText(location.lastSeenMillis)
            ).joinToString(" | "),
            keyId = location.id()
        )
    }

    private fun falseLocationTitle(location: FalsePlaceAlertLocation): String {
        return if (location.isEnter) {
            MR.strings.place_improvements_falseEnter.localizedString()
        } else {
            MR.strings.place_improvements_falseExit.localizedString()
        }
    }

    private fun placePin(place: GroupPlace): MapPinData {
        val placeData = place.data.data
        return MapPinData(
            groupOrSpaceId = place.groupOrSpaceId,
            markerId = place.id,
            pinType = MapPinType.PLACE,
            markerStyle = MapMarkerStyle.CIRCLE,
            focused = false,
            active = true,
            obsolete = false,
            hidden = false,
            title = placeData?.name,
            image = placeData?.image ?: ImageModel.emoji(Emojis.home),
            lat = placeData?.lat ?: 0.0,
            lng = placeData?.lng ?: 0.0,
            accuracy = placeData?.radiusMeters?.toFloat() ?: 0f,
            isApproximate = false,
            shouldShowApproximateCircle = true
        )
    }

    private fun falseLocationPin(
        place: GroupPlace,
        index: Int,
        location: FalsePlaceAlertLocation
    ): MapPinData {
        return MapPinData(
            groupOrSpaceId = place.groupOrSpaceId,
            markerId = "false_location_$index",
            pinType = MapPinType.PLACE,
            markerStyle = MapMarkerStyle.CIRCLE,
            focused = false,
            active = location.hits >= 2,
            obsolete = false,
            hidden = false,
            title = falseLocationTitle(location),
            image = ImageModel.emoji(Emojis.lock),
            lat = location.lat,
            lng = location.lng,
            // area around false location that is filtered
            accuracy = 150f,
            isApproximate = false,
            shouldShowApproximateCircle = true
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DeleteItem -> {
                _uiState.update {
                    it.copy(
                        alertDialog = MainAlertDialog.ForgetLearnedLocationItem(
                            itemId = event.item.keyId,
                            title = MR.strings.place_improvements_forgetFalseLocation_title.localizedString(),
                            message = MR.strings.place_improvements_forgetFalseLocation_message.localizedString(),
                        )
                    )
                }
            }

            is GenericSettingsScreenUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }
                when (val dialog = event.action.dialog) {
                    is MainAlertDialog.ForgetLearnedLocationItem -> {
                        if (event.action.isOk) {
                            coroutineScope.launch {
                                internalState.value?.falseLocations
                                    ?.firstOrNull { it.id() == dialog.itemId }
                                    ?.let { location ->
                                        falseAlertMemory.forget(
                                            placeId = placeId,
                                            location = location
                                        )
                                    }
                                reload()
                            }
                        }
                    }

                    else -> {}
                }
            }

            else -> {}
        }
    }

    private fun FalsePlaceAlertLocation.id(): String {
        return "$lat-$lng-$isEnter"
    }
}
