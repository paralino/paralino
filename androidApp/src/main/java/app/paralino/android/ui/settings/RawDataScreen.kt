package app.paralino.android.ui.settings

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
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
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(MR.strings.raw_data_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
                            contentDescription = null
                        )
                    }
                }
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