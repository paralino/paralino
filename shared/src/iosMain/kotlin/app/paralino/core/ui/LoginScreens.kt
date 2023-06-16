package app.paralino.core.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.login.LoginScreenCore
import app.paralino.core.ui.login.ProfileSetupScreenCore
import app.paralino.core.ui.login.WelcomeScreen
import app.paralino.core.ui.theme.AppTheme

object LoginScreens {
    fun WelcomeScreenVC(
        vm: LoggedOutViewModel,
        onLoginClick: () -> Unit,
        onStartClick: () -> Unit,
    ) = ComposeUIViewController {
        WelcomeScreen(
            vm,
            onLoginClick = onLoginClick,
            onStartClick = onStartClick
        )
    }

    fun LoginScreenVC(
        startScreenUiType: LoginScreenUiType,
        vm: LoginViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        onExit: () -> Unit
    ) = ComposeUIViewController {
        AppTheme {
            Surface(color = MaterialTheme.colors.background) {
                LoginScreenCore(
                    startScreenUiType = startScreenUiType,
                    vm = vm,
                    onShowDialog = onShowDialog,
                    onExit = onExit
                )
            }
        }
    }

    fun CompleteProfileVC(
        vm: EditProfileViewModel,
        onBottomSheetAction: (BottomSheetsScreen) -> Unit,
    ) = ComposeUIViewController {
        AppTheme {
            Surface(color = MaterialTheme.colors.background) {
                ProfileSetupScreenCore(
                    vm = vm,
                    onBottomSheetAction = onBottomSheetAction,
                )
            }
        }
    }
}