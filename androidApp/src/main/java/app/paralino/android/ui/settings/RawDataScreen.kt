package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.RawDataScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RawDataScreen(
    appState: MainAppState,
    type: RawPresentationDataType,
    vm: RawDataPresentationViewModel = koinViewModel { parametersOf(type) }
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.raw_data_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        RawDataScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it
        )
    }
}