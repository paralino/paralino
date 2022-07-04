package app.paralino.mobile.ui.login

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.login.*
import app.paralino.mobile.R
import app.paralino.mobile.ui.MainAppState
import app.paralino.mobile.ui.components.*
import app.paralino.mobile.ui.shared.errorString
import app.paralino.mobile.ui.shared.getFormValidationErrorText
import app.paralino.mobile.ui.theme.AppColors
import app.paralino.mobile.ui.theme.ButtonStyle
import app.paralino.mobile.utils.extensions.safeCast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LoginScreen(
    appState: MainAppState,
    startScreenUiType: LoginScreenUiType,
    vm: LoginViewModel = getViewModel { parametersOf(startScreenUiType) },
) {

    val uiState by vm.uiState.collectAsState(initial = LoginScreenState(uiType = startScreenUiType))
    val focusManager = LocalFocusManager.current

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

        Icon(
            imageVector = Icons.Rounded.PrivacyTip,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .align(alignment = Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {

            AnimatedVisibility(
                visible = uiState.uiType == LoginScreenUiType.SIGNUP,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAccountEditSmallItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TextFieldDefaults.MinHeight),
                    name = uiState.userName,
                    image = uiState.userImage,
                    hasError = uiState.hasNameError,
                    onImageClick = {
                        appState.uiScope.launch {
                            appState.showBottomSheetForResult(BottomSheetsScreen.EmojiPicker)
                                .safeCast<ImageModel>()?.let {
                                    vm.onUserImageChange(it)
                                }
                        }
                    },
                    onTextChange = {
                        vm.onUserNameChange(it)
                    }
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            MainInputField(
                text = uiState.email,
                hint = stringResource(id = R.string.login_placeholder_email),
                hasError = uiState.hasEmailError,
                keyboardType = KeyboardType.Email,
                imeAction = if(uiState.uiType == LoginScreenUiType.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next,
                onInputChange = { vm.onEmailChange(it) }
            )

            Spacer(modifier = Modifier.size(8.dp))

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

//            Spacer(modifier = Modifier.size(8.dp))

            AnimatedVisibility(
                visible = uiState.uiType == LoginScreenUiType.SIGNUP,
                modifier = Modifier.fillMaxWidth()
            ) {
                PasswordInputField(
                    modifier = Modifier.padding(top = 8.dp),
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
                    vm.proceed()
                }
            ) {
                val text = when (uiState.uiType) {
                    LoginScreenUiType.LOGIN -> stringResource(id = R.string.login_button_login)
                    LoginScreenUiType.SIGNUP -> stringResource(id = R.string.login_button_create_account)
                    LoginScreenUiType.FORGOT_PASSWORD -> stringResource(id = R.string.login_button_reset_password)
                }
                Text(text = text)
            }

        }

        Spacer(modifier = Modifier.weight(1f))

        val switchText = when (uiState.uiType) {
            LoginScreenUiType.LOGIN -> stringResource(id = R.string.login_signup_switch_button_signup)
            LoginScreenUiType.SIGNUP -> stringResource(id = R.string.login_signup_switch_button_login)
            LoginScreenUiType.FORGOT_PASSWORD -> stringResource(id = R.string.login_signup_switch_button_login)
        }

        Text(
            text = switchText,
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
        null -> ""
    }
}

fun getErrorText(context: Context, error: SignUpErrorType): String {
    return when (error) {
        SignUpErrorType.ACCOUNT_CONFLICT -> context.getString(R.string.form_invalid_email_conflict_description)
        SignUpErrorType.CREDENTIALS -> context.getString(R.string.form_invalid_credentials_description)
        SignUpErrorType.LOGIN_CRYPTO -> context.getString(R.string.login_step_error_encryption_logIn)
        SignUpErrorType.SIGNUP_CRYPTO -> context.getString(R.string.login_step_error_encryption_signUp)
        SignUpErrorType.SIGNUP_DATA -> context.getString(R.string.login_step_error_user_setup_signUp)
        is SignUpErrorType.SOURCE -> error.error.errorString(context)
    }
}

@Composable
fun UserAccountEditSmallItem(
    modifier: Modifier = Modifier,
    name: String,
    image: ImageModel,
    hasError: Boolean,
    onImageClick: () -> Unit,
    onTextChange: (String) -> Unit,
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageModelImage(
            imageModel = image,
            modifier = Modifier
                .fillMaxHeight()
                .width(TextFieldDefaults.MinHeight)
                .clickable { onImageClick() }
        )

        Spacer(modifier = Modifier.size(8.dp))

        MainInputField(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            text = name,
            hint = stringResource(id = R.string.login_placeholder_user_name),
            hasError = hasError,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            onInputChange = onTextChange
        )
    }
}