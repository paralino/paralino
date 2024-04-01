package app.paralino.android.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.presentation.settings.blocklist.BlocklistVideModel
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.BlocklistScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun BlocklistScreen(
    appState: MainAppState,
    vm: BlocklistVideModel = getViewModel()
) {

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    BlocklistScreenCore(
        appState = appState,
        vm = vm,
        onShowDialog = { showDialog = it },
        onNavigateBack = { appState.navController.popBackStack() }
    )

    when (val dialog = showDialog) {
        is MainAlertDialog.UnblockGroupOrUser -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.removeBlock(dialog.id)
                }
            }
        }

        else -> {}
    }
}