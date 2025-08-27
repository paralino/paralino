package app.paralino.core.presentation.personal_space.settings

import app.paralino.core.presentation.CoreViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PersonalSpaceSettingsViewModel : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        PersonalSpaceSettingsScreenState(
            loadingAction = false,
            alertDialog = null
        )
    )
    val uiState: StateFlow<PersonalSpaceSettingsScreenState> = _uiState

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(loadingAction = loading) }
    }
}


