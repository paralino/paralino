package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.provider.localizedString
import kotlinx.coroutines.flow.update

class MapInteractionsViewModel(
    private val appPrefs: AppPrefs
): BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_mapInteractions_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.mapInteractions
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.Subtitle(MR.strings.setting_mapInteractions_info.localizedString()),
                    GenericSettingListItem.GroupOptions(
                        defaultId = MapInteractionsType.DEVICE_DEFAULT.key,
                        keyId = "interactions",
                        items = MapInteractionsType.entries.map {
                            GroupOptionItem(
                                key = it,
                                id = it.key,
                                title = it.title.localizedString(),
                                description = it.description.localizedString(),
                                tag = null,
                                isTagWarning = false,
                                isPremiumFeatureEnabled = true,
                                isSelected = it == currentOption
                            )
                        }
                    ),
                    GenericSettingListItem.Spacer(spacing = 40.dp, keyId = "bottom_space")
                )
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                (event.option.key as? MapInteractionsType?)?.let {
                    appPrefs.mapInteractions = it
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
            else -> {}
        }
    }
}

enum class MapInteractionsType(val key: String) {
    DEVICE_DEFAULT("device_default"),
    NONE("none"),
    MINIMIZE("minimize"),
    COLLAPSED("collapsed");

    val title get() = when(this) {
        DEVICE_DEFAULT -> MR.strings.setting_mapInteractions_deviceDefault_title
        NONE -> MR.strings.setting_mapInteractions_none_title
        MINIMIZE -> MR.strings.setting_mapInteractions_minimize_title
        COLLAPSED -> MR.strings.setting_mapInteractions_collapsed_title
    }

    val description get() = when(this) {
        DEVICE_DEFAULT -> MR.strings.setting_mapInteractions_deviceDefault_info
        NONE -> MR.strings.setting_mapInteractions_none_info
        MINIMIZE -> MR.strings.setting_mapInteractions_minimize_info
        COLLAPSED -> MR.strings.setting_mapInteractions_collapsed_info
    }

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }

        fun forScreenSize(
            defaultType: MapInteractionsType,
            maxWidthDp: Dp,
            maxHeightDp: Dp
        ): MapInteractionsType {
            return when (defaultType) {
                MapInteractionsType.DEVICE_DEFAULT -> {
                    when {
                        maxHeightDp.value < 460 -> {
                            MapInteractionsType.COLLAPSED
                        }

                        maxHeightDp.value < 800 -> {
                            MapInteractionsType.MINIMIZE
                        }

                        else -> {
                            MapInteractionsType.NONE
                        }
                    }
                }

                else -> defaultType
            }
        }
    }
}
