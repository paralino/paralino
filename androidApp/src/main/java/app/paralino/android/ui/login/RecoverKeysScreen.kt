package app.paralino.android.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.Show
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.login.RecoverKeysScreenCore
import org.koin.androidx.compose.getViewModel


@Composable
fun RecoverKeysScreen(
    appState: MainAppState,
    vm: RecoverKeysViewModel = getViewModel(),
) {

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    when (val dialog = showDialog) {
        is MainAlertDialog.RecoveryResetAccount -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.resetAccount()
            }
        }
        is MainAlertDialog.RecoveryRecoverAccount -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.recoverAccount()
            }
        }
        is MainAlertDialog.Logout -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.logout()
            }
        }
        else -> {}
    }

    RecoverKeysScreenCore(
        appState = appState,
        vm = vm,
        onShowDialog = { showDialog = it}
    )
}
