package app.paralino.android.ui.main

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.paralino.core.ui.CoreUiState
import kotlinx.coroutines.CoroutineScope

class MainAppState(
    scaffoldState: ScaffoldState,
    uiScope: CoroutineScope,
    val navController: NavHostController,
): CoreUiState(scaffoldState, uiScope)

@Composable
fun rememberAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    uiScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController()
) = remember(scaffoldState, navController) {
    MainAppState(
        scaffoldState = scaffoldState,
        uiScope = uiScope,
        navController = navController
    )
}