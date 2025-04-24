package app.paralino.android.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import app.paralino.android.app_rating.AppRatingPresenter
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.navigation.MainNavHost
import app.paralino.android.ui.navigation.loggedOutGraph
import app.paralino.android.ui.shared.errorString
import app.paralino.core.MR
import app.paralino.core.app_update.AppUpdateStatus
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ConfigData
import app.paralino.core.presentation.main.MainScreenEvent
import app.paralino.core.presentation.main.MainViewModel
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.ui.components.AppSnackbar
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val vm: MainViewModel by viewModel()
    private val appRatingPresenter: AppRatingPresenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RootApp(
                vm = vm,
                onAppClose = { finishAffinity() },
                onNewVersionDownload = { vm.startAppUpdate() }
            )
        }
        handleExtraDeeplink(intent?.data)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appRatingPresenter.ratingEvent.collectLatest {
                    if (it != null) {
                        appRatingPresenter.triggerAppRatingDialog(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExtraDeeplink(intent.data)
    }

    private fun handleExtraDeeplink(deeplinkUri: Uri?) {
        vm.setDeeplink(DeeplinkType.parse(deeplinkUri?.toString()))
    }
}

@Composable
fun RootApp(
    vm: MainViewModel,
    onAppClose: () -> Unit,
    onNewVersionDownload: (ConfigData) -> Unit
) {
    AppTheme {
        val appState = rememberAppState()

        MessageDispatcherEffect(vm, appState)

        val uiState by vm.uiState.collectAsState()

        when (val state = uiState.userState) {
            Resource.NotInitialized -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colors.secondary)
                }
            }

            is Resource.Ready -> {
                when (val finalState = state.data) {
                    is AppAccountState.ACCOUNT_RESET -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.LoggedOut.AccountRecovery,
                            pendingDeeplink = null,
                            onDeeplinkComplete = { vm.setDeeplink(null) }
                        )
                    }

                    is AppAccountState.NO_PROFILE -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.LoggedOut.ProfileSetup,
                            pendingDeeplink = null,
                            onDeeplinkComplete = { vm.setDeeplink(null) }
                        )
                    }

                    is AppAccountState.EMAIL_VERIFICATION -> {
                        AppStartScreen(
                            appState = appState,
                            start = AppScreen.LoggedOut.EmailVerification,
                            pendingDeeplink = null,
                            onDeeplinkComplete = { vm.setDeeplink(null) }
                        )
                    }

                    is AppAccountState.NO_USER -> {
                        AppStartScreen(
                            appState = appState,
                            start = if (finalState.canRecoverGuest) {
                                AppScreen.LoggedOut.Login
                            } else {
                                AppScreen.LoggedOut.Welcome
                            },
                            pendingDeeplink = uiState.pendingDeeplinkType.takeIf { !finalState.canRecoverGuest },
                            onDeeplinkComplete = { vm.setDeeplink(null) }
                        )
                    }

                    is AppAccountState.NO_USER_DATA, AppAccountState.DATA_REFRESH -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colors.primary)
                        }
                    }

                    is AppAccountState.USER_READY -> {
                        MainAppScreen(
                            appState = appState,
                            pendingDeeplink = uiState.pendingDeeplinkType,
                            onAppClose = onAppClose,
                            onDeeplinkComplete = { vm.setDeeplink(null) }
                        )
                    }
                }

                if (uiState.isSessionExpiredShown) {
                    MainAlertDialog.SessionExpired.Show {
                        vm.cancelSessionExpired()
                    }
                }

                when (uiState.updateType) {
                    null, is AppUpdateType.NONE -> null
                    is AppUpdateType.SOFT -> MainAlertDialog.AppVersionInfo(forceUpdate = false)
                    is AppUpdateType.FORCE,
                    is AppUpdateType.FORCE_AND_CANCEL -> MainAlertDialog.AppVersionInfo(forceUpdate = true)
                }?.Show {
                    if (it == MainAlertDialog.Buttons.buttonCancel) {
                        vm.cancelVersion()
                    } else if (it == MainAlertDialog.Buttons.buttonOk) {
                        uiState.updateType?.configData?.let {
                            onNewVersionDownload(it)
                        }
                    }
                }
            }
        }

        when (val finalState = uiState.appUpdateStatus) {
            AppUpdateStatus.Idle -> {}
            is AppUpdateStatus.Downloading -> {
                LoadingDialog(text = "Downloading...", progress = finalState.progress)
            }

            AppUpdateStatus.Installing -> {
                LoadingDialog(text = "Installing...")
            }
        }

        if (uiState.dataRefreshBlockingLoadingShown) {
            LoadingDialog(
                text = stringResource(MR.strings.login_step_generic_loading),
                progress = null
            )
        }
    }
}

@Composable
fun AppStartScreen(
    appState: MainAppState,
    start: AppScreen,
    pendingDeeplink: DeeplinkType?,
    onDeeplinkComplete: () -> Unit,
) {
    LaunchedEffect(pendingDeeplink) {
        delay(500)
        when (pendingDeeplink) {
            is DeeplinkType.PasswordReset -> {
                appState.navController.navigate(
                    AppScreen.LoggedOut.PasswordReset.route(deeplink = pendingDeeplink)
                )
                onDeeplinkComplete()
            }

            else -> {}
        }
    }
    RootAppScreen(appState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = appState.scaffoldState,
            snackbarHost = {
                AppSnackbar(
                    drawingPaddingValues = WindowInsets.navigationBars.asPaddingValues(),
                    snackbarHostState = it
                )
            }
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = appState.navController,
                startDestination = AppScreen.LoggedOut.routeSignature
            ) {
                loggedOutGraph(appState, start = start)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    appState: MainAppState,
    pendingDeeplink: DeeplinkType?,
    onDeeplinkComplete: () -> Unit,
    onAppClose: () -> Unit
) {
    LaunchedEffect(pendingDeeplink) {
        delay(500)
        when (pendingDeeplink) {
            is DeeplinkType.ParalinoID -> {
                appState.navController.navigate(
                    AppScreen.MainHome.GroupMemberAdd.route(
                        groupId = null,
                        code = pendingDeeplink.paralinoID,
                        isOnboarding = false
                    )
                )
                onDeeplinkComplete()
            }

            else -> {}
        }
    }
    RootAppScreen(appState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = appState.scaffoldState,
            snackbarHost = {
                AppSnackbar(
                    drawingPaddingValues = WindowInsets.navigationBars.asPaddingValues(),
                    snackbarHostState = it
                )
            }
        ) { innerPadding ->
            MainNavHost(
                modifier = Modifier.padding(innerPadding),
                appState = appState,
                onAppClose = onAppClose
            )
        }
    }
}

@Composable
fun RootAppScreen(
    appState: MainAppState,
    content: @Composable () -> Unit,
) {
    content()
}