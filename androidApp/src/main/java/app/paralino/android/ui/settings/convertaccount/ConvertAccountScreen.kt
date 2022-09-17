package app.paralino.android.ui.settings.convertaccount

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.AppButtonState
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.MainInputField
import app.paralino.android.ui.components.PasswordInputField
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.shared.getFormValidationErrorText
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountScreenEvent
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountScreenState
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun ConvertAccountScreen(
    appState: MainAppState,
    vm: ConvertAccountViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = ConvertAccountScreenState())
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is ConvertAccountScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is ConvertAccountScreenEvent.EmailError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_email_conflict_description))
                }
                is ConvertAccountScreenEvent.CredentialsError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_credentials_description))
                }
                is ConvertAccountScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                ConvertAccountScreenEvent.AccountUpdated -> {
                    appState.navController.popBackStack()
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
                title = { Text(text = stringResource(id = R.string.settings_account_guest_convert)) },
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
                .padding(it)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.size(30.dp))

            Text(
                text = stringResource(id = R.string.settings_account_edit_guest_account_info),
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
                    hint = stringResource(id = R.string.login_placeholder_email),
                    hasError = uiState.hasEmailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onInputChange = { vm.onEmailChange(it) }
                )

                Spacer(modifier = Modifier.size(8.dp))

                PasswordInputField(
                    password = uiState.password,
                    hint = stringResource(id = R.string.login_placeholder_password),
                    hasError = uiState.hasPasswordError,
                    imeAction = ImeAction.Next,
                    focusManager = focusManager,
                    onInputChange = { vm.onPasswordChange(it) }
                )

                Spacer(modifier = Modifier.size(8.dp))

                PasswordInputField(
                    password = uiState.passwordConfirm,
                    hint = stringResource(id = R.string.login_placeholder_password_confirm),
                    hasError = uiState.hasPasswordConfirmError,
                    focusManager = focusManager,
                    onInputChange = { vm.onConfirmPasswordChange(it) }
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
                    Text(text = stringResource(id = R.string.settings_account_edit_guest_account_button))
                }

            }

            Spacer(modifier = Modifier.weight(1f))

        }
    }

}