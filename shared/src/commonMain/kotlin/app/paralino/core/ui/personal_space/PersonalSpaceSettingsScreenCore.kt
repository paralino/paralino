package app.paralino.core.ui.personal_space

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PersonalSpaceSettingsScreenCore(
    appState: CoreUiState,
    vm: PersonalSpaceSettingsViewModel,
    openRawDataScreen: () -> Unit,
    openBatteryNotificationsSettings: () -> Unit,
    openLocationHistorySettings: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)

    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 60.dp)
    ) {
        SettingsActionItem(
            name = stringResource(MR.strings.raw_data_screen_title),
            icon = CoreAppIcons.Security.asPainterResource()
        ) {
            openRawDataScreen()
        }

        SettingsActionItem(
            name = stringResource(MR.strings.group_settings_battery_notifications_title),
            icon = CoreAppIcons.NotificationsActive.asPainterResource()
        ) {
            openBatteryNotificationsSettings()
        }

        if (state.hasDevFeatures) {
            SettingsActionItem(
                name = stringResource(MR.strings.history_delete_screen_title),
                icon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                iconBackgroundTint = AppColors.errorBackground
            ) {
                openLocationHistorySettings()
            }
        }
    }

    if (state.loadingAction) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}


