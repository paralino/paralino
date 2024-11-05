package app.paralino.core.ui.settings.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountScreenEvent
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountScreenState
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.PasswordValidationElement
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConvertAccountScreenCore(
    appState: CoreUiState,
    vm: ConvertAccountViewModel,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = ConvertAccountScreenState())
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                ConvertAccountScreenEvent.AccountUpdated -> {
                    onNavigateBack()
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
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.size(30.dp))

        Text(
            text = stringResource(MR.strings.settings_account_edit_guest_account_info),
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.size(30.dp))

        Column(
            Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {

            MainInputField(
                text = uiState.email,
                hint = stringResource(MR.strings.login_placeholder_email),
                hasError = uiState.hasEmailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onInputChange = { vm.onEmailChange(it) }
            )

            Spacer(modifier = Modifier.size(8.dp))

            PasswordInputField(
                password = uiState.password,
                hint = stringResource(MR.strings.login_placeholder_password),
                hasError = uiState.hasPasswordError,
                imeAction = ImeAction.Next,
                focusManager = focusManager,
                onInputChange = { vm.onPasswordChange(it) }
            )

            Spacer(modifier = Modifier.size(8.dp))

            PasswordInputField(
                password = uiState.passwordConfirm,
                hint = stringResource(MR.strings.login_placeholder_password_confirm),
                hasError = uiState.hasPasswordConfirmError,
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
                Text(text = stringResource(MR.strings.settings_account_edit_guest_account_button))
            }

        }

        Spacer(modifier = Modifier.weight(1f))
    }
}