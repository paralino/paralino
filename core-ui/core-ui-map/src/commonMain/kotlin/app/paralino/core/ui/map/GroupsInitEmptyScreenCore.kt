package app.paralino.core.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.map.GroupsInitEmptyScreenState
import app.paralino.core.presentation.map.GroupsInitEmptyScreenUiEvent
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.invite.manage.PersonalCodeItem
import app.paralino.core.ui.invite.manage.PersonalCodeItemWithHints
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun GroupsInitEmptyScreenContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: GroupsInitEmptyScreenState,
    onEvent: (GroupsInitEmptyScreenUiEvent) -> Unit,
) {

    state.alertDialog?.let { dialog ->
        dialog.Show { action ->
            onEvent(
                GroupsInitEmptyScreenUiEvent.DialogAction(
                    AlertDialogAction(dialog = dialog, action = action)
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(paddingValues)
    ) {

        if (state.errors.isNotEmpty())
            Spacer(modifier = Modifier.size(20.dp))

        state.errors.forEach {
            ConditionErrorItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                data = it
            ) {
                onEvent(GroupsInitEmptyScreenUiEvent.ErrorItemAction(it))
            }
        }

        Spacer(modifier = Modifier.size(20.dp))

        val personalCode = state.personalCode
        if (personalCode != null) {
            PersonalCodeItemWithHints {
                PersonalCodeItem(
                    modifier = this,
                    state = personalCode,
                    onCopy = {
                        onEvent(GroupsInitEmptyScreenUiEvent.CopyPersonalCode)
                    },
                    onShare = { onEvent(GroupsInitEmptyScreenUiEvent.SharePersonalCode(it)) },
                    onDelete = {}
                )
            }
        } else {
            LargeSeekbarSwitch(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .height(LargeSwitchDefaults.minHeight)
                    .fillMaxWidth(1f),
                icon = CoreAppIcons.AddLink.asPainterResource(),
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
                    onEvent(GroupsInitEmptyScreenUiEvent.OpenPersonalCodes)
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
                onEvent(GroupsInitEmptyScreenUiEvent.TryCreateGroup)
            }
        )

        Spacer(modifier = Modifier.size(80.dp))
    }
}