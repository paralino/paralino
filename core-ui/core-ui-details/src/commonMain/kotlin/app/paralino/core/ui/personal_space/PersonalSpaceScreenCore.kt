package app.paralino.core.ui.personal_space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.groups.detail.GroupSpaceFocusedFeatureChildItem
import app.paralino.core.presentation.personal_space.PersonalSpaceScreenEvent
import app.paralino.core.presentation.personal_space.PersonalSpaceScreenUiEvent
import app.paralino.core.presentation.personal_space.PersonalSpaceViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.PremiumPlanBannerSmall
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.components.GroupSpaceDetailsHeader
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonalSpaceScreenCore(
    appState: CoreUiState,
    vm: PersonalSpaceViewModel,
    contentPadding: PaddingValues,
    openSettings: () -> Unit,
    openMap: () -> Unit,
    openTimeline: () -> Unit,
    openPlaces: () -> Unit,
    openPlace: (GroupSpaceFocusedFeatureChildItem) -> Unit,
    openSharing: () -> Unit,
    openPaywall: () -> Unit,
    askNotificationPermission: () -> Unit,
    askLocationService: () -> Unit,
    askLocationPermission: () -> Unit,
    askBackgroundLocationPermission: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                PersonalSpaceScreenEvent.OpenPaywall -> openPaywall()
                PersonalSpaceScreenEvent.OpenPlaces -> openPlaces()
                is PersonalSpaceScreenEvent.OpenPlace -> openPlace(event.item)
                PersonalSpaceScreenEvent.OpenSharingScreen -> openSharing()
                PersonalSpaceScreenEvent.OpenTimeline -> openTimeline()
                PersonalSpaceScreenEvent.OpenMap -> openMap()

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(PersonalSpaceScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
        }
    }

    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = uiState.isLoadingData,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = !uiState.isLoadingData,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                if (uiState.isFabButtonActive) {
                    if (uiState.sharingStatus != null) {
                        FilledButton(
                            modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                            style = ButtonStyle.SMALL(
                                iconSize = 24.dp
                            ),
                            icon = CoreAppIcons.StopCircle.asPainterResource(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.error,
                                contentColor = MaterialTheme.colors.onError
                            ),
                            onClick = { vm.stopSharing() }
                        ) {
                            Text(text = stringResource(MR.strings.location_sharing_button_stop_personal))
                        }
                    } else {
                        FilledButton(
                            modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                            style = ButtonStyle.SMALL(
                                iconSize = 24.dp
                            ),
                            icon = CoreAppIcons.ShareLocation.asPainterResource(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = MaterialTheme.colors.onPrimary
                            ),
                            onClick = { vm.onEvent(PersonalSpaceScreenUiEvent.OpenSharingScreen) }
                        ) {
                            if (uiState.isCurrentDeviceActivated) {
                                Text(text = stringResource(MR.strings.location_sharing_button_start_personal))
                            } else {
                                Text(text = stringResource(MR.strings.personalSpace_tracking_activate))
                            }
                        }
                    }
                }
            },
            isFloatingActionButtonDocked = true
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = contentPadding.calculateTopPadding() + 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp
                )
            ) {
                if (uiState.arePlacesOverLimit == true) {
                    this.item(key = "over_limit_notice") {
                        NoticeInfoChip(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            text = stringResource(MR.strings.plan_limit_overLimit_places_banner_title),
                            icon = CoreAppIcons.Warning.asPainterResource(),
                            backgroundColor = MaterialTheme.colors.error,
                            textColor = MaterialTheme.colors.onError,
                            iconTint = MaterialTheme.colors.onError
                        )
                    }
                } else if (uiState.isSpaceOverLimit == true) {
                    this.item(key = "over_limit_notice") {
                        NoticeInfoChip(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            text = stringResource(MR.strings.plan_limit_overLimit_sharing_banner_title),
                            icon = CoreAppIcons.Warning.asPainterResource(),
                            backgroundColor = MaterialTheme.colors.error,
                            textColor = MaterialTheme.colors.onError,
                            iconTint = MaterialTheme.colors.onError
                        )
                    }
                }

                this.items(items = uiState.errors, key = { "error_$it" }) {
                    ConditionErrorItem(data = it) {
                        when (it) {
                            ConditionErrorType.GPS -> askLocationService()
                            ConditionErrorType.PermissionNotifications -> askNotificationPermission()
                            is ConditionErrorType.PermissionLocation -> askLocationPermission()
                            ConditionErrorType.PermissionLocationBackground -> askBackgroundLocationPermission()
                            ConditionErrorType.Network -> vm.showSnackbar(
                                text = MR.strings.error_no_connection_snackbar.localizedString()
                            )

                            else -> {}
                        }
                    }
                }

                if (uiState.errors.isNotEmpty()) {
                    this.item { Spacer(modifier = Modifier.size(10.dp)) }
                }

                this.item(key = "main_header") {
                    GroupSpaceDetailsHeader(
                        modifier = Modifier.animateItem(),
                        isPersonalSpace = true,
                        groupName = uiState.userName,
                        groupImage = uiState.userImage,
                        sharingStatus = uiState.sharingStatus,
                        featurePlaces = uiState.placesFeatureItems,
                        featurePrivacyZones = remember { emptyList() },
                        onPlacesClick = { vm.onEvent(PersonalSpaceScreenUiEvent.OpenPlaces) },
                        onPrivacyZonesClick = { },
                        onPlaceClick = { vm.onEvent(PersonalSpaceScreenUiEvent.OpenPlace(it)) },
                        onPrivacyZoneClick = { },
                        onMapClick = { vm.onEvent(PersonalSpaceScreenUiEvent.OpenMap) },
                        onSettingsClick = openSettings,
                        onSharingClick = { vm.onEvent(PersonalSpaceScreenUiEvent.OpenSharingScreen) },
                    )
                }

                if (uiState.shouldShowPlanBanner) {
                    this.item(key = "plan_upgrade_banner") {
                        PremiumPlanBannerSmall(
                            modifier = Modifier.zIndex(1f),
                            description = stringResource(MR.strings.plans_banner_inline_personal_space),
                            onClick = { vm.onEvent(PersonalSpaceScreenUiEvent.OpenPaywall) }
                        )
                        Spacer(modifier = Modifier.size(30.dp))
                    }
                }

                this.stickyHeader(key = "active_devices_list_header") {
                    SettingsSectionHeader(
                        modifier = Modifier.fillParentMaxWidth().zIndex(0f),
                        name = stringResource(MR.strings.personalSpace_section_activated),
                        count = uiState.activeDevices.size,
                        limit = uiState.devicesLimit
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }

                this.itemsIndexed(
                    items = uiState.activeDevices,
                    key = { index, item -> item.id }
                ) { index, item ->

                    if (index > 0)
                        Spacer(modifier = Modifier.size(8.dp))

                    UserDeviceItem(
                        modifier = Modifier.animateItem(),
                        state = item,
                        onEvent = { event ->
                            vm.onEvent(PersonalSpaceScreenUiEvent.OnDeviceEvent(event))
                        }
                    )
                }

                if (uiState.activeDevices.isEmpty()) {
                    this.item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(
                                    if (uiState.plan.hasPersonalSpace) {
                                        MR.strings.personalSpace_section_activated_empty
                                    } else {
                                        MR.strings.personalSpace_section_activated_upgrade
                                    }
                                ),
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                            )
                        }
                    }
                }

                if (uiState.otherDevices.isNotEmpty()) {
                    this.stickyHeader(key = "other_devices_list_header") {
                        SettingsSectionHeader(
                            modifier = Modifier.fillParentMaxWidth().zIndex(0f),
                            name = stringResource(MR.strings.personalSpace_section_other)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    this.itemsIndexed(
                        items = uiState.otherDevices,
                        key = { index, item -> item.id }
                    ) { index, item ->

                        if (index > 0)
                            Spacer(modifier = Modifier.size(8.dp))

                        UserDeviceItem(
                            modifier = Modifier.animateItem(),
                            state = item,
                            onEvent = { event ->
                                vm.onEvent(PersonalSpaceScreenUiEvent.OnDeviceEvent(event))
                            }
                        )
                    }
                }

                if (uiState.devicesWithSessions.isEmpty()) {
                    this.item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(MR.strings.personalSpace_section_empty),
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.loadingAction) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}
