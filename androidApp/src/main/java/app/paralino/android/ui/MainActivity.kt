package app.paralino.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import app.paralino.android.R
import app.paralino.android.ui.components.AppSnackbar
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.components.SheetContent
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.navigation.Deeplink
import app.paralino.android.ui.navigation.MainNavHost
import app.paralino.android.ui.navigation.loggedOutGraph
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppTheme
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.models.Resource
import app.paralino.core.presentation.MainScreenEvent
import app.paralino.core.presentation.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    val vm: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RootApp(vm) }
        handleExtraDeeplink(intent?.data)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleExtraDeeplink(intent?.data)
    }

    private fun handleExtraDeeplink(deeplinkUri: Uri?) {
        handleExtraDeeplink(Deeplink.parse(deeplinkUri?.toString()))
    }

    private fun handleExtraDeeplink(deeplink: Deeplink?) {
        when (deeplink) {
            is Deeplink.EmailVerify -> {
                vm.verifyEmail(deeplink.userId, deeplink.secret)
            }
            else -> {}
        }
    }

}

@Composable
fun RootApp(
    vm: MainViewModel = getViewModel(),
) {
    AppTheme {
        val appState = rememberAppState()
        val userState by vm.userState.collectAsState(initial = Resource.NotInitialized)
        var showDialog: MainAlertDialog? by remember {
            mutableStateOf(null)
        }

        val context = LocalContext.current
        LaunchedEffect(context) {
            vm.events.collectLatest { event ->
                when (event) {
                    MainScreenEvent.EmailVerified -> {
                        appState.showSnackbar(context.getString(R.string.info_email_verified))
                    }
                    is MainScreenEvent.Error -> {
                        appState.showSnackbar(event.error.errorString(context))
                    }
                    null -> {}
                }
            }
        }

        LaunchedEffect(LocalContext.current) {
            vm.sessionExpiredFlow.collectLatest { showDialog = MainAlertDialog.SessionExpired }
        }

        when (val state = userState) {
            Resource.NotInitialized -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colors.secondary)
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

                when (val finalState = state.data) {
                    is AppAccountState.ACCOUNT_RESET -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.AppStartRoot.AccountRecovery
                        )
                    }
                    is AppAccountState.NO_PROFILE -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.AppStartRoot.ProfileSetup
                        )
                    }
                    is AppAccountState.NO_USER -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.AppStartRoot.Welcome
                        )
                    }
                    is AppAccountState.NO_USER_DATA -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colors.primary)
                        }
                    }
                    is AppAccountState.USER_READY -> {
                        val startScreen = if (finalState.groupCount > 0) {
                            AppScreen.MapRoot
                        } else {
                            AppScreen.DashboardRoot
                        }
                        MainAppScreen(appState, startScreen)
                    }
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
fun AppStartScreen(
    appState: MainAppState,
    start: AppScreen,
) {
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
                loggedOutGraph(appState, start = start)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    appState: MainAppState,
    startDestination: AppScreen,
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
                modifier = Modifier.padding(innerPadding),
                appState = appState,
                startDestination = startDestination
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
    val selectedColor = LocalContentColor.current
    val homeSelected =
        currentDestination?.hierarchy?.any { it.route == AppScreen.DashboardRoot.routeSignature } == true
    val mapSelected =
        currentDestination?.hierarchy?.any { it.route == AppScreen.MapRoot.routeSignature } == true
    val settingsSelected =
        currentDestination?.hierarchy?.any { it.route == AppScreen.SettingsRoot.routeSignature } == true

    BottomNavigationItem(
        selected = homeSelected,
        onClick = {
            naviClick(
                navController = appState.navController,
                route = AppScreen.DashboardRoot.routeSignature,
                firstRoute = AppScreen.DashboardRoot.Dashboard.routeSignature
            )
        },
        label = {
            Text(
                text = stringResource(id = R.string.title_dashboard),
                fontWeight = if (homeSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        alwaysShowLabel = true,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Workspaces,
                contentDescription = null
            )
        },
        selectedContentColor = selectedColor,
        unselectedContentColor = unselectedColor
    )
    BottomNavigationItem(
        selected = mapSelected,
        onClick = {
            naviClick(
                navController = appState.navController,
                route = AppScreen.MapRoot.routeSignature,
                firstRoute = AppScreen.MapRoot.MainMap.routeSignature
            )
        },
        label = {
            Text(
                text = stringResource(id = R.string.title_map),
                fontWeight = if (mapSelected) FontWeight.ExtraBold else FontWeight.Normal
            )
        },
        alwaysShowLabel = true,
        icon = { Icon(imageVector = Icons.Rounded.PersonPinCircle, contentDescription = null) },
        selectedContentColor = selectedColor,
        unselectedContentColor = unselectedColor
    )
    BottomNavigationItem(
        selected = settingsSelected,
        onClick = {
            naviClick(
                navController = appState.navController,
                route = AppScreen.SettingsRoot.routeSignature,
                firstRoute = AppScreen.SettingsRoot.Settings.routeSignature
            )
        },
        label = {
            Text(
                text = stringResource(id = R.string.title_settings),
                fontWeight = if (settingsSelected) FontWeight.Bold else FontWeight.Light
            )
        },
        alwaysShowLabel = true,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null
            )
        },
        selectedContentColor = selectedColor,
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