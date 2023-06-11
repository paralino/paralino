package app.paralino.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.presentation.settings.blocklist.BlockListScreenEvent
import app.paralino.core.presentation.settings.blocklist.BlocklistElementState
import app.paralino.core.presentation.settings.blocklist.BlocklistScreenState
import app.paralino.core.presentation.settings.blocklist.BlocklistVideModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun BlocklistScreen(
    appState: MainAppState,
    vm: BlocklistVideModel = getViewModel()
) {

    val uiState by vm.uiState.collectAsState(BlocklistScreenState())
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }


    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is BlockListScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                null -> { /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.UnblockGroupOrUser -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.removeBlock(dialog.id)
                }
            }
        }
        else -> {}
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.blocklist_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) {
        if (uiState.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.isEmpty) {
            DefaultEmptyState(
                content = stringResource(id = R.string.blocklist_screen_empty_title),
                icon = AppIcons.Block,
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
                        SettingsSectionHeader(name = stringResource(id = R.string.blocklist_screen_title_groups))
                    }
                }
                items(uiState.groups, key = { it.id }) {
                    BlocklistListItem(
                        state = it,
                        onDelete = {
                            showDialog =
                                MainAlertDialog.UnblockGroupOrUser(id = it.id, type = it.type)
                        }
                    )
                }

                if (uiState.users.isNotEmpty()) {
                    item(key = "users") {
                        SettingsSectionHeader(name = stringResource(id = R.string.blocklist_screen_title_users))
                    }
                }
                items(uiState.users, key = { it.id }) {
                    BlocklistListItem(
                        state = it,
                        onDelete = {
                            showDialog =
                                MainAlertDialog.UnblockGroupOrUser(id = it.id, type = it.type)
                        }
                    )
                }
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
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 5.dp, vertical = 5.dp)
    ) {
        val (image, name, action) = createRefs()
        ImageModelImage(
            imageModel = state.image,
            modifier = Modifier
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                }
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
            modifier = Modifier.constrainAs(name) {
                start.linkTo(image.end, margin = 16.dp)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(action.start, margin = 16.dp)
                width = Dimension.fillToConstraints
            }
        )

        IconButton(
            onClick = { if (state.actionLoading != true) onDelete() },
            modifier = Modifier.constrainAs(action) {
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        ) {
            if (state.actionLoading == true) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = AppIcons.Delete,
                    contentDescription = null
                )
            }
        }
    }
}