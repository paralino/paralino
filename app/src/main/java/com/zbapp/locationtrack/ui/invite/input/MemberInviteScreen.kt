package com.zbapp.locationtrack.ui.invite.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.presentation.invite.input.MemberInviteScreenEvent
import com.zbapp.core.presentation.invite.input.MemberInviteScreenState
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.AppButtonState
import com.zbapp.locationtrack.ui.components.BottomSheetsScreen
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.components.ImageModelImage
import com.zbapp.locationtrack.ui.groups.GroupListItem
import com.zbapp.locationtrack.ui.settings.SettingsSectionHeader
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.annotatedBold
import com.zbapp.locationtrack.utils.extensions.safeCast
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
                vm.loadMember()
            },
            onAddMember = {
                vm.addMember()
            },
            onPickGroup = {
                appState.uiScope.launch {
                    val groups = vm.getAllGroups()
                    appState.showBottomSheetForResult(
                        BottomSheetsScreen.GroupPicker(groups)
                    ).safeCast<GroupItemData>()?.let {
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
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            value = state.code,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions {
                focusManager.clearFocus()
            },
            placeholder = {
                Text(
                    stringResource(id = R.string.invite_connect_code_placeholder),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onValueChange = onCodeChange,
            shape = MaterialTheme.shapes.medium,
            isError = state.hasInputError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface,
                unfocusedBorderColor = MaterialTheme.colors.surface,
            )
        )

        when (state) {
            is MemberInviteScreenState.Empty -> {
                Spacer(modifier = Modifier.size(20.dp))
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
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = state.userName, style = MaterialTheme.typography.h6)
                }

                if (state.group == null && state.canChangeGroup) {
                    Spacer(modifier = Modifier.size(20.dp))
                    FilledButton(
                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                        style = ButtonStyle.SMALL_ACTION,
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
                        style = MaterialTheme.typography.caption
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