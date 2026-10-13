package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.notification.rememberPushNotificationServicePermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.status.AppStatusViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.AppStatusScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppStatusScreen(
    appState: MainAppState,
    vm: AppStatusViewModel = koinViewModel()
) {
    val locationPermissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        locationStatusProvider = vm.deviceApiProvider.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    val pushServiceHandler = rememberPushNotificationServicePermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        serviceManager = vm.notificationServiceManager
    )

    Scaffold(
        modifier = Modifier.safeDrawingPadding().fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.setting_status_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        AppStatusScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it,
            askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
            askLocationService = { locationPermissionHandler.askLocationService() },
            askLocationPermission = { locationPermissionHandler.askLocationPermission() },
            askBackgroundLocationPermission = { locationPermissionHandler.askBackgroundLocationPermission() },
            askBackgroundAccessPermission = {
                locationPermissionHandler.askUnrestrictedBackgroundAccess(forcePrompt = true)
            },
            askPushNotificationService = { pushServiceHandler.askPushNotificationService() }
        )
    }
}
