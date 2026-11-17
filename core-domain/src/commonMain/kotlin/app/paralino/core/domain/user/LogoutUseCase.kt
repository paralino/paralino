package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationSharingContractFilter
import app.paralino.core.repo.places.FalsePlaceAlertMemory
import app.paralino.core.repo.places.WifiAnchorMemory
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.cancelChildren

class LogoutUseCase(
    private val userRepo: UserRepository,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val pushManager: IPushProviderManager,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationRepo: LocationUploadEngine,
    private val scopeProvider: ICoroutineScopeProvider,
    private val geofenceManager: IGeofenceManager,
    private val sessionMonitor: UserSessionMonitor,
    private val limitedExecutionParentManager: LimitedExecutionParentManager,
    private val accessLogs: AccessLogDataSource,
    private val appPrefs: AppPrefs,
    private val sharingContractFilter: LocationSharingContractFilter,
    private val appMainDatabase: MainDatabaseHelper,
    private val falsePlaceAlertMemory: FalsePlaceAlertMemory,
    private val wifiAnchorMemory: WifiAnchorMemory,
) : LimitedExecution(scope = limitedExecutionParentManager) {

    private val executionKey = "logout_or_delete"

    suspend fun fullLogout(logoutLocallyOnError: Boolean) {
        logg.w("account_state", "Start full logout")
        accountAction(logoutLocallyOnError = logoutLocallyOnError) {
            userRepo.remoteLogout()
        }
    }

    suspend fun deleteAccount(logoutLocallyOnError: Boolean) {
        accountAction(logoutLocallyOnError = logoutLocallyOnError) {
            userRepo.deleteAccount()
            appPrefs.invalidateGuestAccount()
        }
    }

    suspend fun resetAccount(keyToPersist: UserCryptoKeyRaw?, logoutLocallyOnError: Boolean) {
        accountAction(logoutLocallyOnError = logoutLocallyOnError) {
            userRepo.resetAccount(keyToPersist = keyToPersist)
        }
    }

    private suspend fun accountAction(logoutLocallyOnError: Boolean, action: suspend () -> Unit) {
        startProcessing(key = executionKey)
        try {
            action()
            sessionMonitor.resetSessionState()
            appPrefs.wasUserLoggedIn = false
            logoutLocallyImpl()
        } catch (e: Exception) {
            if (e is UnauthorizedException) {
                appPrefs.wasUserLoggedIn = false
            }

            if (logoutLocallyOnError || e is UnauthorizedException) {
                logoutLocallyImpl()
            }
            throw e
        } finally {
            stopProcessing(key = executionKey)
        }
    }

    suspend fun logoutLocally() {
        // TODO clear appwrite session cookie if still somehow present
        // would be possible if we fork appwrite sdk

        try {
            startProcessing(key = executionKey)
        } catch (e: Exception) {
            logg.w("account_state", "Local logout already processing")
            return
        }

        try {
            logoutLocallyImpl()
        } finally {
            stopProcessing(key = executionKey)
        }
    }

    private suspend fun logoutLocallyImpl() {
        logg.w("account_state", "Logging user out locally")
        appPrefs.clear()
        scopeProvider.userSessionIOScope.coroutineContext.cancelChildren()

        appMainDatabase.logoutDao.logout()

        userKeyRepo.clearAllKeys()
        cryptoKEKRepo.clearCachedKeys()
        pushManager.logout()
        deviceApiProvider.logout()
        locationRepo.logout()
        placeActivityRepo.clearCache()
        falsePlaceAlertMemory.clear()
        wifiAnchorMemory.clear()
        geofenceManager.deleteAllGeofences()
        accessLogs.clearAll()
        sharingContractFilter.clearFilter()
        logg.i("account_state", "Cleared all data on logout")
    }
}