package app.paralino.core.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.paralino.core.ui.components.AppSnackbarData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class CoreUiState(
    val scaffoldState: ScaffoldState,
    val uiScope: CoroutineScope
) {

    fun showSnackbar(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        uiScope.launch {
            showSnackbar(text = text, actionText = null, duration = duration)
        }
    }

    suspend fun showSnackbar(
        text: String,
        actionText: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return scaffoldState.snackbarHostState.showSnackbar(
            message = text,
            actionLabel = actionText,
            duration = duration
        )
    }

    suspend fun showSnackbar(data: AppSnackbarData): SnackbarResult {
        return showSnackbar(
            text = data.text,
            actionText = data.actionText,
            duration = when (data.duration) {
                app.paralino.core.ui.components.SnackbarDuration.Long -> SnackbarDuration.Long
                app.paralino.core.ui.components.SnackbarDuration.Forever -> SnackbarDuration.Indefinite
                else -> SnackbarDuration.Short
            }
        )
    }
}

@Composable
fun rememberCoreUiState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    uiScope: CoroutineScope = rememberCoroutineScope()
) = remember(scaffoldState) {
    CoreUiState(
        scaffoldState = scaffoldState,
        uiScope = uiScope
    )
}
