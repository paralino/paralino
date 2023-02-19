package app.paralino.android.ui.settings.emailchange

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.settings.emailchange.ChangeEmailScreenEvent
import app.paralino.core.presentation.settings.emailchange.ChangeEmailScreenState
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.AppButtonState
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.MainInputField
import app.paralino.android.ui.components.PasswordInputField
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.shared.getFormValidationErrorText
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangeEmailScreen(
    appState: MainAppState,
    vm: ChangeEmailViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = ChangeEmailScreenState(currentEmail = ""))
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is ChangeEmailScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is ChangeEmailScreenEvent.EmailError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_email_conflict_description))
                }
                is ChangeEmailScreenEvent.CredentialsError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_credentials_description))
                }
                is ChangeEmailScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                ChangeEmailScreenEvent.EmailChanged -> {
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
                title = { Text(text = stringResource(id = R.string.settings_account_edit_email)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = AppIcons.Close,
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

                MainInputField(
                    text = uiState.email,
                    hint = stringResource(id = R.string.settings_account_edit_email_placeholder_email),
                    hasError = uiState.hasEmailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onInputChange = { vm.onEmailChange(it) }
                )

                Spacer(modifier = Modifier.size(8.dp))

                PasswordInputField(
                    password = uiState.password,
                    hint = stringResource(id = R.string.settings_account_edit_email_placeholder_password),
                    hasError = uiState.hasPasswordError,
                    focusManager = focusManager,
                    onInputChange = { vm.onPasswordChange(it) }
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
                    Text(text = stringResource(id = R.string.settings_account_edit_email_button))
                }

            }

            Spacer(modifier = Modifier.weight(1f))

        }
    }


}

