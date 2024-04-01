package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupCreateScreen(
    appState: MainAppState,
    groupId: String?,
    vm: CreateGroupViewModel = getViewModel { parametersOf(groupId) },
) {
    GroupCreateScreenCore(
        appState = appState,
        vm = vm,
        onBottomSheetAction = {
            appState.uiScope.launch {
                appState.showBottomSheetForResult(it)
                    .safeCast<ImageModel>()?.let {
                        vm.updateImage(it)
                    }
            }
        },
        groupUpdated = { newGroupId ->
            if (groupId.isNullOrBlank()) {
                appState.navController.navigate(AppScreen.MainHome.GroupDetails.route(newGroupId)) {
                    this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                        this.inclusive = false
                    }
                }
            } else {
                appState.navController.popBackStack()
            }
        },
        onExit = {
            appState.navController.popBackStack()
        }
    )
}