package com.zbapp.locationtrack.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.locationtrack.data.repos.LogoutControl
import kotlinx.coroutines.flow.catch

class SettingsViewModel(
    private val logoutControl: LogoutControl
) : ViewModel() {

    fun logout(): LiveData<Boolean> {
        return logoutControl.logout().asLiveData()
    }

    fun deleteAccount(): LiveData<Boolean> {
        return logoutControl.deleteAccount()
            .catch { emit(false) }
            .asLiveData()
    }
}