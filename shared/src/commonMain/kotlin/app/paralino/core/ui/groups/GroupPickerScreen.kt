package app.paralino.core.ui.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.billing.PremiumPlanBannerSmall
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GroupBottomSheetPicker(
    modifier: Modifier = Modifier,
    state: GroupBottomSheetPickerState,
    onClick: (GroupPickerAction) -> Unit
) {
    ModalBottomSheetLayout(
        modifier = modifier.fillMaxSize(),
        sheetBackgroundColor = MaterialTheme.colors.background,
        sheetShape = RoundedCornerShape(topStartPercent = 5, topEndPercent = 5),
        sheetState = state.bottomSheetState,
        scrimColor = MaterialTheme.colors.surface.copy(alpha = 0.6f),
        sheetContent = {
            Spacer(modifier = Modifier.height(1.dp))
            GroupPickerScreen(
                state = state.state,
                onClick = onClick
            )
        },
        content = {}
    )
}

class GroupBottomSheetPickerState @OptIn(ExperimentalMaterialApi::class) constructor(
    val bottomSheetState: ModalBottomSheetState
) {
    var state by mutableStateOf<BottomSheetPickerType.GroupPickerBottomSheet?>(null)

    @OptIn(ExperimentalMaterialApi::class)
    val isVisible: Boolean get() = bottomSheetState.isVisible

    @OptIn(ExperimentalMaterialApi::class)
    suspend fun show(state: BottomSheetPickerType.GroupPickerBottomSheet) {
        this.state = state
        bottomSheetState.show()
    }

    @OptIn(ExperimentalMaterialApi::class)
    suspend fun hide() {
        bottomSheetState.hide()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberGroupBottomSheetPickerState(
    initialValue: ModalBottomSheetValue = ModalBottomSheetValue.Hidden
): GroupBottomSheetPickerState {
    return key(initialValue) {
        GroupBottomSheetPickerState(
            bottomSheetState = rememberModalBottomSheetState(
                initialValue = initialValue,
                skipHalfExpanded = true
            )
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupPickerScreen(
    state: BottomSheetPickerType.GroupPickerBottomSheet?,
    onClick: (GroupPickerAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        stickyHeader(key = "title") {
            DefaultToolbar(
                modifier = Modifier
                    .padding(bottom = 10.dp, top = 10.dp)
                    .padding(horizontal = 10.dp),
                elevation = 0.dp,
                titleComposable = {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(MR.strings.title_dashboard),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        if (state != null) {
                            PlanLimitCount(
                                count = state.groups.size,
                                limit = null
                            )
                        }
                    }
                },
                navigationIcon = { ToolbarButtons.Close(onClick = { onClick(GroupPickerAction.Close) }) },
                actions = {}
            )
        }

        if (state != null && state.shouldShowPlanBanner) {
            item(key = "plan_upgrade_banner") {
                PremiumPlanBannerSmall(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 30.dp),
                    description = stringResource(MR.strings.plans_banner_inline_groups_picker),
                    onClick = { onClick(GroupPickerAction.OpenSubscriptions) }
                )
            }
        }

        if (state != null) {
            items(state.groups.size, key = { state.groups.get(it).id }) {
                val group = state.groups[it]
                GroupListItem(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    data = group,
                    onClick = { onClick(GroupPickerAction.GroupPicked(group)) },
                    onEdit = { onClick(GroupPickerAction.GroupEditPicked(group)) }
                )
            }
        }

        if (state?.onlyShowGroups == false) {
            item(key = "new_group") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(horizontal = 40.dp)
                        .padding(top = 20.dp, bottom = 4.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colors.surface)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    FilledButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        style = ButtonStyle.BIG,
                        icon = CoreAppIcons.Add.asPainterResource(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        onClick = { onClick(GroupPickerAction.NewGroup) },
                    ) {
                        Text(stringResource(MR.strings.group_picker_button_create))
                    }
                    FilledButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        style = ButtonStyle.BIG,
                        icon = CoreAppIcons.Key.asPainterResource(),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                        onClick = { onClick(GroupPickerAction.JoinGroup) }
                    ) {
                        Text(stringResource(MR.strings.group_picker_button_join))
                    }
                }
            }
        }
    }
}

sealed interface GroupPickerAction {
    object NewGroup : GroupPickerAction
    object JoinGroup : GroupPickerAction
    object Close : GroupPickerAction
    object OpenSubscriptions : GroupPickerAction
    data class GroupPicked(val group: GroupItemState) : GroupPickerAction
    data class GroupEditPicked(val group: GroupItemState) : GroupPickerAction
}