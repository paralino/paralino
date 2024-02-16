package app.paralino.core

import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.Resource
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CoreAppManager(
    private val scopeProvider: ICoroutineScopeProvider,
    private val configRepo: AppConfigRepository,
    private val userRepo: UserRepository,
    private val refreshManager: RefreshEverythingUseCase,
    private val sessionMonitor: UserSessionMonitor,
    private val logoutUseCase: LogoutUseCase,
    private val connectionStatusProvider: IConnectionStatusProvider,
    val locationSharing: LocationSharingManager,
    private val locationStatus: ILocationStatusProvider
) {

    private var wasInBackground: Boolean = false

    fun onAppStart() {
        sessionMonitor.listenForSessionExpired()
            .onEach {
                logg.tag("account_session").v("Got session expired event - logout")
                logoutUseCase.logoutLocally()
            }
            .launchIn(scopeProvider.defaultScope)
    }

    fun onAppActive() {
        logg.tag("app_state").v("App is in foreground")
        updateLocationStatus()
        updateConnectionStatus()
        updateConfig()

        if (wasInBackground) {
            wasInBackground = false
            updateGroups()
        }
    }

    fun onAppInactive() {
        logg.tag("app_state").v("App is in background")
        wasInBackground = true
    }

    fun locationSharingStatus(onChange: (Resource<Boolean>) -> Unit) {
        locationSharing.sharingStatus.onEach { onChange(it) }.launchIn(scopeProvider.defaultIOScope)
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