package app.paralino.core.ui.components

import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.paralino.core.presentation.common.AppSnackbarResult
import app.paralino.core.presentation.common.AppSnackbarResultType
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
        messageDispatcher.snackbarChannel.collectLatest { snackbarData ->
            val result = coreUiState.showSnackbar(snackbarData).let {
                when (it) {
                    SnackbarResult.Dismissed -> AppSnackbarResultType.Dismissed
                    SnackbarResult.ActionPerformed -> AppSnackbarResultType.ActionPerformed
                }
            }
            messageDispatcher.onSnackbarResult(
                result = AppSnackbarResult(
                    data = snackbarData,
                    result = result
                )
            )
        }
    }
}