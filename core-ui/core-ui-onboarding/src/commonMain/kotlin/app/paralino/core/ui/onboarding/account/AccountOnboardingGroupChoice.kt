package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountOnboardingGroupChoice(
    modifier: Modifier = Modifier,
    title: String,
    info: String?,
    onJoinGroup: () -> Unit,
    onCreateGroup: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onBackground,
            )

            if (!info.isNullOrBlank()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = info.annotatedBoldCompose(),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OnboardingChoiceCard(
                icon = CoreAppIcons.Groups.asPainterResource(),
                title = stringResource(MR.strings.onboarding_step_groupChoice_join_title),
                info = stringResource(MR.strings.onboarding_step_groupChoice_join_info),
                nextHint = stringResource(MR.strings.onboarding_step_groupChoice_join_next),
                onClick = onJoinGroup
            )

            OnboardingChoiceCard(
                icon = CoreAppIcons.Add.asPainterResource(),
                title = stringResource(MR.strings.onboarding_step_groupChoice_create_title),
                info = stringResource(MR.strings.onboarding_step_groupChoice_create_info),
                nextHint = stringResource(MR.strings.onboarding_step_groupChoice_create_next),
                onClick = onCreateGroup
            )
        }

        NoticeInfoChip(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 40.dp
                ),
            text = stringResource(MR.strings.onboarding_step_groupChoice_hint),
            icon = CoreAppIcons.InfoCircle.asPainterResource(),
            backgroundColor = MaterialTheme.colors.surface,
            textColor = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            iconTint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
        )
    }
}
