package com.zbapp.locationtrack.ui.settings.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.settings.editprofile.EditProfileViewModel
import com.zbapp.core.repo.user.UserRepository

class EditProfileViewModel(
    userRepository: UserRepository,
) : ViewModel() {

    private val coreVm = EditProfileViewModel(userRepository, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onImageChange(imageModel: ImageModel) = coreVm.onImageChange(imageModel)
    fun onNameChange(name: String) = coreVm.onNameChange(name)
}