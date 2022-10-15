package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.repo.user.IPushNotificationManager
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class AppAccountStateMonitor(
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val userMapper: UserMapperCore,
    private val logoutUseCase: LogoutUseCase,
    private val pushManager: IPushNotificationManager
) {

    private var oldUserState: Resource<AppAccountState> = Resource.NotInitialized

    val userState: Flow<Resource<AppAccountState>> =
        combine(
            userRepo.localUserRaw,
            userKeyRepo.userKeyState()
        ) { userState, isKeyValid ->

            when (userState) {
                Resource.NotInitialized -> Resource.NotInitialized
                is Resource.Ready -> {
                    val user = userState.data
                    when {
                        user == null -> Resource.Ready(data = AppAccountState.NO_USER)
                        !isKeyValid -> {
                            if (config.temporaryBackupPassword.isNotBlank()) {
                                Resource.Ready(data = AppAccountState.ACCOUNT_RESET(user))
                            } else {
                                logoutUseCase.logoutLocally()
                                Resource.Ready(data = AppAccountState.NO_USER)
                            }
                        }
                        else -> {
                            try {
                                val decryptedUser = userMapper.remote2Core(user)
                                if (decryptedUser.hasValidData) {
                                    Resource.Ready(data = AppAccountState.USER_READY(decryptedUser))
                                } else {
                                    Resource.Ready(data = AppAccountState.NO_PROFILE(decryptedUser))
                                }
                            } catch (e: Exception) {
                                if (config.temporaryBackupPassword.isNotBlank()) {
                                    Resource.Ready(data = AppAccountState.ACCOUNT_RESET(user))
                                } else {
                                    logoutUseCase.logoutLocally()
                                    Resource.Ready(data = AppAccountState.NO_USER)
                                }
                            }
                        }
                    }
                }
            }

        }
            .onStart { emit(Resource.NotInitialized) }
            .conflate()
            .onEach {
                // we are delaying when user state changes from true to false
                // so in case of login, navigation is delayed a bit
                val wasUserNull = oldUserState.data is AppAccountState.NO_USER
                oldUserState = it
                if (wasUserNull && it.data is AppAccountState.USER_READY) {
                    delay(500)
                }
            }
            .distinctUntilChanged()
            .onEach {
                if(it.data is AppAccountState.USER_READY) {
                    logg.tag("push_notifications").v("User is ready, start push token registration")
                    pushManager.syncPushToken()
                }
            }

}

sealed class AppAccountState {
    object NO_USER : AppAccountState()
    data class NO_PROFILE(val user: UserMe) : AppAccountState()
    data class ACCOUNT_RESET(val userRaw: UserMeRaw) : AppAccountState()
    data class USER_READY(val user: UserMe) : AppAccountState()
}