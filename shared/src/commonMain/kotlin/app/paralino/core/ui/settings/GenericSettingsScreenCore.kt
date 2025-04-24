package app.paralino.core.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenEvent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GenericSettingsScreenCore(
    coreUiState: CoreUiState,
    vm: BaseGenericSettingViewModel,
    paddingValues: PaddingValues,
    openPaywall: () -> Unit
) {
    MessageDispatcherEffect(vm, coreUiState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                GenericSettingsScreenEvent.OpenPaywall -> openPaywall()
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                GenericSettingsScreenUiEvent.DialogAction(
                    AlertDialogAction(
                        dialog = dialog,
                        action = it
                    )
                )
            )
        }
    }

    when (val finalState = uiState) {
        is GenericSettingsScreenState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        is GenericSettingsScreenState.EditScreen -> {}
        is GenericSettingsScreenState.ListItems -> {
            GenericSettingsListScreen(
                coreUiState = coreUiState,
                paddingValues = paddingValues,
                state = finalState,
                onEvent = { vm.onEvent(it) }
            )
        }
    }
}