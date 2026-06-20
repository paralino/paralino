package app.paralino.core.ui.invite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.groups.add_member.MemberInviteScreenEvent
import app.paralino.core.presentation.groups.add_member.MemberInviteScreenState
import app.paralino.core.presentation.groups.add_member.MemberInviteScreenUiEvent
import app.paralino.core.presentation.groups.add_member.MemberInviteViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.PlanLimitSmallUpgradeButton
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.GroupListItem
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MemberInviteScreenCore(
    appState: CoreUiState,
    vm: MemberInviteViewModel,
    isOnboarding: Boolean,
    contentPadding: PaddingValues,
    onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
    openPaywall: (BillingScreenSourceType) -> Unit,
    onMemberAdded: () -> Unit,
    onSkipOnboarding: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                MemberInviteScreenEvent.MemberAdded -> onMemberAdded()
                is MemberInviteScreenEvent.OpenPaywall -> openPaywall(event.type)

                null -> {
                    /* do nothing */
                }

                is MemberInviteScreenEvent.ShowGroupPicker -> {
                   onPickGroup(event.sheetState)
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show { action ->
            vm.onEvent(
                MemberInviteScreenUiEvent.DialogAction(
                    AlertDialogAction(dialog = dialog, action = action)
                )
            )
        }
    }

    MemberInviteScreen(
        modifier = Modifier.padding(contentPadding),
        state = uiState,
        isOnboarding = isOnboarding,
        onCodeChange = {
            vm.codeChanged(it)
        },
        onConfirm = {
            vm.addOrLoadMember()
        },
        onSkipOnboarding = onSkipOnboarding,
        onPickGroup = {
            appState.uiScope.launch {
                val groupPickerState = vm.getGroupPickerState()
                onPickGroup(groupPickerState)
            }
        },
        openPaywall = openPaywall
    )
}

@Composable
private fun MemberInviteScreen(
    modifier: Modifier = Modifier,
    state: MemberInviteScreenState,
    isOnboarding: Boolean,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onPickGroup: () -> Unit,
    openPaywall: (BillingScreenSourceType) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    BaseOnboardingScreen(
        modifier = modifier.fillMaxSize(),
        nextButton = @Composable {
            FilledButton(
                modifier = Modifier,
                style = ButtonStyle.BIG,
                state = AppButtonState.loadingStateWithCheck(state.actionLoading),
                enabled = state.isNextButtonEnabled,
                icon = if (state.isLoaded && state.group != null) {
                    CoreAppIcons.PersonAdd.asPainterResource()
                } else {
                    null
                },
                onClick = {
                    focusManager.clearFocus()
                    onConfirm()
                }
            ) {
                Text(
                    text = if (state.isLoaded && state.group != null) {
                        stringResource(MR.strings.invite_connect_button_addToGroup)
                    } else {
                        stringResource(MR.strings.button_next)
                    }
                )
            }
        },
        skipButton = if (isOnboarding) {
            @Composable {
                OnboardingSkipButton(onSkip = onSkipOnboarding)
            }
        }  else if (state.shouldUpgradePlan) {
            @Composable {
                PlanLimitSmallUpgradeButton(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(MR.strings.invite_plan_limit_chip_owner),
                    onClick = { openPaywall(BillingScreenSourceType.UPGRADE_DEFAULT) }
                )
            }
        } else {
            null
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(bottom = 20.dp),
            verticalArrangement = if (!state.isLoaded || state.group == null) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val loadedState = state.loadedState
            val group = state.group
            val premiumStatus = state.premiumStatus

            if (loadedState != null) {
                if (group != null) {
                    Spacer(modifier = Modifier.size(20.dp))
//                    SettingsSectionHeader(name = stringResource(MR.strings.invite_section_title_group))
                    GroupListItem(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clickable {
                                if (loadedState.canChangeGroup) {
                                    onPickGroup()
                                }
                            },
                        data = group,
                        onEdit = {}
                    )

                    if (premiumStatus != null && state.members.isNotEmpty()) {
                        SettingsSectionHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            name = stringResource(MR.strings.group_details_section_members_title),
                            count = premiumStatus.payload.count,
                            limit = premiumStatus.payload.limit
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            this.items(state.members, key = { it.id }) { item ->
                                DataInfoChip(
                                    modifier = Modifier,
                                    image = item.image,
                                    text = item.name
                                )
                            }
                        }
                    }
                }
            }

                if (state.isLoaded) {
                    SettingsSectionHeader(
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp),
                        name = stringResource(MR.strings.invite_section_title_user)
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .padding(bottom = 40.dp, top = 20.dp)
                            .padding(horizontal = 20.dp),
                        text = stringResource(MR.strings.invite_connect_title).annotatedBoldCompose(),
                        style = MaterialTheme.typography.h6
                    )
                }

                MainInputField(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    text = state.code,
                    textStyle = MaterialTheme.typography.h4.copy(
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    hint = stringResource(MR.strings.invite_connect_code_placeholder),
                    hasError = state.hasInputError,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    focusManager = focusManager,
                    onInputChange = onCodeChange
                )

            if (loadedState != null) {
                Spacer(modifier = Modifier.size(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageModelImage(
                        imageModel = loadedState.userImage,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = loadedState.userName, style = MaterialTheme.typography.h5)
                }

                Spacer(modifier = Modifier.size(20.dp))

                if (group != null) {
                    NoticeInfoChip(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .padding(horizontal = 20.dp),
                        icon = CoreAppIcons.Person.asPainterResource(),
                        text = stringResource(
                            MR.strings.invite_connect_disclaimer_user,
                            loadedState.userName,
                            group.name
                        )
                    )
                }
            }
        }
    }
}