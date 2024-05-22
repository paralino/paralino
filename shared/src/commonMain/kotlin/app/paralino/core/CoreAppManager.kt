package app.paralino.core

import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.location.ActiveSharingEngine
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CoreAppManager(
    private val scopeProvider: ICoroutineScopeProvider,
    private val configRepo: AppConfigRepository,
    private val refreshManager: RefreshEverythingUseCase,
    private val sessionMonitor: UserSessionMonitor,
    private val logoutUseCase: LogoutUseCase,
    private val connectionStatusProvider: IConnectionStatusProvider,
    private val locationStatus: ILocationStatusProvider,
    private val activeSharingEngine: ActiveSharingEngine,
    private val deviceApiProvider: IDeviceApiProvider,
    private val userRepo: UserRepository,
    private val config: AppConfig
) {

    fun onAppStart() {
        sessionMonitor.listenForSessionExpired()
            .onEach {
                logg.tag("account_session").v("Got session expired event - logout")
                logoutUseCase.logoutLocally()
            }
            .launchIn(scopeProvider.defaultScope)

        deviceApiProvider.appLifecycleState.onEach { state ->
            if (state.current == AppLifecycleType.ACTIVE) {
                updateLocationStatus()
                updateConnectionStatus()
                updateConfig()
            }

            if (state.comingFromBackground) {
                updateGroups()
            }
        }
            .launchIn(scopeProvider.defaultIOScope)

        configRepo.getUpdateType()
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
        if (!config.isTokenValid) {
            logg.tag("app_state").v("Skip refresh, token is invalid")
            return
        }

        logg.tag("app_state").v("App is active but was in background, refresh user and groups")
        scopeProvider.userSessionIOScope.launch {
            userRepo.refreshUserMaybe(timeDelayMillis = 60_000)
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