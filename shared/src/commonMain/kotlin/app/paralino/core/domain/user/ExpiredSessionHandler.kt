package app.paralino.core.domain.user

import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.logg
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

class ExpiredSessionRecovery(
    private val logoutUseCase: LogoutUseCase,
    private val loginUseCase: LoginUseCase,
    private val parentManager: LimitedExecutionParentManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val sessionMonitor: UserSessionMonitor,
    private val userRepo: UserRepository,
    private val config: AppConfig,
) : LimitedExecution(scope = parentManager) {

    private val sessionRecoveryKey = "guest_session_recovery"

    override fun isProcessing(key: String): Boolean {
        return isProcessingAny()
    }

    suspend fun processExpiredSession() {
        startProcessing(sessionRecoveryKey)

        val cachedUser = userRepo.cachedUser
        if (cachedUser == null || !cachedUser.isGuestUser || !config.hasGuestAccount()) {
            logg.w("account_state", "User is not a guest, skip session recovery")
            logoutUseCase.logoutLocally()
            sessionMonitor.updateSessionState(state = UserSessionState.EXPIRED)
            stopProcessing(sessionRecoveryKey)
            return
        }

        logg.w("account_state", "User is guest, try to recover session")

        try {
            blocking {
                // clearing user here will disable any further potential actions until session is refreshed
                userRepo.clearLocally()
                loginUseCase.login(email = config.guestEmail, password = config.guestPassword)
                    .catch {
                        logg.w(
                            tag = "account_state",
                            message = "Error while recovering guest session: ${it.message}",
                            t = it
                        )
                        val cause = it.cause
                        if (cause is UnauthorizedException) {
                            if (cause.type == UserSessionMonitor.invalidCredentials) {
                                logg.w(
                                    tag = "account_state",
                                    message = "Guest user is no longer authorized, logout locally and clear user"
                                )
                                config.guestEmail = ""
                                config.guestPassword = ""
                            } else {
                                logg.w(
                                    tag = "account_state",
                                    message = "Guest user is no longer authorized, logout locally"
                                )
                            }

                            logoutUseCase.logoutLocally()
                            sessionMonitor.updateSessionState(state = UserSessionState.EXPIRED)
                        }
                    }
                    .collectLatest {
                        if (it == SignUpStep.ALL_DONE) {
                            // done and ready
                            logg.w(
                                tag = "account_state",
                                message = "Guest session recovery finished"
                            )
                        }
                    }
            }
        } finally {
            stopProcessing(key = sessionRecoveryKey)
        }
    }
}

class ExpiredSessionHandler(
    private val expiredSessionRecovery: ExpiredSessionRecovery,
    private val refreshEverythingUseCase: RefreshEverythingUseCase,
    private val sessionMonitor: UserSessionMonitor,
) : LimitedExecution() {

    private val sessionRecoveryKey = "guest_session_handler"

    fun observeProcessing(): Flow<Boolean> {
        return observeIsProcessing(sessionRecoveryKey)
    }

    suspend fun processExpiredSession() {
        startProcessing(sessionRecoveryKey)

        try {
            expiredSessionRecovery.processExpiredSession()

            if (sessionMonitor.isSessionValid()) {
                logg.i("account_state", "Guest session recovered, refresh everything")
                refreshEverythingUseCase.refreshEverything(forceGroupRefresh = true)
            }
        } finally {
            stopProcessing(sessionRecoveryKey)
        }
    }
}