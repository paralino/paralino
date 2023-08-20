package app.paralino.android.ui.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.Show
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.login.GroupInitScreenCore
import org.koin.androidx.compose.getViewModel


@Composable
fun GroupInitScreen(
    appState: MainAppState,
    vm: GroupInitViewModel = getViewModel(),
    onClose: () -> Unit,
) {

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        locationStatusProvider = vm.locationStatusProvider
    )

    when (val dialog = showDialog) {
        is MainAlertDialog.VerifyEmail -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.sendEmailVerification()
                }
            }
        }

        else -> {}
    }

    val context = LocalContext.current
    GroupInitScreenCore(
        appState = appState,
        vm = vm,
        onShowDialog = { showDialog = it },
        openSettings = { appState.navController.navigate(AppScreen.Settings.routeSignature) },
        onCreateGroup = { appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(null)) },
        openPersonalCodes = { appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature) },
        onSharePersonalCode = { context.share(it) },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
        onClose = onClose
    )
}
