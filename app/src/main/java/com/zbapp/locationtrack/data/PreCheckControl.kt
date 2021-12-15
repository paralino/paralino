package com.zbapp.locationtrack.data

import android.os.StrictMode
import com.zbapp.core.config
import com.zbapp.core.logg
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.NotFoundException
import com.zbapp.locationtrack.BuildConfig
import kotlinx.coroutines.flow.*

class PreCheckControl(
    private val userRepository: UserRepository
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
            userRepository.restoreSession()
            emit(DataSetupState.SESSION_READY)
            initialRefresh().firstOrNull()
            emit(DataSetupState.USER_READY)
        }.onEach { logg.tag("initial_refresh").d(it.name) }
    }

    private fun initialRefresh(): Flow<Unit> {
        return userRepository.refreshUserIfAvailable()
            .map { Unit }
    }

    enum class DataSetupState {
        SESSION_READY, USER_READY, DATA_READY
    }

}