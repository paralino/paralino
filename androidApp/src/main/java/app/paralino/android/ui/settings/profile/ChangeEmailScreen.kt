package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.ui.settings.profile.ChangeEmailScreenCore
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangeEmailScreen(
    appState: MainAppState,
    vm: ChangeEmailViewModel = getViewModel(),
) {
    ChangeEmailScreenCore(
        appState = appState,
        vm = vm,
        onNavigateBack = { appState.navController.popBackStack() }
    )
}

