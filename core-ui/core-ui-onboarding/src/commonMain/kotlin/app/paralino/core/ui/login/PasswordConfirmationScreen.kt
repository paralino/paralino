package app.paralino.core.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.login.password_confirmation.PasswordConfirmationScreenState
import app.paralino.core.presentation.login.password_confirmation.PasswordConfirmationScreenUiEvent
import app.paralino.core.presentation.login.password_confirmation.PasswordConfirmationViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PasswordInputField
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PasswordConfirmationScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: PasswordConfirmationViewModel
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    uiState.alertDialog?.let { dialog ->
        dialog.Show { action ->
            vm.onEvent(
                PasswordConfirmationScreenUiEvent.DialogAction(
                    AlertDialogAction(dialog = dialog, action = action)
                )
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.password_confirmation_screen_title),
                navigationIcon = null,
                actions = {
                    FilledButton(
                        modifier = Modifier.padding(end = 4.dp).size(28.dp),
                        onClick = { vm.onEvent(PasswordConfirmationScreenUiEvent.Logout) },
                        icon = CoreAppIcons.Logout.asPainterResource(),
                        style = ButtonStyle.ICON(
                            iconSize = 24.dp
                        ),
                        elevation = 0.dp,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onBackground
                        ),
                        content = null
                    )
                }
            )
        },
    ) {
        PasswordConfirmationScreen(
            modifier = Modifier.padding(it),
            state = uiState,
            onPasswordChange = {
                vm.onEvent(PasswordConfirmationScreenUiEvent.EditPassword(it))
            },
            onConfirm = {
                vm.onEvent(PasswordConfirmationScreenUiEvent.ConfirmPassword)
            }
        )
    }
}

@Composable
private fun PasswordConfirmationScreen(
    modifier: Modifier = Modifier,
    state: PasswordConfirmationScreenState,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(MR.strings.password_confirmation_header),
            style = MaterialTheme.typography.h5
        )
        Spacer(modifier = Modifier.size(30.dp))
        
        Text(text = stringResource(MR.strings.password_confirmation_caption))
        Spacer(modifier = Modifier.size(20.dp))
        
        PasswordInputField(
            password = state.password,
            hint = stringResource(MR.strings.login_placeholder_password),
            hasError = state.hasPasswordError,
            imeAction = ImeAction.Done,
            focusManager = focusManager,
            onInputChange = { onPasswordChange(it) }
        )

        Spacer(modifier = Modifier.size(40.dp))
        
        FilledButton(
            style = ButtonStyle.BIG,
            state = if (state.confirmLoading == null)
                AppButtonState.CONTENT
            else
                AppButtonState.loadingStateWithCheck(state.confirmLoading),
            onClick = {
                focusManager.clearFocus()
                onConfirm()
            }
        ) {
            Text(text = stringResource(MR.strings.password_confirmation_button_confirm))
        }
    }
}
