package app.paralino.core.presentation.login

import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.base.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class LoggedOutViewModel(
    private val remoteConfigRepository: RemoteConfigRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(LoggedOutScreenState(hasLogin = remoteConfigRepository.configState.value?.isEmailEnabled == true))
    val uiState: Flow<LoggedOutScreenState> = _uiState

    init {
        remoteConfigRepository.getConfig()
            .map { it.isEmailEnabled }
            .distinctUntilChanged()
            .onEach { canUseEmail ->
                _uiState.update { it.copy(hasLogin = canUseEmail) }
            }
            .launchIn(coroutineScope)
    }
}