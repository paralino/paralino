package app.paralino.core.domain

import app.paralino.core.logg
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.math.absoluteValue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface ILimitedExecution<T: Any> {

    val executionState: MutableStateFlow<List<LimitedExecutionState<T>>>

    fun isProcessing(key: T): Boolean {
        return executionState.value.any { it.key == key }
    }

    fun processingState(key: T): Flow<Boolean> {
        return executionState.map { it.any { it.key == key } }
    }

    fun setAsProcessing(key: T) {
        logg.v("execution", "Start execution: ${this::class.simpleName} ($key)")
        executionState.update { it.toMutableList().apply { add(LimitedExecutionState(key)) } }
    }

    fun stopProcessing(key: T) {
        executionState.update {
            val time = it.firstOrNull { it.key == key }?.startTime?.minus(nowUTC().toEpochMilliseconds())?.absoluteValue?.toDuration(DurationUnit.MILLISECONDS)
            logg.v("execution", "Stop execution: ${this::class.simpleName} ($key), time: $time")
            it.toMutableList().apply {
                removeAll { it.key == key }
            }
        }
    }

}

data class LimitedExecutionState<T>(
    val key: T,
    val startTime: Long = nowUTC().toEpochMilliseconds()
)