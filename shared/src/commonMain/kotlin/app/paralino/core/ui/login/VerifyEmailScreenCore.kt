package app.paralino.core.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.login.emailverify.VerifyEmailScreenEvent
import app.paralino.core.presentation.login.emailverify.VerifyEmailScreenUiEvent
import app.paralino.core.presentation.login.emailverify.VerifyEmailViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VerifyEmailScreenCore(
    appState: CoreUiState,
    vm: VerifyEmailViewModel,
    openChangeEmail: () -> Unit
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                VerifyEmailScreenEvent.EditEmail -> {
                    openChangeEmail()
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                VerifyEmailScreenUiEvent.DialogAction(
                    AlertDialogAction(
                        dialog,
                        it
                    )
                )
            )
        }
    }

    ActionScaffold(
        modifier = Modifier.fillMaxSize(),
        buttonState = AppButtonState.loadingStateWithCheck(uiState.actionLoading),
        buttonText = stringResource(MR.strings.info_email_confirm_alert_button_resend),
        buttonIcon = null,
        onConfirm = {
            vm.onEvent(VerifyEmailScreenUiEvent.ResendVerification)
        },
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.info_email_confirm_alert_title),
                navigationIcon = null,
                actions = {
                    IconButton(
                        onClick = {
                            vm.onEvent(VerifyEmailScreenUiEvent.Logout)
                        }
                    ) {
                        Icon(
                            painter = CoreAppIcons.Logout.asPainterResource(),
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
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {
//            ConditionErrorItem(
//                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
//                icon = null,
//                text = stringResource(MR.strings.info_email_check_instructions),
//                endAction = null
//            )

            NoticeInfoChip(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 0.dp),
                icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                text = stringResource(MR.strings.info_email_check_instructions, uiState.currentEmail)
            )

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .padding(top = 8.dp),
                    text = stringResource(MR.strings.info_email_confirm_notice_message).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Start
                )
            }

            SettingsActionItem(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).padding(horizontal = 8.dp),
                name = stringResource(MR.strings.settings_account_edit_email_button),
                tag = uiState.currentEmail,
                icon = CoreAppIcons.Email.asPainterResource(),
                onClick = { vm.onEvent(VerifyEmailScreenUiEvent.EditCurrentEmail) }
            )

            AnimatedVisibility(uiState.isEmailVerificationShown) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp)
                ) {
                    MainInputField(
                        text = uiState.emailVerification,
                        hint = stringResource(MR.strings.info_email_confirm_verification_placeholder),
                        hasError = uiState.hasEmailVerificationError,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words,
                        focusManager = focusManager,
                        onInputChange = {
                            vm.onEvent(VerifyEmailScreenUiEvent.EditEmailVerification(it))
                        }
                    )
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            text = stringResource(MR.strings.info_email_confirm_manual_verification_notice).annotatedBoldCompose(),
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Start
                        )
                    }
                    FilledButton(
                        modifier = Modifier.padding(top = 20.dp),
                        style = ButtonStyle.BIG,
                        state = AppButtonState.loadingState(uiState.actionLoading),
                        onClick = { vm.onEvent(VerifyEmailScreenUiEvent.CheckVerification) }
                    ) {
                        Text(text = stringResource(MR.strings.button_verify))
                    }
                }
            }

            AnimatedVisibility(!uiState.isEmailVerificationShown) {
                SettingsActionItem(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    name = stringResource(MR.strings.info_email_confirm_manual_verification_button),
                    icon = CoreAppIcons.Code.asPainterResource(),
                    type = SettingsActionItemType.Action,
                    onClick = { vm.onEvent(VerifyEmailScreenUiEvent.ToggleManualVerificationInput) }
                )
            }
        }
    }

    if (uiState.fullLoading) {
        LoadingDialog(
            text = stringResource(MR.strings.login_step_generic_loading),
            progress = null
        )
    }
}