package app.paralino.android.ui.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.core.presentation.groups.GroupItemState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupPickerScreen(
    groups: List<GroupItemState>,
    onClose: () -> Unit,
    onClick: (GroupPickerAction) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        stickyHeader(key = "title") {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .fillMaxWidth()
                    .padding(bottom = 10.dp, top = 10.dp)
                    .padding(horizontal = 10.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = AppIcons.Close,
                        contentDescription = null
                    )
                }
                Text(
                    text = stringResource(id = R.string.title_dashboard), modifier = Modifier
                        .align(
                            Alignment.Center
                        )
                        .fillMaxWidth(1f), textAlign = TextAlign.Center
                )
            }
        }
        items(groups.size, key = { groups.get(it).id }) {
            val group = groups[it]
            GroupListItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                data = group,
                onClick = { onClick(GroupPickerAction.GroupPicked(group)) })
        }
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
                    icon = AppIcons.Add,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    onClick = { onClick(GroupPickerAction.NewGroup) },
                ) {
                    Text(stringResource(R.string.group_picker_button_create))
                }
                FilledButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    style = ButtonStyle.BIG,
                    icon = AppIcons.Key,
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                    onClick = { onClick(GroupPickerAction.JoinGroup) }
                ) {
                    Text(stringResource(R.string.group_picker_button_join))
                }
            }
        }
    }
}

sealed interface GroupPickerAction {
    object NewGroup : GroupPickerAction
    object JoinGroup : GroupPickerAction
    data class GroupPicked(val group: GroupItemState) : GroupPickerAction
}