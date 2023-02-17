package app.paralino.android.ui.invite.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.presentation.invite.input.MemberInviteScreenEvent
import app.paralino.core.presentation.invite.input.MemberInviteScreenState
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.*
import app.paralino.android.ui.groups.GroupListItem
import app.paralino.android.ui.settings.SettingsSectionHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.android.utils.extensions.safeCast
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MemberInviteScreen(
    appState: MainAppState,
    groupId: String?,
    code: String?,
    vm: MemberInviteViewModel = getViewModel { parametersOf(groupId, code) },
) {

    val state by vm.uiState.collectAsState(initial = MemberInviteScreenState.Empty())

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is MemberInviteScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                MemberInviteScreenEvent.ErrorWrongCode -> {
                    appState.showSnackbar(context.getString(R.string.invite_connect_alert_wrongCode))
                }
                MemberInviteScreenEvent.ErrorMissingGroup -> {
                    appState.showSnackbar(context.getString(R.string.invite_connect_alert_pickGroup))
                }
                MemberInviteScreenEvent.MemberAdded -> {
                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.group_details_add_member_button)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        MemberInviteScreen(
            modifier = Modifier.padding(it),
            state = state,
            onCodeChange = {
                vm.codeChanged(it)
            },
            onLoadCode = {
                vm.addOrLoadMember()
            },
            onAddMember = {
                vm.addOrLoadMember()
            },
            onPickGroup = {
                appState.uiScope.launch {
                    val groups = vm.getAllGroups()
                    appState.showBottomSheetForResult(
                        BottomSheetsScreen.GroupPicker(groups)
                    ).safeCast<GroupItemState>()?.let {
                        vm.selectGroup(it)
                    }
                }
            }
        )
    }

}

@Composable
fun MemberInviteScreen(
    modifier: Modifier = Modifier,
    state: MemberInviteScreenState,
    onCodeChange: (String) -> Unit,
    onLoadCode: () -> Unit,
    onAddMember: () -> Unit,
    onPickGroup: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(20.dp),
        verticalArrangement = if (state is MemberInviteScreenState.Empty) Arrangement.Center else Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        if (state is MemberInviteScreenState.Loaded) {
            SettingsSectionHeader(name = stringResource(id = R.string.invite_section_title_user))
        } else {
            Text(
                modifier = Modifier.padding(bottom = 40.dp),
                text = stringResource(id = R.string.invite_connect_title).annotatedBold(),
                style = MaterialTheme.typography.h6
            )
        }

        MainInputField(
            text = state.code,
            hint = stringResource(id = R.string.invite_connect_code_placeholder),
            hasError = state.hasInputError,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            focusManager = focusManager,
            onInputChange = onCodeChange
        )

        when (state) {
            is MemberInviteScreenState.Empty -> {
                Spacer(modifier = Modifier.size(40.dp))
                FilledButton(
                    style = ButtonStyle.BIG,
                    state = AppButtonState.loadingStateWithCheck(state.actionLoading),
                    icon = null,
                    onClick = {
                        focusManager.clearFocus()
                        onLoadCode()
                    }
                ) {
                    Text(text = stringResource(id = R.string.button_next))
                }
            }
            is MemberInviteScreenState.Loaded -> {

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
                        icon = Icons.Rounded.Group,
                        onClick = onPickGroup
                    ) {
                        Text(text = stringResource(id = R.string.invite_connect_button_pickGroup))
                    }
                } else if (state.group != null) {
                    Spacer(modifier = Modifier.size(20.dp))
                    SettingsSectionHeader(name = stringResource(id = R.string.invite_section_title_group))
                    GroupListItem(data = state.group!!) {
                        if (state.canChangeGroup) onPickGroup()
                    }
                }

                Spacer(modifier = Modifier.size(20.dp))

                if (state.group != null) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colors.error)
                            .padding(10.dp),
                        text = stringResource(
                            id = R.string.invite_connect_disclaimer_user,
                            state.userName,
                            state.group!!.name
                        ).annotatedBold(),
                        style = MaterialTheme.typography.body2
                    )
                }

                Spacer(modifier = Modifier.size(40.dp))

                FilledButton(
                    style = ButtonStyle.BIG,
                    state = AppButtonState.loadingStateWithCheck(state.actionLoading),
                    icon = Icons.Rounded.PersonAdd,
                    onClick = onAddMember
                ) {
                    Text(text = stringResource(id = R.string.invite_connect_button_addToGroup))
                }
            }
        }

    }
}