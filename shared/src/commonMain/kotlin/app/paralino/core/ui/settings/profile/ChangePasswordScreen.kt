package app.paralino.core.ui.settings.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.domain.user.ChangeCredentialsStep
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordScreenEvent
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.PasswordValidationElement
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ChangePasswordScreenCore(
    appState: CoreUiState,
    vm: ChangePasswordViewModel,
    onNavigateBack: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                ChangePasswordScreenEvent.PasswordChanged -> {
                    onNavigateBack()
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    ActionScaffold(
        modifier = Modifier.fillMaxSize(),
        buttonState = AppButtonState.loadingStateWithCheck(uiState.actionLoading),
        buttonText = stringResource(MR.strings.settings_account_edit_password_button),
        buttonIcon = null,
        onConfirm = {
            focusManager.clearFocus()
            vm.proceed()
        }
    ) {
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
                .padding(it)
                .padding(20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (uiState.lastPasswordUpdateTime != null) {
                SmallActionButton(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onSurface,
                    icon = CoreAppIcons.AccessTime.asPainterResource(),
                    text = {
                        Text(text = uiState.lastPasswordUpdateTime ?: "".annotatedBoldCompose())
                    },
                    onClick = null
                )
                Spacer(modifier = Modifier.size(20.dp))
            }

            PasswordInputField(
                password = uiState.oldPassword,
                hint = stringResource(MR.strings.settings_account_edit_email_placeholder_password),
                hasError = uiState.hasOldPasswordError,
                imeAction = ImeAction.Next,
                focusManager = focusManager,
                onInputChange = { vm.onOldPasswordChange(it) }
            )

            Spacer(modifier = Modifier.size(8.dp))

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

            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                title = stringResource(MR.strings.settings_account_credentialsChange_revokeSessions_title),
                description = stringResource(MR.strings.settings_account_credentialsChange_revokeSessions_info),
                state = rememberSeekbarState(
                    key = uiState,
                    state = LargeSeekbarSwitchState.SeekbarSwitchState(
                        seekbarState = null,
                        switchState = LargeSeekbarSwitchState.Switch(uiState.logoutOtherDevices)
                    )
                ).value,
                onStateChange = {
                    vm.onDeviceSessionToggleChange(it)
                }
            )

            val stepText = getStepText(uiState.credentialsStep)
            AnimatedVisibility(
                visible = stepText.isNotBlank(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stepText,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .animateContentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.accent.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun getStepText(step: ChangeCredentialsStep?): String {
    return when (step) {
        ChangeCredentialsStep.PREPARING,
        ChangeCredentialsStep.KEY_SETUP,
        ChangeCredentialsStep.REVOKING_SESSIONS -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_prepare)
        ChangeCredentialsStep.CHANGING_CREDENTIALS -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_change)
        ChangeCredentialsStep.KEY_APPLY -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_verify)
        ChangeCredentialsStep.DONE -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_done)
        null -> ""
    }
}