package app.paralino.core.domain.user

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.data.db.AppConfig
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationRepository
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

class LogoutUseCase(
    private val userRepo: UserRepository,
    private val groupsRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val sharingManager: LocationSharingManager,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val codeRepo: PersonalCodesRepository,
    private val pushManager: IPushProviderManager,
    private val deviceStatus: IDeviceApiProvider,
    private val locationRepo: LocationRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val geofenceManager: IGeofenceManager,
    private val config: AppConfig
) {

    fun logout(): Flow<Unit> {
        return userRepo.remoteLogout()
            .catch { emit(Unit) }
            .onEach {
                logoutLocally()
            }
    }

    fun deleteAccount(logoutOnError: Boolean = false): Flow<Unit> {
        return userRepo.deleteAccount()
            .catch {
                if (logoutOnError)
                    emit(Unit)
                else throw it
            }
            .onEach {
                logoutLocally()
            }
    }

    fun resetAccount(keyToPersist: UserCryptoKeyRaw?, logoutOnError: Boolean = false): Flow<Unit> {
        return userRepo.resetAccount(keyToPersist)
            .catch {
                if (logoutOnError)
                    emit(Unit)
                else throw it
            }
            .onEach {
                logoutLocally()
            }
    }

    suspend fun logoutLocally() {
        config.clear()
        scopeProvider.userSessionIOScope.coroutineContext.cancelChildren()
        clearResources()
        userRepo.logoutLocally()
    }

    private suspend fun clearResources() {
        userKeyRepo.clearAll()
        cryptoKEKRepo.clearKeys()
        pushManager.logout()
        deviceStatus.logout()
        locationRepo.logout()
        groupsRepo.clear()
        placesRepo.clear()
        codeRepo.clearLocally()
        sharingManager.stopAllSharing()
        geofenceManager.deleteAllGeofences()
    }
}