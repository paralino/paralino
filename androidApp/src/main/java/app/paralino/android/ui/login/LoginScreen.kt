package app.paralino.android.ui.login

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.*
import app.paralino.android.ui.navigation.Deeplink
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.shared.getFormValidationErrorText
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.android.utils.extensions.openUri
import app.paralino.core.presentation.login.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalTextApi::class)
@Composable
fun LoginScreen(
    appState: MainAppState,
    startScreenUiType: LoginScreenUiType,
    vm: LoginViewModel = getViewModel { parametersOf(startScreenUiType) },
) {

    val uiState by vm.uiState.collectAsState(initial = LoginScreenState(uiType = startScreenUiType))
    val focusManager = LocalFocusManager.current
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    LaunchedEffect(key1 = vm) {
        showDialog = if (uiState.hasGuestAccount) MainAlertDialog.GuestAccountRecover else null
    }

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

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is LoginScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is LoginScreenEvent.LoginError -> {
                    val errorText = getErrorText(context, event.errorType)
                    appState.showSnackbar(errorText)
                }
                is LoginScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                LoginScreenEvent.UserReady -> {
//                    appState.navController.navigate(AppScreen.DashboardRoot.routeSignature)
                }
                LoginScreenEvent.PasswordResetDone -> {
                    appState.showSnackbar(context.getString(R.string.login_info_password_reset_done))
                    delay(500)
                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.GuestAccountCreating -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.proceed()
            }
        }
        is MainAlertDialog.GuestAccountRecover -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.recoverGuestAccount()
                else if (it == MainAlertDialog.Buttons.buttonCancel) {
                    vm.dismissGuestAccount()
                } else {
                    vm.hideGuestAccountDialog()
                }
            }
        }
        else -> {}
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

        Spacer(modifier = Modifier.size(10.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
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
                            .alpha(if(uiState.canUseEmail) 1f else 0.4f)
                            .padding(end = 10.dp)
                            .fillMaxWidth(0.5f),
                        icon = AppIcons.Person,
                        title = stringResource(id = R.string.login_signup_account_type_regular),
                        description = stringResource(id = R.string.login_signup_account_type_regular_info),
                        focusDescription = false,
                        state = signUpRegularSwitchState,
                        onStateChange = {
                            vm.setScreenUiType(LoginScreenUiType.SIGNUP)
                        }
                    )
                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = AppIcons.Incognito,
                        title = stringResource(id = R.string.login_signup_account_type_guest),
                        description = stringResource(id = R.string.login_signup_account_type_guest_info),
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
                        hint = stringResource(id = R.string.login_placeholder_email),
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
                            hint = stringResource(id = R.string.login_placeholder_password),
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
                            hint = stringResource(id = R.string.login_placeholder_password_confirm),
                            hasError = uiState.hasPasswordConfirmError,
                            focusManager = focusManager,
                            onInputChange = { vm.onPasswordConfirmChange(it) }
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.uiType == LoginScreenUiType.LOGIN,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(id = R.string.login_button_forgot_password),
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
                    if (uiState.uiType == LoginScreenUiType.SIGNUP_GUEST) {
                        showDialog = MainAlertDialog.GuestAccountCreating
                    } else {
                        vm.proceed()
                    }
                }
            ) {
                val text = when (uiState.uiType) {
                    LoginScreenUiType.LOGIN -> stringResource(id = R.string.login_button_login)
                    LoginScreenUiType.SIGNUP -> stringResource(id = R.string.login_button_create_account)
                    LoginScreenUiType.SIGNUP_GUEST -> stringResource(id = R.string.login_button_create_guest_account)
                    LoginScreenUiType.FORGOT_PASSWORD -> stringResource(id = R.string.login_button_reset_password)
                }
                Text(text = text)
            }
        }

        TermsAndPrivacyElement(uiType = uiState.uiType)

        Spacer(modifier = Modifier.weight(1f))

        if(uiState.canUseEmail) {
            val switchText = when (uiState.uiType) {
                LoginScreenUiType.LOGIN -> stringResource(id = R.string.login_signup_switch_button_signup)
                LoginScreenUiType.SIGNUP, LoginScreenUiType.SIGNUP_GUEST -> stringResource(id = R.string.login_signup_switch_button_login)
                LoginScreenUiType.FORGOT_PASSWORD -> stringResource(id = R.string.login_signup_switch_button_login)
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
        }
    }
}

@Composable
fun ColumnScope.TermsAndPrivacyElement(uiType: LoginScreenUiType) {
    val text = stringResource(id = R.string.login_signup_terms_privacy_text)
    val privacy = stringResource(id = R.string.login_signup_terms_privacy_privacy)
    val terms = stringResource(id = R.string.login_signup_terms_privacy_terms)
    val privacyStart = text.indexOf(privacy)
    val privacyEnd = text.indexOf(privacy) + privacy.length
    val termsStart = text.indexOf(terms)
    val termsEnd = text.indexOf(terms) + terms.length

    val context = LocalContext.current
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
                context.openUri(Deeplink.privacyPolicyUrl)
            } else if (it in termsStart..termsEnd) {
                context.openUri(Deeplink.termsOfUseUrl)
            }
        }
    }
}

@Composable
fun getStepText(stepInfo: SignUpStepInfo?): String {
    return when (stepInfo) {
        SignUpStepInfo.Info.START_LOGIN -> stringResource(id = R.string.login_step_logIn)
        SignUpStepInfo.Info.START_SIGNUP -> stringResource(id = R.string.login_step_signUp)
        SignUpStepInfo.Info.START_PASSWORD_RESET -> stringResource(id = R.string.login_step_generic_loading)
        SignUpStepInfo.Info.CRYPTO_SETUP -> stringResource(id = R.string.login_step_encryption)
        SignUpStepInfo.Info.USER_SETUP -> stringResource(id = R.string.login_step_user_setup)
        SignUpStepInfo.Info.DONE -> stringResource(id = R.string.login_step_allDone)
        SignUpStepInfo.Error.LOGIN -> stringResource(id = R.string.login_step_error_logIn)
        SignUpStepInfo.Error.SIGN_UP -> stringResource(id = R.string.login_step_error_signUp)
        SignUpStepInfo.Error.PASSWORD_RESET -> stringResource(id = R.string.error_something_wrong)
        SignUpStepInfo.Error.CRYPTO -> stringResource(id = R.string.login_step_error_encryption_signUp)
        SignUpStepInfo.Error.USER_DATA -> stringResource(id = R.string.login_step_error_user_setup)
        SignUpStepInfo.Error.RECOVER_GUEST -> stringResource(id = R.string.login_step_error_recoverGuest)
        null -> ""
    }
}

fun getErrorText(context: Context, error: SignUpErrorType): String {
    return when (error) {
        SignUpErrorType.ACCOUNT_CONFLICT -> context.getString(R.string.form_invalid_email_conflict_description)
        SignUpErrorType.CREDENTIALS -> context.getString(R.string.form_invalid_credentials_description)
        SignUpErrorType.CREDENTIALS_GUEST -> context.getString(R.string.login_step_error_recoverGuest)
        SignUpErrorType.LOGIN_CRYPTO -> context.getString(R.string.login_step_error_encryption_logIn)
        SignUpErrorType.SIGNUP_CRYPTO -> context.getString(R.string.login_step_error_encryption_signUp)
        SignUpErrorType.SIGNUP_DATA -> context.getString(R.string.login_step_error_user_setup_signUp)
        is SignUpErrorType.SOURCE -> error.error.errorString(context)
    }
}