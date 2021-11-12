package com.zbapp.locationtrack.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.locationtrack.data.repos.LogoutControl
import kotlinx.coroutines.flow.catch

class SettingsViewModel(
    private val logoutControl: LogoutControl,
    private val userRepo: UserRepository
) : ViewModel() {

    val userData = userRepo.localUser.asLiveData()

    fun logout(): LiveData<Boolean> {
        return logoutControl.logout().asLiveData()
    }

    fun deleteAccount(): LiveData<Boolean> {
        return logoutControl.deleteAccount()
            .catch { emit(false) }
            .asLiveData()
    }
}