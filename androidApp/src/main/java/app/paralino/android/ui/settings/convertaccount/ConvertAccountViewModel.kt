package app.paralino.android.ui.settings.convertaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.ConvertAccountUseCase
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel

class ConvertAccountViewModel(
    convertAccountUseCase: ConvertAccountUseCase,
) : ViewModel() {

    private val coreVm = ConvertAccountViewModel(convertAccountUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onEmailChange(email: String) = coreVm.onEmailChange(email)
    fun onPasswordChange(password: String) = coreVm.onPasswordChange(password)
    fun onConfirmPasswordChange(password: String) = coreVm.onConfirmPasswordChange(password)

}