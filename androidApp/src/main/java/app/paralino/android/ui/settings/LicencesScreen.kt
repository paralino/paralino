package app.paralino.android.ui.settings

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.LicencesScreenCore
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun LicencesScreen(appState: MainAppState) {
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.setting_title_libraries),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        LicencesScreenCore(
            coreUiState = appState,
            contentPadding = it
        )
    }
}