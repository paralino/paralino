package app.paralino.core.ui.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesListScreenState
import app.paralino.core.presentation.places.list.PlacesScreenEvent
import app.paralino.core.presentation.places.list.PlacesScreenUiEvent
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.PremiumPlanBannerSmall
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlacesScreenCore(
    appState: CoreUiState,
    vm: PlacesViewModel,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    askNotificationPermission: () -> Unit,
    addNewPlace: () -> Unit,
    openPaywall: () -> Unit,
    onSkipOnboarding: () -> Unit,
    openPlaceDetails: (place: PlaceListItemData) -> Unit,
    mapContent: @Composable (PlacesListScreenState) -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                PlacesScreenEvent.AddPlace -> {
                    addNewPlace()
                }

                is PlacesScreenEvent.OpenPaywall -> {
                    openPaywall()
                }

                PlacesScreenEvent.MissingNotificationPermission -> {
                    askNotificationPermission()
                }

                null -> { /* do nothing */
                }
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(PlacesScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    when (val state = uiState) {
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
            PlacesScreen(
                state = state.data,
                isOnboarding = vm.isOnboarding,
                userScrollEnabled = userScrollEnabled,
                contentPadding = contentPadding,
                onPlaceClick = {
                    openPlaceDetails(it)
                },
                onCreatePlace = {
                    vm.onEvent(PlacesScreenUiEvent.CreatePlace)
                },
                onSkipOnboarding = onSkipOnboarding,
                onPlanBannerClick = openPaywall,
                onErrorItemClick = {
                    askNotificationPermission()
                },
                onPostSampleNotification = {
                    vm.onEvent(PlacesScreenUiEvent.PostSampleNotification(it))
                },
                mapContent = mapContent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlacesScreen(
    state: PlacesListScreenState,
    isOnboarding: Boolean,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onCreatePlace: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onPlanBannerClick: () -> Unit,
    onPostSampleNotification: (String) -> Unit,
    mapContent: @Composable (PlacesListScreenState) -> Unit
) {
    BaseOnboardingScreen(
        modifier = Modifier.fillMaxSize().conditional(isOnboarding || state.places.isEmpty()) {
            padding(contentPadding)
        },
        nextButton = if (isOnboarding || state.places.isEmpty()) {
            @Composable {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.CONTENT,
                    icon = if (state.placesLimitReached) {
                        null
                    } else {
                        CoreAppIcons.MapsHomeWork.asPainterResource()
                    },
                    onClick = {
                        if (state.placesLimitReached) {
                            onSkipOnboarding()
                        } else {
                            onCreatePlace()
                        }
                    }
                ) {
                    Text(
                        text = if (state.placesLimitReached) {
                            stringResource(MR.strings.button_next)
                        } else {
                            stringResource(MR.strings.places_create_button_create)
                        }
                    )
                }
            }
        } else {
            null
        },
        skipButton = if (isOnboarding && !state.placesLimitReached) {
            @Composable {
                OnboardingSkipButton(
                    skipButtonText = if (state.places.isEmpty()) {
                        stringResource(MR.strings.button_skip)
                    } else {
                        stringResource(MR.strings.button_next)
                    },
                    onSkip = onSkipOnboarding
                )
            }
        } else {
            null
        }
    ) {
        if (state.places.isEmpty()) {
            PlacesEmptyState(
                modifier = Modifier.padding(it),
                isPersonalSpace = state.isPersonalSpace,
                permissionError = state.error,
                onErrorItemClick = onErrorItemClick,
                onPostSampleNotification = onPostSampleNotification
            )
        } else {
            val error = state.error
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(it),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 40.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                userScrollEnabled = userScrollEnabled
            ) {
                if (state.places.isNotEmpty()) {
                    this.item(key = "main_header") {
                        DataInfoChip(
                            modifier = Modifier,
                            image = state.groupImage,
                            text = state.groupName,
                            backgroundColor = if (state.isPersonalSpace) {
                                AppColors.accent.copy(0.1f)
                            } else {
                                MaterialTheme.colors.surface
                            }
                        )
                    }

                    if (state.isOverLimit) {
                        this.item(key = "over_limit_notice") {
                            NoticeInfoChip(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                text = stringResource(MR.strings.plan_limit_overLimit_places_banner_title),
                                icon = CoreAppIcons.Warning.asPainterResource(),
                                backgroundColor = MaterialTheme.colors.error,
                                textColor = MaterialTheme.colors.onError,
                                iconTint = MaterialTheme.colors.onError
                            )
                        }
                    }

                    if (error != null) {
                        this.item(key = error.toString()) {
                            ConditionErrorItem(
                                modifier = Modifier.padding(top = 8.dp),
                                data = error
                            ) {
                                onErrorItemClick(error)
                            }
                        }
                    }

                    this.item(key = "map") {
                        // map
                        Surface(
                            modifier = Modifier
                                .zIndex(10f)
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(top = 8.dp),
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
                        Spacer(modifier = Modifier.size(30.dp))
                    }

                    if (state.shouldShowPlanBanner) {
                        this.item(key = "plan_upgrade_banner") {
                            PremiumPlanBannerSmall(
                                modifier = Modifier.zIndex(5f),
                                description = stringResource(MR.strings.plans_banner_inline_places),
                                onClick = onPlanBannerClick
                            )
                            Spacer(modifier = Modifier.size(30.dp))
                        }
                    }

                    this.stickyHeader(key = "count_header") {
                        Row(
                            modifier = Modifier
                                .zIndex(0f)
                                .fillParentMaxWidth()
                                .background(MaterialTheme.colors.background)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(MR.strings.places_list_screen_title),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.alpha(ContentAlpha.medium)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            PlanLimitCount(
                                modifier = Modifier.padding(top = 4.dp),
                                count = state.places.size,
                                limit = if (state.canShowPlacesLimit) {
                                    state.placesLimit
                                } else {
                                    null
                                }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            SmallActionButton(
                                modifier = Modifier,
                                icon = CoreAppIcons.Add.asPainterResource(),
                                text = {
                                    Text(
                                        modifier = Modifier.padding(end = 2.dp),
                                        text = stringResource(MR.strings.group_details_add_member_button)
                                    )
                                },
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = MaterialTheme.colors.onPrimary,
                                onClick = onCreatePlace
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }

                this.itemsIndexed(
                    items = state.places,
                    key = { index, item -> item.id }
                ) { index, item ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    PlaceListItem(
                        modifier = Modifier.animateItem(),
                        state = item,
                        hasEndArrow = true,
                        onClick = { onPlaceClick(item) },
                    )
                }
            }
        }
    }
}
