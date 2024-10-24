package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.groups.GroupDetailScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String?,
) {
    if (groupId.isNullOrBlank()) {
        DefaultEmptyState(content = stringResource(MR.strings.group_load_error))
    } else {
        GroupDetailScreen(appState = appState, groupId = groupId)
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
private fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String,
    vm: GroupDetailViewModel = getViewModel { parametersOf(groupId) },
) {

    val locationPermissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.deviceApiProvider.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    Scaffold(
        topBar = {
            DefaultToolbar(
                title = null,
                navigationIcon = { ToolbarButtons.Close { appState.navController.popBackStack() } }
            )
        }
    ) {
        GroupDetailScreenCore(
            appState = appState,
            vm = getViewModel { parametersOf(groupId) },
            groupId = groupId,
            contentPadding = remember { PaddingValues() },
            openMemberAdd = {
                appState.navController.navigate(
                    AppScreen.MainHome.GroupMemberAdd.route(groupId = groupId)
                )
            },
            openMap = {
                appState.navController.navigate(AppScreen.MainHome.MainMap.route(groupId)) {
                    popUpTo(AppScreen.MainHome.MainMap.routeSignature) { inclusive = true }
                    this.launchSingleTop = true
                }
            },
            openPlaces = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlacesList.route(groupId)
                )
            },
            openGroupSettings = {
                appState.navController.navigate(
                    AppScreen.MainHome.GroupSettings.route(groupId)
                )
            },
            openMemberRawData = {
                appState.navController.navigate(
                    AppScreen.MainHome.RawDataScreen.route(
                        RawPresentationDataType.GroupMemberPresentation(it)
                    )
                )
            },
            openSharing = {
                appState.navController.navigate(
                    AppScreen.MainHome.SharingStart.route(groupId)
                )
            },
            openPaywall = {
                appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
            },
            onNavigateBack = { appState.navController.popBackStack() },
            askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
            askLocationService = { locationPermissionHandler.askLocationService() },
            askLocationPermission = { locationPermissionHandler.askLocationPermission() },
            askBackgroundLocationPermission = { locationPermissionHandler.askBackgroundLocationPermission() }
        )
    }
}