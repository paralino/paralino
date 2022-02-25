package com.zbapp.locationtrack.data.repos

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class LogoutControl(
    private val userRepo: UserRepository,
    private val groupsRepo: GroupRepository,
    private val locationRepo: LocationRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore
) {

    fun logout(): Flow<Boolean> {
        return userRepo.logout()
            .map { true }
            .onEach {
                groupsRepo.clear()
                locationRepo.clear()
                cryptoKEKRepo.clearKeys()
                cryptoCore.clearUserPersonalKey()
            }
    }

    fun deleteAccount(): Flow<Boolean> {
        return userRepo.deleteAccount()
            .map { true }
            .onEach {
                groupsRepo.clear()
                locationRepo.clear()
                cryptoKEKRepo.clearKeys()
                cryptoCore.clearUserPersonalKey()
            }
    }

}