package com.zbapp.locationtrack.ui.settings.passwordchange

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zbapp.core.presentation.settings.passwordchange.ChangePasswordScreenEvent
import com.zbapp.core.presentation.settings.passwordchange.ChangePasswordScreenState
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.AppButtonState
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.shared.getFormValidationErrorText
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangePasswordScreen(
    appState: MainAppState,
    vm: ChangePasswordViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = ChangePasswordScreenState())
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is ChangePasswordScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is ChangePasswordScreenEvent.RemotePasswordError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_email_conflict_description))
                }
                is ChangePasswordScreenEvent.CredentialsError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_credentials_description))
                }
                is ChangePasswordScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                ChangePasswordScreenEvent.PasswordChanged -> {
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
                title = { Text(text = stringResource(id = R.string.settings_account_edit_password)) },
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
                    password = uiState.oldPassword,
                    hint = stringResource(id = R.string.settings_account_edit_email_placeholder_password),
                    hasError = uiState.hasOldPasswordError,
                    focusManager = focusManager,
                    onInputChange = { vm.onOldPasswordChange(it) }
                )

                Spacer(modifier = Modifier.size(8.dp))

                PasswordInputField(
                    password = uiState.newPassword,
                    hint = stringResource(id = R.string.settings_account_edit_password_placeholder_password_new),
                    hasError = uiState.hasNewPasswordError,
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

                Spacer(modifier = Modifier.size(40.dp))

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

@Composable
fun PasswordInputField(
    modifier: Modifier = Modifier,
    password: String,
    hint: String,
    hasError: Boolean,
    focusManager: FocusManager = LocalFocusManager.current,
    passwordHidden: MutableState<Boolean> = rememberSaveable { mutableStateOf(true) },
    onInputChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .height(TextFieldDefaults.MinHeight),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        value = password,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password
        ),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus()
        },
        visualTransformation = if (passwordHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
        placeholder = {
            Text(
                text = hint,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        onValueChange = {
            onInputChange(it)
        },
        shape = MaterialTheme.shapes.small,
        isError = hasError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            unfocusedBorderColor = MaterialTheme.colors.surface,
        ),
        trailingIcon = {
            IconButton(onClick = { passwordHidden.value = !passwordHidden.value }) {
                Icon(
                    imageVector = if (passwordHidden.value) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null
                )
            }
        }
    )
}