package app.paralino.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.ui.groups.GroupDetailScreenCore
import app.paralino.core.ui.invite.MemberInviteScreenCore
import app.paralino.core.ui.login.GroupInitScreenCore
import app.paralino.core.ui.share.SharingStartScreenCore

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
        groupUpdated: (String) -> Unit,
        onExit: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupCreateScreenCore(
                appState = it,
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
        openMemberAdd: () -> Unit,
        openGroupEdit: () -> Unit,
        openMap: () -> Unit,
        openPlaces: () -> Unit,
        openGroupRawData: () -> Unit,
        openMemberRawData: (String) -> Unit,
        openSharing: () -> Unit,
        onNavigateBack: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupDetailScreenCore(
                appState = it,
                vm = vm,
                groupId = groupId,
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


    fun MemberInviteScreenVC(
        vm: MemberInviteViewModel,
        onPickGroup: (BottomSheetsScreen.GroupPicker) -> Unit,
        onExit: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            MemberInviteScreenCore(
                appState = it,
                vm = vm,
                onPickGroup = onPickGroup,
                onNavigateBack = onExit
            )
        }
    }

    fun StartSharingScreenVC(
        vm: SharingStartViewModel,
        groupId: String?,
        onPickGroup: (BottomSheetsScreen.GroupPicker) -> Unit,
        onNavigateBack: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            SharingStartScreenCore(
                appState = it,
                vm = vm,
                groupId = groupId,
                onPickGroup = onPickGroup,
                onNavigateBack = onNavigateBack,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }
}
