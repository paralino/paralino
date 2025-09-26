package app.paralino.core.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.CoroutineScope

open class CoreUiState(
    val scaffoldState: ScaffoldState,
    val uiScope: CoroutineScope
) {
    suspend fun showSnackbar(data: AppSnackbarData): SnackbarResult {
        return scaffoldState.snackbarHostState.showSnackbar(
            message = BaseRemoteModule.formatter.encodeToString(data.payload),
            actionLabel = data.actionText,
            duration = when (data.duration) {
                SnackbarDuration.Long -> androidx.compose.material.SnackbarDuration.Long
                SnackbarDuration.Forever -> androidx.compose.material.SnackbarDuration.Indefinite
                else -> androidx.compose.material.SnackbarDuration.Short
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
