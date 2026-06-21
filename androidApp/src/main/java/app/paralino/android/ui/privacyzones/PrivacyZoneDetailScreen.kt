package app.paralino.android.ui.privacyzones

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimpleGenericMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenStep
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailScreenState
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailViewModel
import app.paralino.core.presentation.privacyzones.detail.makeMapPin
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.privacyzones.PrivacyZoneDetailScreenCore
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PrivacyZoneDetailScreen(
    appState: MainAppState,
    groupId: String,
    zoneId: String,
    vm: PrivacyZoneDetailViewModel = koinViewModel { parametersOf(groupId, zoneId) },
) {
    var userScroll by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = null,
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        PrivacyZoneDetailScreenCore(
            appState = appState,
            vm = vm,
            userScrollEnabled = userScroll,
            contentPadding = PaddingValues(),
            onNavigateBack = { appState.navController.popBackStack() },
            openPrivacyZoneRawDataScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.RawDataScreen.route(
                        RawPresentationDataType.GroupPrivacyZonePresentation(
                            privacyZoneId = zoneId
                        )
                    )
                )
            },
            openPrivacyZoneNameEditScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PrivacyZoneCreate.route(
                        groupId = groupId,
                        zoneId = zoneId,
                        editStep = CreatePrivacyZoneScreenStep.NAME_SUGGESTION
                    )
                )
            },
            openPrivacyZonePositionEditScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PrivacyZoneCreate.route(
                        groupId = groupId,
                        zoneId = zoneId,
                        editStep = CreatePrivacyZoneScreenStep.ADDRESS
                    )
                )
            },
            openPrivacyZoneSettingsEditScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PrivacyZoneCreate.route(
                        groupId = groupId,
                        zoneId = zoneId,
                        editStep = CreatePrivacyZoneScreenStep.SETTINGS
                    )
                )
            },
            mapContent = { state ->
                PrivacyZoneDetailMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapInteraction = { userScroll = !it }
                )
            }
        )
    }
}

@Composable
private fun PrivacyZoneDetailMapComposable(
    modifier: Modifier = Modifier,
    state: PrivacyZoneDetailScreenState,
    onMapInteraction: (Boolean) -> Unit,
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
        fallbackCenterLatLng = null,
        zoomLevel = 4f,
        isSatelliteMode = false,
        canTapOnCircles = true,
        onMapCameraSettled = {},
        onMapInteraction = onMapInteraction,
        onMapTap = { _, _ -> },
        onMapLongPress = { _, _ -> }
    )
}
