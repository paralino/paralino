package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimpleGenericMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.detail.PlaceSettingsState
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.places.PlaceDetailScreenCore
import app.paralino.core.ui.places.PlaceDetailUserListItem
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PlaceDetailScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String,
    vm: PlaceDetailViewModel = koinViewModel { parametersOf(groupId, placeId) },
) {

    var userScroll by remember {
        mutableStateOf(true)
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = null,
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        PlaceDetailScreenCore(
            appState = appState,
            vm = vm,
            userScrollEnabled = userScroll,
            contentPadding = remember {
                PaddingValues()
            },
            onNavigateBack = {
                appState.navController.popBackStack()
            },
            openPlaceRawDataScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.RawDataScreen.route(
                        RawPresentationDataType.GroupPlacePresentation(
                            placeId = placeId
                        )
                    )
                )
            },
            openPlaceAlertsScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceAlerts.route(
                        groupId = groupId,
                        placeId = placeId
                    )
                )
            },
            openPlaceSensitivityScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceSensitivity.route(
                        groupId = groupId,
                        placeId = placeId
                    )
                )
            },
            openPlaceEditScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceCreate.route(
                        groupId = groupId,
                        placeId = placeId
                    )
                )
            },
            mapContent = { state ->
                PlaceDetailMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapInteraction = { userScroll = !it }
                )
            }
        )
    }
}

@Composable
private fun PlaceDetailMapComposable(
    modifier: Modifier = Modifier,
    state: PlaceDetailScreenState,
    onMapInteraction: (Boolean) -> Unit,
) {
    val pin = remember(state.lat, state.lng, state.radiusMeters, state.image) {
        if (state.lat != null && state.lng != null)
            MapPinData(state)
        else null
    }
    SimpleGenericMapComposable(
        modifier = modifier,
        mapsSDKProvider = state.androidMapSDKProvider,
        isSystemInDarkTheme = isSystemInDarkTheme(),
        markers = listOfNotNull(pin),
        fallbackCenterLatLng = null,
        zoomLevel = 4f,
        isSatelliteMode = false,
        canTapOnCircles = true,
        onMapCameraSettled = {},
        onMapInteraction = onMapInteraction,
        onMapTap = { lat, lng -> },
        onMapLongPress = { lat, lng -> }
    )
}


@Preview("place_detail_user_item")
@Composable
private fun PlaceDetailUserItemPreview() {
    AppTheme {
        Column {
            val item = PlaceSettingsState(
                groupId = "",
                placeId = "",
                userId = "",
                deviceId = null,
                alertItemId = "",
                isCurrentUserOrDevice = false,
                name = "Some user",
                image = ImageModel.emoji(Emojis.lightBulb),
                tag = null,
                notifyEnter = false,
                notifyExit = false,
                loadingEnter = false,
                loadingExit = false
            )
            PlaceDetailUserListItem(
                state = item,
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(notifyEnter = true, notifyExit = true),
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(notifyEnter = true, loadingEnter = true),
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(loadingExit = true, isCurrentUserOrDevice = true),
                onEnterChange = {},
                onExitChange = {}
            )
        }
    }
}