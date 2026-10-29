package app.paralino.android.ui.login

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.ui.login.WelcomeScreen
import app.paralino.core.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoggedOutScreen(
    appState: MainAppState,
    vm: LoggedOutViewModel = koinViewModel()
) {

    val activity = LocalActivity.current
    val darkIconsDefault = !isSystemInDarkTheme()

    // Edge-to-edge keeps the system bars transparent, so only the icon appearance needs to change
    DisposableEffect(activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        onDispose {
            insetsController.isAppearanceLightStatusBars = darkIconsDefault
            insetsController.isAppearanceLightNavigationBars = darkIconsDefault
        }
    }

    WelcomeScreen(
        vm = vm,
        paddingValues = WindowInsets.safeDrawing.asPaddingValues(),
        onLoginClick = {
            appState.navController.navigate(AppScreen.LoggedOut.Login.route(LoginScreenUiType.SIGNUP))
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
            WelcomeScreen(
                paddingValues = PaddingValues(vertical = 20.dp),
                onStartClick = {}
            )
        }
    }
}
