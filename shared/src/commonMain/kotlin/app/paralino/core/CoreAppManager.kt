package app.paralino.core

import app.paralino.core.domain.location.ActiveSharingEngine
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.AppUpdateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoreAppManager(
    private val scopeProvider: ICoroutineScopeProvider,
    private val configRepo: AppConfigRepository,
    private val refreshManager: RefreshEverythingUseCase,
    private val sessionMonitor: UserSessionMonitor,
    private val logoutUseCase: LogoutUseCase,
    private val connectionStatusProvider: IConnectionStatusProvider,
    private val locationStatus: ILocationStatusProvider,
    private val activeSharingEngine: ActiveSharingEngine
) {

    private var _appLifecycleState = MutableStateFlow(AppLifecycleState())
    val appLifecycleState = _appLifecycleState.asStateFlow()

    fun onAppStart() {
        sessionMonitor.listenForSessionExpired()
            .onEach {
                logg.tag("account_session").v("Got session expired event - logout")
                logoutUseCase.logoutLocally()
            }
            .launchIn(scopeProvider.defaultScope)

        appLifecycleState.onEach {
            if (appLifecycleState.value.current == AppLifecycleType.ACTIVE && appLifecycleState.value.previous == AppLifecycleType.BACKGROUND) {
                logg.tag("app_state").v("App is active but was in background, refresh groups")
                updateGroups()
            }
        }
            .launchIn(scopeProvider.defaultScope)

        configRepo.getUpdateType()
            .onEach { updateType ->
                if (updateType is AppUpdateType.FORCE_AND_CANCEL) {
                    logoutUseCase.logoutLocally()
                }
            }
            .launchIn(scopeProvider.defaultIOScope)

        activeSharingEngine.startMonitoring()
    }

    fun onAppActive() {
        logg.tag("app_state").v("App is in foreground")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.ACTIVE
            )
        }

        updateLocationStatus()
        updateConnectionStatus()
        updateConfig()
    }

    fun onAppInactive() {
        logg.tag("app_state").v("App is inactive")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.INACTIVE
            )
        }
    }

    fun onAppBackground() {
        logg.tag("app_state").v("App is in background")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.BACKGROUND
            )
        }
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
        scopeProvider.userSessionIOScope.launch {
            refreshManager.scheduleRefreshAllGroupsWithPlaces(
                time = 60_000
            )
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

data class AppLifecycleState(
    val previous: AppLifecycleType = AppLifecycleType.DESTROYED,
    val current: AppLifecycleType = AppLifecycleType.DESTROYED
)
enum class AppLifecycleType {
    ACTIVE, INACTIVE, BACKGROUND, DESTROYED
}