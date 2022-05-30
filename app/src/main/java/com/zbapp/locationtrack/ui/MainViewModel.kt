package com.zbapp.locationtrack.ui

import androidx.lifecycle.ViewModel
import com.zbapp.core.models.Payload
import com.zbapp.core.models.Resource
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class MainViewModel(
    private val userRepo: UserRepository
): ViewModel() {

    val userState: Flow<Resource<Boolean>> = userRepo.localUser
        .map {
            when(it) {
                Payload.Loading -> Resource.NotInitialized
                is Payload.Success -> Resource.Ready(true)
                is Payload.Error -> Resource.Ready(false)
            }
        }

}