package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.repo.circle.CircleRepository
import com.zbapp.core.utils.UnverifiedUserException
import kotlinx.coroutines.flow.*

class LoginManager(
    val userRepo: UserRepository,
    val groupRepo: CircleRepository
) {

    fun isLoggedIn(): Boolean {
        return config.isTokenValid
    }

    fun isVerified(): Boolean {
        return config.emailConfirmed
    }

    fun signUp(serverUrl: String, username: String, email: String, password: String): Flow<Unit> {
        return userRepo.signUp(serverUrl, username, email, password)
    }

    fun login(serverUrl: String, username: String, password: String): Flow<Unit> {
        return userRepo.login(serverUrl, username, password)
            .flatMapLatest { syncData() }
            .catch {
                if(it is UnverifiedUserException)
                    emit(Unit)
                else throw it
            }
    }

    fun generateUsername(): Flow<String> {
        return userRepo.generateUsername()
    }

    fun syncData(): Flow<Unit> {
        return userRepo.refreshUserIfAvailable()
            .flatMapLatest { groupRepo.getAllCircles() }
            .map { Unit }
    }

}