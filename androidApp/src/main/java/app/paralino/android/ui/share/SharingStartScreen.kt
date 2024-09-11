package app.paralino.android.ui.share

import android.annotation.SuppressLint
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.groups.GroupBottomSheetPicker
import app.paralino.core.ui.groups.GroupPickerAction
import app.paralino.core.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.core.ui.share.SharingStartScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SharingStartScreen(
    appState: MainAppState,
    groupId: String?,
    vm: SharingStartViewModel = getViewModel { parametersOf(groupId) },
) {

    val permissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    val pickerState = rememberGroupBottomSheetPickerState()

    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.groups_sharing_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        SharingStartScreenCore(
            appState = appState,
            groupId = groupId,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() },
            onPickGroup = {
                appState.uiScope.launch { pickerState.show(it) }
            },
            askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
            askLocationService = { permissionHandler.askLocationService() },
            askLocationPermission = { permissionHandler.askLocationPermission() },
            askBackgroundLocationPermission = { permissionHandler.askBackgroundLocationPermission() }
        )
    }

    GroupBottomSheetPicker(
        state = pickerState,
        onClick = {
            when (it) {
                is GroupPickerAction.GroupPicked -> {
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