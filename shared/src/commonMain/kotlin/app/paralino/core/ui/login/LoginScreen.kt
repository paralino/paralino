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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.login.LoginScreenEvent
import app.paralino.core.presentation.login.LoginScreenState
import app.paralino.core.presentation.login.LoginScreenUiEvent
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.SignUpStepInfo
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.PasswordValidationElement
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import paralino.shared.generated.resources.Res
import paralino.shared.generated.resources.ic_launcher_foreground

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LoginScreenCore(
    appState: CoreUiState,
    vm: LoginViewModel,
    onExit: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState(initial = LoginScreenState(uiType = vm.initialUiType))
    val focusManager = LocalFocusManager.current

    val signUpRegularSwitchState by rememberSeekbarState(
        key = uiState.uiType,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(uiState.uiType == LoginScreenUiType.SIGNUP)
        )
    )

    val signUpGuestSwitchState by rememberSeekbarState(
        key = uiState.uiType,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(uiState.uiType == LoginScreenUiType.SIGNUP_GUEST)
        )
    )

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
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .verticalScroll(rememberScrollState()),
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

            AnimatedVisibility(
                visible = uiState.uiType.isCreatingAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LargeSeekbarSwitch(
                        modifier = Modifier
                            .alpha(if (uiState.canUseEmail) 1f else 0.4f)
                            .padding(end = 10.dp)
                            .fillMaxWidth(0.5f),
                        icon = CoreAppIcons.Person.asPainterResource(),
                        title = stringResource(resource = MR.strings.login_signup_account_type_regular),
                        description = stringResource(resource = MR.strings.login_signup_account_type_regular_info),
                        focusDescription = false,
                        state = signUpRegularSwitchState,
                        elevation = if (uiState.canUseEmail) 16.dp else 0.dp,
                        onStateChange = {
                            vm.setScreenUiType(LoginScreenUiType.SIGNUP)
                        }
                    )
                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = CoreAppIcons.Incognito.asPainterResource(),
                        title = stringResource(resource = MR.strings.login_signup_account_type_guest),
                        description = stringResource(resource = MR.strings.login_signup_account_type_guest_info),
                        focusDescription = false,
                        state = signUpGuestSwitchState,
                        onStateChange = {
                            vm.setScreenUiType(LoginScreenUiType.SIGNUP_GUEST)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.size(30.dp))

            AnimatedVisibility(
                visible = uiState.uiType != LoginScreenUiType.SIGNUP_GUEST,
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
                                MaterialTheme.colors.error
                            else
                                AppColors.accent.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.size(40.dp))

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
                    LoginScreenUiType.SIGNUP -> stringResource(resource = MR.strings.login_button_create_account)
                    LoginScreenUiType.SIGNUP_GUEST -> stringResource(resource = MR.strings.login_button_create_guest_account)
                    LoginScreenUiType.FORGOT_PASSWORD -> stringResource(resource = MR.strings.login_button_reset_password)
                }
                Text(text = text)
            }
        }

        TermsAndPrivacyElement(uiType = uiState.uiType)

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.canUseEmail) {
            val switchText = when (uiState.uiType) {
                LoginScreenUiType.LOGIN -> stringResource(resource = MR.strings.login_signup_switch_button_signup)
                LoginScreenUiType.SIGNUP, LoginScreenUiType.SIGNUP_GUEST -> stringResource(resource = MR.strings.login_signup_switch_button_login)
                LoginScreenUiType.FORGOT_PASSWORD -> stringResource(resource = MR.strings.login_signup_switch_button_login)
            }

            Text(
                text = switchText.annotatedBold(),
                style = MaterialTheme.typography.button,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        vm.toggleLoginSignup()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ColumnScope.TermsAndPrivacyElement(uiType: LoginScreenUiType) {
    val text = stringResource(resource = MR.strings.login_signup_terms_privacy_text)
    val privacy = stringResource(resource = MR.strings.login_signup_terms_privacy_privacy)
    val terms = stringResource(resource = MR.strings.login_signup_terms_privacy_terms)
    val privacyStart = text.indexOf(privacy)
    val privacyEnd = text.indexOf(privacy) + privacy.length
    val termsStart = text.indexOf(terms)
    val termsEnd = text.indexOf(terms) + terms.length

    val uriHandler = LocalUriHandler.current
    AnimatedVisibility(
        visible = uiType == LoginScreenUiType.SIGNUP || uiType == LoginScreenUiType.SIGNUP_GUEST,
        modifier = Modifier.fillMaxWidth()
    ) {
        ClickableText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 10.dp, bottom = 30.dp)
                .align(Alignment.CenterHorizontally),
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                    )
                ) {
                    append(text)
                }
                addStyle(
                    style = SpanStyle(
                        color = if (!isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ), start = privacyStart, end = privacyEnd
                )
                addStyle(
                    style = SpanStyle(
                        color = if (!isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ), start = termsStart, end = termsEnd
                )
            }
        ) {
            if (it in privacyStart..privacyEnd) {
                uriHandler.openUri(BackendResourceIDs.privacyPolicyUrl)
            } else if (it in termsStart..termsEnd) {
                uriHandler.openUri(BackendResourceIDs.termsOfUseUrl)
            }
        }
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