package app.paralino.core.ui.privacyzones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.PlaceZoneType
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailScreenEvent
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailScreenState
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailScreenUiEvent
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PrivacyZoneDetailScreenCore(
    appState: CoreUiState,
    vm: PrivacyZoneDetailViewModel,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    openPrivacyZoneRawDataScreen: () -> Unit,
    openPrivacyZoneEditScreen: () -> Unit,
    mapContent: @Composable (PrivacyZoneDetailScreenState) -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PrivacyZoneDetailScreenEvent.ZoneDeleted -> onNavigateBack()
                null -> {}
            }
        }
    }

    val dialog = state.data?.alertDialog
    dialog?.Show {
        vm.onEvent(PrivacyZoneDetailScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    when (val finalState = state) {
        Payload.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
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
            PrivacyZoneDetailScreen(
                state = finalState.data,
                paddingValues = contentPadding,
                canEditZone = finalState.data.canEdit,
                userScrollEnabled = userScrollEnabled,
                openPrivacyZoneRawDataScreen = openPrivacyZoneRawDataScreen,
                openPrivacyZoneEditScreen = openPrivacyZoneEditScreen,
                onZoneDelete = { vm.onEvent(PrivacyZoneDetailScreenUiEvent.DeleteZone) },
                mapContent = mapContent
            )

            if (finalState.data.fullLoading) {
                LoadingDialog(
                    text = stringResource(MR.strings.login_step_generic_loading),
                    progress = null
                )
            }
        }
    }
}

@Composable
private fun PrivacyZoneDetailScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PrivacyZoneDetailScreenState,
    canEditZone: Boolean,
    userScrollEnabled: Boolean,
    openPrivacyZoneRawDataScreen: () -> Unit,
    openPrivacyZoneEditScreen: () -> Unit,
    onZoneDelete: () -> Unit,
    mapContent: @Composable (PrivacyZoneDetailScreenState) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 40.dp,
        ),
        userScrollEnabled = userScrollEnabled
    ) {
        item(key = "main_header") {
            PlaceListItem(
                modifier = Modifier.padding(top = 26.dp).padding(horizontal = 20.dp),
                state = remember(state) {
                    PlaceListItemData(
                        id = state.zoneId,
                        groupOrSpaceId = state.groupOrSpaceId,
                        type = PlaceZoneType.PrivacyZone,
                        name = state.name,
                        image = state.image,
                        private = true,
                        lat = null,
                        lng = null,
                        radiusMeters = 0
                    )
                },
                hasEndArrow = false,
                backgroundColor = Color.Transparent
            )
        }

        item(key = "map") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(top = 26.dp)
                    .padding(horizontal = 20.dp),
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

        item(key = "settings") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.size(20.dp))
                SettingsActionItem(
                    name = stringResource(MR.strings.raw_data_screen_title),
                    icon = CoreAppIcons.Security.asPainterResource(),
                ) {
                    openPrivacyZoneRawDataScreen()
                }

                if (canEditZone) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.privacy_zones_edit_screen_title),
                        icon = CoreAppIcons.Edit.asPainterResource(),
                    ) {
                        openPrivacyZoneEditScreen()
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.privacy_zone_menu_delete),
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground
                    ) {
                        onZoneDelete()
                    }
                }
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}
