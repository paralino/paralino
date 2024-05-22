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

open class LimitedExecution : ILimitedExecution {
    override val executionState = MutableStateFlow<Map<String, LimitedExecutionState>>(mapOf())
    override val mutex = Mutex()
    override var tag: String? = this::class.simpleName
}

open class LimitedExecutionRepository() : BaseRepository(), ILimitedExecution {
    override val executionState = MutableStateFlow<Map<String, LimitedExecutionState>>(mapOf())
    override val mutex = Mutex()
    override var tag: String? = this::class.simpleName
}

class LimitedExecutionParentManager() : BaseRepository(), ILimitedExecution {
    override val executionState = MutableStateFlow<Map<String, LimitedExecutionState>>(mapOf())
    override val mutex = Mutex()
    override var tag: String? = this::class.simpleName
}

interface ILimitedExecution {

    val executionState: MutableStateFlow<Map<String, LimitedExecutionState>>
    val mutex: Mutex
    var tag: String?

    fun isProcessingAny(): Boolean {
        val now = nowUTC().toEpochMilliseconds()
        return executionState.value.any {
            val duration = now - it.value.startTimeMillis
            return duration < it.value.timeoutMillis
        }
    }

    fun isProcessing(key: String): Boolean {
        val current = executionState.value.get(key) ?: return false
        val duration = nowUTC().toEpochMilliseconds() - current.startTimeMillis
        return duration < current.timeoutMillis
    }

    suspend fun startProcessing(
        key: String,
        delayedThresholdMillis: Long = 0,
        timeoutMillis: Long = 40_000
    ) {
        logg.v("execution", "Try to start execution: ${tag ?: this::class.simpleName} ($key)")
        mutex.withLock {

            if (isProcessing(key)) {
                logg.d(
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

    suspend fun stopProcessing(key: String) {
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


    fun <T> Flow<T>.startProcessing(key: String): Flow<T> {
        return this
            .onStart { this@ILimitedExecution.startProcessing(key) }
            .onCompletion { if (it !is ExecutionInProgressException) stopProcessing(key) }
    }
}

data class LimitedExecutionState(
    val key: String,
    val delayedThresholdMillis: Long = 0,
    val timeoutMillis: Long = 40_000,
    val startTimeMillis: Long = nowUTC().toEpochMilliseconds(),
)

class ExecutionInProgressException(message: String?) : Exception(message)