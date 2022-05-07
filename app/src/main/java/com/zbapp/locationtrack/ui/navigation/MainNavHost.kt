package com.zbapp.locationtrack.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.zbapp.locationtrack.ui.dashboard.DashboardScreen
import com.zbapp.locationtrack.ui.login.LoggedOutScreen
import com.zbapp.locationtrack.ui.settings.AccountScreen
import com.zbapp.locationtrack.ui.settings.SettingsScreen


@Composable
fun MainNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.DashboardRoot.routeSignature,
        modifier = modifier
    ) {
        dashboardGraph(navController)
        mapGraph(navController)
        settingsGraph(navController)
    }
}

fun NavGraphBuilder.dashboardGraph(navController: NavController) {
    navigation(
        startDestination = AppScreen.DashboardRoot.Dashboard.routeSignature,
        route = AppScreen.DashboardRoot.routeSignature
    ) {
        composable(AppScreen.DashboardRoot.Dashboard.routeSignature) {
            DashboardScreen(navController)
        }
        composable(AppScreen.DashboardRoot.GroupDetails.routeSignature) {
            val groupId = it.arguments!!.getString(AppScreen.DashboardRoot.GroupDetails.argumentGroupId)
            Text("Group details - $groupId")
        }
        composable(AppScreen.DashboardRoot.SharingStart.routeSignature) {
            val groupId = it.arguments?.getString(AppScreen.DashboardRoot.GroupDetails.argumentGroupId)
            Text("Share")
        }
    }
}

fun NavGraphBuilder.mapGraph(navController: NavController) {
    navigation(
        startDestination = AppScreen.MapRoot.MainMap.routeSignature,
        route = AppScreen.MapRoot.routeSignature
    ) {
        composable(AppScreen.MapRoot.MainMap.routeSignature) {
            Text("Map")
        }
    }
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    navigation(
        startDestination = AppScreen.SettingsRoot.Settings.routeSignature,
        route = AppScreen.SettingsRoot.routeSignature
    ) {
        composable(AppScreen.SettingsRoot.Settings.routeSignature) {
            SettingsScreen(navController)
        }
        composable(AppScreen.SettingsRoot.ProfileDetails.routeSignature) {
            AccountScreen(navController)
        }
        composable(AppScreen.SettingsRoot.InviteCodes.routeSignature) {
            Text("Invites")
        }
        composable(AppScreen.Login.routeSignature) {
            LoggedOutScreen()
        }
    }
}