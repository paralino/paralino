package app.paralino.android.ui.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.login.GroupInitScreenCore
import org.koin.androidx.compose.getViewModel


@Composable
fun GroupInitScreen(
    appState: MainAppState,
    vm: GroupInitViewModel = getViewModel(),
) {
    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    val context = LocalContext.current
    GroupInitScreenCore(
        appState = appState,
        vm = vm,
        openSettings = { appState.navController.navigate(AppScreen.Settings.routeSignature) },
        onCreateGroup = { appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(null)) },
        openPersonalCodes = { appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature) },
        onSharePersonalCode = { context.share(it) },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
    )
}
