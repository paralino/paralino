package app.paralino.core.presentation.common

import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NetworkQualityMonitor() {

    private val networkLogs = MutableStateFlow(listOf<NetworkRequestEntry>())

    fun addEntry(key: String) {
        networkLogs.update { logs ->
            val newLog = NetworkRequestEntry(
                key = key,
                start = nowUTC(),
                end = null,
                errorType = null
            )

            val threshold = nowUTC().minus(3.minutes)
            logs.filter { it.start > threshold } + newLog
        }
    }

    fun stopEntry(key: String, errorType: NetworkRequestErrorType?) {
        networkLogs.update { logs ->
            logs.map {
                if (it.key == key) {
                    it.copy(
                        end = nowUTC(),
                        errorType = errorType
                    )
                } else {
                    it
                }
            }
        }
    }

    fun listenForBadNetwork(): Flow<Unit> {
        return networkLogs
            .map {
                val threshold = nowUTC().minus(2.minutes)
                it
                    .filter { it.start > threshold && it.end != null && it.errorType != null }
                    .take(100)
            }
            .map {
                val timeoutCount = it.count { it.errorType == NetworkRequestErrorType.TIMEOUT }
                val errorCount = it.count { it.errorType == NetworkRequestErrorType.UNKNOWN_HOST }
                val veryLongRequestCount = it.count { it.duration?.let { it > 15.seconds } == true }
                val longRequestCount = it.count { it.duration?.let { it > 10.seconds } == true }
                val mediumRequestCount = it.count { it.duration?.let { it > 10.seconds } == true }

                val result = timeoutCount + veryLongRequestCount + sqrt(longRequestCount.toFloat()).toInt() + errorCount
                result > 2 || mediumRequestCount > 5
            }
            .filter { it }
            .map { Unit }
    }

    fun clearLogs() {
        networkLogs.update { emptyList() }
    }

    data class NetworkRequestEntry(
        val key: String,
        val start: Instant,
        val end: Instant?,
        val errorType: NetworkRequestErrorType?
    ) {
        val duration: Duration? get() = end?.minus(start)
    }

    enum class NetworkRequestErrorType {
        TIMEOUT, UNKNOWN_HOST, OTHER
    }
}