package app.paralino.android.ui.places

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.map.SimplePlaceMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.ui.places.PlacesScreenCore
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlacesScreen(
    appState: MainAppState,
    groupId: String,
    vm: PlacesViewModel = getViewModel { parametersOf(groupId) },
) {

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    var userScroll by remember {
        mutableStateOf(true)
    }

    PlacesScreenCore(
        appState = appState,
        vm = vm,
        userScrollEnabled = userScroll,
        askNotificationPermission = {
            notificationPermissionHandler.askNotificationPermission()
        },
        addNewPlace = {
            appState.navController.navigate(
                AppScreen.MainHome.PlaceCreate.route(
                    groupId = groupId,
                    placeId = null
                )
            )
        },
        openPlaceDetails = { place ->
            appState.navController.navigate(
                AppScreen.MainHome.PlaceDetail.route(
                    groupId = place.groupId,
                    placeId = place.id
                )
            )
        },
        onNavigateBack = {
            appState.navController.popBackStack()
        },
        mapContent = { state ->
            SimplePlaceMapComposable(
                markers = state.markers,
                onMapInteraction = { userScroll = !it },
                onMapLongPress = { lat, lng -> }
            )
        }
    )
}

@Preview("place_item")
@Composable
private fun PlaceListItemPreview() {
    AppTheme {
        PlaceListItem(
            state = PlaceListItemData(
                id = "",
                groupId = "",
                name = "Home",
                image = ImageModel.emoji(Emojis.lightBulb),
                private = false,
                lat = 0.0,
                lng = 0.0,
                radiusMeters = 1000
            )
        ) {}
    }
}