package com.zbapp.core.domain.user

import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.personal_code.PersonalCodesRepository
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class LogoutUseCase(
    private val userRepo: UserRepository,
    private val groupsRepo: GroupRepository,
    private val locationRepo: LocationRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val codeRepo: PersonalCodesRepository,
) {

    fun logout(): Flow<Boolean> {
        return userRepo.logout()
            .map { true }
            .onEach {
                clearResources()
            }
    }

    fun deleteAccount(): Flow<Boolean> {
        return userRepo.deleteAccount()
            .map { true }
            .onEach {
                clearResources()
            }
    }

    suspend fun logoutLocally() {
        userRepo.logoutLocally()
        clearResources()
    }

    private suspend fun clearResources() {
        groupsRepo.clear()
        locationRepo.clear()
        cryptoKEKRepo.clearKeys()
        userKeyRepo.clearUserPersonalKey()
        codeRepo.clearLocally()
    }

}