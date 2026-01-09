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
            add(MapsSDKProvider.MAPLIBRE_PREMIUM)
        }

        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.Subtitle(MR.strings.setting_mapsProvider_info.localizedString()),
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
                                isPremiumFeatureEnabled = if (it == MapsSDKProvider.MAPLIBRE_PREMIUM) {
                                    canUserHavePremiumMaps()
                                } else {
                                    true
                                },
                                isSelected = it == currentOption
                            )
                        }
                    )
                )
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DialogAction -> {}
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {}
            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                if (!event.option.isPremiumFeatureEnabled) {
                    _uiState.update {
                        it.copy(alertDialog = MainAlertDialog.LocationPerformancePremiumInfo)
                    }
                    return
                }

                (event.option.key as? MapsSDKProvider?)?.let {
                    appPrefs.setAndroidMapSDKProvider(it)
                }
                _uiState.update { state ->
                    state.copy(
                        content = when (state.content) {
                            is GenericSettingsScreenStateContent.ListItems -> {
                                state.content.copy(
                                    items = state.content.items.map { item ->
                                        when (val finalItem = item) {
                                            is GenericSettingListItem.GroupOptions -> {
                                                finalItem.copy(
                                                    items = finalItem.items.map {
                                                        it.copy(isSelected = it.id == event.option.id)
                                                    }
                                                )
                                            }
                                            else -> item
                                        }
                                    }
                                )
                            }

                            else -> state.content
                        }
                    )
                }
            }
        }
    }

    private fun canUserHavePremiumMaps(): Boolean {
        return userRepository.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
    }
}

enum class MapsSDKProvider constructor(val key: String) {
    GOOGLE("google_maps"),
    MAPLIBRE_FREE("maplibre_free"),
    MAPLIBRE_PREMIUM("maplibre_premium");

    val title get() = when(this) {
        GOOGLE -> MR.strings.setting_mapsProvider_googleMaps_title
        MAPLIBRE_FREE -> MR.strings.setting_mapsProvider_mapLibreFree_title
        MAPLIBRE_PREMIUM -> MR.strings.setting_mapsProvider_mapLibrePremium_title
    }

    val description get() = when(this) {
        GOOGLE -> MR.strings.setting_mapsProvider_googleMaps_info
        MAPLIBRE_FREE -> MR.strings.setting_mapsProvider_mapLibreFree_info
        MAPLIBRE_PREMIUM -> MR.strings.setting_mapsProvider_mapLibrePremium_info
    }

    val tag get() = when(this) {
        GOOGLE -> null
        MAPLIBRE_FREE -> MR.strings.setting_mapsProvider_mapLibreFree_tag
        MAPLIBRE_PREMIUM -> null
    }

    companion object {
        fun fromKey(key: String): MapsSDKProvider? {
            return entries.firstOrNull { it.key == key }
        }
    }
}