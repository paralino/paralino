package app.paralino.mobile.ui.login.passwordreset

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.navOptions
import app.paralino.core.presentation.login.passwordreset.ResetPasswordScreenEvent
import app.paralino.core.presentation.login.passwordreset.ResetPasswordScreenState
import app.paralino.mobile.R
import app.paralino.mobile.ui.MainAppState
import app.paralino.mobile.ui.components.AppButtonState
import app.paralino.mobile.ui.components.FilledButton
import app.paralino.mobile.ui.components.PasswordInputField
import app.paralino.mobile.ui.navigation.AppScreen
import app.paralino.mobile.ui.shared.errorString
import app.paralino.mobile.ui.shared.getFormValidationErrorText
import app.paralino.mobile.ui.theme.ButtonStyle
import app.paralino.mobile.utils.extensions.annotatedBold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ResetPasswordScreen(
    appState: MainAppState,
    userId: String,
    secret: String,
    vm: ResetPasswordViewModel = getViewModel { parametersOf(userId, secret) },
) {

    val uiState by vm.uiState.collectAsState(initial = ResetPasswordScreenState())
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is ResetPasswordScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is ResetPasswordScreenEvent.CredentialsError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_credentials_description))
                }
                is ResetPasswordScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                ResetPasswordScreenEvent.PasswordChanged -> {
                    appState.showSnackbar(context.getString(R.string.reset_password_success_message))
                    delay(500)
                    appState.navController.navigate(
                        AppScreen.AppStartRoot.Login.routeSignature,
                        navOptions {
                            popUpTo(AppScreen.AppStartRoot.PasswordReset.routeSignature) {
                                inclusive = true
                            }
                        }
                    )
                }
                ResetPasswordScreenEvent.InvalidToken -> {
                    appState.showSnackbar(context.getString(R.string.reset_password_error_token_message))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.reset_password_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            )
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
                    hint = stringResource(id = R.string.settings_account_edit_password_placeholder_password_new),
                    hasError = uiState.hasNewPasswordError,
                    imeAction = ImeAction.Next,
                    focusManager = focusManager,
                    onInputChange = { vm.onNewPasswordChange(it) }
                )

                Spacer(modifier = Modifier.size(8.dp))

                PasswordInputField(
                    password = uiState.confirmPassword,
                    hint = stringResource(id = R.string.settings_account_edit_password_placeholder_password_confirm),
                    hasError = uiState.hasConfirmPasswordError,
                    focusManager = focusManager,
                    onInputChange = { vm.onConfirmPasswordChange(it) }
                )

                Spacer(modifier = Modifier.size(80.dp))

                Text(text = stringResource(id = R.string.recover_account_caption_reset).annotatedBold())
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
                    Text(text = stringResource(id = R.string.settings_account_edit_password_button))
                }

            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

}
