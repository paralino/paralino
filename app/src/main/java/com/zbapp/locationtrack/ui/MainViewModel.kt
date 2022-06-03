package com.zbapp.locationtrack.ui

import androidx.lifecycle.ViewModel
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class MainViewModel(
    private val userRepo: UserRepository,
) : ViewModel() {

    private var oldUserState: Resource<Boolean> = Resource.NotInitialized
    val userState: Flow<Resource<Boolean>> = userRepo.localUser
        .onStart { emit(Resource.NotInitialized) }
        .map {
            when (it) {
                Resource.NotInitialized -> Resource.NotInitialized
                is Resource.Ready -> Resource.Ready(data = it.data != null)
            }
        }
        .conflate()
        .onEach {
            // TODO replace with better approach
            // now we are delaying when user state changes from true to false
            // so in case of login, navigation is delayed a bit
            val wasUserNull = oldUserState.data == false
            oldUserState = it
            if (wasUserNull && it.data == true) {
                delay(500)
            }
        }

}