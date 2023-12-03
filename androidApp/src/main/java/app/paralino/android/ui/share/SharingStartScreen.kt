package app.paralino.android.ui.share

import androidx.compose.runtime.Composable
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.share.SharingStartScreenCore
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SharingStartScreen(
    appState: MainAppState,
    groupId: String?,
    vm: SharingStartViewModel = getViewModel { parametersOf(groupId) },
) {

    val permissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    SharingStartScreenCore(
        appState = appState,
        groupId = groupId,
        vm = vm,
        onNavigateBack = { appState.navController.popBackStack() },
        onPickGroup = {
            appState.uiScope.launch {
                appState.showBottomSheetForResult(it).safeCast<GroupItemState>()?.let {
                    vm.setSelectedGroup(it.id)
                }
            }
        },
        askNotificationPermission = { },
        askLocationService = { permissionHandler.askLocationService() },
        askLocationPermission = { permissionHandler.askLocationPermission() },
        askBackgroundLocationPermission = { permissionHandler.askBackgroundLocationPermission() }
    )
}