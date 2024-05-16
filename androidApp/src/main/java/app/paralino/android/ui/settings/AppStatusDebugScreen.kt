package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.AppStatusDebugScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun AppStatusDebugScreen(
    appState: MainAppState,
    vm: AppStatusDebugViewModel = getViewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = "App status",
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        AppStatusDebugScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it
        )
    }
}