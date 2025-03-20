package app.paralino.android.ui.groups

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.groups.edit.GroupInitViewModel
import app.paralino.core.ui.onboarding.account.GroupInitScreenCore
import app.paralino.core.ui.settings.billing.BillingScreenSourceType


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
        modifier = Modifier.safeDrawingPadding(),
        appState = appState,
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
        openPaywall = {
            appState.navController.navigate(
                AppScreen.MainHome.BillingPaywall.route(groupId = null, type = BillingScreenSourceType.UPGRADE)
            )
        },
        openPersonalCodes = { appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature) },
        onSharePersonalCode = { context.share(it) },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
    )
}
