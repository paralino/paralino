package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.places.PlaceAlertsSettingsScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PlaceAlertsSettingsScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String,
    vm: PlaceDetailViewModel = koinViewModel { parametersOf(groupId, placeId) },
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.place_detail_notification_settings_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        _root_ide_package_.app.paralino.core.ui.places.PlaceAlertsSettingsScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = remember {
                PaddingValues()
            }
        )
    }
}