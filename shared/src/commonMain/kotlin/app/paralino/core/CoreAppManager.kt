package app.paralino.core

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.ActiveSharingEngine
import app.paralino.core.domain.user.ExpiredSessionHandler
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CoreAppManager(
    private val scopeProvider: ICoroutineScopeProvider,
    private val configRepo: RemoteConfigRepository,
    private val refreshManager: RefreshEverythingUseCase,
    private val sessionMonitor: UserSessionMonitor,
    private val logoutUseCase: LogoutUseCase,
    private val connectionStatusProvider: IConnectionStatusProvider,
    private val locationStatus: ILocationStatusProvider,
    private val activeSharingEngine: ActiveSharingEngine,
    private val deviceApiProvider: IDeviceApiProvider,
    private val userRepo: UserRepository,
    private val expiredSessionHandler: ExpiredSessionHandler,
    private val appBuildConfig: AppBuildConfig,
    private val appPrefs: AppPrefs
) {

    private var sessionMonitorJob: Job? = null
    private var appLifecycleJob: Job? = null
    private var appUpdateTypeJob: Job? = null

    fun onAppStart() {

        sessionMonitorJob?.cancel()
        appLifecycleJob?.cancel()
        appUpdateTypeJob?.cancel()

        when (appBuildConfig.appHost) {
            AppHost.MAIN_APP -> {
                sessionMonitorJob = sessionMonitor.listenForSessionExpired()
                    .onEach {
                        logg.tag("account_session").v("Got session expired event")
                        expiredSessionHandler.processExpiredSession()
                    }
                    .catch {
                        logg.tag("account_session").i("Error processing expired session: ${it.message}")
                    }
                    .launchIn(scopeProvider.defaultScope)

                appLifecycleJob = deviceApiProvider.appLifecycleState.onEach { state ->
                    if (state.current == AppLifecycleType.ACTIVE) {
                        updateLocationStatus()
                        updateConnectionStatus()

                        if (state.comingFromBackground) {
                            updateConfig()
                        }
                    }

                    if (state.comingFromBackground) {
                        updateGroups()
                    }
                }
                    .launchIn(scopeProvider.defaultIOScope)

                appUpdateTypeJob = configRepo.getUpdateType()
                    .onEach { updateType ->
                        if (updateType is AppUpdateType.FORCE_AND_CANCEL) {
                            try {
                                logoutUseCase.fullLogout(logoutLocallyOnError = true)
                            } catch (e: Exception) {
                                logg.w("account_state", "Error on forced app update logout", e)
                            }
                        }
                    }
                    .launchIn(scopeProvider.defaultIOScope)

                activeSharingEngine.startMonitoring()
            }
            AppHost.IOS_NOTIFICATION_EXTENSION -> {
                // do nothing
            }
            AppHost.IOS_LOCATION_EXTENSION -> {
                activeSharingEngine.startMonitoring()
            }
        }
    }

    fun onAppActive() {
        deviceApiProvider.onAppActive()
    }

    fun onAppInactive() {
        deviceApiProvider.onAppInactive()
    }

    fun onAppBackground() {
        deviceApiProvider.onAppBackground()
    }

    fun updateLocationStatus() {
        locationStatus.refreshLocationStatus()
    }

    fun updateConnectionStatus() {
        scopeProvider.defaultIOScope.launch {
            connectionStatusProvider.initAndGetStatus()
        }
    }

    private fun updateGroups() {
        if (!appPrefs.isTokenValid) {
            logg.tag("app_state").v("Skip refresh, token is invalid")
            return
        }

        logg.tag("app_state").v("App is active but was in background, refresh user and groups")
        scopeProvider.userSessionIOScope.launch {
            userRepo.refreshUserMaybe(timeDelayMillis = 30_000)
            refreshManager.scheduleRefreshAllGroupsWithPlaces(time = 60_000)
        }
    }

    private fun updateConfig() {
        scopeProvider.defaultIOScope.launch {
            try {
                configRepo.refreshConfig()
            } catch (e: Exception) {
                logg.e("Error updating config", e)
            }
        }
    }
}