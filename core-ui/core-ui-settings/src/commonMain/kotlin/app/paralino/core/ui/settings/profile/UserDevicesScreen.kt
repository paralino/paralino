package app.paralino.core.ui.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.settings.userdevices.UserDevicesScreenUiEvent
import app.paralino.core.presentation.settings.userdevices.UserDevicesViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.personal_space.UserDeviceItem
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun UserDevicesScreenCore(
    appState: CoreUiState,
    vm: UserDevicesViewModel,
    contentPadding: PaddingValues,
) {
    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState()

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(UserDevicesScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
        }
    }

    if (uiState.isLoadingData) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 100.dp
            )
        ) {
            if (uiState.devices.isEmpty()) {
                item(key = "devices_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(MR.strings.personalSpace_section_empty),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.devices,
                    key = { _, item -> item.id }
                ) { index, item ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    UserDeviceItem(
                        state = item,
                        onEvent = { event ->
                            vm.onEvent(UserDevicesScreenUiEvent.OnDeviceEvent(event))
                        }
                    )
                }
            }
        }
    }

    if (uiState.loadingAction) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}
