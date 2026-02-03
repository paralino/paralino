package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapsProviderViewModel(
    private val appPrefs: AppPrefs,
    private val premiumStatusManager: PremiumStatusManager,
    private val userRepository: UserRepository,
    private val appConfigRepository: AppConfigRepository
): BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_mapsProvider_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
        coroutineScope.launch {
            try {
                premiumStatusManager.adjustDeviceSettingsForPlan()
            } catch (e: Exception) {
            }
        }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.getAndroidMapSDKProviderOrDefault(
            hasGoogleMaps = appConfigRepository.hasGoogleMaps,
            isPremiumPlan = canUserHavePremiumMaps()
        )
        val options = buildList {
            if (appConfigRepository.hasGoogleMaps) {
                add(MapsSDKProvider.GOOGLE)
            }

            add(MapsSDKProvider.MAPLIBRE_FREE)
            add(MapsSDKProvider.MAPLIBRE_PREMIUM())
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
                                    isPremiumFeatureEnabled = if (it is MapsSDKProvider.MAPLIBRE_PREMIUM) {
                                        canUserHavePremiumMaps()
                                    } else {
                                        true
                                    },
                                    isSelected = it.key == currentOption.key
                                )
                            }
                        )
                    )

                    if (currentOption is MapsSDKProvider.MAPLIBRE_PREMIUM) {
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
                                items = MapsSDKProvider.MapStyle.entries.map {
                                    GroupOptionItem(
                                        key = it,
                                        id = "light_${it.key}",
                                        title = it.title.localizedString(),
                                        description = null,
                                        isSelected = it == currentOption.lightStyle,
                                        tag = null,
                                        isTagWarning = false,
                                        isPremiumFeatureEnabled = true,
                                    )
                                }
                            )
                        )

                        add(GenericSettingListItem.Spacer(20.dp, keyId = "premium_space_dark"))
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
                                items = MapsSDKProvider.MapStyle.entries.map {
                                    GroupOptionItem(
                                        key = it,
                                        id = "dark_${it.key}",
                                        title = it.title.localizedString(),
                                        description = null,
                                        isSelected = it == currentOption.darkStyle,
                                        tag = null,
                                        isTagWarning = false,
                                        isPremiumFeatureEnabled = true,
                                    )
                                }
                            )
                        )
                    }
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
                val currentItems = (currentState.content as? GenericSettingsScreenStateContent.ListItems)?.items ?: return
                
                val currentOption = appPrefs.getAndroidMapSDKProviderOrDefault(
                    hasGoogleMaps = appConfigRepository.hasGoogleMaps,
                    isPremiumPlan = canUserHavePremiumMaps()
                )

                when (event.option.key) {
                    is MapsSDKProvider -> {
                        val newProvider = event.option.key as MapsSDKProvider
                        val finalProvider = if (newProvider is MapsSDKProvider.MAPLIBRE_PREMIUM) {
                            newProvider.copy(
                                lightStyle = (currentOption as? MapsSDKProvider.MAPLIBRE_PREMIUM)?.lightStyle ?: MapsSDKProvider.MapStyle.OPEN_FREE_MAP,
                                darkStyle = (currentOption as? MapsSDKProvider.MAPLIBRE_PREMIUM)?.darkStyle ?: MapsSDKProvider.MapStyle.OPEN_FREE_MAP
                            )
                        } else {
                            newProvider
                        }
                        appPrefs.setAndroidMapSDKProvider(finalProvider)
                        _uiState.update { createInitialState() }
                    }

                    is MapsSDKProvider.MapStyle -> {
                        val newStyle = event.option.key as MapsSDKProvider.MapStyle
                        if (currentOption is MapsSDKProvider.MAPLIBRE_PREMIUM) {
                            val isLightStyle = currentItems.any { 
                                it is GenericSettingListItem.GroupOptions && 
                                it.keyId == "mapStyleLight" && 
                                it.items.any { it.id == event.option.id } 
                            }
                            
                            val updatedProvider = if (isLightStyle) {
                                currentOption.copy(lightStyle = newStyle)
                            } else {
                                currentOption.copy(darkStyle = newStyle)
                            }
                            appPrefs.setAndroidMapSDKProvider(updatedProvider)
                            _uiState.update { createInitialState() }
                        }
                    }
                }
            }

            else -> {}
        }
    }

    private fun canUserHavePremiumMaps(): Boolean {
        return userRepository.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
    }
}

sealed interface MapsSDKProvider {
    val key: String
    val title: dev.icerock.moko.resources.StringResource
    val description: dev.icerock.moko.resources.StringResource
    val tag: dev.icerock.moko.resources.StringResource?
    val hasSatelliteMaps: Boolean
    val isPremium: Boolean

    enum class MapStyle(val key: String) {
        OPEN_FREE_MAP("openfreemap"),
        MAPTILER("maptiler");

        val title get() = when(this) {
            OPEN_FREE_MAP -> MR.strings.setting_mapsProvider_style_openFreeMap
            MAPTILER -> MR.strings.setting_mapsProvider_style_mapTiler
        }

        companion object {
            fun fromKey(key: String): MapStyle = entries.firstOrNull { it.key == key } ?: OPEN_FREE_MAP
        }
    }

    data object GOOGLE : MapsSDKProvider {
        override val key: String = "google_maps"
        override val title = MR.strings.setting_mapsProvider_googleMaps_title
        override val description = MR.strings.setting_mapsProvider_googleMaps_info
        override val tag = null
        override val hasSatelliteMaps = true
        override val isPremium = false
    }

    data object MAPLIBRE_FREE : MapsSDKProvider {
        override val key: String = "maplibre_free"
        override val title = MR.strings.setting_mapsProvider_mapLibreFree_title
        override val description = MR.strings.setting_mapsProvider_mapLibreFree_info
        override val tag = MR.strings.setting_mapsProvider_mapLibreFree_tag
        override val hasSatelliteMaps = false
        override val isPremium = false
    }

    data class MAPLIBRE_PREMIUM(
        val lightStyle: MapStyle = MapStyle.OPEN_FREE_MAP,
        val darkStyle: MapStyle = MapStyle.OPEN_FREE_MAP
    ) : MapsSDKProvider {
        override val key: String = "maplibre_premium"
        override val title = MR.strings.setting_mapsProvider_mapLibrePremium_title
        override val description = MR.strings.setting_mapsProvider_mapLibrePremium_info
        override val tag = null
        override val hasSatelliteMaps = true
        override val isPremium = true
    }

    companion object {
        fun fromKey(
            key: String,
            lightStyle: MapStyle = MapStyle.OPEN_FREE_MAP,
            darkStyle: MapStyle = MapStyle.OPEN_FREE_MAP
        ): MapsSDKProvider {
            return when (key) {
                GOOGLE.key -> GOOGLE
                MAPLIBRE_FREE.key -> MAPLIBRE_FREE
                MAPLIBRE_PREMIUM().key -> MAPLIBRE_PREMIUM(lightStyle, darkStyle)
                else -> MAPLIBRE_FREE
            }
        }
    }
}