package app.paralino.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
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
import app.paralino.core.ui.components.DefaultEmptyState
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
import app.paralino.core.MR
import app.paralino.core.presentation.login.LoginScreenState
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.places.detail.PlaceSensitivityViewModel
import app.paralino.core.models.settings.RawPresentationDataType
import dev.icerock.moko.resources.compose.stringResource
import app.paralino.core.presentation.settings.generic.maps.MapsProviderViewModel
import app.paralino.core.presentation.settings.generic.maps.MapInteractionsViewModel
import app.paralino.core.presentation.settings.generic.AppThemeViewModel
import app.paralino.core.presentation.settings.generic.MeasurementSystemViewModel
import app.paralino.core.presentation.settings.generic.NotificationsGroupingViewModel
import app.paralino.core.presentation.settings.generic.PersonalSpaceFeatureSettingsViewModel
import app.paralino.core.presentation.settings.generic.PlaceSearchSettingsViewModel
import app.paralino.android.presentation.settings.generic.NotificationSocketSettingsViewModel
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.presentation.places.create.CreatePlaceScreenStep
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenStep
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    appState: MainAppState,
    onAppClose: () -> Unit
) {
    NavHost(
        modifier = modifier,
        navController = appState.navController.navController,
        startDestination = AppScreen.MainHome.routeSignature,
        enterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, targetOffset = { it / 4 }) },
        popEnterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, initialOffset = { -it / 4 }) },
        popExitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End) },
        predictivePopEnterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, initialOffset = { -it / 4 }) },
        predictivePopExitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        homeGraph(appState = appState, onAppClose = onAppClose)
        settingsGraph(appState)
    }
}

fun NavGraphBuilder.homeGraph(
    appState: MainAppState,
    onAppClose: () -> Unit,
) {
    navigation(
        startDestination = AppScreen.MainHome.MainMap.routeSignature,
        route = AppScreen.MainHome.routeSignature
    ) {
        composable(AppScreen.MainHome.MainMap.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.MainMap.argumentGroupId)
            MapHomeScreen(appState = appState, groupId = groupId, onClose = onAppClose)
        }
        composable(AppScreen.MainHome.GroupDetails.routeSignature) {
            val groupId =
                it.arguments!!.getString(AppScreen.MainHome.GroupDetails.argumentGroupId)
            GroupDetailScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.MainHome.GroupSettings.routeSignature) {
            val groupId = it.arguments!!.getString(AppScreen.MainHome.GroupSettings.argumentGroupId)
            GroupSettingsScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.MainHome.BatteryNotificationSettings.routeSignature) {
            val groupId = it.arguments!!.getString(AppScreen.MainHome.BatteryNotificationSettings.argumentGroupId)!!
            BatteryNotificationsScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.MainHome.GroupCreate.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.GroupCreate.argumentGroupId)
            GroupCreateScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.MainHome.GroupJoinWelcome.routeSignature) {
            val groupId =
                it.arguments!!.getString(AppScreen.MainHome.GroupJoinWelcome.argumentGroupId)!!
            GroupJoinWelcomeScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.MainHome.GroupMemberAdd.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.GroupMemberAdd.argumentGroupId)
            val code =
                it.arguments?.getString(AppScreen.MainHome.GroupMemberAdd.argumentCode)
            val onboarding =
                it.arguments?.getString(AppScreen.MainHome.GroupMemberAdd.argumentOnboarding)?.toBoolean() ?: false
            MemberInviteScreen(appState = appState, groupId = groupId, code = code, isOnboarding = onboarding)
        }
        composable(AppScreen.MainHome.SharingStart.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.SharingStart.argumentGroupId)
            val presentation = SharingStartPresentationType.fromRouteArgument(
                it.arguments?.getString(AppScreen.MainHome.SharingStart.argumentPresentation)
            )
            SharingStartScreen(appState = appState, groupId = groupId, presentationType = presentation)
        }
        composable(AppScreen.MainHome.PlacesList.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.PlacesList.argumentGroupId)!!
            val onboarding =
                it.arguments?.getString(AppScreen.MainHome.PlacesList.argumentOnboarding)?.toBoolean() ?: false
            PlacesScreen(appState = appState, groupId = groupId, isOnboarding = onboarding)
        }
        composable(AppScreen.MainHome.PrivacyZonesList.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.PrivacyZonesList.argumentGroupId)!!
            val onboarding =
                it.arguments?.getString(AppScreen.MainHome.PrivacyZonesList.argumentOnboarding)?.toBoolean() ?: false
            PrivacyZonesScreen(appState = appState, groupId = groupId, isOnboarding = onboarding)
        }
        composable(AppScreen.MainHome.PrivacyZoneCreate.routeSignature) {
            val groupId = it.arguments?.getString(AppScreen.MainHome.PrivacyZoneCreate.argumentGroupId)!!
            val zoneId = it.arguments?.getString(AppScreen.MainHome.PrivacyZoneCreate.argumentZoneId)
            val editStep = it.arguments?.getString(AppScreen.MainHome.PrivacyZoneCreate.argumentEditStep)?.let {
                CreatePrivacyZoneScreenStep.fromKey(it)
            }
            val lat = it.arguments?.takeIf { it.containsKey(AppScreen.MainHome.PrivacyZoneCreate.argumentLat) }
                ?.getString(AppScreen.MainHome.PrivacyZoneCreate.argumentLat)?.toDoubleOrNull()
            val lng = it.arguments?.takeIf { it.containsKey(AppScreen.MainHome.PrivacyZoneCreate.argumentLng) }
                ?.getString(AppScreen.MainHome.PrivacyZoneCreate.argumentLng)?.toDoubleOrNull()
            CreatePrivacyZoneScreen(
                appState = appState,
                groupId = groupId,
                zoneId = zoneId,
                editStep = editStep,
                lat = lat,
                lng = lng
            )
        }
        composable(AppScreen.MainHome.PrivacyZoneDetail.routeSignature) {
            val groupId = it.arguments?.getString(AppScreen.MainHome.PrivacyZoneDetail.argumentGroupId)!!
            val zoneId = it.arguments?.getString(AppScreen.MainHome.PrivacyZoneDetail.argumentZoneId)!!
            PrivacyZoneDetailScreen(
                appState = appState,
                groupId = groupId,
                zoneId = zoneId
            )
        }
        composable(AppScreen.MainHome.PlaceDetail.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.PlaceDetail.argumentGroupId)!!
            val placeId =
                it.arguments?.getString(AppScreen.MainHome.PlaceDetail.argumentPlaceId)!!
            PlaceDetailScreen(appState = appState, groupId = groupId, placeId = placeId)
        }
        composable(AppScreen.MainHome.PlaceAlerts.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.PlaceAlerts.argumentGroupId)!!
            val placeId =
                it.arguments?.getString(AppScreen.MainHome.PlaceAlerts.argumentPlaceId)!!
            PlaceAlertsSettingsScreen(appState = appState, groupId = groupId, placeId = placeId)
        }
        composable(AppScreen.MainHome.PlaceCreate.routeSignature) {
            val groupId = it.arguments?.getString(AppScreen.MainHome.PlaceCreate.argumentGroupId)!!
            val placeId = it.arguments?.getString(AppScreen.MainHome.PlaceCreate.argumentPlaceId)
            val editStep = it.arguments?.getString(AppScreen.MainHome.PlaceCreate.argumentEditStep)?.let {
                CreatePlaceScreenStep.fromKey(it)
            }
            val lat = it.arguments?.takeIf { it.containsKey(AppScreen.MainHome.PlaceCreate.argumentLat) }
                ?.getString(AppScreen.MainHome.PlaceCreate.argumentLat)?.toDoubleOrNull()
            val lng = it.arguments?.takeIf { it.containsKey(AppScreen.MainHome.PlaceCreate.argumentLng) }
                ?.getString(AppScreen.MainHome.PlaceCreate.argumentLng)?.toDoubleOrNull()
            CreatePlaceScreen(
                appState = appState,
                groupId = groupId,
                placeId = placeId,
                editStep = editStep,
                lat = lat.takeIf { lng != null },
                lng = lng.takeIf { lat != null }
            )
        }
        composable(AppScreen.MainHome.RawDataScreen.routeSignature) {
            val type =
                it.arguments?.getString(AppScreen.MainHome.RawDataScreen.argumentDataType)!!
            val id =
                it.arguments?.getString(AppScreen.MainHome.RawDataScreen.argumentDataId)
            val rawDataType = RawPresentationDataType.fromKey(type, id ?: "")
            RawDataScreen(appState = appState, type = rawDataType!!)
        }
        composable(AppScreen.MainHome.Subscriptions.routeSignature) {
            SubscriptionScreen(appState)
        }
        composable(AppScreen.MainHome.BillingPaywall.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.BillingPaywall.argumentGroupId)
            val type = it.arguments?.getString(AppScreen.MainHome.BillingPaywall.argumentSourceType)
                .let { BillingScreenSourceType.fromKey(it) }
            PaywallScreen(appState, groupId = groupId, type = type)
        }
        composable(AppScreen.MainHome.FreePlanNoticeScreen.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.FreePlanNoticeScreen.argumentGroupId)
            FreePlanInfoScreen(appState)
        }
        composable(AppScreen.MainHome.LocationHistoryDelete.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.LocationHistoryDelete.argumentGroupId)!!
            LocationHistoryDeleteScreen(
                appState = appState,
                groupId = groupId
            )
        }
        composable(AppScreen.MainHome.PersonalSpace.routeSignature) {
            PersonalSpaceScreen(appState = appState)
        }
        composable(AppScreen.MainHome.PersonalSpaceSettings.routeSignature) {
            PersonalSpaceSettingsScreen(appState = appState)
        }
        composable(AppScreen.MainHome.PlaceSensitivity.routeSignature) {
            val groupId = it.arguments?.getString(AppScreen.MainHome.PlaceSensitivity.argumentGroupId)!!
            val placeId = it.arguments?.getString(AppScreen.MainHome.PlaceSensitivity.argumentPlaceId)!!
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<PlaceSensitivityViewModel> { parametersOf(groupId, placeId) }
            )
        }
    }
}

fun NavGraphBuilder.settingsGraph(appState: MainAppState) {
    navigation(
        startDestination = AppScreen.Settings.SettingsRoot.routeSignature,
        route = AppScreen.Settings.routeSignature
    ) {
        composable(AppScreen.Settings.SettingsRoot.routeSignature) {
            SettingsScreen(appState)
        }
        composable(AppScreen.Settings.ProfileDetails.routeSignature) {
            AccountScreen(appState)
        }
        composable(AppScreen.Settings.PersonalCodes.routeSignature) {
            PersonalCodeScreen(appState)
        }
        composable(AppScreen.Settings.BlockList.routeSignature) {
            BlocklistScreen(appState)
        }
        composable(AppScreen.Settings.UserDevices.routeSignature) {
            UserDevicesScreen(appState)
        }
        composable(AppScreen.Settings.ProfileDetails.EditProfile.routeSignature) {
            EditProfileScreen(appState)
        }
        composable(AppScreen.Settings.ProfileDetails.ChangeEmail.routeSignature) {
            ChangeEmailScreen(
                appState = appState,
                onNavigateBack = {
                    // do nothing, email verification will trigger new graph
                }
            )
        }
        composable(AppScreen.Settings.ProfileDetails.ChangePassword.routeSignature) {
            ChangePasswordScreen(appState)
        }
        composable(AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature) {
            ConvertAccountScreen(appState)
        }
        composable(AppScreen.Settings.Licences.routeSignature) {
            LicencesScreen(appState)
        }
        composable(AppScreen.Settings.LicenceCode.routeSignature) {
            LicenceCodeScreen(appState)
        }
        composable(AppScreen.Settings.AppStatusDebug.routeSignature) {
            AppStatusDebugScreen(appState)
        }
        composable(AppScreen.Settings.AppStatus.routeSignature) {
            AppStatusScreen(appState)
        }
        composable(AppScreen.Settings.Subscriptions.routeSignature) {
            SubscriptionScreen(appState)
        }
        composable(AppScreen.Settings.LocationSettings.routeSignature) {
            LocationSettingsScreen(appState)
        }
        composable(AppScreen.Settings.GeneralInfo.routeSignature) {
            GeneralInfoScreen(appState)
        }
        composable(AppScreen.Settings.BillingPaywall.routeSignature) {
            val type = it.arguments?.getString(AppScreen.Settings.BillingPaywall.argumentSourceType)
                .let { BillingScreenSourceType.fromKey(it) }
            PaywallScreen(appState, groupId = null, type = type)
        }
        composable(AppScreen.Settings.ProfileDetails.RawDataScreen.routeSignature) {
            val type =
                it.arguments?.getString(AppScreen.Settings.ProfileDetails.RawDataScreen.argumentDataType)!!
            val id =
                it.arguments?.getString(AppScreen.Settings.ProfileDetails.RawDataScreen.argumentDataId)
            val rawDataType = RawPresentationDataType.fromKey(type, id ?: "")
            RawDataScreen(appState = appState, type = rawDataType!!)
        }
        composable(AppScreen.Settings.WelcomeOnboarding.routeSignature) {
            WelcomeOnboardingScreen(
                isDemo = true,
                onFinish = { appState.navController.popBackStack() }
            )
        }
        composable(AppScreen.Settings.AppSettings.routeSignature) {
            AppSettingsScreen(appState)
        }
        composable(AppScreen.Settings.AppTheme.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<AppThemeViewModel>()
            )
        }
        composable(AppScreen.Settings.MeasurementSystem.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<MeasurementSystemViewModel>()
            )
        }
        composable(AppScreen.Settings.NotificationGrouping.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<NotificationsGroupingViewModel>()
            )
        }
        composable(AppScreen.Settings.MapInteractions.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<MapInteractionsViewModel>()
            )
        }
        composable(AppScreen.Settings.PlaceSearch.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<PlaceSearchSettingsViewModel>()
            )
        }
        composable(AppScreen.Settings.PersonalSpace.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<PersonalSpaceFeatureSettingsViewModel>()
            )
        }
        composable(AppScreen.Settings.NotificationSocket.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<NotificationSocketSettingsViewModel>()
            )
        }
        composable(AppScreen.Settings.MapsProvider.routeSignature) {
            GenericSettingsScreen(
                appState = appState,
                vm = koinViewModel<MapsProviderViewModel>()
            )
        }
    }
}

fun NavGraphBuilder.loggedOutGraph(
    appState: MainAppState,
    start: AppScreen
) {
    navigation(
        startDestination = start.routeSignature,
        route = AppScreen.LoggedOut.routeSignature
    ) {
        composable(AppScreen.LoggedOut.Welcome.routeSignature) {
            LoggedOutScreen(appState)
        }
        composable(AppScreen.LoggedOut.WelcomeOnboarding.routeSignature) {
            WelcomeOnboardingScreen(
                isDemo = false,
                onFinish = {
                    appState.navController.navController.navigate(
                        AppScreen.LoggedOut.Login.route(LoginScreenUiType.SIGNUP)
                    ) {
                        this.popUpTo(route = AppScreen.LoggedOut.WelcomeOnboarding.routeSignature) {
                            this.inclusive = true
                        }
                    }
                }
            )
        }
        composable(AppScreen.LoggedOut.Login.routeSignature) {
            val startScreen = it.arguments?.getString(AppScreen.LoggedOut.Login.argumentStartScreenType)
                ?.let { screenName -> LoginScreenUiType.values().firstOrNull { it.name == screenName } }
                ?: LoginScreenState.defaultStartScreen
            LoginScreen(appState, startScreen)
        }
        composable(AppScreen.LoggedOut.ProfileSetup.routeSignature) {
            ProfileSetupScreen(appState)
        }
        composable(AppScreen.LoggedOut.AccountRecovery.routeSignature) {
            RecoverKeysScreen(appState)
        }
        composable(AppScreen.LoggedOut.PasswordConfirmation.routeSignature) {
            PasswordConfirmationScreen(appState)
        }
        composable(
            route = AppScreen.LoggedOut.EmailVerification.routeSignature,
            enterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, targetOffset = { it / 4 }) },
            popEnterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, initialOffset = { -it / 4 }) },
            popExitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            VerifyEmailScreen(appState)
        }
        composable(
            route = AppScreen.LoggedOut.ChangeEmail.routeSignature,
            enterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, targetOffset = { it / 4 }) },
            popEnterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, initialOffset = { -it / 4 }) },
            popExitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            ChangeEmailScreen(appState, onNavigateBack = { appState.navController.popBackStack() })
        }
        composable(AppScreen.LoggedOut.PasswordReset.routeSignature) {
            val userId = it.arguments?.getString(AppScreen.LoggedOut.PasswordReset.argumentUserId)
            val secret = it.arguments?.getString(AppScreen.LoggedOut.PasswordReset.argumentSecret)
            ResetPasswordScreen(appState, userId = userId!!, secret = secret!!)
        }
    }
}