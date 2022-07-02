package com.zbapp.core.domain.user

import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.personal_code.PersonalCodesRepository
import com.zbapp.core.repo.user.UserRepository
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