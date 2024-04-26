package app.paralino.android.ui.login

import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.navOptions
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.login.ResetPasswordScreenCore
import dev.icerock.moko.resources.compose.stringResource
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
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.reset_password_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
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
            }
        )
    }
}
