package app.paralino.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.ui.groups.GroupDetailScreenCore
import app.paralino.core.ui.login.GroupInitScreenCore

object HomeScreens {
    fun GroupInitScreenVC(
        vm: GroupInitViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        openSettings: () -> Unit,
        onCreateGroup: () -> Unit,
        openPersonalCodes: () -> Unit,
        onSharePersonalCode: (String) -> Unit,
        askNotificationPermission: () -> Unit,
        onClose: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupInitScreenCore(
                appState = it,
                vm = vm,
                onShowDialog = onShowDialog,
                openSettings = openSettings,
                onCreateGroup = onCreateGroup,
                openPersonalCodes = openPersonalCodes,
                onSharePersonalCode = onSharePersonalCode,
                askNotificationPermission = askNotificationPermission,
                onClose = onClose
            )
        }
    }

    fun GroupCreateScreenVC(
        vm: CreateGroupViewModel,
        onBottomSheetAction: (BottomSheetsScreen) -> Unit,
        groupUpdated: (String?) -> Unit,
        onExit: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupCreateScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                groupUpdated = groupUpdated,
                onBottomSheetAction = onBottomSheetAction,
                onExit = onExit,
            )
        }
    }

    fun GroupDetailScreenVC(
        vm: GroupDetailViewModel,
        groupId: String?,
        onShowDialog: (MainAlertDialog?) -> Unit,
        openMemberAdd: (String) -> Unit,
        openGroupEdit: (String) -> Unit,
        openMap: (String) -> Unit,
        openPlaces: (String) -> Unit,
        openGroupRawData: (String) -> Unit,
        openMemberRawData: (String) -> Unit,
        openSharing: (String) -> Unit,
        onNavigateBack: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupDetailScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                groupId = groupId,
                onShowDialog = onShowDialog,
                openMemberAdd = openMemberAdd,
                openGroupEdit = openGroupEdit,
                openMap = openMap,
                openPlaces = openPlaces,
                openGroupRawData = openGroupRawData,
                openMemberRawData = openMemberRawData,
                openSharing = openSharing,
                onNavigateBack = onNavigateBack,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }
}
