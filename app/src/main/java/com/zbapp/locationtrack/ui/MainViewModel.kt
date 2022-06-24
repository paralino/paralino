package com.zbapp.locationtrack.ui

import androidx.lifecycle.ViewModel
import com.zbapp.core.domain.user.UserSessionMonitor
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.presentation.common.AppAccountState
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class MainViewModel(
    private val userRepo: UserRepository,
    private val sessionMonitor: UserSessionMonitor
) : ViewModel() {

    val sessionExpiredFlow = sessionMonitor.listenForSessionExpired()

    private var oldUserState: Resource<AppAccountState> = Resource.NotInitialized
    val userState: Flow<Resource<AppAccountState>> = userRepo.localUser
        .onStart { emit(Resource.NotInitialized) }
        .map {
            when (it) {
                Resource.NotInitialized -> {
                    Resource.NotInitialized
                }
                is Resource.Ready -> {
                    val user = it.data
                    when {
                        user == null -> Resource.Ready(data = AppAccountState.NO_USER)
                        !user.hasValidData -> {
                            Resource.Ready(data = AppAccountState.NO_PROFILE)
                        }
                        else -> {
                            Resource.Ready(data = AppAccountState.USER_READY)
                        }
                    }
                }
            }
        }
        .conflate()
        .onEach {
            // TODO replace with better approach
            // now we are delaying when user state changes from true to false
            // so in case of login, navigation is delayed a bit
            val wasUserNull = oldUserState.data is AppAccountState.NO_USER
            oldUserState = it
            if (wasUserNull && it.data is AppAccountState.USER_READY) {
                delay(500)
            }
        }

}