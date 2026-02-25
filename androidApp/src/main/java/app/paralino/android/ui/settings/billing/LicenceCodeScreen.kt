package app.paralino.android.ui.settings.billing

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.billing.licence.LicenceCodeViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.billing.licence.LicenceCodeScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LicenceCodeScreen(
    appState: MainAppState,
    vm: LicenceCodeViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.licence_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        LicenceCodeScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() }
        )
    }
}
