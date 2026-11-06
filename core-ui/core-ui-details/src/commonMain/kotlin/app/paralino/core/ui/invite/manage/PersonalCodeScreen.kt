package app.paralino.core.ui.invite.manage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.paralinoid.PersonalCodeScreenUiEvent
import app.paralino.core.presentation.paralinoid.PersonalCodeViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PersonalCodeScreenCore(
    appState: CoreUiState,
    vm: PersonalCodeViewModel,
    askNotificationPermission: () -> Unit,
    onShare: (String) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    when (val dialog = uiState.alertDialog) {
        is MainAlertDialog.PersonalCodeDelete -> {
            dialog.Show {
                vm.onEvent(PersonalCodeScreenUiEvent.HideDialog)
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.onEvent(PersonalCodeScreenUiEvent.DisableCodeConfirm)
            }
        }

        else -> {}
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        this.items(uiState.errors, key = { it.toString() }) {
            ConditionErrorItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                data = it
            ) {
                when (it) {
                    ConditionErrorType.Network -> {
                        vm.showSnackbar(
                            text = MR.strings.error_no_connection_snackbar.localizedString()
                        )
                    }

                    ConditionErrorType.PermissionNotifications -> {
                        askNotificationPermission()
                    }

                    else -> {
                        /* do nothing */
                    }
                }
            }
        }

        if (uiState.errors.isNotEmpty()) {
            this.item (key = "spacing") {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }

        if (uiState.activeCode != null) {
            this.item(key = "activeCode") {
                PersonalCodeItemWithHints(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .animateItem()
                ) {
                    PersonalCodeItem(
                        modifier = this,
                        state = uiState.activeCode!!,
                        onCopy = {
                            vm.onEvent(PersonalCodeScreenUiEvent.CopyPersonalCode)
                            vm.showSnackbar(text = MR.strings.toast_copy_invite_code.localizedString())
                        },
                        onShare = onShare,
                        onDelete = {
                            vm.onEvent(PersonalCodeScreenUiEvent.DisableCode)
                        }
                    )
                }
            }
        } else if (uiState.inactiveCodes.isNotEmpty()) {
            this.item(key = "inactiveCode") {
                DefaultEmptyState(
                    modifier = Modifier.animateItem(),
                    icon = CoreAppIcons.AddLink.asPainterResource(),
                    content = stringResource(MR.strings.invite_manage_empty_description)
                )
            }
        }

        if (uiState.inactiveCodes.isNotEmpty()) {
            this.item(key = "inactive_header") {
                SettingsSectionHeader(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    name = stringResource(MR.strings.settings_section_personal_code_inactive_title),
                    count = uiState.inactiveCodes.size,
                    limit = null
                )
            }
            this.items(uiState.inactiveCodes, key = { it.id }) { code ->
                PersonalCodeItemInactive(
                    modifier = Modifier.animateItem(),
                    state = code
                ) {
                    vm.onEvent(PersonalCodeScreenUiEvent.ActivateCode(it))
                }
            }
            this.item(key = "inactive_spacer") {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        if (uiState.inactiveCodes.isEmpty() && uiState.activeCode == null) {
            this.item(key = "empty_state") {
                DefaultEmptyState(
                    icon = CoreAppIcons.AddLink.asPainterResource(),
                    content = stringResource(MR.strings.invite_manage_empty_description),
                    buttonText = stringResource(MR.strings.invite_create_button),
                    buttonState = if (uiState.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                    onClick = { vm.onEvent(PersonalCodeScreenUiEvent.CreateCode) }
                )
            }
        } else {
            this.item(key = "button") {
                if (uiState.canCreateAlternativeId) {
                    FilledButton(
                        modifier = Modifier.padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 10.dp),
                        style = ButtonStyle.BIG,
                        state = if (uiState.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                        icon = CoreAppIcons.AddLink.asPainterResource(),
                        onClick = { vm.onEvent(PersonalCodeScreenUiEvent.CreateCode) }
                    ) {
                        Text(stringResource(MR.strings.invite_create_new_button))
                    }
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier.padding(horizontal = 40.dp)
                                .padding(bottom = 20.dp),
                            text = stringResource(
                                MR.strings.invite_create_alternative_description,
                                "${uiState.maxCodes}"
                            ).annotatedBoldCompose(),
                            style = MaterialTheme.typography.caption,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}