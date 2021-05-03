package com.zbapp.locationtrack.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zbapp.locationtrack.data.repos.LogoutControl

class SettingsViewModel(
    private val logoutControl: LogoutControl
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    fun logout(): LiveData<Boolean> {
        return logoutControl.logout()
    }
}