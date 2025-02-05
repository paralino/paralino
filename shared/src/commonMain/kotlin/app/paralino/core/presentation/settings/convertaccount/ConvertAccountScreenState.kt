package app.paralino.core.presentation.settings.convertaccount

import app.paralino.core.domain.user.ChangeCredentialsStep
import app.paralino.core.presentation.common.FormValidation

data class ConvertAccountScreenState constructor(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val hasPasswordConfirmError: Boolean = false,
    val credentialsStep: ChangeCredentialsStep? = null,
    val screenStep: ConvertAccountScreenStep = ConvertAccountScreenStep.ONBOARDING,
    val actionLoading: Boolean? = null,
) {
    val passwordValidation: FormValidation.PasswordConfirm.PasswordConfirmValidationResult =
        FormValidation.PasswordConfirm.validate(password, passwordConfirm)
}

enum class ConvertAccountScreenStep {
    ONBOARDING, INPUT
}

sealed class ConvertAccountScreenEvent {
    object AccountUpdated : ConvertAccountScreenEvent()
}
