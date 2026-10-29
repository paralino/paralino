package app.paralino.android.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.onboarding.GroupJoinWelcomeViewModel
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.ui.onboarding.GroupJoinWelcomeScreenCore
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupJoinWelcomeScreen(
    appState: MainAppState,
    groupId: String,
    vm: GroupJoinWelcomeViewModel = koinViewModel { parametersOf(groupId) }
) {
    BackHandler(enabled = true) {
        // block back until onboarding finishes
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding()
    ) { paddingValues ->
        GroupJoinWelcomeScreenCore(
            coreUiState = appState,
            vm = vm,
            paddingValues = paddingValues,
            openSharing = {
                appState.navController.navigate(
                    AppScreen.MainHome.SharingStart(
                        groupId = groupId,
                        presentation = SharingStartPresentationType.GROUP_JOIN
                    )
                )
            },
            openMap = {
                appState.navController.navigate(AppScreen.MainHome.MainMap(groupId), popUpTo = AppScreen.MainHome.MainMap::class, inclusive = true)
            }
        )
    }
}
