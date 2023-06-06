package app.paralino.core.presentation.login

import app.paralino.core.models.BackendConfig
import app.paralino.core.presentation.CoreViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LoggedOutViewModel(
    private val backendConfig: BackendConfig
): CoreViewModel() {

    private val _uiState = MutableStateFlow(LoggedOutScreenState(hasLogin = backendConfig.canUseEmail))
    val uiState: Flow<LoggedOutScreenState> = _uiState

}