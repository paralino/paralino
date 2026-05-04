package app.paralino.core.presentation.places.detail

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceSensitivityData
import app.paralino.core.models.places.PlaceSensitivityDelayType
import app.paralino.core.models.places.PlaceSensitivityLocationRefreshType
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class PlaceSensitivityViewModel(
    val groupId: String,
    val placeId: String,
    private val placesRepo: PlacesRepository,
    private val placeLocal: PlacesLocalDataSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val appPrefs: AppPrefs
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.place_sensitivity_screen_title.localizedString()
) {

    private val placeData = MutableStateFlow<GroupPlace?>(null)
    private val groupData = MutableStateFlow<UserGroup?>(null)

    init {
        placeLocal.getPlaceWithoutSettings(placeId = placeId)
            .mapToPayload { it }
            .onEach { result ->
                groupData.update { groupLocal.getGroupOnce(groupId = groupId) }
                placeData.update { result.data }
                _uiState.update {
                    createInitialState()
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    override fun createInitialState(): GenericSettingsScreenState {
        when (val place = placeData.value) {
            null -> {
                return GenericSettingsScreenState(
                    screenTitle = initialScreenTitle,
                    alertDialog = null,
                    content = GenericSettingsScreenStateContent.Loading
                )
            }

            else -> {
                val canEdit =
                    place.ownerUserId == appPrefs.userId || groupData.value?.isCurrentUserOwner == true
                val userOptions = appPrefs.getPlaceSensitivityForPlace(placeId = placeId)
                val placeOptions = place.alertSensitivity
                val currentOptions = userOptions ?: placeOptions
                val isAppliedToAllUsers = userOptions == null || userOptions == placeOptions

                return GenericSettingsScreenState(
                    screenTitle = initialScreenTitle,
                    alertDialog = null,
                    content = GenericSettingsScreenStateContent.ListItems(
                        items = buildList {
                            add(GenericSettingListItem.Spacer(20.dp, keyId = "top_space"))

                            if (canEdit) {
                                add(GenericSettingListItem.Subtitle(MR.strings.place_sensitivity_info_owner.localizedString()))
                            } else {
                                add(GenericSettingListItem.Subtitle(MR.strings.place_sensitivity_info_member.localizedString()))
                            }

                            add(
                                GenericSettingListItem.Notice(
                                    content = MR.strings.place_sensitivity_defaultNotice.localizedString(),
                                    icon = CoreAppIcons.Warning
                                )
                            )

                            add(
                                GenericSettingListItem.Spacer(
                                    spacing = 20.dp,
                                    keyId = "switchAll_space"
                                )
                            )

                            if (canEdit) {
                                add(
                                    GenericSettingListItem.Switch(
                                        key = "switchAllUsers",
                                        keyId = "switchAllUsers",
                                        title = MR.strings.place_sensitivity_allUsersToggle_title.localizedString(),
                                        description = if (isAppliedToAllUsers) {
                                            MR.strings.place_sensitivity_allUsersToggle_info_on.localizedString()
                                        } else {
                                            MR.strings.place_sensitivity_allUsersToggle_info_off.localizedString()
                                        },
                                        icon = CoreAppIcons.Group,
                                        isChecked = isAppliedToAllUsers,
                                        isEnabled = true,
                                        isLoading = false,
                                        isVisible = true
                                    )
                                )
                            }

                            add(GenericSettingListItem.StickyHeader(content = MR.strings.place_sensitivity_sectionDelay_title.localizedString()))
                            add(GenericSettingListItem.Subtitle(MR.strings.place_sensitivity_sectionDelay_info.localizedString()))

                            add(
                                GenericSettingListItem.GroupOptions(
                                    defaultId = PlaceSensitivityDelayType.default.key,
                                    keyId = "delayOptions",
                                    items = PlaceSensitivityDelayType.entries.map {
                                        GroupOptionItem(
                                            key = it,
                                            id = it.key,
                                            title = it.title.localizedString(),
                                            description = it.description.localizedString(),
                                            tag = null,
                                            isTagWarning = false,
                                            isPremiumFeatureEnabled = true,
                                            isSelected = it == currentOptions.sensitivityDelay
                                        )
                                    }
                                )
                            )

                            add(
                                GenericSettingListItem.Spacer(
                                    spacing = 20.dp,
                                    keyId = "accuracy_space"
                                )
                            )
                            add(GenericSettingListItem.StickyHeader(content = MR.strings.place_sensitivity_sectionRefresh_title.localizedString()))
                            add(GenericSettingListItem.Subtitle(MR.strings.place_sensitivity_sectionRefresh_info.localizedString()))

                            add(
                                GenericSettingListItem.Switch(
                                    key = "switchIgnoreInaccurate",
                                    keyId = "switchIgnoreInaccurate",
                                    title = MR.strings.place_sensitivity_optionRefresh_onlyAccurate_title.localizedString(),
                                    description = if (currentOptions.ignoreInaccurate) {
                                        MR.strings.place_sensitivity_optionRefresh_onlyAccurate_info_on.localizedString()
                                    } else {
                                        MR.strings.place_sensitivity_optionRefresh_onlyAccurate_info_off.localizedString()
                                    },
                                    icon = null,
                                    isChecked = currentOptions.ignoreInaccurate,
                                    isEnabled = true,
                                    isLoading = false,
                                    isVisible = true
                                )
                            )

                            add(
                                GenericSettingListItem.Switch(
                                    key = "switchAccuracy",
                                    keyId = "switchAccuracy",
                                    title = MR.strings.place_sensitivity_optionRefresh_inaccurate_title.localizedString(),
                                    description = if (currentOptions.refreshOnlyInaccurate) {
                                        MR.strings.place_sensitivity_optionRefresh_inaccurate_info_on.localizedString()
                                    } else {
                                        MR.strings.place_sensitivity_optionRefresh_inaccurate_info_off.localizedString()
                                    },
                                    icon = null,
                                    isChecked = currentOptions.refreshOnlyInaccurate,
                                    isEnabled = true,
                                    isLoading = false,
                                    isVisible = currentOptions.sensitivityRefresh != PlaceSensitivityLocationRefreshType.NONE
                                )
                            )

                            add(
                                GenericSettingListItem.GroupOptions(
                                    defaultId = PlaceSensitivityLocationRefreshType.default.key,
                                    keyId = "refreshOptions",
                                    items = PlaceSensitivityLocationRefreshType.entries.map {
                                        GroupOptionItem(
                                            key = it,
                                            id = it.key,
                                            title = it.title.localizedString(),
                                            description = it.description.localizedString(),
                                            tag = null,
                                            isTagWarning = false,
                                            isPremiumFeatureEnabled = true,
                                            isSelected = it == currentOptions.sensitivityRefresh
                                        )
                                    }
                                )
                            )

                            add(GenericSettingListItem.Spacer(spacing = 40.dp, keyId = "bottom_space"))
                        }
                    )
                )
            }
        }
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {
                if (isLoading()) {
                    return
                }

                when (event.switch.keyId) {
                    "switchAllUsers" -> {
                        toggleAllUsersSwitch(switch = event.switch)
                    }

                    "switchAccuracy" -> {
                        toggleRefreshInaccurateSwitch(switch = event.switch)
                    }

                    "switchIgnoreInaccurate" -> {
                        toggleIgnoreInaccurateSwitch(switch = event.switch)
                    }
                }
            }

            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                if (isLoading()) {
                    return
                }

                when (event.groupOptions.keyId) {
                    "delayOptions" -> {
                        toggleDelayOption(option = event.option)
                    }

                    "refreshOptions" -> {
                        toggleLocationAccuracyOption(option = event.option)
                    }
                }
            }

            else -> {}
        }
    }

    private fun toggleAllUsersSwitch(switch: GenericSettingListItem.Switch<*>) {
        if (switch.isChecked) {
            showSnackbar(text = MR.strings.place_sensitivity_allUsersToggle_alreadyActive.localizedString())
            return
        }

        val place = placeData.value
        if (place != null && switch.isEnabled) {
            val selectedOptions = getCurrentSettings()

            if (selectedOptions != null) {
                if (!switch.isChecked && place.data.data != null) {
                    val newPlace = place.copy(
                        data = place.data.mapSuccess {
                            it.copy(sensitivity = selectedOptions)
                        }
                    )

                    placesRepo.updatePlace(newPlace)
                        .mapToPayload<Unit, Unit>()
                        .onEach { data ->
                            _uiState.update { state ->
                                state.copy(
                                    content = when (val stateContent = state.content) {
                                        is GenericSettingsScreenStateContent.ListItems -> {
                                            stateContent.copy(
                                                items = stateContent.items.map { item ->
                                                    when (val finalItem = item) {
                                                        is GenericSettingListItem.Switch<*> -> {
                                                            finalItem.copy(isLoading = data is Payload.Loading)
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

                            when (data) {
                                is Payload.Error<*> -> {
                                    showSnackbar(data.exception.errorString())
                                }

                                Payload.Loading -> {}
                                is Payload.Success<*> -> {
                                    appPrefs.setPlaceSensitivityForPlace(
                                        placeId = placeId,
                                        sensitivity = null
                                    )
                                }
                            }
                        }
                        .launchIn(coroutineScope)
                }
            }
        }
    }

    private fun toggleRefreshInaccurateSwitch(switch: GenericSettingListItem.Switch<*>) {
        val option = getCurrentSettings()?.copy(
            refreshOnlyInaccurate = !switch.isChecked
        )

        option?.let {
            appPrefs.setPlaceSensitivityForPlace(placeId = placeId, sensitivity = it)
        }
        val isChanged = placeData.value?.alertSensitivity != option

        _uiState.update { state ->
            state.copy(
                content = when (val stateContent = state.content) {
                    is GenericSettingsScreenStateContent.ListItems -> {
                        stateContent.copy(
                            items = stateContent.items.map { item ->
                                when (val finalItem = item) {
                                    is GenericSettingListItem.Switch<*> -> {
                                        when (finalItem.keyId) {
                                            "switchAllUsers" -> {
                                                finalItem.copy(
                                                    isChecked = !isChanged,
                                                    description = if (!isChanged) {
                                                        MR.strings.place_sensitivity_allUsersToggle_info_on.localizedString()
                                                    } else {
                                                        MR.strings.place_sensitivity_allUsersToggle_info_off.localizedString()
                                                    }
                                                )
                                            }

                                            "switchAccuracy" -> {
                                                finalItem.copy(
                                                    isChecked = !finalItem.isChecked,
                                                    description = if (!finalItem.isChecked) {
                                                        MR.strings.place_sensitivity_optionRefresh_inaccurate_info_on.localizedString()
                                                    } else {
                                                        MR.strings.place_sensitivity_optionRefresh_inaccurate_info_off.localizedString()
                                                    }
                                                )
                                            }

                                            else -> item
                                        }
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

    private fun toggleIgnoreInaccurateSwitch(switch: GenericSettingListItem.Switch<*>) {
        val option = getCurrentSettings()?.copy(
            ignoreInaccurate = !switch.isChecked
        )

        option?.let {
            appPrefs.setPlaceSensitivityForPlace(placeId = placeId, sensitivity = it)
        }
        val isChanged = placeData.value?.alertSensitivity != option

        _uiState.update { state ->
            state.copy(
                content = when (val stateContent = state.content) {
                    is GenericSettingsScreenStateContent.ListItems -> {
                        stateContent.copy(
                            items = stateContent.items.map { item ->
                                when (val finalItem = item) {
                                    is GenericSettingListItem.Switch<*> -> {
                                        when (finalItem.keyId) {
                                            "switchAllUsers" -> {
                                                finalItem.copy(
                                                    isChecked = !isChanged,
                                                    description = if (!isChanged) {
                                                        MR.strings.place_sensitivity_allUsersToggle_info_on.localizedString()
                                                    } else {
                                                        MR.strings.place_sensitivity_allUsersToggle_info_off.localizedString()
                                                    }
                                                )
                                            }

                                            "switchIgnoreInaccurate" -> {
                                                finalItem.copy(
                                                    isChecked = !finalItem.isChecked,
                                                    description = if (!finalItem.isChecked) {
                                                        MR.strings.place_sensitivity_optionRefresh_onlyAccurate_info_on.localizedString()
                                                    } else {
                                                        MR.strings.place_sensitivity_optionRefresh_onlyAccurate_info_off.localizedString()
                                                    }
                                                )
                                            }

                                            else -> item
                                        }
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

    private fun toggleDelayOption(option: GroupOptionItem) {
        val settings = getCurrentSettings()?.copy(
            delay = option.id
        )

        if (settings != null) {
            toggleOption(
                settings = settings,
                keyId = "delayOptions",
                settingId = settings.delay
            )
        }
    }

    private fun toggleLocationAccuracyOption(option: GroupOptionItem) {
        val settings = getCurrentSettings()?.copy(
            refresh = option.id
        )

        if (settings != null) {
            toggleOption(
                settings = settings,
                keyId = "refreshOptions",
                settingId = settings.refresh
            )
        }
    }

    private fun toggleOption(
        settings: GroupPlaceSensitivityData,
        keyId: String,
        settingId: String
    ) {
        appPrefs.setPlaceSensitivityForPlace(placeId = placeId, sensitivity = settings)
        val isChanged = placeData.value?.alertSensitivity != settings

        _uiState.update { state ->
            state.copy(
                content = when (val stateContent = state.content) {
                    is GenericSettingsScreenStateContent.ListItems -> {
                        stateContent.copy(
                            items = stateContent.items.map { item ->
                                when (val finalItem = item) {
                                    is GenericSettingListItem.GroupOptions -> {
                                        if (finalItem.keyId == keyId) {
                                            finalItem.copy(
                                                items = finalItem.items.map {
                                                    it.copy(
                                                        isSelected = it.id == settingId
                                                    )
                                                }
                                            )
                                        } else {
                                            finalItem
                                        }
                                    }

                                    is GenericSettingListItem.Switch<*> -> {
                                        when (finalItem.keyId) {
                                            "switchAllUsers" -> {
                                                finalItem.copy(
                                                    isChecked = !isChanged,
                                                    description = if (!isChanged) {
                                                        MR.strings.place_sensitivity_allUsersToggle_info_on.localizedString()
                                                    } else {
                                                        MR.strings.place_sensitivity_allUsersToggle_info_off.localizedString()
                                                    }
                                                )
                                            }

                                            "switchAccuracy" -> {
                                                finalItem.copy(
                                                    isVisible = settings.sensitivityRefresh != PlaceSensitivityLocationRefreshType.NONE
                                                )
                                            }

                                            else -> item
                                        }
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

    private fun getCurrentSettings(): GroupPlaceSensitivityData? {
        when (val state = _uiState.value.content) {
            is GenericSettingsScreenStateContent.ListItems -> {
                return GroupPlaceSensitivityData(
                    delay = state.items.filterIsInstance<GenericSettingListItem.GroupOptions>()
                        .firstOrNull {
                            it.keyId == "delayOptions"
                        }?.selectedItem?.let {
                        PlaceSensitivityDelayType.fromKey(key = it.id)
                    }?.key ?: PlaceSensitivityDelayType.default.key,
                    refresh = state.items.filterIsInstance<GenericSettingListItem.GroupOptions>()
                        .firstOrNull {
                            it.keyId == "refreshOptions"
                        }?.selectedItem?.let {
                        PlaceSensitivityLocationRefreshType.fromKey(key = it.id)
                    }?.key ?: PlaceSensitivityLocationRefreshType.default.key,
                    refreshOnlyInaccurate = state.items.filterIsInstance<GenericSettingListItem.Switch<*>>()
                        .firstOrNull {
                            it.keyId == "switchAccuracy"
                        }?.isChecked ?: true,
                    ignoreInaccurate = state.items.filterIsInstance<GenericSettingListItem.Switch<*>>()
                        .firstOrNull {
                            it.keyId == "switchIgnoreInaccurate"
                        }?.isChecked ?: true
                )
            }

            else -> return null
        }
    }

    private fun isLoading(): Boolean {
        return when (val state = _uiState.value.content) {
            is GenericSettingsScreenStateContent.ListItems -> {
                state.items.filterIsInstance<GenericSettingListItem.Switch<*>>().any {
                    it.isLoading
                }
            }

            is GenericSettingsScreenStateContent.EditScreen,
            is GenericSettingsScreenStateContent.EmptyState,
            is GenericSettingsScreenStateContent.Loading -> return true
        }
    }
}