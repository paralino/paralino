package app.paralino.core.ui.personal_space

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsEvent
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsUiEvent
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PersonalSpaceSettingsScreenCore(
    appState: CoreUiState,
    vm: PersonalSpaceSettingsViewModel,
    openRawDataScreen: () -> Unit,
    openBatteryNotificationsSettings: () -> Unit,
    openLocationHistorySettings: () -> Unit,
    openMap: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)

    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                PersonalSpaceSettingsEvent.OpenMap -> openMap()
            }
        }
    }

    state.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                PersonalSpaceSettingsUiEvent.DialogAction(
                    AlertDialogAction(dialog = dialog, action = it)
                )
            )
        }
    }

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

        SettingsActionItem(
            name = stringResource(MR.strings.settings_personalSpace_disable_action),
            icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
            iconBackgroundTint = AppColors.errorBackground
        ) {
            vm.onEvent(PersonalSpaceSettingsUiEvent.DisablePersonalSpaceClick)
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


