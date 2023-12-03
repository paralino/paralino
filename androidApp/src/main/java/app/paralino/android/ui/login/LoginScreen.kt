package app.paralino.android.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.paralino.android.ui.MainAppState
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.login.LoginScreenCore
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LoginScreen(
    appState: MainAppState,
    startScreenUiType: LoginScreenUiType,
    vm: LoginViewModel = getViewModel { parametersOf(startScreenUiType) },
) {
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    LoginScreenCore(
        appState = appState,
        vm = vm,
        onShowDialog = {
            showDialog = it
        },
        onExit = {
            appState.navController.popBackStack()
        }
    )

    showDialog?.let { dialog ->
        dialog.Show { dialogAction ->
            when (val dialog = dialog) {
                is MainAlertDialog.GuestAccountCreating -> {
                    showDialog = null
                    if (dialogAction == MainAlertDialog.Buttons.buttonOk)
                        vm.proceed()
                }

                is MainAlertDialog.GuestAccountRecover -> {
                    showDialog = null
                    if (dialogAction == MainAlertDialog.Buttons.buttonOk)
                        vm.recoverGuestAccount()
                    else if (dialogAction == MainAlertDialog.Buttons.buttonCancel) {
                        vm.dismissGuestAccount()
                    } else {
                        vm.hideGuestAccountDialog()
                    }
                }

                else -> {}
            }
        }
    }
}
