package app.paralino.android.ui.invite.manage

import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.AppButtonState
import app.paralino.android.ui.components.DefaultEmptyState
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.components.PlanLimitCount
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.extensions.copy
import app.paralino.android.utils.extensions.share
import app.paralino.core.models.Payload
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenEvent
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonalCodeScreen(
    appState: MainAppState,
    vm: PersonalCodeViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is PersonalCodeScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
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
            DefaultEmptyState(content = stringResource(id = R.string.error_load_data_generic))
        }

        is Payload.Success -> {

            when (val dialog = showDialog) {
                is MainAlertDialog.PersonalCodeDelete -> {
                    dialog.Show {
                        showDialog = null
                        if (it == MainAlertDialog.Buttons.buttonOk)
                            vm.deletePersonalCode()
                    }
                }

                else -> {}
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                this.item(key = "header") {
                    SettingsBigHeader(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        name = stringResource(id = R.string.settings_section_personal_code_title)
                    )
                }

                if (state.data.activeCode != null) {
                    this.item(key = "activeCode") {
                        PersonalCodeItem(
                            modifier = Modifier.animateItemPlacement(),
                            state = state.data.activeCode!!,
                            deleteLoading = state.data.actionLoading,
                            onCopy = {
                                context.copy(state.data.activeCode!!.code)
                                context.let {
                                    Toast.makeText(
                                        it,
                                        it.getString(R.string.toast_copy_invite_code),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onShare = {
                                context.share(state.data.activeCode!!.code)
                            },
                            onDelete = {
                                showDialog =
                                    MainAlertDialog.PersonalCodeDelete(state.data.activeCode!!.id)
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
                                text = stringResource(id = R.string.settings_section_personal_code_inactive_title),
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
                }

                if (state.data.inactiveCodes.isNotEmpty() || state.data.activeCode != null) {
                    this.item(key = "button") {
                        if (state.data.canCreate) {
                            FilledButton(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                style = ButtonStyle.BIG,
                                state = if (state.data.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                                icon = AppIcons.AddLink,
                                onClick = { vm.createPersonalCode() }
                            ) {
                                Text(stringResource(id = R.string.invite_create_new_button))
                            }
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                } else {
                    this.item(key = "empty_state") {
                        DefaultEmptyState(
                            icon = AppIcons.AddLink,
                            content = stringResource(id = R.string.invite_manage_empty_description),
                            buttonText = if (state.data.canCreate) stringResource(id = R.string.invite_create_button) else null,
                            buttonState = if (state.data.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                            onClick = { vm.createPersonalCode() }
                        )
                    }
                }
            }
        }
    }
}