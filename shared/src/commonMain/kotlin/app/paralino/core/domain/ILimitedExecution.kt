package app.paralino.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface ILimitedExecution<T: Any> {

    val executionState: MutableStateFlow<List<T>>

    fun isProcessing(key: T): Boolean {
        return executionState.value.any { it == key }
    }

    fun setAsProcessing(key: T) {
        executionState.update { it.toMutableList().apply { add(key) } }
    }

    fun stopProcessing(key: T) {
        executionState.update { it.toMutableList().apply { removeAll { it == key } } }
    }

}