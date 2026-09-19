package app.paralino.android.ui.onboarding

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.onboarding.AccountOnboardingViewModel
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.ui.onboarding.account.AccountOnboardingScreenCore


@Composable
fun AccountOnboardingScreen(
    appState: MainAppState,
    vm: AccountOnboardingViewModel
) {
    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    val context = LocalContext.current
    AccountOnboardingScreenCore(
        modifier = Modifier.safeDrawingPadding(),
        appState = appState,
        vm = vm,
        onCreateGroup = {
            appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(null))
        },
        openPaywall = {
            appState.navController.navigate(
                AppScreen.MainHome.BillingPaywall.route(groupId = null, type = it)
            )
        },
        openPersonalCodes = { appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature) },
        onSharePersonalCode = { context.share(it) },
        openSharingScreen = {
            appState.navController.navigate(
                AppScreen.MainHome.SharingStart.route(
                    groupId = it.id,
                    presentation = SharingStartPresentationType.GROUP_CREATE_ONBOARDING
                )
            )
        },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
    )
}
