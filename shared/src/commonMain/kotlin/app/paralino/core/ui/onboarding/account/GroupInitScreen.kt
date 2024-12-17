package app.paralino.core.ui.onboarding.account

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.GroupInitScreenEvent
import app.paralino.core.presentation.groups.GroupInitScreenState
import app.paralino.core.presentation.groups.GroupInitScreenUiEvent
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.invite.manage.PersonalCodeItem
import app.paralino.core.ui.invite.manage.PersonalCodeItemWithHints
import app.paralino.core.utils.ConditionErrorType
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest


@Composable
fun GroupInitScreenCore(
    appState: CoreUiState,
    vm: GroupInitViewModel,
    openSettings: () -> Unit,
    onCreateGroup: () -> Unit,
    openFreePlanInfo: () -> Unit,
    openPersonalCodes: () -> Unit,
    onSharePersonalCode: (String) -> Unit,
    askNotificationPermission: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                GroupInitScreenEvent.AskNotificationPermission -> askNotificationPermission()
                is GroupInitScreenEvent.SharePersonalCode -> onSharePersonalCode(event.code)
                GroupInitScreenEvent.OpenPersonalCodes -> openPersonalCodes()
                GroupInitScreenEvent.OpenGroupCreate -> onCreateGroup()

                null -> {
                    /* do nothing */
                }

            }
        }
    }

    when (val dialog = state.data?.alertDialog) {
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

    when (val finalState = state) {
        Resource.NotInitialized -> {}
        is Resource.Ready -> {
            when (val finalStep = finalState.data.onboardingStep) {
                is AccountOnboardingContent -> {
                    AccountOnboardingScreen(
                        coreUiState = appState,
                        step = finalStep,
                        onEvent = {
                            vm.onEvent(GroupInitScreenUiEvent.OnboardingStep(event = it))
                        }
                    )
                }
                null -> {
                    GroupInitScreenFinal(
                        state = finalState.data,
                        vm = vm,
                        openSettings = openSettings,
                        onCreateGroup = onCreateGroup,
                        openFreePlanInfo = openFreePlanInfo,
                        openPersonalCodes = openPersonalCodes,
                        onSharePersonalCode = onSharePersonalCode,
                        askNotificationPermission = askNotificationPermission
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupInitScreenFinal(
    state: GroupInitScreenState,
    vm: GroupInitViewModel,
    openSettings: () -> Unit,
    onCreateGroup: () -> Unit,
    openFreePlanInfo: () -> Unit,
    openPersonalCodes: () -> Unit,
    onSharePersonalCode: (String) -> Unit,
    askNotificationPermission: () -> Unit
) {
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
                        is ConditionErrorType.EmailVerification -> {
                            vm.onEvent(GroupInitScreenUiEvent.SendEmailVerification)
                        }

                        ConditionErrorType.Network -> {
                            vm.showSnackbar(
                                text = MR.strings.error_no_connection_snackbar.localizedString()
                            )
                        }

                        ConditionErrorType.PermissionNotifications -> {
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
                PersonalCodeItemWithHints {
                    PersonalCodeItem(
                        modifier = this,
                        state = state.personalCode!!,
                        onCopy = {
                            vm.onEvent(GroupInitScreenUiEvent.CopyPersonalCode)
                        },
                        onShare = onSharePersonalCode,
                        onDelete = {}
                    )
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
                    if (vm.shouldShowFreePlanNoticeScreen) {
                        openFreePlanInfo()
                    } else {
                        onCreateGroup()
                    }
                }
            )

            Spacer(modifier = Modifier.size(80.dp))
        }
    }
}