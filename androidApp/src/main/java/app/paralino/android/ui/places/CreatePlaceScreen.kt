package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimpleGenericMapComposable
import app.paralino.core.resources.moko.*
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceScreenStep
import app.paralino.core.presentation.places.create.CreatePlaceScreenUiEvent
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.create.makeMapPin
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import app.paralino.core.ui.places.CreatePlaceScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreatePlaceScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String?,
    editStep: CreatePlaceScreenStep?,
    lat: Double? = null,
    lng: Double? = null,
    vm: CreatePlaceViewModel = koinViewModel { parametersOf(groupId, placeId, editStep, lat, lng) },
) {
    val pickerState = rememberEmojiBottomSheetPickerState()

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = when (vm.isNewPlace) {
                    true -> stringResource(MR.strings.places_create_screen_title)
                    false -> stringResource(MR.strings.places_edit_screen_title)
                },
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        CreatePlaceScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() },
            onBottomSheetAction = { screen ->
                appState.uiScope.launch {
                    pickerState.show(screen.emojis)
                }
            },
            mapContent = { state ->
                PlaceCreateMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapCameraSettled = {
                        vm.onEvent(
                            CreatePlaceScreenUiEvent.OnMapPositionChanged(it)
                        )
                    },
                    onMapInteraction = { },
                    onMapTap = { lat, lng ->
                        vm.onEvent(
                            CreatePlaceScreenUiEvent.OnMapTap(
                                lat = lat,
                                lng = lng
                            )
                        )
                    },
                    onMapLongPress = { lat, lng ->
                        vm.onEvent(
                            CreatePlaceScreenUiEvent.OnMapTap(
                                lat = lat,
                                lng = lng
                            )
                        )
                    }
                )
            }
        )
    }

    EmojiBottomSheetPicker(
        state = pickerState,
        onClick = {
            vm.onEvent(CreatePlaceScreenUiEvent.OnImageChanged(it))
            appState.uiScope.launch { pickerState.hide() }
        }
    )
}

@Composable
private fun PlaceCreateMapComposable(
    modifier: Modifier = Modifier,
    state: CreatePlaceScreenState,
    onMapInteraction: (Boolean) -> Unit,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {
    val pin = remember(state.lat, state.lng, state.radiusMeters, state.image) {
        if (state.lat != null && state.lng != null) {
            state.makeMapPin()
        } else {
            null
        }
    }
    SimpleGenericMapComposable(
        modifier = modifier,
        mapsSDKProvider = state.androidMapSDKProvider,
        isSystemInDarkTheme = isSystemInDarkTheme(),
        markers = listOfNotNull(pin),
        fallbackCenterLatLng = state.fallbackCenterLatLng?.location,
        zoomLevel = if (state.fallbackCenterLatLng?.isRegion == true) 4f else 10f,
        isSatelliteMode = state.isSatelliteModeActive,
        canTapOnCircles = false,
        onMapCameraSettled = onMapCameraSettled,
        onMapInteraction = onMapInteraction,
        onMapTap = onMapTap,
        onMapLongPress = onMapLongPress
    )
}
