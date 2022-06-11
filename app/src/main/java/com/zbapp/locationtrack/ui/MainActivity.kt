package com.zbapp.locationtrack.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zbapp.core.models.Resource
import com.zbapp.locationtrack.ui.components.AppSnackbar
import com.zbapp.locationtrack.ui.components.MainAlertDialog
import com.zbapp.locationtrack.ui.components.SheetContent
import com.zbapp.locationtrack.ui.navigation.AppScreen
import com.zbapp.locationtrack.ui.navigation.MainNavHost
import com.zbapp.locationtrack.ui.navigation.loggedOutGraph
import com.zbapp.locationtrack.ui.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RootApp() }
    }

}

@Composable
fun RootApp(
    vm: MainViewModel = getViewModel(),
) {
    AppTheme {
        val userState by vm.userState.collectAsState(initial = Resource.NotInitialized)
        var showDialog: MainAlertDialog? by remember {
            mutableStateOf(null)
        }

        LaunchedEffect(LocalContext.current) {
            vm.sessionExpiredFlow.collectLatest { showDialog = MainAlertDialog.SessionExpired }
        }

        val appState = rememberAppState()

        when (val state = userState) {
            Resource.NotInitialized -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Ready -> {

                when (val dialog = showDialog) {
                    is MainAlertDialog.SessionExpired -> {
                        dialog.Show {
                            showDialog = null
                        }
                    }
                    else -> {}
                }

                if (state.data) {
                    MainAppScreen(appState)
                } else {
                    AppStartScreen(appState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RootAppScreen(
    appState: MainAppState,
    content: @Composable () -> Unit,
) {
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
        content()
    }
}

@Composable
fun AppStartScreen(appState: MainAppState) {
    RootAppScreen(appState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = appState.scaffoldState,
            snackbarHost = { AppSnackbar(it) }
        ) { innerPadding ->
            NavHost(
                navController = appState.navController,
                startDestination = AppScreen.AppStartRoot.routeSignature,
                modifier = Modifier.padding(innerPadding)
            ) {
                loggedOutGraph(appState)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    appState: MainAppState,
) {
    RootAppScreen(appState) {
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
    appState: MainAppState,
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