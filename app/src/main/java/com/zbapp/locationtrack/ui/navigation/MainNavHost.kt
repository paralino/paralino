package com.zbapp.locationtrack.ui.navigation

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.dashboard.DashboardScreen
import com.zbapp.locationtrack.ui.groups.create.GroupCreateScreen
import com.zbapp.locationtrack.ui.login.LoggedOutScreen
import com.zbapp.locationtrack.ui.map.MapHomeScreen
import com.zbapp.locationtrack.ui.settings.AccountScreen
import com.zbapp.locationtrack.ui.settings.SettingsScreen


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
            Text("Group details - $groupId")
        }
        composable(AppScreen.DashboardRoot.GroupCreate.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.GroupCreate.argumentGroupId)
            GroupCreateScreen(appState = appState, groupId = groupId)
        }
        composable(AppScreen.DashboardRoot.SharingStart.routeSignature) {
            val groupId =
                it.arguments?.getString(AppScreen.DashboardRoot.GroupDetails.argumentGroupId)
            Text("Share")
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
        composable(AppScreen.SettingsRoot.InviteCodes.routeSignature) {
            Text("Invites")
        }
        composable(AppScreen.Login.routeSignature) {
            LoggedOutScreen()
        }
    }
}