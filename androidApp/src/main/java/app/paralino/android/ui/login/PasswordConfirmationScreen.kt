package app.paralino.android.ui.login

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.login.password_confirmation.PasswordConfirmationViewModel
import app.paralino.core.ui.login.PasswordConfirmationScreenCore
import org.koin.androidx.compose.koinViewModel

@Composable
fun PasswordConfirmationScreen(
    appState: MainAppState,
    vm: PasswordConfirmationViewModel = koinViewModel()
) {
    _root_ide_package_.app.paralino.core.ui.login.PasswordConfirmationScreenCore(
        modifier = Modifier.safeDrawingPadding(),
        appState = appState,
        vm = vm
    )
}
