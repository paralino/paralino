package app.paralino.core.domain

import app.paralino.core.logg
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.absoluteValue
import kotlin.time.DurationUnit
import kotlin.time.toDuration


open class LimitedExecutionRepository(
    private val execution: LimitedExecution = LimitedExecution()
) : BaseRepository(), ILimitedExecution by execution {
    init {
        execution.tag = this::class.simpleName
    }
}

interface ILimitedExecution {
    fun isProcessing(key: String): Boolean
    suspend fun startProcessing(
        key: String,
        delayedThresholdMillis: Long = 0,
        timeoutMillis: Long = 40_000
    )

    suspend fun stopProcessing(key: String)
    fun <T> Flow<T>.startProcessing(key: String): Flow<T> {
        return this
            .onStart { this@ILimitedExecution.startProcessing(key) }
            .onCompletion { if (it !is ExecutionInProgressException) stopProcessing(key) }
    }
}

open class LimitedExecution : ILimitedExecution {

    private val executionState = MutableStateFlow<Map<String, LimitedExecutionState>>(mapOf())
    private val mutex = Mutex()
    var tag: String? = this::class.simpleName

    override fun isProcessing(key: String): Boolean {
        val current = executionState.value.get(key) ?: return false
        val duration = nowUTC().toEpochMilliseconds() - current.startTimeMillis
        return duration < current.timeoutMillis
    }

    override suspend fun startProcessing(
        key: String,
        delayedThresholdMillis: Long,
        timeoutMillis: Long
    ) {
        logg.v("execution", "Start execution: ${tag ?: this::class.simpleName} ($key)")
        mutex.withLock {

            if (isProcessing(key)) {
                logg.w(
                    "execution",
                    "Execution ${this::class.simpleName} ($key) is already processing"
                )
                throw ExecutionInProgressException("Execution ${this::class.simpleName} ($key) is already processing")
            }

            executionState.value = executionState.value.toMutableMap()
                .apply {
                    put(
                        key = key,
                        value = LimitedExecutionState(
                            key = key,
                            delayedThresholdMillis = delayedThresholdMillis,
                            timeoutMillis = timeoutMillis
                        )
                    )
                }
        }
    }

    override suspend fun stopProcessing(key: String) {
        mutex.withLock {
            val currentState = executionState.value
            val current = currentState.get(key) ?: return
            val time = current
                .startTimeMillis
                .minus(nowUTC().toEpochMilliseconds())
                .absoluteValue
                .toDuration(DurationUnit.MILLISECONDS)

            logg.v("execution", "Stop execution: ${this::class.simpleName} ($key), time: $time")

            if (current.delayedThresholdMillis > 0) {
                executionState.value = currentState.toMutableMap().apply {
                    // shift and block execution by delayedThresholdMillis
                    set(
                        key = key,
                        value = current.copy(
                            startTimeMillis = nowUTC().toEpochMilliseconds(),
                            timeoutMillis = current.delayedThresholdMillis
                        )
                    )
                }
            } else {
                executionState.value = currentState.toMutableMap().apply { remove(key) }
            }
        }
    }

}

data class LimitedExecutionState(
    val key: String,
    val delayedThresholdMillis: Long = 0,
    val timeoutMillis: Long = 40_000,
    val startTimeMillis: Long = nowUTC().toEpochMilliseconds(),
)

class ExecutionInProgressException(message: String?): Exception(message)