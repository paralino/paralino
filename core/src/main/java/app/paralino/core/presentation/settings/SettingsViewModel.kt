package app.paralino.core.presentation.settings

import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.exception
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _accountState = MutableStateFlow<Payload<AccountScreenState>>(Payload.Loading)
    val accountState: Flow<Payload<AccountScreenState>> = _accountState

    private val _eventChannel = Channel<AccountScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepo.localUser
            .onEach { userState ->
                _accountState.update {
                    when (userState) {
                        Resource.NotInitialized -> it
                        is Resource.Ready -> {
                            if (userState.data != null) {
                                Payload.Success(
                                    AccountScreenState(
                                        userState = UserState(
                                            name = userState.data.name,
                                            userId = userState.data.id,
                                            email = userState.data.email,
                                            image = userState.data.data?.userImage
                                        ),
                                        logoutState = null,
                                        deleteAccountState = null
                                    )
                                )
                            } else {
                                Payload.Error(UnauthorizedException(type = null, "User is not ready"))
                            }
                        }
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun logout() {
        logoutControl.logout()
            .mapToPayload { Unit }
            .onEach { state ->
                _accountState.update {
                    it.mapSuccess {
                        _eventChannel.send(state.exception?.let { AccountScreenEvent.Error(it) })
                        it.copy(logoutState = state)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun deleteAccount() {
        logoutControl.deleteAccount()
            .mapToPayload { Unit }
            .onEach { state ->
                _accountState.update {
                    it.mapSuccess {
                        _eventChannel.send(state.exception?.let { AccountScreenEvent.Error(it) })
                        it.copy(deleteAccountState = state)
                    }
                }
            }
            .launchIn(coroutineScope)
    }


}