package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.location.LatLng
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class MapsProviderViewModel(
    private val appPrefs: AppPrefs,
    private val premiumStatusManager: PremiumStatusManager,
    private val userRepository: UserRepository,
    private val appConfigRepo: AppConfigRepository,
    private val userConfigRepo: UserConfigRepository,
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_mapsProvider_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
        coroutineScope.launch {
            try {
                userConfigRepo.refreshMaybeAndGetConfig(cacheTime = 1.minutes)
            } catch (e: Exception) {
            }

            try {
                premiumStatusManager.adjustDeviceSettingsForPlan()
            } catch (e: Exception) {
            }

            _uiState.update { createInitialState() }
        }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.getAndroidMapSDKProviderOrDefault(
            hasGoogleMaps = appConfigRepo.hasGoogleMaps,
            isPremiumPlan = canUserHavePremiumMaps(),
            mapTilerToken = appPrefs.mapTilerToken
        )

        val options = buildList {
            if (appConfigRepo.hasGoogleMaps) {
                add(MapsSDKProvider.GOOGLE)
            }

            add(MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE())
            add(MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM(mapTilerToken = ""))
        }

        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = buildList {
                    add(GenericSettingListItem.Spacer(20.dp, keyId = "top_space"))
                    add(GenericSettingListItem.Subtitle(MR.strings.setting_mapsProvider_info.localizedString()))
                    add(
                        GenericSettingListItem.GroupOptions(
                            defaultId = currentOption.key,
                            keyId = "mapProvider",
                            items = options.map {
                                GroupOptionItem(
                                    key = it,
                                    id = it.key,
                                    title = it.title.localizedString(),
                                    description = it.description.localizedString(),
                                    tag = it.tag?.localizedString(),
                                    isTagWarning = true,
                                    isPremiumFeatureEnabled = if (it is MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM) {
                                        canUserHavePremiumMaps()
                                    } else {
                                        true
                                    },
                                    isSelected = it.key == currentOption.key
                                )
                            }
                        )
                    )

                    if (currentOption is MapsSDKProvider.MAPLIBRE) {
                        add(GenericSettingListItem.Spacer(20.dp, keyId = "premium_space"))
                        add(
                            GenericSettingListItem.Subtitle(
                                content = MR.strings.setting_mapsProvider_style_light_title.localizedString(),
                                bottomPadding = 10.dp
                            )
                        )
                        add(
                            GenericSettingListItem.GroupOptions(
                                defaultId = currentOption.lightStyle.key,
                                keyId = "mapStyleLight",
                                items = currentOption.allLightStyles.map {
                                    GroupOptionItem(
                                        key = it,
                                        id = "light_${it.key}",
                                        title = it.title,
                                        description = null,
                                        isSelected = it == currentOption.lightStyle,
                                        tag = null,
                                        isTagWarning = false,
                                        isPremiumFeatureEnabled = !it.isPremium || canUserHavePremiumMaps(),
                                    )
                                }
                            )
                        )
                        add(
                            GenericSettingListItem.Map(
                                keyId = "lightMap_${currentOption.key}",
                                mapSDKProvider = currentOption,
                                isDarkMode = false,
                                height = 150.dp,
                                // hardcoded to Salzburg, Austria
                                centerLocation = LatLng(
                                    lat = 47.798478,
                                    lng = 13.046638
                                ),
                                zoomLevel = 14f,
                                isVisible = true
                            )
                        )

                        add(GenericSettingListItem.Spacer(30.dp, keyId = "premium_space_dark"))
                        add(
                            GenericSettingListItem.Subtitle(
                                content = MR.strings.setting_mapsProvider_style_dark_title.localizedString(),
                                bottomPadding = 10.dp
                            )
                        )
                        add(
                            GenericSettingListItem.GroupOptions(
                                defaultId = currentOption.darkStyle.key,
                                keyId = "mapStyleDark",
                                items = currentOption.allDarkStyles.map {
                                    GroupOptionItem(
                                        key = it,
                                        id = "dark_${it.key}",
                                        title = it.title,
                                        description = null,
                                        isSelected = it == currentOption.darkStyle,
                                        tag = null,
                                        isTagWarning = false,
                                        isPremiumFeatureEnabled = !it.isPremium || canUserHavePremiumMaps(),
                                    )
                                }
                            )
                        )
                        add(
                            GenericSettingListItem.Map(
                                keyId = "darkMap_${currentOption.key}",
                                mapSDKProvider = currentOption,
                                isDarkMode = true,
                                height = 150.dp,
                                // hardcoded to Salzburg, Austria
                                centerLocation = LatLng(
                                    lat = 47.798478,
                                    lng = 13.046638
                                ),
                                zoomLevel = 14f,
                                isVisible = true
                            )
                        )
                    }

                    add(GenericSettingListItem.Spacer(spacing = 40.dp, keyId = "bottom_space"))
                }
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                if (!event.option.isPremiumFeatureEnabled) {
                    _uiState.update {
                        it.copy(alertDialog = MainAlertDialog.LocationPerformancePremiumInfo)
                    }
                    return
                }

                val currentState = _uiState.value
                val currentItems =
                    (currentState.content as? GenericSettingsScreenStateContent.ListItems)?.items
                        ?: return

                val currentOption = appPrefs.getAndroidMapSDKProviderOrDefault(
                    hasGoogleMaps = appConfigRepo.hasGoogleMaps,
                    isPremiumPlan = canUserHavePremiumMaps(),
                    mapTilerToken = appPrefs.mapTilerToken
                )

                when (event.option.key) {
                    is MapsSDKProvider -> {
                        val newProvider = event.option.key as MapsSDKProvider
                        val finalProvider = when (newProvider) {
                            MapsSDKProvider.GOOGLE -> newProvider

                            is MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE -> {
                                newProvider.copy(
                                    lightStyle = (currentOption as? MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE)?.lightStyle
                                        ?: MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                                    darkStyle = (currentOption as? MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE)?.darkStyle
                                        ?: MapStyle.Dark.OPEN_FREE_MAP_DARK
                                )
                            }

                            is MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM -> {
                                newProvider.copy(
                                    lightStyle = (currentOption as? MapsSDKProvider.MAPLIBRE)?.lightStyle
                                        ?: MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                                    darkStyle = (currentOption as? MapsSDKProvider.MAPLIBRE)?.darkStyle
                                        ?: MapStyle.Dark.OPEN_FREE_MAP_DARK
                                )
                            }
                        }
                        appPrefs.setAndroidMapSDKProvider(finalProvider)
                        _uiState.update { createInitialState() }
                    }

                    is MapStyle -> {
                        val newStyle = event.option.key as MapStyle

                        val updatedProvider = when (currentOption) {
                            MapsSDKProvider.GOOGLE -> null
                            is MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE -> {
                                when (newStyle) {
                                    is MapStyle.Dark -> currentOption.copy(darkStyle = newStyle)
                                    is MapStyle.Light -> currentOption.copy(lightStyle = newStyle)
                                }
                            }

                            is MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM -> {
                                when (newStyle) {
                                    is MapStyle.Dark -> currentOption.copy(darkStyle = newStyle)
                                    is MapStyle.Light -> currentOption.copy(lightStyle = newStyle)
                                }
                            }
                        }

                        if (updatedProvider != null) {
                            appPrefs.setAndroidMapSDKProvider(updatedProvider)
                            _uiState.update { createInitialState() }
                        }
                    }
                }
            }

            is GenericSettingsScreenUiEvent.OnMapInteraction -> {
                _uiState.update { state ->
                    state.copy(
                        content = when (state.content) {
                            is GenericSettingsScreenStateContent.ListItems -> {
                                state.content.copy(isUserScrollEnabled = !event.isTouchingMap)
                            }

                            else -> state.content
                        }
                    )
                }
            }

            else -> {}
        }
    }

    private fun canUserHavePremiumMaps(): Boolean {
        return userRepository.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium && !appPrefs.mapTilerToken.isNullOrBlank()
    }
}

sealed interface MapsSDKProvider {
    val key: String
    val title: dev.icerock.moko.resources.StringResource
    val description: dev.icerock.moko.resources.StringResource
    val tag: dev.icerock.moko.resources.StringResource?
    val hasSatelliteMaps: Boolean
    val isPremium: Boolean

    data object GOOGLE : MapsSDKProvider {
        override val key: String = "google_maps"
        override val title = MR.strings.setting_mapsProvider_googleMaps_title
        override val description = MR.strings.setting_mapsProvider_googleMaps_info
        override val tag = null
        override val hasSatelliteMaps = true
        override val isPremium = false
    }

    sealed interface MAPLIBRE : MapsSDKProvider {
        val lightStyle: MapStyle.Light
        val darkStyle: MapStyle.Dark

        val allLightStyles: List<MapStyle.Light>
        val allDarkStyles: List<MapStyle.Dark>

        fun lightStyleUri(isSatelliteMaps: Boolean): String
        fun darkStyleUri(isSatelliteMaps: Boolean): String

        data class MAPLIBRE_FREE(
            override val lightStyle: MapStyle.Light = MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            override val darkStyle: MapStyle.Dark = MapStyle.Dark.OPEN_FREE_MAP_DARK
        ) : MAPLIBRE {
            override val key: String = "maplibre_free"
            override val title = MR.strings.setting_mapsProvider_mapLibreFree_title
            override val description = MR.strings.setting_mapsProvider_mapLibreFree_info
            override val tag = MR.strings.setting_mapsProvider_mapLibreFree_tag
            override val hasSatelliteMaps = false
            override val isPremium = false

            override val allLightStyles: List<MapStyle.Light> = listOf(
                MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                MapStyle.Light.OPEN_FREE_MAP_BRIGHT,
                MapStyle.Light.OPEN_FREE_MAP_POSITRON
            )

            override val allDarkStyles: List<MapStyle.Dark> = listOf(
                MapStyle.Dark.OPEN_FREE_MAP_DARK,
                MapStyle.Dark.OPEN_FREE_MAP_FIORD,
            )

            override fun lightStyleUri(isSatelliteMaps: Boolean): String {
                return when (lightStyle) {
                    MapStyle.Light.MAPTILER_DATAVIZ_LIGHT,
                    MapStyle.Light.MAPTILER_OUTDOOR_LIGHT,
                    MapStyle.Light.MAPTILER_STREET_LIGHT,
                    MapStyle.Light.OPEN_FREE_MAP_LIBERTY -> "https://tiles.openfreemap.org/styles/liberty"

                    MapStyle.Light.OPEN_FREE_MAP_BRIGHT -> "https://tiles.openfreemap.org/styles/bright"
                    MapStyle.Light.OPEN_FREE_MAP_POSITRON -> "https://tiles.openfreemap.org/styles/positron"
                }
            }

            override fun darkStyleUri(isSatelliteMaps: Boolean): String {
                return when (darkStyle) {
                    MapStyle.Dark.MAPTILER_DATAVIZ_DARK,
                    MapStyle.Dark.MAPTILER_OUTDOOR_DARK,
                    MapStyle.Dark.MAPTILER_STREET_DARK,
                    MapStyle.Dark.OPEN_FREE_MAP_DARK -> "https://tiles.openfreemap.org/styles/dark"

                    MapStyle.Dark.OPEN_FREE_MAP_FIORD -> "https://tiles.openfreemap.org/styles/fiord"
                }
            }
        }

        data class MAPLIBRE_PREMIUM(
            val mapTilerToken: String,
            override val lightStyle: MapStyle.Light = MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            override val darkStyle: MapStyle.Dark = MapStyle.Dark.OPEN_FREE_MAP_DARK
        ) : MAPLIBRE {
            override val key: String = "maplibre_premium"
            override val title = MR.strings.setting_mapsProvider_mapLibrePremium_title
            override val description = MR.strings.setting_mapsProvider_mapLibrePremium_info
            override val tag = null
            override val hasSatelliteMaps = true
            override val isPremium = true

            override val allLightStyles: List<MapStyle.Light> = listOf(
                MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                MapStyle.Light.OPEN_FREE_MAP_BRIGHT,
                MapStyle.Light.OPEN_FREE_MAP_POSITRON,
                MapStyle.Light.MAPTILER_STREET_LIGHT,
                MapStyle.Light.MAPTILER_DATAVIZ_LIGHT,
                MapStyle.Light.MAPTILER_OUTDOOR_LIGHT,
            )

            override val allDarkStyles: List<MapStyle.Dark> = listOf(
                MapStyle.Dark.OPEN_FREE_MAP_DARK,
                MapStyle.Dark.OPEN_FREE_MAP_FIORD,
                MapStyle.Dark.MAPTILER_STREET_DARK,
                MapStyle.Dark.MAPTILER_DATAVIZ_DARK,
                MapStyle.Dark.MAPTILER_OUTDOOR_DARK,
            )

            override fun lightStyleUri(isSatelliteMaps: Boolean): String {
                if (isSatelliteMaps) {
                    return "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${mapTilerToken}"
                }

                return when (lightStyle) {
                    MapStyle.Light.MAPTILER_DATAVIZ_LIGHT -> "https://api.maptiler.com/maps/dataviz-v4/style.json?key=${mapTilerToken}"
                    MapStyle.Light.MAPTILER_OUTDOOR_LIGHT -> "https://api.maptiler.com/maps/outdoor-v4/style.json?key=${mapTilerToken}"
                    MapStyle.Light.MAPTILER_STREET_LIGHT -> "https://api.maptiler.com/maps/streets-v4/style.json?key=${mapTilerToken}"
                    MapStyle.Light.OPEN_FREE_MAP_LIBERTY -> "https://tiles.openfreemap.org/styles/liberty"
                    MapStyle.Light.OPEN_FREE_MAP_BRIGHT -> "https://tiles.openfreemap.org/styles/bright"
                    MapStyle.Light.OPEN_FREE_MAP_POSITRON -> "https://tiles.openfreemap.org/styles/positron"
                }
            }

            override fun darkStyleUri(isSatelliteMaps: Boolean): String {
                if (isSatelliteMaps) {
                    return "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${mapTilerToken}"
                }

                return when (darkStyle) {
                    MapStyle.Dark.MAPTILER_DATAVIZ_DARK -> "https://api.maptiler.com/maps/dataviz-v4-dark/style.json?key=${mapTilerToken}"
                    MapStyle.Dark.MAPTILER_OUTDOOR_DARK -> "https://api.maptiler.com/maps/outdoor-v4-dark/style.json?key=${mapTilerToken}"
                    MapStyle.Dark.MAPTILER_STREET_DARK -> "https://api.maptiler.com/maps/streets-v4-dark/style.json?key=${mapTilerToken}"
                    MapStyle.Dark.OPEN_FREE_MAP_DARK -> "https://tiles.openfreemap.org/styles/dark"
                    MapStyle.Dark.OPEN_FREE_MAP_FIORD -> "https://tiles.openfreemap.org/styles/fiord"
                }
            }
        }
    }

    companion object {
        fun fromKey(
            key: String,
            mapTilerToken: String,
            lightStyle: MapStyle.Light = MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            darkStyle: MapStyle.Dark = MapStyle.Dark.OPEN_FREE_MAP_DARK
        ): MapsSDKProvider? {
            return when (key) {
                GOOGLE.key -> GOOGLE

                MAPLIBRE.MAPLIBRE_FREE().key -> MAPLIBRE.MAPLIBRE_FREE(
                    lightStyle = lightStyle.takeIf { !it.isPremium }
                        ?: MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                    darkStyle = darkStyle.takeIf { !it.isPremium }
                        ?: MapStyle.Dark.OPEN_FREE_MAP_DARK
                )

                MAPLIBRE.MAPLIBRE_PREMIUM(mapTilerToken = mapTilerToken).key -> {
                    MAPLIBRE.MAPLIBRE_PREMIUM(
                        mapTilerToken = mapTilerToken,
                        lightStyle = lightStyle,
                        darkStyle = darkStyle
                    )
                }

                else -> null
            }
        }
    }
}

sealed class MapStyle {
    abstract val key: String
    open val themeAlias: List<String> = emptyList()
    abstract val isPremium: Boolean

    sealed class Light : MapStyle() {
        data object OPEN_FREE_MAP_LIBERTY : Light() {
            override val key: String = "ofm_liberty"
            override val themeAlias: List<String> = listOf("openfreemap")
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_BRIGHT : Light() {
            override val key: String = "ofm_bright"
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_POSITRON : Light() {
            override val key: String = "ofm_positron"
            override val isPremium: Boolean = false
        }

        data object MAPTILER_STREET_LIGHT : Light() {
            override val key: String = "maptiler_street_light"
            override val themeAlias: List<String> = listOf("maptiler")
            override val isPremium: Boolean = true
        }

        data object MAPTILER_OUTDOOR_LIGHT : Light() {
            override val key: String = "maptiler_outdoor_light"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_DATAVIZ_LIGHT : Light() {
            override val key: String = "maptiler_dataviz_light"
            override val isPremium: Boolean = true
        }
    }

    sealed class Dark : MapStyle() {
        data object OPEN_FREE_MAP_DARK : Dark() {
            override val key: String = "ofm_dark"
            override val themeAlias: List<String> = listOf("openfreemap")
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_FIORD : Dark() {
            override val key: String = "ofm_fiord"
            override val isPremium: Boolean = false
        }

        data object MAPTILER_STREET_DARK : Dark() {
            override val key: String = "maptiler_street_dark"
            override val themeAlias: List<String> = listOf("maptiler")
            override val isPremium: Boolean = true
        }

        data object MAPTILER_OUTDOOR_DARK : Dark() {
            override val key: String = "maptiler_outdoor_dark"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_DATAVIZ_DARK : Dark() {
            override val key: String = "maptiler_dataviz_dark"
            override val isPremium: Boolean = true
        }
    }

    val allKeys get() = listOf(key).plus(themeAlias)

    val title: String
        get() = when (this) {
            is MapStyle.Light.OPEN_FREE_MAP_LIBERTY -> "OpenFreeMap Liberty"
            is MapStyle.Light.OPEN_FREE_MAP_BRIGHT -> "OpenFreeMap Bright"
            is MapStyle.Light.OPEN_FREE_MAP_POSITRON -> "OpenFreeMap Positron"
            is MapStyle.Dark.OPEN_FREE_MAP_DARK -> "OpenFreeMap Dark"
            is MapStyle.Dark.OPEN_FREE_MAP_FIORD -> "OpenFreeMap Fiord"
            is MapStyle.Light.MAPTILER_STREET_LIGHT -> "MapTiler Street"
            is MapStyle.Dark.MAPTILER_STREET_DARK -> "MapTiler Street"
            is MapStyle.Light.MAPTILER_OUTDOOR_LIGHT -> "MapTiler Outdoor"
            is MapStyle.Dark.MAPTILER_OUTDOOR_DARK -> "MapTiler Outdoor"
            is MapStyle.Light.MAPTILER_DATAVIZ_LIGHT -> "MapTiler Dataviz"
            is MapStyle.Dark.MAPTILER_DATAVIZ_DARK -> "MapTiler Dataviz"
        }

    companion object {

        val allLightStyles = listOf(
            MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            MapStyle.Light.OPEN_FREE_MAP_BRIGHT,
            MapStyle.Light.OPEN_FREE_MAP_POSITRON,
            MapStyle.Light.MAPTILER_STREET_LIGHT,
            MapStyle.Light.MAPTILER_DATAVIZ_LIGHT,
            MapStyle.Light.MAPTILER_OUTDOOR_LIGHT,
        )

        val allDarkStyles = listOf(
            MapStyle.Dark.OPEN_FREE_MAP_DARK,
            MapStyle.Dark.OPEN_FREE_MAP_FIORD,
            MapStyle.Dark.MAPTILER_STREET_DARK,
            MapStyle.Dark.MAPTILER_DATAVIZ_DARK,
            MapStyle.Dark.MAPTILER_OUTDOOR_DARK,
        )


        fun lightStyleFromKey(key: String): MapStyle.Light {
            return allLightStyles.firstOrNull { it.allKeys.contains(key) }
                ?: Light.OPEN_FREE_MAP_LIBERTY
        }

        fun darkStyleFromKey(key: String): MapStyle.Dark {
            return allDarkStyles.firstOrNull { it.allKeys.contains(key) } ?: Dark.OPEN_FREE_MAP_DARK
        }
    }
}