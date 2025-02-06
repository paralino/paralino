package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.profile.ChangeEmailScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangeEmailScreen(
    appState: MainAppState,
    vm: ChangeEmailViewModel = getViewModel(),
) {
    val canNavigateBack by vm.canNavigateBack.collectAsState(initial = true)
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_edit_email),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        ChangeEmailScreenCore(
            appState = appState,
            vm = vm,
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

