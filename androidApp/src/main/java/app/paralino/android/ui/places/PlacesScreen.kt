package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimpleGenericMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.PlaceZoneExpirationStatus
import app.paralino.core.models.PlaceZoneType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.places.PlacesScreenCore
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PlacesScreen(
    appState: MainAppState,
    groupId: String,
    isOnboarding: Boolean,
    vm: PlacesViewModel = koinViewModel { parametersOf(groupId, isOnboarding) },
) {

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    var userScroll by remember {
        mutableStateOf(true)
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.places_list_screen_title),
                navigationIcon = if (isOnboarding) {
                    null
                } else {
                    { ToolbarButtons.Back { appState.navController.popBackStack() } }
                }
            )
        }
    ) {
        PlacesScreenCore(
            appState = appState,
            vm = vm,
            userScrollEnabled = userScroll,
            contentPadding = PaddingValues(),
            askNotificationPermission = {
                notificationPermissionHandler.askNotificationPermission()
            },
            addNewPlace = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceCreate.route(
                        groupId = groupId,
                        placeId = null,
                        editStep = null
                    )
                )
            },
            onSkipOnboarding = {
                appState.navController.navigate(
                    AppScreen.MainHome.SharingStart.route(
                        groupId = groupId,
                        presentation = if (isOnboarding) {
                            SharingStartPresentationType.GROUP_CREATE_ONBOARDING
                        } else {
                            SharingStartPresentationType.FULL
                        }
                    )
                )
            },
            openPlaceDetails = { place ->
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceDetail.route(
                        groupId = place.groupOrSpaceId.id,
                        placeId = place.id
                    )
                )
            },
            openPaywall = {
                appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
            },
            mapContent = { state ->
                SimpleGenericMapComposable(
                    mapsSDKProvider = state.androidMapSDKProvider,
                    markers = state.markers,
                    isSystemInDarkTheme = isSystemInDarkTheme(),
                    fallbackCenterLatLng = null,
                    zoomLevel = 4f,
                    isSatelliteMode = false,
                    canTapOnCircles = true,
                    onMapCameraSettled = {},
                    onMapInteraction = { userScroll = !it },
                    onMapTap = { lat, lng -> },
                    onMapLongPress = { lat, lng -> }
                )
            }
        )
    }

    BackHandler (enabled = isOnboarding) {
        // block back for onboarding
    }
}

@Preview("place_item")
@Composable
private fun PlaceListItemPreview() {
    AppTheme {
        PlaceListItem(
            state = PlaceListItemData(
                id = "",
                groupOrSpaceId = "".asGroupOrSpaceId(),
                type = PlaceZoneType.PlaceWithAlert,
                name = "Home",
                image = ImageModel.emoji(Emojis.lightBulb),
                expirationStatus = PlaceZoneExpirationStatus.ActiveAlways(showStatus = false),
                private = false,
                lat = 0.0,
                lng = 0.0,
                radiusMeters = 1000
            )
        ) {}
    }
}