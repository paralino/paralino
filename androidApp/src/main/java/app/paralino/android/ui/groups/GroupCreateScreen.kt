package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
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
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = null,
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
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
                if (vm.isGroupCreate()) {
                    appState.navController.navigate(AppScreen.MainHome.GroupDetails.route(newGroupId)) {
                        this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                            this.inclusive = false
                        }
                    }
                } else {
                    appState.navController.popBackStack()
                }
            }
        )
    }
}