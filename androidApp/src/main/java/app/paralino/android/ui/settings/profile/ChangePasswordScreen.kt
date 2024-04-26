package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.profile.ChangePasswordScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangePasswordScreen(
    appState: MainAppState,
    vm: ChangePasswordViewModel = getViewModel(),
) {
    Scaffold(
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
            onNavigateBack = { appState.navController.popBackStack() }
        )
    }
}
