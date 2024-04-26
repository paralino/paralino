package app.paralino.android.ui.invite.manage

import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.utils.extensions.share
import app.paralino.core.MR
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.invite.manage.PersonalCodeScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PersonalCodeScreen(
    appState: MainAppState,
    vm: PersonalCodeViewModel = getViewModel(),
) {

    val context = LocalContext.current
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_section_personal_code_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        PersonalCodeScreenCore(
            appState = appState,
            vm = vm,
            onShare = { context.share(it) }
        )
    }
}