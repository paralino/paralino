package app.paralino.core.ui.invite

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.invite.input.MemberInviteScreenEvent
import app.paralino.core.presentation.invite.input.MemberInviteScreenState
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.groups.GroupListItem
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
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
    onMemberAdded: () -> Unit,
    onSkipOnboarding: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                MemberInviteScreenEvent.MemberAdded -> {
                    onMemberAdded()
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    MemberInviteScreen(
        modifier = Modifier.padding(contentPadding),
        state = state,
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
        }
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
) {
    val focusManager = LocalFocusManager.current

    BaseOnboardingScreen(
        modifier = modifier.fillMaxSize(),
        nextButton = @Composable {
            FilledButton(
                modifier = Modifier,
                style = ButtonStyle.BIG,
                state = AppButtonState.loadingStateWithCheck(state.actionLoading),
                icon = if (state is MemberInviteScreenState.Loaded) {
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
                    text = when (state) {
                        is MemberInviteScreenState.Empty -> stringResource(MR.strings.button_next)
                        is MemberInviteScreenState.Loaded -> stringResource(MR.strings.invite_connect_button_addToGroup)
                    }
                )
            }
        },
        skipButton = if (isOnboarding) {
            @Composable {
                OnboardingSkipButton(onSkip = onSkipOnboarding)
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = if (state is MemberInviteScreenState.Empty) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            if (state is MemberInviteScreenState.Loaded) {
                SettingsSectionHeader(name = stringResource(MR.strings.invite_section_title_user))
            } else {
                Text(
                    modifier = Modifier.padding(bottom = 40.dp, top = 20.dp),
                    text = stringResource(MR.strings.invite_connect_title).annotatedBold(),
                    style = MaterialTheme.typography.h6
                )
            }

            MainInputField(
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

            if (state is MemberInviteScreenState.Loaded) {

                Spacer(modifier = Modifier.size(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageModelImage(
                        imageModel = state.userImage,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = state.userName, style = MaterialTheme.typography.h5)
                }

                if (state.group == null && state.canChangeGroup) {
                    Spacer(modifier = Modifier.size(40.dp))
                    FilledButton(
                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                        style = ButtonStyle.SMALL,
                        icon = CoreAppIcons.Group.asPainterResource(),
                        onClick = onPickGroup
                    ) {
                        Text(text = stringResource(MR.strings.invite_connect_button_pickGroup))
                    }
                } else if (state.group != null) {
                    Spacer(modifier = Modifier.size(20.dp))
                    SettingsSectionHeader(name = stringResource(MR.strings.invite_section_title_group))
                    GroupListItem(
                        modifier = Modifier
                            .clickable {
                                if (state.canChangeGroup) onPickGroup()
                            },
                        data = state.group,
                        onEdit = {}
                    )
                }

                Spacer(modifier = Modifier.size(20.dp))

                if (state.group != null) {
                    NoticeInfoChip(
                        modifier = Modifier
                            .padding(vertical = 10.dp),
                        icon = CoreAppIcons.Person.asPainterResource(),
                        text = stringResource(
                            MR.strings.invite_connect_disclaimer_user,
                            state.userName,
                            state.group.name
                        )
                    )
                }
            }
        }
    }
}