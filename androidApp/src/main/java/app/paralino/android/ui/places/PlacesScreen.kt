package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimplePlaceMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.places.PlacesScreenCore
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PlacesScreen(
    appState: MainAppState,
    groupId: String,
    isOnboarding: Boolean,
    vm: PlacesViewModel = getViewModel { parametersOf(groupId, isOnboarding) },
) {

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    var userScroll by remember {
        mutableStateOf(true)
    }

    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.places_list_screen_title),
                navigationIcon = if (isOnboarding) {
                    null
                } else {
                    { ToolbarButtons.Back { appState.navController.popBackStack() } }
                },
                actions = {
                    if (isOnboarding) {
                        SmallActionButton(
                            modifier = Modifier.padding(end = 8.dp),
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary,
                            icon = null,
                            hasEndArrow = true,
                            text = {
                                Text(
                                    text = stringResource(MR.strings.button_skip),
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                appState.navController.navigate(
                                    AppScreen.MainHome.SharingStart.route(
                                        groupId = groupId, isOnboarding = isOnboarding
                                    )
                                )
                            }
                        )
                    }
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
            openPaywall = {
                appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
            },
            mapContent = { state ->
                SimplePlaceMapComposable(
                    markers = state.markers,
                    onMapInteraction = { userScroll = !it },
                    onMapTap = { lat, lng -> },
                    onMapLongPress = { lat, lng -> }
                )
            }
        )
    }
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