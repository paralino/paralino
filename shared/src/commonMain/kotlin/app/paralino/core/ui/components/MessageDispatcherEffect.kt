package app.paralino.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.ui.CoreUiState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MessageDispatcherEffect(
    messageDispatcher: ICoreMessageDispatcher,
    coreUiState: CoreUiState
) {
    LaunchedEffect(key1 = Unit) {
        // will dismiss previous snackbars
        messageDispatcher.snackbarChannel.collectLatest {
            val result = coreUiState.showSnackbar(it)
            messageDispatcher.onSnackbarResult(AppSnackbarResult(it, result))
        }
    }
}