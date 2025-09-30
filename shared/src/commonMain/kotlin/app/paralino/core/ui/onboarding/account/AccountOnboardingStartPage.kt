package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.ui.components.CheckButtonIcon
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.map.WavyDashedLine
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountOnboardingStartPage(
    modifier: Modifier = Modifier,
    isGroupChecked: Boolean,
    isPersonalSpaceChecked: Boolean,
    onGroupModeClick: () -> Unit,
    onPersonalSpaceModeClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp),
                text = stringResource(MR.strings.onboarding_step_accountStart_title),
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colors.onBackground,
            )

            Text(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                text = stringResource(MR.strings.onboarding_step_accountStart_info),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )

            Spacer(modifier = Modifier.weight(0.1f))
            ModeButton(
                modifier = Modifier
                    .fillMaxHeight(),
                isSelected = isGroupChecked,
                title = stringResource(MR.strings.onboarding_step_accountStart_groupMode_title),
                info = stringResource(MR.strings.onboarding_step_accountStart_groupMode_info),
                icon = CoreAppIcons.Groups.asPainterResource(),
                waveAngle = -10f,
                onClick = onGroupModeClick
            )
            Spacer(modifier = Modifier.weight(0.1f))
            ModeButton(
                modifier = Modifier
                    .fillMaxHeight(),
                isSelected = isPersonalSpaceChecked,
                title = stringResource(MR.strings.onboarding_step_accountStart_personalSpaceMode_title),
                info = stringResource(MR.strings.onboarding_step_accountStart_personalSpaceMode_info),
                icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
                waveAngle = 15f,
                onClick = onPersonalSpaceModeClick
            )
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

@Composable
private fun ModeButton(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    info: String,
    icon: Painter,
    waveAngle: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(
                color = AppColors.accentLight.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .requiredHeightIn(min = 200.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            WavyDashedLine(
                modifier = Modifier
                    .matchParentSize(),
                lineColor = AppColors.accent.copy(alpha = 0.1f),
                dotColor = AppColors.accent.copy(alpha = 0.1f),
                waveAngle = waveAngle
            )

            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(70.dp)
                    .rotate(-10f)
                    .offset(x = (-20).dp, y = 10.dp),
                tint = AppColors.accent.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier
                    .matchParentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                if (isSelected) {
                    CheckButtonIcon(
                        modifier = Modifier
                            .padding(start = 20.dp),
                        isChecked = true
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .weight(1f)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(bottom = 4.dp),
                        text = title,
                        style = MaterialTheme.typography.h6.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                    )

                    Text(
                        modifier = Modifier,
                        text = info,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
                    )
                }

                Icon(
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(22.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                )
            }
        }
    }
}
