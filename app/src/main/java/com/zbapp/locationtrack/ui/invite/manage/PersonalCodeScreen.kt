package com.zbapp.locationtrack.ui.invite.manage

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.Payload
import com.zbapp.core.presentation.invite.manage.PersonalCodeScreenEvent
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.AppButtonState
import com.zbapp.locationtrack.ui.components.DefaultEmptyState
import com.zbapp.locationtrack.ui.components.MainAlertDialog
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.utils.extensions.copy
import com.zbapp.locationtrack.utils.extensions.share
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun PersonalCodeScreen(
    appState: MainAppState,
    vm: PersonalCodesViewModel = getViewModel(),
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
                            vm.deletePersonalCode(dialog.codeId)
                    }
                }
                else -> {}
            }

            if (state.data.code != null) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Spacer(modifier = Modifier.size(20.dp))
                    PersonalCodeItem(
                        state = state.data.code!!,
                        deleteLoading = state.data.actionLoading,
                        onCopy = {
                            context.copy(state.data.code!!.code)
                            context.let {
                                Toast.makeText(
                                    it,
                                    it.getString(R.string.toast_copy_invite_code),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShare = {
                            context.share(state.data.code!!.code)
                        },
                        onDelete = {
                            showDialog = MainAlertDialog.PersonalCodeDelete(state.data.code!!.id)
                        }
                    )
                    DefaultEmptyState(content = stringResource(id = R.string.invite_manage_description))
                }
            } else {
                DefaultEmptyState(
                    icon = Icons.Rounded.AddLink,
                    content = stringResource(id = R.string.invite_manage_empty_description),
                    buttonText = stringResource(id = R.string.invite_create_button),
                    buttonState = if (state.data.createLoading) AppButtonState.LOADING else AppButtonState.CHECK_THEN_CONTENT,
                    onClick = { vm.createPersonalCode() }
                )
            }

        }
    }

}