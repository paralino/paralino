package app.paralino.android.data

import android.os.StrictMode
import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.NotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class PreCheckControl(
    private val userRepository: UserRepository,
) : BaseRepository() {

    fun start(): Flow<DataSetupState> {

        if (app.paralino.android.BuildConfig.internalTesting) {
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