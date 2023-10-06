package app.paralino.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.ui.components.AppSnackbarData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

actual abstract class BaseViewModel actual constructor() : ViewModel() {
    actual val coroutineScope: CoroutineScope = viewModelScope

    actual override fun onCleared() {
        super.onCleared()
    }

    actual fun cleanup() {
        coroutineScope.cancel()
        onCleared()
    }
}