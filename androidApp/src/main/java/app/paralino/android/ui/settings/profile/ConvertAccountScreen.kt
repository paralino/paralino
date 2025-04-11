package app.paralino.android.ui.settings.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.profile.ConvertAccountScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConvertAccountScreen(
    appState: MainAppState,
    vm: ConvertAccountViewModel = koinViewModel(),
) {
    val canNavigateBack by vm.canNavigateBack.collectAsState(initial = true)
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_guest_convert),
                navigationIcon = { ToolbarButtons.Back { if (canNavigateBack) appState.navController.popBackStack() } }
            )
        }
    ) {
        ConvertAccountScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = it,
            onNavigateBack = {
                if (canNavigateBack) {
                    appState.navController.popBackStack()
                }
            }
        )
    }

    BackHandler(
        enabled = !canNavigateBack
    ) {
        // block
    }
}