package app.paralino.android.ui.groups

import androidx.compose.runtime.Composable
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultEmptyState
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

@Composable
fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String,
    vm: GroupDetailViewModel = getViewModel { parametersOf(groupId) },
) {

    val locationPermissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    GroupDetailScreenCore(
        appState = appState,
        vm = getViewModel { parametersOf(groupId) },
        groupId = groupId,
        openMemberAdd = {
            appState.navController.navigate(
                AppScreen.MainHome.GroupMemberAdd.route(groupId = groupId)
            )
        },
        openGroupEdit = {
            appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(groupId))
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
        openGroupRawData = {
            appState.navController.navigate(
                AppScreen.MainHome.RawDataScreen.route(
                    RawPresentationDataType.GroupPresentation(groupId)
                )
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
        onNavigateBack = { appState.navController.popBackStack() },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
        askLocationService = { locationPermissionHandler.askLocationService() },
        askLocationPermission = { locationPermissionHandler.askLocationPermission() },
        askBackgroundLocationPermission = { locationPermissionHandler.askBackgroundLocationPermission() }
    )
}