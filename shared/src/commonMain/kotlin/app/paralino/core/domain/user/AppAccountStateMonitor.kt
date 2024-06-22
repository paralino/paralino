package app.paralino.core.domain.user

import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.data.db.AppConfig
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class AppAccountStateMonitor constructor(
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val userMapper: UserMapperCore,
    private val refreshEverything: RefreshEverythingUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val pushManager: IPushProviderManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val userSessionMonitor: UserSessionMonitor,
    private val config: AppConfig
) {

    private var oldUserState: Resource<AppAccountState> = Resource.NotInitialized
    private var everHadUser = false

    val userState: Flow<Resource<AppAccountState>> =
        userRepo.localUserRaw.map { userState ->
            when (userState) {
                Resource.NotInitialized -> Resource.NotInitialized
                is Resource.Ready -> {
                    val user = userState.data
                    when {
                        user == null -> {
                            if (config.isTokenValid) {
                                // try and sync user
                                logg.tag("account_state")
                                    .v("User is null but token is active - try to refresh")
                                Resource.Ready(data = AppAccountState.NO_USER_DATA)
                            } else {
                                logg.tag("account_state").v("User is null and token is missing")
                                Resource.Ready(data = AppAccountState.NO_USER(canRecoverGuest = config.hasGuestAccount()))
                            }
                        }

                        userKeyRepo.userKeyState().firstOrNull() != true -> {
                            if (config.temporaryMasterAuthKey.isNotBlank()) {
                                logg.tag("account_state")
                                    .v("Key is not valid but backup password is active")
                                Resource.Ready(data = AppAccountState.ACCOUNT_RESET(user))
                            } else {
                                logg.tag("account_state")
                                    .v("Key is not valid and no backup password")
                                Resource.Ready(data = AppAccountState.NO_USER(canRecoverGuest = config.hasGuestAccount()))
                            }
                        }

                        else -> {
                            try {
                                val decryptedUser = userMapper.remote2Core(user)
                                if (decryptedUser.hasValidData) {
                                    logg.tag("account_state").v("User is ready")
                                    Resource.Ready(data = AppAccountState.USER_READY(decryptedUser))
                                } else {
                                    logg.tag("account_state")
                                        .v("User is ready but profile is missing")
                                    Resource.Ready(data = AppAccountState.NO_PROFILE(decryptedUser))
                                }
                            } catch (e: Exception) {
                                logg.e("Error decrypting user", e)
                                if (config.temporaryMasterAuthKey.isNotBlank()) {
                                    logg.tag("account_state").v("User needs to reset account")
                                    Resource.Ready(data = AppAccountState.ACCOUNT_RESET(user))
                                } else {
                                    logg.tag("account_state")
                                        .w("User can not be initialized (${e.message})")
                                    Resource.Ready(data = AppAccountState.NO_USER(canRecoverGuest = config.hasGuestAccount()))
                                }
                            }
                        }
                    }
                }
            }
        }
            .onStart { emit(Resource.NotInitialized) }
            .conflate()
            .transform {
                // we are delaying when user state changes from true to false
                // so in case of login, navigation is delayed a bit
                val wasUserNull = oldUserState.data is AppAccountState.NO_USER
                oldUserState = it

                if (it is Resource.Ready && it.data !is AppAccountState.NO_USER) {
                    userSessionMonitor.updateSessionState(state = UserSessionState.OK)
                    everHadUser = true
                } else {
                    userSessionMonitor.resetSessionState()
                }

                if (wasUserNull && it.data is AppAccountState.USER_READY) {
                    logg.tag("account_state").v("${it.data} - User ready - delay for ui")
                    delay(500)
                }

                if ((wasUserNull && it.data is AppAccountState.USER_READY) || it.data is AppAccountState.NO_USER_DATA) {
                    logg.tag("account_state").v("${it.data} - User data needs refresh")
                    emit(Resource.Ready(AppAccountState.DATA_REFRESH))
                    refreshEverything.refreshEverything(forceGroupRefresh = true)
                } else if (it.data is AppAccountState.USER_READY) {
                    if (config.didEverFullyRefreshData) {
                        logg.tag("account_state")
                            .v("${it.data} - Refresh user data in background - don't force")
                        scopeProvider.userSessionIOScope.launch {
                            try {
                                logg.tag("account_state").v("Refresh everything on different scope")
                                refreshEverything.refreshEverything(forceGroupRefresh = false)
                            } catch (e: Exception) {
                                logg.tag("account_state").v("Unable to refresh everything: ${e.message}")
                            }
                        }
                    } else {
                        logg.tag("account_state").v("${it.data} - First time refresh")
                        emit(Resource.Ready(AppAccountState.DATA_REFRESH))
                        refreshEverything.refreshEverything(forceGroupRefresh = true)
                        config.didEverFullyRefreshData = true
                    }
                } else if (!everHadUser && it.data is AppAccountState.NO_USER) {
                    logg.tag("account_state")
                        .v("${it.data} - try to logout (${config.wasUserLoggedIn})")
                    if (config.wasUserLoggedIn) {
                        try {
                            logoutUseCase.fullLogout(logoutLocallyOnError = true)
                        } catch (e: Exception) {
                        }
                    }
                } else {
                    logg.tag("account_state").v("${it.data} - Not refreshing user data")
                }
                emit(it)
            }
            .distinctUntilChanged()
            .onEach {
                when (it.data) {
                    is AppAccountState.USER_READY -> {
                        logg.tag("push_notifications")
                            .v("User is ready, start push token registration")
                        pushManager.syncPushToken()
                        pushManager.syncPushNotifications()
                    }

                    else -> {}
                }
            }
            .flowOn(Dispatchers.IO)
}

sealed class AppAccountState {
    data class NO_USER(val canRecoverGuest: Boolean) : AppAccountState()
    object NO_USER_DATA : AppAccountState()
    object DATA_REFRESH : AppAccountState()
    data class NO_PROFILE(val user: UserMe) : AppAccountState()
    data class ACCOUNT_RESET(val userRaw: UserMeRaw) : AppAccountState()
    data class USER_READY(val user: UserMe) : AppAccountState()
}