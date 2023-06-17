package app.paralino.android.ui.login

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.navigation.navOptions
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.ui.login.ResetPasswordScreenCore
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ResetPasswordScreen(
    appState: MainAppState,
    userId: String,
    secret: String,
    vm: ResetPasswordViewModel = getViewModel { parametersOf(userId, secret) },
) {
    ResetPasswordScreenCore(
        appState = appState,
        vm = vm,
        openLoginScreen = {
            appState.navController.navigate(
                AppScreen.LoggedOut.Login.route(LoginScreenUiType.LOGIN),
                navOptions {
                    popUpTo(AppScreen.LoggedOut.PasswordReset.routeSignature) {
                        inclusive = true
                    }
                }
            )
        },
        onClose = { appState.navController.popBackStack() }
    )
}
