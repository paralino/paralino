package app.paralino.android.ui.login

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.ui.login.RecoverKeysScreenCore
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecoverKeysScreen(
    appState: MainAppState,
    vm: RecoverKeysViewModel = koinViewModel(),
) {
    RecoverKeysScreenCore(
        modifier = Modifier.safeDrawingPadding(),
        appState = appState,
        vm = vm
    )
}
