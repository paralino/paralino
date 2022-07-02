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
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.Payload
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.presentation.login.recover.RecoverKeysScreenEvent
import com.zbapp.core.presentation.login.recover.RecoverKeysScreenState
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.*
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.shared.getFormValidationErrorText
import com.zbapp.locationtrack.ui.theme.AppColors
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.annotatedBold
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel


@Composable
fun RecoverKeysScreen(
    appState: MainAppState,
    vm: RecoverKeysViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
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
                    vm.resetAccount()
            }
        }
        is MainAlertDialog.RecoveryRecoverAccount -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.recoverAccount()
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

    when (val state = uiState) {
        Payload.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        is Payload.Error -> {
            DefaultEmptyState(content = stringResource(id = R.string.recover_account_screen_error))
        }
        is Payload.Success -> {

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
                    state = state.data,
                    onPasswordChange = {
                        vm.onOldPasswordChange(it)
                    },
                    onReset = {
                        showDialog = MainAlertDialog.RecoveryResetAccount
                    },
                    onRecover = {
                        if (state.data.recoverState.data != null) {
                            showDialog = MainAlertDialog.RecoveryRecoverAccount
                        } else {
                            vm.checkForKey()
                        }
                    }
                )
            }
        }
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

        if (state.canRecoverOldKey) {
            Text(text = stringResource(id = R.string.recover_account_header_recover_possible),
                style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.size(30.dp))
            Text(text = stringResource(id = R.string.recover_account_caption_recover_possible))
            Spacer(modifier = Modifier.size(10.dp))
            PasswordInputField(
                password = state.oldPassword,
                hint = stringResource(id = R.string.login_placeholder_password_old),
                hasError = state.hasOldPasswordError,
                imeAction = ImeAction.Next,
                focusManager = focusManager,
                onInputChange = { onPasswordChange(it) }
            )
        } else {
            Text(text = stringResource(id = R.string.recover_account_header_recover_not_possible),
                style = MaterialTheme.typography.h5)
        }

        AnimatedVisibility(
            visible = state.recoverState is Resource.Ready,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            val content = when (state.recoverState) {
                Resource.NotInitialized -> ""
                is Resource.Ready -> when (state.recoverState.data) {
                    null -> stringResource(id = R.string.recover_account_step_password_wrong)
                    else -> stringResource(id = R.string.recover_account_step_password_correct)
                }
            }

            Text(
                text = content,
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .animateContentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (state.recoverState) {
                            Resource.NotInitialized -> Color.Transparent
                            is Resource.Ready -> when (state.recoverState.data) {
                                null -> MaterialTheme.colors.error
                                else -> AppColors.accent.copy(alpha = 0.1f)
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }


        if (state.canRecoverOldKey) {
            Spacer(modifier = Modifier.size(40.dp))
            FilledButton(
                style = ButtonStyle.BIG,
                state = if (state.checkLoading == null)
                    AppButtonState.CONTENT
                else
                    AppButtonState.loadingStateWithCheck(state.checkLoading!!),
                icon = if (state.recoverState.data != null) Icons.Rounded.AutoFixHigh else null,
                onClick = {
                    focusManager.clearFocus()
                    onRecover()
                }
            ) {
                if (state.recoverState.data != null)
                    Text(text = stringResource(id = R.string.recover_account_button_recover))
                else
                    Text(text = stringResource(id = R.string.recover_account_button_check))
            }
        }

        Spacer(modifier = Modifier.size(40.dp))

        AnimatedVisibility(
            visible = !state.canRecoverOldKey || (state.recoverState is Resource.Ready && state.recoverState.data == null),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Column {
                Spacer(modifier = Modifier.size(20.dp))

                if (state.canRecoverOldKey)
                    Text(text = stringResource(id = R.string.recover_account_caption_recover_not_possible).annotatedBold())
                else
                    Text(text = stringResource(id = R.string.recover_account_caption_reset).annotatedBold())

                Spacer(modifier = Modifier.size(10.dp))
                FilledButton(
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.onError,
                        contentColor = Color.White),
                    style = ButtonStyle.BIG,
                    state = if (state.resetLoading == null)
                        AppButtonState.CONTENT
                    else
                        AppButtonState.loadingStateWithCheck(state.resetLoading!!),
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

}