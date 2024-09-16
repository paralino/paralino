package app.paralino.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import app.paralino.android.ui.groups.GroupCreateScreen
import app.paralino.android.ui.groups.GroupDetailScreen
import app.paralino.android.ui.groups.GroupSettingsScreen
import app.paralino.android.ui.invite.input.MemberInviteScreen
import app.paralino.android.ui.invite.manage.PersonalCodeScreen
import app.paralino.android.ui.login.LoggedOutScreen
import app.paralino.android.ui.login.LoginScreen
import app.paralino.android.ui.login.ProfileSetupScreen
import app.paralino.android.ui.login.RecoverKeysScreen
import app.paralino.android.ui.login.ResetPasswordScreen
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.MapHomeScreen
import app.paralino.android.ui.places.CreatePlaceScreen
import app.paralino.android.ui.places.PlaceDetailScreen
import app.paralino.android.ui.places.PlacesScreen
import app.paralino.android.ui.settings.AppStatusDebugScreen
import app.paralino.android.ui.settings.BillingScreen
import app.paralino.android.ui.settings.BlocklistScreen
import app.paralino.android.ui.settings.LicencesScreen
import app.paralino.android.ui.settings.RawDataScreen
import app.paralino.android.ui.settings.SettingsScreen
import app.paralino.android.ui.settings.SubscriptionScreen
import app.paralino.android.ui.settings.profile.AccountScreen
import app.paralino.android.ui.settings.profile.ChangeEmailScreen
import app.paralino.android.ui.settings.profile.ChangePasswordScreen
import app.paralino.android.ui.settings.profile.ConvertAccountScreen
import app.paralino.android.ui.settings.profile.EditProfileScreen
import app.paralino.android.ui.share.SharingStartScreen
import app.paralino.core.presentation.login.LoginScreenState
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.rawdata.RawPresentationDataType


@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    appState: MainAppState,
    onAppClose: () -> Unit
) {
    NavHost(
        modifier = modifier,
        navController = appState.navController,
        startDestination = AppScreen.MainHome.routeSignature,
        enterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, targetOffset = { it / 4 }) },
        popEnterTransition = { this.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, initialOffset = { -it / 4 }) },
        popExitTransition = { this.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End) },
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
        composable(AppScreen.MainHome.GroupCreate.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.GroupCreate.argumentGroupId)
            GroupCreateScreen(appState = appState, groupId = groupId)
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
            val onboarding =
                it.arguments?.getString(AppScreen.MainHome.SharingStart.argumentOnboarding)?.toBoolean() ?: false
            SharingStartScreen(appState = appState, groupId = groupId, isOnboarding = onboarding)
        }
        composable(AppScreen.MainHome.PlacesList.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.PlacesList.argumentGroupId)!!
            val onboarding =
                it.arguments?.getString(AppScreen.MainHome.PlacesList.argumentOnboarding)?.toBoolean() ?: false
            PlacesScreen(appState = appState, groupId = groupId, isOnboarding = onboarding)
        }
        composable(AppScreen.MainHome.PlaceDetail.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.MainHome.PlaceDetail.argumentGroupId)!!
            val placeId =
                it.arguments?.getString(AppScreen.MainHome.PlaceDetail.argumentPlaceId)!!
            PlaceDetailScreen(appState = appState, groupId = groupId, placeId = placeId)
        }
        composable(AppScreen.MainHome.PlaceCreate.routeSignature) {
            val groupId = it.arguments?.getString(AppScreen.MainHome.PlaceCreate.argumentGroupId)!!
            val placeId = it.arguments?.getString(AppScreen.MainHome.PlaceCreate.argumentPlaceId)
            val lat = it.arguments?.takeIf { it.containsKey(AppScreen.MainHome.PlaceCreate.argumentLat) }
                ?.getString(AppScreen.MainHome.PlaceCreate.argumentLat)?.toDoubleOrNull()
            val lng = it.arguments?.takeIf { it.containsKey(AppScreen.MainHome.PlaceCreate.argumentLng) }
                ?.getString(AppScreen.MainHome.PlaceCreate.argumentLng)?.toDoubleOrNull()
            CreatePlaceScreen(
                appState = appState,
                groupId = groupId,
                placeId = placeId,
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
            BillingScreen(appState, groupId = groupId)
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
        composable(AppScreen.Settings.ProfileDetails.EditProfile.routeSignature) {
            EditProfileScreen(appState)
        }
        composable(AppScreen.Settings.ProfileDetails.ChangeEmail.routeSignature) {
            ChangeEmailScreen(appState)
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
        composable(AppScreen.Settings.AppStatusDebug.routeSignature) {
            AppStatusDebugScreen(appState)
        }
        composable(AppScreen.Settings.Subscriptions.routeSignature) {
            SubscriptionScreen(appState)
        }
        composable(AppScreen.Settings.BillingPaywall.routeSignature) {
            BillingScreen(appState)
        }
        composable(AppScreen.Settings.ProfileDetails.RawDataScreen.routeSignature) {
            val type =
                it.arguments?.getString(AppScreen.Settings.ProfileDetails.RawDataScreen.argumentDataType)!!
            val id =
                it.arguments?.getString(AppScreen.Settings.ProfileDetails.RawDataScreen.argumentDataId)
            val rawDataType = RawPresentationDataType.fromKey(type, id ?: "")
            RawDataScreen(appState = appState, type = rawDataType!!)
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
        composable(
            AppScreen.LoggedOut.PasswordReset.routeSignature,
            deepLinks = listOf(
                navDeepLink {
                    this.uriPattern = Deeplink.PasswordReset.uriPattern()
                }
            )
        ) {
            val userId = it.arguments?.getString(AppScreen.LoggedOut.PasswordReset.argumentUserId)
            val secret = it.arguments?.getString(AppScreen.LoggedOut.PasswordReset.argumentSecret)
            ResetPasswordScreen(appState, userId = userId!!, secret = secret!!)
        }
    }
}