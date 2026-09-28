package app.paralino.core.ui.onboarding.account

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay


@Composable
fun AccountOnboardingGroupCreateIntro(
    modifier: Modifier = Modifier,
    title: String,
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(10)
        animationProgress = 1f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp, top = 12.dp)
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
            contentAlignment = Alignment.Center
        ) {
            GroupOnboardingContent1(
                modifier = Modifier
                    .offset(
                        y = animateDpAsState(
                            targetValue = animationProgress
                                .times(-maxHeight.value)
                                .plus(maxHeight.value).dp,
                            animationSpec = tween(durationMillis = 400, delayMillis = 100)
                        ).value
                    )
                    .rotate(-2f)
            )
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp),
            text = title,
            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colors.onBackground,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(MR.strings.onboarding_step_groupCreate_admin_steps_title).uppercase(),
                style = MaterialTheme.typography.caption.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_groupCreate_admin_create_title),
                info = stringResource(MR.strings.onboarding_step_groupCreate_admin_create_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.Edit.asPainterResource()) }
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_groupCreate_admin_add_title),
                info = stringResource(MR.strings.onboarding_step_groupCreate_admin_add_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.PersonAdd.asPainterResource()) }
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_groupCreate_admin_places_title),
                info = stringResource(MR.strings.onboarding_step_groupCreate_admin_places_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.MapsHomeWork.asPainterResource()) }
            )
            OnboardingInfoRow(
                title = stringResource(MR.strings.onboarding_step_groupCreate_admin_share_title),
                info = stringResource(MR.strings.onboarding_step_groupCreate_admin_share_info),
                leading = { OnboardingIconTile(icon = CoreAppIcons.LocationOn.asPainterResource()) }
            )
        }
    }
}
