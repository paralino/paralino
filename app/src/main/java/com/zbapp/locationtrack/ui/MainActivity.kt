package com.zbapp.locationtrack.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.valueIterator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zbapp.core.config
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.components.AppSnackbar
import com.zbapp.locationtrack.ui.components.SheetContent
import com.zbapp.locationtrack.ui.login.LoggedOutScreen
import com.zbapp.locationtrack.ui.navigation.AppScreen
import com.zbapp.locationtrack.ui.navigation.MainNavHost
import com.zbapp.locationtrack.ui.theme.AppTheme
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainAppScreen() {

    val appState = rememberAppState()

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetBackgroundColor = MaterialTheme.colors.background,
        sheetShape = RoundedCornerShape(topStartPercent = 5, topEndPercent = 5),
        sheetState = appState.bottomSheetState.bottomSheetState,
        scrimColor = MaterialTheme.colors.surface.copy(alpha = 0.6f),
        sheetContent = {
            Spacer(modifier = Modifier.height(1.dp))
            appState.bottomSheetState.currentBottomSheet.value?.let {
                SheetContent(
                    screen = appState.bottomSheetState.currentBottomSheet.value,
                    onClose = {
                        appState.uiScope.launch { appState.bottomSheetState.bottomSheetState.hide() }
                    },
                    onResult = {
                        appState.uiScope.launch {
                            appState.bottomSheetState.sheetResultChannel.send(it)
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = appState.scaffoldState,
            bottomBar = {
                BottomNavigation(
                    backgroundColor = MaterialTheme.colors.background,
                    elevation = 4.dp
                ) {
                    BottomBarButtons(appState = appState)
                }
            },
            snackbarHost = { AppSnackbar(it) }
        ) { innerPadding ->
            MainNavHost(
                appState = appState,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun RowScope.BottomBarButtons(
    appState: MainAppState
) {
    val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val unselectedColor = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.high)

    BottomNavigationItem(
        selected = currentDestination?.hierarchy?.any { it.route == AppScreen.DashboardRoot.routeSignature } == true,
        onClick = {
            naviClick(
                navController = appState.navController,
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
                navController = appState.navController,
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
                navController = appState.navController,
                route = AppScreen.SettingsRoot.routeSignature,
                firstRoute = AppScreen.SettingsRoot.Settings.routeSignature
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null
            )
        },
        unselectedContentColor = unselectedColor
    )
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