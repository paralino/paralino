package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.LicencesScreenCore
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun LicencesScreen(appState: MainAppState) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.setting_title_libraries),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        _root_ide_package_.app.paralino.core.ui.settings.LicencesScreenCore(
            coreUiState = appState,
            contentPadding = it
        )
    }
}