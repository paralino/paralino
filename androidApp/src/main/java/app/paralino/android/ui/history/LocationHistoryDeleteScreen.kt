package app.paralino.android.ui.history

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.history.LocationHistoryDeleteViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.history.LocationHistoryDeleteScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LocationHistoryDeleteScreen(
    appState: MainAppState,
    groupId: String,
    vm: LocationHistoryDeleteViewModel = koinViewModel { parametersOf(groupId) },
) {
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.history_delete_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        LocationHistoryDeleteScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() }
        )
    }
}
