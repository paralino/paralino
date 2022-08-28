package app.paralino.android.ui.groups

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.dashboard.GroupItemData

@Composable
fun GroupPickerScreen(
    groups: List<GroupItemData>,
    onClick: (GroupItemData) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(groups.size) {
            val group = groups[it]
            GroupListItem(data = group, onClick = { onClick(group) })
        }
    }
}