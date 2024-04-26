package app.paralino.android.ui.login

import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.ui.login.RecoverKeysScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun RecoverKeysScreen(
    appState: MainAppState,
    vm: RecoverKeysViewModel = getViewModel(),
) {
    RecoverKeysScreenCore(
        appState = appState,
        vm = vm
    )
}
