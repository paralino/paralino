package app.paralino.core.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.settings.FormValidation
import app.paralino.core.presentation.login.LoginScreenEvent
import app.paralino.core.presentation.login.LoginScreenState
import app.paralino.core.presentation.login.LoginScreenUiEvent
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.SignUpStepInfo
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.FullWidthActionButton
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.PasswordValidationElement
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.splitRanges
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource
import paralino.shared.generated.resources.Res
import paralino.shared.generated.resources.ic_launcher_foreground

@Composable
fun LoginScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: LoginViewModel,
    onExit: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState(initial = LoginScreenState(uiType = vm.initialUiType))
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                LoginScreenEvent.UserReady -> {
//                    appState.navController.navigate(AppScreen.DashboardRoot.routeSignature)
                }

                LoginScreenEvent.PasswordResetDone -> {
                    vm.showSnackbar(MR.strings.login_info_password_reset_done.localizedString())
                    delay(500)
                    onExit()
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show { vm.onEvent(LoginScreenUiEvent.DialogAction(AlertDialogAction(dialog, it))) }
    }

    Column(
        modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.weight(0.2f))

        Image(
            painter = painterResource(resource = Res.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                .size(80.dp)
                .scale(1.3f)
                .align(alignment = Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.canUseEmail) {
                    LargeSeekbarSwitch(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .fillMaxWidth(0.5f),
                        icon = CoreAppIcons.PersonAdd.asPainterResource(),
                        title = stringResource(resource = MR.strings.login_signup_selection_signup_title),
                        description = stringResource(resource = MR.strings.login_signup_selection_signup_info),
                        focusDescription = false,
                        state = rememberSeekbarState(
                            key = uiState.uiType,
                            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = LargeSeekbarSwitchState.Switch(uiState.uiType == LoginScreenUiType.SIGNUP)
                            )
                        ).value,
                        onStateChange = {
                            vm.setScreenUiType(LoginScreenUiType.SIGNUP)
                        }
                    )

                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = CoreAppIcons.Login.asPainterResource(),
                        title = stringResource(resource = MR.strings.login_signup_selection_login_title),
                        description = stringResource(resource = MR.strings.login_signup_selection_login_info),
                        focusDescription = false,
                        state = rememberSeekbarState(
                            key = uiState.uiType,
                            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = LargeSeekbarSwitchState.Switch(uiState.uiType == LoginScreenUiType.LOGIN)
                            )
                        ).value,
                        onStateChange = {
                            vm.setScreenUiType(LoginScreenUiType.LOGIN)
                        }
                    )
                } else {
                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = CoreAppIcons.Incognito.asPainterResource(),
                        title = stringResource(resource = MR.strings.login_signup_selection_guest_title),
                        description = stringResource(resource = MR.strings.login_signup_selection_guest_info),
                        focusDescription = false,
                        state = rememberSeekbarState(
                            key = uiState.uiType,
                            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = null
                            )
                        ).value,
                        elevation = 0.dp,
                        onStateChange = {
                            vm.setScreenUiType(LoginScreenUiType.SIGNUP)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = !uiState.canUseEmail,
                modifier = Modifier.fillMaxWidth()
            ) {
                NoticeInfoChip(
                    modifier = Modifier
                        .padding(top = 20.dp),
                    text = stringResource(MR.strings.login_signup_guest_notice_message)
                )
            }

            if (uiState.canUseEmail) {
                Spacer(modifier = Modifier.size(40.dp))
            }

            AnimatedVisibility(
                visible = uiState.canUseEmail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    MainInputField(
                        text = uiState.email,
                        hint = stringResource(resource = MR.strings.login_placeholder_email),
                        hasError = uiState.hasEmailError,
                        keyboardType = KeyboardType.Email,
                        imeAction = if (uiState.uiType == LoginScreenUiType.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next,
                        onInputChange = { vm.onEmailChange(it) }
                    )

                    Spacer(modifier = Modifier.size(4.dp))

                    AnimatedVisibility(
                        visible = uiState.uiType != LoginScreenUiType.FORGOT_PASSWORD,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PasswordInputField(
                            password = uiState.password,
                            hint = stringResource(resource = MR.strings.login_placeholder_password),
                            hasError = uiState.hasPasswordError,
                            focusManager = focusManager,
                            onInputChange = { vm.onPasswordChange(it) }
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.uiType == LoginScreenUiType.SIGNUP,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PasswordInputField(
                            modifier = Modifier.padding(top = 4.dp),
                            password = uiState.passwordConfirm,
                            hint = stringResource(resource = MR.strings.login_placeholder_password_confirm),
                            hasError = uiState.hasPasswordConfirmError,
                            focusManager = focusManager,
                            onInputChange = { vm.onPasswordConfirmChange(it) }
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.uiType == LoginScreenUiType.SIGNUP,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PasswordValidationElement(
                            modifier = Modifier.padding(top = 12.dp),
                            minPasswordLength = FormValidation.Password.PasswordStrength.minPasswordLength,
                            hasMinPasswordLength = uiState.passwordValidation.minPasswordLength,
                            hasMinPasswordStrength = uiState.passwordValidation.passwordStrength.isOK,
                            doPasswordsMatch = uiState.passwordValidation.passwordsMatch
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.uiType == LoginScreenUiType.LOGIN,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(resource = MR.strings.login_button_forgot_password),
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    vm.toggleForgotPassword()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }

                }
            }

            AnimatedVisibility(
                visible = uiState.uiType == LoginScreenUiType.FORGOT_PASSWORD,
                modifier = Modifier.fillMaxWidth()
            ) {
                NoticeInfoChip(
                    modifier = Modifier
                        .padding(top = 12.dp),
                    text = stringResource(MR.strings.login_reset_password_info)
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            val stepText = getStepText(uiState.stepInfo)

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
                        .background(
                            if (uiState.stepInfo is SignUpStepInfo.Error)
                                AppColors.errorBackground
                            else
                                AppColors.accent.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.size(20.dp))

            FilledButton(
                style = ButtonStyle.BIG,
                state = AppButtonState.loadingState(uiState.actionLoading),
                onClick = {
                    focusManager.clearFocus()
                    vm.onEvent(LoginScreenUiEvent.LoginOrSignup)
                }
            ) {
                val text = when (uiState.uiType) {
                    LoginScreenUiType.LOGIN -> stringResource(resource = MR.strings.login_button_login)
                    LoginScreenUiType.SIGNUP -> {
                        if (uiState.canUseEmail) {
                            stringResource(resource = MR.strings.login_button_create_account)
                        } else {
                            stringResource(resource = MR.strings.login_button_create_guest_account)
                        }
                    }

                    LoginScreenUiType.FORGOT_PASSWORD -> stringResource(resource = MR.strings.login_button_reset_password)
                }
                Text(text = text)
            }
        }

        AnimatedVisibility(
            visible = uiState.uiType == LoginScreenUiType.SIGNUP && uiState.canUseEmail,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = stringResource(MR.strings.login_button_create_account_separator_text),
                        style = MaterialTheme.typography.caption
                    )
                    Divider(modifier = Modifier.weight(1f))
                }

                FullWidthActionButton(
                    modifier = Modifier
                        .zIndex(0f)
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    startIcon = CoreAppIcons.Incognito.asPainterResource(),
                    endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                    text = stringResource(MR.strings.login_button_create_guest_account),
                    onClick = {
                        focusManager.clearFocus()
                        vm.onEvent(LoginScreenUiEvent.CreateGuestAccount)
                    }
                )
            }
        }

        TermsAndPrivacyElement(uiType = uiState.uiType)

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ColumnScope.TermsAndPrivacyElement(uiType: LoginScreenUiType) {
    val textWithMarkers = stringResource(resource = MR.strings.login_signup_terms_privacy_text)
    val ranges = textWithMarkers.splitRanges("**")
    val cleanText = textWithMarkers.replace("**", "")

    val privacyRange = ranges.getOrNull(0)?.second
    val termsRange = ranges.getOrNull(1)?.second

    val annotatedText = buildAnnotatedString {
        withStyle(
            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Normal)
                .toSpanStyle().copy(
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        ) {
            append(cleanText)
        }
        privacyRange?.let { range ->
            addStyle(
                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold)
                    .toSpanStyle().copy(
                    color = if (!isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
                ),
                start = range.first,
                end = range.last
            )
            addLink(
                url = LinkAnnotation.Url(BackendResourceIDs.privacyPolicyUrl),
                start = range.first,
                end = range.last
            )
        }
        termsRange?.let { range ->
            addStyle(
                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold)
                    .toSpanStyle().copy(
                    color = if (!isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
                ),
                start = range.first,
                end = range.last
            )
            addLink(
                url = LinkAnnotation.Url(BackendResourceIDs.termsOfUseUrl),
                start = range.first,
                end = range.last
            )
        }
    }

    AnimatedVisibility(
        visible = uiType == LoginScreenUiType.SIGNUP,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 10.dp, bottom = 30.dp)
                .align(Alignment.CenterHorizontally),
            text = annotatedText,
            style = MaterialTheme.typography.button
        )
    }
}

@Composable
fun getStepText(stepInfo: SignUpStepInfo?): String {
    return when (stepInfo) {
        SignUpStepInfo.Info.START_LOGIN -> stringResource(resource = MR.strings.login_step_logIn)
        SignUpStepInfo.Info.START_SIGNUP -> stringResource(resource = MR.strings.login_step_signUp)
        SignUpStepInfo.Info.START_PASSWORD_RESET -> stringResource(resource = MR.strings.login_step_generic_loading)
        SignUpStepInfo.Info.CRYPTO_SETUP -> stringResource(resource = MR.strings.login_step_encryption)
        SignUpStepInfo.Info.USER_SETUP -> stringResource(resource = MR.strings.login_step_user_setup)
        SignUpStepInfo.Info.DONE -> stringResource(resource = MR.strings.login_step_allDone)
        SignUpStepInfo.Error.LOGIN -> stringResource(resource = MR.strings.login_step_error_logIn)
        SignUpStepInfo.Error.SIGN_UP -> stringResource(resource = MR.strings.login_step_error_signUp)
        SignUpStepInfo.Error.PASSWORD_RESET -> stringResource(resource = MR.strings.error_something_wrong)
        SignUpStepInfo.Error.CRYPTO -> stringResource(resource = MR.strings.login_step_error_encryption_signUp)
        SignUpStepInfo.Error.USER_DATA -> stringResource(resource = MR.strings.login_step_error_user_setup)
        SignUpStepInfo.Error.RECOVER_GUEST -> stringResource(resource = MR.strings.login_step_error_recoverGuest)
        null -> ""
    }
}