package app.paralino.core.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.onboarding.GroupJoinWelcomePreviewChipData
import app.paralino.core.presentation.onboarding.GroupJoinWelcomeScreenEvent
import app.paralino.core.presentation.onboarding.GroupJoinWelcomeScreenUiEvent
import app.paralino.core.presentation.onboarding.GroupJoinWelcomeStepContent
import app.paralino.core.presentation.onboarding.GroupJoinWelcomeViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.BaseOnboardingHeaderContent
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupJoinWelcomeScreenCore(
    coreUiState: CoreUiState,
    vm: GroupJoinWelcomeViewModel,
    paddingValues: PaddingValues,
    openSharing: () -> Unit,
    openMap: () -> Unit
) {
    MessageDispatcherEffect(vm, coreUiState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                GroupJoinWelcomeScreenEvent.OpenSharing -> openSharing()
                GroupJoinWelcomeScreenEvent.OpenMap -> openMap()
                null -> {}
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val step = uiState.step
        val title = when {
            step is GroupJoinWelcomeStepContent.WelcomeLanding && !uiState.groupName.isNullOrBlank() -> {
                stringResource(
                    MR.strings.group_member_current_add_notification,
                    uiState.groupName ?: ""
                )
            }

            step is GroupJoinWelcomeStepContent.ShareLocation && uiState.isSharingActive -> {
                stringResource(MR.strings.onboarding_step_groupJoinWelcome_share_already_title)
            }

            step is GroupJoinWelcomeStepContent.Done && !uiState.isSharingActive -> {
                stringResource(MR.strings.onboarding_step_groupJoinWelcome_done_title_skipped)
            }

            else -> step.title()
        }

        val info =
            if (step is GroupJoinWelcomeStepContent.ShareLocation && uiState.isSharingActive) {
                stringResource(MR.strings.onboarding_step_groupJoinWelcome_share_already_info)
            } else {
                step.info?.let { stringResource(it) }
            }

        val titleTag = if (step is GroupJoinWelcomeStepContent.WelcomeLanding) {
            stringResource(MR.strings.onboarding_step_groupJoinWelcome_title_tag)
        } else {
            uiState.groupName?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.onboarding_step_groupCreate_title_tag)
        }

        BaseOnboardingScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            order = step.titleKey,
            title = title,
            info = info,
            buttonText = stringResource(
                if (step is GroupJoinWelcomeStepContent.Done && !uiState.isSharingActive) {
                    MR.strings.button_continue
                } else {
                    step.buttonText
                }
            ),
            skipButtonText = stringResource(MR.strings.button_skip),
            contentSizeRatio = 0.28f,
            minHeaderSize = 340.dp,
            onSkip = null,
            onNext = { vm.onEvent(GroupJoinWelcomeScreenUiEvent.Confirm) },
            headerContent = {
                BaseOnboardingHeaderContent(
                    titleKey = step.titleKey,
                    animatedContentKey = step.parentKey,
                    titleTag = titleTag,
                    content = { parentKey ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp, top = 12.dp)
                                .dropShadow(
                                    shape = RoundedCornerShape(20.dp),
                                    shadow = Shadow(
                                        radius = 12.dp,
                                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                                    )
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colors.surface)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.accentDark.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (parentKey == GroupJoinWelcomeStepContent.WelcomeLanding.parentKey) {
                                GroupJoinWelcomeGroupHeader(
                                    groupName = uiState.groupName,
                                    groupImage = uiState.groupImage,
                                    members = uiState.members,
                                    places = uiState.places
                                )
                            } else {
                                GroupJoinWelcomeSharingHeader(
                                    isCurrent = step is GroupJoinWelcomeStepContent.ShareLocation,
                                    isSharingActive = uiState.isSharingActive
                                )
                            }
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun GroupJoinWelcomeGroupHeader(
    groupName: String?,
    groupImage: ImageModel?,
    members: List<GroupJoinWelcomePreviewChipData>,
    places: List<GroupJoinWelcomePreviewChipData>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .scale(1.25f)
                .blur(
                    radius = if (isSystemInDarkTheme()) 8.dp else 5.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                ),
            painter = if (isSystemInDarkTheme()) {
                CoreAppIcons.MapMockupDark.asPainterResource()
            } else {
                CoreAppIcons.MapMockupLight.asPainterResource()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.accentDark.copy(alpha = 0.12f))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (groupImage != null) {
                    ImageModelImage(
                        imageModel = groupImage,
                        modifier = Modifier.size(88.dp),
                        clipShape = RoundedCornerShape(22.dp)
                    )
                }
                if (!groupName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (members.isNotEmpty()) {
                Spacer(modifier = Modifier.size(12.dp))
                GroupJoinWelcomeChipRow(
                    title = stringResource(MR.strings.group_details_section_members_title),
                    items = members
                )
            }
            if (places.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                GroupJoinWelcomeChipRow(
                    title = stringResource(MR.strings.places_list_screen_title),
                    items = places
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun GroupJoinWelcomeChipRow(
    title: String,
    items: List<GroupJoinWelcomePreviewChipData>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            text = title,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            textAlign = TextAlign.Center
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items, key = { it.id }) { item ->
                DataInfoChip(
                    image = item.image,
                    text = item.name,
                    backgroundColor = MaterialTheme.colors.surface
                )
            }
        }
    }
}

@Composable
private fun GroupJoinWelcomeSharingHeader(
    isCurrent: Boolean,
    isSharingActive: Boolean
) {
    val status = when {
        isSharingActive -> GroupJoinWelcomeShareStatus.COMPLETED
        isCurrent -> GroupJoinWelcomeShareStatus.CURRENT
        else -> GroupJoinWelcomeShareStatus.SKIPPED
    }

    val title = when (status) {
        GroupJoinWelcomeShareStatus.SKIPPED -> {
            stringResource(MR.strings.onboarding_step_groupJoinWelcome_share_point_skipped)
        }

        GroupJoinWelcomeShareStatus.COMPLETED -> {
            stringResource(MR.strings.onboarding_step_groupJoinWelcome_share_point_on)
        }

        GroupJoinWelcomeShareStatus.CURRENT -> {
            stringResource(MR.strings.onboarding_step_groupJoinWelcome_share_point)
        }
    }

    val nodeBackground by animateColorAsState(
        targetValue = when (status) {
            GroupJoinWelcomeShareStatus.CURRENT,
            GroupJoinWelcomeShareStatus.COMPLETED -> MaterialTheme.colors.primary

            GroupJoinWelcomeShareStatus.SKIPPED -> AppColors.errorBackground
        },
        animationSpec = tween(durationMillis = 350)
    )

    val nodeContentColor by animateColorAsState(
        targetValue = when (status) {
            GroupJoinWelcomeShareStatus.CURRENT,
            GroupJoinWelcomeShareStatus.COMPLETED -> MaterialTheme.colors.onPrimary

            GroupJoinWelcomeShareStatus.SKIPPED -> AppColors.redMain
        },
        animationSpec = tween(durationMillis = 350)
    )

    val icon: Painter = when (status) {
        GroupJoinWelcomeShareStatus.COMPLETED -> CoreAppIcons.CheckCircle.asPainterResource()
        GroupJoinWelcomeShareStatus.SKIPPED -> CoreAppIcons.LocationOff.asPainterResource()
        GroupJoinWelcomeShareStatus.CURRENT -> CoreAppIcons.ShareLocation.asPainterResource()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.popOnce(active = isCurrent),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(nodeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = nodeContentColor
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
    }
}

private enum class GroupJoinWelcomeShareStatus {
    CURRENT,
    COMPLETED,
    SKIPPED
}

@Composable
private fun Modifier.popOnce(active: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active) {
            scale.snapTo(0.82f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            scale.snapTo(1f)
        }
    }
    return this.scale(scale.value)
}
