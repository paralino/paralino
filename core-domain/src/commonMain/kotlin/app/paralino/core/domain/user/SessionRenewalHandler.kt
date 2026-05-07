package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.logg
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SessionRenewalHandler(
    private val parentManager: LimitedExecutionParentManager,
    private val userRepo: UserRepository,
    private val appPrefs: AppPrefs,
    private val deviceApiProvider: IDeviceApiProvider
) : LimitedExecution(scope = parentManager) {

    private val sessionRenewalKey = "session_renewal"

    override fun isProcessing(key: String): Boolean {
        return isProcessingAny()
    }

    suspend fun processSessionRenewal() {
        try {
            startProcessing(sessionRenewalKey)
        } catch (e: Exception) {
            return
        }

        try {
            renewSessionIfNeeded()
        } catch (e: Exception) {
            return
        } finally {
            stopProcessing(sessionRenewalKey)
        }
    }

    private suspend fun renewSessionIfNeeded() {
        if (!deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
            logg.i(sessionRenewalKey, "Device not in foreground, skip session renewal")
            return
        }

        val lastCheck = appPrefs.sessionRenewalCheckTime ?: Instant.DISTANT_PAST
        val expiration = appPrefs.sessionExpiresAt ?: Instant.DISTANT_PAST
        val alreadyCheckedToday = !lastCheck.isOlderThan(1.days)
        val expiresSoon = expiration.minus(30.days) < nowUTC()

        if (alreadyCheckedToday) {
            if (!expiresSoon) {
                logg.i(sessionRenewalKey, "Session renewal not needed")
                return
            }
        }

        appPrefs.sessionRenewalCheckTime = nowUTC()

        logg.i(sessionRenewalKey, "Fetch current session ...")
        val currentSession = try {
            userRepo.getCurrentSessionFromRemote()
        } catch (e: Exception) {
            null
        }

        if (currentSession?.canRenewSessions == false) {
            logg.i(sessionRenewalKey, "No need to renew session, skip")
        } else {
            logg.i(sessionRenewalKey, "Renew session (Active: ${currentSession != null}) ...")
            userRepo.renewCurrentSession()
        }
    }
}