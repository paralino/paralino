package app.paralino.android.ui.settings

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.RawDataScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RawDataScreen(
    appState: MainAppState,
    type: RawPresentationDataType,
    vm: RawDataPresentationViewModel = getViewModel { parametersOf(type) }
) {
    Scaffold(
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