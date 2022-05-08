package com.zbapp.locationtrack.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainAppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
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
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG -> SnackbarDuration.Long
                com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE -> SnackbarDuration.Indefinite
                else -> SnackbarDuration.Short
            }
        )
    }
}

@Composable
fun rememberAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
    uiScope: CoroutineScope = rememberCoroutineScope()
) = remember(scaffoldState, navController) {
    MainAppState(
        scaffoldState = scaffoldState,
        navController = navController,
        uiScope = uiScope
    )
}