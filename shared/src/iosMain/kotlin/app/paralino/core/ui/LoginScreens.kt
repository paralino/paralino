package app.paralino.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.login.LoginScreenCore
import app.paralino.core.ui.login.ProfileSetupScreenCore
import app.paralino.core.ui.login.RecoverKeysScreenCore
import app.paralino.core.ui.login.ResetPasswordScreenCore
import app.paralino.core.ui.login.WelcomeScreen

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
        vm: LoginViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        onExit: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            LoginScreenCore(
                appState = it,
                vm = vm,
                onShowDialog = onShowDialog,
                onExit = onExit
            )
        }
    }

    fun CompleteProfileVC(
        vm: EditProfileViewModel,
        onBottomSheetAction: (BottomSheetsScreen) -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            ProfileSetupScreenCore(
                appState = it,
                vm = vm,
                onBottomSheetAction = onBottomSheetAction,
            )
        }
    }

    fun RecoverKeysVC(
        vm: RecoverKeysViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            RecoverKeysScreenCore(
                appState = it,
                vm = vm,
                onShowDialog = onShowDialog,
            )
        }
    }

    fun ResetPasswordVC(
        vm: ResetPasswordViewModel,
        openLoginScreen: () -> Unit,
        onClose: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            ResetPasswordScreenCore(
                appState = it,
                vm = vm,
                openLoginScreen = openLoginScreen,
                onClose = onClose
            )
        }
    }
}