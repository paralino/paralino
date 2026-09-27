package app.paralino.core.presentation.personal_space.settings

import app.paralino.core.domain.user.PersonalSpaceFeatureUseCase
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.repo.base.AppConfigRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalSpaceSettingsViewModel(
    private val appConfig: AppConfigRepository,
    private val personalSpaceFeature: PersonalSpaceFeatureUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        PersonalSpaceSettingsScreenState(
            loadingAction = false,
            alertDialog = null,
            hasDevFeatures = appConfig.isInAppDevMode
        )
    )
    val uiState: StateFlow<PersonalSpaceSettingsScreenState> = _uiState

    private val _eventChannel = Channel<PersonalSpaceSettingsEvent>()
    val events = _eventChannel.receiveAsFlow()

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(loadingAction = loading) }
    }

    fun onEvent(event: PersonalSpaceSettingsUiEvent) {
        when (event) {
            PersonalSpaceSettingsUiEvent.DisablePersonalSpaceClick -> {
                _uiState.update { it.copy(alertDialog = MainAlertDialog.DisablePersonalSpaceOnDevice) }
            }

            is PersonalSpaceSettingsUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }
                if (event.action.dialog is MainAlertDialog.DisablePersonalSpaceOnDevice && event.action.isOk) {
                    disablePersonalSpace()
                }
            }
        }
    }

    private fun disablePersonalSpace() {
        coroutineScope.launch {
            _uiState.update { it.copy(loadingAction = true) }
            personalSpaceFeature.disable()
            _uiState.update { it.copy(loadingAction = false) }
            _eventChannel.send(PersonalSpaceSettingsEvent.OpenMap)
        }
    }
}
