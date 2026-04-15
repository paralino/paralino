package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.settings.NotificationGroupingType
import app.paralino.core.provider.localizedString
import kotlinx.coroutines.flow.update

class NotificationsGroupingViewModel(
    private val appPrefs: AppPrefs
): BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_notificationGrouping_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.notificationGrouping
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.Subtitle(MR.strings.setting_notificationGrouping_info.localizedString()),
                    GenericSettingListItem.GroupOptions(
                        defaultId = NotificationGroupingType.TYPE.key,
                        keyId = "grouping",
                        items = NotificationGroupingType.entries.map {
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
                (event.option.key as? NotificationGroupingType?)?.let {
                    appPrefs.notificationGrouping = it
                }
                _uiState.update { state ->
                    state.copy(
                        content = when (val stateContent = state.content) {
                            is GenericSettingsScreenStateContent.ListItems -> {
                                stateContent.copy(
                                    items = stateContent.items.map { item ->
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

