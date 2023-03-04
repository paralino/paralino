package app.paralino.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
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
import app.paralino.android.utils.extensions.openUri
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.models.Resource
import app.paralino.core.presentation.MainScreenEvent
import app.paralino.core.presentation.MainViewModel
import app.paralino.core.repo.base.AppUpdateType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    val vm: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootApp(
                vm = vm,
                onAppClose = { finishAffinity() }
            )
        }
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
    onAppClose: () -> Unit
) {
    AppTheme {
        val appState = rememberAppState()
        val userState by vm.userState.collectAsState(initial = Resource.NotInitialized)
        var showDialog: MainAlertDialog? by remember { mutableStateOf(null) }
        var showAppUpdateDialog: MainAlertDialog.AppVersionInfo? by remember { mutableStateOf(null) }

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
                    is MainScreenEvent.NewAppVersion -> {
                        showAppUpdateDialog = when(event.updateType) {
                            AppUpdateType.NONE -> null
                            AppUpdateType.SOFT -> MainAlertDialog.AppVersionInfo(forceUpdate = false)
                            AppUpdateType.FORCE -> MainAlertDialog.AppVersionInfo(forceUpdate = true)
                        }
                    }
                    MainScreenEvent.SessionExpired -> {
                        showDialog = MainAlertDialog.SessionExpired
                    }
                    null -> {}
                }
            }
        }

        when (val state = userState) {
            Resource.NotInitialized -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colors.secondary)
                }
            }
            is Resource.Ready -> {

                if(showAppUpdateDialog != null) {
                    showAppUpdateDialog?.Show {
                        if(it == MainAlertDialog.Buttons.buttonCancel) {
                            vm.cancelVersion()
                            showAppUpdateDialog = null
                        } else if(it == MainAlertDialog.Buttons.buttonOk) {
                            // TODO update
                            context.openUri(Deeplink.homeUrl.toUri())
                        }
                    }
                }

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
                            start = AppScreen.LoggedOut.AccountRecovery
                        )
                    }
                    is AppAccountState.NO_PROFILE -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.LoggedOut.ProfileSetup
                        )
                    }
                    is AppAccountState.NO_USER -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.LoggedOut.Welcome
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
                        MainAppScreen(appState = appState, onAppClose = onAppClose)
                    }
                }
            }
        }
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
                startDestination = AppScreen.LoggedOut.routeSignature,
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
    onAppClose: () -> Unit
) {
    RootAppScreen(appState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = appState.scaffoldState,
            snackbarHost = { AppSnackbar(it) }
        ) { innerPadding ->
            MainNavHost(
                modifier = Modifier.padding(innerPadding),
                appState = appState,
                onAppClose = onAppClose
            )
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