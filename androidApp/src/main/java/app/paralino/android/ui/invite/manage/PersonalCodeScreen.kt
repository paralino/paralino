package app.paralino.android.ui.invite.manage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.Show
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.invite.manage.PersonalCodeScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun PersonalCodeScreen(
    appState: MainAppState,
    vm: PersonalCodeViewModel = getViewModel(),
) {

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    val context = LocalContext.current

    PersonalCodeScreenCore(
        appState = appState,
        vm = vm,
        onShowDialog = { showDialog = it },
        onShare = { context.share(it) },
        onNavigateBack = { appState.navController.popBackStack() }
    )

    when (val dialog = showDialog) {
        is MainAlertDialog.PersonalCodeDelete -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.deletePersonalCode()
            }
        }

        else -> {}
    }
}