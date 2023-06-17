package app.paralino.core.ui.login

import androidx.compose.foundation.clickable
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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupInitScreenEvent
import app.paralino.core.presentation.groups.GroupInitScreenState
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.invite.manage.PersonalCodeItem
import app.paralino.core.ui.rememberCoreUiState
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest


@Composable
fun GroupInitScreenCore(
    appState: CoreUiState = rememberCoreUiState(),
    vm: GroupInitViewModel,
    onShowDialog: (MainAlertDialog?) -> Unit,
    openSettings: () -> Unit,
    onCreateGroup: () -> Unit,
    openPersonalCodes: () -> Unit,
    onSharePersonalCode: (String) -> Unit,
    askNotificationPermission: () -> Unit,
    onClose: () -> Unit,
) {

    val vmState by vm.uiState.collectAsState(GroupInitScreenState())
    val state = vmState

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is GroupInitScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString())
                }

                GroupInitScreenEvent.EmailVerificationSent -> {
                    appState.showSnackbar(MR.strings.info_email_check_instructions.localizedString())
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
                            contentDescription = null
                        )
                    }
                },
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
                            onShowDialog(MainAlertDialog.VerifyEmail(it.email))
                        }

                        ConditionErrorType.NETWORK -> {
                            appState.showSnackbar(
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

            SettingsBigHeader(
                modifier = Modifier
                    .padding(horizontal = 20.dp),
                name = stringResource(MR.strings.group_create_start_title)
            )

            Spacer(modifier = Modifier.size(20.dp))

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

            Spacer(modifier = Modifier.size(20.dp))

            if (state.personalCode != null) {
                SettingsSectionHeader(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    name = stringResource(MR.strings.dashboard_groups_join_info)
                )
                PersonalCodeItem(
                    state = state.personalCode,
                    deleteLoading = false,
                    onCopy = {
                        vm.copyToClipboard()
                        appState.showSnackbar(MR.strings.toast_copy_invite_code.localizedString())
                    },
                    onShare = {
                        onSharePersonalCode(state.personalCode.code)
                    },
                    onDelete = {}
                )
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
            }

            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}
