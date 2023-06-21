package app.paralino.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.ui.components.MainAlertDialog
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
                onSharePersonalCode =  onSharePersonalCode,
                askNotificationPermission = askNotificationPermission,
                onClose = onClose
            )
        }
    }
}
