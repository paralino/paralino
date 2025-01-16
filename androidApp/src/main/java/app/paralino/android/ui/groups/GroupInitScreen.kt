package app.paralino.android.ui.groups

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.groups.edit.GroupInitViewModel
import app.paralino.core.ui.onboarding.account.GroupInitScreenCore


@Composable
fun GroupInitScreen(
    appState: MainAppState,
    vm: GroupInitViewModel
) {
    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    val context = LocalContext.current
    GroupInitScreenCore(
        appState = appState,
        paddingValues = remember { PaddingValues() },
        vm = vm,
        openSettings = { appState.navController.navigate(AppScreen.Settings.routeSignature) },
        onCreateGroup = {
            appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(null))
        },
        openFreePlanInfo = {
            appState.navController.navigate(
                AppScreen.MainHome.FreePlanNoticeScreen.route(groupId = null)
            )
        },
        openPersonalCodes = { appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature) },
        onSharePersonalCode = { context.share(it) },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
    )
}
