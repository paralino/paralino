package app.paralino.core.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.groups.settings.GroupSettingsScreenEvent
import app.paralino.core.presentation.groups.settings.GroupSettingsUiEvent
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ComponentInfoBox
import app.paralino.core.ui.components.ComponentInfoPoint
import app.paralino.core.ui.components.MembershipPlanFeatureTable
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupSettingsScreenCore(
    appState: CoreUiState,
    vm: GroupSettingsViewModel,
    openBatteryNotificationsSettings: () -> Unit,
    openEditGroup: () -> Unit,
    onGroupDeleted: () -> Unit,
    openRawDataScreen: () -> Unit,
    openLocationHistorySettings: () -> Unit
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Resource.NotInitialized)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                GroupSettingsScreenEvent.GroupDeleted -> onGroupDeleted()
                null -> {}
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(GroupSettingsUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    when (val state = uiState) {
        Resource.NotInitialized -> {}

        is Resource.Ready -> {
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
                if (state.data.canEdit) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_edit_screen_title),
                        icon = CoreAppIcons.Edit.asPainterResource()
                    ) {
                        openEditGroup()
                    }

//                    SettingsActionItem(
//                        modifier = Modifier.alpha(if (state.data.isMemberVerificationEnabled) 1f else 0.5f),
//                        name = stringResource(MR.strings.group_settings_verification_title),
//                        description = if (state.data.isMemberVerificationOn) {
//                            stringResource(MR.strings.group_settings_verification_description_on)
//                        } else {
//                            stringResource(MR.strings.group_settings_verification_description_off)
//                        },
//                        icon = CoreAppIcons.Security.asPainterResource(),
//                        type = SettingsActionItemType.Switch(checked = state.data.isMemberVerificationOn)
//                    ) {
//                        vm.onEvent(GroupSettingsUiEvent.ToggleMemberVerification)
//                    }

                    if (state.data.hasDevFeatures) {
                        SettingsActionItem(
                            name = stringResource(MR.strings.history_delete_screen_title),
                            icon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                            iconBackgroundTint = AppColors.errorBackground
                        ) {
                            openLocationHistorySettings()
                        }
                    }

                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_delete),
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.DeleteGroup)
                    }
                } else {
                    if (state.data.hasDevFeatures) {
                        SettingsActionItem(
                            name = stringResource(MR.strings.history_delete_screen_title),
                            icon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                            iconBackgroundTint = AppColors.errorBackground
                        ) {
                            openLocationHistorySettings()
                        }
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_block),
                        icon = CoreAppIcons.Block.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.BlockGroup)
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_leave),
                        icon = CoreAppIcons.Logout.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.LeaveGroup)
                    }
                }

                if (state.data.canEdit) {
                    Divider(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 20.dp)
                            .padding(horizontal = 30.dp),
                        color = Color.Gray.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 4.dp, top = 20.dp),
                        text = stringResource(
                            MR.strings.paywall_current_plan_label,
                            state.data.planName
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
                    )

                    MembershipPlanFeatureTable(
                        modifier = Modifier,
                        table = state.data.featureTableState,
                        canExpand = false,
                        isExpanded = true,
                        onExpandPlans = {},
                        onCellClick = { table, cell ->
                            vm.onEvent(
                                GroupSettingsUiEvent.OnCellItemClick(table = table, cellItem = cell)
                            )
                        }
                    )
                }

                GroupInfoBox(
                    groupId = state.data.id.takeIf { state.data.canEdit },
                    memberId = state.data.memberId,
                    locationId = state.data.locationId,
                    createdAtDateFormatted = state.data.createdAtDateFormatted,
                    memberSinceDateFormatted = state.data.memberSinceDateFormatted,
                    onContentCopy = { vm.onEvent(GroupSettingsUiEvent.Copy(it)) }
                )
            }

            if (state.data.loadingAction) {
                LoadingDialog(
                    text = stringResource(MR.strings.login_step_generic_loading),
                    progress = null
                )
            }
        }
    }
}

@Composable
private fun GroupInfoBox(
    groupId: String?,
    memberId: String?,
    locationId: String?,
    createdAtDateFormatted: String?,
    memberSinceDateFormatted: String?,
    onContentCopy: (String) -> Unit
) {
    val items = listOfNotNull(
        groupId?.let { id ->
            ComponentInfoPoint(
                content = stringResource(MR.strings.group_settings_info_group_id, id)
            )
        },
        memberId?.let { id ->
            ComponentInfoPoint(
                content = stringResource(MR.strings.group_settings_info_member_id, id)
            )
        },
        locationId?.let { id ->
            ComponentInfoPoint(
                content = stringResource(MR.strings.group_settings_info_location_id, id)
            )
        },
    )
    val footerInfoText = createdAtDateFormatted?.let {
        stringResource(MR.strings.group_settings_info_created_at, it)
    } ?: memberSinceDateFormatted?.let {
        stringResource(MR.strings.group_settings_info_member_since, it)
    }

    if (items.isEmpty() && footerInfoText.isNullOrBlank()) {
        return
    }

    Divider(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 32.dp)
            .padding(horizontal = 30.dp),
        color = Color.Gray.copy(alpha = 0.2f),
        thickness = 0.5.dp
    )

    ComponentInfoBox(
        modifier = Modifier.padding(top = 30.dp),
        items = items,
        footerInfoText = footerInfoText,
        onCopy = onContentCopy,
    )
}