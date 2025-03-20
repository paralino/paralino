package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.profile.ChangePasswordScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangePasswordScreen(
    appState: MainAppState,
    vm: ChangePasswordViewModel = koinViewModel(),
) {
    val canNavigateBack by vm.canNavigateBack.collectAsState(initial = true)
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_edit_password),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        ChangePasswordScreenCore(
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
