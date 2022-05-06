package com.zbapp.locationtrack.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.valueIterator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zbapp.core.config
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.settings.AccountScreen
import com.zbapp.locationtrack.ui.settings.LoggedOutSettingsSection
import com.zbapp.locationtrack.ui.settings.SettingsScreen
import com.zbapp.locationtrack.ui.theme.AppTheme
import com.zbapp.locationtrack.utils.extensions.safeCast

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RootApp() }
//        setContentView(R.layout.activity_main)
//        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment
        ) as NavHostFragment
        navController = navHostFragment.navController

        // Setup the bottom navigation view with navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnItemReselectedListener { item ->
            navController.graph.nodes.valueIterator().asSequence().firstOrNull {
                it.id == item.itemId
            }?.safeCast<NavGraph>()?.let {
                navController.popBackStack(it.findStartDestination().id, false)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    fun navigateDeeplink(deeplink: String) {
        navigateDeeplink(Uri.parse(deeplink))
    }

    fun navigateDeeplink(uri: Uri) {
        navController.navigate(
            uri,
            navOptions {
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        )
    }

}

@Composable
fun RootApp() {
    AppTheme {
        // TODO replace with proper state
        val userState = config.isTokenValid

        if (userState) {
            MainAppScreen()
        } else {
            LoggedOutScreen()
        }
    }
}

@Composable
fun LoggedOutScreen() {
    LoggedOutSettingsSection()
}

@Composable
fun MainAppScreen() {

    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        bottomBar = {
            BottomNavigation(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val unselectedColor = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.high)

                BottomNavigationItem(
                    selected = currentDestination?.hierarchy?.any { it.route == AppScreen.DashboardRoot.routeSignature } == true,
                    onClick = {
                        naviClick(
                            navController = navController,
                            route = AppScreen.DashboardRoot.routeSignature,
                            firstRoute = AppScreen.DashboardRoot.Dashboard.routeSignature
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Dashboard,
                            contentDescription = null
                        )
                    },
                    unselectedContentColor = unselectedColor
                )
                BottomNavigationItem(
                    selected = currentDestination?.hierarchy?.any { it.route == AppScreen.MapRoot.routeSignature } == true,
                    onClick = {
                        naviClick(
                            navController = navController,
                            route = AppScreen.MapRoot.routeSignature,
                            firstRoute = AppScreen.MapRoot.MainMap.routeSignature
                        )
                    },
                    icon = { Icon(imageVector = Icons.Rounded.Map, contentDescription = null) },
                    unselectedContentColor = unselectedColor
                )
                BottomNavigationItem(
                    selected = currentDestination?.hierarchy?.any { it.route == AppScreen.SettingsRoot.routeSignature } == true,
                    onClick = {
                        naviClick(
                            navController = navController,
                            route = AppScreen.SettingsRoot.routeSignature,
                            firstRoute = AppScreen.SettingsRoot.Settings.routeSignature
                        )
                    },
                    icon = { Icon(imageVector = Icons.Rounded.Settings, contentDescription = null) },
                    unselectedContentColor = unselectedColor
                )
            }
        }
    ) { innerPadding ->
        MainNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

fun naviClick(navController: NavController, route: String, firstRoute: String) {
    if (navController.currentBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true) {
        // if re-selecting the same tab - pop to first route
        navController.popBackStack(firstRoute, false)
    } else {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when re-selecting the same item
            launchSingleTop = true
            // Restore state when re-selecting previously selected item
            restoreState = true
        }
    }
}

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
            Text(
                "Dashboard",
                Modifier.clickable { navController.navigate(AppScreen.DashboardRoot.SharingStart.routeSignature) })
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
