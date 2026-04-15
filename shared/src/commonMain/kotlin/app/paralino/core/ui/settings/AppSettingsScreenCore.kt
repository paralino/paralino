package app.paralino.core.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.generic.AppSettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsActionItem
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AppSettingsScreenCore(
    vm: AppSettingsViewModel,
    appState: CoreUiState,
    paddingValues: PaddingValues,
    onMeasurementSystemClick: () -> Unit,
    onNotificationGroupingClick: () -> Unit,
    onMapInteractionsClick: () -> Unit,
    onNavigationAppClick: () -> Unit,
    onPlaceSearchSettingsClick: () -> Unit,
    onNotificationSocketSettingsClick: () -> Unit,
    onMapSourceSettingsClick: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

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
            icon = CoreAppIcons.Ruler.asPainterResource(),
            onClick = onMeasurementSystemClick
        )
        SettingsActionItem(
            name = stringResource(MR.strings.setting_notificationGrouping_title),
            icon = CoreAppIcons.NotificationsActive.asPainterResource(),
            onClick = onNotificationGroupingClick
        )
        SettingsActionItem(
            name = stringResource(MR.strings.setting_placeSearch_title),
            icon = CoreAppIcons.Search.asPainterResource(),
            onClick = onPlaceSearchSettingsClick
        )

        if (uiState.showMapsProvider) {
            SettingsActionItem(
                name = stringResource(MR.strings.setting_mapsProvider_title),
                icon = CoreAppIcons.Map.asPainterResource(),
                onClick = onMapSourceSettingsClick
            )
        }

        SettingsActionItem(
            name = stringResource(MR.strings.setting_mapInteractions_title),
            icon = CoreAppIcons.HandSwipeTouch.asPainterResource(),
            onClick = onMapInteractionsClick
        )

        if (uiState.showNotificationSocket) {
            SettingsActionItem(
                name = stringResource(MR.strings.setting_notificationSocket_title),
                icon = CoreAppIcons.PhonelinkSetup.asPainterResource(),
                onClick = onNotificationSocketSettingsClick
            )
        }

        if (uiState.showNavigationApps) {
            SettingsActionItem(
                name = stringResource(MR.strings.setting_navigationApp_settings_title),
                icon = CoreAppIcons.Map.asPainterResource(),
                onClick = onNavigationAppClick
            )
        }
    }
}