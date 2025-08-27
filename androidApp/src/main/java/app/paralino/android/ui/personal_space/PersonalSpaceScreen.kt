package app.paralino.android.ui.personal_space

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.personal_space.PersonalSpaceViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.personal_space.PersonalSpaceScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonalSpaceScreen(
    appState: MainAppState,
    vm: PersonalSpaceViewModel = koinViewModel()
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
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.personalSpace_spaceTitle),
                navigationIcon = { ToolbarButtons.Close { appState.navController.popBackStack() } }
            )
        }
    ) { padding ->
        PersonalSpaceScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = padding,
            openSettings = {
                appState.navController.navigate(
                    AppScreen.MainHome.PersonalSpaceSettings.route()
                )
            },
            openTimeline = {
                appState.navController.navigate(
                    AppScreen.MainHome.LocationHistory.route(
                        groupId = UserGroup.groupPersonalDevicesId,
                        memberUserId = ""
                    )
                )
            },
            openPlaces = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlacesList.route(groupId = UserGroup.groupPersonalDevicesId)
                )
            },
            openSharing = {
                appState.navController.navigate(
                    AppScreen.MainHome.SharingStart.route(UserGroup.groupPersonalDevicesId)
                )
            },
            openPaywall = {
                appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
            },
            askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
            askLocationService = { locationPermissionHandler.askLocationService() },
            askLocationPermission = { locationPermissionHandler.askLocationPermission() },
            askBackgroundLocationPermission = { locationPermissionHandler.askBackgroundLocationPermission() }
        )
    }
}