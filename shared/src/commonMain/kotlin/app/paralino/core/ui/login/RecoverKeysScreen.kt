package app.paralino.core.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.presentation.login.recover.RecoverKeysScreenState
import app.paralino.core.presentation.login.recover.RecoverKeysScreenUiEvent
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun RecoverKeysScreenCore(
    appState: CoreUiState,
    vm: RecoverKeysViewModel
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    when (val dialog = uiState.data?.alertDialog) {
        is MainAlertDialog.RecoveryResetAccount -> {
            dialog.Show {
                vm.onEvent(RecoverKeysScreenUiEvent.HideDialog)
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.onEvent(RecoverKeysScreenUiEvent.ConfirmResetAccount)
                }
            }
        }

        is MainAlertDialog.RecoveryRecoverAccount -> {
            dialog.Show {
                vm.onEvent(RecoverKeysScreenUiEvent.HideDialog)
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.onEvent(RecoverKeysScreenUiEvent.ConfirmRecoverAccount)
                }
            }
        }

        is MainAlertDialog.Logout -> {
            dialog.Show {
                vm.onEvent(RecoverKeysScreenUiEvent.HideDialog)
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.onEvent(RecoverKeysScreenUiEvent.ConfirmLogout)
                }
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
            DefaultEmptyState(content = stringResource(MR.strings.recover_account_screen_error))
        }

        is Payload.Success -> {
            Scaffold(
                topBar = {
                    DefaultToolbar(
                        title = stringResource(resource = MR.strings.recover_account_screen_title),
                        navigationIcon = null,
                        actions = {
                            IconButton(
                                onClick = {
                                    vm.onEvent(RecoverKeysScreenUiEvent.Logout)
                                }
                            ) {
                                Icon(
                                    painter = CoreAppIcons.Logout.asPainterResource(),
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
                        vm.onEvent(RecoverKeysScreenUiEvent.EditOldPassword(it))
                    },
                    onReset = {
                        vm.onEvent(RecoverKeysScreenUiEvent.ResetAccount)
                    },
                    onRecover = {
                        if (state.data.recoverState.data != null) {
                            vm.onEvent(RecoverKeysScreenUiEvent.RecoverAccount)
                        } else {
                            vm.onEvent(RecoverKeysScreenUiEvent.CheckEncryptionKey)
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
            Text(
                text = stringResource(MR.strings.recover_account_header_recover_possible),
                style = MaterialTheme.typography.h5
            )
            Spacer(modifier = Modifier.size(30.dp))
            Text(text = stringResource(MR.strings.recover_account_caption_recover_possible))
            Spacer(modifier = Modifier.size(10.dp))
            PasswordInputField(
                password = state.oldPassword,
                hint = stringResource(MR.strings.login_placeholder_password_old),
                hasError = state.hasOldPasswordError,
                imeAction = ImeAction.Next,
                focusManager = focusManager,
                onInputChange = { onPasswordChange(it) }
            )
        } else {
            Text(
                text = stringResource(MR.strings.recover_account_header_recover_not_possible),
                style = MaterialTheme.typography.h5
            )
        }

        AnimatedVisibility(
            visible = state.recoverState is Resource.Ready,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            val content = when (state.recoverState) {
                Resource.NotInitialized -> ""
                is Resource.Ready -> when (state.recoverState.data) {
                    null -> stringResource(MR.strings.recover_account_step_password_wrong)
                    else -> stringResource(MR.strings.recover_account_step_password_correct)
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
                    AppButtonState.loadingStateWithCheck(state.checkLoading),
                icon = if (state.recoverState.data != null) CoreAppIcons.AutoFixHigh.asPainterResource() else null,
                onClick = {
                    focusManager.clearFocus()
                    onRecover()
                }
            ) {
                if (state.recoverState.data != null)
                    Text(text = stringResource(MR.strings.recover_account_button_recover))
                else
                    Text(text = stringResource(MR.strings.recover_account_button_check))
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
                    Text(text = stringResource(MR.strings.recover_account_caption_recover_not_possible).annotatedBold())
                else
                    Text(text = stringResource(MR.strings.recover_account_caption_reset).annotatedBold())

                Spacer(modifier = Modifier.size(10.dp))
                FilledButton(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error
                    ),
                    style = ButtonStyle.BIG,
                    state = if (state.resetLoading == null)
                        AppButtonState.CONTENT
                    else
                        AppButtonState.loadingStateWithCheck(state.resetLoading),
                    onClick = {
                        focusManager.clearFocus()
                        onReset()
                    }
                ) {
                    Text(text = stringResource(MR.strings.recover_account_button_reset))
                }
            }
        }
    }
}