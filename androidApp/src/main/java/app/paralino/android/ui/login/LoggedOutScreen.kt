package app.paralino.android.ui.login

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.login.LoggedOutScreenState
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.ui.login.WelcomeScreen
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.AppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.androidx.compose.getViewModel

@Composable
fun LoggedOutScreen(
    appState: MainAppState,
    vm: LoggedOutViewModel = getViewModel()
) {

    val systemUiController = rememberSystemUiController()
    val defaultColor = MaterialTheme.colors.background
    val darkIconsDefault = !isSystemInDarkTheme()

    DisposableEffect(systemUiController) {
        systemUiController.setSystemBarsColor(
            color = AppColors.darkBackground,
            darkIcons = false
        )
        onDispose {
            systemUiController.setSystemBarsColor(
                color = defaultColor,
                darkIcons = darkIconsDefault
            )
        }
    }

    WelcomeScreen(
        vm = vm,
        onLoginClick = {
            appState.navController.navigate(AppScreen.LoggedOut.Login.route(LoginScreenUiType.LOGIN))
        },
        onStartClick = {
            appState.navController.navigate(
                AppScreen.LoggedOut.WelcomeOnboarding.routeSignature
            )
        }
    )
}


@Preview
@Composable
fun LoggedOutScreenPreview() {
    AppTheme() {
        Surface {
            WelcomeScreen(onStartClick = {})
        }
    }
}
