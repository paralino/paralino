package app.paralino.core.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.login.passwordreset.ResetPasswordScreenEvent
import app.paralino.core.presentation.login.passwordreset.ResetPasswordScreenState
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.PasswordValidationElement
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ResetPasswordScreenCore(
    appState: CoreUiState,
    vm: ResetPasswordViewModel,
    openLoginScreen: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = ResetPasswordScreenState())
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is ResetPasswordScreenEvent.Error -> {
                    vm.showSnackbar(event.exception.errorString())
                }

                is ResetPasswordScreenEvent.CredentialsError -> {
                    vm.showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                }

                is ResetPasswordScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        validation = event.validationResult.type
                    )
                    vm.showSnackbar(errorText)
                }

                ResetPasswordScreenEvent.PasswordChanged -> {
                    vm.showSnackbar(MR.strings.reset_password_success_message.localizedString())
                    delay(500)
                    openLoginScreen()
                }

                ResetPasswordScreenEvent.InvalidToken -> {
                    vm.showSnackbar(MR.strings.reset_password_error_token_message.localizedString())
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.size(30.dp))

        Column(
            Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {

            PasswordInputField(
                password = uiState.newPassword,
                hint = stringResource(MR.strings.settings_account_edit_password_placeholder_password_new),
                hasError = uiState.hasNewPasswordError,
                imeAction = ImeAction.Next,
                focusManager = focusManager,
                onInputChange = { vm.onNewPasswordChange(it) }
            )

            Spacer(modifier = Modifier.size(8.dp))

            PasswordInputField(
                password = uiState.confirmPassword,
                hint = stringResource(MR.strings.settings_account_edit_password_placeholder_password_confirm),
                hasError = uiState.hasConfirmPasswordError,
                focusManager = focusManager,
                onInputChange = { vm.onConfirmPasswordChange(it) }
            )

            PasswordValidationElement(
                modifier = Modifier.padding(top = 12.dp),
                minPasswordLength = FormValidation.Password.PasswordStrength.minPasswordLength,
                hasMinPasswordLength = uiState.passwordValidation.minPasswordLength,
                hasMinPasswordStrength = uiState.passwordValidation.passwordStrength.isOK,
                doPasswordsMatch = uiState.passwordValidation.passwordsMatch
            )

            Spacer(modifier = Modifier.size(80.dp))

            Text(text = stringResource(MR.strings.recover_account_caption_reset).annotatedBold())
            Spacer(modifier = Modifier.size(10.dp))
            FilledButton(
                style = ButtonStyle.BIG,
                state = if (uiState.actionLoading == null)
                    AppButtonState.CONTENT
                else
                    AppButtonState.loadingStateWithCheck(uiState.actionLoading!!),
                onClick = {
                    focusManager.clearFocus()
                    vm.proceed()
                }
            ) {
                Text(text = stringResource(MR.strings.settings_account_edit_password_button))
            }

        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
