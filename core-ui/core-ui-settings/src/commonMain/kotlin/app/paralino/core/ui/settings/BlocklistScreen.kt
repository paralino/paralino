package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.settings.blocklist.BlockListScreenUiEvent
import app.paralino.core.presentation.settings.blocklist.BlocklistElementState
import app.paralino.core.presentation.settings.blocklist.BlocklistScreenState
import app.paralino.core.presentation.settings.blocklist.BlocklistViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun BlocklistScreenCore(
    appState: CoreUiState,
    vm: BlocklistViewModel,
    paddingValues: PaddingValues
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(BlocklistScreenState())

    when (val dialog = uiState.confirmationDialog) {
        is MainAlertDialog.UnblockGroupOrUser -> {
            MainAlertDialog(
                data = dialog.dialogData,
                onAction = {
                    vm.onEvent(BlockListScreenUiEvent.HideDialog)
                    if (it == MainAlertDialog.Buttons.buttonOk) {
                        vm.onEvent(BlockListScreenUiEvent.UnblockUserOrGroupConfirm(dialog.id))
                    }
                }
            )
        }

        else -> {}
    }

    if (uiState.loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    } else if (uiState.isEmpty) {
        DefaultEmptyState(
            content = stringResource(MR.strings.blocklist_screen_empty_title),
            icon = CoreAppIcons.Block.asPainterResource(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp
            ),
        ) {
            if (uiState.groups.isNotEmpty()) {
                item(key = "groups") {
                    SettingsSectionHeader(name = stringResource(MR.strings.blocklist_screen_title_groups))
                }
            }
            items(uiState.groups, key = { it.id }) {
                BlocklistListItem(
                    state = it,
                    onDelete = {
                        vm.onEvent(
                            BlockListScreenUiEvent.UnblockUserOrGroup(
                                id = it.id,
                                type = it.type
                            )
                        )
                    }
                )
            }

            if (uiState.users.isNotEmpty()) {
                item(key = "users") {
                    SettingsSectionHeader(name = stringResource(MR.strings.blocklist_screen_title_users))
                }
            }
            items(uiState.users, key = { it.id }) {
                BlocklistListItem(
                    state = it,
                    onDelete = {
                        vm.onEvent(
                            BlockListScreenUiEvent.UnblockUserOrGroup(
                                id = it.id,
                                type = it.type
                            )
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun BlocklistListItem(
    modifier: Modifier = Modifier,
    state: BlocklistElementState,
    onDelete: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageModelImage(
            imageModel = state.image,
            modifier = Modifier
                .size(46.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(10.dp)
                )
        )

        Text(
            text = state.name,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp)
        )

        IconButton(
            onClick = { if (state.actionLoading != true) onDelete() },
            modifier = Modifier
        ) {
            if (state.actionLoading == true) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = CoreAppIcons.Delete.asPainterResource(),
                    contentDescription = null
                )
            }
        }
    }
}