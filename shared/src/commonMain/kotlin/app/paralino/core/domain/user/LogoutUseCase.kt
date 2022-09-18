package app.paralino.core.domain.user

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

class LogoutUseCase(
    private val userRepo: UserRepository,
    private val groupsRepo: GroupRepository,
    private val locationRepo: LocationRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val codeRepo: PersonalCodesRepository,
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
        clearResources()
        userRepo.logoutLocally()
    }

    private suspend fun clearResources() {
        userKeyRepo.clearAll()
        cryptoKEKRepo.clearKeys()
        groupsRepo.clear()
        locationRepo.clear()
        codeRepo.clearLocally()
    }

}