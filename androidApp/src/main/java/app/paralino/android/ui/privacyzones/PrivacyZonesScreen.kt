package app.paralino.android.ui.privacyzones

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
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimpleGenericMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.privacyzones.PrivacyZonesScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PrivacyZonesScreen(
    appState: MainAppState,
    groupId: String,
    isOnboarding: Boolean,
    vm: PrivacyZonesViewModel = koinViewModel { parametersOf(groupId, isOnboarding) },
) {
    var userScroll by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.privacy_zones_list_screen_title),
                navigationIcon = if (isOnboarding) {
                    null
                } else {
                    { ToolbarButtons.Back { appState.navController.popBackStack() } }
                }
            )
        }
    ) {
        PrivacyZonesScreenCore(
            appState = appState,
            vm = vm,
            userScrollEnabled = userScroll,
            contentPadding = PaddingValues(),
            addNewPrivacyZone = {
                appState.navController.navigate(
                    AppScreen.MainHome.PrivacyZoneCreate.route(
                        groupId = groupId,
                        zoneId = null
                    )
                )
            },
            onSkipOnboarding = {
                appState.navController.navigate(
                    AppScreen.MainHome.SharingStart.route(
                        groupId = groupId,
                        isOnboarding = isOnboarding
                    )
                )
            },
            openPrivacyZoneDetails = { zone ->
                appState.navController.navigate(
                    AppScreen.MainHome.PrivacyZoneDetail.route(
                        groupId = zone.groupOrSpaceId.id,
                        zoneId = zone.id
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
                    onMapTap = { _, _ -> },
                    onMapLongPress = { _, _ -> }
                )
            }
        )
    }

    BackHandler(enabled = isOnboarding) {
        // block back for onboarding
    }
}
