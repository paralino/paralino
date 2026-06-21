package app.paralino.android.ui.privacyzones

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
import app.paralino.core.MR
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenState
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenStep
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenUiEvent
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneViewModel
import app.paralino.core.presentation.privacyzones.create.makeMapPin
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import app.paralino.core.ui.privacyzones.CreatePrivacyZoneScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreatePrivacyZoneScreen(
    appState: MainAppState,
    groupId: String,
    zoneId: String?,
    editStep: CreatePrivacyZoneScreenStep?,
    lat: Double? = null,
    lng: Double? = null,
    vm: CreatePrivacyZoneViewModel = koinViewModel { parametersOf(groupId, zoneId, editStep, lat, lng) },
) {
    val pickerState = rememberEmojiBottomSheetPickerState()

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = when (vm.isNewZone) {
                    true -> stringResource(MR.strings.privacy_zones_create_screen_title)
                    false -> stringResource(MR.strings.privacy_zones_edit_screen_title)
                },
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        CreatePrivacyZoneScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() },
            onBottomSheetAction = { screen ->
                appState.uiScope.launch {
                    pickerState.show(screen.emojis)
                }
            },
            mapContent = { state ->
                PrivacyZoneCreateMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapCameraSettled = {
                        vm.onEvent(
                            CreatePrivacyZoneScreenUiEvent.OnMapPositionChanged(it)
                        )
                    },
                    onMapInteraction = { },
                    onMapTap = { lat, lng ->
                        vm.onEvent(
                            CreatePrivacyZoneScreenUiEvent.OnMapTap(lat, lng)
                        )
                    },
                    onMapLongPress = { lat, lng ->
                        vm.onEvent(
                            CreatePrivacyZoneScreenUiEvent.OnMapTap(lat, lng)
                        )
                    }
                )
            }
        )
    }

    EmojiBottomSheetPicker(
        state = pickerState,
        onClick = {
            vm.onEvent(CreatePrivacyZoneScreenUiEvent.OnImageChanged(it))
            appState.uiScope.launch { pickerState.hide() }
        }
    )
}

@Composable
private fun PrivacyZoneCreateMapComposable(
    modifier: Modifier = Modifier,
    state: CreatePrivacyZoneScreenState,
    onMapInteraction: (Boolean) -> Unit,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {
    val pin = remember(state.lat, state.lng, state.radiusValue, state.image) {
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
