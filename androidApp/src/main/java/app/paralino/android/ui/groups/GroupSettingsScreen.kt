package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.GroupSettingsViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.groups.GroupSettingsScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupSettingsScreen(
    appState: MainAppState,
    groupId: String?,
    vm: GroupSettingsViewModel = getViewModel { parametersOf(groupId) },
) {
    val groupName by vm.uiState.map { it.data?.name }.collectAsState(initial = null)
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = if (groupName != null) groupName else stringResource(MR.strings.title_settings),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        GroupSettingsScreenCore(
            appState = appState,
            vm = vm,
            openEditGroup = {
                appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(vm.groupId))
            },
            onGroupDeleted = {
                appState.navController.navigate(AppScreen.MainHome.MainMap.route()) {
                    popUpTo(AppScreen.MainHome.MainMap.routeSignature) { inclusive = true }
                    this.launchSingleTop = true
                }
            },
            openRawDataScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.RawDataScreen.route(
                        RawPresentationDataType.GroupPresentation(vm.groupId)
                    )
                )
            }
        )
    }
}