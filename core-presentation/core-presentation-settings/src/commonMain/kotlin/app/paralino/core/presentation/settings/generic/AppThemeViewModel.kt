package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.settings.AppThemeType
import app.paralino.core.models.settings.DarkThemeStyleType
import app.paralino.core.provider.localizedString
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppThemeViewModel(
    private val appPrefs: AppPrefs
): BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_appTheme_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.appTheme
        val currentDarkThemeStyle = appPrefs.darkThemeStyle
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = buildList {
                    add(GenericSettingListItem.Spacer(20.dp, keyId = "top_space"))
                    add(
                        GenericSettingListItem.GroupOptions(
                            defaultId = AppThemeType.SYSTEM.key,
                            keyId = "theme",
                            items = AppThemeType.entries.map {
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
                        )
                    )

                    // no dark styles with light theme
                    if (currentOption != AppThemeType.LIGHT) {
                        add(GenericSettingListItem.Spacer(30.dp, keyId = "dark_style_space"))
                        add(
                            GenericSettingListItem.Subtitle(
                                content = MR.strings.setting_appTheme_darkStyle_title.localizedString(),
                                bottomPadding = 10.dp
                            )
                        )
                        add(
                            GenericSettingListItem.GroupOptions(
                                defaultId = DarkThemeStyleType.PARALINO.key,
                                keyId = "dark_style",
                                items = DarkThemeStyleType.entries.map {
                                    GroupOptionItem(
                                        key = it,
                                        id = it.key,
                                        title = it.title.localizedString(),
                                        description = it.description.localizedString(),
                                        tag = null,
                                        isTagWarning = false,
                                        isPremiumFeatureEnabled = true,
                                        isSelected = it == currentDarkThemeStyle
                                    )
                                }
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
                when (val option = event.option.key) {
                    is AppThemeType -> {
                        appPrefs.appTheme = option
                        _uiState.update { createInitialState() }
                        coroutineScope.launch {
                            _eventChannel.send(GenericSettingsScreenEvent.ApplyAppTheme(option))
                        }
                    }

                    is DarkThemeStyleType -> {
                        appPrefs.darkThemeStyle = option
                        _uiState.update { createInitialState() }
                        coroutineScope.launch {
                            _eventChannel.send(GenericSettingsScreenEvent.ApplyDarkThemeStyle(option))
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
