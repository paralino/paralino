package app.paralino.core.ui.privacyzones

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesListScreenState
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesScreenEvent
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesScreenUiEvent
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BaseOnboardingScreen
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
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PrivacyZonesScreenCore(
    appState: CoreUiState,
    vm: PrivacyZonesViewModel,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    addNewPrivacyZone: () -> Unit,
    openPaywall: () -> Unit,
    onSkipOnboarding: () -> Unit,
    openPrivacyZoneDetails: (zone: PlaceListItemData) -> Unit,
    mapContent: @Composable (PrivacyZonesListScreenState) -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                PrivacyZonesScreenEvent.AddPrivacyZone -> addNewPrivacyZone()
                is PrivacyZonesScreenEvent.OpenPaywall -> openPaywall()
                null -> { /* do nothing */
                }
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(PrivacyZonesScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
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
            PrivacyZonesScreen(
                state = state.data,
                isOnboarding = vm.isOnboarding,
                userScrollEnabled = userScrollEnabled,
                contentPadding = contentPadding,
                onZoneClick = openPrivacyZoneDetails,
                onCreatePrivacyZone = {
                    vm.onEvent(PrivacyZonesScreenUiEvent.CreatePrivacyZone)
                },
                onSkipOnboarding = onSkipOnboarding,
                onPlanBannerClick = openPaywall,
                mapContent = mapContent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrivacyZonesScreen(
    state: PrivacyZonesListScreenState,
    isOnboarding: Boolean,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    onZoneClick: (PlaceListItemData) -> Unit,
    onCreatePrivacyZone: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onPlanBannerClick: () -> Unit,
    mapContent: @Composable (PrivacyZonesListScreenState) -> Unit,
) {
    BaseOnboardingScreen(
        modifier = Modifier.fillMaxSize().conditional(isOnboarding || state.zones.isEmpty()) {
            padding(contentPadding)
        },
        nextButton = if (isOnboarding || state.zones.isEmpty()) {
            @Composable {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.CONTENT,
                    icon = if (state.privacyZonesLimitReached) {
                        null
                    } else {
                        CoreAppIcons.PrivacyZone.asPainterResource()
                    },
                    onClick = {
                        if (state.privacyZonesLimitReached) {
                            onSkipOnboarding()
                        } else {
                            onCreatePrivacyZone()
                        }
                    }
                ) {
                    Text(
                        text = if (state.privacyZonesLimitReached) {
                            stringResource(MR.strings.button_next)
                        } else {
                            stringResource(MR.strings.privacy_zones_create_button_create)
                        }
                    )
                }
            }
        } else {
            null
        },
        skipButton = if (isOnboarding && !state.privacyZonesLimitReached) {
            @Composable {
                OnboardingSkipButton(
                    skipButtonText = if (state.zones.isEmpty()) {
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
        if (state.zones.isEmpty()) {
            PrivacyZonesEmptyState(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(it),
            )
        } else {
            var isIntroVisible by remember { mutableStateOf(false) }
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(it),
                state = listState,
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 40.dp
                ),
                userScrollEnabled = userScrollEnabled
            ) {
                this.item(key = "main_header") {
                    DataInfoChip(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        image = state.groupImage,
                        text = state.groupName,
                        backgroundColor = MaterialTheme.colors.surface
                    )
                }

                if (state.isOverLimit) {
                    this.item(key = "over_limit_notice") {
                        NoticeInfoChip(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            text = stringResource(MR.strings.plan_limit_overLimit_privacy_zones_banner_title),
                            icon = CoreAppIcons.Warning.asPainterResource(),
                            backgroundColor = MaterialTheme.colors.error,
                            textColor = MaterialTheme.colors.onError,
                            iconTint = MaterialTheme.colors.onError
                        )
                    }
                }

                this.item(key = "map") {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
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
                            modifier = Modifier
                                .zIndex(5f)
                                .padding(horizontal = 20.dp),
                            description = stringResource(MR.strings.plans_banner_inline_privacy_zones),
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
                            .padding(horizontal = 20.dp)
                            .background(MaterialTheme.colors.background)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(MR.strings.privacy_zones_list_screen_title),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.alpha(ContentAlpha.medium)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        PlanLimitCount(
                            modifier = Modifier.padding(top = 4.dp),
                            count = state.zones.size,
                            limit = state.privacyZonesLimit
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        SmallActionButton(
                            modifier = Modifier,
                            icon = CoreAppIcons.Add.asPainterResource(),
                            text = {
                                Text(
                                    modifier = Modifier.padding(end = 2.dp),
                                    text = stringResource(MR.strings.privacy_zones_list_add_button)
                                )
                            },
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary,
                            onClick = onCreatePrivacyZone
                        )
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                }

                this.itemsIndexed(
                    items = state.zones,
                    key = { _, item -> item.id }
                ) { index, item ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    PlaceListItem(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .animateItem(),
                        state = item,
                        hasEndArrow = true,
                        onClick = { onZoneClick(item) },
                    )
                }

                this.item(key = "intro_toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp)
                            .clickable { isIntroVisible = !isIntroVisible }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "How it works",
                            style = MaterialTheme.typography.caption
                        )
                        Icon(
                            modifier = Modifier
                                .size(26.dp)
                                .padding(4.dp)
                                .rotate(
                                    animateFloatAsState(
                                        if (isIntroVisible) -90f else 90f
                                    ).value
                                ),
                            painter = CoreAppIcons.ChevronRight.asPainterResource(),
                            contentDescription = null
                        )
                    }
                }

                if (isIntroVisible) {
                    this.item(key = "intro") {
                        PrivacyZonesEmptyState(
                            modifier = Modifier
                                .animateItem()
                        )
                    }
                }
            }
        }
    }
}
