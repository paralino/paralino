package app.paralino.core.ui.onboarding.account

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.places.NotificationMockupItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.pulsate
import kotlinx.coroutines.delay

@Composable
fun AccountOnboardingPushNotifications(
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
        contentAlignment = Alignment.TopCenter
    ) {
        NotificationContent1(
            modifier = Modifier
                .offset(
                    x = (-8).dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.02).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 100)
                    ).value
                )
                .rotate(3f)
                .scale(0.93f)
        )

        NotificationContent2(
            modifier = Modifier
                .offset(
                    x = 16.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.15).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 300)
                    ).value
                )
                .rotate(-3f)
                .scale(0.98f)
        )

        NotificationContent3(
            modifier = Modifier
                .offset(
                    x = (-12).dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.38).dp,
                        animationSpec = tween(durationMillis = 300, delayMillis = 500)
                    ).value
                )
                .rotate(1f)
        )

        NotificationContent4(
            modifier = Modifier
                .offset(
                    x = (-4).dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.6).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 700)
                    ).value
                )
                .rotate(-1f)
                .scale(0.98f)
        )

        NotificationContent5(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    x = 12.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value - maxHeight.value * 0.05).dp,
                        animationSpec = tween(durationMillis = 300, delayMillis = 900)
                    ).value
                )
                .rotate(3f)
                .scale(0.94f)
        )
    }
}

@Composable
private fun NotificationContent1(modifier: Modifier = Modifier) {
    val notificationContent = remember {
        MR.strings.place_notification_enter.localizedString(
            "Peter",
            MR.strings.places_notification_demo_school.localizedString()
        )
    }
    NotificationContentItem(
        modifier = modifier,
        notificationContent = notificationContent
    )
}

@Composable
private fun NotificationContent2(modifier: Modifier = Modifier) {
    val notificationContent = remember {
        MR.strings.group_member_current_add_notification_default.localizedString()
    }
    NotificationContentItem(
        modifier = modifier,
        notificationContent = notificationContent
    )
}

@Composable
private fun NotificationContent3(modifier: Modifier = Modifier) {
    val notificationContent = remember {
        MR.strings.group_member_sharing_started_notification.localizedString(
            "Heidi"
        )
    }
    NotificationContentItem(
        modifier = modifier,
        notificationContent = notificationContent
    )
}

@Composable
private fun NotificationContent4(modifier: Modifier = Modifier) {
    val notificationContent = remember {
        MR.strings.place_notification_exit.localizedString(
            "Heidi",
            MR.strings.places_notification_demo_home.localizedString()
        )
    }
    NotificationContentItem(
        modifier = modifier,
        notificationContent = notificationContent
    )
}

@Composable
private fun NotificationContent5(modifier: Modifier = Modifier) {
    val notificationContent = remember {
        MR.strings.group_member_add_notification_default.localizedString()
    }
    NotificationContentItem(
        modifier = modifier,
        notificationContent = notificationContent
    )
}

@Composable
private fun NotificationContentItem(
    modifier: Modifier = Modifier,
    notificationContent: String
) {
    NotificationMockupItem(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .pulsate(pulseAmount = 0.95f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        description = notificationContent,
    )
}