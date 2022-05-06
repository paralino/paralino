package com.zbapp.core.presentation.settings

import com.zbapp.core.domain.user.LogoutUseCase
import com.zbapp.core.models.Payload
import com.zbapp.core.models.exception
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository,
    scope: CoroutineScope
) : CoreViewModel(scope) {

    private val _accountState = MutableStateFlow<Payload<AccountScreenState>>(Payload.Loading)
    val accountState: Flow<Payload<AccountScreenState>> = _accountState

    private val _eventChannel = Channel<AccountScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepo.localUser
            .onEach { userState ->
                _accountState.update {
                    userState.mapSuccess { user ->
                        AccountScreenState(
                            userState = UserState(
                                name = user.name,
                                userId = user.id,
                                email = user.email,
                                image = user.data?.userImage
                            ),
                            logoutState = null,
                            deleteAccountState = null
                        )
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