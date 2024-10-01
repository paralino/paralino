package app.paralino.core.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupInitScreenUiEvent
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.invite.manage.PersonalCodeItem
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun GroupInitScreenCore(
    appState: CoreUiState,
    vm: GroupInitViewModel,
    openSettings: () -> Unit,
    onCreateGroup: () -> Unit,
    openPersonalCodes: () -> Unit,
    onSharePersonalCode: (String) -> Unit,
    askNotificationPermission: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    when (val dialog = state.alertDialog) {
        is MainAlertDialog.VerifyEmail -> {
            dialog.Show {
                vm.onEvent(GroupInitScreenUiEvent.HideDialog)
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.onEvent(GroupInitScreenUiEvent.ConfirmSendEmailVerification)
                }
            }
        }

        else -> {}
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.group_create_start_title),
                navigationIcon = null,
                actions = {
                    ImageModelImage(
                        imageModel = state.userImage,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { openSettings() }
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(it)
        ) {

            if (state.errors.isNotEmpty())
                Spacer(modifier = Modifier.size(20.dp))

            state.errors.forEach {
                ConditionErrorItem(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    data = it
                ) {
                    when (it) {
                        is ConditionErrorType.EMAIL_VERIFICATION -> {
                            vm.onEvent(GroupInitScreenUiEvent.SendEmailVerification)
                        }

                        ConditionErrorType.NETWORK -> {
                            vm.showSnackbar(
                                text = MR.strings.error_no_connection_snackbar.localizedString()
                            )
                        }

                        ConditionErrorType.PERMISSION_NOTIFICATIONS -> {
                            askNotificationPermission()
                        }

                        else -> {
                            /* do nothing */
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(20.dp))

            if (state.personalCode != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(MR.strings.dashboard_paralino_id_title).annotatedBold(),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    DataInfoChip(
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp),
                        image = null,
                        backgroundColor = AppColors.accentLight.copy(alpha = 0.3f),
                        text = stringResource(MR.strings.dashboard_paralino_id_description)
                    )
                    Box(
                        modifier = Modifier.padding(top = 8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        PersonalCodeItem(
                            modifier = Modifier.padding(top = 40.dp),
                            state = state.personalCode!!,
                            onCopy = {
                                vm.onEvent(GroupInitScreenUiEvent.CopyPersonalCode)
                                vm.showSnackbar(MR.strings.toast_copy_invite_code.localizedString())
                            },
                            onShare = onSharePersonalCode,
                            onDelete = {}
                        )
                        Icon(
                            modifier = Modifier.height(70.dp).rotate(-10f),
                            painter = CoreAppIcons.ArrowSpinny.asPainterResource(),
                            tint = AppColors.highlightGold,
                            contentDescription = null
                        )
                    }
                }
            } else {
                LargeSeekbarSwitch(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .height(LargeSwitchDefaults.minHeight)
                        .fillMaxWidth(1f),
                    icon = CoreAppIcons.Key.asPainterResource(),
                    title = stringResource(MR.strings.dashboard_groups_join_button),
                    titleStyle = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal),
                    description = null,
                    state = LargeSeekbarSwitchState.SeekbarSwitchState(
                        switchState = LargeSeekbarSwitchState.Switch(
                            checkState = false,
                            unCheckedIconRes = CoreAppIcons.ChevronRight
                        ),
                        seekbarState = null
                    ),
                    onStateChange = {
                        openPersonalCodes()
                    }
                )
                Spacer(modifier = Modifier.size(20.dp))
            }

            if (state.personalCode != null) {
                SettingsSectionHeader(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    name = stringResource(MR.strings.dashboard_groups_create_info)
                )
            }

            LargeSeekbarSwitch(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .height(LargeSwitchDefaults.minHeight)
                    .fillMaxWidth(1f),
                icon = CoreAppIcons.Add.asPainterResource(),
                title = stringResource(MR.strings.dashboard_groups_add_button),
                titleStyle = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onCreateGroup()
                }
            )

            Spacer(modifier = Modifier.size(80.dp))
        }
    }
}
