package app.paralino.core.presentation.places.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlaceDetailScreenCore(
    appState: CoreUiState,
    vm: PlaceDetailViewModel,
    userScrollEnabled: Boolean,
    onNavigateBack: () -> Unit,
    openPlaceRawDataScreen: () -> Unit,
    openPlaceEditScreen: () -> Unit,
    mapContent: @Composable (PlaceDetailScreenState) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState(initial = Payload.Loading)
    var optionMenuExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlaceDetailScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
                }

                is PlaceDetailScreenEvent.PlaceDeleted -> {
                    onNavigateBack()
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.DeletePlace -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.deletePlace()
            }
        }

        else -> {}
    }


    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = {
                    Text(state.data?.groupName ?: "")
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onNavigateBack() }
                    ) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            openPlaceRawDataScreen()
                        }
                    ) {
                        Icon(
                            painter = CoreAppIcons.Security.asPainterResource(),
                            tint = AppColors.accent,
                            contentDescription = null
                        )
                    }
                    if (state.data?.canEdit == true) {
                        IconButton(
                            onClick = {
                                openPlaceEditScreen()
                            }
                        ) {
                            Icon(
                                painter = CoreAppIcons.Edit.asPainterResource(),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { optionMenuExpanded = true }) {
                            Icon(
                                painter = CoreAppIcons.MoreVert.asPainterResource(),
                                contentDescription = null
                            )
                        }

                        DropdownMenu(
                            expanded = optionMenuExpanded,
                            onDismissRequest = { optionMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    optionMenuExpanded = false
                                    showDialog = MainAlertDialog.DeletePlace
                                }
                            ) {
                                Text(text = stringResource(MR.strings.place_list_menu_delete))
                            }
                        }
                    }
                }
            )
        }
    ) {

        when (val finalState = state) {
            Payload.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            is Payload.Error -> {
                DefaultEmptyState(content = stringResource(MR.strings.error_load_data_generic))
            }

            is Payload.Success -> {
                PlaceDetailScreen(
                    state = finalState.data,
                    paddingValues = it,
                    onEnterChange = {
                        vm.toggleEnterSettings(it)
                    },
                    onExitChange = {
                        vm.toggleExitSettings(it)
                    },
                    userScrollEnabled = userScrollEnabled,
                    mapContent = mapContent
                )

                AnimatedVisibility(
                    finalState.data.fullLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colors.background.copy(alpha = 0.5f))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaceDetailScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PlaceDetailScreenState,
    onEnterChange: (String) -> Unit,
    onExitChange: (String) -> Unit,
    userScrollEnabled: Boolean,
    mapContent: @Composable (PlaceDetailScreenState) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 40.dp,
            start = 20.dp,
            end = 20.dp
        ),
        userScrollEnabled = userScrollEnabled
    ) {
        if (state.settings.isNotEmpty()) {

            this.item(key = "main_header") {
                PlaceListItem(
                    modifier = Modifier.padding(top = 26.dp),
                    state = PlaceListItemData(
                        id = state.placeId,
                        groupId = state.groupId,
                        name = state.name,
                        image = state.image,
                        private = state.private,
                        lat = 0.0,
                        lng = 0.0,
                        radiusMeters = 0
                    ),
                    hasEndArrow = false,
                    backgroundColor = Color.Transparent
                )
            }

            this.item(key = "map") {
                // map
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(top = 26.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.background,
                    border = BorderStroke(6.dp, MaterialTheme.colors.surface),
                    elevation = 12.dp
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colors.background,
                    ) {
                        mapContent(state)
                    }
                }
            }
        }

        this.item(key = "spacer") {
            Spacer(modifier = Modifier.size(20.dp))
        }

        if (state.settings.isEmpty()) {
            this.item {
                DefaultEmptyState(
                    content = stringResource(MR.strings.places_list_screen_empty_info),
                    icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                    buttonText = null
                ) { }
            }
        } else {
            this.item(key = "notification_header") {
                SettingsSectionHeader(name = stringResource(MR.strings.place_detail_notification_settings_title))
            }
            this.itemsIndexed(
                items = state.settings,
                key = { index, item -> item.memberUserId }
            ) { index, item ->
                PlaceDetailUserListItem(
                    modifier = Modifier.animateItemPlacement(),
                    state = item,
                    onEnterChange = onEnterChange,
                    onExitChange = onExitChange,
                )
            }
        }
    }

}