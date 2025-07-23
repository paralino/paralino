package app.paralino.core.ui.onboarding.account

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.groups.GroupListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.pulsate
import kotlinx.coroutines.delay

@Composable
fun AccountOnboardingGroupCreate(
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(10)
        animationProgress = 1f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp, top = 12.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.accentDark.copy(alpha = 0.2f)),
        contentAlignment = Alignment.TopCenter
    ) {
        GroupContent1(
            modifier = Modifier
                .offset(
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.05).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 100)
                    ).value
                )
                .rotate(3f)
        )

        GroupContent2(
            modifier = Modifier
                .offset(
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.38).dp,
                        animationSpec = tween(durationMillis = 500, delayMillis = 300)
                    ).value
                )
                .rotate(-3f)
        )

        GroupContent3(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value - maxHeight.value * 0.05).dp,
                        animationSpec = tween(durationMillis = 600, delayMillis = 500)
                    ).value
                )
                .rotate(2f)
        )
    }
}

@Composable
private fun GroupContent1(modifier: Modifier = Modifier) {
    val data = remember {
        GroupItemState(
            id = "",
            name = MR.strings.onboarding_step_groupCreate_group_home.localizedString(),
            image = ImageModel.emoji(Emojis.heart),
            membersCount = 6,
            devicesCount = null,
            placesCount = 12,
            isPersonalSpace = false,
            uiType = GroupItemState.GroupUiType.ACTIVE,
            canEdit = true,
            isCurrentUserOwner = true,
            isSelected = true,
            isLarge = true
        )
    }
    GroupListItem(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .pulsate(pulseAmount = 0.9f)
            .clickable { }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
        data = data,
        onEdit = {}
    )
}

@Composable
private fun GroupContent2(modifier: Modifier = Modifier) {
    val data = remember {
        GroupItemState(
            id = "",
            name = MR.strings.onboarding_step_groupCreate_group_friends.localizedString(),
            image = ImageModel.emoji(Emojis.star),
            membersCount = 8,
            devicesCount = null,
            placesCount = 6,
            isPersonalSpace = false,
            uiType = GroupItemState.GroupUiType.INACTIVE,
            canEdit = false,
            isCurrentUserOwner = true,
            isSelected = false,
            isLarge = true
        )
    }
    GroupListItem(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .pulsate(pulseAmount = 0.9f)
            .clickable { }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
        data = data,
        onEdit = {}
    )
}

@Composable
private fun GroupContent3(modifier: Modifier = Modifier) {
    val data = remember {
        GroupItemState(
            id = "",
            name = MR.strings.onboarding_step_groupCreate_group_work.localizedString(),
            image = ImageModel.emoji(Emojis.suitecase),
            membersCount = 10,
            devicesCount = null,
            placesCount = 3,
            isPersonalSpace = false,
            uiType = GroupItemState.GroupUiType.INACTIVE,
            canEdit = true,
            isCurrentUserOwner = true,
            isSelected = false,
            isLarge = true
        )
    }
    GroupListItem(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .pulsate(pulseAmount = 0.9f)
            .clickable { }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
        data = data,
        onEdit = {}
    )
}