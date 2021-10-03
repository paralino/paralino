package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.UnverifiedUserException
import kotlinx.coroutines.flow.*

class LoginManager(
    val userRepo: UserRepository,
    val groupRepo: GroupRepository
) {

    fun isLoggedIn(): Boolean {
        return config.isTokenValid
    }

    fun isVerified(): Boolean {
        return config.emailConfirmed
    }

    fun signUp(userName: String, email: String, password: String): Flow<Unit> {
        return userRepo.signUp(userName, email, password)
    }

    fun login(email: String, password: String): Flow<Unit> {
        return userRepo.login(email, password)
            .flatMapLatest { syncData() }
            .catch {
                if(it is UnverifiedUserException)
                    emit(Unit)
                else throw it
            }
    }

    fun syncData(): Flow<Unit> {
        return userRepo.refreshUserIfAvailable()
            .flatMapLatest { groupRepo.getAllGroups() }
            .map { Unit }
    }

}