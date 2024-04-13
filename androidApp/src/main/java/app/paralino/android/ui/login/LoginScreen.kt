package app.paralino.android.ui.login

import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.ui.login.LoginScreenCore
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LoginScreen(
    appState: MainAppState,
    startScreenUiType: LoginScreenUiType,
    vm: LoginViewModel = getViewModel { parametersOf(startScreenUiType) },
) {
    LoginScreenCore(
        appState = appState,
        vm = vm,
        onExit = {
            appState.navController.popBackStack()
        }
    )
}
