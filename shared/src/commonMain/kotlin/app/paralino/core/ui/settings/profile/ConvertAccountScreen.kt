package app.paralino.core.ui.settings.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.domain.user.ChangeCredentialsStep
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountScreenEvent
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountScreenStep
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.PasswordValidationElement
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.ui.theme.AppColors
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
    val uiState by vm.uiState.collectAsState()
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

    ActionScaffold(
        modifier = Modifier.fillMaxSize(),
        buttonState = AppButtonState.loadingStateWithCheck(uiState.actionLoading),
        buttonText = when (uiState.screenStep) {
            ConvertAccountScreenStep.ONBOARDING -> {
                stringResource(MR.strings.button_continue)
            }

            ConvertAccountScreenStep.INPUT -> {
                stringResource(MR.strings.settings_account_edit_guest_account_button)
            }
        },
        buttonIcon = null,
        onConfirm = {
            focusManager.clearFocus()
            vm.proceed()
        }
    ) {
        AnimatedVisibility(
            visible = uiState.screenStep == ConvertAccountScreenStep.ONBOARDING,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
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
                    .padding(contentPadding)
                    .padding(it)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SettingsActionItem(
                    name = stringResource(MR.strings.settings_account_guest_convert_intro_multiple_devices),
                    tag = stringResource(MR.strings.settings_account_guest_convert_intro_multiple_devices_tag),
                    icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
                    type = SettingsActionItemType.Action
                )
                Spacer(Modifier.size(20.dp))
                SettingsActionItem(
                    name = stringResource(MR.strings.settings_account_guest_convert_intro_account_recovery),
                    tag = stringResource(MR.strings.settings_account_guest_convert_intro_account_recovery_tag),
                    icon = CoreAppIcons.Replay.asPainterResource(),
                    type = SettingsActionItemType.Action
                )
                Spacer(Modifier.size(20.dp))
                SettingsActionItem(
                    name = stringResource(MR.strings.settings_account_guest_convert_intro_personal_device_tracking),
                    tag = stringResource(MR.strings.settings_account_guest_convert_intro_personal_device_tracking_tag),
                    icon = CoreAppIcons.ShareLocation.asPainterResource(),
                    type = SettingsActionItemType.Action
                )

            }
        }

        AnimatedVisibility(
            visible = uiState.screenStep == ConvertAccountScreenStep.INPUT,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
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
                    .padding(contentPadding)
                    .padding(it)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                NoticeInfoChip(
                    text = stringResource(MR.strings.settings_account_edit_guest_account_info),
                    icon = CoreAppIcons.Warning.asPainterResource()
                )

                Spacer(modifier = Modifier.size(20.dp))

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

                val stepText = getStepText(uiState.credentialsStep)

                AnimatedVisibility(
                    visible = stepText.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .animateContentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.accent.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun getStepText(step: ChangeCredentialsStep?): String {
    return when (step) {
        ChangeCredentialsStep.PREPARING, ChangeCredentialsStep.KEY_SETUP -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_prepare)
        ChangeCredentialsStep.CHANGING_CREDENTIALS -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_change)
        ChangeCredentialsStep.KEY_APPLY -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_verify)
        ChangeCredentialsStep.DONE -> stringResource(resource = MR.strings.settings_account_credentialsChange_step_done)
        null -> ""
    }
}