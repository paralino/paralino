package app.paralino.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.dashboard.DashboardScreen
import app.paralino.android.ui.groups.create.GroupCreateScreen
import app.paralino.android.ui.groups.detail.GroupDetailScreen
import app.paralino.android.ui.invite.input.MemberInviteScreen
import app.paralino.android.ui.invite.manage.PersonalCodeScreen
import app.paralino.android.ui.login.LoggedOutScreen
import app.paralino.android.ui.login.LoginScreen
import app.paralino.android.ui.login.ProfileSetupScreen
import app.paralino.android.ui.login.passwordreset.ResetPasswordScreen
import app.paralino.android.ui.login.recover.RecoverKeysScreen
import app.paralino.android.ui.map.MapHomeScreen
import app.paralino.android.ui.places.create.CreatePlaceScreen
import app.paralino.android.ui.places.detail.PlaceDetailScreen
import app.paralino.android.ui.places.list.PlacesScreen
import app.paralino.android.ui.settings.AccountScreen
import app.paralino.android.ui.settings.LicencesScreen
import app.paralino.android.ui.settings.SettingsScreen
import app.paralino.android.ui.settings.convertaccount.ConvertAccountScreen
import app.paralino.android.ui.settings.editprofile.EditProfileScreen
import app.paralino.android.ui.settings.emailchange.ChangeEmailScreen
import app.paralino.android.ui.settings.passwordchange.ChangePasswordScreen
import app.paralino.android.ui.share.SharingStartScreen
import app.paralino.core.presentation.login.LoginScreenState
import app.paralino.core.presentation.login.LoginScreenUiType


@Composable
fun MainNavHost(
    appState: MainAppState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = appState.navController,
        startDestination = AppScreen.DashboardRoot.routeSignature,
        modifier = modifier
    ) {
        dashboardGraph(appState)
        mapGraph(appState)
        settingsGraph(appState)
    }
}

fun NavGraphBuilder.dashboardGraph(appState: MainAppState) {
    navigation(
        startDestination = AppScreen.DashboardRoot.Dashboard.routeSignature,
        route = AppScreen.DashboardRoot.routeSignature
    ) {
        composable(AppScreen.DashboardRoot.Dashboard.routeSignature) {
            DashboardScreen(appState)
        }
        composable(AppScreen.DashboardRoot.GroupDetails.routeSignature) {
            val groupId =
                it.arguments!!.getString(AppScreen.DashboardRoot.GroupDetails.argumentGroupId)
            GroupDetailScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.DashboardRoot.GroupCreate.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.GroupCreate.argumentGroupId)
            GroupCreateScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.DashboardRoot.GroupMemberAdd.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.GroupMemberAdd.argumentGroupId)
            val code =
                it.arguments?.getString(AppScreen.DashboardRoot.GroupMemberAdd.argumentCode)
            MemberInviteScreen(appState = appState, groupId = groupId, code = code)
        }
        composable(AppScreen.DashboardRoot.SharingStart.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.SharingStart.argumentGroupId)
            SharingStartScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.DashboardRoot.PlacesList.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.PlacesList.argumentGroupId)!!
            PlacesScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.DashboardRoot.PlaceDetail.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.PlaceDetail.argumentGroupId)!!
            val placeId =
                it.arguments?.getString(AppScreen.DashboardRoot.PlaceDetail.argumentPlaceId)!!
            PlaceDetailScreen(appState = appState, groupId = groupId, placeId = placeId)
        }
        composable(AppScreen.DashboardRoot.PlaceCreate.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.PlaceCreate.argumentGroupId)!!
            val placeId =
                it.arguments?.getString(AppScreen.DashboardRoot.PlaceCreate.argumentPlaceId)
            CreatePlaceScreen(appState = appState, groupId = groupId, placeId = placeId)
        }
    }
}

fun NavGraphBuilder.mapGraph(appState: MainAppState) {
    navigation(
        startDestination = AppScreen.MapRoot.MainMap.routeSignature,
        route = AppScreen.MapRoot.routeSignature
    ) {
        composable(AppScreen.MapRoot.MainMap.routeSignature) {
            val groupId =
                it.arguments!!.getString(AppScreen.MapRoot.MainMap.argumentGroupId)
            MapHomeScreen(appState = appState, groupId = groupId)
        }
    }
}

fun NavGraphBuilder.settingsGraph(appState: MainAppState) {
    navigation(
        startDestination = AppScreen.SettingsRoot.Settings.routeSignature,
        route = AppScreen.SettingsRoot.routeSignature
    ) {
        composable(AppScreen.SettingsRoot.Settings.routeSignature) {
            SettingsScreen(appState)
        }
        composable(AppScreen.SettingsRoot.ProfileDetails.routeSignature) {
            AccountScreen(appState)
        }
        composable(AppScreen.SettingsRoot.PersonalCodes.routeSignature) {
            val createCode = it.arguments?.getString(AppScreen.SettingsRoot.PersonalCodes.argumentCreateCode)?.toBoolean() ?: false
            PersonalCodeScreen(appState, createCode = createCode)
        }
        composable(AppScreen.SettingsRoot.ProfileDetails.EditProfile.routeSignature) {
            EditProfileScreen(appState)
        }
        composable(AppScreen.SettingsRoot.ProfileDetails.ChangeEmail.routeSignature) {
            ChangeEmailScreen(appState)
        }
        composable(AppScreen.SettingsRoot.ProfileDetails.ChangePassword.routeSignature) {
            ChangePasswordScreen(appState)
        }
        composable(AppScreen.SettingsRoot.ProfileDetails.ConvertAccount.routeSignature) {
            ConvertAccountScreen(appState)
        }
        composable(AppScreen.SettingsRoot.Licences.routeSignature) {
            LicencesScreen(appState)
        }
    }
}

fun NavGraphBuilder.loggedOutGraph(
    appState: MainAppState,
    start: AppScreen
) {
    navigation(
        startDestination = start.routeSignature,
        route = AppScreen.AppStartRoot.routeSignature
    ) {
        composable(AppScreen.AppStartRoot.Welcome.routeSignature) {
            LoggedOutScreen(appState)
        }
        composable(AppScreen.AppStartRoot.Login.routeSignature) {
            val startScreen = it.arguments?.getString(AppScreen.AppStartRoot.Login.argumentStartScreenType)
                ?.let { screenName -> LoginScreenUiType.values().firstOrNull { it.name == screenName } }
                ?: LoginScreenState.defaultStartScreen
            LoginScreen(appState, startScreen)
        }
        composable(AppScreen.AppStartRoot.ProfileSetup.routeSignature) {
            ProfileSetupScreen(appState)
        }
        composable(AppScreen.AppStartRoot.AccountRecovery.routeSignature) {
            RecoverKeysScreen(appState)
        }
        composable(
            AppScreen.AppStartRoot.PasswordReset.routeSignature,
            deepLinks = listOf(
                navDeepLink {
                    this.uriPattern = Deeplink.PasswordReset.uriPattern()
                }
            )
        ) {
            val userId = it.arguments?.getString(AppScreen.AppStartRoot.PasswordReset.argumentUserId)
            val secret = it.arguments?.getString(AppScreen.AppStartRoot.PasswordReset.argumentSecret)
            ResetPasswordScreen(appState, userId = userId!!, secret = secret!!)
        }
    }
}