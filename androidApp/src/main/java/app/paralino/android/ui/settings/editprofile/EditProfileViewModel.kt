package app.paralino.android.ui.settings.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.repo.user.UserRepository

class EditProfileViewModel(
    userRepository: UserRepository,
    updateUserDataUseCase: UpdateUserDataUseCase
) : ViewModel() {

    private val coreVm = EditProfileViewModel(userRepository, updateUserDataUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onImageChange(imageModel: ImageModel) = coreVm.onImageChange(imageModel)
    fun onNameChange(name: String) = coreVm.onNameChange(name)
}