package com.zbapp.locationtrack.data.repos

import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class LogoutControl(
    private val userRepo: UserRepository,
    private val groupsRepo: GroupRepository,
    private val locationRepo: LocationRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository
) {

    fun logout(): Flow<Boolean> {
        return userRepo.logout()
            .map { true }
            .onEach {
                groupsRepo.clear()
                locationRepo.clear()
                cryptoKEKRepo.clearKeys()
                userKeyRepo.clearUserPersonalKey()
            }
    }

    fun deleteAccount(): Flow<Boolean> {
        return userRepo.deleteAccount()
            .map { true }
            .onEach {
                groupsRepo.clear()
                locationRepo.clear()
                cryptoKEKRepo.clearKeys()
                userKeyRepo.clearUserPersonalKey()
            }
    }

}