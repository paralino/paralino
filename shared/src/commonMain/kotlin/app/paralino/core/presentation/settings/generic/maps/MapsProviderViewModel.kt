package app.paralino.core.presentation.settings.generic.maps

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.presentation.settings.generic.GroupOptionItem
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
                                items = MapStyle.Light.allLightStyles
                                    .filter {
                                        if (canUserHavePremiumMaps()) {
                                            !it.isPremium || currentOption.isPremium
                                        } else {
                                            true
                                        }
                                    }
                                    .map {
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
                                keyId = "lightMap_${currentOption.lightStyle.key}",
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
                                items = MapStyle.Dark.allDarkStyles
                                    .filter {
                                        if (canUserHavePremiumMaps()) {
                                            !it.isPremium || currentOption.isPremium
                                        } else {
                                            true
                                        }
                                    }
                                    .map {
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
                                keyId = "darkMap_${currentOption.darkStyle.key}",
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
