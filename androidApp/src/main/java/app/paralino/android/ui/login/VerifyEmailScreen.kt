package app.paralino.android.ui.login

import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.login.emailverify.VerifyEmailViewModel
import app.paralino.core.ui.login.VerifyEmailScreenCore
import org.koin.androidx.compose.koinViewModel

@Composable
fun VerifyEmailScreen(
    appState: MainAppState,
    vm: VerifyEmailViewModel = koinViewModel(),
) {
    VerifyEmailScreenCore(
        appState = appState,
        vm = vm,
        openChangeEmail = {
            appState.navController.navigate(AppScreen.LoggedOut.ChangeEmail.routeSignature)
        }
    )
}
