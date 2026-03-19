package app.paralino.android.ui.main

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController
import app.paralino.core.ui.CoreUiState
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class MainAppState(
    scaffoldState: ScaffoldState,
    uiScope: CoroutineScope,
    val navController: NavControllerWrapper,
): CoreUiState(scaffoldState, uiScope)

@Composable
fun rememberAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    uiScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavControllerWrapper = rememberNavControllerWrapper()
) = remember(scaffoldState, navController) {
    MainAppState(
        scaffoldState = scaffoldState,
        uiScope = uiScope,
        navController = navController
    )
}

@Composable
fun rememberNavControllerWrapper(): NavControllerWrapper {
    val navController = rememberNavController()
    return remember(navController) { NavControllerWrapper(navController) }
}

class NavControllerWrapper(
    val navController: NavHostController
) {
    private val currentRoute = MutableStateFlow<Pair<String, Instant>?>(null)

    fun navigate(route: String) {
        navigateSafeAction(
            key = route,
            action = { navController.navigate(route = route) }
        )
    }

    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        navigateSafeAction(
            key = route,
            action = { navController.navigate(route = route, builder = builder) }
        )
    }

    fun navigate(route: String, navOptions: NavOptions?) {
        navigateSafeAction(
            key = route,
            action = { navController.navigate(route = route, navOptions = navOptions) }
        )
    }

    private fun navigateSafeAction(key: String, action: () -> Unit) {
        if (currentRoute.value?.first == key) {
            if (currentRoute.value?.second?.isOlderThan(500.milliseconds) == false) {
                // we ignore navigation here because fast tapping on the same route
            } else {
                currentRoute.update { key to nowUTC() }
                action()
            }
        } else {
            currentRoute.update { key to nowUTC() }
            action()
        }
    }

    fun popBackStack(route: String, inclusive: Boolean) {
        navController.popBackStack(route = route, inclusive = inclusive)
    }

    fun popBackStack() {
        navController.popBackStack()
    }
}