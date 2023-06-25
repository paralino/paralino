package app.paralino.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.presentation.settings.blocklist.BlocklistVideModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.invite.manage.PersonalCodeScreenCore
import app.paralino.core.ui.settings.BlocklistScreenCore
import app.paralino.core.ui.settings.RawDataScreenCore
import app.paralino.core.ui.settings.SettingsScreenCore
import app.paralino.core.ui.settings.profile.AccountScreenCore
import app.paralino.core.ui.settings.profile.ChangeEmailScreenCore
import app.paralino.core.ui.settings.profile.ChangePasswordScreenCore
import app.paralino.core.ui.settings.profile.ConvertAccountScreenCore
import app.paralino.core.ui.settings.profile.EditProfileScreen

object SettingsScreens {
    fun SettingsScreenVC(
        vm: SettingsViewModel,
        onShareApp: (String) -> Unit,
        openConvertAccount: () -> Unit,
        openProfileDetails: () -> Unit,
        openPersonalCodes: () -> Unit,
        openBlockList: () -> Unit,
        openLicences: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            SettingsScreenCore(
                appState = rememberCoreUiState(),
                viewModel = vm,
                onShareApp = onShareApp,
                openConvertAccount = openConvertAccount,
                openProfileDetails = openProfileDetails,
                openPersonalCodes = openPersonalCodes,
                openBlockList = openBlockList,
                openLicences = openLicences
            )
        }
    }

    fun AccountScreenVC(
        vm: SettingsViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        openProfileRawScreen: () -> Unit,
        openConvertAccount: () -> Unit,
        openEditProfile: () -> Unit,
        openChangeEmail: () -> Unit,
        openChangePassword: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            AccountScreenCore(
                appState = rememberCoreUiState(),
                viewModel = vm,
                openConvertAccount = openConvertAccount,
                onShowDialog = onShowDialog,
                openProfileRawScreen = openProfileRawScreen,
                openEditProfile = openEditProfile,
                openChangePassword = openChangePassword,
                openChangeEmail = openChangeEmail
            )
        }
    }

    fun PersonalCodeScreenVC(
        vm: PersonalCodeViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        onShare: (String) -> Unit,
        onNavigateBack: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            PersonalCodeScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                onShowDialog = onShowDialog,
                onShare = onShare,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun EditProfileScreenVC(
        vm: EditProfileViewModel,
        onBottomSheetAction: (BottomSheetsScreen) -> Unit,
        onExit: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            EditProfileScreen(
                appState = rememberCoreUiState(),
                vm = vm,
                onBottomSheetAction = onBottomSheetAction,
                onExit = onExit,
            )
        }
    }

    fun BlocklistScreenVC(
        vm: BlocklistVideModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        onNavigateBack: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            BlocklistScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                onShowDialog = onShowDialog,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun ConvertAccountScreenVC(
        vm: ConvertAccountViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            ConvertAccountScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun ChangeEmailScreenVC(
        vm: ChangeEmailViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            ChangeEmailScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun ChangePasswordScreenVC(
        vm: ChangePasswordViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            ChangePasswordScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun RawDataScreenVC(
        vm: RawDataPresentationViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            RawDataScreenCore(
                appState = rememberCoreUiState(),
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}