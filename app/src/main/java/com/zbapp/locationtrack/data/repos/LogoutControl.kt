package com.zbapp.locationtrack.data.repos

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.zbapp.core.logg
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class LogoutControl(
    private val userRepo: UserRepository,
    private val groupsRepo: GroupRepository,
    private val locationRepo: LocationRepository
) {

    fun logout(): LiveData<Boolean> {
        return userRepo.logout()
            .onEach {
                groupsRepo.clear()
                locationRepo.clear()
            }
            .map { true }
            .catch {
                logg.e(it)
                emit(false)
            }
            .asLiveData()
    }

}