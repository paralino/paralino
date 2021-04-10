package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.repo.circle.CircleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class LoginManager(
    val userRepo: UserRepository,
    val groupRepo: CircleRepository
) {

    fun isLoggedIn(): Boolean {
        return config.isTokenValid
    }

    fun signUp(username: String, password: String): Flow<Unit> {
        return userRepo.signUp(username, password)
            .flatMapLatest { syncData() }
    }

    fun login(username: String, password: String): Flow<Unit> {
        return userRepo.login(username, password)
            .flatMapLatest { syncData() }
    }

    private fun syncData(): Flow<Unit> {
        return userRepo.refreshUserIfAvailable()
            .flatMapLatest { groupRepo.getAllCircles() }
            .map { Unit }
    }

}