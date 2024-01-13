package app.paralino.android.ui.places.detail

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.MainAppState
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.android.ui.map.PlaceDetailMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.places.detail.PlaceDetailScreenEvent
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlaceDetailScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String,
    vm: PlaceDetailViewModel = getViewModel { parametersOf(groupId, placeId) },
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState(initial = Payload.Loading)
    var optionMenuExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlaceDetailScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString(context))
                }
                is PlaceDetailScreenEvent.PlaceDeleted -> {
                    appState.navController.popBackStack()
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
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            appState.navController.navigate(
                                AppScreen.MainHome.RawDataScreen.route(
                                    RawPresentationDataType.GroupPlacePresentation(
                                        placeId = state.data!!.placeId
                                    )
                                )
                            )
                        }
                    ) {
                        Icon(
                            painter = AppIcons.Security,
                            tint = AppColors.accent,
                            contentDescription = null
                        )
                    }
                    if (state.data?.canEdit == true) {
                        IconButton(
                            onClick = {
                                appState.navController.navigate(
                                    AppScreen.MainHome.PlaceCreate.route(
                                        groupId = state.data!!.groupId,
                                        placeId = state.data!!.placeId
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = AppIcons.Edit,
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { optionMenuExpanded = true }) {
                            Icon(
                                painter = AppIcons.MoreVert,
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
                                Text(text = stringResource(id = MR.strings.place_list_menu_delete.resourceId))
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
                DefaultEmptyState(content = stringResource(id = MR.strings.error_load_data_generic.resourceId))
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
                    }
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
fun PlaceDetailScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PlaceDetailScreenState,
    onEnterChange: (String) -> Unit,
    onExitChange: (String) -> Unit
) {

    var userScroll by remember {
        mutableStateOf(true)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding(),
            start = 20.dp,
            end = 20.dp
        ),
        userScrollEnabled = userScroll
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
                    hasEndArrow = false
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
                        PlaceDetailMapComposable(
                            modifier = Modifier.fillMaxSize(),
                            state = state,
                            onMapInteraction = { userScroll = !it }
                        )
                    }
                }
            }
        }

        this.item(key = "spacer") {
            Spacer(modifier = Modifier.size(40.dp))
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

        if (state.settings.isEmpty()) {
            this.item {
                DefaultEmptyState(
                    content = stringResource(id = MR.strings.places_list_screen_empty_info.resourceId),
                    icon = AppIcons.Home,
                    buttonText = null
                ) { }
            }
        }
    }

}