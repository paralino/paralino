package app.paralino.android.ui.login

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.login.ResetPasswordScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ResetPasswordScreen(
    appState: MainAppState,
    userId: String,
    secret: String,
    vm: ResetPasswordViewModel = koinViewModel { parametersOf(userId, secret) },
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
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
                    screen = AppScreen.LoggedOut.Login(LoginScreenUiType.LOGIN),
                    popUpTo = AppScreen.LoggedOut.PasswordReset::class,
                    inclusive = true
                )
            }
        )
    }
}
