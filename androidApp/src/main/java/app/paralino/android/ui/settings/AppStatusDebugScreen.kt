package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.AppStatusDebugScreenCore
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppStatusDebugScreen(
    appState: MainAppState,
    vm: AppStatusDebugViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding().fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = "App status",
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        _root_ide_package_.app.paralino.core.ui.settings.AppStatusDebugScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it
        )
    }
}