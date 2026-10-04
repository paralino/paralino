package app.paralino.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.models.settings.AppThemeType
import app.paralino.core.presentation.paralinoid.PersonalCodeViewModel
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.presentation.settings.billing.PaywallScreenEvent
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.SubscriptionViewModel
import app.paralino.core.presentation.settings.billing.licence.LicenceCodeViewModel
import app.paralino.core.presentation.settings.blocklist.BlocklistViewModel
import app.paralino.core.presentation.settings.userdevices.UserDevicesViewModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.general.GeneralInfoViewModel
import app.paralino.core.presentation.settings.generic.AppSettingsViewModel
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.location.LocationSettingsViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.ui.invite.manage.PersonalCodeScreenCore
import app.paralino.core.ui.settings.AppSettingsScreenCore
import app.paralino.core.ui.settings.AppStatusDebugScreenCore
import app.paralino.core.ui.settings.BlocklistScreenCore
import app.paralino.core.ui.settings.GeneralInfoScreenCore
import app.paralino.core.ui.settings.GenericSettingsScreenCore
import app.paralino.core.ui.settings.LicencesScreenCore
import app.paralino.core.ui.settings.RawDataScreenCore
import app.paralino.core.ui.settings.SettingsScreenCore
import app.paralino.core.ui.settings.billing.PaywallScreenCore
import app.paralino.core.ui.settings.billing.SubscriptionScreenCore
import app.paralino.core.ui.settings.billing.licence.LicenceCodeScreenCore
import app.paralino.core.ui.settings.location.LocationSettingsScreenCore
import app.paralino.core.ui.settings.profile.AccountScreenCore
import app.paralino.core.ui.settings.profile.UserDevicesScreenCore
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
        openGeneralInfo: () -> Unit,
        openAppStatus: () -> Unit,
        openSubscriptions: () -> Unit,
        openPaywall: () -> Unit,
        openAppSettings: () -> Unit,
        openLocationSettings: () -> Unit,
        onShare: (String) -> Unit,
        askNotificationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            SettingsScreenCore(
                appState = it,
                viewModel = vm,
                onShareApp = onShareApp,
                openConvertAccount = openConvertAccount,
                openProfileDetails = openProfileDetails,
                openPersonalCodes = openPersonalCodes,
                openBlockList = openBlockList,
                openGeneralInfo = openGeneralInfo,
                openAppStatus = openAppStatus,
                openSubscriptions = openSubscriptions,
                openPaywall = openPaywall,
                openAppSettings = openAppSettings,
                openLocationSettings = openLocationSettings,
                onShareCode = onShare,
                askNotificationPermission = askNotificationPermission
            )
        }
    }

    fun AccountScreenVC(
        vm: SettingsViewModel,
        openConvertAccount: () -> Unit,
        openEditProfile: () -> Unit,
        openChangeEmail: () -> Unit,
        openChangePassword: () -> Unit,
        openProfileRawScreen: () -> Unit,
        openBlockList: () -> Unit,
        openUserDevices: () -> Unit,
        onShare: (String) -> Unit,
        askNotificationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            AccountScreenCore(
                appState = it,
                viewModel = vm,
                openConvertAccount = openConvertAccount,
                openEditProfile = openEditProfile,
                openChangePassword = openChangePassword,
                openChangeEmail = openChangeEmail,
                openProfileRawScreen = openProfileRawScreen,
                openBlockList = openBlockList,
                openUserDevices = openUserDevices,
                onShare = onShare,
                askNotificationPermission = askNotificationPermission
            )
        }
    }

    fun UserDevicesScreenVC(
        vm: UserDevicesViewModel,
    ) = ComposeUIViewController {
        RootScreenIOS {
            UserDevicesScreenCore(
                appState = it,
                vm = vm,
                contentPadding = PaddingValues()
            )
        }
    }

    fun PersonalCodeScreenVC(
        vm: PersonalCodeViewModel,
        askNotificationPermission: () -> Unit,
        onShare: (String) -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            PersonalCodeScreenCore(
                appState = it,
                vm = vm,
                askNotificationPermission = askNotificationPermission,
                onShare = onShare,
            )
        }
    }

    fun EditProfileScreenVC(
        vm: EditProfileViewModel,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
        onExit: () -> Unit
    ) = ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        RootScreenIOS {
            EditProfileScreen(
                appState = it,
                vm = vm,
                onBottomSheetAction = onBottomSheetAction,
                onExit = onExit,
            )
        }
    }

    fun BlocklistScreenVC(
        vm: BlocklistViewModel
    ) = ComposeUIViewController {
        RootScreenIOS {
            BlocklistScreenCore(
                appState = it,
                vm = vm,
                paddingValues = PaddingValues()
            )
        }
    }

    fun ConvertAccountScreenVC(
        vm: ConvertAccountViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        RootScreenIOS {
            ConvertAccountScreenCore(
                appState = it,
                vm = vm,
                contentPadding = PaddingValues(),
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun ChangeEmailScreenVC(
        vm: ChangeEmailViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        RootScreenIOS {
            ChangeEmailScreenCore(
                appState = it,
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun ChangePasswordScreenVC(
        vm: ChangePasswordViewModel,
        onNavigateBack: () -> Unit
    ) = ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        RootScreenIOS {
            ChangePasswordScreenCore(
                appState = it,
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun RawDataScreenVC(
        vm: RawDataPresentationViewModel
    ) = ComposeUIViewController {
        RootScreenIOS {
            RawDataScreenCore(
                appState = it,
                vm = vm,
                paddingValues = PaddingValues()
            )
        }
    }

    fun SubscriptionsScreenVC(
        vm: SubscriptionViewModel,
        onNavigateBack: () -> Unit,
        openBillingStore: () -> Unit,
        openPaywall: () -> Unit,
        openLicenceScreen: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            SubscriptionScreenCore(
                appState = it,
                vm = vm,
                paddingValues = PaddingValues(bottom = 30.dp),
                onNavigateBack = onNavigateBack,
                openBillingStore = openBillingStore,
                openPaywall = openPaywall,
                openLicenceScreen = openLicenceScreen
            )
        }
    }

    fun PaywallScreenVC(
        vm: PaywallViewModel,
        paddingValues: AppleScreenPaddingValues,
        onNavigateBack: (Boolean) -> Unit,
        onStartPurchase: (PaywallScreenEvent.OnStartPurchase) -> Unit,
        onRestorePurchase: (PaywallScreenEvent.OnRestorePurchase) -> Unit,
        openLicenceScreen: () -> Unit,
        openBillingStore: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            PaywallScreenCore(
                appState = it,
                vm = vm,
                paddingValues = paddingValues.toComposePaddingValues(),
                onNavigateBack = onNavigateBack,
                onStartPurchase = onStartPurchase,
                onRestorePurchase = onRestorePurchase,
                openLicenceScreen = openLicenceScreen,
                openBillingStore = openBillingStore
            )
        }
    }

    fun LicenceCodeScreenVC(
        vm: LicenceCodeViewModel,
        onNavigateBack: () -> Unit,
    ) = ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        RootScreenIOS {
            LicenceCodeScreenCore(
                appState = it,
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun AppStatusDebugScreenVC(
        vm: AppStatusDebugViewModel
    ) = ComposeUIViewController {
        RootScreenIOS {
            AppStatusDebugScreenCore(
                appState = it,
                vm = vm,
                paddingValues = PaddingValues(bottom = 30.dp)
            )
        }
    }

    fun LicencesScreenVC() = ComposeUIViewController {
        RootScreenIOS {
            LicencesScreenCore(
                coreUiState = it,
                contentPadding = PaddingValues(bottom = 20.dp)
            )
        }
    }

    fun GeneralInfoScreenVC(
        vm: GeneralInfoViewModel,
        openLicencesScreen: () -> Unit,
        onShowOnboarding: () -> Unit,
        onContactSupport: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GeneralInfoScreenCore(
                appState = it,
                vm = vm,
                paddingValues = PaddingValues(),
                onLicencesClick = openLicencesScreen,
                onShowOnboarding = onShowOnboarding,
                onContactSupport = onContactSupport
            )
        }
    }

    fun LocationSettingsScreenVC(
        vm: LocationSettingsViewModel,
        openPaywall: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            LocationSettingsScreenCore(
                coreUiState = it,
                vm = vm,
                paddingValues = PaddingValues(bottom = 20.dp),
                openPaywall = openPaywall
            )
        }
    }

    fun AppSettingsScreenVC(
        vm: AppSettingsViewModel,
        openAppThemeScreen: () -> Unit,
        openMeasurementSystemScreen: () -> Unit,
        openNotificationGroupingScreen: () -> Unit,
        openMapInteractionsScreen: () -> Unit,
        openNavigationAppScreen: () -> Unit,
        openPlaceSearchSettings: () -> Unit,
        openPersonalSpaceSettings: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            AppSettingsScreenCore(
                vm = vm,
                appState = it,
                paddingValues = PaddingValues(),
                onAppThemeClick = openAppThemeScreen,
                onMeasurementSystemClick = openMeasurementSystemScreen,
                onNotificationGroupingClick = openNotificationGroupingScreen,
                onMapInteractionsClick = openMapInteractionsScreen,
                onNavigationAppClick = openNavigationAppScreen,
                onPlaceSearchSettingsClick = openPlaceSearchSettings,
                onPersonalSpaceSettingsClick = openPersonalSpaceSettings,
                // not available in iOS
                onMapSourceSettingsClick = {},
                onNotificationSocketSettingsClick = {}
            )
        }
    }

    fun GenericSettingsScreenVC(
        vm: BaseGenericSettingViewModel,
        openPaywall: () -> Unit,
        askNotificationPermission: () -> Unit,
        askBackgroundAccessPermission: () -> Unit,
        applyAppTheme: (AppThemeType) -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GenericSettingsScreenCore(
                coreUiState = it,
                paddingValues = PaddingValues(),
                vm = vm,
                openPaywall = openPaywall,
                // not available in iOS
                mapContent = {},
                openNotificationSocketChannelSettings = {},
                askNotificationPermission = askNotificationPermission,
                askBackgroundAccessPermission = askBackgroundAccessPermission,
                applyAppTheme = applyAppTheme,
            )
        }
    }
}