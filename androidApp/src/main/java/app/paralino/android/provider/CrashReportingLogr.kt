package app.paralino.android.provider

import app.paralino.core.db.AppPrefs
import app.paralino.core.provider.ICrashReportingLogr
import app.paralino.core.provider.LogPriority
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.exceptions.InternalConnectionException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException

class CrashReportingLogr(
    private val appConfig: AppConfigRepository,
    private val appPrefs: AppPrefs
): ICrashReportingLogr() {
    override fun systemLog(
        priority: LogPriority,
        tag: String?,
        message: String,
        fullOutput: String,
        t: Throwable?
    ) {
        if (!appPrefs.errorReporting) {
            return
        }

        if (priority != LogPriority.ERROR || !appConfig.logErrorsToRemote) {
            return
        }

        val exceptionAllowed =
            t != null && t !is SocketTimeoutException && t !is InternalConnectionException && t !is ConnectException &&
                    t !is MissingGroupKeyException && t !is CancellationException
        if (exceptionAllowed) {
            Sentry.captureException(t!!)
        }
    }
}