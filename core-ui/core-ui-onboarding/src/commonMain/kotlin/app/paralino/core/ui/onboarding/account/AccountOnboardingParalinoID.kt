package app.paralino.core.ui.onboarding.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.onboarding.AccountOnboardingContent
import app.paralino.core.presentation.paralinoid.PersonalCodeScreenCodeData
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.IconButtonWithClickConfirmation
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.invite.manage.PersonalCodeItem
import app.paralino.core.ui.invite.manage.PersonalCodeItemWithHints
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.DeeplinkType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun AccountOnboardingParalinoID(
    modifier: Modifier = Modifier,
    step: AccountOnboardingContent.ParalinoID,
    title: String,
    info: String?,
    onSharePersonalCode: (String) -> Unit,
    onCopyPersonalCode: () -> Unit
) {
    AnimatedContent(
        modifier = modifier.fillMaxWidth(),
        targetState = step.didShareCode,
        label = "paralino id join state"
    ) { didShareCode ->
        if (didShareCode) {
            ParalinoIDWaiting(
                personalCode = step.personalCode,
                title = title,
                info = info,
                onSharePersonalCode = onSharePersonalCode,
                onCopyPersonalCode = onCopyPersonalCode
            )
        } else {
            ParalinoIDJoin(
                personalCode = step.personalCode,
                onSharePersonalCode = onSharePersonalCode,
                onCopyPersonalCode = onCopyPersonalCode
            )
        }
    }
}

@Composable
private fun ParalinoIDJoin(
    personalCode: PersonalCodeScreenCodeData?,
    onSharePersonalCode: (String) -> Unit,
    onCopyPersonalCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 20.dp)
    ) {

        if (personalCode != null) {
            PersonalCodeItemWithHints(hintDelayMillis = 800) {
                PersonalCodeItem(
                    modifier = this,
                    state = personalCode,
                    hintDelayMillis = 0,
                    hasShareHint = false,
                    onCopy = onCopyPersonalCode,
                    onShare = onSharePersonalCode,
                    onDelete = {}
                )
            }
        } else {
            DefaultEmptyState(
                modifier = Modifier,
                icon = CoreAppIcons.AddLink.asPainterResource(),
                content = stringResource(MR.strings.invite_manage_empty_description)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(MR.strings.onboarding_step_paralinoID_steps_title).uppercase(),
                style = MaterialTheme.typography.caption.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_paralinoID_step_send_title),
                info = stringResource(MR.strings.onboarding_step_paralinoID_step_send_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.Share.asPainterResource()) }
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_paralinoID_step_add_title),
                info = stringResource(MR.strings.onboarding_step_paralinoID_step_add_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.PersonAdd.asPainterResource()) }
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_paralinoID_step_done_title),
                info = stringResource(MR.strings.onboarding_step_paralinoID_step_done_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.LocationOn.asPainterResource()) }
            )
        }
    }
}

@Composable
private fun ParalinoIDWaiting(
    personalCode: PersonalCodeScreenCodeData?,
    title: String,
    info: String?,
    onSharePersonalCode: (String) -> Unit,
    onCopyPersonalCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaitingAvatar(
            userImage = personalCode?.userImage ?: ImageModel.emoji(Emojis.robot)
        )

        Column(
            modifier = Modifier.padding(top = 10.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center
            )

            if (!info.isNullOrBlank()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = info.annotatedBoldCompose(),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (personalCode != null) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(MR.strings.onboarding_step_paralinoID_waiting_code_label),
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
                        )
                        Text(
                            text = personalCode.code,
                            style = MaterialTheme.typography.h6.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colors.onBackground,
                        )
                    }
                    IconButtonWithClickConfirmation(
                        defaultIcon = CoreAppIcons.ContentCopy,
                        onClick = onCopyPersonalCode
                    )
                }

                Divider(color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(MR.strings.onboarding_step_paralinoID_waiting_resend_info),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                    )
                    FilledButton(
                        style = ButtonStyle.SMALL,
                        icon = CoreAppIcons.Share.asPainterResource(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        elevation = 0.dp,
                        onClick = {
                            onSharePersonalCode(DeeplinkType.ParalinoID.buildUrl(personalCode.code))
                        }
                    ) {
                        Text(text = stringResource(MR.strings.button_share))
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingAvatar(
    modifier: Modifier = Modifier,
    userImage: ImageModel
) {
    val transition = rememberInfiniteTransition(label = "exploding rings")
    val firstRing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "first ring"
    )
    val secondRing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1200)
        ),
        label = "second ring"
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        listOf(firstRing, secondRing).forEach { progress ->
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(0.9f + progress * 1.5f)
                    .alpha(0.9f * (1f - progress))
                    .clip(CircleShape)
                    .background(AppColors.accentDark.copy(alpha = 0.2f))
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.surface)
                .border(width = 4.dp, color = AppColors.accentDark, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ImageModelImage(
                imageModel = userImage,
                modifier = Modifier.size(60.dp),
                useBackgroundForEmoji = false
            )
        }
    }
}
