package app.paralino.core.presentation.places.detail

import androidx.compose.ui.unit.dp
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.WifiAnchorMemory
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.moko.MR
import app.paralino.core.resources.moko.place_improvements_forgetWifi_message
import app.paralino.core.resources.moko.place_improvements_forgetWifi_title
import app.paralino.core.resources.moko.place_improvements_info
import app.paralino.core.resources.moko.place_improvements_lastSeen
import app.paralino.core.resources.moko.place_improvements_wifi_accessPoints
import app.paralino.core.resources.moko.place_improvements_wifi_confidence
import app.paralino.core.resources.moko.place_improvements_wifi_empty
import app.paralino.core.resources.moko.place_improvements_wifi_hidden
import app.paralino.core.resources.moko.place_improvements_wifi_info
import app.paralino.core.resources.moko.place_improvements_wifi_status_active
import app.paralino.core.resources.moko.place_improvements_wifi_status_learning
import app.paralino.core.resources.moko.place_improvements_wifi_status_unstable
import app.paralino.core.resources.moko.place_improvements_wifi_title
import app.paralino.core.resources.moko.wifi_networks_empty
import app.paralino.core.resources.moko.wifi_networks_info
import app.paralino.core.resources.moko.wifi_networks_notUsed
import app.paralino.core.resources.moko.wifi_networks_screen_title
import app.paralino.core.resources.moko.wifi_networks_usedFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

class WifiNetworksViewModel(
    val placeId: String?,
    private val placeLocal: PlacesLocalDataSource,
    private val wifiAnchorMemory: WifiAnchorMemory,
    private val geofenceManager: IGeofenceManager,
    private val valueFormatter: IValueFormatter,
) : BaseGenericSettingViewModel(
    initialScreenTitle = if (placeId != null) {
        MR.strings.place_improvements_wifi_title.localizedString()
    } else {
        MR.strings.wifi_networks_screen_title.localizedString()
    }
) {

    private data class WiFiNetworksScreeData(
        val wifis: List<WiFiNetworksPoint>,
        val places: List<GroupPlace>,
    )

    private data class WiFiNetworksPoint(
        val key: String,
        val accessPoints: List<WifiAnchorMemory.LearnedWifi>
    ) {
        val name: String = accessPoints
            .firstOrNull()
            ?.ssid
            ?: MR.strings.place_improvements_wifi_hidden.localizedString(key.takeLast(5))
    }

    private val internalState = MutableStateFlow<WiFiNetworksScreeData?>(null)

    init {
        reload()
    }

    private fun reload() {
        coroutineScope.launch(Dispatchers.IO) {
            val places = if (placeId != null) {
                listOfNotNull(placeLocal.getPlaceWithoutSettingsOnce(placeId = placeId))
            } else {
                placeLocal.getAllPlaces().first()
            }
            val allWifi = wifiAnchorMemory.allWifi()
            internalState.update {
                WiFiNetworksScreeData(
                    wifis = if (placeId != null) {
                        allWifi.filter { wifi -> places.any { it.contains(wifi) } }
                    } else {
                        allWifi
                    }
                        .groupBy { it.ssid ?: it.bssid }
                        .map {
                            WiFiNetworksPoint(
                                key = it.key,
                                accessPoints = it.value
                            )
                        },
                    places = places
                )
            }
            _uiState.update { createInitialState() }
        }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val data = internalState.value ?: return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.Loading
        )

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
                    add(
                        GenericSettingListItem.Subtitle(
                            if (placeId != null) {
                                MR.strings.place_improvements_wifi_info.localizedString()
                            } else {
                                MR.strings.wifi_networks_info.localizedString()
                            }
                        )
                    )

                    if (data.wifis.isEmpty()) {
                        add(
                            GenericSettingListItem.Notice(
                                content = if (placeId != null) {
                                    MR.strings.place_improvements_wifi_empty.localizedString()
                                } else {
                                    MR.strings.wifi_networks_empty.localizedString()
                                },
                                icon = CoreAppIcons.InfoCircle
                            )
                        )
                    } else {
                        // same network name usually means multiple access points or bands of the same network
                        data.wifis.forEach { entry ->
                            add(wifiItem(entry = entry, places = data.places))
                        }
                    }
                    add(GenericSettingListItem.Spacer(20.dp, keyId = "bottom_space"))
                }
            )
        )
    }

    private fun wifiItem(
        entry: WiFiNetworksPoint,
        places: List<GroupPlace>,
    ): GenericSettingListItem.DeletableItem {
        val statuses = entry.accessPoints.map { it.status }
        val status = when {
            WifiAnchorMemory.WifiStatus.ACTIVE in statuses -> MR.strings.place_improvements_wifi_status_active
            WifiAnchorMemory.WifiStatus.LEARNING in statuses -> MR.strings.place_improvements_wifi_status_learning
            else -> MR.strings.place_improvements_wifi_status_unstable
        }

        val usedFor = if (placeId == null) {
            val names = places
                .filter { place -> entry.accessPoints.any { place.contains(it) } }
                .mapNotNull { it.data.data?.name }
                .distinct()
            if (names.isEmpty()) {
                MR.strings.wifi_networks_notUsed.localizedString()
            } else {
                MR.strings.wifi_networks_usedFor.localizedString(names.joinToString(", "))
            }
        } else {
            null
        }

        return GenericSettingListItem.DeletableItem(
            title = entry.name,
            icon = CoreAppIcons.SignalWifi3,
            tag = status.localizedString(),
            isTagActive = WifiAnchorMemory.WifiStatus.ACTIVE in statuses,
            description = listOfNotNull(
                usedFor,
                MR.strings.place_improvements_wifi_confidence.localizedString(
                    "${(entry.accessPoints.maxOf { it.confidence } * 100).roundToInt()}%"
                ),
                if (entry.accessPoints.size > 1) {
                    MR.strings.place_improvements_wifi_accessPoints.localizedString(entry.accessPoints.size)
                } else {
                    null
                },
                valueFormatter.lastSeenText(entry.accessPoints.maxOf { it.lastSeenMillis })
            ).joinToString(" | "),
            keyId = entry.key
        )
    }

    private fun GroupPlace.contains(wifi: WifiAnchorMemory.LearnedWifi): Boolean {
        val placeData = data.data ?: return false
        return geofenceManager.distanceBetweenPoints(
            lat1 = wifi.location.lat,
            lng1 = wifi.location.lng,
            lat2 = placeData.lat,
            lng2 = placeData.lng
        ) <= placeData.radiusMeters
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DeleteItem -> {
                val name = internalState.value?.wifis
                    ?.firstOrNull { it.key == event.item.keyId }
                    ?.name
                    ?: return
                _uiState.update {
                    it.copy(
                        alertDialog = MainAlertDialog.ForgetLearnedLocationItem(
                            itemId = event.item.keyId,
                            title = MR.strings.place_improvements_forgetWifi_title.localizedString(),
                            message = MR.strings.place_improvements_forgetWifi_message.localizedString(
                                name
                            )
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
                                internalState.value?.wifis
                                    ?.firstOrNull { it.key == dialog.itemId }
                                    ?.accessPoints
                                    ?.map { it.bssid }
                                    ?.let { bssids ->
                                        wifiAnchorMemory.forget(bssids = bssids)
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
}

internal fun IValueFormatter.lastSeenText(timeMillis: Long): String? {
    val time = formatRelativeDateTime(
        timeUTC = Instant.fromEpochMilliseconds(timeMillis).toLocalDateTime(TimeZone.UTC)
    ) ?: return null
    return MR.strings.place_improvements_lastSeen.localizedString(time)
}
