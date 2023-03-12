package app.paralino.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface ILimitedExecution<T: Any> {

    val executionState: MutableStateFlow<List<T>>

    fun isProcessing(key: T): Boolean {
        return executionState.value.any { it == key }
    }

    fun processingState(key: T): Flow<Boolean> {
        return executionState.map { it.any { it == key } }
    }

    fun setAsProcessing(key: T) {
        executionState.update { it.toMutableList().apply { add(key) } }
    }

    fun stopProcessing(key: T) {
        executionState.update { it.toMutableList().apply { removeAll { it == key } } }
    }

}