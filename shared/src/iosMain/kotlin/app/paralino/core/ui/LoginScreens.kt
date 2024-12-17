package app.paralino.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.presentation.onboarding.WelcomeOnboardingViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.login.LoginScreenCore
import app.paralino.core.ui.login.ProfileSetupScreenCore
import app.paralino.core.ui.login.RecoverKeysScreenCore
import app.paralino.core.ui.login.ResetPasswordScreenCore
import app.paralino.core.ui.login.WelcomeScreen
import app.paralino.core.ui.onboarding.welcome.WelcomeOnboardingContent
import app.paralino.core.ui.onboarding.welcome.WelcomeOnboardingScreenCore

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
        onExit: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            LoginScreenCore(
                appState = it,
                vm = vm,
                onExit = onExit
            )
        }
    }

    fun CompleteProfileVC(
        vm: EditProfileViewModel,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
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
        vm: RecoverKeysViewModel
    ) = ComposeUIViewController {
        RootScreenIOS {
            RecoverKeysScreenCore(
                appState = it,
                vm = vm
            )
        }
    }

    fun ResetPasswordVC(
        vm: ResetPasswordViewModel,
        openLoginScreen: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            ResetPasswordScreenCore(
                appState = it,
                vm = vm,
                openLoginScreen = openLoginScreen
            )
        }
    }

    fun WelcomeOnboardingScreen(
        vm: WelcomeOnboardingViewModel,
        isDemo: Boolean,
        onFinish: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            val step = rememberSaveable { mutableStateOf(WelcomeOnboardingContent.STEP_LOCATION) }
            WelcomeOnboardingScreenCore(
                vm = vm,
                isDemo = isDemo,
                step = step,
                onFinish = onFinish
            )
        }
    }
}