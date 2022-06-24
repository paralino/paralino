package com.zbapp.locationtrack.ui.login.recover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.presentation.login.recover.RecoverKeysScreenEvent
import com.zbapp.core.presentation.login.recover.RecoverKeysScreenState
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.AppButtonState
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.components.MainAlertDialog
import com.zbapp.locationtrack.ui.components.PasswordInputField
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.shared.getFormValidationErrorText
import com.zbapp.locationtrack.ui.theme.AppColors
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel


@Composable
fun RecoverKeysScreen(
    appState: MainAppState,
    vm: RecoverKeysViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = RecoverKeysScreenState())
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is RecoverKeysScreenEvent.Error -> {
                    appState.showSnackbar(event.exception.errorString(context))
                }
                is RecoverKeysScreenEvent.CredentialsError -> {
                    appState.showSnackbar(context.getString(R.string.form_invalid_credentials_description))
                }
                is RecoverKeysScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                RecoverKeysScreenEvent.PasswordChanged -> {
                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.RecoveryResetAccount -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.proceed()
            }
        }
        is MainAlertDialog.RecoveryRecoverAccount -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.proceed()
            }
        }
        is MainAlertDialog.Logout -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.logout()
            }
        }
        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.recover_account_screen_title)) },
                actions = {
                    IconButton(
                        onClick = {
                            showDialog = MainAlertDialog.Logout
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) {

        RecoverKeysScreen(
            modifier = Modifier.padding(it),
            state = uiState,
            onPasswordChange = {
                vm.onOldPasswordChange(it)
            },
            onReset = {
                showDialog = MainAlertDialog.RecoveryResetAccount
            },
            onRecover = {
                if (uiState.recoverState.data == true) {
                    showDialog = MainAlertDialog.RecoveryRecoverAccount
                } else {
                    vm.checkForKey()
                }
            }
        )
    }

}

@Composable
fun RecoverKeysScreen(
    modifier: Modifier = Modifier,
    state: RecoverKeysScreenState,
    onPasswordChange: (String) -> Unit,
    onReset: () -> Unit,
    onRecover: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier
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

        Text(text = "Your keys appears to be corrupted or missing")

        if (state.canRecoverOldKey) {
            Text(text = "There are some old keys, enter old password to recover")
            PasswordInputField(
                password = state.oldPassword,
                hint = stringResource(id = R.string.login_placeholder_password_old),
                hasError = state.hasOldPasswordError,
                imeAction = ImeAction.Next,
                focusManager = focusManager,
                onInputChange = { onPasswordChange(it) }
            )
        } else {
            Text(text = "This will delete all previous data in your account!")
        }

        Spacer(modifier = Modifier.size(80.dp))

        AnimatedVisibility(
            visible = state.recoverState is Resource.Ready,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "stepText",
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .animateContentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (state.recoverState.data == false)
                            MaterialTheme.colors.error
                        else
                            AppColors.accent.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }


        if (state.canRecoverOldKey) {
            Spacer(modifier = Modifier.size(40.dp))
            FilledButton(
                style = ButtonStyle.BIG,
                state = if (state.actionLoading == null)
                    AppButtonState.CONTENT
                else
                    AppButtonState.loadingStateWithCheck(state.actionLoading!!),
                onClick = {
                    focusManager.clearFocus()
                    onRecover()
                }
            ) {
                if (state.recoverState.data == true)
                    Text(text = stringResource(id = R.string.recover_account_button_recover))
                else
                    Text(text = stringResource(id = R.string.recover_account_button_check))
            }
        }

        Spacer(modifier = Modifier.size(40.dp))

        AnimatedVisibility(
            visible = !state.canRecoverOldKey || state.recoverState.data == false,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            FilledButton(
                style = ButtonStyle.BIG,
                state = if (state.actionLoading == null)
                    AppButtonState.CONTENT
                else
                    AppButtonState.loadingStateWithCheck(state.actionLoading!!),
                onClick = {
                    focusManager.clearFocus()
                    onReset()
                }
            ) {
                Text(text = stringResource(id = R.string.recover_account_button_reset))
            }
        }

    }

}