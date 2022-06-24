package com.zbapp.core.presentation.login.recover

import com.zbapp.core.models.Resource
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.common.FormValidation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecoverKeysViewModel(
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow(RecoverKeysScreenState())
    val uiState: Flow<RecoverKeysScreenState> = _uiState

    private val _eventChannel = Channel<RecoverKeysScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun proceed() {
        if (validatePassword()) {

        }
    }

    fun checkForKey() {
        if (validatePassword()) {

        }
    }

    fun validatePassword(): Boolean {
        val passwordOld = _uiState.value.oldPassword
        val oldPasswordValidation = FormValidation.Password.validate(passwordOld)

        _uiState.update {
            it.copy(
                hasOldPasswordError = !oldPasswordValidation.result
            )
        }

        if (!oldPasswordValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(RecoverKeysScreenEvent.ValidationError(oldPasswordValidation))
            }
        }

        return oldPasswordValidation.result
    }

    fun logout() {

    }

    fun onOldPasswordChange(password: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update {
            it.copy(
                oldPassword = password,
                hasOldPasswordError = false,
                recoverState = Resource.NotInitialized
            )
        }
    }

}