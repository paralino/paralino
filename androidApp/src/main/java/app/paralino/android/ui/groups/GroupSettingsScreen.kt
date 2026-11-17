package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.resources.moko.*
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.groups.GroupSettingsScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupSettingsScreen(
    appState: MainAppState,
    groupId: String?,
    vm: GroupSettingsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val groupName by vm.groupNameState.collectAsState(initial = null)
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
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
                appState.navController.navigate(AppScreen.MainHome.GroupCreate(vm.groupId))
            },
            openBatteryNotificationsSettings = {
                appState.navController.navigate(
                    AppScreen.MainHome.BatteryNotificationSettings(groupId = vm.groupId)
                )
            },
            onGroupDeleted = {
                appState.navController.navigate(AppScreen.MainHome.MainMap(), popUpTo = AppScreen.MainHome.MainMap::class, inclusive = true)
            },
            openRawDataScreen = {
                appState.navController.navigate(
                    AppScreen.Common.RawDataScreen(
                        RawPresentationDataType.GroupPresentation(vm.groupId)
                    )
                )
            },
            openLocationHistorySettings = {
                appState.navController.navigate(
                    AppScreen.MainHome.LocationHistoryDelete(groupId = vm.groupId)
                )
            }
        )
    }
}