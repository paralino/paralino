package app.paralino.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.paralino.android.presentation.settings.generic.NotificationSocketSettingsViewModel
import app.paralino.android.ui.groups.BatteryNotificationsScreen
import app.paralino.android.ui.groups.GroupCreateScreen
import app.paralino.android.ui.groups.GroupDetailScreen
import app.paralino.android.ui.groups.GroupSettingsScreen
import app.paralino.android.ui.history.LocationHistoryDeleteScreen
import app.paralino.android.ui.invite.input.MemberInviteScreen
import app.paralino.android.ui.invite.manage.PersonalCodeScreen
import app.paralino.android.ui.login.LoggedOutScreen
import app.paralino.android.ui.login.LoginScreen
import app.paralino.android.ui.login.PasswordConfirmationScreen
import app.paralino.android.ui.login.ProfileSetupScreen
import app.paralino.android.ui.login.RecoverKeysScreen
import app.paralino.android.ui.login.ResetPasswordScreen
import app.paralino.android.ui.login.VerifyEmailScreen
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.MapHomeScreen
import app.paralino.android.ui.onboarding.FreePlanInfoScreen
import app.paralino.android.ui.onboarding.GroupJoinWelcomeScreen
import app.paralino.android.ui.onboarding.WelcomeOnboardingScreen
import app.paralino.android.ui.personal_space.PersonalSpaceScreen
import app.paralino.android.ui.personal_space.PersonalSpaceSettingsScreen
import app.paralino.android.ui.places.CreatePlaceScreen
import app.paralino.android.ui.places.PlaceAlertsSettingsScreen
import app.paralino.android.ui.places.PlaceDetailScreen
import app.paralino.android.ui.places.PlacesScreen
import app.paralino.android.ui.privacyzones.CreatePrivacyZoneScreen
import app.paralino.android.ui.privacyzones.PrivacyZoneDetailScreen
import app.paralino.android.ui.privacyzones.PrivacyZonesScreen
import app.paralino.android.ui.settings.AppSettingsScreen
import app.paralino.android.ui.settings.AppStatusDebugScreen
import app.paralino.android.ui.settings.AppStatusScreen
import app.paralino.android.ui.settings.BlocklistScreen
import app.paralino.android.ui.settings.GeneralInfoScreen
import app.paralino.android.ui.settings.GenericSettingsScreen
import app.paralino.android.ui.settings.LicencesScreen
import app.paralino.android.ui.settings.LocationSettingsScreen
import app.paralino.android.ui.settings.RawDataScreen
import app.paralino.android.ui.settings.SettingsScreen
import app.paralino.android.ui.settings.billing.LicenceCodeScreen
import app.paralino.android.ui.settings.billing.PaywallScreen
import app.paralino.android.ui.settings.billing.SubscriptionScreen
import app.paralino.android.ui.settings.profile.AccountScreen
import app.paralino.android.ui.settings.profile.ChangeEmailScreen
import app.paralino.android.ui.settings.profile.ChangePasswordScreen
import app.paralino.android.ui.settings.profile.ConvertAccountScreen
import app.paralino.android.ui.settings.profile.EditProfileScreen
import app.paralino.android.ui.settings.profile.UserDevicesScreen
import app.paralino.android.ui.share.SharingStartScreen
import app.paralino.core.presentation.login.LoginScreenState
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.places.detail.PlaceSensitivityViewModel
import app.paralino.core.presentation.settings.generic.AppThemeViewModel
import app.paralino.core.presentation.settings.generic.MeasurementSystemViewModel
import app.paralino.core.presentation.settings.generic.NotificationsGroupingViewModel
import app.paralino.core.presentation.settings.generic.PersonalSpaceFeatureSettingsViewModel
import app.paralino.core.presentation.settings.generic.PlaceSearchSettingsViewModel
import app.paralino.core.presentation.settings.generic.maps.MapInteractionsViewModel
import app.paralino.core.presentation.settings.generic.maps.MapsProviderViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    appState: MainAppState,
    onAppClose: () -> Unit
) {
    NavDisplay(
        modifier = modifier,
        backStack = appState.navController.backStack,
        onBack = { appState.navController.popBackStack() },
        entryDecorators = rememberAppEntryDecorators(),
        transitionSpec = { slidePushTransition() },
        popTransitionSpec = { slidePopTransition() },
        predictivePopTransitionSpec = { slidePopTransition() },
        entryProvider = entryProvider {
            homeEntries(appState = appState, onAppClose = onAppClose)
            settingsEntries(appState)
            commonEntries(appState)
        }
    )
}

@Composable
fun rememberAppEntryDecorators(): List<NavEntryDecorator<NavKey>> = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)

private fun AnimatedContentTransitionScope<*>.slidePushTransition(): ContentTransform =
    slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) togetherWith
            slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, targetOffset = { it / 4 })

private fun AnimatedContentTransitionScope<*>.slidePopTransition(): ContentTransform =
    slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, initialOffset = { -it / 4 }) togetherWith
            slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End)

private val slideTransitionMetadata: Map<String, Any> =
    NavDisplay.transitionSpec { slidePushTransition() } +
            NavDisplay.popTransitionSpec { slidePopTransition() } +
            NavDisplay.predictivePopTransitionSpec { slidePopTransition() }

fun EntryProviderScope<NavKey>.homeEntries(
    appState: MainAppState,
    onAppClose: () -> Unit,
) {
    entry<AppScreen.MainHome.MainMap> {
        MapHomeScreen(appState = appState, groupId = it.groupId, onClose = onAppClose)
    }
    entry<AppScreen.MainHome.GroupDetails> {
        GroupDetailScreen(appState = appState, groupId = it.groupId)
    }
    entry<AppScreen.MainHome.GroupSettings> {
        GroupSettingsScreen(appState = appState, groupId = it.groupId)
    }
    entry<AppScreen.MainHome.BatteryNotificationSettings> {
        BatteryNotificationsScreen(appState = appState, groupId = it.groupId)
    }
    entry<AppScreen.MainHome.GroupCreate> {
        GroupCreateScreen(appState = appState, groupId = it.groupId)
    }
    entry<AppScreen.MainHome.GroupJoinWelcome> {
        GroupJoinWelcomeScreen(appState = appState, groupId = it.groupId)
    }
    entry<AppScreen.MainHome.GroupMemberAdd> {
        MemberInviteScreen(appState = appState, groupId = it.groupId, code = it.code, isOnboarding = it.isOnboarding)
    }
    entry<AppScreen.MainHome.SharingStart> {
        SharingStartScreen(appState = appState, groupId = it.groupId, presentationType = it.presentation)
    }
    entry<AppScreen.MainHome.PlacesList> {
        PlacesScreen(appState = appState, groupId = it.groupId, isOnboarding = it.isOnboarding)
    }
    entry<AppScreen.MainHome.PrivacyZonesList> {
        PrivacyZonesScreen(appState = appState, groupId = it.groupId, isOnboarding = it.isOnboarding)
    }
    entry<AppScreen.MainHome.PrivacyZoneCreate> {
        CreatePrivacyZoneScreen(
            appState = appState,
            groupId = it.groupId,
            zoneId = it.zoneId,
            editStep = it.editStep,
            lat = it.lat,
            lng = it.lng
        )
    }
    entry<AppScreen.MainHome.PrivacyZoneDetail> {
        PrivacyZoneDetailScreen(
            appState = appState,
            groupId = it.groupId,
            zoneId = it.zoneId
        )
    }
    entry<AppScreen.MainHome.PlaceDetail> {
        PlaceDetailScreen(appState = appState, groupId = it.groupId, placeId = it.placeId)
    }
    entry<AppScreen.MainHome.PlaceAlerts> {
        PlaceAlertsSettingsScreen(appState = appState, groupId = it.groupId, placeId = it.placeId)
    }
    entry<AppScreen.MainHome.PlaceCreate> { key ->
        CreatePlaceScreen(
            appState = appState,
            groupId = key.groupId,
            placeId = key.placeId,
            editStep = key.editStep,
            // both lat & lng have to be valid
            lat = key.lat.takeIf { key.lng != null },
            lng = key.lng.takeIf { key.lat != null }
        )
    }
    entry<AppScreen.MainHome.FreePlanNoticeScreen> {
        FreePlanInfoScreen(appState)
    }
    entry<AppScreen.MainHome.LocationHistoryDelete> {
        LocationHistoryDeleteScreen(
            appState = appState,
            groupId = it.groupId
        )
    }
    entry<AppScreen.MainHome.PersonalSpace> {
        PersonalSpaceScreen(appState = appState)
    }
    entry<AppScreen.MainHome.PersonalSpaceSettings> {
        PersonalSpaceSettingsScreen(appState = appState)
    }
    entry<AppScreen.MainHome.PlaceSensitivity> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<PlaceSensitivityViewModel> { parametersOf(it.groupId, it.placeId) }
        )
    }
}

fun EntryProviderScope<NavKey>.settingsEntries(appState: MainAppState) {
    entry<AppScreen.Settings.SettingsRoot> {
        SettingsScreen(appState)
    }
    entry<AppScreen.Settings.ProfileDetails> {
        AccountScreen(appState)
    }
    entry<AppScreen.Settings.PersonalCodes> {
        PersonalCodeScreen(appState)
    }
    entry<AppScreen.Settings.BlockList> {
        BlocklistScreen(appState)
    }
    entry<AppScreen.Settings.UserDevices> {
        UserDevicesScreen(appState)
    }
    entry<AppScreen.Settings.ProfileDetails.EditProfile> {
        EditProfileScreen(appState)
    }
    entry<AppScreen.Settings.ProfileDetails.ChangeEmail> {
        ChangeEmailScreen(
            appState = appState,
            onNavigateBack = {
                // do nothing, email verification will trigger new graph
            }
        )
    }
    entry<AppScreen.Settings.ProfileDetails.ChangePassword> {
        ChangePasswordScreen(appState)
    }
    entry<AppScreen.Settings.ProfileDetails.ConvertAccount> {
        ConvertAccountScreen(appState)
    }
    entry<AppScreen.Settings.Licences> {
        LicencesScreen(appState)
    }
    entry<AppScreen.Settings.LicenceCode> {
        LicenceCodeScreen(appState)
    }
    entry<AppScreen.Settings.AppStatusDebug> {
        AppStatusDebugScreen(appState)
    }
    entry<AppScreen.Settings.AppStatus> {
        AppStatusScreen(appState)
    }
    entry<AppScreen.Settings.LocationSettings> {
        LocationSettingsScreen(appState)
    }
    entry<AppScreen.Settings.GeneralInfo> {
        GeneralInfoScreen(appState)
    }
    entry<AppScreen.Settings.WelcomeOnboarding> {
        WelcomeOnboardingScreen(
            isDemo = true,
            onFinish = { appState.navController.popBackStack() }
        )
    }
    entry<AppScreen.Settings.AppSettings> {
        AppSettingsScreen(appState)
    }
    entry<AppScreen.Settings.AppTheme> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<AppThemeViewModel>()
        )
    }
    entry<AppScreen.Settings.MeasurementSystem> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<MeasurementSystemViewModel>()
        )
    }
    entry<AppScreen.Settings.NotificationGrouping> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<NotificationsGroupingViewModel>()
        )
    }
    entry<AppScreen.Settings.MapInteractions> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<MapInteractionsViewModel>()
        )
    }
    entry<AppScreen.Settings.PlaceSearch> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<PlaceSearchSettingsViewModel>()
        )
    }
    entry<AppScreen.Settings.PersonalSpaceFeature> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<PersonalSpaceFeatureSettingsViewModel>()
        )
    }
    entry<AppScreen.Settings.NotificationSocket> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<NotificationSocketSettingsViewModel>()
        )
    }
    entry<AppScreen.Settings.MapsProvider> {
        GenericSettingsScreen(
            appState = appState,
            vm = koinViewModel<MapsProviderViewModel>()
        )
    }
}

fun EntryProviderScope<NavKey>.commonEntries(appState: MainAppState) {
    entry<AppScreen.Common.RawDataScreen> {
        RawDataScreen(appState = appState, type = it.type)
    }
    entry<AppScreen.Common.Subscriptions> {
        SubscriptionScreen(appState)
    }
    entry<AppScreen.Common.BillingPaywall> {
        PaywallScreen(appState, groupId = it.groupId, type = it.type)
    }
}

fun EntryProviderScope<NavKey>.loggedOutEntries(appState: MainAppState) {
    entry<AppScreen.LoggedOut.Welcome> {
        LoggedOutScreen(appState)
    }
    entry<AppScreen.LoggedOut.WelcomeOnboarding> {
        WelcomeOnboardingScreen(
            isDemo = false,
            onFinish = {
                appState.navController.navigate(
                    screen = AppScreen.LoggedOut.Login(LoginScreenUiType.SIGNUP),
                    popUpTo = AppScreen.LoggedOut.WelcomeOnboarding::class,
                    inclusive = true
                )
            }
        )
    }
    entry<AppScreen.LoggedOut.Login> {
        LoginScreen(appState, it.screenUiType ?: LoginScreenState.defaultStartScreen)
    }
    entry<AppScreen.LoggedOut.ProfileSetup> {
        ProfileSetupScreen(appState)
    }
    entry<AppScreen.LoggedOut.AccountRecovery> {
        RecoverKeysScreen(appState)
    }
    entry<AppScreen.LoggedOut.PasswordConfirmation> {
        PasswordConfirmationScreen(appState)
    }
    entry<AppScreen.LoggedOut.EmailVerification>(metadata = slideTransitionMetadata) {
        VerifyEmailScreen(appState)
    }
    entry<AppScreen.LoggedOut.ChangeEmail>(metadata = slideTransitionMetadata) {
        ChangeEmailScreen(appState, onNavigateBack = { appState.navController.popBackStack() })
    }
    entry<AppScreen.LoggedOut.PasswordReset> {
        ResetPasswordScreen(appState, userId = it.userId, secret = it.secret)
    }
}
