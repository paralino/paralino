package app.paralino.core.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.login.GroupInitScreenCore
import app.paralino.core.ui.theme.AppTheme

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
        AppTheme {
            Surface(color = MaterialTheme.colors.background) {
                GroupInitScreenCore(
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
}