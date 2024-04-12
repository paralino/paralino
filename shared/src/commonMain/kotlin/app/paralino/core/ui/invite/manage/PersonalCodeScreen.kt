package app.paralino.core.ui.invite.manage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenEvent
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonalCodeScreenCore(
    appState: CoreUiState,
    vm: PersonalCodeViewModel,
    onShowDialog: (MainAlertDialog?) -> Unit,
    onShare: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PersonalCodeScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
                }

                PersonalCodeScreenEvent.CodeCreated -> {}
                PersonalCodeScreenEvent.CodeDeleted -> {}
                null -> { /* do nothing */
                }
            }
        }
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
            DefaultEmptyState(content = stringResource(MR.strings.error_load_data_generic))
        }

        is Payload.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                this.item(key = "header") {
                    SettingsBigHeader(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        name = stringResource(MR.strings.settings_section_personal_code_title)
                    )
                }

                if (state.data.activeCode != null) {
                    this.item(key = "activeCode") {
                        PersonalCodeItem(
                            modifier = Modifier.animateItemPlacement(),
                            state = state.data.activeCode!!,
                            deleteLoading = state.data.actionLoading,
                            onCopy = {
                                vm.copyToClipboard(state.data.activeCode!!.code)
                                vm.showSnackbar(text = MR.strings.toast_copy_invite_code.localizedString())
                            },
                            onShare = {
                                onShare(state.data.activeCode!!.code)
                            },
                            onDelete = {
                                onShowDialog(MainAlertDialog.PersonalCodeDelete(state.data.activeCode!!.id))
                            }
                        )
                    }
                }

                if (state.data.inactiveCodes.isNotEmpty()) {
                    this.item(key = "inactive_header") {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 20.dp)
                                .animateItemPlacement(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(MR.strings.settings_section_personal_code_inactive_title),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.alpha(ContentAlpha.medium)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            PlanLimitCount(count = state.data.inactiveCodes.size, limit = 0)
                        }
                    }
                    this.items(state.data.inactiveCodes, key = { it.id }) { code ->
                        PersonalCodeItemInactive(
                            modifier = Modifier.animateItemPlacement(),
                            code = code.code
                        ) {
                            vm.activatePersonalCode(code = it)
                        }
                    }
                    this.item(key = "inactive_spacer") {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                if (state.data.inactiveCodes.isNotEmpty() || state.data.activeCode != null) {
                    this.item(key = "button") {
                        if (state.data.canCreateAlternativeId) {
                            FilledButton(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                style = ButtonStyle.BIG,
                                state = if (state.data.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                                icon = CoreAppIcons.AddLink.asPainterResource(),
                                onClick = { vm.createPersonalCode() }
                            ) {
                                Text(stringResource(MR.strings.invite_create_new_button))
                            }
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                } else {
                    this.item(key = "empty_state") {
                        DefaultEmptyState(
                            icon = CoreAppIcons.AddLink.asPainterResource(),
                            content = stringResource(MR.strings.invite_manage_empty_description),
                            buttonText = stringResource(MR.strings.invite_create_button),
                            buttonState = if (state.data.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                            onClick = { vm.createPersonalCode() }
                        )
                    }
                }
            }
        }
    }
}