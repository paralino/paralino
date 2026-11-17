package app.paralino.core.presentation.places.detail

import androidx.compose.ui.unit.dp
import app.paralino.core.models.data
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.settings.SettingsActionItemType
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsDestination
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenEvent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.places.FalsePlaceAlertMemory
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.WifiAnchorMemory
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.moko.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaceImprovementsViewModel(
    val groupId: String,
    val placeId: String,
    private val placeLocal: PlacesLocalDataSource,
    private val falseAlertMemory: FalsePlaceAlertMemory,
    private val wifiAnchorMemory: WifiAnchorMemory,
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.place_improvements_screen_title.localizedString()
) {

    private class Summary(
        val falseLocations: Int,
        val activeWifi: List<String>,
    )

    private var summary: Summary? = null

    init {
        reload()
    }

    private fun reload() {
        coroutineScope.launch(Dispatchers.IO) {
            val placeData = placeLocal.getPlaceWithoutSettingsOnce(placeId = placeId)?.data?.data
            val wifi = placeData?.let {
                wifiAnchorMemory.wifiForPlace(
                    placeCenter = LatLng(lat = it.lat, lng = it.lng),
                    radiusMeters = it.radiusMeters.toFloat()
                )
            }.orEmpty()
            summary = Summary(
                falseLocations = falseAlertMemory.falseLocations(placeId = placeId).size,
                activeWifi = wifi
                    .filter { it.status == WifiAnchorMemory.WifiStatus.ACTIVE }
                    .mapNotNull { it.ssid }
                    .distinct()
            )
            _uiState.update { createInitialState() }
        }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val summary = summary ?: return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.Loading
        )

        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.SettingsActionItem(
                        text = MR.strings.place_improvements_falseLocations_title.localizedString(),
                        icon = CoreAppIcons.NotListedLocation,
                        type = SettingsActionItemType.Navigation,
                        isVisible = true,
                        tag = if (summary.falseLocations > 0) {
                            MR.strings.place_improvements_falseLocations_summary.localizedString(summary.falseLocations)
                        } else {
                            MR.strings.place_improvements_falseLocations_summary_none.localizedString()
                        },
                        description = null,
                        keyId = KEY_FALSE_LOCATIONS
                    ),
                    GenericSettingListItem.SettingsActionItem(
                        text = MR.strings.place_improvements_wifi_title.localizedString(),
                        icon = CoreAppIcons.SignalWifi3,
                        type = SettingsActionItemType.Navigation,
                        isVisible = true,
                        tag = if (summary.activeWifi.isNotEmpty()) {
                            MR.strings.place_improvements_wifi_summary_active.localizedString(summary.activeWifi.joinToString(", "))
                        } else {
                            MR.strings.place_improvements_wifi_summary_none.localizedString()
                        },
                        description = null,
                        keyId = KEY_WIFI
                    ),
                    GenericSettingListItem.SettingsActionItem(
                        text = MR.strings.place_sensitivity_screen_title.localizedString(),
                        icon = CoreAppIcons.ShareLocation,
                        type = SettingsActionItemType.Navigation,
                        isVisible = true,
                        tag = null,
                        description = null,
                        keyId = KEY_SENSITIVITY
                    ),
                )
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        if (event !is GenericSettingsScreenUiEvent.SelectActionItem) {
            return
        }
        val destination = when (event.item.keyId) {
            KEY_FALSE_LOCATIONS -> GenericSettingsDestination.FalseLocations(groupId = groupId, placeId = placeId)
            KEY_WIFI -> GenericSettingsDestination.WifiNetworks(placeId = placeId)
            KEY_SENSITIVITY -> GenericSettingsDestination.PlaceSensitivity(groupId = groupId, placeId = placeId)
            else -> return
        }
        coroutineScope.launch {
            _eventChannel.send(GenericSettingsScreenEvent.OpenScreen(destination))
        }
    }

    private companion object {
        const val KEY_FALSE_LOCATIONS = "false_locations"
        const val KEY_WIFI = "wifi"
        const val KEY_SENSITIVITY = "sensitivity"
    }
}
