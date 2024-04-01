package app.paralino.android.ui.settings

import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.settings.RawDataScreenCore
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RawDataScreen(
    appState: MainAppState,
    type: RawPresentationDataType,
    vm: RawDataPresentationViewModel = getViewModel { parametersOf(type) }
) {
    RawDataScreenCore(
        appState = appState,
        vm = vm,
        onNavigateBack = { appState.navController.popBackStack() }
    )
}