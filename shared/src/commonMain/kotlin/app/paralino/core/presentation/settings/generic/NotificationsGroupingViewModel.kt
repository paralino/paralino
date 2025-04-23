package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
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
        return GenericSettingsScreenState.ListItems(
            screenTitle = initialScreenTitle,
            alertDialog = null,
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
                            batteryImpact = null,
                            highBatteryImpact = false,
                            isPremiumFeatureEnabled = true,
                            isSelected = it == currentOption
                        )
                    }
                )
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DialogAction -> {}
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {}
            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                (event.option.key as? NotificationGroupingType?)?.let {
                    appPrefs.notificationGrouping = it
                }
                _uiState.update { state ->
                    when (state) {
                        is GenericSettingsScreenState.ListItems -> {
                            state.copy(
                                items = state.items.map { item ->
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
                        else -> state
                    }
                }
            }
        }
    }
}

enum class NotificationGroupingType(val key: String) {
    NONE("none"),
    EVERYTHING("everything"),
    TYPE("type"),
    GROUP("group"),
    PERSON("person");

    val title get() = when(this) {
        NONE -> MR.strings.setting_notificationGrouping_noGroup_title
        EVERYTHING -> MR.strings.setting_notificationGrouping_everything_title
        GROUP -> MR.strings.setting_notificationGrouping_byGroup_title
        PERSON -> MR.strings.setting_notificationGrouping_byPerson_title
        TYPE -> MR.strings.setting_notificationGrouping_byType_title
    }

    val description get() = when(this) {
        NONE -> MR.strings.setting_notificationGrouping_noGroup_info
        EVERYTHING -> MR.strings.setting_notificationGrouping_everything_info
        GROUP -> MR.strings.setting_notificationGrouping_byGroup_info
        PERSON -> MR.strings.setting_notificationGrouping_byPerson_info
        TYPE -> MR.strings.setting_notificationGrouping_byType_info
    }

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}