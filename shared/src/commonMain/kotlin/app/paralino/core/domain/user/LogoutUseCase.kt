package app.paralino.core.domain.user

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.cancelChildren

class LogoutUseCase(
    private val userRepo: UserRepository,
    private val groupsRepo: UserGroupLocalDataSource,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val sharingManager: LocationSharingManager,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val codeRepo: PersonalCodesRepository,
    private val pushManager: IPushProviderManager,
    private val deviceStatus: IDeviceApiProvider,
    private val locationRepo: LocationUploadEngine,
    private val scopeProvider: ICoroutineScopeProvider,
    private val geofenceManager: IGeofenceManager,
    private val sessionMonitor: UserSessionMonitor,
    private val limitedExecutionParentManager: LimitedExecutionParentManager,
    private val appPrefs: AppPrefs,
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
            appPrefs.guestEmail = ""
            appPrefs.guestPassword = ""
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

        userKeyRepo.clearAll()
        cryptoKEKRepo.clearKeys()
        pushManager.logout()
        deviceStatus.logout()
        locationRepo.logout()
        groupsRepo.clearAll()
        placeActivityRepo.clearAll()
        codeRepo.clearLocally()
        sharingManager.stopAllSharing()
        geofenceManager.deleteAllGeofences()
        userRepo.clearLocally()
        logg.i("account_state", "Cleared all data on logout")
    }
}