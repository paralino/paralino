package com.zbapp.locationtrack.ui.login.recover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.login.recover.RecoverKeysViewModel

class RecoverKeysViewModel(

) : ViewModel() {

    private val coreVm = RecoverKeysViewModel(viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun logout() = coreVm.logout()
    fun checkForKey() = coreVm.checkForKey()
    fun onOldPasswordChange(password: String) = coreVm.onOldPasswordChange(password)

}