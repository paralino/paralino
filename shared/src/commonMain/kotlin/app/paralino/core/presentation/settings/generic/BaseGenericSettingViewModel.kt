package app.paralino.core.presentation.settings.generic

import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseGenericSettingViewModel(
    val initialScreenTitle: String,
    val deviceApiProvider: IDeviceApiProvider? = null
) : CoreViewModel() {

    protected val _uiState = MutableStateFlow<GenericSettingsScreenState>(
        GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.Loading
        )
    )
    val uiState: StateFlow<GenericSettingsScreenState> = _uiState
    val screenTitle: Flow<String> = _uiState.map { it.screenTitle }

    protected val _eventChannel = Channel<GenericSettingsScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun onEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.LocationPerformancePremiumInfo -> {
                        _uiState.update {
                            it.copy(alertDialog = null)
                        }
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            coroutineScope.launch {
                                _eventChannel.send(GenericSettingsScreenEvent.OpenPaywall)
                            }
                        }
                    }

                    else -> handleUiEvent(event)
                }
            }

            is GenericSettingsScreenUiEvent.SelectOptionItem -> handleUiEvent(event)
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> handleUiEvent(event)
            is GenericSettingsScreenUiEvent.SelectActionItem -> handleUiEvent(event)
            is GenericSettingsScreenUiEvent.EmptyStateActionClick -> handleUiEvent(event)
        }
    }

    protected abstract fun createInitialState(): GenericSettingsScreenState
    protected abstract fun handleUiEvent(event: GenericSettingsScreenUiEvent)
}