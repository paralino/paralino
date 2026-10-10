package app.paralino.android.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.PushServiceNotificationHandler
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.SimpleGenericMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.applyAsNightMode
import app.paralino.android.utils.extensions.openNotificationChannelSettings
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.GenericSettingsScreenCore
import app.paralino.core.presentation.billing.BillingScreenSourceType

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GenericSettingsScreen(
    appState: MainAppState,
    vm: BaseGenericSettingViewModel,
) {
    val screenTitle by vm.screenTitle.collectAsState(initial = null)
    val context = LocalContext.current
    val notificationPermissionHandler = when(val deviceApi = vm.deviceApiProvider) {
        null -> null

        else -> {
            rememberNotificationsPermissionHandler(
                appState = appState,
                messageDispatcher = vm.messageDispatcher,
                notificationStatusProvider = deviceApi.notificationStatusProvider
            )
        }
    }

    val locationPermissionHandler = when(val deviceApi = vm.deviceApiProvider) {
        null -> null

        else -> {
            rememberLocationPermissionHandler(
                appState = appState,
                messageDispatcher = vm.messageDispatcher,
                locationStatusProvider = deviceApi.locationStatusProvider
            )
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = if (screenTitle != null) screenTitle else vm.initialScreenTitle,
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        GenericSettingsScreenCore(
            coreUiState = appState,
            vm = vm,
            paddingValues = remember { PaddingValues() },
            mapContent = { state ->
                key(state.keyId) {
                    SimpleGenericMapComposable(
                        mapsSDKProvider = state.mapSDKProvider,
                        isSystemInDarkTheme = state.isDarkMode ?: isSystemInDarkTheme(),
                        markers = emptyList(),
                        fallbackCenterLatLng = state.centerLocation,
                        zoomLevel = state.zoomLevel,
                        isSatelliteMode = false,
                        canTapOnCircles = true,
                        onMapCameraSettled = {},
                        onMapInteraction = {
                            vm.onEvent(
                                GenericSettingsScreenUiEvent.OnMapInteraction(
                                    isTouchingMap = it
                                )
                            )
                        },
                        onMapTap = { lat, lng -> },
                        onMapLongPress = { lat, lng -> }
                    )
                }
            },
            openPaywall = {
                appState.navController.navigate(AppScreen.Settings.BillingPaywall.route(type = BillingScreenSourceType.UPGRADE_DEFAULT))
            },
            openNotificationSocketChannelSettings = {
                context.openNotificationChannelSettings(PushServiceNotificationHandler.channelId)
            },
            askNotificationPermission = { notificationPermissionHandler?.askNotificationPermission() },
            askBackgroundAccessPermission = {
                locationPermissionHandler?.askUnrestrictedBackgroundAccess(
                    forcePrompt = true
                )
            },
            applyAppTheme = { it.applyAsNightMode(context) },
            applyDarkThemeStyle = {
                // AppTheme is observing this with app prefs
            }
        )
    }
}