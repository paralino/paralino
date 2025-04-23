package app.paralino.core.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AppSettingsScreenCore(
    appState: CoreUiState,
    paddingValues: PaddingValues,
    onMeasurementSystemClick: () -> Unit,
) {
//    MessageDispatcherEffect(vm, appState)
//    val uiState by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(paddingValues)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 60.dp)
    ) {
        SettingsActionItem(
            name = stringResource(MR.strings.setting_measurementSystem_title),
            icon = CoreAppIcons.Speed.asPainterResource(),
            onClick = onMeasurementSystemClick
        )
    }
}