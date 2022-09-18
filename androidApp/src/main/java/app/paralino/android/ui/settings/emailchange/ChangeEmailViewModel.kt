package app.paralino.android.ui.settings.emailchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.ChangeEmailUseCase
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.repo.user.UserRepository

class ChangeEmailViewModel(
    userRepository: UserRepository,
    changeEmailUseCase: ChangeEmailUseCase
): ViewModel() {

    private val coreVm = ChangeEmailViewModel(userRepository, changeEmailUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onEmailChange(email: String) = coreVm.onEmailChange(email)
    fun onPasswordChange(password: String) = coreVm.onPasswordChange(password)

}