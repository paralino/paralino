package app.paralino.android.ui.share

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.groups.GroupBottomSheetPicker
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.share.SharingStartScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SharingStartScreen(
    appState: MainAppState,
    groupId: String?,
    isOnboarding: Boolean,
    vm: SharingStartViewModel = koinViewModel { parametersOf(groupId, isOnboarding) },
) {

    val permissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.deviceApiProvider.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    val pickerState = rememberGroupBottomSheetPickerState()

    fun finishWithSharingScreen(
        isOnboarding: Boolean
    ) {
        if (vm.shouldProceedToPaywall) {
            appState.navController.navigate(
                AppScreen.MainHome.BillingPaywall.route(
                    groupId = vm.currentGroupId,
                    type = when (isOnboarding) {
                        true -> BillingScreenSourceType.GROUP_ONBOARDING
                        false -> BillingScreenSourceType.UPGRADE
                    }
                )
            ) {
                this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                    this.inclusive = false
                }
            }
        } else if(isOnboarding) {
            appState.navController.navigate(
                AppScreen.MainHome.GroupDetails.route(groupId = vm.currentGroupId!!)
            ) {
                this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                    this.inclusive = false
                }
            }
        } else {
            appState.navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.groups_sharing_title),
                navigationIcon = if (isOnboarding) {
                    null
                } else {
                    { ToolbarButtons.Back { appState.navController.popBackStack() } }
                }
            )
        }
    ) {
        SharingStartScreenCore(
            appState = appState,
            vm = vm,
            onSharingStarted = {
                finishWithSharingScreen(isOnboarding = isOnboarding)
            },
            onSkipOnboarding = {
                finishWithSharingScreen(isOnboarding = true)
            },
            onPickGroup = {
                appState.uiScope.launch { pickerState.show(it) }
            },
            askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
            askLocationService = { permissionHandler.askLocationService() },
            askLocationPermission = { permissionHandler.askLocationPermission() },
            askBackgroundLocationPermission = { permissionHandler.askBackgroundLocationPermission() },
            openSubscriptions = {
                appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
            }
        )
    }


    BackHandler (enabled = isOnboarding) {
        // block back for onboarding
    }

    GroupBottomSheetPicker(
        state = pickerState,
        onClick = {
            when (it) {
                is GroupPickerAction.GroupPicked -> {
                    appState.uiScope.launch { pickerState.hide() }
                    vm.setSelectedGroup(it.group.id.id)
                }

                GroupPickerAction.Close -> {
                    appState.uiScope.launch { pickerState.hide() }
                }

                else -> {}
            }
        }
    )
}