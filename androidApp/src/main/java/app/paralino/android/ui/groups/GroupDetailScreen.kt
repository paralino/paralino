package app.paralino.android.ui.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.groups.GroupDetailScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String?,
) {
    if (groupId.isNullOrBlank()) {
        DefaultEmptyState(content = stringResource(MR.strings.group_load_error))
    } else {
        GroupDetailScreen(appState = appState, groupId = groupId)
    }
}

@Composable
fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String,
    vm: GroupDetailViewModel = getViewModel { parametersOf(groupId) },
) {

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val locationPermissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    when (val dialog = showDialog) {
        is MainAlertDialog.DeleteGroup -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.leaveOrDeleteGroup()
                }
            }
        }

        is MainAlertDialog.LeaveGroup -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.leaveOrDeleteGroup()
                }
            }
        }

        is MainAlertDialog.BlockGroup -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.blockGroup()
                }
            }
        }

        is MainAlertDialog.BlockUser -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.blockUser(userId = dialog.userId)
                }
            }
        }

        is MainAlertDialog.UnblockGroupOrUser -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.unblockUser(userId = dialog.id)
                }
            }
        }

        is MainAlertDialog.DeleteGroupMember -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.removeMember(dialog.memberUserId)
                }
            }
        }

        is MainAlertDialog.DeleteMemberLocation -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.removeMemberLocation(
                        memberId = dialog.memberId,
                        locationId = dialog.locationId
                    )
                }
            }
        }

        is MainAlertDialog.PlanLimit -> {
            dialog.Show {
                showDialog = null
            }
        }

        else -> {}
    }

    GroupDetailScreenCore(
        appState = appState,
        vm = getViewModel { parametersOf(groupId) },
        groupId = groupId,
        onShowDialog = { showDialog = it },
        openMemberAdd = {
            appState.navController.navigate(
                AppScreen.MainHome.GroupMemberAdd.route(groupId = groupId)
            )
        },
        openGroupEdit = {
            appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(groupId))
        },
        openMap = {
            appState.navController.navigate(AppScreen.MainHome.MainMap.route(groupId)) {
                popUpTo(AppScreen.MainHome.MainMap.routeSignature) { inclusive = true }
                this.launchSingleTop = true
            }
        },
        openPlaces = {
            appState.navController.navigate(
                AppScreen.MainHome.PlacesList.route(groupId)
            )
        },
        openGroupRawData = {
            appState.navController.navigate(
                AppScreen.MainHome.RawDataScreen.route(
                    RawPresentationDataType.GroupPresentation(groupId)
                )
            )
        },
        openMemberRawData = {
            appState.navController.navigate(
                AppScreen.MainHome.RawDataScreen.route(
                    RawPresentationDataType.GroupMemberPresentation(it)
                )
            )
        },
        openSharing = {
            appState.navController.navigate(
                AppScreen.MainHome.SharingStart.route(groupId)
            )
        },
        onNavigateBack = { appState.navController.popBackStack() },
        askNotificationPermission = { notificationPermissionHandler.askNotificationPermission() },
        askLocationService = { locationPermissionHandler.askLocationService() },
        askLocationPermission = { locationPermissionHandler.askLocationPermission() },
        askBackgroundLocationPermission = { locationPermissionHandler.askBackgroundLocationPermission() }
    )
}