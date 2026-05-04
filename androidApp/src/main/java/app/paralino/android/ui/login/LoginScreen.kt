package app.paralino.android.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.ui.login.LoginScreenCore
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LoginScreen(
    appState: MainAppState,
    startScreenUiType: LoginScreenUiType,
    vm: LoginViewModel = koinViewModel { parametersOf(startScreenUiType) },
) {
    val canNavigateBack by vm.canNavigateBack.collectAsState(initial = true)
    LoginScreenCore(
        modifier = Modifier.safeDrawingPadding(),
        appState = appState,
        vm = vm,
        onExit = {
            if (canNavigateBack) {
                appState.navController.popBackStack()
            }
        }
    )

    BackHandler(
        enabled = true
    ) {
        // block
        if (canNavigateBack) {
            appState.navController.popBackStack()
        }
    }
}
