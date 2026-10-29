package app.paralino.android.ui.share

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.notification.rememberPushNotificationServicePermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.groups.GroupBottomSheetPicker
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.presentation.share.SharingStartScreenUiEvent
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
    presentationType: SharingStartPresentationType,
    vm: SharingStartViewModel = koinViewModel { parametersOf(groupId, presentationType) },
) {

    val permissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        locationStatusProvider = vm.deviceApiProvider.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    val pushServiceHandler = rememberPushNotificationServicePermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        serviceManager = vm.notificationServiceManager
    )

    LifecycleResumeEffect(Unit) {
        vm.onEvent(SharingStartScreenUiEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    val pickerState = rememberGroupBottomSheetPickerState()
    val isCreateOnboarding = presentationType == SharingStartPresentationType.GROUP_CREATE_ONBOARDING

    fun finishWithSharingScreen(
        isCreateOnboarding: Boolean
    ) {
        if (vm.shouldProceedToPaywall) {
            appState.navController.navigate(
                AppScreen.MainHome.BillingPaywall.route(
                    groupId = vm.currentGroupId,
                    type = when (isCreateOnboarding) {
                        true -> {
                            if (vm.isPersonalSpace) {
                                BillingScreenSourceType.PERSONAL_SPACE_ONBOARDING
                            } else {
                                BillingScreenSourceType.GROUP_ONBOARDING
                            }
                        }
                        false -> BillingScreenSourceType.UPGRADE_DEFAULT
                    }
                )
            ) {
                this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                    this.inclusive = false
                }
            }
        } else if(isCreateOnboarding) {
            if (vm.isPersonalSpace) {
                appState.navController.popBackStack(
                    route = AppScreen.MainHome.MainMap.routeSignature,
                    inclusive = false
                )
            } else {
                appState.navController.navigate(
                    AppScreen.MainHome.GroupDetails.route(groupId = vm.currentGroupId!!)
                ) {
                    this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                        this.inclusive = false
                    }
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
                title = if(vm.initialGroupId?.isPersonalSpace == true) {
                    stringResource(resource = MR.strings.personalSpace_tracking_title)
                } else {
                    stringResource(resource = MR.strings.groups_sharing_title)
                },
                navigationIcon = if (isCreateOnboarding) {
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
                finishWithSharingScreen(isCreateOnboarding = isCreateOnboarding)
            },
            onSkipOnboarding = {
                if (presentationType == SharingStartPresentationType.GROUP_JOIN) {
                    appState.navController.popBackStack()
                } else {
                    finishWithSharingScreen(isCreateOnboarding = true)
                }
            },
            onPickGroup = {
                appState.uiScope.launch { pickerState.show(it) }
            },
            askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
            askLocationService = { permissionHandler.askLocationService() },
            askLocationPermission = { permissionHandler.askLocationPermission() },
            askBackgroundLocationPermission = { permissionHandler.askBackgroundLocationPermission() },
            askBackgroundAccessPermission = {
                permissionHandler.askUnrestrictedBackgroundAccess(
                    forcePrompt = true
                )
            },
            askPushNotificationService = { pushServiceHandler.askPushNotificationService() },
            openSubscriptions = {
                appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
            }
        )
    }


    BackHandler (enabled = isCreateOnboarding) {
        // block back for onboarding
    }

    GroupBottomSheetPicker(
        state = pickerState,
        onClick = {
            when (it) {
                is GroupPickerAction.GroupPicked -> {
                    appState.uiScope.launch { pickerState.hide() }
                    vm.setSelectedGroup(it.group.id)
                }

                GroupPickerAction.Close -> {
                    appState.uiScope.launch { pickerState.hide() }
                }

                else -> {}
            }
        }
    )
}
