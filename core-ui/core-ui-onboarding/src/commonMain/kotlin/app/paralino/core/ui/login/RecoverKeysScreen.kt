package app.paralino.core.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.login.recover.RecoverKeysScreenState
import app.paralino.core.presentation.login.recover.RecoverKeysScreenUiEvent
import app.paralino.core.presentation.login.recover.RecoverKeysUiStep
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun RecoverKeysScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: RecoverKeysViewModel
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    uiState.data?.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(RecoverKeysScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
        }
    }

    when (val state = uiState) {
        Payload.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        is Payload.Error -> {
            DefaultEmptyState(content = stringResource(MR.strings.recover_account_screen_error))
        }

        is Payload.Success -> {
            RecoverKeysScreen(
                modifier = modifier,
                state = state.data,
                onEvent = vm::onEvent
            )
        }
    }
}

@Composable
private fun RecoverKeysScreen(
    modifier: Modifier = Modifier,
    state: RecoverKeysScreenState,
    onEvent: (RecoverKeysScreenUiEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val uiStep = state.uiStep
    val skipButtonText = uiStep.skipButtonTextRes?.let { stringResource(it) }

    BaseOnboardingScreen(
        modifier = modifier,
        topBarContent = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.recover_account_screen_title),
                navigationIcon = null,
                actions = {
                    FilledButton(
                        modifier = Modifier.padding(end = 4.dp).size(28.dp),
                        onClick = { onEvent(RecoverKeysScreenUiEvent.Logout) },
                        icon = CoreAppIcons.Logout.asPainterResource(),
                        style = ButtonStyle.ICON(
                            iconSize = 24.dp
                        ),
                        elevation = 0.dp,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onBackground
                        ),
                        content = null
                    )
                }
            )
        },
        nextButton = @Composable {
            RecoverKeysConfirmButton(
                uiStep = uiStep,
                isLoading = state.actionLoading,
                onClick = {
                    focusManager.clearFocus()
                    onEvent(RecoverKeysScreenUiEvent.OnConfirmAction)
                }
            )
        },
        skipButton = if (skipButtonText != null) {
            @Composable {
                OnboardingSkipButton(
                    skipButtonText = skipButtonText,
                    onSkip = {
                        focusManager.clearFocus()
                        onEvent(RecoverKeysScreenUiEvent.OnSkipAction)
                    }
                )
            }
        } else {
            null
        }
    ) { paddingValues ->
        AnimatedContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            targetState = uiStep,
            // only animate when step changes, not when input inside step changes
            contentKey = { it::class },
            transitionSpec = {
                fadeIn() + slideInVertically { it / 4 } togetherWith
                        fadeOut() + slideOutVertically { -it / 4 }
            },
            label = "recover keys step"
        ) { step ->
            RecoverKeysStepContent(
                step = step,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun RecoverKeysStepContent(
    modifier: Modifier = Modifier,
    step: RecoverKeysUiStep,
    onEvent: (RecoverKeysScreenUiEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        RecoverKeysHeader(
            icon = step.headerIcon,
            isWarning = step.isReset || step is RecoverKeysUiStep.FetchKeysFailed,
            title = stringResource(step.titleRes),
            info = stringResource(step.infoRes)
        )

        if (step.isReset) {
            RecoverKeysWarningChip(
                modifier = Modifier.padding(top = 20.dp),
                text = stringResource(MR.strings.recover_account_caption_reset)
            )
        }

        when (step) {
            is RecoverKeysUiStep.TryRecoverClassicAccount -> {
                PasswordInputField(
                    modifier = Modifier.padding(top = 20.dp),
                    password = step.oldPassword,
                    hint = stringResource(MR.strings.login_placeholder_password_old),
                    hasError = step.hasOldPasswordError || step.hasWrongOldPasswordError,
                    focusManager = focusManager,
                    onInputChange = { onEvent(RecoverKeysScreenUiEvent.EditOldPassword(it)) }
                )

                AnimatedVisibility(visible = step.hasWrongOldPasswordError) {
                    RecoverKeysWarningChip(
                        modifier = Modifier.padding(top = 10.dp),
                        text = stringResource(MR.strings.recover_account_step_password_wrong)
                    )
                }
            }

            is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> {
                PasswordInputField(
                    modifier = Modifier.padding(top = 20.dp),
                    password = step.currentPassword,
                    hint = stringResource(MR.strings.settings_account_edit_email_placeholder_password),
                    hasError = step.hasCurrentPasswordError || step.hasWrongCurrentPasswordError,
                    focusManager = focusManager,
                    onInputChange = { onEvent(RecoverKeysScreenUiEvent.EditCurrentPassword(it)) }
                )

                AnimatedVisibility(visible = step.hasWrongCurrentPasswordError) {
                    RecoverKeysWarningChip(
                        modifier = Modifier.padding(top = 10.dp),
                        text = stringResource(MR.strings.recover_account_step_password_wrong)
                    )
                }
            }

            is RecoverKeysUiStep.ResetClassicAccount -> {
                PasswordInputField(
                    modifier = Modifier.padding(top = 20.dp),
                    password = step.currentPassword,
                    hint = stringResource(MR.strings.settings_account_edit_email_placeholder_password),
                    hasError = step.hasCurrentPasswordError || step.hasWrongCurrentPasswordError,
                    focusManager = focusManager,
                    onInputChange = { onEvent(RecoverKeysScreenUiEvent.EditCurrentPassword(it)) }
                )

                AnimatedVisibility(visible = step.hasWrongCurrentPasswordError) {
                    RecoverKeysWarningChip(
                        modifier = Modifier.padding(top = 10.dp),
                        text = stringResource(MR.strings.recover_account_step_password_wrong)
                    )
                }
            }

            RecoverKeysUiStep.FetchKeysFailed,
            RecoverKeysUiStep.MissingKeysResetOnlyGuest,
            RecoverKeysUiStep.InvalidKeysResetGuestAccount,
            RecoverKeysUiStep.TryRecoverGuestAccount,
            is RecoverKeysUiStep.RecoverGuestAccountFinalStep -> Unit
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RecoverKeysHeader(
    modifier: Modifier = Modifier,
    icon: Painter,
    isWarning: Boolean,
    title: String,
    info: String
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = if (isWarning) AppColors.errorBackground else AppColors.accent.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(44.dp),
                painter = icon,
                tint = if (isWarning) AppColors.redMain else AppColors.accent,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = info.annotatedBoldCompose(),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecoverKeysWarningChip(
    modifier: Modifier = Modifier,
    text: String
) {
    NoticeInfoChip(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = AppColors.errorBackground,
        iconTint = AppColors.redMain,
        icon = CoreAppIcons.Warning.asPainterResource(),
        text = text
    )
}

@Composable
private fun RecoverKeysConfirmButton(
    uiStep: RecoverKeysUiStep,
    isLoading: Boolean?,
    onClick: () -> Unit
) {
    val isReset = uiStep.isReset
    val isFinalRecovery = uiStep is RecoverKeysUiStep.RecoverGuestAccountFinalStep ||
            uiStep is RecoverKeysUiStep.RecoverClassicAccountFinalStep

    FilledButton(
        style = ButtonStyle.BIG,
        state = AppButtonState.loadingState(isLoading),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = animateColorAsState(
                if (isReset) MaterialTheme.colors.error else MaterialTheme.colors.primary
            ).value,
            contentColor = animateColorAsState(
                if (isReset) MaterialTheme.colors.onError else MaterialTheme.colors.onPrimary
            ).value
        ),
        icon = if (isFinalRecovery) CoreAppIcons.AutoFixHigh.asPainterResource() else null,
        onClick = onClick
    ) {
        Text(
            text = stringResource(
                when {
                    uiStep is RecoverKeysUiStep.FetchKeysFailed -> MR.strings.button_retry
                    isReset -> MR.strings.recover_account_button_reset
                    isFinalRecovery -> MR.strings.recover_account_button_recover
                    else -> MR.strings.recover_account_button_unlock
                }
            )
        )
    }
}

private val RecoverKeysUiStep.isReset: Boolean
    get() = when (this) {
        RecoverKeysUiStep.MissingKeysResetOnlyGuest,
        RecoverKeysUiStep.InvalidKeysResetGuestAccount,
        is RecoverKeysUiStep.ResetClassicAccount -> true

        RecoverKeysUiStep.FetchKeysFailed,
        RecoverKeysUiStep.TryRecoverGuestAccount,
        is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
        is RecoverKeysUiStep.TryRecoverClassicAccount,
        is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> false
    }

private val RecoverKeysUiStep.headerIcon: Painter
    @Composable
    get() = when (this) {
        RecoverKeysUiStep.FetchKeysFailed -> CoreAppIcons.WifiOff

        RecoverKeysUiStep.MissingKeysResetOnlyGuest,
        RecoverKeysUiStep.InvalidKeysResetGuestAccount,
        is RecoverKeysUiStep.ResetClassicAccount -> CoreAppIcons.Warning

        RecoverKeysUiStep.TryRecoverGuestAccount -> CoreAppIcons.Key
        is RecoverKeysUiStep.TryRecoverClassicAccount -> CoreAppIcons.Password

        is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
        is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> CoreAppIcons.LockOpen
    }.asPainterResource()

private val RecoverKeysUiStep.titleRes: StringResource
    get() = when (this) {
        RecoverKeysUiStep.FetchKeysFailed -> MR.strings.recover_account_title_fetch_failed
        RecoverKeysUiStep.MissingKeysResetOnlyGuest -> MR.strings.recover_account_title_missing_keys
        RecoverKeysUiStep.InvalidKeysResetGuestAccount -> MR.strings.recover_account_title_invalid_keys
        is RecoverKeysUiStep.ResetClassicAccount -> if (canRecoverOldKeys) {
            MR.strings.recover_account_title_reset
        } else {
            MR.strings.recover_account_title_missing_keys
        }

        RecoverKeysUiStep.TryRecoverGuestAccount,
        is RecoverKeysUiStep.TryRecoverClassicAccount -> MR.strings.recover_account_title_try_recover

        is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
        is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> MR.strings.recover_account_step_password_correct
    }

private val RecoverKeysUiStep.infoRes: StringResource
    get() = when (this) {
        RecoverKeysUiStep.FetchKeysFailed -> MR.strings.recover_account_info_fetch_failed
        RecoverKeysUiStep.MissingKeysResetOnlyGuest -> MR.strings.recover_account_info_missing_keys_guest
        RecoverKeysUiStep.InvalidKeysResetGuestAccount -> MR.strings.recover_account_info_invalid_keys_guest
        is RecoverKeysUiStep.ResetClassicAccount -> if (canRecoverOldKeys) {
            MR.strings.recover_account_info_reset_classic
        } else {
            MR.strings.recover_account_info_missing_keys_classic
        }

        RecoverKeysUiStep.TryRecoverGuestAccount -> MR.strings.recover_account_info_try_recover_guest
        is RecoverKeysUiStep.TryRecoverClassicAccount -> MR.strings.recover_account_info_try_recover_classic
        is RecoverKeysUiStep.RecoverGuestAccountFinalStep -> MR.strings.recover_account_info_recover_guest
        is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> MR.strings.recover_account_info_recover_classic
    }

private val RecoverKeysUiStep.skipButtonTextRes: StringResource?
    get() = when (this) {
        is RecoverKeysUiStep.TryRecoverClassicAccount -> MR.strings.recover_account_button_skip_to_reset
        is RecoverKeysUiStep.ResetClassicAccount -> if (canRecoverOldKeys) {
            MR.strings.recover_account_button_skip_to_recover
        } else {
            null
        }

        RecoverKeysUiStep.FetchKeysFailed,
        RecoverKeysUiStep.MissingKeysResetOnlyGuest,
        RecoverKeysUiStep.InvalidKeysResetGuestAccount,
        RecoverKeysUiStep.TryRecoverGuestAccount,
        is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
        is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> null
    }
