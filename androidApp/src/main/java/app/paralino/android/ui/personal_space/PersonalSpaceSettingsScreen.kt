package app.paralino.android.ui.personal_space

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.personal_space.PersonalSpaceSettingsScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PersonalSpaceSettingsScreen(
    appState: MainAppState,
    vm: PersonalSpaceSettingsViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.personalSpace_spaceTitle),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        PersonalSpaceSettingsScreenCore(
            appState = appState,
            vm = vm,
            openRawDataScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.RawDataScreen.route(
                        RawPresentationDataType.PersonalSpacePresentation
                    )
                )
            },
            openBatteryNotificationsSettings = {
                // Reuse group battery notifications with personal space group id
                appState.navController.navigate(
                    AppScreen.MainHome.BatteryNotificationSettings.route(UserGroup.groupPersonalDevicesId)
                )
            },
            openLocationHistorySettings = {
                appState.navController.navigate(
                    AppScreen.MainHome.LocationHistoryDelete.route(groupId = UserGroup.groupPersonalDevicesId)
                )
            }
        )
    }
}


