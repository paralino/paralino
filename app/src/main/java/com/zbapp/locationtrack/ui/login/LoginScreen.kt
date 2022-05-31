package com.zbapp.locationtrack.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.common.FormValidation
import com.zbapp.core.presentation.login.LoginScreenEvent
import com.zbapp.core.presentation.login.LoginScreenState
import com.zbapp.core.presentation.login.LoginScreenUiType
import com.zbapp.core.repo.user.SignUpStep
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.AppButtonState
import com.zbapp.locationtrack.ui.components.BottomSheetsScreen
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.components.ImageModelImage
import com.zbapp.locationtrack.ui.navigation.AppScreen
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.theme.AppColors
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun LoginScreen(
    appState: MainAppState,
    vm: LoginViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = LoginScreenState())
    val focusManager = LocalFocusManager.current
    var passwordHidden by remember(uiState.uiType) { mutableStateOf(true) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is LoginScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is LoginScreenEvent.ForgotPasswordError -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is LoginScreenEvent.LoginError -> {

                }
                is LoginScreenEvent.SignUpError -> {

                }
                LoginScreenEvent.UserReady -> {
                    appState.navController.navigate(AppScreen.DashboardRoot.routeSignature)
                }
                is LoginScreenEvent.ValidationError -> {
                    when (event.validationResult.type) {
                        FormValidation.Email -> appState.showSnackbar(context.getString(R.string.form_invalid_email_description))
                        FormValidation.Password -> appState.showSnackbar(context.getString(R.string.form_invalid_password_length_description))
                        FormValidation.PasswordConfirm -> appState.showSnackbar(context.getString(R.string.form_invalid_password_match_description))
                    }
                }
                null -> { /* do nothing */
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

        Spacer(modifier = Modifier.size(50.dp))

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
                    hasError = false,
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

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TextFieldDefaults.MinHeight),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                value = uiState.email,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email),
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
                placeholder = {
                    Text(
                        stringResource(id = R.string.login_placeholder_email),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                onValueChange = {
                    vm.onEmailChange(it)
                },
                shape = MaterialTheme.shapes.small,
                isError = uiState.hasEmailError,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    unfocusedBorderColor = MaterialTheme.colors.surface,
                )
            )

            Spacer(modifier = Modifier.size(8.dp))

            AnimatedVisibility(
                visible = uiState.uiType != LoginScreenUiType.FORGOT_PASSWORD,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TextFieldDefaults.MinHeight),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                    value = uiState.password,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions {
                        focusManager.clearFocus()
                    },
                    visualTransformation = if (passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
                    placeholder = {
                        Text(
                            stringResource(id = R.string.login_placeholder_password),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onValueChange = {
                        vm.onPasswordChange(it)
                    },
                    shape = MaterialTheme.shapes.small,
                    isError = uiState.hasPasswordError,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        unfocusedBorderColor = MaterialTheme.colors.surface,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordHidden = !passwordHidden }) {
                            Icon(
                                imageVector = if (passwordHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
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

            val stepText = getStepText(uiState.signUpStep, uiState.uiType)

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
fun getStepText(signUpStep: SignUpStep?, uiType: LoginScreenUiType): String {
    return when (signUpStep) {
        SignUpStep.STARTING -> {
            when (uiType) {
                LoginScreenUiType.LOGIN -> stringResource(id = R.string.login_step_logIn)
                LoginScreenUiType.SIGNUP -> stringResource(id = R.string.login_step_signUp)
                LoginScreenUiType.FORGOT_PASSWORD -> stringResource(id = R.string.login_step_generic_loading)
            }
        }
        SignUpStep.GENERATING_KEYS -> stringResource(id = R.string.login_step_encryption)
        SignUpStep.INIT_USER_DATA -> stringResource(id = R.string.login_step_user_setup)
        SignUpStep.SAVE_USER -> stringResource(id = R.string.login_step_user_setup)
        SignUpStep.ALL_DONE -> stringResource(id = R.string.login_step_allDone)
        null -> ""
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

    val focusManager = LocalFocusManager.current

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

        OutlinedTextField(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            value = name,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Down)
            },
            placeholder = {
                Text(
                    stringResource(id = R.string.login_placeholder_username_or_random),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onValueChange = { onTextChange(it) },
            shape = MaterialTheme.shapes.small,
            isError = hasError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface,
                unfocusedBorderColor = MaterialTheme.colors.surface,
            )
        )
    }
}