package app.paralino.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.presentation.groups.detail.GroupDetailViewModel
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.presentation.groups.settings.BatteryNotificationsViewModel
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
import app.paralino.core.presentation.history.LocationHistoryDeleteViewModel
import app.paralino.core.presentation.groups.add_member.MemberInviteViewModel
import app.paralino.core.presentation.onboarding.AccountOnboardingViewModel
import app.paralino.core.presentation.personal_space.PersonalSpaceViewModel
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsViewModel
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesListScreenState
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesListScreenState
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesViewModel
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.ui.groups.BatteryNotificationsScreenCore
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.ui.groups.GroupDetailScreenCore
import app.paralino.core.ui.groups.GroupSettingsScreenCore
import app.paralino.core.ui.history.LocationHistoryDeleteScreenCore
import app.paralino.core.ui.invite.MemberInviteScreenCore
import app.paralino.core.ui.onboarding.FreePlanInfoScreenCore
import app.paralino.core.ui.onboarding.account.AccountOnboardingScreenCore
import app.paralino.core.ui.personal_space.PersonalSpaceScreenCore
import app.paralino.core.ui.personal_space.PersonalSpaceSettingsScreenCore
import app.paralino.core.ui.places.CreatePlaceScreenCore
import app.paralino.core.ui.places.PlaceAlertsSettingsScreenCore
import app.paralino.core.ui.places.PlaceDetailScreenCore
import app.paralino.core.ui.places.PlacesScreenCore
import app.paralino.core.ui.privacyzones.PrivacyZonesScreenCore
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.ui.share.SharingStartScreenCore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIViewController

object HomeScreens {
    fun AccountOnboardingScreenVC(
        vm: AccountOnboardingViewModel,
        paddingValues: AppleScreenPaddingValues,
        onCreateGroup: () -> Unit,
        openPersonalCodes: () -> Unit,
        openPaywall: (BillingScreenSourceType) -> Unit,
        onSharePersonalCode: (String) -> Unit,
        openSharingScreen: (GroupOrSpaceId) -> Unit,
        askNotificationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            AccountOnboardingScreenCore(
                modifier = Modifier.padding(paddingValues.toComposePaddingValues()),
                appState = it,
                vm = vm,
                onCreateGroup = onCreateGroup,
                openPersonalCodes = openPersonalCodes,
                openPaywall = openPaywall,
                onSharePersonalCode = onSharePersonalCode,
                openSharingScreen = openSharingScreen,
                askNotificationPermission = askNotificationPermission,
            )
        }
    }

    fun GroupCreateScreenVC(
        vm: CreateGroupViewModel,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
        groupUpdated: (String) -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupCreateScreenCore(
                appState = it,
                vm = vm,
                groupUpdated = groupUpdated,
                onBottomSheetAction = onBottomSheetAction,
            )
        }
    }

    fun GroupDetailScreenVC(
        vm: GroupDetailViewModel,
        groupId: String?,
        paddingValues: AppleScreenPaddingValues,
        openMemberAdd: () -> Unit,
        openMap: () -> Unit,
        openPlaces: () -> Unit,
        openGroupSettings: () -> Unit,
        openMemberRawData: (String) -> Unit,
        openSharing: () -> Unit,
        openPaywall: () -> Unit,
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
                contentPadding = paddingValues.toComposePaddingValues(),
                openMemberAdd = openMemberAdd,
                openMap = openMap,
                openPlaces = openPlaces,
                openGroupSettings = openGroupSettings,
                openMemberRawData = openMemberRawData,
                openSharing = openSharing,
                openPaywall = openPaywall,
                onNavigateBack = onNavigateBack,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }

    fun GroupSettingsScreenVC(
        vm: GroupSettingsViewModel,
        openEditGroup: () -> Unit,
        onGroupDeleted: () -> Unit,
        openRawDataScreen: () -> Unit,
        openBatteryNotificationsSettings: () -> Unit,
        openLocationHistorySettings: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupSettingsScreenCore(
                appState = it,
                vm = vm,
                openEditGroup = openEditGroup,
                onGroupDeleted = onGroupDeleted,
                openRawDataScreen = openRawDataScreen,
                openBatteryNotificationsSettings = openBatteryNotificationsSettings,
                openLocationHistorySettings = openLocationHistorySettings
            )
        }
    }

    fun BatteryNotificationsSettingsScreenVC(
        vm: BatteryNotificationsViewModel,
        paddingValues: AppleScreenPaddingValues,
        openPaywall: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            BatteryNotificationsScreenCore(
                appState = it,
                vm = vm,
                contentPadding = paddingValues.toComposePaddingValues(),
                openPaywall = openPaywall
            )
        }
    }

    fun MemberInviteScreenVC(
        vm: MemberInviteViewModel,
        isOnboarding: Boolean,
        onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
        openPaywall: (BillingScreenSourceType) -> Unit,
        onMemberAdded: () -> Unit,
        onSkipOnboarding: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            MemberInviteScreenCore(
                appState = it,
                vm = vm,
                isOnboarding = isOnboarding,
                contentPadding = PaddingValues(),
                onPickGroup = onPickGroup,
                openPaywall = openPaywall,
                onMemberAdded = onMemberAdded,
                onSkipOnboarding = onSkipOnboarding
            )
        }
    }

    fun PersonalSpaceScreenVC(
        vm: PersonalSpaceViewModel,
        paddingValues: AppleScreenPaddingValues,
        openSettings: () -> Unit,
        openTimeline: () -> Unit,
        openMap: () -> Unit,
        openPlaces: () -> Unit,
        openSharing: () -> Unit,
        openPaywall: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            PersonalSpaceScreenCore(
                appState = it,
                vm = vm,
                contentPadding = paddingValues.toComposePaddingValues(),
                openSettings = openSettings,
                openTimeline = openTimeline,
                openMap = openMap,
                openPlaces = openPlaces,
                openSharing = openSharing,
                openPaywall = openPaywall,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }

    fun PersonalSpaceSettingsScreenVC(
        vm: PersonalSpaceSettingsViewModel,
        openRawDataScreen: () -> Unit,
        openBatteryNotificationsSettings: () -> Unit,
        openLocationHistorySettings: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            PersonalSpaceSettingsScreenCore(
                appState = it,
                vm = vm,
                openRawDataScreen = openRawDataScreen,
                openBatteryNotificationsSettings = openBatteryNotificationsSettings,
                openLocationHistorySettings = openLocationHistorySettings
            )
        }
    }

    fun StartSharingScreenVC(
        vm: SharingStartViewModel,
        onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
        onSharingStarted: () -> Unit,
        onSkipOnboarding: () -> Unit,
        openSubscriptions: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
        askBackgroundAccessPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            SharingStartScreenCore(
                appState = it,
                vm = vm,
                onPickGroup = onPickGroup,
                onSharingStarted = onSharingStarted,
                onSkipOnboarding = onSkipOnboarding,
                openSubscriptions = openSubscriptions,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission,
                askBackgroundAccessPermission = askBackgroundAccessPermission,
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
    fun PlacesScreenVC(
        vm: PlacesViewModel,
        paddingValues: AppleScreenPaddingValues,
        addNewPlace: () -> Unit,
        openPaywall: () -> Unit,
        onSkipOnboarding: () -> Unit,
        openPlaceDetailScreen: (place: PlaceListItemData) -> Unit,
        askNotificationPermission: () -> Unit,
        mapUIView: (PlacesListScreenState) -> UIViewController,
    ) = ComposeUIViewController {
        RootScreenIOS {

            var userScroll by remember {
                mutableStateOf(true)
            }

            PlacesScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = paddingValues.toComposePaddingValues(),
                askNotificationPermission = askNotificationPermission,
                addNewPlace = addNewPlace,
                openPaywall = openPaywall,
                onSkipOnboarding = onSkipOnboarding,
                openPlaceDetails = openPlaceDetailScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = { mapUIView(state) },
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
    fun PrivacyZonesScreenVC(
        vm: PrivacyZonesViewModel,
        paddingValues: AppleScreenPaddingValues,
        addNewPrivacyZone: () -> Unit,
        openPaywall: () -> Unit,
        onSkipOnboarding: () -> Unit,
        openPrivacyZoneDetailScreen: (zone: PlaceListItemData) -> Unit,
        mapUIView: (PrivacyZonesListScreenState) -> UIViewController,
    ) = ComposeUIViewController {
        RootScreenIOS {
            var userScroll by remember {
                mutableStateOf(true)
            }

            PrivacyZonesScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = paddingValues.toComposePaddingValues(),
                addNewPrivacyZone = addNewPrivacyZone,
                openPaywall = openPaywall,
                onSkipOnboarding = onSkipOnboarding,
                openPrivacyZoneDetails = openPrivacyZoneDetailScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = { mapUIView(state) },
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun PlaceDetailScreenVC(
        vm: PlaceDetailViewModel,
        paddingValues: AppleScreenPaddingValues,
        openPlaceRawDataScreen: () -> Unit,
        openPlaceAlertsScreen: () -> Unit,
        openPlaceSensitivityScreen: () -> Unit,
        openPlaceEditScreen: () -> Unit,
        onNavigateBack: () -> Unit,
        mapUIView: (PlaceDetailScreenState) -> UIViewController,
    ) = ComposeUIViewController {
        RootScreenIOS {

            var userScroll by remember {
                mutableStateOf(true)
            }

            PlaceDetailScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = paddingValues.toComposePaddingValues(),
                onNavigateBack = onNavigateBack,
                openPlaceRawDataScreen = openPlaceRawDataScreen,
                openPlaceAlertsScreen = openPlaceAlertsScreen,
                openPlaceSensitivityScreen = openPlaceSensitivityScreen,
                openPlaceEditScreen = openPlaceEditScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = { mapUIView(state) },
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun PlaceAlertsSettingsScreenVC(
        vm: PlaceDetailViewModel,
        paddingValues: AppleScreenPaddingValues,
    ) = ComposeUIViewController {
        RootScreenIOS {
            PlaceAlertsSettingsScreenCore(
                appState = it,
                vm = vm,
                contentPadding = paddingValues.toComposePaddingValues()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun PlaceCreateScreenVC(
        vm: CreatePlaceViewModel,
        onNavigateBack: () -> Unit,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
        mapUIView: () -> UIViewController,
    ) = ComposeUIViewController {
        RootScreenIOS {
            CreatePlaceScreenCore(
                appState = it,
                vm = vm,
                onNavigateBack = onNavigateBack,
                onBottomSheetAction = onBottomSheetAction,
                mapContent = {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = mapUIView,
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun LocationHistoryDeleteScreenVC(
        vm: LocationHistoryDeleteViewModel,
        paddingValues: AppleScreenPaddingValues,
        onNavigateBack: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            LocationHistoryDeleteScreenCore(
                appState = it,
                vm = vm,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    fun FreePlanInfoScreenVC(
        vm: PlanInfoViewModel,
        paddingValues: AppleScreenPaddingValues,
        onContinue: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            FreePlanInfoScreenCore(
                coreUiState = it,
                vm = vm,
                paddingValues = paddingValues.toComposePaddingValues(),
                onContinue = onContinue,
            )
        }
    }
}
