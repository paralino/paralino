package com.zbapp.locationtrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.dashboard.DashboardScreen
import com.zbapp.locationtrack.ui.groups.create.GroupCreateScreen
import com.zbapp.locationtrack.ui.groups.detail.GroupDetailScreen
import com.zbapp.locationtrack.ui.invite.input.MemberInviteScreen
import com.zbapp.locationtrack.ui.invite.manage.PersonalCodeScreen
import com.zbapp.locationtrack.ui.login.LoggedOutScreen
import com.zbapp.locationtrack.ui.login.LoginScreen
import com.zbapp.locationtrack.ui.login.ProfileSetupScreen
import com.zbapp.locationtrack.ui.login.recover.RecoverKeysScreen
import com.zbapp.locationtrack.ui.map.MapHomeScreen
import com.zbapp.locationtrack.ui.settings.AccountScreen
import com.zbapp.locationtrack.ui.settings.SettingsScreen
import com.zbapp.locationtrack.ui.settings.editprofile.EditProfileScreen
import com.zbapp.locationtrack.ui.settings.emailchange.ChangeEmailScreen
import com.zbapp.locationtrack.ui.settings.passwordchange.ChangePasswordScreen
import com.zbapp.locationtrack.ui.share.SharingStartScreen


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
            PersonalCodeScreen(appState)
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
            LoginScreen(appState)
        }
        composable(AppScreen.AppStartRoot.ProfileSetup.routeSignature) {
            ProfileSetupScreen(appState)
        }
        composable(
            AppScreen.AppStartRoot.AccountRecovery.routeSignature,
        ) {
            RecoverKeysScreen(appState)
        }
    }
}