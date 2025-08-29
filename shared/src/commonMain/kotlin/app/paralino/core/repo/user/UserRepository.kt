package app.paralino.core.repo.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.UserCryptoKeyRefreshUseCase
import app.paralino.core.domain.user.UserCryptoKeyState
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.domain.user.UserSessionState
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.data
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.raw.LoginDetailsRaw
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.BlocklistModel
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPlans
import app.paralino.core.models.user.UserSession
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class UserRepository constructor(
    private val scopeProvider: ICoroutineScopeProvider,
    private val local: UserLocalDataSource,
    private val remote: UserRemoteSource,
    private val sessionMonitor: UserSessionMonitor,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val userMapper: UserMapperCore,
    private val cryptoCore: CryptoCore,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val refreshUserKeys: UserCryptoKeyRefreshUseCase,
    private val appPrefs: AppPrefs
) : BaseRepository() {

    val localUser: StateFlow<Resource<UserMe?>> = combine(
        local.getMeRaw(),
        userKeyRepo.userKeyState()
    ) { user, keyState ->
        if (keyState != UserCryptoKeyState.OK) {
            Resource.NotInitialized
        } else {
            try {
                Resource.Ready(user?.let { userMapper.remote2Core(it) })
            } catch (e: Exception) {
                Resource.NotInitialized
            }
        }
    }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.NotInitialized
        )

    val localUserRaw: StateFlow<Resource<UserMeRaw?>> = local.getMeRaw()
        .map<UserMeRaw?, Resource<UserMeRaw?>> {
            logg.v("Local raw user changed, valid (${it != null})")
            Resource.Ready(it)
        }
        .catch {
            logg.w("Local raw user error: ${it.message}")
            emit(Resource.NotInitialized)
        }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.NotInitialized
        )

    val userPlans: StateFlow<UserPlans> = combine(
        local.getPlans(),
        remoteConfigRepo.getConfigLocally()
    ) { plans, config ->
        UserPlans(
            rawPlans = plans,
            config = config
        )
    }
        .catch {
            logg.w("User plans parsing error: ${it.message}")
            emit(UserPlans(emptyList(), remoteConfigRepo.getConfigOnce()))
        }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPlans(emptyList(), remoteConfigRepo.getCachedConfigOnce())
        )

    val cachedUser: UserMe? get() = localUser.value.data

    val shouldShowFreePlanNoticeScreen: Boolean get() {
        return userPlans.value.highestValidPlanConfig.planTypeEnum == PlanType.SILVER
                && !appPrefs.didStartedGroupOnboarding
    }

    suspend fun login(email: String, password: String): UserSession {
        return triggerRemoteOnly { remote.login(email, password) }
    }

    suspend fun getAllSessionsFromRemote(): List<UserSession> {
        return triggerRemoteOnly { remote.getAllSessions() }
    }

    suspend fun remoteSignUp(
        email: String,
        password: String,
    ): UserMeRaw {
        return triggerRemoteOnly { remote.signUp(email, password) }
    }

    suspend fun initLocallyAfterSignUp(userMeRaw: UserMeRaw, userSession: UserSession) {
        updateLocalUser(userMeRaw)
        appPrefs.deviceId = cryptoCore.hashProvider.computeHash(
            data = "${userMeRaw.id}${appPrefs.deviceIdSeed}",
            type = HashType.GENERIC_256,
            encodingType = EncodingType.BASE64_URL
        )
        appPrefs.sessionId = userSession.id
        sessionMonitor.updateSessionState(state = UserSessionState.OK)
    }

    suspend fun updateUserEmailAndPassword(newEmail: String, oldPassword: String, newPassword: String) {
        triggerRemoteOnly {
            remote.updateEmailAndPassword(
                email = newEmail,
                oldPassword = oldPassword,
                newPassword = newPassword
            )

        }.also {
            appPrefs.emailVerificationSkipTime = null
            refreshUserMaybe(timeDelayMillis = 0)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        triggerRemoteOnly {
            remote.changePassword(
                newPassword = newPassword,
                oldPassword = oldPassword
            )
        }
    }

    suspend fun initResetPassword(email: String) {
        triggerRemoteOnly {
            remote.initResetPassword(email)
        }
    }

    suspend fun resetPassword(userId: String, secret: String, password: String) {
        triggerRemoteOnly {
            remote.resetPassword(userId, secret, password)
        }
    }

    suspend fun getUserFromRemote(): UserMeRaw {
        return try {
            triggerRemoteOnly { remote.getMe() }
        } catch (e: Exception) {
            if (e is UnauthorizedException) {
                logg.v("User session is not valid")
            } else if (e is NotFoundException) {
                logg.v("User does not exist")
            } else {
                logg.e("Error refreshing user")
            }
            throw e
        }
    }

    suspend fun refreshUserIfAvailable(): UserMeRaw {
        return getUserFromRemote()
            .also { updateLocalUser(it) }
            .also { sessionMonitor.updateSessionState(state = UserSessionState.OK) }
            .also { refreshUserKeys.checkUserKeyStatus() }
            .also {
                try {
                    remote.getPlans().also { local.savePlans(it) }
                } catch (e: Exception) {
                    logg.w("Error refreshing user plans: ${e.message}")
                }
            }
    }

    suspend fun refreshUserMaybe(timeDelayMillis: Long = 10_000): Payload<UserMeRaw?> {
        try {
            return triggerRemoteMaybe(
                remote = { refreshUserIfAvailable() },
                remotePreference = RemotePreference.CACHE("user_call", timeDelayMillis)
            ).let { Payload.Success(it) }
        } catch (e: Exception) {
            return Payload.Error(e)
        }
    }

    suspend fun updateLocalUser(data: UserMeRaw) {
        appPrefs.userId = data.id
        appPrefs.email = data.email
        appPrefs.wasUserLoggedIn = true
        local.update(data)

        if (appPrefs.emailConfirmed != data.emailVerification) {
            appPrefs.emailVerificationSkipTime = null
        }

        appPrefs.emailConfirmed = data.emailVerification
    }

    suspend fun initEmailVerification() {
        triggerRemoteOnly { remote.initEmailVerification() }
    }

    suspend fun confirmEmail(userId: String, secret: String) {
        triggerRemoteOnly { remote.confirmEmail(userId, secret) }
    }

    fun getBlocklistLocally(): Flow<List<BlocklistModel>> {
        return local.getBlocklist()
    }

    fun getBlocklist(): Flow<List<BlocklistModel>> {
        return mergeDataWithRemote(
            local = local.getBlocklist(),
            remote = { remote.getBlocklist() },
            localUpdate = { local.recreateBlocklist(it) }
        )
    }

    suspend fun refreshBlocklist(cacheTimeMillis: Long) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(key = "blocklist", timeMillis = cacheTimeMillis)
        ) { remote.getBlocklist() }
            ?.also { local.recreateBlocklist(it) }
    }

    suspend fun removeUserFromBlocklist(userId: String) {
        triggerRemoteOnly {
            val id = local.getBlocklist().first().first { it.resourceId == userId }.id
            remote.unblock(id)
            id
        }
            .also { local.removeFromBlocklist(it) }
    }

    suspend fun removeFromBlocklist(id: String) {
        triggerRemoteOnly { remote.unblock(id) }
            .also { local.removeFromBlocklist(id) }
    }

    suspend fun redeemPromoCode(code: String) {
        triggerRemoteOnly { remote.redeemPromoCode(code = code) }
    }

    suspend fun clearUserAndPlansLocally() {
        local.clearUserAndPlans()
    }

    suspend fun remoteLogout(sessionId: String) {
        triggerRemoteOnly { remote.logoutSession(sessionId = sessionId) }
    }

    suspend fun remoteLogout() {
        triggerRemoteOnly { remote.logoutCurrentSession() }
    }

    suspend fun deleteAccount() {
        triggerRemoteOnly { remote.deleteAccount() }
    }

    suspend fun resetAccount(keyToPersist: UserCryptoKeyRaw?) {
        triggerRemoteOnly { remote.resetAccount(keyToPersist) }
    }

    suspend fun getLoginDetails(key: String): LoginDetailsRaw {
        return triggerRemoteOnly {
            try {
                remote.getLoginDetails(key)
            } catch (e: Exception) {
                delay(500)
                remote.getLoginDetails(key)
            }
        }
    }
}