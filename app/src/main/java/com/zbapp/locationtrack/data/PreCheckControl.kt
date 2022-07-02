package com.zbapp.locationtrack.data

import android.os.StrictMode
import com.zbapp.core.config
import com.zbapp.core.logg
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.exceptions.NotFoundException
import com.zbapp.locationtrack.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class PreCheckControl(
    private val userRepository: UserRepository,
) : BaseRepository() {

    fun start(): Flow<DataSetupState> {

        if (BuildConfig.internalTesting) {
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        }

        if (!config.isTokenValid)
            return flow { throw NotFoundException("User not found") }

        return flow {
            emit(DataSetupState.SESSION_READY)
            initialRefresh()
            emit(DataSetupState.USER_READY)
        }.onEach { logg.tag("initial_refresh").d(it.name) }
    }

    private suspend fun initialRefresh() {
        userRepository.refreshUserIfAvailable()
    }

    enum class DataSetupState {
        SESSION_READY, USER_READY, DATA_READY
    }

}