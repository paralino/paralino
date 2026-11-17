package app.paralino.android.ui.main

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.ui.CoreUiState
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class MainAppState(
    scaffoldState: ScaffoldState,
    uiScope: CoroutineScope,
    val navController: NavControllerWrapper,
): CoreUiState(scaffoldState, uiScope)


@Composable
fun rememberAppState(
    startScreen: AppScreen,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    uiScope: CoroutineScope = rememberCoroutineScope(),
): MainAppState {
    val backStack = rememberNavBackStack(startScreen)
    return remember(scaffoldState, uiScope, backStack) {
        MainAppState(
            scaffoldState = scaffoldState,
            uiScope = uiScope,
            navController = NavControllerWrapper(backStack)
        )
    }
}


class NavControllerWrapper(
    val backStack: NavBackStack<NavKey>
) {
    private var lastNavigation: Pair<AppScreen, Instant>? = null

    fun navigate(screen: AppScreen) {
        navigateSafeAction(screen) { push(screen) }
    }

    fun navigate(
        screen: AppScreen,
        popUpTo: KClass<out AppScreen>,
        inclusive: Boolean = false
    ) {
        navigateSafeAction(screen) {
            val index = backStack.indexOfLast { popUpTo.isInstance(it) }
            if (index == -1) {
                push(screen)
            } else {
                val keepCount = if (inclusive) index else index + 1
                // add first, so back stack is never empty
                backStack.add(screen)
                repeat(backStack.size - 1 - keepCount) { backStack.removeAt(keepCount) }
            }
        }
    }

    private fun navigateSafeAction(
        screen: AppScreen,
        action: () -> Unit
    ) {
        val last = lastNavigation
        if (last?.first == screen && !last.second.isOlderThan(500.milliseconds)) {
            // we ignore navigation here because fast tapping on the same screen
            return
        }
        lastNavigation = screen to nowUTC()
        action()
    }

    private fun push(screen: AppScreen) {
        // same screen twice on top would clash in nav display
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun popBackStack(popUpTo: KClass<out AppScreen>, inclusive: Boolean) {
        val index = backStack.indexOfLast { popUpTo.isInstance(it) }
        if (index == -1) return
        val keepCount = (if (inclusive) index else index + 1).coerceAtLeast(1)
        while (backStack.size > keepCount) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}
