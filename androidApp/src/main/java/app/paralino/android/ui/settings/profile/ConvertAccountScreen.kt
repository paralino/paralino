package app.paralino.android.ui.settings.profile

import androidx.compose.runtime.Composable
import app.paralino.android.ui.MainAppState
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.ui.settings.profile.ConvertAccountScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun ConvertAccountScreen(
    appState: MainAppState,
    vm: ConvertAccountViewModel = getViewModel(),
) {
    ConvertAccountScreenCore(
        appState = appState,
        vm = vm,
        onNavigateBack = { appState.navController.popBackStack() }
    )
}